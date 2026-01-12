# Priority 2 (v0.2) - Implementation Drop

This branch implements the **big building blocks** described in `future.txt` under Priority 2 (v0.2):

- Advanced CPU vs GPU detection primitives (tick vs render time + OS load sampling).
- Deep scenario tracking (30s sliding window, hostile mob proximity, block place/break rates, dimension context).
- Manual mode confirmation system (pending suggestions queue + keybind apply + expiry).
- Director Mode V2 primitives (known-mod matrix + dynamic bias hints).

> Note: The project already contains core decision-making components. This PR focuses on shipping the missing *modules* and APIs in a single, large step; wiring into existing governors/HUD can follow as a focused integration PR if needed.
