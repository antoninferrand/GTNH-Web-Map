# Pipe geometry

The installed registry dump reports MTE 5593 (huge tin item pipe) with thickness
0.875. Its old mapping used size 1000 and rendered a full cube; use size 875.

GregTechMachineRenderer initializes the shared pipe generator with a zero-sized
center fitting. With no connections, it previously generated a degenerate box.
Use the pipe radius for the isolated center while preserving existing fitting
sizes and all connected meshes. Mask saved connection flags to the six direction
bits before indexing the 64-entry mesh array.

These changes address geometry independently of texture-resource fallback.
Dedicated-server rendering and visual comparisons remain part of final fixture
validation; geometry behavior is tested with an isolated renderer harness.
