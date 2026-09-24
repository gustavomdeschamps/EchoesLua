"""Normaliza as folhas fonte dos chefes de Lua e Marte em 4 células 384x384."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools" / "source_assets" / "part3_20260921"
OUTPUT = ROOT / "assets" / "textures"


def install(source_name: str, output_name: str) -> None:
    source = Image.open(SOURCE / source_name).convert("RGBA")
    frame_width = source.width // 4
    sheet = Image.new("RGBA", (384 * 4, 384), (0, 0, 0, 0))
    for column in range(4):
        left = column * frame_width
        right = source.width if column == 3 else (column + 1) * frame_width
        frame = source.crop((left, 0, right, source.height))
        alpha_box = frame.getchannel("A").getbbox()
        if alpha_box is None:
            raise ValueError(f"Quadro transparente em {source_name}, coluna {column}")
        actor = frame.crop(alpha_box)
        scale = min(320 / actor.width, 320 / actor.height)
        size = (max(1, round(actor.width * scale)), max(1, round(actor.height * scale)))
        actor = actor.resize(size, Image.Resampling.LANCZOS)
        x = column * 384 + (384 - actor.width) // 2
        y = 354 - actor.height
        sheet.alpha_composite(actor, (x, y))
    destination = OUTPUT / output_name
    sheet.save(destination, optimize=True)
    print(f"{destination.relative_to(ROOT)}: {sheet.size[0]}x{sheet.size[1]}")


install("lunar_boss_raw.png", "lunar_boss_sheet_v1.png")
install("mars_boss_raw.png", "mars_boss_sheet_v1.png")


def install_keys() -> None:
    source = Image.open(SOURCE / "boss_keys_raw.png").convert("RGBA")
    frame_width = source.width // 4
    sheet = Image.new("RGBA", (192 * 4, 192), (0, 0, 0, 0))
    for column in range(4):
        right = source.width if column == 3 else (column + 1) * frame_width
        frame = source.crop((column * frame_width, 0, right, source.height))
        actor = frame.crop(frame.getchannel("A").getbbox())
        scale = min(154 / actor.width, 154 / actor.height)
        actor = actor.resize((round(actor.width * scale), round(actor.height * scale)), Image.Resampling.LANCZOS)
        sheet.alpha_composite(actor, (column * 192 + (192 - actor.width) // 2,
                                      (192 - actor.height) // 2))
    sheet.save(OUTPUT / "boss_keys_sheet_v1.png", optimize=True)
    print("assets\\textures\\boss_keys_sheet_v1.png: 768x192")


install_keys()
