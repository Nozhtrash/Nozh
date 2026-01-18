# NOZH: Motor Inteligente de Ritmo de Fotogramas (Modo Dios)

> **[READ IN ENGLISH / LEER EN INGLÉS](README.md)**

**NOZH** es un mod de optimización de próxima generación para **Minecraft Fabric 1.20.1** (del lado del cliente). A diferencia de los "boosters de FPS" tradicionales que bajan la configuración a ciegas, NOZH utiliza un **motor híbrido neuronal** para estabilizar los tiempos de fotograma, erradicar los micro-cortes (`1% lows`) y equilibrar dinámicamente la calidad visual frente al rendimiento.

Si tu juego tartamudea durante el combate o la carga de chunks, NOZH lo soluciona sacrificando efectos visuales *solo cuando es necesario* y restaurándolos instantáneamente cuando la carga desaparece.

---

## 🚀 Características Clave (v2.1.0)

### 🧠 Optimización Inteligente

- **Predictor Perceptrón**: Una red neuronal ligera que predice picos de lag antes de que ocurran.
- **Calidad Dinámica**: Ajusta automáticamente la configuración de Sodium (distancia de renderizado, partículas, entidades) en tiempo real.
- **Detección de Escenarios**: Identifica qué estás haciendo (Combate, Exploración, AFK, Construcción) y cambia de perfil automáticamente.

### 🥔 Modo Patata (¡Nuevo!)

- **Protocolo de Emergencia**: Si tus FPS caen por debajo de un umbral crítico (ej. <20 FPS), NOZH activa el "Modo Patata".
- **Tácticas Agresivas**: Minimiza instantáneamente partículas, sombras y renderizado innecesario para restaurar la jugabilidad.
- **Activación Manual**: Actívalo manualmente con `[K]` (configurable).

### 🛡️ Sistemas de Seguridad

- **Reversión de Crisis (Rollback)**: Si NOZH hace un cambio y tus FPS *empeoran*, revierte automáticamente ese cambio en 45 segundos.
- **Modo Seguro**: Si el juego crashea o se vuelve inestable, NOZH se bloquea en "Modo Seguro" para prevenir bucles de error.
- **Auto-Chequeo**: Ejecuta `/nozh selfcheck` para auditar tu instalación, detectar conflictos de mods (ej. Sodium + OptiFabric) y verificar la salud del sistema.

### 📊 HUD Profesional 2.0

- **Métricas Avanzadas**:
  - **P99 Lows**: La verdadera medida de la fluidez (stutter).
  - **Varianza (ms²)**: Qué tan "inestable" se siente tu juego.
  - **Cuello de Botella**: Te dice si estás limitado por CPU o GPU.
- **Gráfico Visual**: Gráfico de tiempos de fotograma en tiempo real con cero impacto en memoria.
- **Menú Rápido**: Presiona `[H]` para abrir una interfaz rápida de ajustes.

### 🌍 Soporte Global de Idiomas

Totalmente traducido a:

- 🇺🇸 Inglés (US)
- 🇪🇸 Español (ES)
- 🇧🇷 Portugués (BR)
- 🇫🇷 Francés (FR)
- 🇩🇪 Alemán (DE)
- 🇮🇹 Italiano (IT)
- 🇯🇵 Japonés (JP)

---

## 🛠️ Instalación

1. **Instala Fabric Loader** para Minecraft 1.20.1.
2. **Instala Sodium** (Requerido). NOZH actúa como un "director de orquesta" inteligente para el motor de renderizado de Sodium.
3. Descarga la última versión `nozh-x.x.x.jar`.
4. Colócalo en tu carpeta `.minecraft/mods`.

**Recomendado:** Úsalo junto con `Indium` y `Lithium` para mejores resultados.

---

## ⚙️ Configuración

Presiona `[K]` o usa ModMenu para abrir el **Panel de Control de NOZH**.

### Pestañas

1. **General**: Interruptor maestro, FPS objetivo y Perfiles Preestablecidos (Patata, Bajo, Medio, Ultra).
2. **Automatización**: Configura qué tan agresiva debe ser la red neuronal (`Presupuesto de Decisión`, `Tamaño de Historial`).
3. **Visuales**: Ajusta qué tiene permitido degradar NOZH (ej. solo partículas, pero mantener distancia de renderizado).
4. **Sistema**: Ver logs, exportar telemetría o Restablecimiento de Fábrica.

### Comandos

- `/nozh status`: Ver confianza actual de la IA y perfil activo.
- `/nozh selfcheck`: Ejecutar diagnóstico del sistema.
- `/nozh profile`: Ejecutar un benchmark de 10 segundos para calibrar el motor.
- `/nozh toggle`: Activar/Desactivar el mod al vuelo.

---

## ❓ Preguntas Frecuentes (FAQ)

**P: ¿Esto aumentará mis FPS máximos?**
R: Tal vez. Pero el objetivo de NOZH es la **consistencia**, no números pico. 60 FPS sin tirones se sienten más fluidos que 400 FPS que caen a 20 cada pocos segundos.

**P: ¿Es compatible con shaders?**
R: ¡Sí! NOZH detecta Iris/Shaders y cambia a un "Modo Conservador" para evitar romper efectos visuales.

**P: ¿Puedo usarlo en un servidor?**
R: Sí. NOZH es estrictamente del lado del cliente. Funciona en cualquier servidor (Vanilla, Spigot, Modded) sin necesidad de estar instalado en el servidor.

---

## 📝 Licencia

Este proyecto está bajo la Licencia MIT.
NOZH es de código abierto y gratuito para siempre.
