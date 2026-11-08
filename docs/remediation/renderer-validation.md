# Renderer validation

Run the CPU suite after compiling main classes. Generate the build's actual compile/runtime classpaths without constructing them from a local dependency inventory:

```sh
./gradlew --no-configuration-cache -I tools/render-test-classpaths.gradle writeRenderTestClasspaths
```

Set `GTNH_VALIDATION_OBJ` to the installed/source `ice_cream_machine.obj` and `GTNH_VALIDATION_EXTRAUTILITIES_JAR` to the matching installed ExtraUtilities JAR. Set `GTNH_VALIDATION_WALRUS_PNG` to AE2 Fluid Crafting's original `assets/ae2fc/textures/blocks/walrus.png`. The complete suite, including external golden comparisons, is:

```sh
python3 tools/run-renderer-regressions.py \
  --compile-classpath build/render-tests/compile-classpath.txt \
  --runtime-classpath build/render-tests/runtime-classpath.txt \
  --classes build/classes/java/main \
  --minecraft-sources build/rfg/mcp_patched_minecraft-sources.jar \
  --minecraft-golden build/rfg/srg_merged_minecraft.jar \
  --obj-model "$GTNH_VALIDATION_OBJ" \
  --extrautilities-jar "$GTNH_VALIDATION_EXTRAUTILITIES_JAR" \
  --walrus-atlas "$GTNH_VALIDATION_WALRUS_PNG" \
  --require-goldens
```

The runner writes one log per harness and `build/render-test-results/report.json`, including exact executed commands, exit codes, elapsed times, model selection and external-golden coverage. It runs only CPU checks and uses temporary compiler output directories. It does not run Gradle, change worlds, start a game client or modify a server. `--source-root`, `--classes`, `--output` and `--jobs` support an immutable local snapshot. Copy all referenced generated class/resource directories when snapshotting classpath files; main classes alone do not include patched Minecraft dependencies.

Omitting an external golden flag explicitly records a coverage gap and reports `PASS_WITH_OPTIONAL_COVERAGE_GAPS`. `--require-goldens` rejects incomplete fixture arguments. Minecraft ModelBox reflection requires the SRG binary; MCP-renamed classes have different field names. The baked-mesh task selects only JSON resources with an atlas definition: the wall mirror's source pixel-grid JSON uses a separate renderer and is not a BakedMesh input.

| Harness | Invocation and coverage |
| --- | --- |
| GregTech and vending | `tools/test-gregtech-renderer.py` with compile/runtime classpath files and main classes; source parsing and saved-state rendering |
| Wavefront | `tools/test-wavefront-model.py` with both classpaths, main classes and `--model`; actual OBJ parsing and patch geometry |
| Baked meshes | `tools/test-baked-models.py` with both classpaths, main classes and all atlas-model resource names; poses, UVs, geometry and WitchingGadgets tint isolation |
| Mimic | `tools/test-inventory-mimic.py` with both classpaths and main classes |
| Renderer failures | `tools/test-renderer-failure.py` with both classpaths and main classes; failed initialization and unavailable client-class linkage |
| Patch capacity | `tools/test-patch-capacity.py` with both classpaths and main classes; actual perspective path beyond texture-slot capacity |
| Water | `tools/test-water-surface.py` with both classpaths, main classes and `--minecraft-sources`; actual RenderBlocks liquid-height golden |
| HydroEnergy | `tools/test-hydroenergy.py` with both classpaths and main classes |
| Wall mirror | `tools/test-wall-mirror.py` with both classpaths and main classes; source pixel-grid cuboids, palette, active/dummy neighbor state and four facings |
| Vanilla texture pixels | `tools/test-vanilla-texture-pixels.py` with both classpaths and main classes; real TexturePack construction/loadTerrain preserves native RP and legacy pixels, explicit consumers remain required |
| Model triangle API | `tools/test-model-triangle-bounds.py` with both classpaths and main classes; opt-in finite model coordinates and legacy bounds/default API compatibility |
| Walrus | `tools/test-walrus.py` with both classpaths, main classes and `--atlas`; source saved scales/facings and exact original/HD atlas padding |
| Dedicated-side aspect lookup | `tools/test-witching-dedicated-aspects.py` with both classpaths and main classes; classloader denies client IIconRegister |
| Techne | `tools/test-techne-baker.py --minecraft-jar` with the SRG golden and runtime classpath; exact Minecraft ModelBox positions/UVs, mirrored cuboids and nested transforms |
| Atlas | `python3 tools/test-atlas-mesh.py` |
| Texture loading | `python3 tools/test-texture-loader.py` |
| Vending updates | `python3 tools/test-vending-updates.py` |
| Cable geometry/colors | `python3 tools/test_cable_rendering.py` |
| Forge resource lookup | `python3 tools/test_forge_resource_lookup.py` |
| Pipe geometry | `python3 tools/test_pipe_geometry.py` |
| Static audit | `python3 tools/test_render_audit.py` |
| Reference ordering | `python3 tools/test_renderdata_references.py` |
| Nested tests | `python3 -m unittest discover -s tools/tests -p 'test_*.py'`; includes AE/IC2 registry gates, resource roles, Chisel variation audit and installed ExtraUtilities pixel golden |

