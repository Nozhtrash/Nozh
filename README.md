<div align="center">
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>Intelligent Optimization Orchestrator for Minecraft (Fabric)</b><br>
    <i>Orquestador inteligente de optimización para Minecraft (Fabric)</i>
  </p>

  <p>
    <a href="https://github.com/NozhMod/Nozh/actions"><img src="https://img.shields.io/badge/Status-ALPHA-ff8c00?style=for-the-badge&logo=appveyor" alt="Status"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Version-1.20.1-FFA500?style=for-the-badge&logo=minecraft" alt="Version"></a>
    <a href="#"><img src="https://img.shields.io/badge/Mode-Context%20Aware-00c853?style=for-the-badge" alt="Mode"></a>
  </p>
</div>

---

# 🇬🇧 English

## What NOZH is

**NOZH** is a client-side optimization orchestrator for Minecraft. Instead of applying static tweaks, it runs a **Governor** that evaluates live telemetry and decides *when* to adjust settings based on the current game scenario (combat, building, exploring, AFK, etc.).

## Current stage

**Alpha / active development.** The core decision loop, telemetry, compatibility matrix, and in-game controls are implemented and usable. Some mod integrations and advanced tuning features are still partial or stubbed.

## What it does today (implemented)

- **Scenario detection** using recent actions, hostiles nearby, world context, and stability scoring (AFK, combat, building, exploring, loading, menu).
- **Governor decision loop** (auto/assisted) that proposes or applies changes based on frametime signals and safety rules.
- **Session Learning** with persistent action outcome tracking (avoids repeating ineffective tweaks on your hardware).
- **Telemetry & HUD** with average frametime, p95 spikes, stutter causes, last actions, and suggested actions.
- **Capability providers** for real Minecraft settings (render distance, simulation distance, particles, mipmap levels, smooth lighting, graphics mode, vsync, FPS cap, etc.).
- **Surgical rendering toggles** (armor stands, item frames, block entities, animations) via NOZH render settings.
- **Compatibility matrix** that detects many popular performance mods and yields control to them when appropriate.
- **Limited adapters** (e.g., Sodium options + LambDynamicLights) for shared control when available.
- **Commands and exports** (`/nozh status`, `/nozh selfcheck`, `/nozh perf`, `/nozh history`, `/nozh telemetry export ...`).

## Partial / experimental / stubbed

- **Fog control** is currently a stub in vanilla options (marked DEGRADED).
- **Iris/Sodium compatibility** is mostly detection + limited reflection; not full shader or render pipeline orchestration.
- **Director V2 + deep mod coordination** is only partially wired; many modules are detection-only.
- **Advanced predictive or ML-driven decisioning** is not yet active (still roadmap).

## Why install it now

- You want **context-aware optimization** instead of a fixed ruleset.
- You want **visibility** into stutter causes and NOZH’s decisions.
- You need **safe orchestration** when combining multiple performance mods.
- You can tolerate alpha-level limitations in exchange for adaptive behavior.

## Future potential (based on `future.txt`)

- More precise **CPU vs GPU** bottleneck detection.
- Deeper **scenario profiling** (dimension context, long-term action windows).
- **Predictive analysis** to avoid oscillations and pre-empt spikes.
- Wider **mod adapter coverage** and richer orchestration rules.

> Roadmap is aspirational and can change. It reflects intent, not a delivery guarantee.

## Known limitations & risks

- Gains are **contextual**, not guaranteed; some hardware or modpacks may see minimal improvement.
- Scenario detection is heuristic and can misclassify (e.g., inventory-heavy building might look like AFK).
- Some capabilities are **DEGRADED or no-op** in vanilla; NOZH will back off in those cases.
- External mod updates can break reflection-based adapters (Sodium/Iris APIs are not stable).

## Configuration (recommended defaults)

These defaults are tuned for **real hardware baselines** and common 60–144 Hz panels:

- `targetFps = 60` (set to 120/144/240 if your monitor and GPU can sustain it).
- `observationWindowSeconds = 5` (enough samples to smooth spikes without lagging reactions).
- `cooldownActionMillis = 120000` (per-action cooldown to avoid churn).
- `cooldownGlobalMinIntervalMillis = 60000` (global guardrail between any actions).

## Rough progress estimate (subjective)

- **Core loop + telemetry + HUD:** ~70–80% complete.
- **Compatibility orchestration:** ~35–50% complete (detection solid, adapters limited).
- **Predictive/advanced intelligence:** ~10–20% complete.

