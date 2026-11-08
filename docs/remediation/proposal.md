# Proposal: complete GTNH 2.9.0 rendering support remediation

Scope: address every one of the 157 unsupported GregTech MTE variants and
637 missing texture references from the 2026-11-08 audit, plus the geometry,
state, connected-texture and multi-block overlay defects needed to correct
the reported tin pipes, coke oven and vending machine appearance.

This is a proposal. The fork remains unchanged at `e0cb1f30`; no implementation
commits, pushes or deployment changes have been made.

## Complete, traceable backlog

Every baseline entry has a work package, evidence and next action:

- [157 MTE variants](baseline/mte-remediation-backlog.csv)
- [637 texture references](baseline/texture-remediation-backlog.csv)

The proposal uses the live registry dump, installed JAR asset inventory and
source tags matching installed GregTech 5.09.54.265, CropsNH 2.0.140 and
VendingMachine 0.4.103. Candidate resource replacements in the CSV are evidence
to investigate, not approved substitutions based solely on similar filenames.

### Texture work packages

| Package | References | Proposed treatment |
| --- | ---: | --- |
| GregTech basic-machine overlays | 222 | Verify their source `customOptional` declaration and represent absent optional layers correctly, retaining the casing and resource-pack overrides. |
| GregTech explicitly optional iconsets | 48 | Matching `Textures.java` declarations use `createOptional`; encode optionality rather than treating the absent image as a required block texture. Check the consuming machine's effective layers. |
| Legacy crop textures | 207 | Audit old IC2/GT++ crop references against installed crop registries and existing CropsNH renderer data. Migrate reachable references; retire entries only when proven obsolete. |
| Material texture fallback | 12 | Resolve the actual material texture first, then GregTech's `NONE` fallback where its texture contract permits. Include the correct asset-owning JAR. |
| Filename case | 6 | Correct the exact declared paths; retain case-sensitive resource lookup. |
| Existing fork fixes | 3 | Verify the current Ginkgo paths against installed assets and credit existing commit `e0cb1f30`. |
| Mod-specific source/model review | 139 | Trace each reference to the actual installed renderer; correct moved textures, atlas coordinates or generated/custom models, or retire verified unreachable references. |
| **Total** | **637** | Every entry gets a recorded resolution and validation evidence. |

The 207 crop references consist of 197 old IC2 paths and ten GT++ Force/Hemp
paths. Installed CropsNH contains candidate stage images for 194 of them,
including case changes and Liveroots/Oilberries name changes. Quantaria (4),
Rape (4) and Force (5) need explicit registry/source review. Existing
`CropsNHRenderer` and crop definitions already exist; reuse and verify them
before introducing duplicate mappings or assuming all legacy entries matter.

The six case corrections include three GT++ active textures, two TecTech GUI
overlay images and the OpenModularTurrets inventory expander texture.

The remaining 139 references are fully enumerated in the CSV. They include
35 AE2, 30 GregTech, 16 ExtraUtilities, 15 dreamcraft, 14 WitchingGadgets,
seven Blood Magic, four Computronics and smaller groups in other mods.
Specific investigation routes:

- AE2 ultra-dense cable definitions: check current part registration and cable
  rendering; determine whether images moved, are derived, or describe removed
  parts. Do not substitute ordinary dense cables without verifying the design.
- ExtraUtilities compressed blocks: inspect how the installed mod produces
  their images; implement the equivalent texture processing if no standalone
  PNG exists. Verify compression levels individually.
- Dreamcraft/GT++ panels and casings: trace current casing registrations and
  icons, including moved assets and obsolete metadata entries.
- Blood Magic, ThaumicBases, Tinkers' Defense, ae2fc and Et Futurum
  `MISSING_ICON_BLOCK_*` entries: these are scraper placeholders, not real
  asset names. Inspect the real model/render path and block state.
- BloodArsenal, ThaumicMachina, WitchingGadgets and MalisisDoors: distinguish
  block textures from model atlases and custom meshes. A similarly named GUI
  image is not sufficient evidence for a replacement.
- HydroEnergy's doubled `.png` GUI-derived path, Computronics numbered image
  references, transparent sentinels and the remaining individual records:
  trace the consuming renderer and correct its actual resource/model contract.

These are investigation routes, not yet proven diagnoses for every residual
record. Their work packages remain open until individually validated.

### Unsupported MTE work packages

| Family | Variants | Work |
| --- | ---: | --- |
| CropsNH machines | 55 | Five tiered single-block families plus industrial farm; preserve tier casing, side/top/front and active textures. |
| Compressed input/output buses | 16 | Correct tier casing, bus overlays, facing and multi-block casing selection. |
| Pattern-provider hatches | 5 | Tier and ME front/active appearance. |
| Extrusion hatches | 4 | Tier, casing and state-specific overlays. |
| Black-plutonium cables | 6 | Registry-derived thickness, material tint, insulation and connection geometry. |
| Nanochip complex, modules, hatches and pipe | 16 | Controller/module textures and state; inspect vacuum-pipe geometry and connections separately. |
| Beamcraft and toxic-residue sensor | 7 | Controllers, beam components and hatch/sensor textures; audit orientation and any special render behavior. |
| TecTech machines/hatches | 17 | BEC family, multi-dynamo hatches and debug devices, with actual casing/state behavior. |
| BartWorks machines | 2 | Acid generator and creative scanner. |
| KubaTech hatches | 2 | Electrode and electrode-detector hatches. |
| Vending controller/uplink | 2 | Controller/hatch mappings plus display overlays and casing behavior described below. |
| Other core/GT++ machines | 25 | Coke oven/controller, mass solidifier, PCB machines, spinmatron, drawer framers, foundry, newer multiblocks, utilities and creative storage; each is identified in the CSV. |
| **Total** | **157** | All 75 Java classes receive source review; shared families reuse implementation. |

