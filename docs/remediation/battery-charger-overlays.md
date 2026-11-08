# Tier-specific battery charger output faces

Runtime audit found fourteen registered chargers whose mappings used a texture
symbol, `OVERLAY_ENERGY_OUT_POWER`, as an undeclared iconset name. IDs 690–699
and 11280–11283 now reference explicit tier-specific iconsets in both historical
GregTech and dreamcraft files.

The source contract is `LoaderMetaTileEntities.registerCharger4x4` in installed
GregTech 5.09.54.265. Despite the historical `batterycharger.16.tier.*` names,
every constructor receives **four** inventory slots. `MTECharger` inherits
`MTEBasicBatteryBuffer.getTextureSet`: a four-slot inventory selects
`OVERLAYS_ENERGY_OUT_MULTI_4A[mTier + 1]`; `getTexture` uses that overlay only on
`mFacing`. The other faces keep the painted tier hull. These are not the 16A
icons suggested by the old names.

All fourteen tier PNGs (ULV through UXV) exist in the installed JAR. The explicit
records retain the source `createOptional` contract and resource owner
`gregtech`; no arbitrary power-symbol alias or required-image suppression is
introduced. Existing hull/paint behavior remains intact.

Production parser/renderer fixtures load the actual charger records and verify
resolved iconsets plus front/back layers on all six facings for every tier.
Shared/coke/substation/charger fixtures pass 439 assertions; vending passes
6,760. A subsequent full-pack audit must confirm zero unresolved iconset names.
