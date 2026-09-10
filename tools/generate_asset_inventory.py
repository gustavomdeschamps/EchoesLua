# -*- coding: utf-8 -*-
"""Gera docs/INVENTARIO_ASSETS.md a partir do codigo e dos arquivos reais.

O documento de arte pede um inventario com caminho, ponto de carregamento,
ponto de desenho, grade, quadros, margem e uso real de cada asset. Escrito a
mão, um inventario desses envelhece na primeira renomeacao: passa a afirmar
coisas que o codigo nao faz mais, e o proprio documento avisa para nao usar
documentacao como substituto de implementacao.

Entao aqui ele e derivado. As tres fontes sao lidas de verdade:

- AssetManager, para descobrir de qual atlas cada textura sai, com que nome de
  regiao, e qual metodo publico entrega os quadros dela;
- o PNG em disco, para medir celula, quadros, margem livre e alpha;
- o resto de core/src/main, para descobrir quem chama cada metodo -- e um
  metodo sem chamador nenhum e um asset carregado que ninguem desenha.

A regra explicita do documento e que nome de arquivo nao decide se um asset
esta em uso; a busca por referencia decide. E isso que a ultima coluna faz.

Uso:
    python tools/generate_asset_inventory.py
"""

from __future__ import annotations

import re
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parent.parent
TEXTURES = REPO / "assets" / "textures"
MAIN = REPO / "core" / "src" / "main" / "java"
ASSET_MANAGER = MAIN / "com" / "orion" / "echoes" / "lua" / "managers" / "AssetManager.java"
OUTPUT = REPO / "docs" / "INVENTARIO_ASSETS.md"

ALPHA_VISIBLE = 12

# Grade declarada em cada chamada de gridRegion, por metodo publico.
GRID_CALL = re.compile(
    r"public\s+TextureRegion\s+(\w+)\s*\([^)]*\)\s*\{(.*?)\n    \}",
    re.DOTALL)
ATLAS_BIND = re.compile(r"(\w+)\s*=\s*(required|optional)\((\w+),\s*\"([^\"]+)\"\)")
LOADER_BIND = re.compile(r"(\w+)\s*=\s*loader\.(?:get|isLoaded)\((\w+)")
PATH_CONST = re.compile(r"(\w+)\s*=\s*\"(textures/[^\"]+)\"")
GRID_ARGS = re.compile(r"gridRegion\((\w+),\s*(\d+),\s*(\d+)")

CATEGORIES = [
    ("Personagem", ("astronaut", "pulse_rifle")),
    ("NPCs", ("npc_",)),
    ("Inimigos e chefe", ("enemy", "hunter", "boss", "drone", "crawler")),
    ("Estações e refinaria", ("station", "refinery", "base_lunar")),
    ("Portais", ("portal",)),
    ("Itens e missão", ("mission", "oxigenio", "comida", "gelo", "resource")),
    ("Obstáculos e terreno", ("obstacle", "formation", "ground", "landmark")),
    ("VFX", ("_fx",)),
    ("Interface", ("ui_", "panel", "button", "bar_", "vignette", "white", "marker")),
    ("Aberturas", ("intro",)),
]


def category(region: str) -> str:
    for name, needles in CATEGORIES:
        if any(needle in region for needle in needles):
            return name
    return "Outros"


def measure(path: Path, columns: int, rows: int):
    """Celula, quadros, margem livre minima e presenca de alpha real."""
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    if columns and rows and (width % columns == 0) and (height % rows == 0):
        cell_w, cell_h = width // columns, height // rows
    else:
        cell_w, cell_h = width, height
        columns = rows = 1
    alpha = np.asarray(image)[:, :, 3]
    real_alpha = bool(alpha.min() < 250)

    slack = 100.0
    for row in range(rows):
        for column in range(columns):
            cell = alpha[row * cell_h:(row + 1) * cell_h,
                         column * cell_w:(column + 1) * cell_w]
            visible = cell > ALPHA_VISIBLE
            if not visible.any():
                continue
            ys, xs = np.where(visible)
            free_v = min(ys.min(), cell_h - 1 - ys.max()) / float(cell_h)
            free_h = min(xs.min(), cell_w - 1 - xs.max()) / float(cell_w)
            slack = min(slack, 100.0 * min(free_v, free_h))
    return (width, height), (cell_w, cell_h), columns * rows, slack, real_alpha


def java_sources():
    return [p for p in MAIN.rglob("*.java") if p != ASSET_MANAGER]


