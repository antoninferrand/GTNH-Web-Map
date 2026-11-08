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
