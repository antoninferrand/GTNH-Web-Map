#!/usr/bin/env python3
import argparse,pathlib,subprocess,tempfile
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description='Exercise immutable HydroEnergy state and real source water patches')
p.add_argument('--compile-classpath',type=pathlib.Path,required=True);p.add_argument('--runtime-classpath',type=pathlib.Path,required=True);p.add_argument('--classes',type=pathlib.Path,required=True);a=p.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-hydro-') as out:
    cp=str(a.classes)+':'+a.compile_classpath.read_text().strip()
    subprocess.run(['javac','--release','8','-cp',cp,'-d',out]+[str(r/file)for file in [
        'src/main/java/org/dynmap/renderer/MapDataContext.java',
        'src/main/java/org/dynmap/hdmap/renderer/VanillaWaterSurfaceRenderer.java',
        'src/main/java/org/dynmap/hdmap/renderer/HydroEnergyWaterRenderer.java',
        'src/main/java/org/dynmap/modsupport/HydroEnergySnapshots.java',
        'tools/tests/HydroEnergyBehaviorTest.java']],check=True)
    subprocess.run(['java','-cp',out+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),'org.dynmap.modsupport.HydroEnergyBehaviorTest'],check=True)
