# GTNH Web Map block support investigation

Investigated 2026-11-08 against the running server. Fork cloned to
`~/Projects/GTNH-Web-Map`, HEAD `e0cb1f30`. No renderer, deployment or server
configuration changes were made; the diagnostic `/gwm gtdump` was run.

## Findings

Server versions: GTNH 2.9.0, Web Map 0.4-beta-1,
GregTech 5.09.54.265, VendingMachine 0.4.103.
The fork's `renderdata/gregtech-texture.txt` is byte-for-byte identical to the
file inside the deployed Web Map JAR. Merely deploying the fork will not fix
these GregTech mappings. Its development dependencies still target
GregTech 5.09.52.594.

| Example | Confirmed cause | Required work |
| --- | --- | --- |
| Tin item pipes | MTE IDs 5589–5598 are already mapped, but reference the absent `materialicons/DULL/pipeSide.png`. Startup logs confirm the failed resource lookup. | Resolve the texture through GregTech's material texture fallback; retain the existing pipe renderer. |
| Vending casing | `vendingmachine:casing` has no render definition. Its saved registry block ID is 11357, but definitions must use the registry name. | Add a mod-scoped texture definition using the vending JAR's `vending_machine_casing.png`. |
| Vending controller / ME uplink | MTE IDs 2741 and 2742 have no mappings. | Register casing, inactive/active front textures and machine records. |
| Coke oven hatch | MTE ID 237 has no mapping. Its casing texture already exists. | Add the MTE record, coke casing and front overlays selected using `inputMode` and facing. |
| Coke oven controller | MTE ID 236 is also unmapped. | Add its casing and inactive/active front overlays at the same time. |

### Tin pipe details

The installed GregTech JAR was checked directly:

```text
absent:  assets/gregtech/textures/blocks/materialicons/DULL/pipeSide.png
present: assets/gregtech/textures/blocks/materialicons/NONE/pipeSide.png
```

GregTech's `GTTextureSetBlockIconContainer` explicitly falls back to the
`NONE` texture set. Web Map's `TexturePackLoader` searches resource packs,
mod resources and its bundled resources, but does not implement this material
fallback. The same failure appears for METALLIC, SHINY, ROUGH, NETHERSTAR and
CUSTOM/awakeneddraconium pipe textures. This is broader than tin.

A targeted data fix can keep the current texture IDs and change their filenames
to the verified fallback PNGs. A more durable loader fix should try the original
resource first, then the appropriate `NONE` resource, and preserve resource-pack
precedence. Do not apply this rule indiscriminately to all missing PNGs:
GregTech also has optional icons, overlays and other fallback behavior.

Keep tin's `DCDCDC` color multiplier. Also correct huge tin pipe ID 5593:
the mapping says `size=1000`, while the installed registry dump reports
thickness `0.875`, requiring `size=875`.

### Coke oven details

`MTEHatchCokeOven.getTexture()` uses `COKE_OVEN_CASING` and selects its front
using the saved `inputMode`: 0 = item input, 1 = item output, 2 = fluid output.
It can add item/fluid indicator layers according to the client setting.

The existing `GregTechMachineRenderer` does not request or use `inputMode`.
A static mapping fixes the black casing; mode-accurate rendering needs an
additional requested NBT field and a texture-selection branch or a suitable
extension to the machine support records. Because this hatch supplies a fixed
coke casing itself, do not rely exclusively on generic hatch casing inference.

The existing icon-set format has a `front` field oriented using `mMainFacing`,
and an `output` field oriented using `mFacing`. Place overlays according to the
actual block's facing semantics. In particular, a hatch overlay can use the
existing `output` mechanism; a new `front` icon alone is insufficient validation.

### Vending machine details

Add `renderdata/vendingmachine-texture.txt`, scoped to `modname:vendingmachine`,
so assets are resolved from the vending JAR. Add:

- The normal casing block mapping.
- GregTech support entries for controller 2741 and ME uplink 2742.
- Casing, inactive/active controller front and inactive/active uplink overlays.
- Casing registration for generic hatch texture inference where appropriate.

These changes restore the basic block appearance. Full client parity is a
separate step: `OverlayHelper` places five graphics on neighboring casing faces
around the controller, using the controller orientation and active state.
The casing also uses a `compact_expanded` connected-texture definition.
Web Map needs neighbor/controller-aware rendering to reproduce those graphics
and connected casing edges. A plain cube texture mapping cannot provide them.

## Determine what is missing

Use three complementary checks; none alone proves visual correctness.

1. **Registry coverage.** Set `dump-missing-blocks: true` in
   `dynmap/configuration.txt` for a future approved restart. Web Map then logs
   `Blocks missing texture definition: ...`. The implementation only reports a
   block ID when *all* of its 16 metadata entries are blank. It does not detect
   partial metadata coverage or missing GregTech MTE variants.
