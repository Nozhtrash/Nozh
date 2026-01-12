## Priority 2 Mega Integration (v0.2)

This PR completes a **big wiring step**:

- Priority2 signals are captured at runtime (tick/render, OS load, deep scenario signals).
- Signals are published via `Priority2Signals` for consumption by core governors/HUD.
- A new lightweight HUD overlay displays bottleneck + deep scenario stats.
- Manual suggestion queue is now visible + de-duplicated, and confirmable with `K`.

### Why this is safe
- Adds functionality by composing new modules instead of rewriting core.
- Uses atomics for thread-safe snapshot reads.
- Suggestions are informational for now; mapping to real capability actions can be added next.
