<div align="center">
  <img src="https://via.placeholder.com/256/000000/FFFFFF/?text=NOZH+v2.0" width="256" height="256" alt="NOZH Logo" />
  <h1>⚡ NOZH ⚡</h1>
  <h3>The Intelligent Performance Orchestrator (Fabric 1.20.1)</h3>
  
  <p>
    <a href="https://github.com/Nozhtrash/Nozh/releases"><img src="https://img.shields.io/badge/Version-2.0.0_God_Mode-00FF00?style=for-the-badge&logo=appveyor" alt="Version"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Download-Modrinth-00AF5C?style=for-the-badge&logo=modrinth" alt="Modrinth"></a>
    <a href="#"><img src="https://img.shields.io/badge/Download-CurseForge-F16436?style=for-the-badge&logo=curseforge" alt="CurseForge"></a>
  </p>
  
  <big><b>🇬🇧 <a href="#-english-documentation">ENGLISH DOCUMENTATION</a> | 🇪🇸 <a href="#-documentación-en-español">DOCUMENTACIÓN EN ESPAÑOL</a></b></big>
</div>

<br><br>

---

# 🇬🇧 ENGLISH DOCUMENTATION

## 📖 Introduction: A New Paradigm

**NOZH** (Novus Optima Zen HUD) challenges the traditional definition of an "Optimizer".
Traditional mods like **Sodium** or **Lithium** are *Passive Optimizers*. They rewrite game code to be faster. They are essential foundation layers.
**NOZH** is an *Active Orchestrator*. It sits on top of those mods and behaves like a "Director" or "Governor".

It asks a simple question every second:
> *"Is the player suffering from lag right now?"*

If the answer is **NO**: NOZH sleeps. It consumes 0 resources.
If the answer is **YES**: NOZH wakes up and makes intelligent sacrifices to save your framerate.

This means NOZH is dynamic. It might reduce your render distance from 12 to 8 during a massive explosion, and then assume it back to 12 when the dust settles. It optimizes *gameplay flow*, not just code.

---

## 🧠 The Intelligence (How it Thinks)

NOZH is powered by a **Behavioral Governor** that comes in three flavors found in `/nozh gui` -> **Advanced**:

### 1. The Perceptron (Neural AI)

This is a Single-Layer Feedforward Neural Network implemented in Java (`PerformancePredictor.java`).

* **How it works**: It takes 4 Real-Time Inputs:
    1. **Entity Pressure**: (dEntity/dt) Are mobs spawning rapidly?
    2. **Chunk Pressure**: (dChunk/dt) Is the world generating faster than the CPU can handle?
    3. **Frame Variance**: Are we seeing micro-stutters?
    4. **Player Velocity**: Are we flying with Elytra?
* **The Math**: It calculates a weighted sum: $P = \sum (Input_i \times Weight_i)$
* **Online Learning**: If NOZH predicts lag, takes action, and FPS improves, it **increases the weight** of that cause. It learns *your* hardware's bottlenecks. A laptop user might have high weights for Rendering (GPU), while a server player might have high weights for Chunk Loading (CPU).

### 2. Heuristics (Rule-Based)

For users who prefer predictability.

* **Logic**: A strict set of IF/THEN rules.
* *Example*: `IF (FPS < 45 AND TimeInCombat > 5s) THEN { CullParticles(); }`
* **Pros**: Instant reaction time (0ms warmup).
* **Cons**: Can be jarring if rules trigger too aggressively.

### 3. Hybrid (Recommended)

Combines both. Uses **Heuristics** for emergency "Panic" situations (FPS < 20) and **Neural** for background fine-tuning to prevent stutters before they happen.

---

## 🥔 Potato Mode: Saving Low-End PCs

A dedicated engine for hardware that barely meets Minecraft's minimum specs.
**Auto-Detection**: On first launch, NOZH checks `Runtime.getRuntime().maxMemory()` and GPU Vendor strings.

* If **RAM < 4GB** OR **GPU == Intel HD/UHD**: Auto-activates Potato Mode.

### The Levels

| Support Level | RAM | Cores | Actions Taken |
|:---|:---|:---|:---|
| **Level 1 (Mild)** | < 8GB | < 6 | Caps Particle Distance to 16m. Reduces Render Distance to 12. |
| **Level 2 (Moderate)** | < 4GB | < 4 | Disables Clouds. Reduces Entity Distance. Caps Particles to 50%. |
| **Level 3 (Aggressive)** | < 3GB | -- | Disables Texture Animations (Water/Lava). Reduces Render Distance to 6. |
| **Level 4 (Extreme)** | < 2GB | < 2 | Disables ALL Particles. Minimal HUD. Render Distance locked to 4. |

