# NOZH Architecture & Philosophy

> **"Unknown is better than wrong."**

NOZH is a **Frametime-first Stability Orchestrator** for Minecraft. It is designed to be honest, conservative, and robust. It prioritizes stability over raw FPS, and accurate measurement over aggressive optimization.

## Core Philosophy

1. **Honesty Technical > Métricas Falsas**: We do not estimate FPS if we cannot measure it accurately. We do not classify bottlenecks if we don't have enough data.
2. **UNKNOWN > Incorrect Conclusion**: If the data is noisy, insufficient, or contradictory, the system returns `UNKNOWN`. We never guess.
3. **Measure ≠ Optimize**: Measurement is a distinct phase. We do not change settings based on feelings; we change them based on proven data.
4. **Classify ≠ Act**: Identifying a bottleneck (Phase 4) is different from deciding to fix it (Phase 5).
5. **Cero Crashes**: The mod itself must never be the cause of a crash. Defensive coding and try-catch blocks surround all critical paths.

## Phase Model

NOZH follows a strict phased implementation model to ensure stability at every step.

* **Phase 0-2**: Foundation, Verification, Polish. (Completed)
* **Phase 3 (Profiler)**: Accurate frametime measurement (P95/Avg). (Completed)
* **Phase 4 (Classification)**: Bound classification (Basic). (Completed)
* **Phase 5 (Decision Engine)**: The "Governor". Analyzes snapshots and issues Decisions. (Completed)
* **Phase 6 (Execution)**: The "Executor". Applies decisions (e.g., DECREASE_PARTICLES) safely. (Completed)
* **Phase 6.5 (Safety)**: Automated Rollback. If an action doesn't improve frametime after 45s, it is reverted. (Completed)

## Component Architecture

### 1. The Profiler (`FrameTimeSampler`)

Measures wall-clock time between frames. Stores data in a ring buffer (`RollingWindowStats`). Strictly measurement, no logic.

### 2. The Governor (`SimulationGovernor`)

Pure logic component. Takes a `PerfSnapshot` and `Bound`, returns a `Decision`.

* **Input**: Stats (Avg, P95).
* **Rules**: Strict thresholds (e.g. >1.5x target frametime = CRITICAL).
* **Output**: `Decision(ActionType, Severity, Confidence)`.
* **Zero Dependencies**: Does not import `net.minecraft` classes.

### 3. The Executor (`StandardActionExecutor`)

The "Hands" of the system.

* **Guard Rails**: Checks `ExecutorGuard` (SafeMode, Cooldowns, Config).
* **ActionHandlers**: Specific implementations (e.g., `DecreaseParticlesHandler`).
* **Rollback**: Tracks history. If performance degrades, `revertLast()` undoes the change.

### 4. Safety First (`SafeMode`)

If the mod crashes or fails strictly, it enters **Safe Mode**.

* **Effect**: All Governor/Executor logic is strictly bypassed.
* **Persistence**: Stored in `config/nozh/state.json`. Requires user intervention or strict stability check to reset.

## Core Architecture Freeze

**Freeze Date**: 2025-02-14  
**Scope (Included)**: Core runtime loop, measurement pipeline, classification logic, decision engine, executor safety rails, rollback system, and safe mode persistence.  
**Scope (Excluded)**: UI/UX surfaces, telemetry/analytics, external integrations, experimental action handlers, and build/packaging tooling.

### Forward Compatibility Policy

* **Supported Versions**: Latest stable release and one previous minor version (e.g., v0.2.x and v0.1.x).
* **Breaking Changes Limit**: Only in minor releases with explicit migration notes; patch releases are strictly backward compatible.
* **Compatibility Matrix**:

| Core API Version | Supported Mod Releases | Notes |
| --- | --- | --- |
| v1 | v0.1.x – v0.2.x | Baseline core architecture freeze. |

### Compatibility Matrix (Runtime)

This matrix captures the **supported runtime combinations** aligned with benchmarking requirements. Fields are sourced from the **Hardware + Environment Requirements** (OS/JVM/Minecraft) and **Recording Configuration** (mods list, shaders) in `docs/benchmarking.md`.

| SO | JVM | Minecraft | Loader (Fabric/Forge) | NOZH | modpack/deps clave |
| --- | --- | --- | --- | --- | --- |
| `<OS version>` | `<JVM vendor + version>` | `<MC version>` | `<Fabric/Forge + version>` | `<NOZH version>` | `<mods list, shader pack>` |

### Stable Public Interfaces

The following interfaces are considered stable and part of the frozen core contract:

* **APIs**: `SimulationGovernor`, `StandardActionExecutor`, `ExecutorGuard`, `FrameTimeSampler`, `RollingWindowStats`.
* **Events**: `WorldRenderEvents.END` frametime sampling hook.
* **Data Contracts**: `PerfSnapshot`, `Decision`, `ActionType`, and `state.json` persistence format.

### Change Process (Deprecations & Versioning)

* **Deprecation Window**: Deprecated interfaces remain supported for at least one minor release with warnings.
* **Versioning**: Core API versions are incremented on breaking changes; new capabilities use additive, backward-compatible fields or methods.
* **Migration Notes**: Required for all breaking changes and must include rationale, steps, and fallback behavior.

## Frametime Definition

In NOZH, **Frametime** is the wall-clock time elapsed between two consecutive `WorldRenderEvents.END` callbacks.

* **Unit**: Nanoseconds (internally), converted to Milliseconds for display.
* **Metrics**: We track P95 (95th percentile) and Average. P95 is more important for smoothness.

## Why UNKNOWN Exists

`UNKNOWN` is not an error state; it is a valid and important informational state. It means the profiler is warming up or data is insufficient.

---
*Document updated for v0.2.0-alpha.*
