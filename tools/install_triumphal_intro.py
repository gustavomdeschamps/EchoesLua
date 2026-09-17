"""Mechanical 16:9 delivery of the reviewed built-in image generation output."""
from pathlib import Path
from PIL import Image, ImageOps
import shutil, hashlib, json
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'tools/source_assets/rework_20260914/intro_panorama.png'
TARGET=ROOT/'assets/textures/intro_keyart_v4.png'
BACKUP=ROOT/'tools/source_assets/before_intro_20260917'
BACKUP.mkdir(parents=True,exist_ok=True)
if not (BACKUP/TARGET.name).exists(): shutil.copy2(TARGET,BACKUP/TARGET.name)
ImageOps.pad(Image.open(SOURCE).convert('RGB'),(1280,720),method=Image.Resampling.LANCZOS).save(TARGET,optimize=True)
audit_path=SOURCE.parent/'installed.json'
audit=json.loads(audit_path.read_text(encoding='utf-8'))
audit[TARGET.name]={'source':str(SOURCE.relative_to(ROOT)),'mode':'keyart',
                  'sha256':hashlib.sha256(TARGET.read_bytes()).hexdigest()}
audit_path.write_text(json.dumps(audit,indent=2)+'\n',encoding='utf-8')
print('Three-world opening installed')
