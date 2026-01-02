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
* **Persistence**: Stored in `nozh-state.json`. Requires user intervention or strict stability check to reset.

## Frametime Definition

In NOZH, **Frametime** is the wall-clock time elapsed between two consecutive `WorldRenderEvents.END` callbacks.

* **Unit**: Nanoseconds (internally), converted to Milliseconds for display.
* **Metrics**: We track P95 (95th percentile) and Average. P95 is more important for smoothness.

## Why UNKNOWN Exists

`UNKNOWN` is not an error state; it is a valid and important informational state. It means the profiler is warming up or data is insufficient.

---
*Document updated for v0.1.0 Release.*
