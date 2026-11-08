# Repeatable local rendering checks

Build and generate the actual mod/Minecraft dependency classpaths:

```sh
./gradlew build
./gradlew --no-configuration-cache -I tools/render-test-classpaths.gradle writeRenderTestClasspaths
```

Run dependency-free loader, geometry and declaration-order checks:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 -m unittest discover -s tools/tests -p 'test_*.py'
python3 tools/test-texture-loader.py
python3 tools/test-vending-updates.py
python3 tools/test-atlas-mesh.py
```

The production parser/renderer fixtures accept:
`--compile-classpath build/render-tests/compile-classpath.txt`
and `--runtime-classpath build/render-tests/runtime-classpath.txt`.
Use those arguments with `tools/test-gregtech-renderer.py`,
`tools/test-wavefront-model.py`, `tools/test-baked-models.py`,
`tools/test-inventory-mimic.py` and `tools/test-renderer-failure.py`.
Baked-model tests additionally accept paths such as
`models/baked/life-infuser.json`; pass all JSON fixtures under
`src/main/resources/models/baked/` to cover the complete model set.

The Techne baker can compare against the actual Minecraft ModelBox class using
`tools/test-techne-baker.py --minecraft-jar build/rfg/srg_merged_minecraft.jar`
and the runtime-classpath argument. Additional golden checks that execute an
installed mod's bytecode need that mod JAR; assets are not redistributed here.
See each test's help/arguments for the exact external fixture input.

Finally, use an isolated full server pack with the built production JAR. A zero
process exit is insufficient: FML can log a boot failure and return zero.
Require server readiness, no custom-renderer/texture-definition failures, a
successful tiny stdtexture render, and an audit with LOADED_PACKS_ONLY coverage.
Run `gwm renderaudit` and preserve the resulting JSON, artifact hash and logs.
Keep production server/world/notifications out of this fixture.

The declaration-order checker covers both eager `moddata` and block references.
`tools/legacy-unresolved-block-textures.json` records the31 exact unresolved
block-face aliases present at baseline `e0cb1f30` (legacy dreamcraft block
registrations). These explicit exceptions do not excuse new missing names or
new forward references. Effective registered blocks still require the runtime
audit and successful textured rendering.
