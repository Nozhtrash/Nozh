# 🧠 Inteligencia de NOZH: Cómo Funciona la IA

> **Descargo de Honestidad**: NOZH usa "IA Estrecha" (Aprendizaje Automático Estadístico). No es "IA Generativa" como ChatGPT. No "piensa" como un humano. Calcula probabilidades basadas en patrones.

## Arquitectura Técnica

El núcleo de la inteligencia de NOZH es la clase `PerformancePredictor.java`. Implementa un **Perceptrón de Capa Única**.

### 1. Entradas (Los Sentidos)

La IA recibe un vector de valores normalizados (-1.0 a 1.0) cada segundo:

* `x1`: **Tendencia de Entidades** (¿Está aumentando el número de mobs rápidamente?)
* `x2`: **Presión de Carga de Chunks** (¿Estamos generando terreno nuevo?)
* `x3`: **Varianza de Tiempo de Fotograma** (¿Son inestables los FPS?)
* `x4`: **Velocidad del Jugador** (¿Nos movemos rápido?)

### 2. Procesamiento (Los Pesos)

Cada entrada corresponde a un "Peso" (`w1` a `w4`).

* Ejemplo: Si la `Tendencia de Entidades` históricamente causa lag en tu PC, `w1` será alto (ej. 0.8).
* Ejemplo: Si la `Velocidad del Jugador` nunca causa lag (tienes un SSD rápido), `w4` será bajo (ej. 0.1).

El Perceptrón calcula la **Probabilidad de Lag**:
`P = Activación( (x1*w1) + (x2*w2) + (x3*w3) + (x4*w4) )`

### 3. Entrenamiento (El Aprendizaje)

Esto es "Aprendizaje No Supervisado en Línea".

1. **Selección**: NOZH predice lag (`P > 0.7`).
2. **Acción**: Toma una acción (ej. reduce partículas).
3. **Retroalimentación**: Espera 5 segundos.
    * Si los FPS mejoraron: **Recompensa** (Fortalece los pesos que dispararon la acción).
    * Si los FPS siguieron mal: **Castigo** (Debilita los pesos; fue un falso positivo).

## Respaldo Heurístico

Si el Gobernador Neuronal no está seguro (`0.3 < P < 0.7`), recurre a **Heurísticas** (Reglas Codificadas).

* *Regla*: "Si FPS < 20 por 3 segundos -> MODO PATATA DE EMERGENCIA".
* *Regla*: "Si TPS del Servidor < 10 -> Ignorar FPS del Cliente (Es problema del servidor)".

## Limitaciones

* **Calentamiento**: La IA comienza con pesos genéricos. Toma unos 10-20 minutos de juego "aprender" los cuellos de botella específicos de tu hardware.
* **Solo Local**: Los datos de entrenamiento se guardan en `brain/weights.json` en tu PC. Nunca se suben a internet.
