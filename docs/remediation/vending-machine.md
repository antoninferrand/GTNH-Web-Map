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
