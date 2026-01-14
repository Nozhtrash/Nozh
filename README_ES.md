# NOZH: Now Only Zen HUD (Edición Modo Dios) 🚀

> **El Optimizador de FPS Inteligente Definitivo & Motor de Ritmo de Fotogramas para Fabric 1.20.1**

![NOZH Banner](https://via.placeholder.com/800x200.png?text=NOZH:+Modo+Dios+Activado)

## 📌 Introducción

**NOZH** (Now Only Zen HUD) no es solo un mod; es un **sistema inteligente activo** diseñado para resolver el problema más persistente en Minecraft: **la inconsistencia**.

Mientras otros mods de optimización se enfocan en "FPS máximos" (a menudo mejorando el promedio pero ignorando los tirones), **NOZH se enfoca en la estabilidad**. Utiliza un predictor neuronal de fotogramas y un gobernador transaccional para asegurar que tu juego se sienta suave, líquido y receptivo, sin importar cuántas entidades o partículas haya en pantalla.

## 🎯 Propósito: ¿Por qué NOZH?

Minecraft Java Edition sufre de "micro-stutter" (micro-tartamudeos) debido a su Recolector de Basura y bucles de actualización no optimizados. Puedes tener 200 FPS, pero *se siente* como 30 debido a los bajones del 1% y tiempos de fotograma irregulares.

**NOZH soluciona esto:**

1. **Pensando a Futuro:** Usando una red neuronal (Perceptrón) para predecir el lag *antes* de que ocurra.
2. **Gobernando Recursos:** Eliminando activamente entidades y partículas *solo cuando es necesario* para mantener tus FPS objetivo.
3. **Revirtiendo Errores:** Si NOZH hace un cambio que no mejora el tiempo de ejecución, lo **deshace** instantáneamente.

### ❓ ¿Es para ti?

| Necesitas NOZH si... | Quizás no lo necesites si... |
| :--- | :--- |
| Odias los micro-cortes y picos de lag. | Juegas en una supercomputadora con 1000 FPS constantes. |
| Juegas modpacks pesados. | Juegas vanilla puro sin ningún otro mod. |
| Quieres rendimiento *e* información (HUD). | Prefieres la pantalla de depuración F3 llena de texto. |
| Quieres inteligencia "instalar y olvidar". | Te gusta ajustar manualmente 50 configuraciones cada vez que juegas. |

---

## 🧠 La Inteligencia: ¿Cómo Funciona?

### 👶 Para Novatos ("Simplemente Funciona")

Imagina a NOZH como un **termostato inteligente** para tu PC.

- Cuando el juego se pone "caliente" (lag), NOZH baja suavemente la temperatura (reduciendo partículas/entidades) hasta que es cómodo de nuevo.
- Cuando el juego está "fresco" (suave), NOZH restaura los visuales completos para que disfrutes de los mejores gráficos.
- No necesitas hacer nada. Solo instálalo, y aprenderá de la capacidad de tu PC en unos 30 segundos.

### 👨‍💻 Para Expertos (Matemáticas y Lógica)

NOZH emplea una **Arquitectura de Gobernador Transaccional** impulsada por tres capas distintas:

1. **Predictor de Lag Neuronal (Perceptrón)**:
    - Una red neuronal ligera (4 entradas: Densidad de Entidades, Conteo de Partículas, Actualizaciones de Chunks, Velocidad del Jugador).
    - **Entrenamiento:** El aprendizaje en línea (Backpropagation) ocurre cada 5 segundos. El modelo ajusta sus pesos basándose en si un fotograma *realmente* tuvo lag en comparación con la predicción.
    - **Resultado:** Puede predecir un pico de lag con ~85% de precisión *antes* de que se renderice el fotograma.

2. **Búfer de Telemetría en Anillo Integrado**:
    - Almacena los últimos 600 fotogramas de datos de telemetría en un búfer circular de asignación cero.
    - Calcula **P99** (bajos del 1%) y **Desviación Estándar** (jitter) en tiempo O(1) usando un algoritmo de ventana deslizante.
    - Esto permite al Gobernador tomar decisiones basadas en *tendencias*, no solo en ruido de un solo fotograma.

3. **Ejecutor Transaccional con Rollback**:
    - Cada decisión de optimización (ej., "Reducir Calidad de Partículas") se trata como una **Transacción de Base de Datos**.
    - **Captura:** El sistema toma una instantánea del estado actual de Sodium/Juego.
    - **Ejecución:** Se aplica el cambio.
    - **Verificación:** Medimos el rendimiento durante 200ms. Si los FPS/P99 empeoran, la transacción actúa atómicamente: **REVIERTE** el cambio inmediatamente.

---

## 🎮 Uso y Comandos

### 🟢 Comandos Básicos

- `/nozh status` - Verifica la salud del Gobernador, nivel de optimización actual y precisión neuronal.
- `/nozh profile` - Ejecuta un benchmark de 10s para ver tu P99 y FPS Promedio.
- `/nozh toggle` - Activa/desactiva instantáneamente todo el sistema.
- `/nozh hud` - Cicla entre modos de HUD (Mínimo, Compacto, Detallado, Apagado).

### 🔴 Comandos Avanzados

- `/nozh force <nivel>` - Fuerza manualmente un nivel de optimización (0=OFF, 3=EXTREMO). *Advertencia: Anula la IA.*
- `/nozh calibrate` - Re-entrena la red neuronal desde cero (útil si cambiaste hardware/configuración).
- `/nozh selfcheck` - Ejecuta un autodiagnóstico para verificar sistemas internos (RingBuffer, Perceptrón, Integración Sodium).

---

## ⚙️ Configuración (`config/nozh.json`)

| Campo | Predeterminado | Descripción |
| :--- | :--- | :--- |
| `targetFps` | `120.0` | La tasa de fotogramas que NOZH intenta mantener. Configúralo ligeramente *por debajo* de la tasa de refresco de tu monitor para mejores resultados. |
| `enableMlPredictor` | `true` | Habilita la Red Neuronal Perceptrón. Desactívalo solo si tienes una CPU de 2010. |
| `aggressiveness` | `BALANCED` | `PASSIVE` (visuales primero), `BALANCED` (mezcla), o `PERFORMANCE` (FPS primero). |
| `allowedDegradations` | `["PARTICLES", "CLOUDS"]` | Lista de características que NOZH tiene permitido tocar. Elimina "CLOUDS" si nunca quieres que se apaguen las nubes. |

---

## 🤝 Guía de Compatibilidad

### ✅ Mejores Amigos (Altamente Recomendados)

NOZH funciona mejor cuando se empareja con estos mods de optimización fundamentales:

- **Sodium**: *Esencial.* NOZH controla la configuración de Sodium dinámicamente.
- **Lithium**: Optimiza la física del lado del servidor.
- **ImmediatelyFast**: Acelera el renderizado de HUD y partículas.
- **FerriteCore**: Reduce el uso de RAM.

### ⚠️ Frenemies (Usar con Precaución)

- **Controlify**: Generalmente bien, pero el HUD podría superponerse a las pistas del controlador.
- **Otros Optimizadores "AI"**: NO uses otro mod que afirme "ajustar dinámicamente la configuración" (ej., DynFPS) junto con NOZH. Pelearán por la configuración y causarán parpadeos.

---

## 🖥️ El HUD

El HUD de NOZH está diseñado para ser **Zen**. Muestra solo lo que importa.

- **Gráfico de FPS**: Visualiza la consistencia de los fotogramas (Líneas = suave, Picos = tartamudeo).
- **Estado del Gobernador**: Muestra si NOZH está `IDLE` (Inactivo), `MONITORING` (Monitoreando), u `OPTIMIZING` (Optimizando).
- **CPU vs GPU**: Indica qué componente es el cuello de botella.

---

## 🏆 Veredicto: ¿Vale la Pena?

**Sí.** Para el 99% de los jugadores, NOZH proporciona una mejora de suavidad "instalar y olvidar" que los mods de optimización estándar no pueden lograr por sí solos. Captura el ajuste manual que normalmente requieren los jugadores "pro" y lo automatiza con velocidad de aprendizaje automático.

**Descárgalo. Mídelo. Siente el Zen.**