def main() -> None:
    source = ASSET_MANAGER.read_text(encoding="utf-8")

    # textura -> (atlas ou loader, nome da regiao ou caminho)
    origin: dict[str, tuple[str, str]] = {}
    for match in ATLAS_BIND.finditer(source):
        field, kind, atlas, region = match.groups()
        origin[field] = (atlas.replace("Atlas", "") + (" (opcional)" if kind == "optional" else ""),
                         region)
    paths = dict(PATH_CONST.findall(source))
    for match in LOADER_BIND.finditer(source):
        field, const = match.groups()
        if field not in origin:
            origin[field] = ("arquivo solto", paths.get(const, const))

    # metodo publico -> (textura, colunas, linhas)
    accessors: dict[str, tuple[str, int, int]] = {}
    for match in GRID_CALL.finditer(source):
        method, body = match.groups()
        found = GRID_ARGS.search(body)
        if not found:
            continue
        texture = found.group(1)
        if texture not in origin:
            # gridRegion sobre parametro local; pega a primeira textura citada
            for field in origin:
                if field in body:
                    texture = field
                    break
        accessors[method] = (texture, int(found.group(2)), int(found.group(3)))

    bodies = {p: p.read_text(encoding="utf-8", errors="ignore") for p in java_sources()}

    def consumers(method: str) -> list[str]:
        needle = "." + method + "("
        return sorted({p.stem for p, text in bodies.items() if needle in text})

    grouped: dict[str, list[str]] = {}
    orphans: list[str] = []

    for method in sorted(accessors):
        texture, columns, rows = accessors[method]
        atlas, region = origin.get(texture, ("?", "?"))
        png = TEXTURES / (region + ".png")
        if not png.exists():
            png = TEXTURES / region
        used_by = consumers(method)

        if png.exists():
            size, cell, frames, slack, real_alpha = measure(png, columns, rows)
            geometry = "%dx%d" % size
            cell_text = "%dx%d" % cell
            slack_text = "%.0f%%" % slack
            alpha_text = "sim" if real_alpha else "**não**"
        else:
            geometry = cell_text = slack_text = alpha_text = "—"
            frames = columns * rows

        row = "| `%s` | %s | `%s` | %dx%d | %d | %s | %s | %s | %s |" % (
            region, atlas, method + "()", columns, rows, frames,
            cell_text, slack_text, alpha_text,
            ", ".join("`%s`" % c for c in used_by) if used_by else "**ninguém**")
        grouped.setdefault(category(region), []).append(row)
        if not used_by:
            orphans.append("%s (via %s)" % (region, method))

    lines = [
        "# Inventário de assets",
        "",
        "**Gerado por `tools/generate_asset_inventory.py`. Não edite à mão.**",
        "",
        "Cada linha é lida de três fontes reais: o `AssetManager`, para saber de",
        "qual atlas a textura sai e qual método entrega os quadros dela; o PNG em",
        "disco, para medir grade, célula, quadros e margem; e o resto de",
        "`core/src/main`, para descobrir quem chama aquele método.",
        "",
        "A última coluna é a que importa para decidir uso: o documento de arte diz",
        "que nome de arquivo não decide se um asset está em uso, e sim a busca por",
        "referência. Um método sem chamador é um asset que o jogo carrega e",
        "ninguém desenha.",
        "",
        "`Margem` é a **menor** folga transparente entre o conteúdo e a borda,",
        "considerando todas as células da folha e os dois eixos. É um número mais",
        "severo que a folga média que `tools/audit_spritesheets.py` reporta, e os",
        "dois medem coisas diferentes de propósito: aquele descreve a folha, este",
        "encontra a célula mais apertada dela.",
        "",
        "O contrato de sprites pede 18% para personagens e NPCs e 16% para estações",
        "compactas. Vários valores abaixo aparecem aqui, e isso **não** significa",
        "quadro cortado: a auditoria confirma que nenhum conteúdo encosta na borda",
        "da célula em folha nenhuma. Significa que a folga é mais apertada que a",
        "meta na célula mais cheia. Fechar essa diferença não é reescala mecânica:",
        "encolher o conteúdo dentro da célula encolheria o sprite em jogo, porque o",
        "tamanho de desenho é o tamanho da célula, e desalinharia as hitboxes",
        "derivadas dele. É trabalho de refazer a fonte com mais respiro, não de",
        "reprocessar o que existe.",
        "",
    ]

    for name, _ in CATEGORIES + [("Outros", ())]:
        rows = grouped.get(name)
        if not rows:
            continue
        lines += [
            "## " + name,
            "",
            "| Região | Atlas | Acessor | Grade | Quadros | Célula | Margem | Alpha | Desenhado em |",
            "|---|---|---|---|---|---|---|---|---|",
        ]
        lines += rows
        lines.append("")

    lines += ["## Acessores sem chamador", ""]
    if orphans:
        lines.append("Carregados e nunca desenhados — cada um é atlas e memória "
                     "gastos à toa, ou um recurso que alguém esqueceu de ligar:")
        lines.append("")
        lines += ["- `%s`" % o for o in orphans]
    else:
        lines.append("Nenhum. Todo acessor de textura tem pelo menos um chamador.")
    lines.append("")

    OUTPUT.write_text("\n".join(lines), encoding="utf-8")
    print("%s escrito: %d acessores, %d sem chamador"
          % (OUTPUT.relative_to(REPO), len(accessors), len(orphans)))


if __name__ == "__main__":
    main()
