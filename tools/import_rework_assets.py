"""Import reviewed generated sources; preserve alpha and normalize grids before packing."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'tools/source_assets/rework_20260914'
BACKUP = ROOT / 'tools/source_assets/before_rework_20260914'
TEXTURES = ROOT / 'assets/textures'
CELL = 313


def bounds(image):
    alpha = np.asarray(image.getchannel('A'))
    ys, xs = np.nonzero(alpha > 10)
    if not len(xs):
        raise ValueError('Empty sprite')
    return (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)


def clean_alpha(image):
    pixels = np.array(image.convert('RGBA'), copy=True)
    pixels[pixels[:, :, 3] <= 10] = 0
    return Image.fromarray(pixels)


def remove_isolated_specks(image, grid=None):
    """Remove disconnected subpixel export debris, never erase a sprite's edge."""
    pixels = np.array(image.convert('RGBA'), copy=True)
    mask = pixels[:, :, 3] > 10
    parents, runs, previous = [], [], []
    def root(label):
        while parents[label] != label:
            parents[label] = parents[parents[label]]
            label = parents[label]
        return label
    for y, line in enumerate(mask):
        changes = np.diff(np.pad(line.astype(np.int8), (1, 1)))
        current = []
        for left, right in zip(np.flatnonzero(changes == 1), np.flatnonzero(changes == -1)):
            neighbours = [r[2] for r in previous if r[1] >= left and r[0] <= right]
            label = len(parents)
            parents.append(label)
            for other in neighbours:
                parents[root(other)] = root(label)
            current.append((left, right, label))
            runs.append((y, left, right, label))
        previous = current
    areas = {}
    for y, left, right, label in runs:
        key = root(label)
        areas[key] = areas.get(key, 0) + right-left
    for y, left, right, label in runs:
        if areas[root(label)] < 24:
            pixels[y, left:right] = 0
    if grid:
        columns, rows = grid
        large = sorted(areas, key=areas.get, reverse=True)[:columns*rows]
        if areas[large[-1]] < image.width*image.height/(columns*rows)*.04:
            raise ValueError('Expected complete separate sprite components not found')
        boxes = {key: [image.width, image.height, 0, 0] for key in large}
        for y, left, right, label in runs:
            key = root(label)
            if key in boxes:
                b = boxes[key]
                b[:] = [min(b[0],left),min(b[1],y),max(b[2],right),max(b[3],y+1)]
        ordered = sorted(large, key=lambda k: (boxes[k][1]+boxes[k][3])/2)
        result = []
        for row in range(rows):
            row_keys = sorted(ordered[row*columns:(row+1)*columns],
                              key=lambda k: (boxes[k][0]+boxes[k][2])/2)
            for key in row_keys:
                b = boxes[key]
                if min(b[0],b[1],image.width-b[2],image.height-b[3]) < 2:
                    raise ValueError('Complete sprite touches source outer border')
                isolated = np.zeros((b[3]-b[1],b[2]-b[0],4), dtype=np.uint8)
                for y,left,right,label in runs:
                    if root(label) == key:
                        isolated[y-b[1],left-b[0]:right-b[0]] = pixels[y,left:right]
                result.append(Image.fromarray(isolated))
        return result
    return Image.fromarray(pixels)


def gutter_boundaries(projection, count):
    """Locate actual transparent gutters; generated sheets rarely have exact cells."""
    size = len(projection)
    edges = [0]
    for index in range(1, count):
        expected = index * size / count
        radius = size / count * .42
        candidates = np.flatnonzero(projection == 0)
        candidates = candidates[(candidates > expected-radius) & (candidates < expected+radius)]
        if not len(candidates):
            raise ValueError(f'No clear gutter near {expected:.1f}/{size}; source requires review')
        runs = np.split(candidates, np.where(np.diff(candidates) != 1)[0]+1)
        runs = [run for run in runs if len(run) >= 4]
        if not runs:
            raise ValueError('Transparent gutter narrower than four pixels')
        run = min(runs, key=lambda r: abs((r[0]+r[-1])/2-expected))
        edges.append(int((run[0]+run[-1])/2))
    return edges + [size]


