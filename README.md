<div align="center">
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>The First Intelligent Optimization Orchestrator for Minecraft</b><br>
    <i>El Primer Orquestador Inteligente de Optimización para Minecraft</i>
  </p>

  <p>
    <a href="https://github.com/NozhMod/Nozh/actions"><img src="https://img.shields.io/badge/Status-GOD%20MODE-8A2BE2?style=for-the-badge&logo=appveyor" alt="Status"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Version-1.20.1-FFA500?style=for-the-badge&logo=minecraft" alt="Version"></a>
    <a href="#"><img src="https://img.shields.io/badge/FPS-UNCAPPED-00FF00?style=for-the-badge&logo=nvidia" alt="FPS"></a>
  </p>
</div>

---

## 📌 Qué es NOZH

**NOZH** es una capa de orquestación inteligente para Minecraft. En lugar de aplicar reglas estáticas, usa un **Sistema Gobernador** que observa el estado del juego y ajusta parámetros en tiempo real para priorizar rendimiento o calidad visual según el contexto.

## ✅ Estado actual

### Qué hace hoy

- Detecta **escenarios de juego** (AFK, combate, minería, construcción) y adapta decisiones de rendimiento.
- Coordina con mods existentes mediante el **Director** (gestor de conflictos y prioridades).
- Registra aprendizaje por sesión para no repetir ajustes ineficientes en tu hardware.
- Instrumenta el motor con **hooks** para medir costos reales de chunk loading y renderizado.
- Controla entidades específicas cuando hay picos de carga (culleado quirúrgico).

### Fase actual

En **desarrollo activo** y enfocado en consolidar estabilidad, compatibilidad y precisión de las decisiones. Las capacidades descritas como “hoy” corresponden a lo ya implementado o en integración inmediata dentro del repositorio.

## 🗺️ Roadmap futuro (exploratorio)

### Qué busca a futuro

- Ampliar la cobertura de escenarios detectados y ajustes posibles.
- Refinar el aprendizaje adaptativo para más perfiles de hardware.
- Mejorar la interacción con ecosistemas de mods nuevos y existentes.

> Nota: este roadmap es **exploratorio** y puede cambiar. No se asumen promesas ni fechas.

## 💡 Por qué valdrá la pena

Porque busca optimizar **contextualmente**, no con reglas fijas. Eso permite mantener una experiencia más fluida sin sacrificar calidad cuando no es necesario.

## 👥 Para quién es

- Jugadores con modpacks exigentes que quieren estabilidad.
- Usuarios que combinan varios mods de optimización y necesitan coordinación.
- Quienes priorizan consistencia de rendimiento sobre ajustes manuales constantes.

## ✨ Diferenciales

- **Orquestación inteligente** en vez de optimizaciones aisladas.
- **Compatibilidad proactiva** con otros mods mediante el Director.
- **Aprendizaje persistente** por hardware y sesión.
- **Control quirúrgico** de entidades con impacto en FPS.

## 🧪 Casos de uso reales y expectativas de mejora de rendimiento

- **Lobbies con muchas entidades decorativas**: reducción de picos de render al ocultar elementos no críticos.
- **Zonas con alta carga de chunks**: menor stutter al detectar cuellos de CPU y ajustar distancia o prioridad.
- **Combate con partículas y efectos**: mejor consistencia de frame pacing al limitar elementos de alto costo.

Las mejoras esperadas son **relativas y contextuales** (consistencia, estabilidad y percepción de fluidez), y dependen del hardware, modpack y configuración.

---

## 🧠 Core Technology / Tecnología Central

### 1. The Director (Mod Conflict Manager)

NOZH recognizes that you probably use other optimization mods. It doesn't fight them; it manages them. It scans your mod folder at startup and builds an **Action Matrix**:

| Capability | Detected Mod | NOZH Action |
| :--- | :--- | :--- |
| **Render Distance** | `Bobby` | **YIELD**: NOZH lets Bobby handle view distance to keep extended chunks visible. |
| **Particles** | `Sodium Extra` | **YIELD**: NOZH lets Sodium Extra handle particle limits. |
| **Formas de Entidad** | *None* | **TAKE CHARGE**: NOZH activates its internal `EntityCuller`. |
| **Clouds** | `Iris` | **YIELD**: Iris handles shader clouds, NOZH backs off. |

*NOZH detects over 25+ mods including Sodium, Lithium, FerriteCore, ModernFix, ImmediatelyFast, MoreCulling, etc.*