> **Transparency Note**: Potato Mode makes the game look significantly worse. This is intentional. It prioritizes *playability* over *aesthetics*.

---

## 🛠️ System Tools & Resilience

NOZH v2.0 introduces professional tools to manage your installation directly from the main menu.

### 🏭 Factory Reset

Located in the **System Tab**.

* **Function**: Completely wipes `config/nozh.json` and re-initializes the configuration state in memory.
* **Use Case**: You changed a setting that turned the screen black or broke rendering.

### 💾 Config Backup & Export

* **Backup**: Saves a timestamped copy of your settings.
* **Clipboard Export**: Serializes your entire configuration path to a JSON string and copies it to the clipboard.
  * *Why?* You can paste this to a friend or support dev to replicate your exact setup instantly.

### 🔥 Hot Reload

Allows editing the `config/nozh.json` file manually with a text editor while the game is running. Pressing "Hot Reload" ingests the file changes immediately without requiring a game restart.

---

## 🤝 Compatibility Stewardship

NOZH follows a strict "Do No Harm" policy. It is aware of the Fabric ecosystem.

### The "Stewardship" Matrix

| Mod Detected | NOZH Reaction | Reasoning |
|:---|:---|:---|
| **Sodium / Embeddium** | **Delegates Rendering** | Sodium is a better renderer. NOZH disables its own chunk optimization logic and becomes a high-level manager for Sodium settings. |
| **Iris / Oculus** | **Protects Visuals** | Disabling Clouds/Shadows often breaks Shader packs. NOZH automatically locks these settings to "ON" when Iris is detected. |
| **C2ME** | **Relaxes Threading** | C2ME handles chunk threading better. NOZH disables its chunk priority override. |
| **ModMenu** | **Integrates** | Injects the settings button natively into the mod list. |
| **Dynamic FPS** | **Yields** | Detects that FPS limiting is handled externally and disables its own background limiter. |

*This list is updated live via the Cloud Config system (`compatibility.json`) every time you launch the game.*

---

<br><br>

---

# 🇪🇸 DOCUMENTACIÓN EN ESPAÑOL

## 📖 Introducción: Un Nuevo Paradigma

**NOZH** (Novus Optima Zen HUD) desafía la definición tradicional de un "Optimizador".
Los mods tradicionales como **Sodium** o **Lithium** son *Optimizadores Pasivos*. Reescriben el código del juego para que sea más rápido. Son capas fundamentales esenciales.
**NOZH** es un *Orquestador Activo*. Se sienta encima de esos mods y se comporta como un "Director" o "Gobernador".

Se hace una pregunta simple cada segundo:
> *"¿Está sufriendo el jugador de lag en este momento?"*

Si la respuesta es **NO**: NOZH duerme. Consume 0 recursos.
Si la respuesta es **SÍ**: NOZH despierta y hace sacrificios inteligentes para salvar tus cuadros por segundo.

Esto significa que NOZH es dinámico. Puede reducir tu distancia de renderizado de 12 a 8 durante una explosión masiva, y luego subirla de nuevo a 12 cuando el polvo se asienta. Optimiza el *flujo de juego*, no solo el código.

---

## 🧠 La Inteligencia (Cómo Piensa)

NOZH está impulsado por un **Gobernador de Comportamiento** que viene en tres sabores encontrados en `/nozh gui` -> **Avanzado**:

### 1. El Perceptrón (IA Neuronal)

Esta es una Red Neuronal de Alimentación Hacia Adelante de Capa Única implementada en Java (`PerformancePredictor.java`).

* **Cómo funciona**: Toma 4 Entradas en Tiempo Real:
    1. **Presión de Entidades**: (dEntity/dt) ¿Están apareciendo mobs rápidamente?
    2. **Presión de Chunks**: (dChunk/dt) ¿Se está generando el mundo más rápido de lo que la CPU puede manejar?
    3. **Varianza de Cuadros**: ¿Estamos viendo micro-tartamudeos?
    4. **Velocidad del Jugador**: ¿Estamos volando con Elytras?
* **La Matemática**: Calcula una suma ponderada: $P = \sum (Input_i \times Weight_i)$
* **Aprendizaje en Línea**: Si NOZH predice lag, toma acción, y los FPS mejoran, **aumenta el peso** de esa causa. Aprende los cuellos de botella de *tu* hardware. Un usuario de laptop puede tener pesos altos para Renderizado (GPU), mientras que un jugador de servidor puede tener pesos altos para Carga de Chunks (CPU).

### 2. Heurística (Basada en Reglas)

Para usuarios que prefieren previsibilidad.

