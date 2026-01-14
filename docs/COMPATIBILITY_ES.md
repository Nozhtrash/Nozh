# 🤝 Registro de Compatibilidad de Mods (Mayordomía)

NOZH conoce los siguientes mods. Ajustará automáticamente su comportamiento para prevenir conflictos si son detectados.

## Mods de Renderizado (Alta Prioridad)

| Mod | Acción de NOZH |
|:---|:---|
| **Sodium / Embeddium** | Desactiva Optimizaciones de Chunks de NOZH. Delega el manejo de Distancia de Renderizado. |
| **Iris / Oculus** | Desactiva optimizaciones de Nubes y Sombras (Los shaders se comportan raro con nubes desactivadas). |
| **Canvas** | Delegación completa. |
| **Nvidium** | Detectado. NOZH respeta el frustum culling de Nvidium. |
| **VulkanMod** | Detectado. Los ganchos de renderizado críticos se desactivan. |

## Optimizadores de Motor

| Mod | Acción de NOZH |
|:---|:---|
| **C2ME (Concurrent Chunk Management Engine)** | NOZH relaja las reglas de prioridad de chunks para permitir que C2ME maneje los hilos. |
| **Lithium** | Completamente Compatible. |
| **Phosphor / Starlight** | Completamente Compatible. |
| **FerriteCore** | Completamente Compatible. |
| **ModernFix** | Completamente Compatible. |
| **Krypton** | Completamente Compatible. |
| **ImmediatelyFast** | Detectado. El culling de animaciones es coordinado. |

## Mods de Utilidad

| Mod | Acción de NOZH |
|:---|:---|
| **ModMenu** | NOZH inyecta su botón de configuración en la lista de mods. |
| **Cloth Config** | No usado por NOZH (usamos nuestro propio motor), pero compatible si está instalado. |
| **Dynamic FPS** | NOZH lo detecta y no peleará por los límites de FPS cuando la ventana no tiene foco. |

## Incompatibilidades Conocidas

* NINGUNA conocida actualmente para v2.0.0.

> **Nota**: Esta lista se actualiza en vivo vía el sistema de Cloud Config (`compatibility.json`) al iniciar el juego.
