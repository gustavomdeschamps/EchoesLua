"""Normaliza assets raster de Echoes sem alterar os arquivos-fonte.

Uso:
  python tools/prepare_visual_assets.py

As entradas *_candidate.png permanecem como fonte. As saídas são os assets
consumidos pelo jogo, com alpha real, grids exatos e terrenos tileáveis.
"""

from __future__ import annotations

import argparse
from collections import deque
from pathlib import Path
import re
from statistics import median

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter, ImageOps, ImageStat


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "assets" / "textures"
SOURCES = ROOT / "tools" / "source_assets"
ART_BIBLE = ROOT / "docs" / "ART_BIBLE.md"
UI_THEME = ROOT / "core" / "src" / "main" / "java" / "com" / "orion" / "echoes" / "lua" / "ui" / "UiTheme.java"


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


def _shaded_checker_alpha(image: Image.Image) -> Image.Image:
    """Remove checker e a sombra projetada sobre ele sem apagar metal claro.

    A remoção parte apenas das bordas. Tons neutros conectados ao fundo são
    descartados mesmo escurecidos pela sombra; áreas claras cercadas pelo
    contorno escuro do próprio sprite continuam intactas.
    """
    rgba = image.convert("RGBA")
    width, height = rgba.size
    pixels = rgba.load()
    seen = bytearray(width * height)
    queue: deque[tuple[int, int]] = deque()

    def background(x: int, y: int) -> bool:
        r, g, b, _ = pixels[x, y]
        return max(r, g, b) - min(r, g, b) <= 24 and min(r, g, b) >= 58

    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
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
                detail = ImageEnhance.Contrast(detail).enhance(2.5)
                alpha = detail.point(lambda value: max(0, min(255, (value - 18) * 8)))
                alpha = alpha.filter(ImageFilter.MaxFilter(3))
                alpha = alpha.filter(ImageFilter.GaussianBlur(radius=1.15))
                alpha = alpha.point(lambda value: 0 if value < 10 else value)

            if fill_subject:
                keyed = cell.copy()
            else:
                # O atlas é renderizado de modo aditivo. Subtrair o gradiente
                # do RGB impede qualquer retângulo escuro mesmo no halo suave.
                keyed_rgb = ImageEnhance.Brightness(
                    ImageChops.subtract(rgb, background, scale=0.58)
                ).enhance(1.35)
                keyed = keyed_rgb.convert("RGBA")
                emission_channels = keyed_rgb.split()
                emission = ImageChops.lighter(
                    emission_channels[0],
                    ImageChops.lighter(emission_channels[1], emission_channels[2]))
                emission_alpha = emission.point(
                    lambda value: max(0, min(255, (value - 24) * 7)))
                emission_alpha = emission_alpha.filter(ImageFilter.MaxFilter(3))
                emission_alpha = emission_alpha.filter(ImageFilter.GaussianBlur(radius=1.0))
                alpha = ImageChops.darker(alpha, emission_alpha)
            keyed.putalpha(alpha)
            output.alpha_composite(keyed, (column * cell_w, row * cell_h))
    return output


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


def _remove_alpha_islands(image: Image.Image, minimum_area: int = 16) -> Image.Image:
    """Remove resíduos desconectados sem corroer o contorno principal.

    A busca usa apenas pixels com alpha visível e apaga componentes menores que
    o limiar. Isso elimina pontos gerados pelo recorte sem alterar massas grandes
    como sombra de contato, cabelo ou acessórios.
    """
    result = image.convert("RGBA")
    alpha = result.getchannel("A")
    pixels = alpha.load()
    width, height = alpha.size
    visited = bytearray(width * height)

    for start_y in range(height):
        for start_x in range(width):
            start = start_y * width + start_x
            if visited[start] or pixels[start_x, start_y] < 20:
                continue
            queue: deque[tuple[int, int]] = deque([(start_x, start_y)])
            component: list[tuple[int, int]] = []
            visited[start] = 1
            while queue:
                x, y = queue.popleft()
                component.append((x, y))
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if nx < 0 or ny < 0 or nx >= width or ny >= height:
                        continue
                    index = ny * width + nx
                    if visited[index] or pixels[nx, ny] < 20:
                        continue
                    visited[index] = 1
                    queue.append((nx, ny))
            if len(component) < minimum_area:
                for x, y in component:
                    pixels[x, y] = 0
    result.putalpha(alpha)
    return result


