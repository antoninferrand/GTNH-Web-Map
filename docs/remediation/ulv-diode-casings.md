# ULV diode casing references

The installed full-pack runtime audit (3,412 registered MTEs) found five existing mapped machines without any base texture: 12617, 12633, 12649, 12665 and 12681 (2/4/8/12/16 amp ULV diodes). Their records referenced nonexistent `hull.tier.-01`. Use existing `hull.tier.00`, which provides the ULV side, top and bottom textures.

Source: GT5-Unofficial 5.09.54.265 `bartworks/common/loaders/ItemRegistry.java:312` constructs each diode with tier `i`, including ULV `i=0`. `MTEDiode` extends `MTEBasicHull`; the inherited `getTextureSet` selects `Textures.BlockIcons.MACHINE_CASINGS[mTier]`. The five ULV records now reference the same tier-zero casing as other ULV machines.

Validation: all five records occur exactly once, all now resolve to the existing tier-zero icon set, and no negative-tier base references remain. Runtime confirmation will use the next isolated full-pack fixture cycle. This does not claim visual parity for the existing family's energy output overlays.
