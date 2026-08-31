"""Normaliza assets raster de Echoes sem alterar os arquivos-fonte.

Uso:
  python tools/prepare_visual_assets.py

As entradas *_candidate.png permanecem como fonte. As saídas são os assets
consumidos pelo jogo, com alpha real, grids exatos e terrenos tileáveis.
"""

from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter, ImageOps


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "assets" / "textures"
SOURCES = ROOT / "tools" / "source_assets"


def _source_path(name: str) -> Path:
    source = SOURCES / name
    return source if source.exists() else TEXTURES / name


def _checker_alpha(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    width, height = rgba.size
    pixels = rgba.load()
    seen = bytearray(width * height)
    queue: deque[tuple[int, int]] = deque()

    def background(x: int, y: int) -> bool:
        r, g, b, _ = pixels[x, y]
        return max(r, g, b) - min(r, g, b) <= 13 and min(r, g, b) >= 178

    for x in range(width):
        if background(x, 0):
            queue.append((x, 0))
        if background(x, height - 1):
            queue.append((x, height - 1))
    for y in range(height):
        if background(0, y):
            queue.append((0, y))
        if background(width - 1, y):
            queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        index = y * width + x
        if seen[index] or not background(x, y):
            continue
        seen[index] = 1
        r, g, b, _ = pixels[x, y]
        pixels[x, y] = (r, g, b, 0)
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))
    return rgba


def _checker_alpha_global(image: Image.Image) -> Image.Image:
    """Remove checker baked mesmo quando halos separam o fundo das bordas."""
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if max(r, g, b) - min(r, g, b) <= 16 and min(r, g, b) >= 174:
                pixels[x, y] = (r, g, b, 0)
    return rgba


def _edge_key_alpha(
    image: Image.Image,
    columns: int,
    rows: int,
    *,
    fill_subject: bool,
) -> Image.Image:
    """Remove gradientes gerados sem sacrificar contornos ou brilho útil.

    Objetos opacos usam uma silhueta fechada derivada das arestas. VFX usam a
    diferença contra um fundo suavizado, preservando apenas fumaça, faíscas e
    energia. Isto evita halos retangulares que aparecem com chroma-key simples.
    """
    rgba = image.convert("RGBA")
    width, height = rgba.size
    cell_w, cell_h = width // columns, height // rows
    output = Image.new("RGBA", (cell_w * columns, cell_h * rows), (0, 0, 0, 0))

    for row in range(rows):
        for column in range(columns):
            cell = rgba.crop((column * cell_w, row * cell_h,
                              (column + 1) * cell_w, (row + 1) * cell_h))
            rgb = cell.convert("RGB")
            # Cada linha do fundo é um gradiente contínuo. Interpolar as duas
            # bordas da própria linha é mais fiel que uma chave de cor única e
            # não deixa a mancha retangular do spotlight gerado.
            source = rgb.load()
            background = Image.new("RGB", rgb.size)
            target = background.load()
            sample = max(3, cell_w // 90)
            for y in range(cell_h):
                left = tuple(sum(source[x, y][channel] for x in range(sample)) / sample
                             for channel in range(3))
                right = tuple(sum(source[cell_w - 1 - x, y][channel] for x in range(sample)) / sample
                              for channel in range(3))
                for x in range(cell_w):
                    amount = x / max(1, cell_w - 1)
                    target[x, y] = tuple(int(left[channel] * (1.0 - amount)
                                             + right[channel] * amount)
                                         for channel in range(3))
            background = background.filter(ImageFilter.GaussianBlur(radius=2.0))
            difference = ImageChops.difference(rgb, background)
            channels = difference.split()
            detail = ImageChops.lighter(channels[0], ImageChops.lighter(channels[1], channels[2]))

            if fill_subject:
                # A linha escura da arte funciona como limite; a expansão fecha
                # pequenas frestas e o flood-fill preserva todo o interior.
                edge = detail.point(lambda value: 255 if value >= 24 else 0)
                edge = edge.filter(ImageFilter.MaxFilter(7))
                edge = edge.filter(ImageFilter.MinFilter(3))
                outside = ImageOps.invert(edge)
                ImageDraw.floodfill(outside, (0, 0), 128, thresh=2)
                interior = outside.point(lambda value: 255 if value != 128 else 0)
                alpha = ImageChops.lighter(edge, interior)
                alpha = alpha.filter(ImageFilter.GaussianBlur(radius=1.2))
            else:
                # Contraste local separa o efeito do gradiente contínuo. Uma
                # pequena dilatação conserva antialias e o halo imediatamente
                # ligado ao núcleo do efeito.
                detail = ImageEnhance.Contrast(detail).enhance(2.1)
                alpha = detail.point(lambda value: max(0, min(255, (value - 5) * 9)))
                alpha = alpha.filter(ImageFilter.MaxFilter(5))
                alpha = alpha.filter(ImageFilter.GaussianBlur(radius=1.6))

            if fill_subject:
                keyed = cell.copy()
            else:
                # O atlas é renderizado de modo aditivo. Subtrair o gradiente
                # do RGB impede qualquer retângulo escuro mesmo no halo suave.
                keyed_rgb = ImageChops.subtract(rgb, background, scale=0.46)
                keyed = keyed_rgb.convert("RGBA")
            keyed.putalpha(alpha)
            output.alpha_composite(keyed, (column * cell_w, row * cell_h))
    return output


def _clear_portal_window(image: Image.Image) -> Image.Image:
    """Remove o checker interno do portal desligado sem alterar os ícones."""
    result = image.copy()
    width, height = result.size
    cell_w, cell_h = width // 4, height // 4
    alpha = result.getchannel("A")
    draw = ImageDraw.Draw(alpha)
    cx = int(cell_w * 0.50)
    cy = int(cell_h * 3.44)
    rx = int(cell_w * 0.185)
    ry = int(cell_h * 0.245)
    draw.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=0)
    result.putalpha(alpha)
    return result


