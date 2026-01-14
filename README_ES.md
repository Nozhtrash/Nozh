# NOZH: Motor Inteligente de Ritmo de Fotogramas

> **[READ IN ENGLISH / LEER EN INGLÉS](README.md)**

**NOZH** es un mod de optimización del lado del cliente para Minecraft Fabric 1.20.1. Está diseñado para estabilizar los tiempos de fotograma ajustando dinámicamente la configuración de renderizado basándose en telemetría de rendimiento en tiempo real.

A diferencia de los mods de optimización generales que buscan "FPS máximos" (a menudo a costa de la estabilidad), NOZH prioriza la **consistencia** (tiempos de fotograma P99). Logra esto reduciendo selectivamente la fidelidad gráfica durante escenarios de alta carga y restaurándola cuando la carga disminuye.

## 🛠 Cómo Funciona (Profundidad Técnica)

NOZH no hace magia. Gestiona un intercambio: **Fidelidad Visual vs. Latencia de Entrada**.

### 1. El Predictor Perceptrón

NOZH utiliza una red neuronal de una sola capa (Perceptrón) para pronosticar la probabilidad de un pico de lag en el *siguiente* fotograma.

- **Entradas:** Densidad de Entidades (normalizada), Conteo de Partículas, Actualizaciones de Chunks, Velocidad del Jugador.
- **Salida:** Una probabilidad estricta (0.0 a 1.0) de que el siguiente fotograma exceda el tiempo objetivo (ej., >8.33ms para 120 FPS).
- **Entrenamiento:** El modelo aprende en línea. Si predice un pico y ocurre uno, los pesos se refuerzan. Si predice un pico pero el fotograma es suave, penaliza el peso. Esto le permite adaptarse a *tu* hardware específico con el tiempo (aprox. 30-120 segundos de juego).

### 2. Gobernador Transaccional

Cualquier cambio realizado en la configuración de tu juego (ej., "Configurar Nubes de Sodium a Rápido") se ejecuta como una **Transacción**.

1. **Captura:** Se registra el estado actual de Sodium.
2. **Ejecución:** Se cambia la configuración.
3. **Auditoría:** El sistema monitorea el rendimiento durante los siguientes 40-200 ticks.
4. **Rollback (Reversión):** Si el cambio no mejora estadísticamente los tiempos de fotograma P99 (o los empeora), la transacción se **revierte**, restaurando tu configuración original.

Esto asegura que NOZH no simplemente "apague todo" a ciegas. Solo mantiene los cambios que realmente ayudan a tu situación específica.

### 3. Búfer de Anillo Integrado

Almacenamos los últimos 600 fotogramas de telemetría en un búfer circular de asignación cero. Esto nos permite calcular la Desviación Estándar y la Media en tiempo O(1), proporcionando una visión estadísticamente significativa de la "suavidad" en lugar de reaccionar a picos de ruido individuales.

---

## ⚠️ Expectativas Realistas

**NOZH NO es para ti si:**

- Quieres 2000 FPS para capturas de pantalla.
- Juegas Minecraft vanilla en una PC de gama alta (probablemente no necesites ajuste dinámico).
- Quieres un mod que "simplemente aumente los FPS" sin cambiar los visuales. NOZH *cambiará* los visuales (nubes, partículas) para salvar fotogramas.

**NOZH SÍ es para ti si:**

- Experimentas "micro-stutter" o "tirones" al cargar chunks o pelear con mobs.
- Juegas modpacks pesados donde el conteo de entidades fluctúa enormemente.
- Prefieres unos consistentes 60/120/144 FPS sobre unos fluctuantes 400 FPS.

---

## 🎮 Guía de Uso

### Instalación

1. Instala **Fabric Loader**.
2. Instala **Sodium** (Requerido). NOZH orquesta la configuración de Sodium; sin él, NOZH hace muy poco.
3. Arrastra `nozh-2.0.0.jar` a tu carpeta `mods`.

### Configuración

El mod funciona tal cual (`config/nozh.json`).

- `targetFps`: Configúralo a la tasa de refresco de tu monitor (ej., 60, 144).
- `allowedDegradations`: Lista de características que NOZH tiene permitido tocar. Si *realmente* amas las nubes, elimina `"CLOUDS"` de esta lista, y NOZH nunca las tocará, incluso si tienes lag.

### Comandos

- `/nozh status` - Ver pesos neuronales actuales y estado del Gobernador.
- `/nozh profile` - Ejecutar un benchmark distintivo de 10 segundos.
- `/nozh toggle` - Desactivar/Activar el mod sobre la marcha.

---

## 🤝 Compatibilidad

- **Compatible:** Sodium, Lithium, ImmediatelyFast, FerriteCore, ModernFix.
- **Incompatible:** Cualquier otro mod de "configuración dinámica" (ej., Dynamic FPS, Adrenalin). Usar dos optimizadores dinámicos causará que peleen por la configuración, resultando en parpadeos.

---

## Código Abierto y Transparencia

Este proyecto es de código abierto. No hay telemetría enviada a servidores externos. Todos los datos de aprendizaje (pesos) se almacenan localmente en tu máquina y se eliminan cuando reinicias el juego (a menos que se persistan en futuras actualizaciones).

Creemos en la optimización honesta. NOZH intercambia calidad visual por rendimiento *solo cuando es necesario*, y valida estrictamente ese intercambio.
