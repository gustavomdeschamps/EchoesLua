"""Split declared grids before TexturePacker; retain canvas/pivot, extrude per frame.

Source art stays unchanged. Only the disposable build/atlas-input is rewritten.
"""
from pathlib import Path
import argparse
from PIL import Image

GRIDS = {
    'astronauta_sheet': (4, 4, 160), 'astronaut_combat_sheet': (4, 3, 160),
    'mission_atlas_unified': (4, 4, 256), 'mars_atlas_v4': (4, 3, 256),
    'lunar_enemy_sheet': (4, 4, 160), 'mars_drone_sheet': (4, 4, 160),
    'mars_crawler_sheet': (4, 4, 160), 'titan_hunter_sheet_v3': (4, 4, 160),
    'titan_boss_sheet_v3': (4, 4, 320), 'campaign_portal_sheet_v2': (4, 4, 256),
    'lunar_repair_stations_v2': (4, 4, 256), 'titan_formations_v2': (3, 2, 256),
    'npc_colony_officer_sheet_v2': (4, 4, 160),
    'npc_commander_ayla_sheet': (4, 4, 160),
    'titan_portal_vertical_v2': (4, 2, 256),
    'lunar_obstacles': (3, 2, 256), 'mars_obstacles': (3, 2, 256),
    'landmarks': (4, 2, 256), 'action_fx_sheet': (6, 4, 128),
    'energy_fx_sheet': (6, 4, 128), 'resource_icons': (4, 1, 64),
}

STANDALONE_LIMITS = {'oxigenio': 128, 'comida': 128, 'gelo': 128}

def split(root: Path):
    root = root.resolve()
    expected = Path(__file__).resolve().parents[1] / 'build' / 'atlas-input'
    if root != expected.resolve():
        raise ValueError('Only build/atlas-input may be processed')
    for path in sorted(root.rglob('*.png')):
        if path.stem in STANDALONE_LIMITS:
            with Image.open(path) as original:
                original = original.convert('RGBA')
                silhouette = original.getchannel('A').point(lambda alpha: 255 if alpha > 8 else 0)
                bbox = silhouette.getbbox()
                if bbox is None:
                    raise ValueError(f'Empty collectible: {path}')
                original = original.crop(bbox)
                limit = STANDALONE_LIMITS[path.stem]
                original.thumbnail((limit - 8, limit - 8), Image.Resampling.LANCZOS)
                padded = Image.new('RGBA', (original.width + 8, original.height + 8))
                padded.paste(original, (4, 4))
                padded.save(path)
            continue
        grid = GRIDS.get(path.stem)
        if grid is None:
            continue
        columns, rows, limit = grid
        with Image.open(path) as original:
            original = original.convert('RGBA')
            if original.width % columns or original.height % rows:
                raise ValueError(f'Non-integral grid: {path}')
            w, h = original.width // columns, original.height // rows
            scale = min(1., limit / max(w, h))
            size = (round(w * scale), round(h * scale))
            for row in range(rows):
                for col in range(columns):
                    frame = original.crop((col*w, row*h, (col+1)*w, (row+1)*h))
                    frame = frame.resize(size, Image.Resampling.LANCZOS)
                    frame.save(path.with_name(f'{path.stem}_{row*columns+col:02}.png'))
        path.unlink()  # build copy only; original art is preserved
        print(f'{path.stem}: {rows*columns} frames, {size}')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root', type=Path)
    split(parser.parse_args().root)
