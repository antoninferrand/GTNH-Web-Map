#!/usr/bin/env python3
"""Run production renderer CPU regressions with explicit build and external golden fixtures."""
import argparse
import concurrent.futures
import json
import os
from pathlib import Path
import subprocess
import sys
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--classes', type=Path, help='Built main class directory or unshaded dev-preshadow JAR')
    parser.add_argument('--compile-classpath', type=Path, required=True)
    parser.add_argument('--runtime-classpath', type=Path, required=True)
    parser.add_argument('--minecraft-sources', type=Path, required=True, help='MCP Minecraft source JAR for RenderBlocks golden')
    parser.add_argument('--minecraft-golden', type=Path, help='Actual SRG Minecraft binary JAR for ModelBox golden')
    parser.add_argument('--obj-model', type=Path, help='Installed/source ice_cream_machine.obj fixture')
    parser.add_argument('--extrautilities-jar', type=Path, help='Installed ExtraUtilities JAR for pixel golden')
    parser.add_argument('--walrus-atlas', type=Path, help='Installed/source AE2 Fluid Crafting walrus PNG')
    parser.add_argument('--require-goldens', action='store_true', help='Require every external golden fixture')
    parser.add_argument('--jobs', type=int, default=3)
    parser.add_argument('--output', type=Path, help='Log/report directory, default build/render-test-results')
    args = parser.parse_args()
    root = args.source_root.resolve()
    classes = (args.classes or root / 'build/classes/java/main').resolve()
    output = (args.output or root / 'build/render-test-results').resolve()
    if args.jobs < 1:
        parser.error('--jobs must be positive')
    for label, path in [('classes', classes), ('compile classpath', args.compile_classpath),
                        ('runtime classpath', args.runtime_classpath), ('Minecraft sources', args.minecraft_sources)]:
        if not path.exists():
            parser.error(label + ' does not exist: ' + str(path))
    external = {'minecraftModelBox': args.minecraft_golden, 'installedObj': args.obj_model,
                'extraUtilitiesPixels': args.extrautilities_jar, 'walrusAtlas': args.walrus_atlas}
    for label, path in external.items():
        if path is not None and not path.is_file():
            parser.error(label + ' fixture does not exist: ' + str(path))
    if args.require_goldens and any(path is None for path in external.values()):
        parser.error('--require-goldens requires --minecraft-golden, --obj-model, --extrautilities-jar and --walrus-atlas')
    common = ['--compile-classpath', str(args.compile_classpath.resolve()),
              '--runtime-classpath', str(args.runtime_classpath.resolve())]
    compiled = ['--classes', str(classes)]
    models = ['models/baked/' + path.name for path in sorted((root / 'src/main/resources/models/baked').glob('*.json'))
              if 'atlas' in json.loads(path.read_text())]
    tasks = {
        'gregtech': ['tools/test-gregtech-renderer.py'] + common + compiled,
        'wavefront': ['tools/test-wavefront-model.py'] + common + compiled,
        'baked': ['tools/test-baked-models.py'] + common + compiled + models,
        'mimic': ['tools/test-inventory-mimic.py'] + common + compiled,
        'renderer-failure': ['tools/test-renderer-failure.py'] + common + compiled,
        'patch-capacity': ['tools/test-patch-capacity.py'] + common + compiled,
        'water': ['tools/test-water-surface.py'] + common + compiled + ['--minecraft-sources', str(args.minecraft_sources.resolve())],
        'hydro': ['tools/test-hydroenergy.py'] + common + compiled,
        'wall-mirror': ['tools/test-wall-mirror.py'] + common + compiled,
        'vanilla-pixels': ['tools/test-vanilla-texture-pixels.py'] + common + compiled,
        'model-triangle-bounds': ['tools/test-model-triangle-bounds.py'] + common + compiled,
        'dedicated-aspects': ['tools/test-witching-dedicated-aspects.py'] + common + compiled,
        'techne': ['tools/test-techne-baker.py'],
        'nested': ['-m', 'unittest', 'discover', '-s', 'tools/tests', '-p', 'test_*.py'],
    }
    if args.walrus_atlas:
        tasks['walrus'] = ['tools/test-walrus.py'] + common + compiled + ['--atlas', str(args.walrus_atlas.resolve())]
    if args.obj_model:
        tasks['wavefront'] += ['--model', str(args.obj_model.resolve())]
    if args.minecraft_golden:
        tasks['techne'] += ['--minecraft-jar', str(args.minecraft_golden.resolve()),
                            '--runtime-classpath', str(args.runtime_classpath.resolve())]
    for name in ['test-atlas-mesh.py', 'test-texture-loader.py', 'test-vending-updates.py',
                 'test_cable_rendering.py', 'test_forge_resource_lookup.py', 'test_pipe_geometry.py',
                 'test_render_audit.py', 'test_renderdata_references.py']:
        tasks[name.removesuffix('.py')] = ['tools/' + name]
    output.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    # An omitted golden stays an explicit coverage gap, regardless of unrelated local files.
    env['EXTRAUTILITIES_JAR'] = str(args.extrautilities_jar.resolve()) if args.extrautilities_jar else str(output / 'not-provided-extrautilities.jar')

    def run(item):
        name, command = item
        command = [sys.executable] + command
        start = time.monotonic()
        process = subprocess.run(command, cwd=root, env=env, stdout=subprocess.PIPE,
                                 stderr=subprocess.STDOUT, text=True)
        log = name + '.log'
        (output / log).write_text(process.stdout)
        result = {'name': name, 'exitCode': process.returncode, 'seconds': round(time.monotonic() - start, 3),
                  'command': command, 'log': log}
        print(('PASS ' if process.returncode == 0 else 'FAIL ') + name, flush=True)
        return result

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as pool:
        results = list(pool.map(run, tasks.items()))
    failed = [result['name'] for result in results if result['exitCode'] != 0]
    gaps = [name for name, path in external.items() if path is None]
    report = {'status': 'FAIL' if failed else 'PASS_WITH_OPTIONAL_COVERAGE_GAPS' if gaps else 'PASS',
              'scope': 'CPU production regressions; dedicated Forge startup and visual comparison remain separate',
              'bakedModels': models, 'externalGoldenCoverage': {name: path is not None for name, path in external.items()},
              'coverageGaps': gaps, 'failed': failed, 'results': results}
    (output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(f"{report['status']}: {len(results) - len(failed)}/{len(results)} harnesses; report {output / 'report.json'}")
    return 1 if failed else 0


if __name__ == '__main__':
    raise SystemExit(main())
