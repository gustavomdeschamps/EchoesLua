# -*- coding: utf-8 -*-
"""Auditoria do audio de producao contra as regras de mixagem do jogo.

Mede o que o documento de audio exige e que ninguem ve lendo codigo:

- clipping e pico inseguro;
- clique no inicio e no fim de um efeito de um disparo so, medido como a
  distancia da primeira e da ultima amostra ate o zero: e o degrau que estala,
  nao a amplitude do trecho. Medir o pico de uma janela de 64 amostras acusa
  como clique qualquer rampa mais longa que a janela, inclusive a que acabou
  de remover o degrau;
- laco descontinuo, medindo o degrau entre a ultima e a primeira amostra;
- silencio no comeco, que atrasa a resposta do evento;
- efeito longo demais para o que dispara, e efeito mudo;
- desalinhamento de mix: um som muito acima da mediana encobre os alertas.

A distincao entre clique e laco importa, e foi onde a primeira versao desta
ferramenta errou. Conteudo em laco -- toda a pasta de musica mais a ambiencia
do menu -- *deve* comecar em amplitude nao-nula: ele continua de onde parou.
Julgar laco pela amplitude de borda acusa como clique justamente o que prova
que o laco esta certo, e "corrigir" isso com uma rampa trocaria um problema
inexistente por um vao audivel a cada volta. Laco se mede pelo degrau;
disparo unico, pela borda.

Uso:
    python tools/audit_audio.py
"""

from __future__ import annotations

import re
from pathlib import Path

import numpy as np
import soundfile as sf

REPO = Path(__file__).resolve().parent.parent
SOUNDS = REPO / "assets" / "sounds"
MUSIC = REPO / "assets" / "music"
SOUND_MANAGER = (REPO / "core" / "src" / "main" / "java" / "com" / "orion"
                 / "echoes" / "lua" / "managers" / "SoundManager.java")

# Toca em laco, venha da pasta que vier: o MusicDirector abre a ambiencia do
# menu de dentro de sounds/, e ela segue as regras de laco, nao as de efeito.
LOOPING_IN_SOUNDS = {"menu_ambiente.ogg"}

# Stingers de tela: tocam em derrota, vitoria e no inicio, quando nao existe
# gameplay em curso. Comparar o volume deles com o do alarme de oxigenio nao
# diz nada, porque os dois nunca soam juntos.
SCREEN_STINGERS = {"game_over.ogg", "vitoria.ogg", "menu_iniciar.ogg"}

# Amplitude acima da qual o pico e considerado arriscado para o mix somado.
PEAK_CEILING = 0.985
# Um efeito que comeca ou termina acima disto estala.
EDGE_LEVEL = 0.06
# Janela usada para medir a borda, em amostras.
EDGE_WINDOW = 64
# Efeito de resposta imediata nao pode ter mais que isto de silencio inicial.
MAX_LEAD_SILENCE = 0.035
# Acima disto um efeito deixa de ser curto.
MAX_SFX_SECONDS = 4.0
SILENCE = 1e-4


GAIN_CONST = re.compile(r"(GAIN_\w+)\s*=\s*(\d*\.?\d+)f;")
GAIN_CALL = re.compile(
    r'tocar(?:Variado|Espacial)?\("([a-z_]+)"[^,]*,\s*Bus\.\w+,\s*(GAIN_\w+|\d*\.?\d+f)')


def playback_gains():
    """Ganho de reproducao por som, lido do SoundManager.

    Sem isto o relatorio compara arquivos, e nao o que o jogador ouve: o blip
    de dialogo era acusado por ser um arquivo baixo, mesmo tocando com ganho
    1.0 de proposito, enquanto o hover do menu passava batido tocando alto
    com ganho baixo.
    """
    if not SOUND_MANAGER.exists():
        return {}
    source = SOUND_MANAGER.read_text(encoding="utf-8")
    constants = {m.group(1): float(m.group(2)) for m in GAIN_CONST.finditer(source)}
    gains = {}
    for match in GAIN_CALL.finditer(source):
        name, raw = match.group(1), match.group(2)
        value = constants.get(raw) if raw.startswith("GAIN_") else float(raw.rstrip("f"))
        if value is not None and name not in gains:
            gains[name] = value
    return gains


def short_term(mono, rate, ms=300):
    """RMS da janela mais alta.

    O RMS do arquivo inteiro achata som esparso: um rugido de 1.8s com um
    pico curto mede baixo e soa alto. A janela de 300ms fica proxima do que
    o ouvido julga num disparo unico.
    """
    window = int(rate * ms / 1000)
    if len(mono) <= window:
        return float(np.sqrt(np.mean(mono ** 2)))
    energy = np.convolve(mono ** 2, np.ones(window) / window, mode="valid")
    return float(np.sqrt(energy.max()))


