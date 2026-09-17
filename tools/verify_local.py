"""Fallback for machines where Gradle's Java loopback transport is unavailable."""
from pathlib import Path
import argparse
import os
import subprocess
import re

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jdk',type=Path,required=True)
    parser.add_argument('--cache',type=Path,required=True)
    parser.add_argument('--capture',action='store_true')
    args = parser.parse_args()
    groups = ('com.badlogicgames.gdx','org.lwjgl','org.junit.jupiter',
              'org.junit.platform','org.apiguardian','org.opentest4j','com.badlogicgames.jlayer','org.jcraft')
    properties = (ROOT/'gradle.properties').read_text(encoding='utf-8')
    lwjgl_version = re.search(r'^lwjgl3Version=(.+)$',properties,re.M).group(1).strip()
    gdx_version = re.search(r'^gdxVersion=(.+)$',properties,re.M).group(1).strip()
    def compatible(path, group):
        artifact, version = path.relative_to(args.cache/group).parts[:2]
        if group == 'org.lwjgl': return version == lwjgl_version
        if group == 'com.badlogicgames.gdx':
            if artifact.startswith('gdx-jnigen'): return True
            return artifact != 'gdx-backend-lwjgl' and (not artifact.startswith('gdx') or version == gdx_version)
        return True
    jars = [str(p) for group in groups for p in (args.cache/group).rglob('*.jar')
            if '-sources' not in p.name and '-javadoc' not in p.name and compatible(p,group)]
    classpath = os.pathsep.join(jars)
    classes = ROOT/'build/manual-classes'
    tests = ROOT/'build/manual-test-classes'
    classes.mkdir(parents=True,exist_ok=True)
    tests.mkdir(parents=True,exist_ok=True)
    def run(exe, options, cwd=ROOT):
        argfile = ROOT/'build'/('verify-'+exe+'.args')
        argfile.write_text('\n'.join('"'+str(arg).replace('\\','/')+'"' for arg in options),encoding='utf-8')
        subprocess.run([str(args.jdk/'bin'/(exe+'.exe')),'@'+str(argfile)],cwd=cwd,check=True)
    sources = list((ROOT/'core/src/main/java').rglob('*.java'))
    sources += list((ROOT/'lwjgl3/src/main/java').rglob('*.java'))
    run('javac',['-encoding','UTF-8','-proc:none','-cp',classpath,'-d',classes,*sources])
    test_sources = list((ROOT/'core/src/test/java').rglob('*.java'))
    test_sources += [ROOT/'tools/java/LocalTestLauncher.java']
    run('javac',['-encoding','UTF-8','-proc:none','-cp',str(classes)+os.pathsep+classpath,
                 '-d',tests,*test_sources])
    run('java',['-Duser.home='+str(ROOT/'build/test-home'),'-cp',
                os.pathsep.join([str(classes),str(tests),classpath]),'LocalTestLauncher'])
    if args.capture:
        run('javac',['-encoding','UTF-8','-proc:none','-cp',str(classes)+os.pathsep+classpath,
                     '-d',tests,ROOT/'tools/java/AssetVisualCapture.java',ROOT/'tools/java/GameSmokeCapture.java'])
        run('java',['-Duser.home='+str(ROOT/'build/test-home'),'-cp',
                    os.pathsep.join([str(classes),str(tests),classpath]),'AssetVisualCapture'],ROOT/'assets')
        run('java',['-Duser.home='+str(ROOT/'build/test-home'),'-cp',
                    os.pathsep.join([str(classes),str(tests),classpath]),'GameSmokeCapture'],ROOT/'assets')

if __name__ == '__main__': main()
