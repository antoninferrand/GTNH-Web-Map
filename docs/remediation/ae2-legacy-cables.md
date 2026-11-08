# AE2 legacy cable reachability

AE2 rv3-beta-1095-GTNH `ItemMultiPart.getTypeByStack` resolves the registered
metadata table and returns `PartType.InvalidType` for absent variants.
`PartType` currently registers dense/dense-covered cables, while the old
ultra-dense fields remain only in the legacy API and are no longer initialized
by `Registration`. Web Map still declares 34 images for those obsolete parts.

Check the installed part registry before resolving a cable recipe's textures.
Current absent variants never mark their images used; old packs that register
those metadata retain their mappings. Keep missing assets for real registered
parts visible. If the registry/API is unavailable, warn and retain declarations
conservatively. Reject damage outside the support array bounds.

This addresses 34 baseline ultra-dense resource references by reachability,
not by swapping them for visually incorrect ordinary dense cable images.
