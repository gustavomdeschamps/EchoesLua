"""Validate, normalize and split sprite sheets for LibGDX TexturePacker.

Source PNGs are never modified. Frames are measured in their original grid
cell, resampled on a common canvas, and then trimmed by TexturePacker. This
preserves originalWidth/originalHeight/offsetX/offsetY for every AtlasRegion.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import argparse
import json

import numpy as np
from PIL import Image


@dataclass(frozen=True)
class Grid:
    columns: int
    rows: int
    limit: int


@dataclass(frozen=True)
class AnchorExpectation:
    foot_min: float
    foot_max: float
    center_min: float
    center_max: float


GRIDS = {
    "astronauta_sheet": Grid(4, 4, 160),
    "astronaut_combat_sheet": Grid(4, 3, 160),
    "mission_atlas_unified": Grid(4, 4, 256),
    "mars_atlas_v4": Grid(4, 3, 256),
    "lunar_enemy_sheet": Grid(4, 4, 160),
    "mars_drone_sheet": Grid(4, 4, 160),
    "mars_crawler_sheet": Grid(4, 4, 160),
    "titan_hunter_sheet_v3": Grid(4, 4, 160),
    "titan_enemy_sheet": Grid(4, 4, 160),
    "titan_boss_sheet_v3": Grid(4, 4, 320),
    "campaign_portal_sheet_v2": Grid(4, 4, 256),
    "lunar_repair_stations_v2": Grid(4, 4, 256),
    "titan_formations_v2": Grid(3, 2, 256),
    "npc_colony_officer_sheet_v2": Grid(4, 4, 160),
    "npc_commander_ayla_sheet": Grid(4, 4, 160),
    "npc_researcher_lira_sheet_v2": Grid(4, 4, 160),
    "titan_portal_vertical_v2": Grid(4, 2, 256),
    "mars_station_sheet_v2": Grid(4, 3, 256),
    "titan_refinery_sheet_v2": Grid(4, 1, 256),
    "lunar_obstacles": Grid(3, 2, 256),
    "mars_obstacles": Grid(3, 2, 256),
    "landmarks": Grid(4, 2, 256),
    "action_fx_sheet": Grid(6, 4, 128),
    "energy_fx_sheet": Grid(6, 4, 128),
    "resource_icons": Grid(4, 1, 64),
}

# Coordinates in the original 313x313 cells. Wider foot ranges are deliberate
# pose changes documented in the supplied art package.
EXPECTED_ANCHORS = {
    "astronauta_sheet": AnchorExpectation(275, 281, 153, 159),
    # The package note says 121..233 for combat footY, but the delivered gold
    # sheet consistently measures 276..278. Using the note would introduce a
    # 43+ pixel jump, so the measured common floor is the reviewed authority.
    "astronaut_combat_sheet": AnchorExpectation(273, 281, 153, 159),
    # Wide attacks intentionally extend the boss silhouette asymmetrically.
    "titan_boss_sheet_v3": AnchorExpectation(256, 291, 149, 167),
    "titan_hunter_sheet_v3": AnchorExpectation(250, 258, 161, 168),
    "lunar_enemy_sheet": AnchorExpectation(271, 278, 151, 159),
    "mars_crawler_sheet": AnchorExpectation(272, 279, 152, 159),
    "mars_drone_sheet": AnchorExpectation(272, 280, 151, 159),
    "titan_enemy_sheet": AnchorExpectation(272, 279, 152, 159),
    "npc_commander_ayla_sheet": AnchorExpectation(283, 290, 155, 161),
    "npc_researcher_lira_sheet_v2": AnchorExpectation(284, 291, 167, 175),
    "npc_colony_officer_sheet_v2": AnchorExpectation(274, 282, 158, 165),
}

STANDALONE_LIMITS = {"oxigenio": 128, "comida": 128, "gelo": 128}
ALPHA_THRESHOLD = 10
MIN_TRANSPARENT_PADDING = 4


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    alpha = np.asarray(image.getchannel("A"), dtype=np.uint8)
    ys, xs = np.nonzero(alpha > ALPHA_THRESHOLD)
    if xs.size == 0:
        return None
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def measurement(bbox: tuple[int, int, int, int]) -> tuple[float, float]:
    left, _top, right, bottom = bbox
    return float(bottom - 1), (left + right - 1) / 2.0


def normalize_alpha(image: Image.Image) -> Image.Image:
    rgba = np.array(image.convert("RGBA"), dtype=np.uint8, copy=True)
    rgba[rgba[:, :, 3] <= ALPHA_THRESHOLD] = 0
    return Image.fromarray(rgba, "RGBA")


def padding_issue(name: str, index: int, bbox: tuple[int, int, int, int], w: int, h: int) -> str | None:
    left, top, right, bottom = bbox
    padding = min(left, top, w - right, h - bottom)
    if padding < MIN_TRANSPARENT_PADDING:
        return f"{name}[{index:02}] possui somente {padding}px de margem transparente"
    return None


def split(root: Path, report_path: Path) -> None:
    root = root.resolve()
    project = Path(__file__).resolve().parents[1]
    import_audit = project / 'tools/source_assets/rework_20260914/installed.json'
    imported = json.loads(import_audit.read_text(encoding='utf-8')) if import_audit.exists() else {}
    if root != (project / "build" / "atlas-input").resolve():
        raise ValueError("Somente build/atlas-input pode ser normalizado")

    report: dict[str, object] = {
        "alphaThreshold": ALPHA_THRESHOLD,
        "minimumPadding": MIN_TRANSPARENT_PADDING,
        "sheets": {},
        "errors": [],
        "warnings": [],
    }
    errors: list[str] = report["errors"]  # type: ignore[assignment]
    warnings: list[str] = report["warnings"]  # type: ignore[assignment]

    for path in sorted(root.rglob("*.png")):
        if path.stem in STANDALONE_LIMITS:
            with Image.open(path) as source:
                source = normalize_alpha(source)
                bbox = alpha_bbox(source)
                if bbox is None:
                    errors.append(f"Coletável vazio: {path.name}")
                    continue
                cropped = source.crop(bbox)
                limit = STANDALONE_LIMITS[path.stem]
                cropped.thumbnail((limit - 8, limit - 8), Image.Resampling.LANCZOS)
                padded = Image.new("RGBA", (cropped.width + 8, cropped.height + 8))
                padded.paste(cropped, (4, 4))
                normalize_alpha(padded).save(path)
            continue

        grid = GRIDS.get(path.stem)
        if grid is None:
            continue
        with Image.open(path) as opened:
            source = normalize_alpha(opened)
            if source.width % grid.columns or source.height % grid.rows:
                errors.append(f"Grade não integral: {path.name} ({source.width}x{source.height})")
                continue
            cell_w, cell_h = source.width // grid.columns, source.height // grid.rows
            scale = min(1.0, grid.limit / max(cell_w, cell_h))
            output_size = (round(cell_w * scale), round(cell_h * scale))
            records: list[dict[str, object]] = []
            source_frames: list[Image.Image] = []

            for row in range(grid.rows):
                for column in range(grid.columns):
                    index = row * grid.columns + column
                    frame = source.crop((column * cell_w, row * cell_h,
                                         (column + 1) * cell_w, (row + 1) * cell_h))
                    bbox = alpha_bbox(frame)
                    if bbox is None:
                        errors.append(f"Quadro vazio: {path.stem}[{index:02}]")
                        continue
                    foot_y, center_x = measurement(bbox)
                    issue = padding_issue(path.stem, index, bbox, cell_w, cell_h)
                    if issue:
                        errors.append(issue)
                    records.append({"index": index, "row": row, "column": column,
                                    "bbox": list(bbox), "footY": foot_y, "centerX": center_x})
                    source_frames.append(frame.copy())

            if records:
                feet = np.array([record["footY"] for record in records], dtype=float)
                centers = np.array([record["centerX"] for record in records], dtype=float)
                median_foot = float(np.median(feet))
                median_center = float(np.median(centers))
                expectation = EXPECTED_ANCHORS.get(path.stem)
                if path.name in imported and imported[path.name]['mode'] == 'actor':
                    expectation = AnchorExpectation(275, 281, 153, 159)
                for record in records:
                    foot = float(record["footY"])
                    center = float(record["centerX"])
                    record["footDeviation"] = round(foot - median_foot, 2)
                    record["centerDeviation"] = round(center - median_center, 2)
                    index = int(record["index"])
                    if expectation is not None:
                        if not expectation.foot_min <= foot <= expectation.foot_max:
                            errors.append(f"{path.stem}[{index:02}] footY={foot:g} fora de "
                                          f"{expectation.foot_min:g}..{expectation.foot_max:g}")
                        if not expectation.center_min <= center <= expectation.center_max:
                            errors.append(f"{path.stem}[{index:02}] centerX={center:g} fora de "
                                          f"{expectation.center_min:g}..{expectation.center_max:g}")
                    elif imported.get(path.name, {}).get('mode') != 'fx' and (
                            abs(foot - median_foot) > 3 or abs(center - median_center) > 3):
                        warnings.append(f"Revisar {path.stem}[{index:02}]: "
                                        f"desvio pé={foot-median_foot:.1f}, centro={center-median_center:.1f}")

                # The boss walk row is locomotion, not an explosion pose. Its
                # delivered silhouettes drift vertically, so align only that
                # row on the disposable packing canvas. Other boss rows retain
                # their documented attack/death height changes.
                row_feet = {
                    row: float(np.median([float(record["footY"]) for record in records
                                          if int(record["row"]) == row]))
                    for row in range(grid.rows)
                }
                for record, frame in zip(records, source_frames):
                    shift_y = 0
                    if path.stem == "titan_boss_sheet_v3" and int(record["row"]) == 1:
                        shift_y = round(row_feet[1] - float(record["footY"]))
                    if shift_y:
                        aligned = Image.new("RGBA", frame.size)
                        aligned.alpha_composite(frame, (0, shift_y))
                        frame = aligned
                    # One fixed canvas preserves the common pivot. TexturePacker
                    # trims it and records the packed offset in the AtlasRegion.
                    normalized = normalize_alpha(frame.resize(output_size, Image.Resampling.LANCZOS))
                    normalized.save(path.with_name(f"{path.stem}_{int(record['index']):02}.png"))

                sheets: dict[str, object] = report["sheets"]  # type: ignore[assignment]
                sheets[path.stem] = {
                    "sourceCell": [cell_w, cell_h], "outputCanvas": list(output_size),
                    "medianFootY": median_foot, "medianCenterX": median_center,
                    "frames": records,
                }
        path.unlink()
        print(f"{path.stem}: {grid.columns * grid.rows} quadros, canvas {output_size}")

    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Relatório de alinhamento: {report_path}")
    if errors:
        for issue in errors:
            print(f"ERRO: {issue}")
        raise SystemExit(f"Normalização bloqueada: {len(errors)} erro(s) exigem revisão")
    print(f"Normalização aprovada; {len(warnings)} aviso(s) para inspeção visual")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--report", type=Path, default=Path("build/asset-qa/sprite_metrics.json"))
    arguments = parser.parse_args()
    split(arguments.root, arguments.report.resolve())
