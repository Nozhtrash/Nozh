# Protocolo rápido de test para modpacks

Este protocolo está pensado para ejecutar una validación rápida (10-15 min)
en modpacks, con foco en escenarios comunes donde el rendimiento suele
degradarse. Úsalo como checklist para capturar señales rápidas de regresión.

## Preparación (1-2 min)
- Reinicia el cliente para arrancar una sesión limpia.
- Asegúrate de que NOZH esté habilitado y con HUD visible.
- Si existe un preset de config para el modpack, cárgalo antes de iniciar.

## Escenarios típicos (8-10 min)

### 1) Mobs (combate / entidades)
1. Dirígete a una zona con alta densidad de entidades (spawner o granja).
2. Observa frametime y spikes durante 2-3 minutos.
3. Anota si hay fluctuaciones fuertes al abrir inventario o atacar.

### 2) Minería (subterráneo / partículas)
1. Mina en una cueva con agua, lava y partículas activas.
2. Verifica que el frametime se mantenga estable al generar nuevos chunks.
3. Confirma que los cambios sugeridos no degraden la jugabilidad.

### 3) Nether (carga y efectos)
1. Entra al nether y recorre biomas con efectos de partículas.
2. Observa spikes en teleports o cambios de bioma.
3. Confirma que la visión no se degrade de forma inesperada.

### 4) Lluvia (clima / shaders)
1. Fuerza lluvia si el modpack lo permite (o espera a clima natural).
2. Valida que no aumente el p95 frametime de forma sostenida.
3. Comprueba si hay necesidad de rollback automático o sugerencias.

## Cierre (1-2 min)
- Revisa el historial de acciones para validar decisiones y rollback.
- Si se detecta regressión, exporta telemetría y adjunta el reporte.

## Señales rápidas de regresión
- p95 frametime sube > 2 ms vs baseline en 2+ escenarios.
- Spike count aumenta de forma sostenida al cambiar de dimensión.
- Rollback no recupera calidad visual cuando el rendimiento mejora.
