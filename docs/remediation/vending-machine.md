# Vending machine appearance

Basic mappings target VendingMachine 0.4.103. The casing block is registered by
registry name, and the controller (2741) and ME uplink (2742) use their real
fixed casing and inactive/active front textures. Controller glow samples frame
zero of its four-frame image. A per-file GregTech support loader declaration is
required because the texture parser does not share loaders across files.

All required paths were checked against the installed JAR inventory. These base
mappings are one independent fix; full assembled appearance also requires
connected casing edges and neighbor-spanning display graphics, implemented
separately rather than claiming basic cube coverage is visual parity.
