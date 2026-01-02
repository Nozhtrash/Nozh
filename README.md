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

### 🇬🇧 English: The Director of Performance

**Stop guessing. Start Orchestrating.**

Most optimization mods are dumb tools. They reduce fidelity blindly to get more frames.
**NOZH is different. NOZH is an AI Manager.**

It sits above your other mods (Sodium, Iris, Lithium, etc.) and acts as a **Director**. It monitors your game engine in real-time, learns from your specific hardware, and makes split-second decisions to keep your framerate perfectly smooth without ruining your visuals.

#### 🚀 Features that feel like Magic

🧠 **Session Learning (Persistent AI)**
NOZH remembers you.
If it attempts an optimization strategy (e.g., reducing view distance) and your FPS *doesn't* improve, it **learns** that your bottleneck is elsewhere (e.g., CPU vs GPU). It saves this knowledge to a persistent database on your disk. It will **never** make the same mistake twice on your machine.

👮 **The Great Orchestrator**
You have 300 mods? No problem.
NOZH detects over **25+ major optimization mods** (Sodium, Iris, Bobby, C2ME, FerriteCore, etc.) and seamlessly integrates with them.

- **Conflict Free:** It knows exactly which mod handles what. It won't fight Bobby for render distance. It won't fight Sodium Extra for particles.
- **Gap Filler:** It identifies gaps in your optimization stack and fills them instantly.

👀 **True Sight (Deep Engine Hook)**
NOZH doesn't guess if you're lagging. It *knows*.
By injecting directly into the Minecraft Chunk Manager and Render Pipeline via Mixins, it sees the exact millisecond a chunk loads or an entity renders. It distinguishes between **"World Loading Lag"** and **"Entity Overload Lag"** with 100% accuracy, applying the correct cure for the specific disease.

🛑 **Surgical Entity Control**
Entities are the #1 FPS killer in modded Minecraft. NOZH introduces **surgical culling** capabilities that no other mod has:

- **Armor Stands**: Auto-hides hundreds of stands in lobby/museum areas to recover 50+ FPS instantly.
- **Item Frames**: Dynamically culls massive storage room walls.
- **Block Entities**: Silences Chests, Shulkers, and Signs when the engine screams for mercy.
- **Global Animation Muter**: Instantly shuts down all particle/texture noise during critical lag spikes.

�️ **Divine Stability**

- **Zero-Allocation Architecture**: We audited every line. NOZH creates **zero garbage** in hot paths, eliminating the "micro-stutter" caused by Java's Garbage Collector.
- **Crash Guard**: If NOZH ever causes a crash, it auto-detects the failure loop and disables itself on the next boot. It is safe to install on any pack.

---

### 🎮 How to Use

NOZH is designed to be **Install & Forget**. However, you retain full control.

**Commands:**

- `/nozh status` - View the active state of the Director and Session AI.
- `/nozh selfcheck` - **Run this first!** It performs a deep system audit and confirms that God Mode modules (Director, Entity Control, True Sight) are active.
- `/nozh history` - See the last decisions the AI made in real-time.
- `/nozh perf` - View a millisecond-precise frametime snapshot of your game performance.

**Configuration:**
Press `O` (or configured key) inside ModMenu to access the **Interactive Dashboard**.

- Fully bilingual (English/Spanish).
- Tooltips explain every single option.

---

### 🇪🇸 Español: El Director de Rendimiento

**Deja de adivinar. Empieza a Orquestar.**

La mayoría de los mods de optimización son herramientas tontas. Reducen gráficos ciegamente.
**NOZH es diferente. NOZH es un Gerente Inteligente.**

Se sitúa por encima de tus otros mods (Sodium, Iris, Lithium, etc.) y actúa como un **Director**. Monitorea el motor del juego en tiempo real, aprende de tu hardware específico y toma decisiones en milisegundos para mantener tus FPS perfectamente suaves sin arruinar tus visuales.

#### 🚀 Características que parecen Magia

🧠 **Aprendizaje de Sesión (IA Persistente)**
NOZH te recuerda.
Si intenta una estrategia (ej. reducir distancia de visión) y tus FPS *no* mejoran, **aprende** que tu cuello de botella está en otro lado (ej. CPU vs GPU). Guarda este conocimiento en una base de datos en tu disco. **Nunca** cometerá el mismo error dos veces en tu PC.

👮 **El Gran Orquestador**
¿Tienes 300 mods? No hay problema.
NOZH detecta más de **25+ mods de optimización** (Sodium, Iris, Bobby, C2ME, FerriteCore, etc.) y se integra perfectamente.

- **Sin Conflictos:** Sabe exactamente qué mod maneja qué cosa. No peleará con Bobby por la distancia. No peleará con Sodium Extra por las partículas.
- **Rellena Huecos:** Identifica qué te falta optimizar y lo soluciona al instante.

👀 **Visión Verdadera (True Sight)**
NOZH no adivina si tienes lag. Lo *sabe*.
Al inyectarse directamente en el Gestor de Chunks y la Tubería de Renderizado (Mixins), ve el milisegundo exacto en que carga un chunk o se renderiza una entidad. Distingue entre **"Lag por Carga de Mundo"** y **"Lag por Exceso de Entidades"** con 100% de precisión, aplicando la cura correcta para la enfermedad exacta.

🛑 **Control Quirúrgico de Entidades**
Las entidades son el asesino #1 de FPS en Minecraft con mods. NOZH introduce **recorte quirúrgico**:

- **Armor Stands**: Oculta automáticamente cientos de soportes en lobbies/museos para recuperar 50+ FPS al instante.
- **Item Frames**: Recorta dinámicamente paredes masivas de salas de cofres.
- **Block Entities**: Silencia Cofres, Shulkers y Carteles cuando el motor pide piedad.
- **Silenciador de Animaciones**: Apaga instantáneamente todo el ruido de partículas/texturas durante picos críticos de lag.

�️ **Estabilidad Divina**

- **Arquitectura Cero-Asignación**: Auditamos cada línea. NOZH crea **cero basura** en la memoria, eliminando los "micro-cortes" causados por el Recolector de Basura de Java.
- **Guardia Anti-Crash**: Si NOZH causa un error, detecta el bucle de fallos y se desactiva solo en el siguiente inicio. Es seguro instalarlo en cualquier modpack.

---

### 🎮 Cómo Usar

NOZH está diseñado para **Instalar y Olvidar**. Sin embargo, tienes el control total.

**Comandos:**

- `/nozh status` - Ver el estado activo del Director y la IA de Sesión.
- `/nozh selfcheck` - **¡Ejecuta esto primero!** Realiza una auditoría profunda del sistema y confirma que los módulos "God Mode" están activos.
- `/nozh history` - Mira las últimas decisiones que tomó la IA en tiempo real.
- `/nozh perf` - Mira una instantánea precisa de milisegundos del rendimiento de tu juego.

**Configuración:**
Presiona `O` (o la tecla configurada) dentro de ModMenu para acceder al **Panel Interactivo**.

- Completamente bilingüe (Inglés/Español nativo para toda LATAM y España).
- Tooltips detallados que te explican qué hace cada opción al pasar el mouse.

---

<div align="center">
  <p><i>Orchestrated by Nozh. Stability via Intelligence.</i></p>
</div>