2. **GregTech variant coverage.** Run `/gwm gtdump` from the server console.
   It generates `gwm-gt-dump.txt` and `gwm-gt-dump-unhandled.txt` in `/opt/gtnh`.
   The latter tests whether an MTE support entry exists; it does not validate
   the entry's texture assets, geometry, orientation or overlays.
3. **Resource resolution.** Collect Web Map's `Resource ... for mod ... not found`
   messages from `logs/fml-server-latest.log`. These catch mapped blocks such as
   tin pipes whose textures fail to resolve. Classify optional/moved/fallback
   resources before treating each message as a visible defect.

Current results:

- 3,412 registered GregTech MTE variants; **157 without support entries**.
- **637 unique missing texture-file references**, including 503 under the
  GregTech loader. These are resource references, not counts of distinct blocks.
- Twelve missing GregTech references have a corresponding `NONE` PNG in the
  matching GregTech source tag: six pipe-side, three block and three frame
  references. Other missing references need separate classification.
- The saved world registry contains 4,258 block entries. This is registry
  inventory, not a count of block types currently placed in the world.
- No local custom texture/model definitions were found at the top level of the
  server's `dynmap/renderdata`; that directory contains README and modsupport.

Full machine-readable findings:

- [Unsupported GregTech variants](baseline/unsupported-gregtech.csv)
- [Missing texture resources](baseline/missing-textures.csv)

The unsupported list includes the coke oven and vending machines, CropsNH
machines, compressed buses, pattern-provider hatches, cables and newer
multiblocks. These are installed variants, including ones that might never be
placed in this world. To prioritize by actual impact, scan an existing world
backup's chunk NBT for block registry ID + metadata + MTE `mID`, recording counts
and sample coordinates, then join that inventory with these coverage findings.
Do not create an extra full-world render merely to collect this inventory.

For repeatable auditing, add a `/gwm renderaudit` command to the fork that exports
effective texture coverage per registry name/metadata, missing MTE entries,
invalid texture/icon-set references and unresolved resources to CSV/JSON.
Report intentionally invisible and uncertain/custom-rendered blocks separately.
Use loaded mappings and selected resource-pack resolution, not just a text-file
search. A visual fixture is still necessary to detect the wrong design.

## Implementation and validation order

1. Fix verified pipe material fallbacks and tin size 5593. Add focused regression
   checks for successful fallback, preference for real material textures and
   resource-pack overrides. Check straight, corner, junction and isolated pipes.
   The shared pipe generator currently receives a zero-sized center box; audit
   isolated pipes separately because zero connections may yield no visible mesh.
2. Add coke oven controller/hatch records and mode-aware hatch rendering. Verify
   all hatch modes, horizontal/vertical facings as permitted and active states.
3. Add vending casing/controller/uplink mappings. Validate basic textures first,
   then implement the multi-block face overlays and connected casing appearance.
4. Add the structured audit and triage the remaining installed variants and
   resource errors. Update development dependencies to match the target pack
   deliberately; regenerating simple mappings does not recreate custom renders.

Build and test in an isolated GTNH 2.9.0 fixture world. Compare the same blocks
in-game and on Flat/Surface maps, at useful zoom levels, including reload from
saved chunks. Verify no new texture errors. Only then publish a versioned fork
artifact and update the URL, filename and SHA-256 in
`ansible/roles/services/gtnh/tasks/web-map.yml`; ensure the previous mod JAR is
removed when switching filenames so both versions are not loaded.

Applying the JAR requires an explicitly approved graceful Minecraft restart
under this repository's operational policy. After that, radius-render the
fixture/base area first; existing tiles retain old pixels until re-rendered.
Use a wider render only after the result is verified. Do not use `dynmap reload`.

## Source references

- [Fork mapping definitions](https://github.com/antoninferrand/GTNH-Web-Map/blob/e0cb1f30/src/main/resources/renderdata/gregtech-texture.txt)
- [Web Map machine renderer](https://github.com/antoninferrand/GTNH-Web-Map/blob/e0cb1f30/src/main/java/org/dynmap/hdmap/renderer/GregTechMachineRenderer.java)
- [Web Map texture loader](https://github.com/antoninferrand/GTNH-Web-Map/blob/e0cb1f30/src/main/java/org/dynmap/hdmap/TexturePackLoader.java)
- [GregTech diagnostics](https://github.com/antoninferrand/GTNH-Web-Map/blob/e0cb1f30/src/main/java/org/dynmap/modsupport/gregtech/GregTechSupport.java)
- [GregTech material fallback, installed version](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/client/iconContainers/blocks/GTTextureSetBlockIconContainer.java)
- [Coke oven hatch, installed version](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/metatileentity/implementations/MTEHatchCokeOven.java)
- [Vending controller, installed version](https://github.com/GTNewHorizons/VendingMachine/blob/0.4.103/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java)
- [Vending multi-block overlays](https://github.com/GTNewHorizons/VendingMachine/blob/0.4.103/src/main/java/com/cubefury/vendingmachine/util/OverlayHelper.java)