Most entries should be declarative texture/model records using shared renderers.
The exact number requiring new renderer code is not established yet. Review
each class and its inherited behavior before assigning a generic cube.

## Implementation approach

1. **Establish the target and audit harness.** Pin the development fixture to the
   installed pack versions, record baseline data and asset hashes, and verify a
   clean build before changing rendering. Add an audit that joins registry IDs,
   MTE IDs, loaded support records, texture/icon-set references and actual asset
   resolution. Track the effective records after overrides, not only raw lines.
   Validate duplicate definitions and invalid references as well as missing ones.

2. **Repair shared resource semantics.** Add explicit required/optional/fallback
   behavior to texture support data and resolution. Preserve texture-pack
   precedence, material tint, overlays and alpha behavior. Required missing
   assets remain errors. Optional absence is classified only with evidence from
   the mod's contract; it must not erase the base face. Use source assets rather
   than copying hundreds of raster images into Web Map.

3. **Repair mapping data by owning mod.** Apply verified exact paths, crop
   migrations and source-proven replacements. Preserve valid old-pack behavior
   with version guards when practical. Resource domains and owning mod JARs
   must both be correct. For removed entries, prove that no installed registry
   variant or active renderer references them before retiring their data.

4. **Extend common GregTech rendering where necessary.** Support additional
   saved state, casing indices/pages, facing rules and texture layers required
   by the reviewed classes. Inspect existing mapped machines as regression
   controls. For pipes/cables, validate all sizes, connection masks, isolated
   pieces, tint and insulation. The known huge tin pipe size mismatch and
   zero-sized isolated center geometry are included here.

5. **Complete all 157 MTE records in family-sized batches.** Derive tiers,
   material properties and common textures from the verified registry/source.
   Use a small declarative generator for repeated tiers where useful, with
   reviewable generated output. Add adapters for state-dependent families
   instead of pretending one static snapshot describes every state. Rendering
   threads must use saved/cached data; do not invoke client-only rendering
   methods on the dedicated server.

6. **Implement visual behavior beyond coverage.** Coke oven hatch mode must
   select input/item-output/fluid-output overlays from `inputMode`, correctly
   oriented on its fixed coke casing. Add controller active/inactive textures.
   Vending support must include its casing, active/inactive controller/uplink,
   connected casing edges and five graphics spanning neighboring faces. Derive
   the display from saved controller state/orientation; account for controller
   changes invalidating neighboring map tiles. Use a stable frame for animated
   textures unless animation support is deliberately added. Validate emission
   and transparency. These are required parts of correcting the reported design.

7. **Close every backlog entry with evidence.** Re-run the registry/resource
   audit in the fixture, compare renders and produce a final before/after
   report. Record each texture as corrected, correctly optional, resolved by
   fallback, already fixed or proven obsolete. Any unknown remains open.

## Commit strategy

Retain the requested separate commits and no-push constraint. Start with one
baseline/audit commit, then commit each independently validated fix: resource
fallback, optional-layer semantics, filename corrections by mod, crop migration,
each residual mod's repairs, shared pipe/state fixes and each MTE family.
Split families further where they contain independent renderer fixes, including
coke oven modes and vending overlays/connected textures. Do not combine all
637 references or all 157 records into one opaque data commit.

Every implementation commit should state the affected backlog IDs/resources,
what evidence established the intended behavior and what validation passed.
Mark entries already fixed by HEAD against their existing commit rather than
creating redundant fixes. Work in `~/Projects/GTNH-Web-Map`; do not modify or
restart the live server as part of implementation validation.

## Completion criteria

- All 157 baseline installed MTE variants have valid, meaningful support;
  adding empty records does not count. The full installed registry audit finds
  no new unexplained gaps introduced by version alignment.
- Every one of the 637 texture references has an evidenced resolution;
  unexpected required-resource failures are zero. Known optional absence and
  unreachable historical definitions are reported separately and do not hide
  required failures.
- A parameterized fixture verifies all new IDs at least once, all distinct
  rendering families, and meaningful mode/facing/active/paint/connection states.
  Existing supported machines and resource-pack overrides serve as regressions.
- In-game versus Flat/Surface comparisons verify tin pipes, all coke hatch
  modes, vending casing edges, controller/uplink and the assembled display.
  Test both loaded chunks and saved/reloaded chunk data.
- A dedicated-server smoke test catches client-class loading and missing-NBT
  failures. Tests exercise optional layers and dynamic textures, including
  deterministic frames for animated assets.
- Final commits remain local and unpushed. Deployment and production tile
  re-rendering are subsequent operations; this proposal does not include them.

The large uncertainty is the 139 mod-specific references and the visual behavior
of the 75 MTE classes. Those are explicitly included in the work, with no claim
that the two original headline counts alone measure complete visual correctness.

See the [initial investigation](initial-investigation.md) for source links
and the live-server evidence behind the reported example defects.
