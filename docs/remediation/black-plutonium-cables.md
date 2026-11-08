# Black-plutonium insulated cables

The installed registry identifies MTE IDs 11396–11401 as `MTECable`, named
`cable.blackplutonium.01/.02/.04/.08/.12/.16`, with diameters 250/375/500/625/750/875.
Add all six to the existing insulated-cable renderer using `gt_cable`.

GregTech 5.09.54.265 `MTECable.getTexture` uses `INSULATION_FULL` for side
surfaces, tinted by `Dyes.CABLE_INSULATION`. The installed default client config
is RGB 64/64/64 (`404040`); use the same default as existing cable definitions,
not the material's `323232` wire tint. Existing dye overrides remain supported.

The required insulation PNG exists in the installed GregTech asset inventory.
The offline audit verifies six new meaningful records with the recorded sizes.
The shared cable renderer's existing simplified end-face appearance remains a
visual validation concern; this mapping change does not claim a new cable model.