def _clear_cell_edge_fragments(
    image: Image.Image, columns: int, rows: int, edge_ratio: float = 0.14
) -> Image.Image:
    """Descarta pedaços de uma pose vizinha que invadiram a célula gerada."""
    result = image.convert("RGBA")
    alpha = result.getchannel("A")
    pixels = alpha.load()
    cell_w, cell_h = result.width // columns, result.height // rows
    for row in range(rows):
        for column in range(columns):
            left, top = column * cell_w, row * cell_h
            visited: set[tuple[int, int]] = set()
            components: list[list[tuple[int, int]]] = []
            for local_y in range(cell_h):
                for local_x in range(cell_w):
                    point = (local_x, local_y)
                    if point in visited or pixels[left + local_x, top + local_y] < 20:
                        continue
                    queue: deque[tuple[int, int]] = deque([point])
                    visited.add(point)
                    component: list[tuple[int, int]] = []
                    while queue:
                        x, y = queue.popleft()
                        component.append((x, y))
                        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                            neighbour = (nx, ny)
                            if (nx < 0 or ny < 0 or nx >= cell_w or ny >= cell_h
                                    or neighbour in visited
                                    or pixels[left + nx, top + ny] < 20):
                                continue
                            visited.add(neighbour)
                            queue.append(neighbour)
                    components.append(component)
            if not components:
                continue
            largest = max(components, key=len)
            for component in components:
                center_x = sum(x for x, _ in component) / len(component)
                is_edge_fragment = (center_x < cell_w * edge_ratio
                                    or center_x > cell_w * (1.0 - edge_ratio))
                if component is not largest and (len(component) < 16 or is_edge_fragment):
                    for x, y in component:
                        pixels[left + x, top + y] = 0
    result.putalpha(alpha)
    return result


def _clear_cell_guard_band(image: Image.Image, columns: int, rows: int,
                           ratio: float = 0.018) -> Image.Image:
    """Cria separação real entre células antes da análise de componentes."""
    result = image.convert("RGBA")
    alpha = result.getchannel("A")
    draw = ImageDraw.Draw(alpha)
    cell_w, cell_h = result.width // columns, result.height // rows
    guard_x = max(2, int(cell_w * ratio))
    guard_y = max(2, int(cell_h * ratio))
    for row in range(rows):
        for column in range(columns):
            x0, y0 = column * cell_w, row * cell_h
            x1, y1 = x0 + cell_w - 1, y0 + cell_h - 1
            draw.rectangle((x0, y0, x1, y0 + guard_y), fill=0)
            draw.rectangle((x0, y1 - guard_y, x1, y1), fill=0)
            draw.rectangle((x0, y0, x0 + guard_x, y1), fill=0)
            draw.rectangle((x1 - guard_x, y0, x1, y1), fill=0)
    result.putalpha(alpha)
    return result


