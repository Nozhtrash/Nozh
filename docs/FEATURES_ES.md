# 🔍 Guía Profunda de Características de NOZH v2.0

Este documento explica exactamente cómo funciona NOZH bajo el capó. Sin marketing—solo mecánica.

## 1. El Gobernador Neuronal ("Motor de Optimización")

NOZH usa un motor de decisiones para gestionar la configuración del juego dinámicamente.

### Cómo funciona

Ejecuta un bucle cada 20 ticks (1 segundo) o cuando ocurren eventos específicos (como entrar en combate).

1. **Entrada**: Recopila datos:
    * **Delta de FPS**: ¿Están cayendo los cuadros?
    * **Densidad de Entidades**: ¿Cuántos mobs hay cerca?
    * **Actualizaciones de Chunks**: ¿El mundo carga rápido?
    * **Velocidad del Jugador**: ¿Estás volando con Elytras?
2. **Proceso**: Alimenta estos datos al `Algoritmo` seleccionado (Neuronal o Heurístico).
3. **Salida**: Calcula un `Puntaje de Presión` (0.0 a 1.0).
4. **Acción**:
    * Presión < 0.2: No hacer nada (Relajado).
    * Presión > 0.5: Activar Acciones de `Nivel 1` (ej. Reducir Distancia de Partículas).
    * Presión > 0.8: Activar Acciones de `Nivel 2` (ej. Desactivar Nubes, Reducir Distancia de Renderizado).

### Algoritmos

* **Heurístico (Por Defecto)**: Usa reglas estáticas (Si FPS < 30, haz X). Rápido y predecible.
* **Neuronal**: Usa un `Perceptrón` que ajusta pesos basado en el éxito. Si desactivar nubes arregló el lag la última vez, lo hará antes la próxima vez.
* **Híbrido**: Usa Heurística para emergencias (Modo Pánico) y Neuronal para ajustes de fondo.

---

## 2. Niveles del Modo Patata

El Modo Patata es una anulación forzada para hardware de gama baja. Ignora las preferencias del usuario para asegurar que el juego sea jugable.

| Nivel | Disparador RAM | Disparador Núcleos | Ajustes Aplicados |
|:---|:---|:---|:---|
| **NIVEL 1 (Leve)** | < 8GB | < 6 Núcleos | RD: 12, ED: 8, Partículas: 75% |
| **NIVEL 2 (Mod)** | < 4GB | < 4 Núcleos | RD: 8, ED: 6, Partículas: 50%, Nubes: OFF |
| **NIVEL 3 (Aggr)** | -- | -- | RD: 6, ED: 4, Partículas: 25%, Animaciones: OFF |
| **NIVEL 4 (Ext)** | < 2GB | < 2 Núcleos | RD: 4, ED: 3, Partículas: 10%, Todo FX: OFF |
| **EXTREMO** | -- | -- | RD: 2, ED: 2, Partículas: 0%, **HUD Mínimo** |

* **RD**: Distancia de Renderizado (Chunks)
* **ED**: Distancia de Entidades (Chunks)

---

## 3. Herramientas del Sistema y Gestión de Config

Ubicadas en la pestaña **Sistema** de la GUI (`/nozh gui`).

### Restablecimiento de Fábrica

* **Qué hace**: Borra `config/nozh.json` y reinicializa inmediatamente `NozhConfig` con valores predeterminados de Java.
* **Cuándo usarlo**: Si arruinaste la configuración tanto que el juego crea glitches visuales (ej. entidades invisibles).

### Backup de Config

* **Qué hace**: Guarda una copia de tus ajustes actuales.
* **Exportar al Portapapeles**: Copia la cadena JSON completa a tu portapapeles. Útil para pegar en Discord para soporte.

### Recarga en Caliente

* **Qué hace**: Relee el archivo desde el disco.
* **Por qué**: Si editas el archivo `.json` manualmente con el Bloc de Notas, haz clic aquí para aplicar cambios sin reiniciar el juego.

---

## 4. Compatibilidad de Mods (Mayordomía)

NOZH sigue un modelo de "Mayordomía" (Stewardship). Reconoce que algunos mods (como Sodium) son dueños de ciertas partes del juego (Renderizado).

* **Modo Exclusivo**: Si Sodium está instalado, NOZH **desactiva completamente** sus propias optimizaciones de Renderizado de Chunks. Cede el control a Sodium.
* **Modo Cooperativo**: Si ModMenu está instalado, NOZH integra su botón en el menú.
* **Evasión de Conflictos**: Si C2ME está instalado, NOZH desactiva el threading agresivo de chunks para prevenir la inanición de hilos.

Mantenemos una lista de 50+ mods en nuestro `CompatRegistry` interno.

---

## 5. Guardia de Seguridad (Crash Guard)

Este sistema te protege de bucles de arranque (boot loops).

1. Al iniciar, NOZH escribe un archivo `boot_marker`.
2. Si el juego crashea antes de llegar al Menú Principal, el marcador permanece "sucio".
3. Si se detectan 3 arranques "sucios" seguidos, NOZH entra en **MODO SEGURO**.
    * **MODO SEGURO**: Toda la lógica compleja (IA Neuronal, Motor Patata) se desactiva. Solo corre el cargador de config mínimo. Esto te permite abrir el juego y arreglar el problema.

---
