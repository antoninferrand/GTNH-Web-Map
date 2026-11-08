# Opt-in model triangle coordinates

`RenderPatchFactory.getModelTriangleExplTexCoords` has the same arguments as the existing explicit triangle API. Its interface default delegates to that API so existing third-party implementations do not acquire a new abstract method. `PatchDefinitionFactory` provides a model-specific implementation that rejects nonfinite positions/UVs, degenerate normals and null visibility, while accepting finite model vertices beyond the block-patch `[-1,2]` range. Finite explicit UV values are preserved exactly, including existing wrapping semantics.

Legacy patch and rotation APIs retain their bounds checks. The existing explicit triangle method already bypassed those checks; this change adds an explicit, validated model contract rather than modifying legacy methods. Callers must transform vertices before requesting the model triangle. Passing an overhanging result to the old rotation API still applies the old range validation.

The immediate source model is AE2 Fluid Crafting's walrus statue: ordinary scale-one geometry extends to Z=-1.06773 in one facing and Z=2.06773 in its opposite facing. Saved scales can range from 0.03 to 256. This API permits that geometry to be represented. It does not change ray traversal, owner-block selection or invalidation for geometry extending into other cells.

`tools/test-model-triangle-bounds.py` compiles the actual interface and factory and passes 75 assertions: scale 0.03/1/16/256, exact vertices and UVs, all 15 position/UV entries with NaN and both infinities, degenerate rejection, null visibility, legacy block/rotation range rejection and interface-default delegation to an old-style implementation.
