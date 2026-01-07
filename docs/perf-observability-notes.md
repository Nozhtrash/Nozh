# Observabilidad de rendimiento (hotspots, I/O, buffers y startup)

## 1) Mapa de hotspots (PerfManager + trazadores)

### Dónde mirar
- `dev.nozh.core.profiler.PerfManager`: orquesta el muestreo, recopila snapshots y expone diagnósticos; integra `RenderPipelineTracer` para trazar fases de render y reportar la fase más “caliente”.
- `dev.nozh.core.profiler.RenderPipelineTracer`: acumula tiempos por fase de render en una ventana fija de 1s y calcula métricas (total, promedio, máximo) por fase, además de la “hottest phase”.
- `dev.nozh.core.profiler.RenderPhase` y `RenderPhaseMetrics`: enum de fases y estructura de métricas (ticks, totalMs, maxMs, avgMs).

### Cómo se registra por fase
- `PerfManager` delega a `RenderPipelineTracer` con `onRenderPhaseStart/End` y `onRenderFrameStart/End`, y lo incluye en `PerfReport` y en `PerfDiagnosticsSnapshot` (incluye la fase más caliente y su `maxMs`).
- Las fases se marcan en mixins:
  - Frame completo: `MixinGameRenderer` llama `onRenderFrameStart/End`.
  - Entities: `MixinEntityRenderDispatcher` marca `RenderPhase.ENTITIES` en `HEAD/RETURN`.
  - Block entities: `MixinBlockEntityRenderDispatcher` marca `RenderPhase.BLOCK_ENTITIES`.
  - Partículas: `MixinParticleManager` marca `RenderPhase.PARTICLES`.

### Resultado práctico (hotspots)
- El “mapa” de hotspots viene de `RenderPipelineTracer.snapshot()` y su `hottestPhase()`; cada fase registra `totalMs`, `avgMs` y `maxMs` por ventana de 1s, lo que permite detectar las fases con mayor tiempo por frame o picos (`maxMs`).

## 2) Puntos de I/O (exportación CSV/JSON)

### Dónde se escribe
- `TelemetryExportWriter.write(...)` usa `Files.writeString` para escribir CSV o JSON en disco dependiendo del formato solicitado.

### Quién invoca el writer y en qué contexto
- `PerfManager.exportTelemetry(...)` arma el reporte (`PerfReport`) y delega la escritura a `TelemetryExportWriter`, creando el directorio `telemetry_exports` y generando el archivo con timestamp (`telemetry_YYYYMMDD_HHMMSS.{csv|json}`).
- `NozhCommands.runTelemetryExport(...)` llama `perfManager.exportTelemetry(...)` cuando el usuario ejecuta el comando de exportación (contexto: comando del cliente).
- `NozhModClient.exportHudReport(...)` también llama `perfManager.exportTelemetry(...)` al exportar desde el HUD (contexto: UI/toast).
- `BenchmarkSession.exportReport(...)` usa `TelemetryExportWriter` para exportar un benchmark controlado (contexto: herramienta/benchmark).

### Resumen I/O
- La escritura real ocurre en `TelemetryExportWriter`, con destino a archivo CSV o JSON y texto en UTF-8. Esto es el punto de I/O principal. Las invocaciones llegan desde comandos, HUD y benchmark controlado.

## 3) Colas / “buffers” de medición (RollingWindowStats / FrameTimeSampler)

### Capacidad y ventana
- `PerfManager` calcula capacidad como `targetFps * windowSeconds` con clamp entre 60 y 600; por defecto ventana de 5s (`windowSeconds = 5`). Esto define el tamaño de la ventana temporal y del ring buffer de `RollingWindowStats`.
- `RollingWindowStats` implementa un ring buffer fijo (`long[] buffer`) con `capacity` y `windowSeconds` almacenados; sobrescribe entradas viejas cuando se llena (writeIndex cíclico).

