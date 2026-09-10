# -*- coding: utf-8 -*-
"""Corrige clique de borda, pico inseguro e laco descontinuo no audio.

Tres defeitos medidos por tools/audit_audio.py, cada um com uma correcao
diferente e deliberadamente conservadora:

**Clique de borda.** Um efeito que comeca no meio da onda, longe do zero,
produz um estalo. O pior caso media 0.741 de amplitude no primeiro bloco -- e
era o som de passo lunar, o mais tocado do jogo. A correcao e uma rampa muito
curta, de poucos milissegundos: longa o bastante para eliminar o degrau,
curta o bastante para nao comer o transiente que da o impacto ao som.

**Pico inseguro.** Tres arquivos decodificavam acima de 1.0 -- distorcao antes
mesmo de somar com qualquer outra coisa. A correcao e ganho linear ate o teto:
o som fica mais baixo, nao diferente. Nada de compressao nem limitacao, que
mudariam o carater.

Reencodar Vorbis faz o pico crescer, e nao um pouco: escrever `alerta_oxigenio`
com pico 0.89 devolveu 1.026 na leitura de volta -- 15% de sobretom, porque o
codec toca por cima do transiente. Por isso o ganho nao e calculado uma vez e
aceito no escuro: o arquivo e regravado, lido de volta e medido, e o ganho e
reduzido ate o pico decodificado -- que e o que o jogo realmente reproduz --
ficar sob o teto.

**Laco descontinuo.** Num arquivo em laco, a ultima amostra precisa encostar
na primeira. A correcao cruza a cauda com a cabeca: o resultado fica N amostras
mais curto, e o ponto de laco passa a cair entre duas amostras que ja eram
vizinhas no original -- continuo por construcao, sem o vao que uma rampa
simples deixaria.

**Sub-nivel para o papel.** Alguns arquivos foram gravados baixo demais para o
que representam: o rugido do chefe final tinha pico 0.47, metade da escala
sobrando, e mesmo tocado com ganho 1.0 saia mais baixo que um hover de menu.
Esses recebem ganho ate o mesmo teto dos demais, para que o ganho de
reproducao tenha faixa para trabalhar.

O que este script NAO faz e igualar volumes. O documento e explicito: nao
normalizar cada som ate todos parecerem igualmente importantes. Elevar um
asset que esta abaixo do proprio papel e o oposto disso -- e o que devolve
faixa ao mixer. O balanco final entre eventos fica no ganho de reproducao, no
SoundManager, nao no arquivo.

Conteudo em laco nunca recebe rampa. Ele comeca em amplitude nao-nula de
proposito, porque continua de onde parou; uma rampa ali trocaria um problema
que nao existe por um vao audivel a cada volta.

Uso:
    python tools/repair_audio.py [--dry-run]
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

import numpy as np
import soundfile as sf

REPO = Path(__file__).resolve().parent.parent
SOUNDS = REPO / "assets" / "sounds"
MUSIC = REPO / "assets" / "music"
BACKUP = REPO / "tools" / "legacy_audio" / "pre_repair"

# Toca em laco mesmo morando em sounds/: segue as regras de laco.
LOOPING_IN_SOUNDS = {"menu_ambiente.ogg"}

# Gravados abaixo do papel que ocupam. Mesmo com ganho 1.0 no mixer ficavam
# atras de sons secundarios, entao o mixer nao tinha faixa para corrigir.
UNDER_LEVELLED = {
    "boss_rugido.ogg", "boss_ataque.ogg", "boss_morte.ogg",
    "impacto_hostil.ogg", "disparo_pulso.ogg", "portal_ativar.ogg",
    "passo_marte.ogg", "passo_tita.ogg",
}

# Teto medido no arquivo ja decodificado, que e o que o jogo reproduz.
PEAK_CEILING = 0.94
# Alvo antes de encodar; a diferenca cobre o sobretom do codec na primeira
# tentativa e evita gastar iteracoes no caso comum.
PRE_ENCODE_TARGET = 0.86
# Tentativas de reducao ate o pico decodificado entrar sob o teto.
MAX_ENCODE_PASSES = 6
# Acima disto o arquivo ja esta estourado no proprio arquivo.
PEAK_TRIGGER = 0.985
# Rampa de borda dos efeitos. Curta para preservar o transiente do impacto.
SFX_FADE_MS = 3.0
# Rampa de borda das musicas que nao entram em laco.
MUSIC_FADE_MS = 25.0
# Trecho cruzado no ponto de laco das musicas.
LOOP_CROSSFADE_MS = 250.0
# Amplitude de borda a partir da qual vale a pena aplicar a rampa.
EDGE_LEVEL = 0.06
EDGE_WINDOW = 64


def edge_peak(mono: np.ndarray, tail: bool) -> float:
    """Degrau da ponta ate o zero; e ele que estala, nao o nivel do trecho."""
    return float(abs(mono[-1] if tail else mono[0]))


def apply_fade(data: np.ndarray, samples: int, tail: bool) -> None:
    samples = min(samples, len(data))
    if samples <= 1:
        return
    ramp = np.linspace(0.0, 1.0, samples, dtype=data.dtype)
    if tail:
        data[-samples:] *= ramp[::-1, None]
    else:
        data[:samples] *= ramp[:, None]


def crossfade_loop(data: np.ndarray, samples: int) -> np.ndarray:
    """Cruza a cauda com a cabeca para o laco fechar sem degrau."""
    samples = min(samples, len(data) // 4)
    if samples <= 1:
        return data
    head = data[:samples]
    tail = data[-samples:]
    ramp = np.linspace(0.0, 1.0, samples, dtype=data.dtype)[:, None]
    merged = head * ramp + tail * (1.0 - ramp)
    result = data[:len(data) - samples].copy()
    result[:samples] = merged
    return result


def repair(path: Path, is_music: bool, dry_run: bool) -> list[str]:
    data, rate = sf.read(str(path), always_2d=True, dtype="float64")
    mono = data.mean(axis=1)
    if not mono.size or float(np.abs(mono).max()) < 1e-4:
        return []

    actions: list[str] = []
    peak = float(np.abs(mono).max())
    clipping = peak > PEAK_TRIGGER
    if clipping:
        actions.append("pico %.3f acima do limite" % peak)
    elif path.name in UNDER_LEVELLED and peak < PRE_ENCODE_TARGET * .95:
        actions.append("sub-nivelado: pico %.3f" % peak)

    if is_music or path.name in LOOPING_IN_SOUNDS:
        # So o degrau do laco justifica tocar em conteudo que repete.
        step = float(abs(mono[0] - mono[-1]))
        if step > EDGE_LEVEL:
            before = len(data)
            data = crossfade_loop(data, int(rate * LOOP_CROSSFADE_MS / 1000.0))
            actions.append("laco cruzado em %.0fms (degrau %.3f, %d->%d amostras)"
                           % (LOOP_CROSSFADE_MS, step, before, len(data)))
    else:
        fade = max(2, int(rate * SFX_FADE_MS / 1000.0))
        if edge_peak(mono, False) > EDGE_LEVEL:
            apply_fade(data, fade, False)
            actions.append("declique de entrada %.0fms" % SFX_FADE_MS)
        if edge_peak(mono, True) > EDGE_LEVEL:
            apply_fade(data, fade, True)
            actions.append("declique de saida %.0fms" % SFX_FADE_MS)

    if not actions:
        return actions

    # O arquivo vai ser regravado de qualquer jeito: garante folga antes da
    # reencodagem, que por si so ja empurra o pico para cima.
    # Todo arquivo regravado sai no mesmo alvo: os estourados descem ate ele,
    # os sub-nivelados sobem ate ele.
    data *= PRE_ENCODE_TARGET / peak
    actions.append("ganho ate %.2f" % PRE_ENCODE_TARGET)

    if dry_run:
        return actions

    backup = BACKUP / path.name
    if not backup.exists():
        BACKUP.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, backup)

    written = write_under_ceiling(path, data, rate)
    actions.append("pico decodificado %.3f" % written)
    return actions


def write_under_ceiling(path: Path, data: np.ndarray, rate: int) -> float:
    """Regrava ate o pico lido de volta ficar sob o teto, e devolve esse pico.

    Medir o vetor em memoria nao serve: o que o jogo toca e o resultado da
    decodificacao, e e la que o sobretom do codec aparece.
    """
    gain = 1.0
    decoded = 0.0
    for attempt in range(MAX_ENCODE_PASSES):
        sf.write(str(path), data * gain, rate, format="OGG", subtype="VORBIS")
        check, _ = sf.read(str(path), always_2d=True)
        decoded = float(np.abs(check.mean(axis=1)).max())
        if decoded <= PEAK_CEILING:
            return decoded
        # Reduz pelo excesso medido, com uma folga para nao repetir a tentativa.
        gain *= (PEAK_CEILING / decoded) * 0.97
    print("      AVISO: %s continuou em %.3f apos %d tentativas"
          % (path.name, decoded, MAX_ENCODE_PASSES))
    return decoded


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    changed = 0
    for label, folder, is_music in (("EFEITOS", SOUNDS, False), ("MUSICAS", MUSIC, True)):
        print("=== %s ===" % label)
        for path in sorted(folder.glob("*.ogg")):
            actions = repair(path, is_music, args.dry_run)
            if actions:
                changed += 1
                print("  %-26s %s" % (path.name, "; ".join(actions)))
    print("\n%d arquivo(s) %s" % (changed, "seriam alterados" if args.dry_run else "corrigidos"))
    if not args.dry_run and changed:
        print("originais preservados em %s" % BACKUP.relative_to(REPO))


if __name__ == "__main__":
    main()
