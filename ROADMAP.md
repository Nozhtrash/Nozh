# NOZH Roadmap

## v0.2.0-alpha (Current) - "The Analyst" (Alpha)

* **Status**: En desarrollo activo (pre-beta)
* **Focus**: Validación de estabilidad, telemetría ampliada y preparación de suites de prueba.
* **Delivered**:
  * Chaos tests en CI con reportes JSON/CSV y metadatos.
  * Escenarios de caos adicionales (entity/chunk/shader).
  * Benchmark artifacts por escenario y matriz de benchmark exportable.
  * Runners de quick-test para modpacks + documentación asociada.
  * Tutorial inicial y expansión de localizaciones.
  * Política de freeze de arquitectura documentada.

## v0.1.0 (Released) - "The Foundation"

* **Status**: Released / RC (IMPLEMENTADO)
* **Features/Phases Completed**:
  * Phase 0-2: Safe architecture, atomic config, SafeMode.
  * Phase 3: Accurate P95 Frametime Profiler.
  * Phase 4: Basic Classification (Bound: UNKNOWN/CPU/GPU).
  * Phase 5: Simulation Governor (Decision Engine).
  * Phase 6: Action Executor (Single Action: `DECREASE_PARTICLES`).
  * Phase 6.5: Automated Rollback Mechanism (45s evaluation window).
  * Phase 7-9: Diagnostics, Hardening, UX.

> **Contract**: Closed phases (0–6.5) are considered architecturally frozen in v0.x releases. New functionality must extend the system, not rewrite it.

## v0.2.0-beta - "The Analyst" (Planificado)

* **Status**: Planificado (NO implementado aún)
* **Goal**: Distinguish CPU vs GPU bottlenecks accurately.
* **Tech**: Tick Time measurements.
* **Phase 4.5**: Implement TickTimeSampler (read-only initially - no actions).
* **Phase 5 update**: Updated rules to target specific bottlenecks (e.g. reduce entities for CPU, render distance for GPU).

## v0.3.0 - "The Executive" (Planificado)

* **Status**: Planificado (NO implementado aún)
* **Goal**: Expand the arsenal of actions.
* **New Actions**:
  * `DECREASE_RENDER_DISTANCE` (with fog adjustment)
  * `DECREASE_SIMULATION_DISTANCE`
  * `DECREASE_ENTITY_DISTANCE`
* **Smart Priority**: Governor prioritizes cheap actions (particles) before expensive play-altering ones (view distance).