def load(path: Path):
    data, rate = sf.read(str(path), always_2d=True)
    mono = data.mean(axis=1)
    return mono, rate


def lead_silence(mono: np.ndarray, rate: int) -> float:
    loud = np.where(np.abs(mono) > 0.01)[0]
    return 0.0 if not loud.size else loud[0] / float(rate)


def edge_level(mono: np.ndarray, tail: bool) -> float:
    """Distancia da amostra da ponta ate o zero: e ela que produz o estalo."""
    return float(abs(mono[-1] if tail else mono[0]))


def report(path: Path, is_music: bool, rms_values: dict) -> list[str]:
    looping = is_music or path.name in LOOPING_IN_SOUNDS
    mono, rate = load(path)
    problems = []
    if not mono.size:
        return ["arquivo vazio"]

    peak = float(np.abs(mono).max())
    rms = float(np.sqrt(np.mean(mono ** 2)))
    seconds = len(mono) / float(rate)
    rms_values[path.name] = rms

    if peak < SILENCE:
        return ["MUDO: placeholder silencioso"]
    if peak > PEAK_CEILING:
        problems.append("PICO %.3f: risco de clipping ao somar com outros" % peak)

    if looping:
        # Continuidade do laco: a ultima amostra precisa encostar na primeira.
        step = float(np.abs(mono[0] - mono[-1]))
        if step > EDGE_LEVEL:
            problems.append("LACO com degrau de %.3f entre fim e inicio" % step)
    else:
        head, tail = edge_level(mono, False), edge_level(mono, True)
        if head > EDGE_LEVEL:
            problems.append("CLIQUE no inicio: amplitude %.3f no primeiro bloco" % head)
        if tail > EDGE_LEVEL:
            problems.append("CLIQUE no fim: amplitude %.3f no ultimo bloco" % tail)
        lead = lead_silence(mono, rate)
        if lead > MAX_LEAD_SILENCE:
            problems.append("ATRASO de %.0fms antes do som comecar" % (lead * 1000))
        if seconds > MAX_SFX_SECONDS:
            problems.append("LONGO demais para um efeito: %.1fs" % seconds)

    return problems


def main() -> None:
    rms_values: dict[str, float] = {}
    total = 0
    for label, folder, is_music in (("EFEITOS", SOUNDS, False), ("MUSICAS", MUSIC, True)):
        print("\n=== %s ===" % label)
        for path in sorted(folder.glob("*.ogg")):
            problems = report(path, is_music, rms_values)
            mono, rate = load(path)
            head = "%-26s %5.2fs  pico %.3f  rms %.4f" % (
                path.name, len(mono) / float(rate),
                float(np.abs(mono).max()), rms_values.get(path.name, 0.0))
            print(("OK " if not problems else "!! ") + head)
            for problem in problems:
                print("      - " + problem)
            total += len(problems)

    # Coerencia de mix: sonoridade que chega ao jogador, nao nivel de arquivo.
    gains = playback_gains()
    effective = {}
    for name in rms_values:
        path = SOUNDS / name
        if not path.exists() or name in LOOPING_IN_SOUNDS:
            continue
        gain = gains.get(name[:-4] if name.endswith(".ogg") else name)
        if gain is None:
            continue
        mono, rate = load(path)
        effective[name] = short_term(mono, rate) * gain

    if effective:
        print("\n=== MIX (sonoridade efetiva: curto prazo x ganho) ===")
        for name, value in sorted(effective.items(), key=lambda kv: -kv[1]):
            print("   %-24s %.3f" % (name, value))

        # Nada pode competir com o alarme de suporte de vida.
        alert = effective.get("alerta_oxigenio.ogg")
        if alert:
            for name, value in effective.items():
                if name in SCREEN_STINGERS or name == "alerta_oxigenio.ogg":
                    continue
                if value > alert * 0.92:
                    print("!! %s encobre o alerta de oxigenio (%.3f vs %.3f)"
                          % (name, value, alert))
                    total += 1

        # Passos sao o mesmo evento nos tres mundos e precisam soar igual.
        steps = [v for n, v in effective.items() if n.startswith("passo_")]
        if len(steps) > 1 and max(steps) > min(steps) * 1.6:
            print("!! passos desiguais entre os mundos: %.3f a %.3f"
                  % (min(steps), max(steps)))
            total += 1

        # O chefe precisa pesar mais que um som de interface.
        boss = max((v for n, v in effective.items() if n.startswith("boss_")),
                   default=0.0)
        hover = effective.get("hover_ui.ogg", 0.0)
        if boss and hover > boss:
            print("!! hover da interface (%.3f) mais alto que o chefe (%.3f)"
                  % (hover, boss))
            total += 1

    print("\nTOTAL DE PROBLEMAS: %d" % total)


if __name__ == "__main__":
    main()
