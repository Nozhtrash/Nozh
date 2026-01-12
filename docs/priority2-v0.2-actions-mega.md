## Priority 2 Mega Actions (v0.2)

This PR is intentionally large and "end-to-end": it turns Priority2 suggestions into **real client-side changes**.

### Included

- `Priority2ActionApplier`: maps suggestion IDs to real `GameOptions` changes (particles, view distance, simulation distance, etc.) using best-effort reflection.
- `NozhPriority2Client`: on `K` confirm, applies suggestion via `Priority2ActionApplier` instead of only printing chat.
- Conservative gradual recovery when performance is *very* good and no pending suggestions.

### Safety

- All option touching is wrapped in defensive reflection and must never crash the game.
- Options persistence is best-effort.