### Backlog / comportamiento de cola
- No hay backlog indefinido: el ring buffer tiene tamaño fijo y sobrescribe los samples más antiguos cuando se alcanza `capacity` (no bloquea ni acumula fuera del buffer).
- La snapshot considera “insuficientes datos” si `count < capacity / 2`, lo que es un indicador de calentamiento o ventana aún no llena al menos a la mitad.
- `FrameTimeSampler` filtra muestras inválidas (≤0 o >500ms) antes de añadirlas al buffer, evitando “backlog” de muestras erróneas o pausas enormes.

## 4) Tiempos de arranque / warm-up (InitialBenchmarkRunner)

### Parámetros de warm-up
- `InitialBenchmarkRunner` define un retardo inicial de 5s (`START_DELAY_MS = 5000`) y una duración de 10s (`DURATION_MS = 10000`) antes de cerrar el benchmark inicial; funciona como proxy de warm-up/estabilización de sesión.
- El benchmark arranca tras el delay (si no hay otro benchmark activo), y se completa al final de la ventana de duración, tomando un `PerfSnapshot` para clasificar validez/ruido.

## 5) Lista priorizada por categoría

1. **Hot paths (render)**
   - Prioridad alta porque impacta directamente el tiempo por frame. El sistema ya mide fases con `RenderPipelineTracer` y expone la fase más caliente (`maxMs`), con ventana de 1s para capturar picos; además las fases están instrumentadas en mixins del render loop (frame, entidades, block entities, partículas).
2. **I/O (exportación de telemetría)**
   - Escrituras directas a disco via `Files.writeString` en `TelemetryExportWriter` cuando se exporta desde comandos o HUD. Esto puede introducir latencias puntuales, aunque ocurre bajo acción explícita del usuario (comando/HUD) o en benchmark controlado.
3. **Colas/Buffers (RollingWindowStats / FrameTimeSampler)**
   - Buffer fijo de tamaño configurable (60–600) calculado por FPS objetivo y ventana en segundos; no hay backlog acumulativo, pero sí sobrescritura y “warm-up” hasta tener al menos la mitad de la ventana llena. Filtrado de spikes evita contaminación del promedio y percentiles.
4. **Startup / Warm-up (InitialBenchmarkRunner)**
   - Retardo inicial de 5s + duración de 10s para benchmark inicial (proxy de warm-up). Útil para entender estabilización antes de considerar la telemetría “fiable”.

## Comandos usados
- `rg -n "class PerfManager|PerfManager" src`
- `rg -n "exportTelemetry|TelemetryExportWriter" src/main/java`
- `sed -n '1,240p' src/main/java/dev/nozh/core/profiler/PerfManager.java`
- `sed -n '1,240p' src/main/java/dev/nozh/core/profiler/RenderPipelineTracer.java`
- `sed -n '1,240p' src/main/java/dev/nozh/core/profiler/RollingWindowStats.java`
- `sed -n '1,220p' src/main/java/dev/nozh/core/profiler/FrameTimeSampler.java`
- `sed -n '1,240p' src/main/java/dev/nozh/core/telemetry/TelemetryExportWriter.java`
- `sed -n '1,220p' src/main/java/dev/nozh/core/profiler/BenchmarkSession.java`
- `sed -n '360,460p' src/main/java/dev/nozh/client/NozhModClient.java`
- `sed -n '300,420p' src/main/java/dev/nozh/client/NozhCommands.java`
- `sed -n '1,240p' src/main/java/dev/nozh/core/benchmark/InitialBenchmarkRunner.java`
- `sed -n '1,200p' src/main/java/dev/nozh/core/profiler/RenderPhase.java`
- `sed -n '1,200p' src/main/java/dev/nozh/core/profiler/RenderPhaseMetrics.java`
- `sed -n '1,220p' src/main/java/dev/nozh/mixin/MixinGameRenderer.java`
- `sed -n '1,220p' src/main/java/dev/nozh/mixin/MixinEntityRenderDispatcher.java`
- `sed -n '1,220p' src/main/java/dev/nozh/mixin/MixinParticleManager.java`
- `sed -n '1,220p' src/main/java/dev/nozh/mixin/MixinBlockEntityRenderDispatcher.java`
