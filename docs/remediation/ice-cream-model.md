# Ice cream machine OBJ model

GT5 5.09.54.265 MTEIceCreamMachine explicitly returns an empty texture array
for its block cube. Its TESR binds `textures/model/ice_cream_machine.png`, draws
`ice_cream_machine.obj`, translates by(0.5,1.0,0.5) and rotates according to
mMainFacing: NORTH90, EAST0, SOUTH270, otherwise180 degrees.

Use those actual installed resources and transforms through a CPU-only OBJ
adapter. Retain explicit per-vertex UVs and source winding, triangulate convex
faces, and retain the source's height above one block. No client renderer,
Minecraft client singleton, VBO or OpenGL methods are linked by this adapter.
The machine's atlas is not treated as a single cube face.

GregTechModelRenderer delegates every other MTE to GregTechMachineRenderer,
including its state, hatch and vending behavior. An unavailable model emits
a required model-loading error while unrelated machines remain usable.

Validation exercises negative OBJ indices, quad triangulation, all four yaw
rotations, UV-preserving real patch allocation and malformed input. The actual
installed-version source OBJ is also tested; no model/texture assets are copied
into Web Map. Rendering-resource loading and visual appearance still need the
isolated pack fixture comparison.
