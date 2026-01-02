# Changelog

## v0.1.0 - "The Foundation" (Golden Master)

**Release Type**: Initial Release
**Stability**: Production Ready

### Key Features

* **Safe Architecture**: Mod is split into Profiler, Governor, and Executor for safety.
* **Honest Profiler**: Measures Frametime P95 and Average accurately. Reports `UNKNOWN` if data is insufficient.
* **Safe Mode ("The Bouncer")**: Automatically locks the mod if a crash loop is detected.
* **Automated Rollback**: If a change (e.g. minimizing particles) doesn't improve performance within 45 seconds, it is undone.
* **Diagnostics**: `/nozh selfcheck` command for health auditing.
* **Compat**: Detects Sodium, Iris, etc. and logs compatibility hints.
* **Localization**: English (US) and Spanish (CL) support.

### Non-Goals (v0.1.0)

* NOZH does NOT increase FPS artificially.
* NOZH does NOT override user preferences without consent.
* NOZH does NOT change render distance, simulation distance, or entities in this version.
* NOZH does NOT guess when data is insufficient.

### Stability Guarantees

* NOZH guarantees reversibility for every automatic action performed in v0.1.0.

### Technical Details

* Zero-allocation hot paths (no garbage generation in `onFrame`).
* Strict config validation and safe clamping.
* Action Cooldowns to prevent "fighting" with the user.
