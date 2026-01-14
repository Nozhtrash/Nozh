# ✨ Guía de Características de NOZH

Este documento define cada característica en NOZH v2.0, explicando **qué hace**, **cómo te ayuda**, y la **lógica técnica** detrás de ella.

> **Nota de Transparencia**: NOZH está diseñado para ser honesto. Nunca falseamos números de FPS. Nunca borramos archivos sin preguntar. Todo es transparente.

---

## 1. Capa Visual (La Interfaz)

### 🖥️ Panel Premium

**Para Novatos**: Un menú hermoso donde puedes cambiar configuraciones.
**Para Expertos**: Una GUI personalizada construida sobre `DrawContext` con renderizado de cero asignaciones. Evita el peso innecesario de Cloth Config para una máxima capacidad de respuesta.

- **Cómo acceder**: Presiona el botón "NOZH" en ModMenu o escribe `/nozh gui`.
- **Característica Clave**: Tooltips en tiempo real para cada botón explicando exactamente qué hace.

### 📈 Gráfico de Telemetría en Vivo

**Para Novatos**: Un gráfico de líneas en la parte superior del menú que muestra si tu juego tiene lag (Rojo) o va fluido (Verde).
**Para Expertos**: Un gráfico renderizado con `GL_LINE_STRIP` que visualiza los últimos 60 segundos del historial de `FrameTime`.

- **Verde**: <16ms (60+ FPS)
- **Amarillo**: <33ms (30+ FPS)
- **Rojo**: >33ms (<30 FPS)

### 🧙‍♂️ Lógica de Primera Ejecución

**Para Novatos**: NOZH detecta automáticamente tu hardware (RAM, GPU) y establece el mejor perfil automáticamente.
**Para Expertos**: En el primer arranque, el `PotatoModeEngine` perfila la JVM (`Runtime.maxMemory`) y el vendedor de la GPU. Si detecta <4GB RAM o Gráficos Integrados Intel, pre-configura el `PotatoConfig` en modo de culling agresivo.

### 🛠️ Gestión del Sistema (Nuevo en v2.0)

**Para Novatos**: Herramientas para arreglar tu configuración si la rompes.
**Para Expertos**:

- **Restablecimiento de Fábrica**: Borra `config/nozh.json` y reinicializa los valores predeterminados seguros.
- **Recarga en Caliente**: Recarga la configuración desde el disco sin reiniciar Minecraft (útil para editar JSON manualmente).
- **Exportar al Portapapeles**: Serializa el estado actual de la configuración a una cadena JSON para soporte/depuración eficiente.

---

## 2. Inteligencia Artificial (El Cerebro)

### 🧠 Predictor Neuronal de Lag

**Para Novatos**: NOZH adivina cuándo vas a tener lag y lo arregla antes de que te des cuenta.
**Para Expertos**: Una red neuronal de Perceptrón de Capa Única (SLP).

- **Entradas**: Tasa de Carga de Chunks, Delta de Conteo de Entidades, Tasa de Asignación de Memoria.
- **Salida**: Probabilidad de Lag (0.0 - 1.0).
- **Entrenamiento**: Usa "Aprendizaje en Línea" (No Supervisado). Si predice lag y el lag ocurre, fortalece los pesos sinápticos.

### 🔀 Motor de Decisión Híbrido

**Para Novatos**: Elige qué tan "inteligente" quieres que sea NOZH.
**Para Expertos**: Un patrón de estrategia configurable en el `Gobernador`.

- **NEURONAL**: Predicción pura por IA (alta precisión, requiere tiempo de calentamiento).
- **HEURÍSTICA**: Lógica basada en reglas (instantáneo, predecible).
- **HÍBRIDO**: Usa Heurística para amenazas inmediatas e IA para predicción de tendencias (Lo mejor de ambos mundos).

### 🎭 Detector de Anomalías

**Para Novatos**: Distingue entre "Mi PC es lenta" y "El Servidor tiene lag".
**Para Expertos**: Compara el Tiempo de Fotograma del Cliente vs Ping/TPS del Servidor vía telemetría de `CrashSafeGuard`.

- Si **FPS es bajo** pero **Ping es bajo**: Problema de GPU/CPU -> **Optimizar Gráficos**.
- Si **FPS es alto** pero **Ping es alto**: Problema de Red -> **No Hacer Nada** (Los ajustes gráficos no ayudarán al lag).

---

## 3. Motores de Optimización (El Músculo)

### 🥔 Modo Patata Extremo

**Para Novatos**: El "Interruptor de Emergencia" para laptops muy viejas. Hace que Minecraft se vea mal pero corra rápido.
**Para Expertos**: Un perfil rígido que anula las preferencias del usuario.

- **Distancia de Renderizado**: Bloqueada a 2 chunks.
- **Distancia de Simulación**: Bloqueada a 2 chunks.
- **Mipmaps**: 0 (Desactivado).
- **Mezcla de Biomas**: 0 (Desactivado).
- **Partículas**: Mínimo.
- **Lógica**: Omite el "Chequeo de Seguridad" para forzar el rendimiento a toda costa.

### 🛡️ Compatibilidad Inteligente de Mods

**Para Novatos**: NOZH sabe si estás jugando con otros mods y se reinicia para no romperlos.
**Para Expertos**: Un `RemoteConfigFetcher` descarga un archivo JSON de la nube al iniciar.

- **Base de Conocimiento**: Contiene metadatos para mods populares (Create, Sodium, Iris).
- **Resolución de Conflictos**: Si se detecta `Iris`, NOZH desactiva sus propias optimizaciones de Shaders/Nubes para evitar errores de renderizado.

---

## 4. Sistemas de Seguridad (El Guardián)

### ↩️ Auto-Rollback

**Para Novatos**: Si NOZH cambia una configuración y tu juego se vuelve MÁS LENTO, deshace el cambio automáticamente.
**Para Expertos**:

1. Mide `AvgFrameTime` (Línea Base).
2. Aplica Acción (ej., `DISMINUIR_DISTANCIA_RENDERIZADO`).
3. Espera 45 segundos (Ventana).
4. Mide `AvgFrameTime` (Nuevo).
5. Si `Nuevo > Línea Base + Umbral`, llama a `Executor.revert()`.

### 🚨 Guardia de Seguridad de Crasheos

**Para Novatos**: Si el juego crashea 3 veces seguidas, NOZH se desactiva para que al menos puedas abrir el juego.
**Para Expertos**:

- Usa un marcador de archivo `crash_guard` en la carpeta config.
- Incrementa un contador en cada arranque.
- Borra el contador después de 5 minutos de estabilidad.
- Si contador >= 3, activa **MODO SEGURO** (Módulos desactivados, Listeners desregistrados).

---

## 🛠️ Resumen de "Orquestación"

NOZH no solo "ajusta configuraciones". **Orquesta** todo tu juego.

1. **Observa** (Telemetría).
2. **Piensa** (IA/Gobernador).
3. **Actúa** (Ejecutor).
4. **Aprende** (Perceptrón).

Es un participante activo en tu bucle de juego, más simple que un humano pero más rápido que uno.