def _align_cells(image: Image.Image, columns: int, rows: int) -> Image.Image:
    """Centraliza cada desenho na horizontal e ancora todos no piso da célula."""
    width, height = image.size
    cell_w, cell_h = width // columns, height // rows
    cropped = image.crop((0, 0, cell_w * columns, cell_h * rows))
    aligned = Image.new("RGBA", cropped.size, (0, 0, 0, 0))
    floor_padding = max(6, int(cell_h * 0.055))
    for row in range(rows):
        for column in range(columns):
            cell = cropped.crop((column * cell_w, row * cell_h,
                                 (column + 1) * cell_w, (row + 1) * cell_h))
            alpha = cell.getchannel("A")
            bbox = alpha.getbbox()
            if not bbox:
                continue
            subject = cell.crop(bbox)
            x = column * cell_w + (cell_w - subject.width) // 2
            y = row * cell_h + cell_h - floor_padding - subject.height
            y = max(row * cell_h + 3, y)
            aligned.alpha_composite(subject, (x, y))
    return aligned


def prepare_grid(
    source: str,
    destination: str,
    columns: int,
    rows: int,
    mode: str,
    *,
    align: bool = True,
) -> None:
    image = Image.open(_source_path(source))
    if mode == "checker":
        image = _checker_alpha(image)
    elif mode == "checker-global":
        image = _checker_alpha_global(image)
    elif mode == "object-edge":
        image = _edge_key_alpha(image, columns, rows, fill_subject=True)
    elif mode == "effect-edge":
        image = _edge_key_alpha(image, columns, rows, fill_subject=False)
    else:
        image = image.convert("RGBA")
    if destination == "mission_atlas_unified.png":
        image = _clear_portal_window(image)
    if align:
        image = _align_cells(image, columns, rows)
    image.save(TEXTURES / destination, optimize=True)


def make_tileable(source: str, destination: str) -> None:
    image = Image.open(_source_path(source)).convert("RGB")
    width, height = image.size
    tiled = Image.new("RGB", (width * 2, height * 2))
    for x in (0, width):
        for y in (0, height):
            tiled.paste(image, (x, y))
    centered = tiled.crop((width // 2, height // 2, width // 2 + width, height // 2 + height))
    softened = centered.filter(ImageFilter.GaussianBlur(radius=max(2, width // 220)))
    mask = Image.new("L", centered.size, 0)
    draw = ImageDraw.Draw(mask)
    band = max(24, width // 28)
    draw.rectangle((width // 2 - band, 0, width // 2 + band, height), fill=210)
    draw.rectangle((0, height // 2 - band, width, height // 2 + band), fill=210)
    mask = mask.filter(ImageFilter.GaussianBlur(radius=band // 2))
    Image.composite(softened, centered, mask).save(TEXTURES / destination, optimize=True)


def generate_ui_frame() -> None:
    size = 96
    frame = Image.new("RGBA", (size, size), (18, 26, 33, 232))
    draw = ImageDraw.Draw(frame)
    border = (82, 96, 107, 185)
    cyan = (113, 211, 223, 225)
    for offset in (0, 1):
        draw.rectangle((8 + offset, 8 + offset, 87 - offset, 87 - offset), outline=border, width=1)
    length = 22
    for x, y, sx, sy in ((8, 8, 1, 1), (87, 8, -1, 1), (8, 87, 1, -1), (87, 87, -1, -1)):
        draw.line((x, y, x + sx * length, y), fill=border, width=3)
        draw.line((x, y, x, y + sy * length), fill=border, width=3)
    draw.line((12, 8, 32, 8), fill=cyan, width=3)
    draw.arc((61, 58, 87, 84), 190, 275, fill=(113, 211, 223, 110), width=2)
    frame.save(TEXTURES / "ui_panel_frame.png", optimize=True)

def main() -> None:
    prepare_grid("lunar_enemy_sheet.png", "lunar_enemy_sheet.png", 4, 4, "none")
    prepare_grid("mars_drone_sheet.png", "mars_drone_sheet.png", 4, 4, "none")
    prepare_grid("mars_crawler_sheet_candidate.png", "mars_crawler_sheet.png", 4, 4, "checker")
    prepare_grid("lunar_obstacles_candidate.png", "lunar_obstacles.png", 3, 2, "checker")
    prepare_grid("mars_obstacles_candidate.png", "mars_obstacles.png", 3, 2, "checker-global")
    prepare_grid("mission_atlas_candidate.png", "mission_atlas_unified.png", 4, 4, "checker")
    prepare_grid("action_fx_candidate.png", "action_fx_sheet.png", 6, 4, "checker-global", align=False)
    prepare_grid("energy_fx_candidate.png", "energy_fx_sheet.png", 6, 4, "checker-global", align=False)
    prepare_grid("landmarks_candidate.png", "landmarks.png", 4, 2, "checker")
    make_tileable("lunar_ground_candidate.png", "lunar_ground.png")
    make_tileable("mars_ground_candidate.png", "mars_ground.png")
    generate_ui_frame()


if __name__ == "__main__":
    main()
