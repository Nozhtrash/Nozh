# Manual Validation Scripts

These checklists are designed for local testing on a client instance. Each script is scoped to a specific validation goal.

## Benchmark Validation

1. Launch Minecraft with NOZH enabled and a stable world save.
2. Warm up for 2 minutes in a static location (no movement, avoid menu switching).
3. Run `/nozh perf` to confirm sufficient samples.
4. Trigger the built-in benchmark (if available) or record a 60-second session with the HUD visible.
5. Export telemetry:
   - `/nozh telemetry export csv` or `/nozh telemetry export json`
6. Verify the export file under `config/nozh/telemetry_exports/` and check that avg, p95, spikes, and sample counts are populated.

## Stress Test Validation

1. Load a heavy scene (dense village or redstone build).
2. Toggle auto-tuning on, then off, to confirm RuntimeState sync.
3. Induce spikes (fast travel, elytra, chunk loading) for at least 2 minutes.
4. Confirm HUD shows p95 and spikes updating and that suggestions appear when generated.
5. Apply a suggestion using `K` and verify a toast/chat confirmation.

## Compatibility Validation

1. Install one performance mod (e.g., Sodium) and one shader mod if available.
2. Launch the game and open the ModMenu configuration panel.
3. Verify Compatibility section shows informational text and the mod runs without crashes.
4. Observe that actions do not conflict with external mod settings during 5 minutes of gameplay.
