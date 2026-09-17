"""Recreate geometric HUD surfaces without baked-in speckles or damaged pixels.

Keeps the existing chamfered silver/blue visual language and nine-patch contracts.
This builds new geometry from scratch; no candidate art or gameplay asset is imported.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import shutil

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT/'assets/textures/ui'
BACKUP = ROOT/'tools/source_assets/before_ui_cleanup_20260917'
SCALE = 4

def save(image, path):
    BACKUP.mkdir(parents=True,exist_ok=True)
    old = BACKUP/path.name
    if path.exists() and not old.exists(): shutil.copy2(path,old)
    image.save(path,optimize=True)

def surface(size, fill, accent, panel=False):
    w,h = size
    image = Image.new('RGBA',(w*SCALE,h*SCALE))
    draw = ImageDraw.Draw(image)
    def polygon(inset,cut,color):
        points = [(inset+cut,inset),(w-inset-cut,inset),(w-inset,inset+cut),
                  (w-inset,h-inset-cut),(w-inset-cut,h-inset),(inset+cut,h-inset),
                  (inset,h-inset-cut),(inset,inset+cut)]
        draw.polygon([(round(x*SCALE),round(y*SCALE)) for x,y in points],fill=color)
    polygon(1,9,accent if panel else (198,202,195,255))
    polygon(2,8,(8,20,29,255) if panel else (38,48,57,255))
    polygon(5,7,accent)
    polygon(6,6,fill)
    return image.resize(size,Image.Resampling.LANCZOS)

def main():
    for state,fill,accent in [
        ('normal',(8,20,29,255),(45,139,208,255)),
        ('hover',(15,35,48,255),(86,183,236,255)),
        ('pressed',(6,17,24,255),(229,164,58,255)),
        ('disabled',(16,22,27,220),(69,90,106,220))]:
        save(surface((192,64),fill,accent),UI/f'button_{state}.png')
    for name,accent in [('hud',(45,139,208,255)),('dialog',(45,139,208,255)),
                        ('modal',(45,139,208,255))]:
        save(surface((96,96),(10,19,27,240),accent,True),UI/f'panel_{name}.png')
    save(surface((96,96),(10,19,27,240),(45,139,208,255),True),
         ROOT/'assets/textures/ui_panel_frame.png')
    for name,color in [('track',(8,18,26,255)),('fill',(45,139,208,255))]:
        save(surface((256,24),color,(144,170,183,255)),UI/f'bar_{name}.png')
    for name in ['default','target']:
        image = Image.new('RGBA',(32*SCALE,32*SCALE))
        draw = ImageDraw.Draw(image)
        if name == 'default':
            points = [(3,3),(3,24),(8,19),(12,27),(16,25),(12,17),(20,17)]
            points = [(x*SCALE,y*SCALE) for x,y in points]
            draw.polygon(points,fill=(230,241,247,255))
            draw.line(points+[points[0]],fill=(14,28,39,255),width=SCALE)
        else:
            draw.ellipse((9*SCALE,9*SCALE,23*SCALE,23*SCALE),
                         outline=(229,164,58,255),width=SCALE)
            for x0,y0,x1,y1 in [(16,3,16,8),(16,24,16,29),(3,16,8,16),(24,16,29,16)]:
                draw.line((x0*SCALE,y0*SCALE,x1*SCALE,y1*SCALE),
                          fill=(229,164,58,255),width=SCALE)
        save(image.resize((32,32),Image.Resampling.LANCZOS),UI/f'cursor_{name}.png')
    print('Clean UI surfaces and compact 32px cursors installed')

if __name__ == '__main__': main()
