"""Offline TexturePacker runner; preserves HUD sources while replacing the key art."""
from pathlib import Path
import argparse
import re
import shutil
import subprocess
import uuid
from split_atlas_frames import split

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--java', required=True)
    parser.add_argument('--gradle-cache', type=Path, required=True)
    args = parser.parse_args()
    from prepare_visual_assets import validate_all
    validate_all()
    config = (ROOT/'build.gradle').read_text(encoding='utf-8')
    groups = {}
    for group in ('game','fx','ui'):
        block = re.search(r'\b'+group+r':\s*\[(.*?)\]', config, re.S).group(1)
        groups[group] = re.findall(r"'([^']+\.png)'", block)
    optional = re.search(r'def optionalGameAssets = \[(.*?)\]', config, re.S).group(1)
    groups['game'] += [name for name in re.findall(r"'([^']+\.png)'",optional)
                       if (ROOT/'assets/textures'/name).exists()]
    staging = ROOT/'build/atlas-input'
    for group,names in groups.items():
        folder = staging/group
        folder.mkdir(parents=True,exist_ok=True)
        # Only disposable top-level PNGs in the exact build folders.
        for old in folder.glob('*.png'):
            if old.parent.resolve() != (ROOT/'build/atlas-input'/group).resolve():
                raise ValueError('Unexpected staging path')
            old.unlink()
        shutil.copy2(ROOT/'tools/texturepacker/pack.json',folder/'pack.json')
        for name in names: shutil.copy2(ROOT/'assets/textures'/name,folder/Path(name).name)
    split(staging,ROOT/'build/asset-qa/sprite_metrics.json')
    jars = sorted(str(path) for group in ('com.badlogicgames.gdx','org.lwjgl.lwjgl')
                  for path in (args.gradle_cache/group).rglob('*.jar'))
    import os
    classpath = os.pathsep.join(jars)
    # Packing directly over existing pages can leave game2.png/game3.png behind.
    # Build into a fresh directory, then replace the entire known production set.
    # Windows tempfile's restrictive ACL can prevent the Java child from listing it.
    output = ROOT/'build'/('packed-visuals-'+uuid.uuid4().hex)
    output.mkdir()
    for group in groups:
        subprocess.run([args.java,'-cp',classpath,'com.badlogic.gdx.tools.texturepacker.TexturePacker',
            str(staging/group),str(output),group],check=True,cwd=ROOT)
    atlas_root = (ROOT/'assets/atlases').resolve()
    backup = ROOT/'tools/source_assets/before_rework_20260914/atlases'
    backup.mkdir(parents=True,exist_ok=True)
    for previous in atlas_root.iterdir():
        if not previous.is_file() or not re.fullmatch(r'(game|ui|fx)(\d+)?\.(png|atlas)',previous.name):
            continue
        if not (backup/previous.name).exists(): shutil.copy2(previous,backup/previous.name)
    installed = {file.name for file in output.iterdir() if file.is_file()}
    for file in output.iterdir():
        if file.is_file(): shutil.copy2(file,atlas_root/file.name)
    for previous in atlas_root.iterdir():
        if previous.is_file() and re.fullmatch(r'(game|ui|fx)\d*\.png',previous.name) and previous.name not in installed:
            if previous.parent.resolve() != atlas_root: raise ValueError('Unexpected atlas path')
            # A recovery copy exists outside runtime before removing an obsolete page.
            previous.unlink()
    asset_root = ROOT/'assets'
    listing = sorted(path.relative_to(asset_root).as_posix() for path in asset_root.rglob('*')
                     if path.is_file() and path.name != 'assets.txt')
    (asset_root/'assets.txt').write_text('\n'.join(listing)+'\n',encoding='utf-8')

if __name__ == '__main__': main()
