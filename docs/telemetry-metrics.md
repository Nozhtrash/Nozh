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