* **Lógica**: Un conjunto estricto de reglas SI/ENTONCES.
* *Ejemplo*: `SI (FPS < 45 Y TiempoEnCombate > 5s) ENTONCES { OcultarParticulas(); }`
* **Pros**: Tiempo de reacción instantáneo (0ms de calentamiento).
* **Contras**: Puede ser brusco si las reglas se activan demasiado agresivamente.

### 3. Híbrido (Recomendado)

Combina ambos. Usa **Heurística** para situaciones de emergencia "Pánico" (FPS < 20) y **Neuronal** para ajustes finos en segundo plano para prevenir tartamudeos antes de que ocurran.

---

## 🥔 Modo Patata: Salvando PCs de Gama Baja

Un motor dedicado para hardware que apenas cumple con los requisitos mínimos de Minecraft.
**Auto-Detección**: En el primer lanzamiento, NOZH verifica `Runtime.getRuntime().maxMemory()` y las cadenas del Vendedor de GPU.

* Si **RAM < 4GB** O **GPU == Intel HD/UHD**: Auto-activa el Modo Patata.

### Los Niveles

| Nivel de Soporte | RAM | Núcleos | Acciones Tomadas |
|:---|:---|:---|:---|
| **Nivel 1 (Leve)** | < 8GB | < 6 | Limita Distancia de Partículas a 16m. Reduce Distancia de Renderizado a 12. |
| **Nivel 2 (Moderado)** | < 4GB | < 4 | Desactiva Nubes. Reduce Distancia de Entidades. Limita Partículas al 50%. |
| **Nivel 3 (Agresivo)** | < 3GB | -- | Desactiva Animaciones de Texturas (Agua/Lava). Reduce Distancia de Renderizado a 6. |
| **Nivel 4 (Extremo)** | < 2GB | < 2 | Desactiva TODAS las Partículas. HUD Mínimo. Distancia de Renderizado bloqueada a 4. |

> **Nota de Transparencia**: El Modo Patata hace que el juego se vea significativamente peor. Esto es intencional. Prioriza la *jugabilidad* sobre la *estética*.

---

## 🛠️ Herramientas del Sistema y Resiliencia

NOZH v2.0 introduce herramientas profesionales para gestionar tu instalación directamente desde el menú principal.

### 🏭 Restablecimiento de Fábrica

Ubicado en la **Pestaña Sistema**.

* **Función**: Borra completamente `config/nozh.json` y reinicializa el estado de configuración en memoria.
* **Caso de Uso**: Cambiaste una configuración que puso la pantalla negra o rompió el renderizado.

### 💾 Backup y Exportación de Config

* **Backup**: Guarda una copia con marca de tiempo de tus ajustes.
* **Exportar al Portapapeles**: Serializa tu ruta de configuración entera a una cadena JSON y la copia al portapapeles.
  * *¿Por qué?* Puedes pegar esto a un amigo o desarrollador de soporte para replicar tu configuración exacta al instante.

### 🔥 Recarga en Caliente

Permite editar el archivo `config/nozh.json` manualmente con un editor de texto mientras el juego se ejecuta. Presionar "Recarga en Caliente" ingiere los cambios del archivo inmediatamente sin requerir reiniciar el juego.

---

## 🤝 Mayordomía de Compatibilidad

NOZH sigue una estricta política de "No Hacer Daño". Es consciente del ecosistema Fabric.

### La Matriz de "Mayordomía"

| Mod Detectado | Reacción de NOZH | Razonamiento |
|:---|:---|:---|
| **Sodium / Embeddium** | **Delega Renderizado** | Sodium es un mejor renderizador. NOZH desactiva su propia lógica de optimización de chunks y se convierte en un gestor de alto nivel para los ajustes de Sodium. |
| **Iris / Oculus** | **Protege Visuales** | Desactivar Nubes/Sombras a menudo rompe los paquetes de Shaders. NOZH bloquea automáticamente estos ajustes en "ENCENDIDO" cuando detecta Iris. |
| **C2ME** | **Relaja Hilos** | C2ME maneja los hilos de chunks mejor. NOZH desactiva su anulación de prioridad de chunks. |
| **ModMenu** | **Integra** | Inyecta el botón de ajustes nativamente en la lista de mods. |
| **Dynamic FPS** | **Cede** | Detecta que el límite de FPS es manejado externamente y desactiva su propio limitador en segundo plano. |

*Esta lista se actualiza en vivo vía el sistema de Cloud Config (`compatibility.json`) cada vez que lanzas el juego.*

---

<div align="center">
  <p><i>Made with ❤️ by the Nozhtrash Team. Open Source. Transparent.</i></p>
</div>
