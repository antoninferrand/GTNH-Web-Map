# Vending machine appearance

Basic mappings target VendingMachine 0.4.103. The casing block is registered by
registry name, and the controller (2741) and ME uplink (2742) use their real
fixed casing and inactive/active front textures. Controller glow samples frame
zero of its four-frame image. A per-file GregTech support loader declaration is
required because the texture parser does not share loaders across files.

All required paths were checked against the installed JAR inventory. These base
mappings are one independent fix; full assembled appearance also requires
connected casing edges and neighbor-spanning display graphics, implemented
separately rather than claiming basic cube coverage is visual parity.

## Connected casing and assembled display

`VendingMachineRenderer` splits each cube face into four patches with the same
full-face UV coordinates. It selects the appropriate quadrant from the mod's
five `compact_expanded` images, preserving isolated borders, joined edges,
centers and diagonal inner corners. This uses source assets directly, without
baking replacement PNGs into Web Map. The independent fixture reference covers
all 256 neighbor masks and all 47 client expansion variants; it includes the
client's tile-41 corner behavior.

Casing joins use the actual tile contract: GregTech `BlockMachines.getIcon`
returns an `ICasingTextureProvider` casing icon for connection tests, including
the controller's front, even though its visible front is a different texture.
The controller and optional uplink therefore join neighboring vending casings.

The five display graphics are discovered from neighboring saved controller
`mID`, `mFacing` and `mActive` fields. Their offsets match `OverlayHelper`'s A/B
coordinates; the fixture verifies them against actual StructureLib
`ExtendedFacing.getWorldOffset` for all four valid horizontal facings. The
optional ME uplink may occupy any display position, so its ordinary front layers
are combined with CTM and the multi-block display rather than losing the display
where a hatch is installed. Inactive or removed controllers draw no graphics,
matching the client's `onPostTick` cleanup. Active animations use frame zero;
parts 0–3 are emissive and part 4 uses normal lighting.

`tools/tests/vending-compact-reference.csv` records the quadrant results derived
from Angelica's `TileOverride.neighborMap` and `CTMTextureGenerator` at commit
`d69fd20a8b6569a510504e392f475efc847cf965`. The production topology implementation
is tested against that independent reference, including its tile-41 exception.
Source reference: https://github.com/GTNewHorizons/Angelica/blob/d69fd20a8b6569a510504e392f475efc847cf965/src/main/java/com/prupe/mcpatcher/ctm/CTMTextureGenerator.java

The vending JVM fixture passes 6,758 assertions: all masks on all faces/quarters,
actual immutable mesh UV bounds, all display positions/orientations on both
casing and uplink blocks, emissive layer selection, inactive/removed/invalid
controller cleanup and the tile-connection contract. It does not replace the
final in-game/Flat/Surface screenshot comparison or block-update integration
smoke test.

## Neighbor updates and server state changes

A changed casing or a changed block beside a vending casing now invalidates a
two-block map volume, covering both the connected edges and all five display
positions even when a controller is removed or a casing is replaced. The
adjacency probe checks only the six cardinal neighbors and returns air for
unloaded chunks/out-of-height coordinates; it never loads chunks to inspect
neighbors.

GT5 5.09.54.265 sends active/facing changes through `GTBlockEventTracker` custom
packets, which do not reliably invoke vanilla `markBlockForUpdate`. To observe
those changes, each world tracker compares detached active/facing values for
loaded vending controller/uplink entities every 20 server ticks. It scans the
loaded tile-entity list on the main server thread, reads only the two vending
MTE types, and serializes no NBT. A change or disappearance invalidates the
relevant chunk snapshot and display volume. Unchanged values cause no work.
Only coordinates and integer state values are retained, with the existing
per-world tracker lifecycle; no live tile entities or world references are
stored in the state cache.

`tools/test-vending-updates.py` passes 54 assertions for bounded probes, disabled
mod behavior, controller removal, activation/rotation/replacement state changes,
unchanged values, detached snapshots and the complete display dependency radius.
The production plugin and server adapter also compile against the actual mod
classpath. A full dedicated-server smoke test still needs to confirm state
change scheduling and render completion in the integration fixture.

The dedicated fixture exposed model initialization occurring before
`GWM_Util`'s texture-phase registry initialization. The casing renderer now
defers its default machine-block lookup until actual rendering; initialization
requires no texture registry. The JVM fixture explicitly initializes the model
with a null registry and checks successful rendering after registry setup.

The static casing mapping provides all 24 quarter-patch fallback entries, matching
its custom renderer's declared texture count. This avoids the texture loader's
insufficient-face-definition error while the dynamic mapper supplies CTM/display
layers for the same patch IDs.
