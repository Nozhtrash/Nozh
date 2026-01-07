# Checklist de métricas y captura

Este documento define un checklist operativo de métricas, su origen dentro de NOZH, los SLOs sugeridos, y el formato de entrega alineado con `TelemetryExportWriter`.

## 1) Checklist de métricas (qué medir)

- **Latencia (frametime)**
  - p95/p99 (ms) por ventana de observación.
  - Promedio (ms) y desviación estándar (ms) para contexto.
- **Throughput**
  - FPS estimado por ventana (`estimatedFps` basado en promedio de frametime).
- **CPU / Memoria**
  - CPU % promedio y pico.
  - RSS/heap/GC pressure según colector externo/OS.
- **Error rate**
  - Conteos de fallos por ventana (acciones fallidas, drops de telemetría, errores de exportación).
  - Ratio de fallos = fallos / total de eventos relevantes.

## 2) Mapeo métrica ➜ fuente

| Métrica | Fuente primaria | Detalle de origen |
| --- | --- | --- |
| p95 frametime (ms) | `PerfSnapshot` | `p95FrametimeMs` y `sampleCount` para suficiencia. |
| p99 frametime (ms) | `PerfSnapshot` | `p99FrametimeMs` (si hay muestras suficientes). |
| FPS estimado | `PerfSnapshot` | `estimatedFps()` derivado de `avgFrametimeMs`. |
| Promedio / stddev | `PerfSnapshot` | `avgFrametimeMs`, `frametimeStddevMs`. |
| Spikes | `PerfSnapshot` / `TelemetrySnapshot` | `spikeCount` (frame) + `droppedSamples` en HUD/Governor. |
| Agregados HUD/Governor | `TelemetrySnapshot` | `avgFrametimeMs`, `p95FrametimeMs`, `spikeCount`, `sampleCount`. |
| CPU / Memoria | Colector externo | OS/agent externo (por integrar). |
| Error rate | Contadores internos | Contadores de fallos (acciones, export, drops). |

## 3) SLOs sugeridos + frecuencia de captura

> Ajustar SLOs por tier de hardware y escenario. Los valores abajo son una línea base.

| Métrica | SLO objetivo | Ventana / frecuencia | Observaciones |
| --- | --- | --- | --- |
| p95 frametime (ms) | ≤ 16.7ms (60 FPS) / ≤ 33.3ms (30 FPS) | Ventana de 30–60s | Requiere `sampleCount` suficiente. |
| p99 frametime (ms) | ≤ 25ms (60 FPS) / ≤ 50ms (30 FPS) | Ventana de 30–60s | Usar solo si `sampleCount` ≥ 2000. |
| FPS estimado | ≥ 60 / ≥ 30 | Ventana de 30–60s | Derivado de `avgFrametimeMs`. |
| Spike count | ≤ 1 por ventana | Ventana de 30–60s | Señal de stutter severo. |
| CPU % promedio | ≤ 80% | Ventana de 30–60s | Colector externo. |
| RSS / heap | Estable (sin crecimiento sostenido) | Ventana de 60–120s | Vigilar presión de GC. |
| Error rate | ≤ 1% | Por sesión + por ventana | Definir denominador (acciones/frames). |

**Frecuencia de captura recomendada**
- **Por frame:** `samplesNanos` (granularidad máxima) para exportar trazas crudas.
- **Por ventana (30–60s):** `PerfSnapshot` (p95/p99/avg) y `TelemetrySnapshot` (HUD/Governor).
- **Por sesión:** SLOs agregados (min/max/avg por escenario) + contadores de error.

## 4) Formato de entrega (alineado con `TelemetryExportWriter`)

### CSV (actual)
`TelemetryExportWriter` exporta:
- **Serie cruda:**
  - `index,frametime_ms` (una línea por muestra de frame).
- **Resumen frame:**
  - `avg_ms`, `p95_ms`, `spikes`, `samples`, `window_seconds`, `timestamp_ms`.
- **Resumen tick:**
  - `tick_avg_ms`, `tick_p95_ms`, `tick_samples`, `tick_window_seconds`.
- **Diagnósticos:**
  - `gc_recent_ms`, `gc_pressure`, `pause_count`, `pause_max_ms`, `stutter_cause`,
    `stutter_confidence`, `hottest_phase`, `hottest_phase_max_ms`.

**Extensión recomendada (si se integra CPU/mem/error rate):**
- Agregar nuevas filas key/value al final del CSV (p. ej., `cpu_avg_pct,` / `mem_rss_mb,` / `error_rate_pct,`).

### JSON (actual)
`TelemetryExportWriter` exporta:
- `frame`: `avgFrametimeMs`, `p95FrametimeMs`, `spikeCount`, `sampleCount`, `windowSeconds`, `timestampMillis`.
- `tick`: `avgTickMs`, `p95TickMs`, `sampleCount`, `windowSeconds`.
- `samplesMs`: arreglo de muestras.
- `diagnostics`: GC, pauses, stutter, hottest render phase.

