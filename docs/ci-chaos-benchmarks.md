# CI: Chaos Tests y Benchmarks

Este pipeline de CI incluye un job dedicado para ejecutar la suite de chaos tests y publicar sus métricas en artefactos y en el resumen del job.

## Pipeline en GitHub Actions

El workflow `.github/workflows/build.yml` define el job `chaos-tests` con los siguientes pasos:

- Ejecuta `./gradlew chaosTest -PoutputDir=build/reports/chaos` para correr la suite.
- Genera un resumen en GitHub Actions con un cuadro de métricas (P95/P99, duración total, pasadas/fallidas).
- Publica los reportes como artifacts (`chaos-reports`).

## Salidas esperadas

Los reportes se guardan en:

- `build/reports/chaos/chaos-test-report.json`
- `build/reports/chaos/chaos-test-report.csv`

El JSON incluye `summary` (duración total, P95, P99, pasadas/fallidas) y el detalle por escenario. El CSV contiene cada escenario con su duración y estado.

## Métricas esperadas (baseline)

Estas métricas sirven como referencia inicial para revisar regresiones. Ajustar según el hardware de CI y la evolución del suite:

- **Duración total**: ≤ 10,000 ms.
- **P95** (duración por escenario): ≤ 1,500 ms.
- **P99** (duración por escenario): ≤ 2,500 ms.
- **Duración máxima por escenario**: ≤ 3,000 ms.
- **Fallos**: 0 (la suite debe terminar sin fallos).

Si se exceden estas métricas, revisar la ejecución del job y comparar contra el reporte JSON/CSV para identificar el escenario afectado.

## Comportamiento en CI

El job `chaos-tests` ejecuta la suite con `-PfailOnChaosError=false` para asegurarse de publicar los artifacts incluso si alguna prueba falla. Los fallos quedan registrados en los reportes y en el summary del job para revisión.
