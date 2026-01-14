<div align="center">
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>Orquestador de Rendimiento Inteligente para Minecraft (Fabric)</b><br>
    <i>El Primer Optimizador "Inteligente" que se Adapta a Tu Hardware (v2.0 Profesional)</i>
  </p>

  <p>
    <a href="https://github.com/NozhMod/Nozh/actions"><img src="https://img.shields.io/badge/Estado-PROFESIONAL%20v2.0-00FF00?style=for-the-badge&logo=appveyor" alt="Estado"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Minecraft-1.20.1-FFA500?style=for-the-badge&logo=minecraft" alt="Version"></a>
    <a href="docs/FEATURES.md"><img src="https://img.shields.io/badge/Leer-CARACTERISTICAS-blue?style=for-the-badge" alt="Caracteristicas"></a>
  </p>
</div>

---

# 📖 Resumen

**NOZH** no es solo un mod de optimización; es un **Gobernador de Rendimiento Impulsado por IA**. A diferencia de los mods estándar (Sodium, FerriteCore) que optimizan *el código de renderizado*, NOZH optimiza el *comportamiento*.

Observa tu juego en tiempo real. Si detecta lag, sacrifica inteligentemente efectos visuales específicos (como nubes, partículas o sombras) para restaurar unos fluidos 60 FPS. Cuando el peligro pasa, los restaura.

### 🧠 ¿Por qué es "Inteligente"?

- **Predicción Neuronal de Lag**: Usa una IA (Perceptrón) para predecir picos de lag *antes* de que ocurran.
- **Conciencia de Escenario**: Conoce la diferencia entre **PvP** (donde los fps importan) y **Construcción** (donde los visuales importan).
- **Perfilado de Hardware**: Identifica si estás ejecutando en una laptop "Patata" o una PC de Gama Alta y se ajusta automáticamente.
- **Conocimiento de Mods**: Detecta automáticamente modpacks (ej. Mods Técnicos, Mágicos) y ajusta la configuración para evitar crasheos.

---

# ✨ Características Principales

| Característica | Descripción |
|----------------|-------------|
| **Panel Premium** | Un menú in-game de calidad AAA (`/nozh gui`) con gráficos de telemetría en tiempo real. |
| **IA Neuronal** | Aprende de tu juego. Si la `Acción A` arregló el lag la última vez, la prioriza la próxima. |
| **Modo Patata** | Un perfil especial `EXTREMO` para PCs con 2GB RAM / Gráficas Intel HD. |
| **Herramientas del Sistema** | Herramientas integradas de **Restablecimiento de Fábrica**, **Backup de Config** y **Reparación en un Clic**. |
| **IA Híbrida** | Motor de decisión configurable: Elige entre lógica **Neuronal**, **Heurística** o **Híbrida**. |
| **Reglas en la Nube** | Obtiene actualizaciones de compatibilidad en vivo desde la nube para prevenir conflictos con nuevos mods. |
| **CrashGuard** | Detecta bucles de arranque y aísla automáticamente el problema para permitirte iniciar el juego. |

👉 **[Leer la Guía Completa de Características (Para Novatos y Expertos)](docs/FEATURES.md)**

---

# 🤖 Cómo Funciona (El Orquestador)

NOZH actúa como un **Supervisor** para tu cliente de Minecraft.

1. **Monitorear**: `VitalsRecorder` mide los Tiempos de Fotograma (ms) y Latencia de Red (Ping).
2. **Analizar**: `AnomalyDetector` determina si un pico de lag es causado por **Gráficos** (GPU) o **Servidor** (Red).
3. **Decidir**: El `Gobernador` (Cerebro) revisa la **Matriz de Acción** para encontrar la mejor solución para el **Escenario** actual.
    - *Ejemplo: "El jugador está en COMBATE. FPS bajos. Desactivar PARTÍCULAS."*
4. **Ejecutar**: Aplica el cambio instantáneamente.
5. **Verificar**: Si los FPS no mejoran en 45 segundos, el **Sistema de Rollback** deshace el cambio.

👉 **[Profundización Técnica (Arquitectura)](docs/ARCHITECTURE.md)**

---

# 🚀 Empezando

### Instalación

1. Instala **Fabric Loader** (1.20.1).
2. Instala **Fabric API**.
3. Arrastra `nozh-2.0.0.jar` a tu carpeta `mods`.
4. ¡Lanza el juego!

### Primera Ejecución

En tu primer lanzamiento, NOZH abrirá el **Asistente de Configuración**.

- Elige **"Modo Patata"** si tienes una PC lenta.
- Elige **"Alta Fidelidad"** si tienes una GPU fuerte.

### Comandos

- `/nozh gui` - Abrir el Panel Principal.
- `/nozh hud <mode>` - Cambiar la info en pantalla (Minimal/Compact/Expert).
- `/nozh status` - Ver qué está pensando la IA.

---

<div align="center">
  <p><i>Hecho con ❤️ por el Equipo Nozhtrash</i></p>
  <p><b>Transparencia • Inteligencia • Rendimiento</b></p>
</div>
