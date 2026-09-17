"""Reproducible approved visual import; does not modify any HUD asset."""
from import_rework_assets import (install, ROOT, SOURCES, BACKUP, TEXTURES,
                                 CELL, bounds, clean_alpha, source_frames,
                                 remove_isolated_specks)
from make_reflected_seamless import periodic_tile
from PIL import Image, ImageDraw, ImageOps
import json
import shutil
import hashlib

SHEETS = [
 ('lunar_stations','lunar_repair_stations_v2.png',4,4,'machine'),
 ('mars_props','mars_atlas_v4.png',4,3,'static'),
 ('mars_stations','mars_station_sheet_v2.png',4,3,'machine'),
 ('lunar_rocks','lunar_obstacles.png',3,2,'static'),
 ('mars_rocks','mars_obstacles.png',3,2,'static'),
 ('titan_rocks','titan_formations_v2.png',3,2,'static'),
 ('mission','mission_atlas_unified.png',4,4,'static'),
 ('refinery','titan_refinery_sheet_v2.png',4,1,'machine'),
 ('landmarks','landmarks.png',4,2,'static'),
 ('lunar_enemy','lunar_enemy_sheet.png',4,4,'actor'),
 ('mars_drone','mars_drone_sheet.png',4,4,'actor'),
 ('mars_crawler','mars_crawler_sheet.png',4,4,'actor'),
 ('titan_hunter','titan_hunter_sheet_v3.png',4,4,'actor'),
 ('titan_hunter','titan_enemy_sheet.png',4,4,'actor'),
 ('titan_boss','titan_boss_sheet_v3.png',4,4,'actor'),
 ('ayla','npc_commander_ayla_sheet.png',4,4,'actor'),
 ('ayyub','npc_colony_officer_sheet_v2.png',4,4,'actor'),
 ('lira','npc_researcher_lira_sheet_v2.png',4,4,'actor'),
 ('portal','campaign_portal_sheet_v2.png',4,4,'machine'),
 ('titan_portal','titan_portal_vertical_v2.png',4,2,'machine'),
 ('hero_walk','astronauta_sheet.png',4,4,'actor'),
 ('hero_combat','astronaut_combat_sheet.png',4,3,'actor'),
 ('action_fx','action_fx_sheet.png',6,4,'fx'),
 ('energy_fx','energy_fx_sheet.png',6,4,'fx'),
]

def install_gait_cycles():
    """Replace gait rows, preserving the reviewed idle/dash scale and foot pivot."""
    target = TEXTURES/'astronauta_sheet.png'
    sheet = Image.open(target).convert('RGBA')
    sources = []
    for row, name in [(1, 'hero_walk_cycle.png'), (2, 'hero_run_cycle.png')]:
        source = SOURCES/name
        frames = source_frames(remove_isolated_specks(clean_alpha(Image.open(source))), 4, 1)
        current = [sheet.crop((col*CELL,row*CELL,(col+1)*CELL,(row+1)*CELL))
                   for col in range(4)]
        target_height = max(bounds(frame)[3]-bounds(frame)[1] for frame in current)
        scale = min(target_height/max(f.height for f in frames),
                    CELL*.8/max(f.width for f in frames))
        for col, frame in enumerate(frames):
            fitted = clean_alpha(frame.resize((round(frame.width*scale),round(frame.height*scale)),
                                             Image.Resampling.LANCZOS))
            canvas = Image.new('RGBA',(CELL,CELL))
            canvas.alpha_composite(fitted,((CELL-fitted.width)//2,279-fitted.height))
            sheet.paste(canvas,(col*CELL,row*CELL))
        sources.append(str(source.relative_to(ROOT)))
    staged = ROOT/'build/rework-staging'/target.name
    sheet.save(staged,optimize=True)
    staged.replace(target)
    audit_path = SOURCES/'installed.json'
    audit = json.loads(audit_path.read_text(encoding='utf-8'))
    audit[target.name]['gait_sources'] = sources
    audit[target.name]['sha256'] = hashlib.sha256(target.read_bytes()).hexdigest()
    audit_path.write_text(json.dumps(audit,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')


def main():
    failures = []
    for key, name, cols, rows, mode in SHEETS:
        try:
            install(key,name,cols,rows,mode)
        except ValueError as error:
            failures.append(f'{key}: {error}')
    if not any(failure.startswith('hero_walk:') for failure in failures):
        install_gait_cycles()
    install('base','base_lunar.png')
    for name, cell in [('oxigenio.png',(0,0)),('comida.png',(1,0)),
                       ('gelo.png',(0,1)),('pulse_rifle.png',(1,1))]:
        install('supplies',name,2,2,cell=cell)
    path = SOURCES/'installed.json'
    audit = json.loads(path.read_text(encoding='utf-8'))
    for key,name in [('ground_lunar','lunar_ground.png'),('ground_mars','mars_ground.png'),
                     ('ground_titan','titan_ground_v2.png'),('intro_lunar','world_intro_lunar_v1.png'),
                     ('intro_mars','world_intro_mars_v1.png'),('intro_titan','world_intro_titan_v1.png'),
                     ('intro_panorama','intro_keyart_v4.png')]:
        target = TEXTURES/name
        if target.exists() and not (BACKUP/name).exists(): shutil.copy2(target,BACKUP/name)
        staged = ROOT/'build/rework-staging'/name
        if key.startswith('ground_'): periodic_tile(SOURCES/(key+'.png'),staged)
        else:
            # Preserve the full composition and the intro screen's 1280x720 contract.
            ImageOps.pad(Image.open(SOURCES/(key+'.png')).convert('RGB'), (1280,720),
                         method=Image.Resampling.LANCZOS, color=(10,14,20)).save(staged)
        staged.replace(target)
        audit[name] = {'source':str((SOURCES/(key+'.png')).relative_to(ROOT)),
                       'mode':'terrain' if key.startswith('ground_') else 'keyart',
                       'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
    path.write_text(json.dumps(audit,indent=2)+'\n',encoding='utf-8')
    for page in range(3):
        contact = Image.new('RGB',(1600,1200),(25,32,41))
        draw = ImageDraw.Draw(contact)
        for index, spec in enumerate(SHEETS[page*8:(page+1)*8]):
            key,name,*_ = spec
            preview = Image.open(TEXTURES/name).convert('RGBA')
            preview.thumbnail((380,545))
            x,y = index%4*400,index//4*600
            contact.paste(preview,(x+(400-preview.width)//2,y+32),preview)
            draw.text((x+12,y+10),name,fill='white')
        contact.save(ROOT/f'build/asset-qa/rework-page-{page}.png')
    print('\n'.join(failures) if failures else 'All imports completed')
    if failures: raise SystemExit(1)

if __name__ == '__main__': main()