The runner supplies the same explicit `--compile-classpath`, `--runtime-classpath` and `--classes` values to each harness that accepts them. The two previously fixed-path harnesses now also accept `--classes`.

## Recorded results and limits

An immutable e82c8d34 source/classes snapshot passed the original 18 harnesses. The follow-up CPU run used source b6db3859, copied e82c8d34 dependency/main classes and recompiled each harness's target production sources. All 19 harnesses passed, including the new dedicated-side aspect lookup. External Minecraft ModelBox, Minecraft RenderBlocks, installed OBJ and ExtraUtilities pixel fixtures were supplied. Notable results include 257 GregTech assertions, 6,760 vending assertions, 20,532 real perspective-intersection assertions, all 16 atlas models, all 36 saved input/output pose combinations where applicable, and 4,000 water-corner samples matching actual Minecraft source bit for bit.

The ordinary WitchingGadgets pose fixture substitutes aspect colors and therefore cannot detect dedicated-server linkage failures. The new dedicated-side test denies IIconRegister, first reproduces the old broad Block-method reflection failure, then executes the actual common-side aspect lookup for all 16 metadata states. This targeted classloader barrier complements the other CPU fixtures. A real Forge dedicated-server boot remains necessary because Minecraft/Forge side stripping and all mod initialization cannot be reproduced by a collection of classpath fixtures.

Full-pack headless startup, runtime resource closure, Chisel masks and final visual-grid rendering are separate fixture checks. Their final candidate validation was pending when this document was committed; successful CPU results do not assert visual parity or replace those checks.

The follow-up suite adds four harnesses, for 23 with the external walrus atlas supplied. The standalone mirror test passes 97,366 source pixel/palette/state assertions. The vanilla test passes 2,343 assertions through the complete production TexturePack constructor, including three native PNGs in both supported formats and explicit legacy/custom null-owner missing resources. It confirms the 013e279d inactive-variant skip leaves known vanilla pixels unchanged. The model-triangle API test passes 75 assertions. The final combined rerun including the imported walrus renderer remains pending until its candidate build is complete.

The texture-definition diagnostic harness adds a 24th CPU task: `tools/test-texture-definition-diagnostics.py` with both classpaths and main classes. Its actual-parser regression intentionally requests tile index 1 from a one-tile declaration, proves the following valid block is not parsed, checks exact file/line/reason diagnostics, verifies snapshot immutability and reset, and confirms valid files plus deliberate enabled/version guards remain error-free. All 18 assertions pass against production parser diagnostics from 417ebc49. An expected severe log is part of the negative fixture, not a suite failure.