def _isolate_cell_subjects(
    image: Image.Image, columns: int, rows: int, *, guard_ratio: float = 0.03
) -> Image.Image:
    """Mantém o objeto de cada célula e seus detalhes próximos.

    Geradores raster às vezes deixam a ponta do objeto vizinho atravessar a
    grade. Limpar apenas a borda corta essa ponta, mas ainda deixa uma meia-lua
    solta dentro da célula. Aqui cada célula é analisada isoladamente: a maior
    massa define o objeto principal e sombras, cabos, ondas e detritos próximos
    continuam presentes; componentes distantes, junto da grade, são removidos.
    """
    source = image.convert("RGBA")
    cell_w, cell_h = source.width // columns, source.height // rows
    source = source.crop((0, 0, cell_w * columns, cell_h * rows))
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    minimum_area = max(12, int(cell_w * cell_h * 0.00015))

    for row in range(rows):
        for column in range(columns):
            cell = source.crop((column * cell_w, row * cell_h,
                                (column + 1) * cell_w, (row + 1) * cell_h))
            alpha = cell.getchannel("A")
            draw = ImageDraw.Draw(alpha)
            guard_x = max(3, int(cell_w * guard_ratio))
            guard_y = max(3, int(cell_h * guard_ratio))
            draw.rectangle((0, 0, cell_w - 1, guard_y), fill=0)
            draw.rectangle((0, cell_h - 1 - guard_y, cell_w - 1, cell_h - 1), fill=0)
            draw.rectangle((0, 0, guard_x, cell_h - 1), fill=0)
            draw.rectangle((cell_w - 1 - guard_x, 0, cell_w - 1, cell_h - 1), fill=0)
            cell.putalpha(alpha)

            pixels = alpha.load()
            visited = bytearray(cell_w * cell_h)
            components: list[dict[str, object]] = []
            for start_y in range(cell_h):
                for start_x in range(cell_w):
                    index = start_y * cell_w + start_x
                    if visited[index] or pixels[start_x, start_y] < 20:
                        continue
                    queue: deque[tuple[int, int]] = deque([(start_x, start_y)])
                    visited[index] = 1
                    points: list[tuple[int, int]] = []
                    min_x = max_x = start_x
                    min_y = max_y = start_y
                    while queue:
                        x, y = queue.popleft()
                        points.append((x, y))
                        min_x, max_x = min(min_x, x), max(max_x, x)
                        min_y, max_y = min(min_y, y), max(max_y, y)
                        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                            if nx < 0 or ny < 0 or nx >= cell_w or ny >= cell_h:
                                continue
                            neighbour = ny * cell_w + nx
                            if visited[neighbour] or pixels[nx, ny] < 20:
                                continue
                            visited[neighbour] = 1
                            queue.append((nx, ny))
                    components.append({"points": points, "bbox": (min_x, min_y, max_x + 1, max_y + 1)})

            if not components:
                continue
            main = max(components, key=lambda component: len(component["points"]))
            main_box = main["bbox"]
            proximity_x = int(cell_w * 0.15)
            proximity_y = int(cell_h * 0.15)
            expanded = (main_box[0] - proximity_x, main_box[1] - proximity_y,
                        main_box[2] + proximity_x, main_box[3] + proximity_y)
            keep = Image.new("L", (cell_w, cell_h), 0)
            keep_pixels = keep.load()
            for component in components:
                points = component["points"]
                box = component["bbox"]
                overlaps_main_neighbourhood = not (
                    box[2] < expanded[0] or box[0] > expanded[2]
                    or box[3] < expanded[1] or box[1] > expanded[3]
                )
                near_cell_edge = (box[0] < cell_w * 0.11 or box[2] > cell_w * 0.89
                                  or box[1] < cell_h * 0.11 or box[3] > cell_h * 0.89)
                looks_like_clipped_neighbour = (
                    component is not main and near_cell_edge
                    and len(points) < len(main["points"]) * 0.08
                )
                if (component is main or (len(points) >= minimum_area
                                           and overlaps_main_neighbourhood)) \
                        and not looks_like_clipped_neighbour:
                    for x, y in points:
                        keep_pixels[x, y] = pixels[x, y]
            # Recupera antialias imediatamente ligado ao recorte conservado.
            keep = keep.filter(ImageFilter.MaxFilter(3))
            final_alpha = ImageChops.darker(alpha, keep)
            cell.putalpha(final_alpha)
            output.alpha_composite(cell, (column * cell_w, row * cell_h))
    return output


def _pad_cells(image: Image.Image, columns: int, rows: int, scale: float) -> Image.Image:
    """Cria respiro entre quadros sem alterar a escala relativa da animação."""
    source = image.convert("RGBA")
    cell_w, cell_h = source.width // columns, source.height // rows
    source = source.crop((0, 0, cell_w * columns, cell_h * rows))
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    target_size = (max(1, int(cell_w * scale)), max(1, int(cell_h * scale)))
    for row in range(rows):
        for column in range(columns):
            cell = source.crop((column * cell_w, row * cell_h,
                                (column + 1) * cell_w, (row + 1) * cell_h))
            cell = cell.resize(target_size, Image.Resampling.LANCZOS)
            x = column * cell_w + (cell_w - target_size[0]) // 2
            y = row * cell_h + (cell_h - target_size[1]) // 2
            output.alpha_composite(cell, (x, y))
    return output


def prepare_character_sheet(
    source: str,
    destination: str,
    columns: int,
    rows: int,
    output_size: tuple[int, int],
    cell_padding_scale: float | None = None,
    isolate_cells: bool = False,
) -> None:
    image = Image.open(_source_path(source))
    width = image.width - image.width % columns
    height = image.height - image.height % rows
    image = image.crop((0, 0, width, height))
    image = _shaded_checker_alpha(image) if isolate_cells else _checker_alpha(image)
    if isolate_cells:
        image = _isolate_cell_subjects(image, columns, rows, guard_ratio=0.045)
    else:
        image = _clear_cell_edge_fragments(image, columns, rows)
    image = _align_cells(image, columns, rows)
    image = _remove_alpha_islands(image)
    if cell_padding_scale:
        image = _pad_cells(image, columns, rows, cell_padding_scale)
    if image.size != output_size:
        image = image.resize(output_size, Image.Resampling.LANCZOS)
    image.save(TEXTURES / destination, optimize=True)


