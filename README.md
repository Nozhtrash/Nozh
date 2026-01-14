<div align="center">
  <img src="https://via.placeholder.com/300/000000/FFFFFF/?text=NOZH+v2.0" width="300" height="300" alt="NOZH Logo" />
  <h1>⚡ NOZH: The God Mode Update ⚡</h1>
  <h3>The First "Behavioral Governor" for Minecraft (Fabric 1.20.1)</h3>
  
  <p>
    <a href="https://github.com/Nozhtrash/Nozh/releases"><img src="https://img.shields.io/badge/Version-2.0.0_God_Mode-00FF00?style=for-the-badge&logo=appveyor" alt="Version"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Architecture-Neural_Network-FF0055?style=for-the-badge&logo=openai" alt="AI"></a>
    <a href="#"><img src="https://img.shields.io/badge/Status-Production_Ready-007EC6?style=for-the-badge&logo=shield" alt="Status"></a>
  </p>
  
  <h2>
    <a href="#-documentation-english">🇺🇸 DOCUMENTATION (ENGLISH)</a> |
    <a href="#-documentación-español">🇪🇸 DOCUMENTACIÓN (ESPAÑOL)</a>
  </h2>
</div>

<br><br>

---

<a name="-documentation-english"></a>

# 🇺🇸 DOCUMENTATION (ENGLISH)

## 📖 PREFACE: Beyond "Optimization"

To understand **NOZH**, you must first understand why traditional optimizers fail to fix *lag spikes*.

Mods like **Sodium**, **Lithium**, or **Starlight** are **Architectural Optimizers**. They rewrite the game's rendering or physics code to be mathematically vastly more efficient. They are brilliant, and NOZH relies on them. However, they are **Static**.

* If you set your Render Distance to 32 chunks, Sodium renders 32 chunks.
* It does this efficiently, but it *always* does it.
* If you enter a jungle with 500 entities and your FPS drops to 10... Sodium keeps rendering 32 chunks. It doesn't "know" you are suffering.

**NOZH** is a **Behavioral Governor**. It does not rewrite rendering code. Instead, it monitors your gameplay like a living thing.

* It asks: *"Is the FPS acceptable right now?"*
* If **YES**: It does nothing.
* If **NO**: It intervenes. It might temporarily drop Render Distance to 12. It might hide particles.
* Once performance recovers, it **restores your settings**.

**In short: Sodium builds a faster car. NOZH drives the car so you don't crash.**

---

## 🚀 SECTION 1: FOR BEGINNERS (The "TL;DR")

### What will this mod do for me?

1. **Eliminate Stutters**: Using a generic AI, it predicts when lag is about to happen and prevents it.
2. **Save Old PCs**: "Potato Mode" (see below) makes unplayable laptops playable.
3. **Automatic Tuning**: You don't need to watch YouTube tutorials on "Best Settings". NOZH finds them for you.

### Does it boost FPS?

* **Peak FPS (Standing still)**: No. Sodium does that.
* **Minimum FPS (Explosions/Combat)**: **YES, MASSIVELY.**
  * Where you usually drop to 15 FPS, NOZH keeps you at 45-60 FPS by dynamically sacrificing visuals you wouldn't notice anyway in the chaos.

### Potato Mode 🥔

If your PC has less than 4GB of RAM or an Intel Integrated Graphics card, NOZH will detect this on the **First Launch** and activate **Potato Mode**.

* This is a "Nuclear Option". It forces the game to look ugly (low distance, no shadows) to guarantee it is playable.
* *You can disable this in the config settings if you prefer pretty slides.*

---

## 🔬 SECTION 2: FOR EXPERTS (Deep Dive)

### The Architecture: "The Feedback Loop"

NOZH operates on a **2000ms Decision Cycle** (configurable). It is an autonomous agent loop:

1. **Sense (Telemetry)**:
    * `FabricFrameTickSampler` collects raw nanosecond timings.
    * Metrics: `avg_frametime`, `p95_frametime` (1% lows), `spike_count` (>50% deviation), `GC_pressure`.
    * **Anomaly Detector**: Uses Ping data to distinguish **Server Lag** (rubberbanding) from **Client Lag** (low FPS). If lag is network-related, NOZH **aborts optimization** (fixing graphics won't fix WiFi).

2. **Think (Prediction & Logic)**:
    * **Linear Regression**: Calculates the `slope` of the last 30 frames. If `slope > 0`, performance is degrading.
    * **Neural Perceptron**: A weighted model inputs `dEntity/dt`, `dChunk/dt`, `PlayerVelocity`.
        * Output is `P(lag)`. If `P > 0.8`, it triggers **Pre-emptive Action**.

3. **Act (Execution)**:
    * **Q-Learning**: The brain selects an action from the `ActionMatrix` (e.g., `reduce_render_distance`).
    * **Director Mode Bias**: If **Sodium** is installed, GPU-heavy actions are de-prioritized (Sodium handles them). If **C2ME** is installed, CPU-heavy actions are de-prioritized.
    * **Async Execution**: The configuration change is applied on a separate thread to ensure NOZH *never* causes a lag spike itself.

4. **Learn (Reinforcement)**:
    * After 1.5 seconds, NOZH measures `dFPS` (Change in FPS).
    * **Reward**: If FPS improved, the weight for that action is increased.
    * **Punish**: If FPS didn't change (or got worse), the action is blacklisted for 5 minutes.

### The Algorithm: Performance Predictor

NOZH uses **Online Least Squares Regression** and **Coefficient of Variation (CV)** for confidence.

* **Formula**: $y = mx + b$ on the `frametimeHistory` buffer.
* **Confidence**: $C = 1.0 - (StdDev / Mean)$.
* NOZH only trusts its own prediction if `C > 0.6` (Data is stable).
* **Input Vectors**:
  * $x_1$: Normalized Entity Count (0.0 - 1.0)
  * $x_2$: Chunk Update Delta
  * $x_3$: Player Velocity Magnitude

### Thread Safety

* All state mutations happen via `AtomicReference` and `ConcurrentHashMap`.
* The `Governor` runs off the Render Thread to prevent "Heisenbugs" (observing the lag causes more lag).

---

## 🥔 SECTION 3: POTATO MODE LEVELS (Detailed)

Potato Mode isn't just a toggle. It's a granular engine (`PotatoModeEngine.java`) that applies specific tiers based on hardware triggers.

| Level | Triggers (OR) | Specific Settings Applied |
|:---|:---|:---|
| **LEVEL 1 (Mild)** | `< 8GB RAM` OR `< 6 Cores` | • Render Dist: **12**<br>• Entity Dist: **8**<br>• Particles: **75%** |
| **LEVEL 2 (Moderate)** | `< 4GB RAM` OR `< 4 Cores` | • Render Dist: **8**<br>• Entity Dist: **6**<br>• Particles: **50%**<br>• Clouds: **OFF**<br>• Shadows: **OFF** |
| **LEVEL 3 (Aggressive)**| `< 3GB RAM` | • Render Dist: **6**<br>• Entity Dist: **4**<br>• Particles: **25%**<br>• Animations: **OFF** (No Fire/Water anims) |
| **LEVEL 4 (Extreme)** | `< 2GB RAM` OR `< 2 Cores` | • Render Dist: **4**<br>• Entity Dist: **3**<br>• Particles: **10%**<br>• **ALL FX OFF** |
| **EXTREME** | Manual Only | • Render Dist: **2**<br>• Particles: **0%**<br>• **HUD Hidden** |

> **Tech Note**: The RAM trigger uses `Runtime.getRuntime().maxMemory()`. This is the memory allocated to Java, not your total system RAM. Assigning more RAM in the launcher can move you from Level 3 to Level 1.

---

## 🤝 SECTION 4: COMPATIBILITY (Stewardship)

NOZH follows a **Stewardship Model**. It knows it is a guest in your modpack. It actively scans for other mods and yields control to avoid conflicts.

### The Mod Matrices

These rules are fetched from the cloud (`compatibility.json`) or hardcoded in `CompatRegistry.java`.

**1. Rendering Gods (Sodium/Embeddium)**

* **Status**: `DELEGATED`
* **Behavior**: NOZH disables its own chunk culling algorithms via Mixin cancellation. It delegates all Render Distance changes to Sodium's API.
* **Why**: Sodium's culling is O(1). NOZH's java-side culling is O(n). Sodium wins.

**2. Shader Engines (Iris/Oculus)**

* **Status**: `PROTECTED`
* **Behavior**: NOZH **locks** Cloud Rendering and Shadow Rendering to "ON".
* **Why**: Disabling vanilla clouds or shadows often breaks Shader pipeline composites, leading to black screens.

**3. Threading Engines (C2ME)**

* **Status**: `COOPERATIVE`
* **Behavior**: NOZH disables "Chunk Update Throttling".
* **Why**: C2ME manages thread priority dynamically. NOZH interfering would cause thread starvation.

**4. Utility Mods**

* **ModMenu**: NOZH injects a configuration button.
* **DynamicFPS**: NOZH detects this and disables its own "Unfocused FPS Limiter".
* **VulkanMod**: Fully supported (Legacy OpenGL calls are strictly avoided).

---

## ⚙️ SECTION 5: CONFIGURATION GUIDE

Access via `/nozh gui` or ModMenu.

### General Tab

* **Enabled**: Master switch.
* **Governor Mode**:
  * `NEURAL`: Full AI (Recommended for high-end PCs).
  * `HEURISTIC`: Rule-based (Recommended for older PCs, 0% CPU overhead).
  * `HYBRID`: Rules for Panic, AI for Idle.

### System Tab

* **Factory Reset**: Deletes `config/nozh.json` and resets internal memory state. **Click this if your game renders weirdly.**
* **Config Backup**: Creates a timestamped .json copy.
* **Hot Reload**: Re-reads the config file from disk without restarting the game.

### Advanced Tab (The Brain Settings)

* `targetFps` (Default: 60): The goal. NOZH won't optimize if you are above this.
* `decisionInterval` (Default: 2000ms): How often the Governor checks for lag. Lower = More responsive but higher CPU usage.
* `learningRate` (Default: 0.1): How fast the AI changes its mind.
* `hysteresisTicks` (Default: 100): 5-second cooldown to prevent settings flickering (going back and forth).

---

<br><br><br><br>

---

<a name="-documentación-español"></a>

# 🇪🇸 DOCUMENTACIÓN (ESPAÑOL)

## 📖 PREFACIO: Más allá de la "Optimización"

Para entender **NOZH**, primero debes entender por qué los optimizadores tradicionales fallan al arreglar los *picos de lag*.

Mods como **Sodium**, **Lithium** o **Starlight** son **Optimizadores Arquitectónicos**. Reescriben el código del juego para que sea matemáticamente más eficiente. Son brillantes, y NOZH depende de ellos. Sin embargo, son **Estáticos**.

* Si pones tu Distancia de Renderizado en 32 chunks, Sodium renderiza 32 chunks.
* Lo hace eficientemente, pero *siempre* lo hace.
* Si entras a una jungla con 500 entidades y tus FPS bajan a 10... Sodium sigue renderizando 32 chunks. No "sabe" que estás sufriendo.

**NOZH** es un **Gobernador de Comportamiento**. No reescribe código de renderizado. En su lugar, monitorea tu juego como un ser vivo.

* Pregunta: *"¿Son aceptables los FPS ahora mismo?"*
* Si **SÍ**: No hace nada.
* Si **NO**: Interviene. Puede reducir temporalmente la distancia a 12. Puede ocultar partículas.
* Una vez que el rendimiento se recupera, **restaura tu configuración**.

**En resumen: Sodium construye un auto más rápido. NOZH conduce el auto para que no te estrelles.**

---

## 🚀 SECCIÓN 1: PARA PRINCIPIANTES (Resumen)

### ¿Qué hará este mod por mí?

1. **Eliminar Tartamudeos (Stutters)**: Usando una IA genérica, predice cuándo va a ocurrir el lag y lo previene.
2. **Salvar PCs Viejas**: El "Modo Patata" (ver abajo) hace jugables laptops que antes no podían correr el juego.
3. **Ajuste Automático**: No necesitas ver tutoriales de YouTube sobre "La Mejor Configuración". NOZH la encuentra por ti.

### ¿Aumenta los FPS?

* **FPS Máximos (Quedándose quieto)**: No. Sodium hace eso.
* **FPS Mínimos (Explosiones/Combate)**: **SÍ, MASIVAMENTE.**
  * Donde usualmente caerías a 15 FPS, NOZH te mantiene a 45-60 FPS sacrificando dinámicamente visuales que no notarías de todos modos en el caos.

### Modo Patata 🥔

Si tu PC tiene menos de 4GB de RAM o una tarjeta gráfica Intel Integrada, NOZH detectará esto en el **Primer Lanzamiento** y activará el **Modo Patata**.

* Esta es una "Opción Nuclear". Fuerza al juego a verse feo (baja distancia, sin sombras) para garantizar que sea jugable.
* *Puedes desactivar esto en la configuración si prefieres diapositivas bonitas.*

---

## 🔬 SECCIÓN 2: PARA EXPERTOS (Profundidad Técnica)

### La Arquitectura: "El Bucle de Retroalimentación"

NOZH opera en un **Ciclo de Decisión de 2000ms** (configurable). Es un bucle de agente autónomo:

1. **Sentir (Telemetría)**:
    * `FabricFrameTickSampler` recolecta tiempos crudos en nanosegundos.
    * Métricas: `avg_frametime` (promedio), `p95_frametime` (bajos del 1%), `spike_count` (desviación >50%), `GC_pressure`.
    * **Detector de Anomalías**: Usa datos de Ping para distinguir **Lag de Servidor** (rubberbanding) de **Lag de Cliente** (bajos FPS). Si el lag es de red, NOZH **aborta la optimización** (arreglar gráficos no arregla el WiFi).

2. **Pensar (Predicción y Lógica)**:
    * **Regresión Lineal**: Calcula la `pendiente` de los últimos 30 cuadros. Si `pendiente > 0`, el rendimiento se está degradando.
    * **Perceptrón Neuronal**: Un modelo ponderado toma `dEntidad/dt`, `dChunk/dt`, `VelocidadJugador`.
        * Salida es `P(lag)`. Si `P > 0.8`, dispara una **Acción Preventiva**.

3. **Actuar (Ejecución)**:
    * **Q-Learning**: El cerebro selecciona una acción de la `ActionMatrix` (ej. `reduce_render_distance`).
    * **Sesgo de Director (Director Mode)**: Si **Sodium** está instalado, las acciones de GPU se des-priorizan (Sodium las maneja). Si **C2ME** está instalado, las acciones de CPU se des-priorizan.
    * **Ejecución Asíncrona**: El cambio de configuración se aplica en un hilo separado para asegurar que NOZH *nunca* cause un pico de lag por sí mismo.

4. **Aprender (Refuerzo)**:
    * Después de 1.5 segundos, NOZH mide `dFPS` (Cambio en FPS).
    * **Recompensa**: Si los FPS mejoraron, el peso para esa acción se incrementa.
    * **Castigo**: Si los FPS no cambiaron (o empeoraron), la acción se pone en lista negra por 5 minutos.

### El Algoritmo: Performance Predictor

NOZH usa **Regresión de Mínimos Cuadrados en Línea** y **Coeficiente de Variación (CV)** para la confianza.

* **Fórmula**: $y = mx + b$ en el buffer `frametimeHistory`.
* **Confianza**: $C = 1.0 - (StdDev / Mean)$.
* NOZH solo confía en su propia predicción si `C > 0.6` (Los datos son estables).
* **Vectores de Entrada**:
  * $x_1$: Conteo de Entidades Normalizado (0.0 - 1.0)
  * $x_2$: Delta de Actualización de Chunks
  * $x_3$: Magnitud de Velocidad del Jugador

### Seguridad de Hilos

* Todas las mutaciones de estado ocurren vía `AtomicReference` y `ConcurrentHashMap`.
* El `Governor` corre fuera del Hilo de Renderizado para prevenir "Heisenbugs" (que observar el lag cause más lag).

---

## 🥔 SECCIÓN 3: NIVELES DEL MODO PATATA (Detallado)

El Modo Patata no es solo un interruptor. Es un motor granular (`PotatoModeEngine.java`) que aplica niveles específicos basados en disparadores de hardware.

| Nivel | Disparadores (O) | Ajustes Específicos Aplicados |
|:---|:---|:---|
| **NIVEL 1 (Leve)** | `< 8GB RAM` O `< 6 Núcleos` | • Dist Render: **12**<br>• Dist Entidad: **8**<br>• Partículas: **75%** |
| **NIVEL 2 (Moderado)** | `< 4GB RAM` O `< 4 Núcleos` | • Dist Render: **8**<br>• Dist Entidad: **6**<br>• Partículas: **50%**<br>• Nubes: **OFF**<br>• Sombras: **OFF** |
| **NIVEL 3 (Agresivo)**| `< 3GB RAM` | • Dist Render: **6**<br>• Dist Entidad: **4**<br>• Partículas: **25%**<br>• Animaciones: **OFF** (No Fuego/Agua) |
| **NIVEL 4 (Extremo)** | `< 2GB RAM` O `< 2 Núcleos` | • Dist Render: **4**<br>• Dist Entidad: **3**<br>• Partículas: **10%**<br>• **TODO FX OFF** |
| **EXTREMO** | Solo Manual | • Dist Render: **2**<br>• Partículas: **0%**<br>• **HUD Oculto** |

> **Nota Técnica**: El disparador de RAM usa `Runtime.getRuntime().maxMemory()`. Esta es la memoria asignada a Java, no tu RAM total del sistema. Asignar más RAM en el launcher puede moverte del Nivel 3 al Nivel 1.

---

## 🤝 SECCIÓN 4: COMPATIBILIDAD (Mayordomía)

NOZH sigue un **Modelo de Mayordomía**. Sabe que es un invitado en tu modpack. Escanea activamente otros mods y cede el control para evitar conflictos.

### Las Matrices de Mods

Estas reglas se obtienen de la nube (`compatibility.json`) o están codificadas en `CompatRegistry.java`.

**1. Dioses del Renderizado (Sodium/Embeddium)**

* **Estado**: `DELEGADO`
* **Comportamiento**: NOZH desactiva sus propios algoritmos de culling de chunks vía cancelación de Mixin. Delega todos los cambios de Distancia de Renderizado a la API de Sodium.
* **Por qué**: El culling de Sodium es O(1). El de NOZH en Java es O(n). Sodium gana.

**2. Motores de Shaders (Iris/Oculus)**

* **Estado**: `PROTEGIDO`
* **Comportamiento**: NOZH **bloquea** el Renderizado de Nubes y Sombras en "ENCENDIDO".
* **Por qué**: Desactivar nubes o sombras vanilla a menudo rompe los compositores de los Shaders, llevando a pantallas negras.

**3. Motores de Hilos (C2ME)**

* **Estado**: `COOPERATIVO`
* **Comportamiento**: NOZH desactiva el "Throttling de Actualización de Chunks".
* **Por qué**: C2ME gestiona la prioridad de hilos dinámicamente. Que NOZH interfiera causaría inanición de hilos.

**4. Mods de Utilidad**

* **ModMenu**: NOZH inyecta un botón de configuración.
* **DynamicFPS**: NOZH detecta esto y desactiva su propio "Limitador de FPS en Segundo Plano".
* **VulkanMod**: Completamente soportado (Las llamadas Legacy OpenGL son estrictamente evitadas).

---

## ⚙️ SECCIÓN 5: GUÍA DE CONFIGURACIÓN

Acceso vía `/nozh gui` o ModMenu.

### Pestaña General

* **Enabled (Activado)**: Interruptor maestro.
* **Governor Mode (Modo Gobernador)**:
  * `NEURAL`: IA completa (Recomendado para PCs potentes).
  * `HEURISTIC`: Basado en reglas (Recomendado para PCs viejas, 0% uso CPU).
  * `HYBRID`: Reglas para Pánico, IA para Reposo.

### Pestaña Sistema

* **Factory Reset**: Borra `config/nozh.json` y resetea el estado de memoria. **Haz clic si tu juego se ve raro.**
* **Config Backup**: Crea una copia .json con fecha.
* **Hot Reload**: Relee el archivo de configuración desde el disco sin reiniciar el juego.

### Pestaña Avanzada (Ajustes del Cerebro)

* `targetFps` (Defecto: 60): La meta. NOZH no optimizará si estás por encima.
* `decisionInterval` (Defecto: 2000ms): Con qué frecuencia el Gobernador busca lag. Más bajo = Más responsivo pero mayor uso de CPU.
* `learningRate` (Defecto: 0.1): Qué tan rápido cambia de opinión la IA.
* `hysteresisTicks` (Defecto: 100): Enfriamiento de 5 segundos para prevenir parpadeo de configuraciones (ir y venir).

---

<div align="center">
  <p><i>Made with ❤️ by the Nozhtrash Team. Open Source. Transparent. Honest.</i></p>
</div>
