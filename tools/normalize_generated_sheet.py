"""Normalize a keyed image into an exact, safely padded sprite-sheet grid."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageOps

from prepare_visual_assets import _fit_cell, _isolate_cell_subjects


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("columns", type=int)
    parser.add_argument("rows", type=int)
    parser.add_argument("width", type=int)
    parser.add_argument("height", type=int)
    parser.add_argument("--keep-components", action="store_true",
                        help="Do not discard disconnected details in already-clean art.")
    parser.add_argument("--margin", type=float, default=.075,
                        help="Transparent margin reserved inside every cell.")
    args = parser.parse_args()

    target = (args.width, args.height)
    image = Image.open(args.input).convert("RGBA")
    image = ImageOps.fit(image, target, method=Image.Resampling.LANCZOS)
    if not args.keep_components:
        image = _isolate_cell_subjects(image, args.columns, args.rows, guard_ratio=.012)

    cell_w, cell_h = args.width // args.columns, args.height // args.rows
    output = Image.new("RGBA", target, (0, 0, 0, 0))
    for row in range(args.rows):
        for column in range(args.columns):
            cell = image.crop((column * cell_w, row * cell_h,
                               (column + 1) * cell_w, (row + 1) * cell_h))
            normalized = _fit_cell(cell, (cell_w, cell_h), margin=args.margin)
            output.alpha_composite(normalized, (column * cell_w, row * cell_h))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    output.save(args.output, optimize=True)


if __name__ == "__main__":
    main()
