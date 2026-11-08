# Coke oven controller and hatch

GT5 5.09.54.265 source `MTECokeOven.getTexture` uses coke oven casing and
inactive/active controller overlays with an optional glow layer. Support for MTE
236 follows that source, preserving per-layer emissive modifiers. The inactive
glow PNG is intentionally absent in the installed assets and is declared
optional, matching `Textures.BlockIcons.createOptional`.

`MTEHatchCokeOven` (237) saves `inputMode` as a byte: 0 input, 1 item output,
2 fluid output. Its front combines PIPE_IN/PIPE_OUT with ITEM_IN_SIGN,
ITEM_OUT_SIGN or FLUID_OUT_SIGN. Those indicator layers match the installed
client's default `Client.Render.Hatch Indicators=true`; a user-specific client
preference cannot be inferred from server chunk data.

The hatch's casing is always `COKE_OVEN_CASING`, independent of neighboring
multi-blocks or generic hatch casing fields. `inheritcasing=false` and
`paint=false` express that contract. Missing or invalid mode data falls back to
input mode. The shared state-field mechanism requests `inputMode` from saved
chunk NBT, and its texture selector works for all six valid facing ordinals.

The production renderer fixture loads these actual mapping declarations and
checks all modes/facings, signs, fixed casing, absent/invalid mode, requested NBT,
and controller inactive/active glow chains. With shared cases included it passes
84 assertions. All required PNG paths were checked against the installed JAR
asset inventory; final in-game comparison remains part of integration validation.
