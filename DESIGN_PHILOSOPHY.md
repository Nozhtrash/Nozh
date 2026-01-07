# NOZH — CIERRE TOTAL DE DISEÑO (ULTRA DEFINITIVO)

Este documento define el "Contrato de Estabilidad" de NOZH. Cualquier desarrollo futuro debe adherirse a estos principios. NO se permiten cambios que violen estas reglas.

## 1. DEFINICIÓN EXACTA

**NOZH optimiza:**

* Frametime P95 (consistencia).
* Reducción de spikes.
* Estabilidad bajo carga real.

**NOZH NO optimiza:**

* FPS promedio como métrica principal.
* Benchmarks sintéticos.
* FPS en menús o pausas.

## 2. MÉTRICAS

**Válidas:** Avg Frametime (suave), P95 Frametime (real), Spike Count.
**Inválidas:** FPS instantáneo, FPS máximo.

## 3. EDGE CASES (YA CUBIERTOS)

NOZH **NO actúa** (Governor = WAIT / Bound = UNKNOWN) si:

* Juego en pausa o menú.
* Cambio de dimensión o mundo.
* Carga inicial de shaders.
* Alt-Tab o minimizado.

## 4. GOVERNOR

* **Stateless**: No tiene memoria, no "aprende".
* **Determinista**: Misma entrada = Misma salida.
* **Input**: Solo `NozhState` (History), `Config` y `PerfSnapshot`.

## 5. EXECUTOR (SEGURIDAD)

* **Prohibido**: Tocar shaders, configs de otros mods, archivos del usuario.
* **Atomicidad**: Una acción por ciclo.
* **Auditabilidad**: Todo queda en `executionHistory`.

## 6. ROLLBACK

Rollback es **protección**, no castigo.
Se activa si:

* P95 empeora.
* Datos son ruidosos/inconclusos.
* Usuario cambia settings manualmente durante la ventana.

## 7. SAFE MODE

**"The Bouncer"**.

* Si se activa, **bloquea** el Executor.
* Se activa por: Crash Loop, Config Force, Comando.
* Solo se desactiva explícitamente (`/nozh safemode reset`).

## 8. COMPATIBILIDAD

NOZH actúa como **coordinador pasivo**.

* Si detecta un mod (ej. Sodium), lo reporta.
* Si no lo detecta, lo ignora y sigue.
* **Nunca** sobreescribe la configuración de otro mod.

## 9. CONFIGURACIÓN

* **Clamping Estricto**: Valores fuera de rango se corrigen automáticamente.
* **No Crash**: Config corrupta -> Defaults + Warning.

## 10. UX

* `/nozh status`: Estado real y honesto.
* `/nozh selfcheck`: Diagnóstico completo para bug reports.

## 11. RENDIMIENTO DEL MOD

* **Zero-Garbage**: Hot paths (Render) no generan basura (allocations).
* **Lazy Logging**: Strings solo se construyen si debug está activo.
* **Smart I/O**: `state.json` solo se escribe si cambió (Hash Check).

## 12. CRITERIO FINAL

NOZH es exitoso si el juego se siente más fluido (menos spikes).
Si NOZH introduce stutter, el diseño ha fallado.

---
**ESTADO DEL PROYECTO: BASE GOLDEN MASTER (v0.1.0) + v0.2.0-alpha en desarrollo**

* Base v0.1.0 completa y congelada.
* Integraciones de v0.2.0-alpha en progreso (preparación beta).
* Extensible (v0.2.0+) sin reescribir la base.
