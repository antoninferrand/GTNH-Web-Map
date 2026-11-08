# Read-only Chisel variation registry audit

`gwm renderaudit` now includes `chiselVariants`, keyed by the actual `chisel:block` registry name. For blocks with a `carverHelper`, it reads the helper's `infoMap` without requesting client icons or touching a world. Each successful row reports the concrete block/helper classes, array length, nonnull metadata bit mask and matching metadata list. Empty helpers explicitly report mask zero. Blocks without a helper, null helpers, missing blocks and unsupported reflection layouts have distinct status/diagnostic fields.

Source: Chisel 2.17.34-GTNH `team/chisel/block/BlockCarvable.java` declares `carverHelper`; `com/cricketcraft/chisel/api/carving/CarvableHelper.java` stores `IVariationInfo[16] infoMap`. Its icon access chooses the missing texture for a null variation. Inherited helpers and package-private helper fields require walking superclasses. Side-specific overrides such as bookshelf top/bottom remain a separate source review; a nonnull mask alone does not establish face texture parity.

Validation: `python3 tools/tests/test_chisel_variant_audit.py` compiles the production reflection methods and covers inherited/private fields, metadata bit 15, matching lists, empty helpers and every unavailable-helper status. The existing schema-2 resource roles, raw outcomes, base-definition checks and unresolved icon-set fields are preserved.
