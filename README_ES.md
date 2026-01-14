<div align="center">
  <img src="https://via.placeholder.com/150/000000/FFFFFF/?text=NOZH" width="128" height="128" alt="Logo de NOZH" />
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>El Orquestador de Rendimiento Inteligente para Minecraft (Fabric 1.20.1)</b><br>
    <i>"Un Gobernador, no solo un Optimizador."</i>
  </p>

  <p>
    <a href="https://github.com/Nozhtrash/Nozh/releases"><img src="https://img.shields.io/badge/Versión-2.0.0_God_Mode-00FF00?style=for-the-badge&logo=appveyor" alt="Versión"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Plataforma-Fabric-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="https://discord.gg/nozh"><img src="https://img.shields.io/discord/1234567890?label=Discord&style=for-the-badge&logo=discord&color=5865F2" alt="Discord"></a>
  </p>
  
  <p>
    <a href="README.md">🇺🇸 <b>READ IN ENGLISH</b></a>
  </p>
</div>

---

# 📖 ¿Qué es NOZH?

**NOZH** (Novus Optima Zen HUD) NO es otro mod de "Subir FPS" como Sodium. Es un **Orquestador de Comportamiento**.

Mientra que Sodium optimiza *cómo* renderiza el juego, NOZH optimiza *qué* debe renderizar el juego. Actúa como un gerente que vigila tu partida en tiempo real y toma decisiones para mantener tus cuadros por segundo estables.

### 🧠 La Filosofía Central: "Orquestación"

La mayoría de mods son estáticos: configuras `Partículas: Todas` y se quedan así para siempre, incluso si te estás muriendo de lag en una raid.

**NOZH es dinámico**:

1. **Vigila**: Usa un `Perceptrón` (Red Neuronal Simple) para monitorear Cargas de Chunks, Cantidad de Entidades y Tiempo de Fotograma.
2. **Decide**: Si predice lag, reduce *temporalmente* la calidad (ej. desactiva nubes, recorta distancia de entidades).
3. **Restaura**: Tan pronto como el rendimiento se estabiliza, restaura tus visuales a la máxima calidad.

---

# ✨ Características Principales

| Característica | Verificación de Realidad (Transparencia) |
|:--- |:--- |
| **Modo Patata** | Un perfil especializado para hardware con <4GB RAM o Gráficas Integradas. Bloquea la distancia de renderizado a 2-6 chunks y recorta agresivamente entidades. **Hace que el juego se vea peor para que sea jugable.** |
| **Gobernador Neuronal** | Usa un algoritmo configurable (Neuronal, Heurístico o Híbrido) para predecir lag. No es ChatGPT; es un modelo matemático entrenado en tu sesión de juego para balancear detalle vs fluidez. |
| **Resiliencia del Sistema** | Incluye herramientas de **Restablecimiento de Fábrica** y **Backup de Config** directamente en el menú. Si rompes tu configuración, puedes arreglarla sin borrar archivos manualmente. |
| **Inteligencia en la Nube** | Descarga un archivo JSON de nuestro GitHub al iniciar (`compatibility.json`). Esto le dice a NOZH sobre nuevos mods para no romperlos (ej. auto-desactiva ajustes de shaders si encuentra Iris). |
| **HUD Premium** | Un HUD sin "basura" (zero-garbage) que muestra gráficos en tiempo real de tu Tiempo de Fotograma (ms). Verde = Bueno, Rojo = Malo. |

👉 **[Leer la Guía de Características Completa (Detallada)](docs/FEATURES_ES.md)**

---

# 🤖 "¿Es Compatible?"

**SÍ.** NOZH está diseñado para ser un "Buen Ciudadano".

Detecta activamente:

* **Sodium / Iris**: Delega tareas de renderizado a ellos. NOZH gestiona la *lógica*, Sodium gestiona los *gráficos*.
* **Lithium / Starlight**: Completamente compatible.
* **C2ME**: NOZH ajusta prioridades de chunks para evitar conflictos.
* **VulkanMod**: Detectado y respetado.

Revisa `docs/COMPATIBILITY.md` (Inglés) para la lista completa de más de 50 mods conocidos.

---

# 🚀 Instalación y Uso

### 1. Instalación

1. Requiere **Fabric Loader** y **Fabric API** para Minecraft 1.20.1.
2. Suelta `nozh-2.0.0.jar` en la carpeta `.minecraft/mods`.
3. (Opcional) Instala [ModMenu](https://modrinth.com/mod/modmenu) para acceder a la pantalla de configuración fácilmente.

### 2. Primer Inicio

NOZH ejecutará un **Escaneo de Hardware**.

* **PC Débil**: Auto-activa el `Modo Patata`.
* **PC Fuerte**: Por defecto entra en `Modo Supervivencia` (Alta Calidad).

### 3. Configuración

Presiona el botón **NOZH** en el menú de pausa (vía ModMenu) o escribe `/nozh gui`.

* **Pestaña Sistema**: Resetea o haz Backup de tu config.
* **Pestaña Avanzada**: Cambia entre gobernador Neuronal (IA) o Heurístico (Reglas).

---

# ⚠️ Descargo de Honestidad

NOZH no puede descargar más RAM para ti.

* Si tu PC es una tostadora, el **Modo Patata** ayudará, pero no hará que corra shaders a 120 FPS.
* La "IA" es un algoritmo local. No envía tus datos a OpenAI ni Google. Corre enteramente en tu CPU (impacto: <0.1ms por tick).

---

<div align="center">
  <p><i>Hecho con ❤️ por el Equipo Nozhtrash. Código Abierto. Transparente.</i></p>
</div>
