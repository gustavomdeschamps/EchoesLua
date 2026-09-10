# -*- coding: utf-8 -*-
"""Ancora a base do corpo de uma linha de maquina numa baseline unica.

O documento de arte exige que as maquinas animadas -- estacoes e refinaria --
mantenham corpo, base e escala identicos entre os quadros, e animem apenas os
componentes que de fato se mexem: LEDs, ventoinha, prato, esteira, vapor.

O pipeline de normalizacao ajusta cada celula isoladamente, entao um quadro em
que o painel abre ou o vapor cresce muda a caixa daquela celula e a maquina
inteira sobe ou desce alguns pixels. Em jogo isso le como a maquina pulando de
posicao a cada quadro, e nao como um mecanismo funcionando.

A correcao aqui e deliberadamente conservadora: apenas translacao vertical,
nunca reescala. A baseline alvo e a mediana da linha, medida so nos pixels
opacos -- vapor e brilho tem alpha baixo e nao podem definir onde o chao da
maquina esta. Se algum deslocamento fosse empurrar conteudo para fora da
celula ou para cima da borda, o script aborta em vez de estragar a folha.

Uso:
    python tools/anchor_machine_rows.py <folha> <colunas> <linhas> <linha>[,<linha>...]
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

# Corpo solido. Vapor, glow e particulas ficam abaixo deste limiar.
OPAQUE = 200
# Qualquer conteudo visivel, para conferir a folga depois do deslocamento.
VISIBLE = 12
# Margem minima que precisa sobrar entre o conteudo e a borda da celula.
EDGE_GUARD = 2

REPO = Path(__file__).resolve().parent.parent
SOURCE_BACKUP = REPO / "tools" / "source_assets"


def opaque_bottom(alpha: np.ndarray) -> int | None:
    rows = np.where((alpha > OPAQUE).any(axis=1))[0]
    return int(rows.max()) if rows.size else None


def visible_box(alpha: np.ndarray):
    ys, xs = np.where(alpha > VISIBLE)
    if not ys.size:
        return None
    return int(ys.min()), int(ys.max()), int(xs.min()), int(xs.max())


def anchor(path: Path, columns: int, rows: int, targets: list[int]) -> int:
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    if width % columns or height % rows:
        sys.exit("A folha %dx%d nao divide na grade %dx%d" % (width, height, columns, rows))
    cell_w, cell_h = width // columns, height // rows
    alpha = np.asarray(image)[:, :, 3]

    moves: list[tuple[int, int, int]] = []
    for row in targets:
        if row < 0 or row >= rows:
            sys.exit("Linha %d fora da grade" % row)
        bottoms = []
        for column in range(columns):
            cell = alpha[row * cell_h:(row + 1) * cell_h,
                         column * cell_w:(column + 1) * cell_w]
            bottoms.append(opaque_bottom(cell))
        known = [b for b in bottoms if b is not None]
        if len(known) < 2:
            print("linha %d: corpo opaco insuficiente, ignorada" % row)
            continue
        target = int(np.median(known))
        for column, bottom in enumerate(bottoms):
            if bottom is None:
                continue
            shift = target - bottom
            if shift:
                moves.append((row, column, shift))

    if not moves:
        print("Nada a corrigir em %s" % path.name)
        return 0

    # Confere folga antes de escrever: um deslocamento nunca pode cortar arte.
    for row, column, shift in moves:
        cell = alpha[row * cell_h:(row + 1) * cell_h,
                     column * cell_w:(column + 1) * cell_w]
        box = visible_box(cell)
        if box is None:
            continue
        top, bottom, _, _ = box
        if top + shift < EDGE_GUARD or bottom + shift > cell_h - 1 - EDGE_GUARD:
            sys.exit("ABORTADO: deslocar c%d,l%d em %+dpx encostaria na borda "
                     "da celula. Refaca a fonte em vez de forcar a margem."
                     % (column, row, shift))

    backup = SOURCE_BACKUP / (path.stem + "_pre_anchor.png")
    if not backup.exists():
        SOURCE_BACKUP.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, backup)
        print("fonte preservada em %s" % backup.relative_to(REPO))

    for row, column, shift in moves:
        left, top = column * cell_w, row * cell_h
        cell = image.crop((left, top, left + cell_w, top + cell_h))
        moved = Image.new("RGBA", (cell_w, cell_h), (0, 0, 0, 0))
        if shift > 0:
            # Desce: o excedente sai pela base, ja garantido vazio pela conferencia.
            moved.paste(cell.crop((0, 0, cell_w, cell_h - shift)), (0, shift))
        else:
            # Sobe: descarta as linhas de cima, tambem ja conferidas como vazias.
            moved.paste(cell.crop((0, -shift, cell_w, cell_h)), (0, 0))
        image.paste(moved, (left, top))
        print("c%d,l%d deslocada %+dpx" % (column, row, shift))

    image.save(path, optimize=True)
    print("%s regravada com %d celula(s) ancorada(s)" % (path.name, len(moves)))
    return len(moves)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sheet", type=Path)
    parser.add_argument("columns", type=int)
    parser.add_argument("rows", type=int)
    parser.add_argument("targets", help="linhas a ancorar, separadas por virgula")
    args = parser.parse_args()
    targets = [int(v) for v in args.targets.split(",") if v.strip()]
    anchor(args.sheet, args.columns, args.rows, targets)


if __name__ == "__main__":
    main()