**Extensión recomendada:**
- Añadir bloques `system` (CPU/mem) y `errors` (contadores + rate) al mismo nivel que `diagnostics`.

## 5) Checklist operativo (validación)

- [ ] p95/p99 exportados desde `PerfSnapshot` con `sampleCount` suficiente.
- [ ] FPS estimado derivado de `avgFrametimeMs`.
- [ ] CPU/memoria provistos por colector externo y adjuntados al export.
- [ ] Error rate definido con denominador explícito y exportado.
- [ ] Export en CSV/JSON válido y compatible con `TelemetryExportWriter`.

## 6) Definición operativa de telemetría, logs y crash

### 6.1 Fuentes actuales (telemetría/logs)

- **Telemetría de performance (en memoria y exportable):**
  - `PerfManager` + `RollingWindowStats` generan `PerfSnapshot` y muestras crudas por ventana. Exportables vía
    `TelemetryExportWriter` en CSV/JSON desde `config/nozh/telemetry_exports/`.
  - `TelemetryManager` agrega métricas por escenario (`TelemetryAggregator`) y eventos (`EventTimeline`) y permite
    reportes en Markdown/JSON si se llama explícitamente.
- **Eventos de crash loop / recuperación (telemetría de eventos):**
  - `CrashLoopGuard.recordFailureContext(...)` captura contexto y lo envía a `TelemetryManager.recordCrashContext(...)`.
  - `CrashLoopGuard.evaluateCrashRecovery(...)` emite un evento de recuperación (`recordCrashRecovery`) cuando se
    activa cuarentena o safe mode.
- **Logging operativo:**
  - Logger principal: `NozhConstants.LOGGER` (SLF4J) para info/warn/error.
  - Logger de diagnóstico opcional: `DebugLogger` escribe a `config/nozh/nozh-debug.log` cuando `debugLogs=true`.

### 6.2 Definición exacta de “crash” (para métricas operativas)

**Se considera “crash” dentro de la telemetría interna cuando ocurre alguno de estos eventos:**
1. **Fallo/exception en ejecución de acción** que llama a
   `CrashLoopGuard.recordFailureContext(...)` (p. ej., fallos en `StandardActionProcessor` o
   `StandardActionExecutor`).
2. **Crash loop recovery** detectado al iniciar: `bootAttempts >= 3` y sesión no marcada como estable,
   con resultado de **cuarentena de capability** o **safe mode**.

**Eventos excluidos (NO cuentan como crash):**
- Spikes de rendimiento, `TelemetrySnapshot` vacíos, o drops del buffer de telemetría.
- Safe mode forzado por configuración o activado manualmente por el usuario.
- Errores de exportación o fallos de diagnóstico sin relación con `CrashLoopGuard`.

### 6.3 Cómo se agregan los eventos de crash

**Unidad base de crash:**
- Cada llamada a `recordFailureContext` cuenta como **1 evento de crash técnico**.
- Cada acción de recuperación (`quarantine` o `safe mode`) cuenta como **1 evento de crash loop**.

**Agregación recomendada (operativa):**
- **Crash rate por sesión:** `crash_events / session_duration_minutes`.
- **Crash loop rate:** `crash_loop_events / total_sessions`.
- **Por capability:** agrupar por `capabilityId` dentro del contexto de crash.

### 6.4 Ventanas temporales y entornos

**Ventanas actuales en el código (tiempo real):**
- **Ventana de telemetría de performance:** 3–10s dinámicos (`PerfManager` ajusta la ventana).
- **Actualización de métricas de estado:** cada 1s (20 ticks).
- **Crash loop guard:**
  - Sesión estable tras ~10s (200 ticks).
  - Cuarentena por 10 minutos (`CRASH_RECOVERY_QUARANTINE_MILLIS`).

**Ventanas operativas externas (recomendadas):**
- **24h / 7d / 30d** para reporting ejecutivo y tendencias.
- Estas ventanas **no se calculan dentro del mod**; deben agregarse a partir de
  exports CSV/JSON o logs agregados por entorno.

**Entornos:**
- `prod`: instancia de jugador final.
- `staging`: pruebas internas con builds de integración.
- `qa`: pruebas automatizadas/regresión.

### 6.5 Ejemplos de cálculo

- **Crash rate (24h, prod):**
  - `total recordFailureContext en 24h / total sesiones prod en 24h`.
- **Crash loop rate (7d, staging):**
  - `eventos de cuarentena + safe mode en 7d / total sesiones staging en 7d`.
- **Crash por capability (30d, qa):**
  - `crash_events agrupados por capabilityId / total sesiones qa en 30d`.
