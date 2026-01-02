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

## 🌎 Introduction / Introducción

### 🇬🇧 English

**NOZH** is an intelligent orchestration layer for your Minecraft instance. Unlike traditional optimization mods that use static rules (e.g., "always disable fog"), NOZH uses a dynamic **Governor System** that monitors your game's internal state 20 times per second.

It detects **Scenario Contexts** (Are you AFK? In Combat? Mining? Building?) and adjusts the engine parameters in real-time to maximize FPS when you need it most, and maximize visual quality when you can afford it.

### 🇪🇸 Español

**NOZH** es una capa de orquestación inteligente para tu Minecraft. A diferencia de los mods tradicionales que usan reglas estáticas (ej. "siempre desactivar niebla"), NOZH usa un **Sistema Gobernador** dinámico que monitorea el estado interno de tu juego 20 veces por segundo.

Detecta **Escenarios** (¿Estás AFK? ¿En Combate? ¿Minando? ¿Construyendo?) y ajusta los parámetros del motor en tiempo real para maximizar los FPS cuando más los necesitas, y la calidad visual cuando te lo puedes permitir.

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
