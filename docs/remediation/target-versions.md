# Target development versions

Align compile/runtime GregTech, AE2, GTNHLib, ServerUtilities, ModularUI2,
Postea, EndlessIDs and Hodgepodge with the installed GTNH2.9.0 JAR inventory.
Include matching CropsNH2.0.140 and VendingMachine0.4.103 runtime fixture mods.
IC2 remains2.2.828-experimental; ModularUI remains1.3.4.

The original baseline compiled successfully before the changes. The aligned
production Java sources compile successfully with the repository's Gradle9.4
and downloaded Java25 toolchain. Development dependency alignment does not
modify the live server or establish visual fixture parity by itself.
