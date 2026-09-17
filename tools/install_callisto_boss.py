"""Mechanical frame slicing and common foot-pivot normalization; retain the raw source."""
from pathlib import Path
from PIL import Image
import shutil

ROOT = Path(__file__).resolve().parents[1]
RAW = Path(r'C:/Users/gustavo_m_deschamps/.codex/generated_images/01a039f8-ac8a-7be1-b177-fd592bff0b47/exec-9576135e-7732-4da0-93ee-871bf57d895f.png')
SOURCE = ROOT/'tools/source_assets/part3_20260917/callisto_boss_raw.png'
SOURCE.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(RAW, SOURCE)
image = Image.open(SOURCE).convert('RGBA')
if image.getextrema()[3][0] == 255:
    raise ValueError('Boss sheet lacks transparency; do not install an opaque image.')
frames=[]
for row in range(3):
    for col in range(4):
        frame=image.crop((round(col*image.width/4), round(row*image.height/3),
                          round((col+1)*image.width/4), round((row+1)*image.height/3)))
        box=frame.getchannel('A').point(lambda p:255 if p>32 else 0).getbbox()
        if box is None: raise ValueError(f'Empty frame {row},{col}')
        frames.append(frame.crop(box))
# Common scale retains raised-arm height and avoids pose-dependent resizing.
scale=min(280/max(f.width for f in frames),300/max(f.height for f in frames))
sheet=Image.new('RGBA',(4*384,3*384))
for i, frame in enumerate(frames):
    frame=frame.resize((round(frame.width*scale),round(frame.height*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(384,384))
    canvas.alpha_composite(frame,((384-frame.width)//2,344-frame.height))
    sheet.alpha_composite(canvas,((i%4)*384,(i//4)*384))
sheet.save(ROOT/'assets/textures/callisto_boss_sheet_v1.png')
print('Installed 12 complete frames on a common foot pivot.')
