# Failed custom renderer lifecycle

The isolated pack fixture exposed a secondary boot failure when a custom model
could not initialize: CustomBlockModel logged the primary error and set render
null, but getTextureCount then dereferenced it. A stale tile-field request bit
could also dereference a failed replacement during chunk snapshots.

Keep the existing explicit error and empty-mesh fallback, and return zero texture
count / no tile fields for the failed renderer. This prevents a model failure
from crashing unrelated map initialization; it does not make the failed model
successful. The original vending initialization-order issue is fixed separately.

The production-class regression replaces a working tile-field model with a
missing renderer and verifies both initialization and snapshot-field access.

Dedicated-server cycle5 exposed a missing client class through reflective
method discovery in a new custom renderer. Its source call is corrected
separately. Model initialization now also records `LinkageError` failures
(including `NoClassDefFoundError`) using the same severe diagnostic/failed-model
path as reflection exceptions. This keeps the server and audit available while
preserving a visible failure; it does not classify that model as supported.
The production regression throws the exact missing-client-class error during
initialization and checks the failed-model audit entry and empty mesh.
