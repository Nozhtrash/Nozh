# ⚙️ Guía de Configuración de NOZH

Este documento explica cada opción que se encuentra en `config/nozh.json` o en la GUI del juego.

## 1. Ajustes Generales

| Opción | Por Defecto | Descripción |
|:---|:---|:---|
| `enabled` | `true` | Interruptor maestro. Si es `false`, NOZH no hace absolutamente nada. |
| `governorMode` | `HEURISTIC` | El cerebro del mod. Opciones: `NEURAL` (IA), `HEURISTIC` (Reglas), `HYBRID` (Híbrido). |
| `potatoMode` | `false` | Si es `true`, anula casi todos los ajustes visuales para subir FPS. |

## 2. Umbrales (Cuándo Actuar)

| Opción | Por Defecto | Descripción |
|:---|:---|:---|
| `minAcceptableFps` | `60` | Si los FPS caen por debajo de esto, NOZH comienza a optimizar. |
| `maxAcceptablePing` | `100` | Usado por el Detector de Anomalías. Si Ping > 100, NOZH asume que el lag es de red, no de GPU. |
| `hysteresisTicks` | `100` | (5 segundos). Cuánto esperar antes de cambiar estados (previene parpadeo). |

## 3. Reglas Heurísticas (Ajuste Manual)

Estos ajustes controlan qué apaga NOZH cuando ocurre lag.

| Opción | Efecto | Impacto |
|:---|:---|:---|
| `decreaseRenderDistance` | Reduce la distancia de chunks dinámicamente (ej. 12 -> 8). | Alto |
| `cullParticles` | Reduce la distancia de renderizado de partículas. | Medio |
| `disableClouds` | Apaga el renderizado de nubes. | Bajo |
| `disableAnimations` | Detiene animaciones de texturas (agua, fuego). | Bajo |
| `cullEntities` | Oculta entidades que están lejos. | Alto |

## 4. Mayordomía (Compatibilidad)

| Opción | Por Defecto | Descripción |
|:---|:---|:---|
| `respectExternalMods` | `true` | Si es `true`, NOZH desactivará sus propias características si encuentra un mod mejor (ej. Sodium) manejándolas. |
| `enableCloudConfig` | `true` | Permite a NOZH descargar `compatibility.json` desde GitHub al iniciar. |

## 5. Depuración

| Opción | Por Defecto | Descripción |
|:---|:---|:---|
| `debugLogging` | `false` | Llena el archivo de log con datos de decisión. Solo úsalo si reportas un bug. |
| `showHud` | `true` | Alterna la superposición en el juego (HUD). |

> **Pro Tip**: Puedes editar `config/nozh.json` mientras el juego corre y presionar "Recarga en Caliente" en la pestaña Sistema para aplicar cambios al instante.
