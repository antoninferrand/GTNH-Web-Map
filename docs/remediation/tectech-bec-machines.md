# BEC block appearances

Installed GT5 5.09.54.265 source establishes ten missing MTEs:

- Controllers15754/55/56/57/60 use MTEBECMultiblockBase: fixed casing,
  BEC_CONTROLLER_BACKGROUND on mFacing and an additional glowing active icon.
  Casings are ElectromagneticallyIsolatedCasing, FineStructureConstantManipulator,
  CoherencePreservingPlasmaConduit, CoherencePreservingPlasmaConduit and
  ElectromagneticallyIsolatedCasing respectively.
- BEC hatch15476 and line-of-sight connector15481 inherit MTEBaseFactoryHatch:
  UIV hull/formed casing plus color-modulated OVERLAY_EM_D_SIDES or ACTIVE and
  EM_DATA_CONN. Preserve all layers rather than replacing the casing.
- Detectors15758/59/61 use UIV casing and their named normal or glowing active
  icons, as getTexturesActive/Inactive specifies.

BlockCasingsBEC registers metadata0-6 and casing page17, slots0-6. Add these
seven normal block mappings too: controller and generic hatch bases must resolve
actual casing designs. Every declared exact image exists in the installed asset
inventory. Vertical animation sheets declare ycount and use deterministic frame0.

This is static block-appearance support. The line-of-sight connector's transient
beam between distant machines is a separate world effect, not stored block mesh.
Visual fixture validation must compare facing, paint and active states.
