# Metodología de medición y reporte de rendimiento

Este documento define cómo medir frametimes y cómo reportar resultados de manera reproducible.

## 1) Metodología de medición

**Herramientas (elige una o combina según disponibilidad):**

- **Spark** (profiling): `spark profiler --only-client` para capturar uso de CPU y correlacionar picos.
- **VisualVM**: para inspección de GC y picos de CPU en Java.
- **Herramientas de frametime** (in-game o externas):
  - FrameTime graph del juego (si disponible)
  - Herramientas tipo RTSS/Afterburner (frametime, FPS, 1% low) si están permitidas

**Requisitos de ejecución (fijos en cada corrida):**

- **Duración por escenario:** 3–5 minutos, con 30–60 s de estabilización previa.
- **Hardware:** CPU, GPU, RAM, versión del SO, versión de Java.
- **Resolución y ajustes:** resolución, fullscreen/windowed, VSync, límite de FPS.
- **Shaders y mods:** nombre exacto, versión, preset de shader.
- **Render/Simulation distance:** valores exactos (chunks).
- **Seed y coordenadas:** para reproducibilidad (si aplica).

**Métrica primaria:** frametime p95 (ms).
**Métrica secundaria:** p99 (ms) cuando el escenario tenga picos evidentes.

## 2) Escenarios reproducibles

Define escenarios con rutas y acciones repetibles. Usa un mundo y seed fijo.

### A) Combate con mobs

- **Ubicación:** plataforma plana a nivel Y fijo.
- **Setup:** spawners o eggs con cantidad exacta (ej. 80 zombis + 20 esqueletos).
- **Acción:** girar 360° cada 10 s, atacar durante 60 s, pausar 10 s, repetir.

### B) Redstone tick-heavy

- **Ubicación:** chunk dedicado.
- **Setup:** máquina con reloj de 1-tick y alto uso de bloques (repeaters/pistons/hoppers).
- **Acción:** permanecer en rango y mirar hacia el circuito durante la medición.

### C) Megabases con entidades

- **Ubicación:** base con gran densidad de entidades (frames, armor stands, villagers).
- **Setup:** número exacto por tipo (ej. 200 villagers, 100 armor stands, 600 item frames).
- **Acción:** recorrido de 60–90 s por un path marcado (waypoints).

## 3) Captura de métricas y configuración

En cada corrida, documenta:

- **Mods y versiones:** lista completa (incluye NOZH y optimizadores).
- **Shaders:** nombre, versión, preset.
- **Distancia de render y simulación.**
- **Ajustes de juego relevantes:** mipmaps, partículas, sombras, clouds.
- **Resolución y FPS limit.**

**Salida mínima:** p95 frametime (ms). Agrega p99 si hay stutter notable.

## 4) Presentación de resultados

### Tabla (formato sugerido)

| Escenario | Resolución | Shaders | Render/Sim Distance | p95 (ms) | p99 (ms) | FPS avg | Notas |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Combate mobs | 1920x1080 | Complementary v4 | 12/10 | 24.1 | 35.9 | 72 | picos al girar |
| Redstone | 1920x1080 | Off | 12/10 | 19.8 | 28.4 | 88 | variabilidad baja |
| Megabase | 1920x1080 | BSL | 16/12 | 31.2 | 45.7 | 61 | alto coste GPU |

### Gráficos (formato sugerido)

- **Serie de tiempo de frametime:** para visualizar picos.
- **Box plot por escenario:** para comparar distribuciones.
- **Barras p95/p99:** comparación rápida entre escenarios.

### Notas de variabilidad y límites

- Reporta temperatura/thermal throttling si ocurrió.
- Indica si hubo GC spikes notables.
- Señala límites (input lag, limitador de FPS, VSync, etc.).
- Indica el número de corridas por escenario (ideal: 3) y usa promedio/mediana.

## Checklist de reporte

- [ ] Hardware y software completos
- [ ] Mods y shaders con versión
- [ ] Escenario reproducible con seed/coords
- [ ] Duración y número de corridas
- [ ] p95 (y p99 si aplica)
- [ ] Tabla y gráficos
- [ ] Notas de variabilidad