### 2. Session Learning (Persistent AI)

NOZH builds a profile of your specific hardware (CPU vs GPU balance).

- If NOZH tries to reduce **Shadow Distance** and your FPS **does not increase**, it marks that action as "Ineffective" for your PC.
- It saves this data to `.minecraft/config/nozh/state.json`.
- Next time, it won't waste resources trying ineffective optimizations. **It learns.**

### 3. True Sight (Mixin Hooks)

NOZH injects probes into the Minecraft Engine:

- `MixinClientChunkManager`: Measures the exact milliseconds spent loading chunks.
- `MixinEntityRenderDispatcher`: Counts the exact millisecond cost of rendering entities.
- **Result:** NOZH creates a "Load Profile". It knows if you are suffering from **CPU Lag** (Chunk loading) or **GPU Lag** (Too many shaders/entities).

---

## 🛑 Surgical Entity Control / Control de Entidades

FPS drops are often caused by specific entities. NOZH implements **Capability Providers** to control them surgically during detected lag spikes:

- **🛡️ Armor Stands**: Automatically hidden in lobbies/museums when FPS drops below target (e.g., < 60).
- **🖼️ Item Frames**: Massive storage rooms are culled intelligently.
- **📦 Block Entities**: Chests, Shulkers, Signs, and Bells are hidden if they exceed rendering budgets.
- **✨ Animations**: Global disable switch for textures/particles during mass-events (like TNT explosions).

---

## 🎮 Installation & Usage / Instalación y Uso

**Requirements:**

- Minecraft 1.20.1
- Fabric Loader

**How to Install:**

1. Drop `nozh-x.x.x.jar` into your `mods` folder.
2. Launch the game.
3. **That's it.** NOZH starts in "Auto-Tuning" mode by default.

### Commands / Comandos

| Command | Description (English) | Descripción (Español) |
| :--- | :--- | :--- |
| `/nozh selfcheck` | **Run first!** Diagnoses system health and verifies Director status. | **¡Ejecuta esto primero!** Diagnostica la salud del sistema y verifica al Director. |
| `/nozh status` | Shows current mode (Active/Passive/Safe) and Uptime. | Muestra el modo actual (Activo/Pasivo/Seguro) y tiempo de actividad. |
| `/nozh history` | Shows the last 10 decisions made by the AI. | Muestra las últimas 10 decisiones tomadas por la IA. |
| `/nozh perf` | Displays precise frametime analysis (avg, p95, spikes). | Muestra análisis preciso de tiempos de cuadro (promedio, p95, picos). |
| `/nozh safemode reset` | Forces exit from Safe Mode if triggered accidentally. | Fuerza la salida del Modo Seguro si se activó por accidente. |

### Configuration / Configuración (ModMenu)

Press `O` inside the Mod Menu to open the dashboard.

- **Target FPS**: The framerate NOZH tries to maintain (Default: 60).
- **Strategy Presets**:
  - `High` (High-End PC): Minimal intervention.
  - `Mid` (Standard): Balanced.
  - `Low` (Potato PC): Aggressive optimization.
- **Rollback System**: If enabled, NOZH reverts changes that make FPS worse.

---

## ❓ FAQ / Preguntas Frecuentes

**Q: Is this client-side only? / ¿Es solo del lado del cliente?**
A: **Yes.** You can use it on any server (Hypixel, SMPs, etc.) without it being installed on the server.
*R: **Sí.** Puedes usarlo en cualquier servidor sin que esté instalado en el servidor.*

**Q: Does it work with OptiFine? / ¿Funciona con OptiFine?**
A: No. NOZH is designed for the modern Fabric ecosystem (Sodium/Iris). OptiFine is obsolete in this environment and breaks the Mixins.
*R: No. NOZH está diseñado para el ecosistema moderno de Fabric. OptiFine es obsoleto aquí y rompe los Mixins.*

**Q: Why "God Mode"? / ¿Por qué "Modo Dios"?**
A: Because it has "True Sight" (sees internal code) and "Omnipotence" (controls other mods).
*R: Porque tiene "Visión Verdadera" (ve el código interno) y "Omnipotencia" (controla otros mods).*

---

<div align="center">
  <p><i>Code written with 0% allocation overhead. Logic executing in O(1).</i></p>
  <p><b>NOZH: Optimization Solved.</b></p>
</div>