Overall: **~45–55% of the “full vision”** described in `future.txt` + extra ideas. This is a **best-effort estimate** based on current repo behavior, not a formal metric.

---

# 🇪🇸 Español

## Qué es NOZH

**NOZH** es un orquestador de optimización del lado del cliente para Minecraft. En lugar de aplicar tweaks estáticos, ejecuta un **Governor** que analiza telemetría en tiempo real y decide *cuándo* ajustar configuraciones según el escenario actual (combate, construcción, exploración, AFK, etc.).

## Etapa actual

**Alpha / desarrollo activo.** El loop de decisión, telemetría, matriz de compatibilidad y controles en juego ya funcionan. Algunas integraciones con mods y mejoras avanzadas siguen parciales o en modo stub.

## Qué hace hoy (implementado)

- **Detección de escenarios** con acciones recientes, hostiles cercanos, contexto del mundo y estabilidad (AFK, combate, construcción, exploración, carga, menú).
- **Governor** en modo automático o asistido, con reglas de seguridad y evaluación de frametime.
- **Session Learning** persistente (evita repetir ajustes ineficientes en tu hardware).
- **Telemetría + HUD** con promedio, p95, spikes, causas de stutter, últimas acciones y sugerencias.
- **Providers** para opciones reales (render distance, simulation distance, partículas, mipmap, smooth lighting, graphics mode, vsync, FPS cap, etc.).
- **Control quirúrgico de render** (armor stands, item frames, block entities, animaciones).
- **Matriz de compatibilidad** que detecta muchos mods de rendimiento y cede control cuando corresponde.
- **Adapters limitados** (p. ej. Sodium options + LambDynamicLights) para control compartido.
- **Comandos y exportación** (`/nozh status`, `/nozh selfcheck`, `/nozh perf`, `/nozh history`, `/nozh telemetry export ...`).

## Parcial / experimental / stub

- **Control de fog** es un stub en vanilla (marcado DEGRADED).
- **Compatibilidad Iris/Sodium** es principalmente detección + reflection limitada; no hay orquestación profunda.
- **Director V2** y coordinación avanzada de mods están solo parcialmente conectados.
- **Modelos predictivos/ML** aún no están activos (solo roadmap).

## Por qué instalarlo hoy

- Quieres **optimización contextual** en vez de reglas fijas.
- Buscas **visibilidad** sobre el origen del stutter y las decisiones.
- Necesitas **orquestación segura** al combinar varios mods de rendimiento.
- Aceptas limitaciones de etapa alpha a cambio de comportamiento adaptativo.

## Potencial futuro (basado en `future.txt`)

- Detección más precisa de **CPU vs GPU bound**.
- Escenarios más profundos (dimensión, ventanas largas de acciones).
- **Análisis predictivo** para evitar oscilaciones y picos.
- Más **adapters** y reglas de orquestación avanzadas.

> El roadmap es aspiracional y puede cambiar. No es una garantía de entrega.

## Limitaciones y riesgos conocidos

- Las mejoras son **contextuales**, no garantizadas; algunos hardwares/modpacks pueden ver poca mejora.
- La detección de escenarios es heurística y puede fallar (p. ej. construcción con inventario puede parecer AFK).
- Algunas capacidades están **DEGRADED o no-op** en vanilla; NOZH retrocede en esos casos.
- Actualizaciones de mods externos pueden romper adapters basados en reflection.

## Configuración (defaults recomendados)

Estos defaults están afinados para **hardware real** y paneles comunes de 60–144 Hz:

- `targetFps = 60` (sube a 120/144/240 si tu monitor y GPU lo sostienen).
- `observationWindowSeconds = 5` (suficientes muestras sin retrasar la reacción).
- `cooldownActionMillis = 120000` (cooldown por acción para evitar oscilaciones).
- `cooldownGlobalMinIntervalMillis = 60000` (mínimo global entre acciones).

## Estimación de progreso (subjetiva)

- **Loop + telemetría + HUD:** ~70–80%.
- **Orquestación de compatibilidad:** ~35–50% (detección sólida, adapters limitados).
- **Inteligencia predictiva/avanzada:** ~10–20%.

En total: **~45–55% de la “visión completa”** descrita en `future.txt` y mejoras adicionales. Es una **estimación orientativa**, no un KPI formal.

---

## Installation (quick)

1. Drop `nozh-x.x.x.jar` into your `mods` folder.
2. Launch Minecraft 1.20.1 with Fabric.
3. Use `/nozh selfcheck` and `/nozh status` to verify.

---

<div align="center">
  <p><i>Context-aware optimization beats static tweaks.</i></p>
</div>
