"""Gera pranchas de auditoria visual sem alterar os assets de produção."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "textures"
OUTPUT = ROOT / "build" / "visual-audit"
OUTPUT.mkdir(parents=True, exist_ok=True)

GROUPS = {
    "world": ["lunar_ground.png", "mars_ground.png", "base_lunar.png",
              "lunar_obstacles.png", "mars_obstacles.png", "landmarks.png"],
    "missions": ["mission_atlas_unified.png", "mars_atlas_v4.png", "oxigenio.png",
                 "comida.png", "gelo.png", "pulse_rifle.png"],
    "enemies_fx": ["lunar_enemy_sheet.png", "mars_drone_sheet.png",
                   "mars_crawler_sheet.png", "action_fx_sheet.png", "energy_fx_sheet.png"],
    "titan": ["titan_ground.png", "titan_enemy_sheet.png", "titan_portal_sheet.png"],
    "ui_keyart": ["intro_keyart_v2.png", "ui_panel_frame.png", "ui/resource_icons.png",
                  "ui/button_normal.png", "ui/panel_hud.png", "ui/damage_vignette.png"],
}

def checker(size):
    image = Image.new("RGB", size, "#B7BDC2")
    draw = ImageDraw.Draw(image)
    tile = 16
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            if (x // tile + y // tile) % 2:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill="#E4E7E9")
    return image

for group, names in GROUPS.items():
    canvas = Image.new("RGB", (1440, 960), "#10161C")
    draw = ImageDraw.Draw(canvas)
    for index, name in enumerate(names):
        x = 20 + index % 3 * 475
        y = 20 + index // 3 * 470
        panel = checker((435, 400))
        source = Image.open(SOURCE / name).convert("RGBA")
        source.thumbnail((415, 380), Image.Resampling.LANCZOS)
        panel.paste(source, ((435-source.width)//2, (400-source.height)//2), source)
        canvas.paste(panel, (x, y + 30))
        draw.text((x, y + 5), name, fill="#F1EEE5")
    canvas.save(OUTPUT / f"{group}.png", optimize=True)
print(OUTPUT)