def prepare_grid(
    source: str,
    destination: str,
    columns: int,
    rows: int,
    mode: str,
    *,
    align: bool = True,
    clean_islands: bool = False,
    output_size: tuple[int, int] | None = None,
    clean_edge_fragments: bool = False,
    normalize_cell_scale: bool = False,
    edge_guard_ratio: float = 0.03,
    cell_padding_scale: float | None = None,
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
    if clean_edge_fragments:
        image = _isolate_cell_subjects(image, columns, rows,
                                       guard_ratio=edge_guard_ratio)
    if align:
        image = _align_cells(image, columns, rows)
    if normalize_cell_scale:
        cell_w, cell_h = image.width // columns, image.height // rows
        normalized = Image.new("RGBA", (cell_w * columns, cell_h * rows), (0, 0, 0, 0))
        for row in range(rows):
            for column in range(columns):
                cell = image.crop((column * cell_w, row * cell_h,
                                   (column + 1) * cell_w, (row + 1) * cell_h))
                normalized.alpha_composite(_fit_cell(cell, (cell_w, cell_h), 0.09),
                                           (column * cell_w, row * cell_h))
        image = normalized
    if cell_padding_scale:
        image = _pad_cells(image, columns, rows, cell_padding_scale)
    if clean_islands:
        image = _remove_alpha_islands(image)
    if output_size and image.size != output_size:
        image = image.resize(output_size, Image.Resampling.LANCZOS)
    image.save(TEXTURES / destination, optimize=True)


def _fit_cell(source: Image.Image, size: tuple[int, int], margin: float = 0.08) -> Image.Image:
    source = source.convert("RGBA")
    bbox = source.getchannel("A").getbbox()
    output = Image.new("RGBA", size, (0, 0, 0, 0))
    if not bbox:
        return output
    subject = source.crop(bbox)
    max_w = int(size[0] * (1 - margin * 2))
    max_h = int(size[1] * (1 - margin * 2))
    scale = min(max_w / subject.width, max_h / subject.height)
    subject = subject.resize((max(1, int(subject.width * scale)),
                              max(1, int(subject.height * scale))), Image.Resampling.LANCZOS)
    x = (size[0] - subject.width) // 2
    y = size[1] - int(size[1] * margin) - subject.height
    output.alpha_composite(subject, (x, y))
    return output


def extract_common_assets() -> None:
    image = Image.open(_source_path("common_assets_realistic_candidate.png"))
    image = _checker_alpha_global(image)
    image = _isolate_cell_subjects(image, 3, 2, guard_ratio=0.075)
    cell_w, cell_h = image.width // 3, image.height // 2
    exports = (
        (0, "base_lunar.png", (768, 640)),
        (1, "oxigenio.png", (512, 512)),
        (2, "comida.png", (512, 512)),
        (3, "gelo.png", (512, 512)),
        (4, "pulse_rifle.png", (768, 512)),
    )
    for index, name, size in exports:
        column, row = index % 3, index // 3
        cell = image.crop((column * cell_w, row * cell_h,
                           (column + 1) * cell_w, (row + 1) * cell_h))
        _fit_cell(_remove_alpha_islands(cell), size).save(TEXTURES / name, optimize=True)


def split_biome_obstacles() -> None:
    image = Image.open(_source_path("obstacles_realistic_candidate.png"))
    image = _checker_alpha_global(image)
    image = _isolate_cell_subjects(image, 6, 2, guard_ratio=0.075)
    cell_w, cell_h = image.width // 6, image.height // 2
    for source_row, destination in ((0, "lunar_obstacles.png"), (1, "mars_obstacles.png")):
        output = Image.new("RGBA", (939, 626), (0, 0, 0, 0))
        for index in range(6):
            cell = image.crop((index * cell_w, source_row * cell_h,
                               (index + 1) * cell_w, (source_row + 1) * cell_h))
            fitted = _fit_cell(_remove_alpha_islands(cell), (313, 313), 0.06)
            output.alpha_composite(fitted, ((index % 3) * 313, (index // 3) * 313))
        output.save(TEXTURES / destination, optimize=True)


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
    result = Image.composite(softened, centered, mask)
    pixels = result.load()
    # Fecha as bordas correspondentes com um blend curto. O primeiro/último
    # pixel ficam idênticos e a correção desaparece antes de alcançar o jogo.
    edge_band = max(12, width // 80)
    for offset in range(edge_band):
        weight = 1.0 - offset / edge_band
        for x in range(width):
            top, bottom = pixels[x, offset], pixels[x, height - 1 - offset]
            average = tuple((top[channel] + bottom[channel]) // 2 for channel in range(3))
            pixels[x, offset] = tuple(int(top[c] * (1 - weight) + average[c] * weight) for c in range(3))
            pixels[x, height - 1 - offset] = tuple(int(bottom[c] * (1 - weight) + average[c] * weight) for c in range(3))
        for y in range(height):
            left, right = pixels[offset, y], pixels[width - 1 - offset, y]
            average = tuple((left[channel] + right[channel]) // 2 for channel in range(3))
            pixels[offset, y] = tuple(int(left[c] * (1 - weight) + average[c] * weight) for c in range(3))
            pixels[width - 1 - offset, y] = tuple(int(right[c] * (1 - weight) + average[c] * weight) for c in range(3))
    result.save(TEXTURES / destination, optimize=True)


def generate_ui_kit() -> None:
    """Kit raster autoral do visor: painéis, estados, barras, ícones e vinheta."""
    ui_dir = TEXTURES / "ui"
    ui_dir.mkdir(parents=True, exist_ok=True)
    palette = {
        "hud": ((10, 19, 27, 232), (45, 139, 208, 210)),
        "dialog": ((17, 25, 31, 242), (103, 184, 121, 210)),
        "modal": ((19, 24, 29, 250), (229, 164, 58, 225)),
    }
    for name, (fill, accent) in palette.items():
        panel = Image.new("RGBA", (96, 96), fill)
        draw = ImageDraw.Draw(panel)
        draw.rounded_rectangle((4, 4, 91, 91), radius=13, outline=(77, 95, 108, 190), width=2)
        draw.line((15, 6, 45, 6), fill=accent, width=3)
        draw.line((6, 15, 6, 31), fill=accent, width=3)
        draw.line((66, 90, 82, 90), fill=accent, width=2)
        draw.arc((66, 66, 88, 88), 15, 105, fill=accent, width=2)
        panel.save(ui_dir / f"panel_{name}.png", optimize=True)

    states = ((17, 28, 37, 242), (24, 49, 64, 248), (12, 34, 47, 255), (24, 28, 31, 170))
    accents = ((66, 104, 128, 210), (45, 139, 208, 255), (229, 164, 58, 255), (77, 85, 91, 130))
    for name, fill, accent in zip(("normal", "hover", "pressed", "disabled"), states, accents):
        button = Image.new("RGBA", (192, 64), fill)
        draw = ImageDraw.Draw(button)
        draw.rounded_rectangle((2, 2, 189, 61), radius=11, outline=accent, width=2)
        draw.line((15, 5, 65, 5), fill=accent, width=3)
        draw.polygon(((176, 25), (182, 31), (176, 37)), fill=accent)
        if name == "pressed":
            draw.rectangle((8, 54, 184, 58), fill=accent)
        button.save(ui_dir / f"button_{name}.png", optimize=True)

    track = Image.new("RGBA", (256, 24), (6, 12, 17, 225))
    draw = ImageDraw.Draw(track)
    draw.rounded_rectangle((1, 1, 254, 22), radius=8, outline=(73, 91, 103, 220), width=2)
    draw.line((12, 5, 244, 5), fill=(255, 255, 255, 22), width=1)
    track.save(ui_dir / "bar_track.png", optimize=True)
    fill = Image.new("RGBA", (256, 24), (45, 139, 208, 255))
    draw = ImageDraw.Draw(fill)
    draw.rounded_rectangle((0, 0, 255, 23), radius=8, fill=(45, 139, 208, 255))
    draw.line((11, 5, 245, 5), fill=(172, 229, 255, 175), width=3)
    fill.save(ui_dir / "bar_fill.png", optimize=True)

    icons = Image.new("RGBA", (256, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icons)
    colors = ((45, 139, 208, 255), (229, 164, 58, 255), (103, 184, 121, 255), (201, 78, 85, 255))
    for index, color in enumerate(colors):
        cx = index * 64 + 32
        draw.ellipse((cx - 22, 10, cx + 22, 54), outline=(202, 216, 224, 230), width=3)
        if index == 0:
            draw.arc((cx - 13, 18, cx + 13, 47), 195, 345, fill=color, width=6)
        elif index == 1:
            draw.polygon(((cx + 5, 12), (cx - 12, 34), (cx, 34), (cx - 6, 53), (cx + 15, 27), (cx + 2, 27)), fill=color)
        elif index == 2:
            draw.polygon(((cx, 13), (cx + 17, 29), (cx + 10, 50), (cx - 10, 50), (cx - 17, 29)), fill=color)
        else:
            draw.line((cx - 13, 32, cx + 13, 32), fill=color, width=7)
            draw.line((cx, 19, cx, 45), fill=color, width=7)
    icons.save(ui_dir / "resource_icons.png", optimize=True)

    # Marcador de objetivo: seta apontando para fora da borda da tela.
    marker = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(marker)
    draw.polygon(((58, 32), (22, 10), (30, 32), (22, 54)), fill=(45, 139, 208, 245))
    draw.polygon(((58, 32), (22, 10), (30, 32), (22, 54)), outline=(202, 232, 255, 235))
    draw.ellipse((4, 26, 16, 38), fill=(229, 164, 58, 235))
    marker.save(ui_dir / "objective_marker.png", optimize=True)

    # Cursor autoral: forma neutra fora de alvo, retículo sobre interagível.
    for name, accent in (("cursor_default", (202, 216, 224, 240)),
                         ("cursor_target", (229, 164, 58, 250))):
        cursor = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        draw = ImageDraw.Draw(cursor)
        draw.ellipse((18, 18, 45, 45), outline=accent, width=3)
        for x0, y0, x1, y1 in ((32, 4, 32, 15), (32, 48, 32, 59),
                               (4, 32, 15, 32), (48, 32, 59, 32)):
            draw.line((x0, y0, x1, y1), fill=accent, width=3)
        if name == "cursor_target":
            draw.ellipse((28, 28, 35, 35), fill=accent)
        cursor.save(ui_dir / f"{name}.png", optimize=True)

    vignette = Image.new("RGBA", (512, 288), (0, 0, 0, 0))
    pixels = vignette.load()
    for y in range(vignette.height):
        for x in range(vignette.width):
            nx = abs(x / (vignette.width - 1) * 2 - 1)
            ny = abs(y / (vignette.height - 1) * 2 - 1)
            edge = max(nx ** 3.2, ny ** 3.2)
            pixels[x, y] = (72, 6, 8, int(205 * edge))
    vignette.save(ui_dir / "damage_vignette.png", optimize=True)
    Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(ui_dir / "white_pixel.png")

    # Compatibilidade temporária para chamadas antigas de uiPanelPatch().
    (ui_dir / "panel_hud.png").replace(TEXTURES / "ui_panel_frame.png")
    Image.open(TEXTURES / "ui_panel_frame.png").save(ui_dir / "panel_hud.png")


def normalize_sheet_dimension(source: str, destination: str, size: int) -> None:
    image = Image.open(_source_path(source)).convert("RGBA")
    if image.size != (size, size):
        image = image.resize((size, size), Image.Resampling.LANCZOS)
    image.save(TEXTURES / destination, optimize=True)


SHEET_GRIDS = {
    "astronauta_sheet.png": (4, 4),
    "astronaut_combat_sheet.png": (4, 3),
    "mission_atlas_unified.png": (4, 4),
    "lunar_enemy_sheet.png": (4, 4),
    "mars_drone_sheet.png": (4, 4),
    "mars_crawler_sheet.png": (4, 4),
    "titan_enemy_sheet.png": (4, 4),
    "lunar_obstacles.png": (3, 2),
    "mars_obstacles.png": (3, 2),
    "action_fx_sheet.png": (6, 4),
    "energy_fx_sheet.png": (6, 4),
    "landmarks.png": (4, 2),
    "mars_atlas_v4.png": (4, 3),
    "titan_portal_sheet.png": (2, 1),
}


def validate_sheet_grids() -> None:
    errors: list[str] = []
    for name, (columns, rows) in SHEET_GRIDS.items():
        path = TEXTURES / name
        if not path.exists():
            errors.append(f"asset ausente: {name}")
            continue
        with Image.open(path) as image:
            width, height = image.size
        if width % columns != 0 or height % rows != 0:
            errors.append(
                f"{name}: {width}x{height} não divide a grade {columns}x{rows}"
            )
    if errors:
        raise SystemExit("QA de spritesheets falhou:\n- " + "\n- ".join(errors))


def validate_character_motion(minimum_mean_difference: float = 5.0) -> None:
    """Impede que uma ação volte a ser formada por poses quase duplicadas."""
    errors: list[str] = []
    for name, (columns, rows) in {
        "astronauta_sheet.png": (4, 4),
        "astronaut_combat_sheet.png": (4, 3),
    }.items():
        image = Image.open(TEXTURES / name).convert("RGBA")
        cell_w, cell_h = image.width // columns, image.height // rows
        for row in range(rows):
            frames = [image.crop((column * cell_w, row * cell_h,
                                  (column + 1) * cell_w, (row + 1) * cell_h))
                      for column in range(columns)]
            for column in range(columns - 1):
                difference = ImageChops.difference(frames[column], frames[column + 1])
                mean = sum(ImageStat.Stat(difference).mean) / 4.0
                if mean < minimum_mean_difference:
                    errors.append(
                        f"{name} linha {row + 1}, quadros {column + 1}/{column + 2}: "
                        f"diferença média {mean:.2f} abaixo de {minimum_mean_difference:.2f}"
                    )
    if errors:
        raise SystemExit("QA de movimento falhou:\n- " + "\n- ".join(errors))


def validate_palette_sync() -> None:
    bible = ART_BIBLE.read_text(encoding="utf-8").upper()
    theme = UI_THEME.read_text(encoding="utf-8")
    missing = []
    for encoded in re.findall(r'Color\.valueOf\("([0-9A-Fa-f]{6,8})"\)', theme):
        rgb = encoded[:6].upper()
        if f"#{rgb}" not in bible:
            missing.append(rgb)
    if missing:
        raise SystemExit("QA de paleta falhou; cores ausentes da ART_BIBLE: "
                         + ", ".join(sorted(set(missing))))


def validate_terrain_seams(maximum_mean_difference: float = 4.0) -> None:
    errors: list[str] = []
    for name in ("lunar_ground.png", "mars_ground.png", "titan_ground.png"):
        image = Image.open(TEXTURES / name).convert("RGB")
        left_right = ImageChops.difference(
            image.crop((0, 0, 1, image.height)),
            image.crop((image.width - 1, 0, image.width, image.height)))
        top_bottom = ImageChops.difference(
            image.crop((0, 0, image.width, 1)),
            image.crop((0, image.height - 1, image.width, image.height)))
        for edge, difference in (("horizontal", left_right), ("vertical", top_bottom)):
            mean = sum(ImageStat.Stat(difference).mean) / 3.0
            if mean > maximum_mean_difference:
                errors.append(f"{name}: costura {edge} com diferença média {mean:.2f}")
    if errors:
        raise SystemExit("QA de terreno falhou:\n- " + "\n- ".join(errors))


def validate_cell_scale(maximum_deviation: float = 0.55) -> None:
    errors: list[str] = []
    for name in ("mission_atlas_unified.png", "landmarks.png",
                 "lunar_obstacles.png", "mars_obstacles.png"):
        columns, rows = SHEET_GRIDS[name]
        image = Image.open(TEXTURES / name).convert("RGBA")
        cell_w, cell_h = image.width // columns, image.height // rows
        areas: list[float] = []
        for row in range(rows):
            for column in range(columns):
                cell = image.crop((column * cell_w, row * cell_h,
                                   (column + 1) * cell_w, (row + 1) * cell_h))
                bbox = cell.getchannel("A").getbbox()
                if bbox:
                    areas.append(((bbox[2] - bbox[0]) * (bbox[3] - bbox[1]))
                                 / (cell_w * cell_h))
        typical = median(areas)
        for index, area in enumerate(areas):
            deviation = abs(area - typical) / typical
            if deviation > maximum_deviation:
                errors.append(f"{name} célula {index + 1}: escala diverge {deviation:.0%}")
    if errors:
        raise SystemExit("QA de escala visual falhou:\n- " + "\n- ".join(errors))


def validate_cell_borders(maximum_visible_pixels: int = 0) -> None:
    """Garante que nenhum desenho atravesse o limite de uma célula do atlas."""
    errors: list[str] = []
    for name, (columns, rows) in SHEET_GRIDS.items():
        if name.startswith("astronaut"):
            continue
        image = Image.open(TEXTURES / name).convert("RGBA")
        alpha = image.getchannel("A")
        cell_w, cell_h = image.width // columns, image.height // rows
        band = max(2, min(cell_w, cell_h) // 80)
        for row in range(rows):
            for column in range(columns):
                cell = alpha.crop((column * cell_w, row * cell_h,
                                   (column + 1) * cell_w, (row + 1) * cell_h))
                border = Image.new("L", cell.size, 0)
                border.paste(cell.crop((0, 0, cell_w, band)), (0, 0))
                border.paste(cell.crop((0, cell_h - band, cell_w, cell_h)),
                             (0, cell_h - band))
                border.paste(cell.crop((0, 0, band, cell_h)), (0, 0))
                border.paste(cell.crop((cell_w - band, 0, cell_w, cell_h)),
                             (cell_w - band, 0))
                visible = sum(border.histogram()[20:])
                if visible > maximum_visible_pixels:
                    errors.append(f"{name} célula {row * columns + column + 1}: "
                                  f"{visible} pixels invadem a borda")
    if errors:
        raise SystemExit("QA de isolamento das células falhou:\n- " + "\n- ".join(errors))


def validate_all() -> None:
    validate_sheet_grids()
    validate_character_motion()
    validate_palette_sync()
    validate_terrain_seams()
    validate_cell_scale()
    validate_cell_borders()

def main() -> None:
    prepare_character_sheet("astronaut_movement_candidate.png", "astronauta_sheet.png",
                            4, 4, (1252, 1252))
    prepare_character_sheet("astronaut_combat_candidate.png", "astronaut_combat_sheet.png",
                            4, 3, (1252, 939))
    prepare_character_sheet("lunar_enemy_realistic_candidate.png", "lunar_enemy_sheet.png",
                            4, 4, (1252, 1252), cell_padding_scale=0.86,
                            isolate_cells=True)
    prepare_character_sheet("mars_drone_realistic_candidate.png", "mars_drone_sheet.png",
                            4, 4, (1252, 1252), cell_padding_scale=0.86,
                            isolate_cells=True)
    prepare_character_sheet("mars_crawler_realistic_candidate.png", "mars_crawler_sheet.png",
                            4, 4, (1252, 1252), cell_padding_scale=0.86,
                            isolate_cells=True)
    prepare_character_sheet("titan_enemy_candidate.png", "titan_enemy_sheet.png",
                            4, 4, (1252, 1252), cell_padding_scale=0.86,
                            isolate_cells=True)
    split_biome_obstacles()
    prepare_grid("mission_atlas_realistic_candidate.png", "mission_atlas_unified.png",
                 4, 4, "checker-global", clean_islands=True, output_size=(1252, 1252),
                 clean_edge_fragments=True, normalize_cell_scale=True,
                 edge_guard_ratio=0.075)
    prepare_grid("mars_atlas_realistic_candidate.png", "mars_atlas_v4.png",
                 4, 3, "checker-global", clean_islands=True, output_size=(1252, 939),
                 clean_edge_fragments=True, edge_guard_ratio=0.075)
    prepare_grid("action_fx_realistic_candidate.png", "action_fx_sheet.png", 6, 4,
                 "checker-global", align=False, clean_islands=True,
                 output_size=(1536, 1024), cell_padding_scale=0.90)
    prepare_grid("energy_fx_realistic_candidate.png", "energy_fx_sheet.png", 6, 4,
                 "effect-edge", align=False, clean_islands=True,
                 output_size=(1536, 1024), cell_padding_scale=0.86)
    prepare_grid("landmarks_realistic_candidate.png", "landmarks.png", 4, 2,
                 "checker-global", clean_islands=True, output_size=(1252, 626),
                 clean_edge_fragments=True, edge_guard_ratio=0.075)
    prepare_grid("titan_portal_candidate.png", "titan_portal_sheet.png", 2, 1,
                 "checker-global", clean_islands=True, output_size=(1024, 512),
                 clean_edge_fragments=True, normalize_cell_scale=True,
                 edge_guard_ratio=0.06)
    extract_common_assets()
    make_tileable("lunar_ground_realistic_candidate.png", "lunar_ground.png")
    make_tileable("mars_ground_realistic_candidate.png", "mars_ground.png")
    make_tileable("titan_ground_candidate.png", "titan_ground.png")
    generate_ui_kit()
    validate_all()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--validate-only", action="store_true")
    args = parser.parse_args()
    if args.validate_only:
        validate_all()
    else:
        main()