def source_frames(image, columns, rows):
    alpha = np.asarray(image.getchannel('A')) > 10
    try:
        return source_frames_columns(image, columns, rows)
    except ValueError:
        # Tall antennas can cross another column's row gap without overlapping
        # a neighbour. Transpose detection, never trim through their pixels.
        transposed = image.transpose(Image.Transpose.TRANSPOSE)
        try:
            parts = source_frames_columns(transposed, rows, columns)
        except ValueError:
            return remove_isolated_specks(image, (columns, rows))
        return [parts[c*rows+r].transpose(Image.Transpose.TRANSPOSE)
                for r in range(rows) for c in range(columns)]


def source_frames_columns(image, columns, rows):
    alpha = np.asarray(image.getchannel('A')) > 10
    y_edges = gutter_boundaries(alpha.sum(axis=1), rows)
    frames = []
    for row in range(rows):
        top, bottom = y_edges[row:row+2]
        x_edges = gutter_boundaries(alpha[top:bottom].sum(axis=0), columns)
        for column in range(columns):
            frame = image.crop((x_edges[column], top, x_edges[column+1], bottom))
            box = bounds(frame)
            if min(box[0], box[1], frame.width-box[2], frame.height-box[3]) < 2:
                raise ValueError(f'Source touches cell edge: {column},{row}; regenerate, do not crop')
            frames.append(frame.crop(box))
    return frames


def normalized_sheet(image, columns, rows, mode):
    frames = source_frames(image, columns, rows)
    sheet = Image.new('RGBA', (CELL * columns, CELL * rows))
    for index, frame in enumerate(frames):
        # Props share footprint; actors share height except intentional death silhouettes.
        max_w, max_h = CELL * .80, CELL * .76
        if mode == 'actor':
            scale = min(max_w/max(f.width for f in frames), max_h/max(f.height for f in frames))
        elif mode == 'fx':
            row_frames = frames[index//columns*columns:(index//columns+1)*columns]
            scale = min(max_w/max(f.width for f in row_frames), max_h/max(f.height for f in row_frames))
        elif mode == 'machine':
            row_frames = frames[index//columns*columns:(index//columns+1)*columns]
            scale = min(max_w/max(f.width for f in row_frames), max_h/max(f.height for f in row_frames))
        else:
            scale = min(max_w/frame.width, max_h/frame.height)
        fitted = clean_alpha(frame.resize((max(1, round(frame.width*scale)),
            max(1, round(frame.height*scale))), Image.Resampling.LANCZOS))
        x = (CELL-fitted.width)//2
        y = (CELL-fitted.height)//2 if mode == 'fx' else round(CELL*.89)-fitted.height
        sheet.alpha_composite(fitted, (index%columns*CELL+x, index//columns*CELL+y))
    return clean_alpha(sheet)


def install(key, filename, columns=1, rows=1, mode='static', cell=None):
    manifest = json.loads((SOURCES/'manifest.json').read_text(encoding='utf-8'))
    source = SOURCES / manifest[key]['source']
    image = clean_alpha(Image.open(source))
    if mode != 'fx':
        image = remove_isolated_specks(image)
    if cell is not None:
        x, y = cell
        image = source_frames(image, columns, rows)[y*columns+x]
        columns = rows = 1
    if columns == rows == 1:
        cropped = image.crop(bounds(image))
        cropped.thumbnail((768, 768), Image.Resampling.LANCZOS)
        output = Image.new('RGBA', (cropped.width+32, cropped.height+32))
        output.alpha_composite(cropped, (16, 16))
    else:
        output = normalized_sheet(image, columns, rows, mode)
    target = TEXTURES / filename
    BACKUP.mkdir(parents=True, exist_ok=True)
    if target.exists() and not (BACKUP/filename).exists():
        shutil.copy2(target, BACKUP/filename)
    staging = ROOT / 'build/rework-staging'
    staging.mkdir(parents=True, exist_ok=True)
    staged = staging / filename
    output.save(staged, optimize=True)
    staged.replace(target)
    audit_path = SOURCES/'installed.json'
    audit = json.loads(audit_path.read_text(encoding='utf-8')) if audit_path.exists() else {}
    audit[filename] = {'source': str(source.relative_to(ROOT)), 'grid': [columns, rows],
        'mode': mode, 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()}
    audit_path.write_text(json.dumps(audit, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    print(f'Installed {filename}: {output.size}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('key')
    parser.add_argument('filename')
    parser.add_argument('--grid', nargs=2, type=int, default=[1, 1])
    parser.add_argument('--mode', choices=['static', 'actor', 'machine', 'fx'], default='static')
    parser.add_argument('--cell', nargs=2, type=int)
    args = parser.parse_args()
    install(args.key, args.filename, *args.grid, args.mode, args.cell)
