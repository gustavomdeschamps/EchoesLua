# -*- coding: utf-8 -*-
"""Auditoria das spritesheets contra a grade declarada no AssetManager.

Percorre cada folha de producao, corta pela grade que o AssetManager usa em
runtime e mede o que so aparece na tela: celula vazia, folha sem alpha real,
conteudo encostado na borda (residuo da celula vizinha ou quadro cortado),
oscilacao de baseline e variacao de escala dentro da mesma linha, e
conteudo mais perto da borda que o inset aplicado em runtime.

Nem toda oscilacao e defeito. Numa folha de formacoes ou de landmarks cada
celula e um objeto diferente e a variacao e esperada; numa folha de maquina
ela significa que o corpo mudou de lugar entre os quadros. Por isso o relatorio
mede e nomeia, e a decisao de corrigir fica com quem le - a ancoragem
automatica vive em tools/anchor_machine_rows.py e so e aplicada onde o
documento de arte exige geometria identica.

O limiar de alpha importa: vapor, brilho e particulas tem alpha baixo, e usar
a caixa cheia faz vapor legitimo parecer maquina fora de lugar.

Uso:
    python tools/audit_spritesheets.py
"""
import os, sys
import numpy as np
from PIL import Image

# nome do arquivo -> (colunas, linhas, inset usado no gridRegion)
SHEETS = {
    "astronauta_sheet.png": (4, 4, 0),
    "astronaut_combat_sheet.png": (4, 3, 0),
    "mars_atlas_v4.png": (4, 3, 2),
    "lunar_enemy_sheet.png": (4, 4, 2),
    "mars_drone_sheet.png": (4, 4, 2),
    "mars_crawler_sheet.png": (4, 4, 2),
    "titan_hunter_sheet_v3.png": (4, 4, 2),
    "titan_boss_sheet_v3.png": (4, 4, 2),
    "npc_colony_officer_sheet_v2.png": (4, 4, 2),
    "npc_commander_ayla_sheet.png": (4, 4, 0),
    "npc_researcher_lira_sheet_v2.png": (4, 4, 2),
    "mars_station_sheet_v2.png": (4, 3, 2),
    "titan_refinery_sheet_v2.png": (4, 1, 2),
    "campaign_portal_sheet_v2.png": (4, 4, 3),
    "titan_portal_vertical_v2.png": (4, 2, 0),
    "lunar_repair_stations_v2.png": (4, 4, 3),
    "titan_formations_v2.png": (3, 2, 3),
    "lunar_obstacles.png": (3, 2, 2),
    "mars_obstacles.png": (3, 2, 2),
    "action_fx_sheet.png": (6, 4, 0),
    "energy_fx_sheet.png": (6, 4, 0),
    "landmarks.png": (4, 2, 2),
    "ui/resource_icons.png": (4, 1, 0),
    "mission_atlas_unified.png": (4, 4, 2),
}

ALPHA_VISIBLE = 12       # acima disto o pixel conta como conteudo
EDGE_BAND = 1            # faixa de pixels colada na borda da celula


def audit(path, cols, rows, inset):
    img = Image.open(path).convert("RGBA")
    w, h = img.size
    problems = []
    if w % cols or h % rows:
        problems.append("GRADE: %dx%d nao divide em %dx%d" % (w, h, cols, rows))
        return problems, None
    cw, ch = w // cols, h // rows
    a = np.asarray(img)[:, :, 3]

    if a.max() == 255 and a.min() == 255:
        problems.append("ALPHA: folha totalmente opaca, sem recorte")

    baselines, heights, widths = [], [], []
    for r in range(rows):
        for c in range(cols):
            cell = a[r*ch:(r+1)*ch, c*cw:(c+1)*cw]
            vis = cell > ALPHA_VISIBLE
            if not vis.any():
                problems.append("CELULA VAZIA: coluna %d, linha %d" % (c, r))
                continue
            ys, xs = np.where(vis)
            top, bottom = ys.min(), ys.max()
            left, right = xs.min(), xs.max()
            # conteudo encostado na borda da celula (§12)
            touch = []
            if top <= EDGE_BAND: touch.append("topo")
            if bottom >= ch - 1 - EDGE_BAND: touch.append("base")
            if left <= EDGE_BAND: touch.append("esquerda")
            if right >= cw - 1 - EDGE_BAND: touch.append("direita")
            if touch:
                problems.append("BORDA c%d,l%d encosta em: %s" % (c, r, ", ".join(touch)))

            # O runtime recorta `inset` px de cada borda antes de entregar a
            # regiao. Conteudo mais perto da borda que isso sai cortado em
            # jogo, e o corte nao aparece olhando a folha.
            slack = min(top, ch - 1 - bottom, left, cw - 1 - right)
            if inset and slack < inset:
                problems.append(
                    "INSET c%d,l%d: conteudo a %dpx da borda, inset recorta %dpx"
                    % (c, r, slack, inset))
            baselines.append((r, c, bottom))
            heights.append((r, c, bottom - top + 1))
            widths.append((r, c, right - left + 1))

    # deriva de baseline dentro da mesma linha da grade (§12: o chao nao sobe e desce)
    for r in range(rows):
        row_b = [b for (rr, c, b) in baselines if rr == r]
        if len(row_b) > 1:
            drift = max(row_b) - min(row_b)
            if drift > ch * 0.06:
                problems.append("BASELINE linha %d oscila %dpx (%.1f%% da celula)"
                                % (r, drift, 100.0 * drift / ch))
        row_h = [v for (rr, c, v) in heights if rr == r]
        if len(row_h) > 1:
            spread = (max(row_h) - min(row_h)) / float(max(row_h))
            if spread > 0.18:
                problems.append("ESCALA linha %d varia %.0f%% de altura" % (r, 100 * spread))

    margin = None
    if heights:
        used_h = max(v for (_, _, v) in heights)
        used_w = max(v for (_, _, v) in widths)
        margin = min(100.0 * (ch - used_h) / ch, 100.0 * (cw - used_w) / cw)
    return problems, (cw, ch, margin, inset)


base = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "assets", "textures")
total_problems = 0
missing = []
for name in sorted(SHEETS):
    path = os.path.join(base, name)
    if not os.path.exists(path):
        missing.append(name)
        continue
    cols, rows, inset = SHEETS[name]
    problems, info = audit(path, cols, rows, inset)
    status = "OK " if not problems else "!! "
    extra = ""
    if info:
        cw, ch, margin, ins = info
        extra = "celula %dx%d, margem livre %.1f%%, inset %d" % (cw, ch, margin, ins)
    print("%s%-36s %s" % (status, name, extra))
    for p in problems:
        print("      - " + p)
    total_problems += len(problems)

if missing:
    print("\nNAO ENCONTRADOS: " + ", ".join(missing))
print("\nTOTAL DE PROBLEMAS: %d" % total_problems)
