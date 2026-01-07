# Observabilidad y reporting (crash-free + compatibilidad)

Este documento define el stack actual de observabilidad, los dashboards requeridos y los jobs automáticos para generar
indicadores de crash-free y compatibilidad.

## 1) Stack actual de observabilidad/reporting

**Fuentes internas (NOZH):**
- **Exportaciones de telemetría**: CSV/JSON generados por `TelemetryExportWriter` mediante
  `/nozh telemetry export csv|json` en `config/nozh/telemetry_exports/`.
- **Telemetría agregada**: reportes Markdown/JSON a partir del `TelemetryReportGenerator` (uso interno y tooling).
- **Estado de crash loop**: `config/nozh/state.json` (boot attempts, safe mode, último contexto de fallo).
- **Compatibilidad**: `CompatMatrix` calcula conflictos y score de compatibilidad en runtime (vía `/nozh selfcheck`).
- **Logs**: `config/nozh/nozh-debug.log` cuando `debugLogs=true`.

**Stack externo (actual):**
- No hay integración nativa con Grafana/Kibana/Sentry. La observabilidad se realiza con exports locales
  y herramientas en `tools/` para convertirlas en reportes o indicadores.

## 2) Dashboards recomendados

### 2.1 Crash-free

**KPI:** `crash_free_percent` (0% o 100% en el estado actual del cliente).

**Fuente de datos:** `config/nozh/state.json`.

**Reglas de cálculo:**
- Crash loop detectado si `safeModeCauses` incluye `CRASH_LOOP` **o** (`bootAttempts >= 3` y `sessionStable == false`).
- `crash_free_percent = 0` si hay crash loop; `100` si no lo hay.

**Campos adicionales:**
- `safe_mode_active`, `safe_mode_causes`, `last_failure_context`.

### 2.2 Compatibilidad

**KPI:** `compat_score` (0–100).

**Fuente de datos:** export de `/nozh selfcheck` (JSON recomendado) o snapshot de compatibilidad generado
por tooling (ver job automático).

**Campos adicionales:**
- `conflict_count`, `loaded_mods`, `recommendations`.

## 3) Jobs automáticos

Los siguientes jobs generan indicadores en intervalos definidos y publican resultados en `reports/`:

### 3.1 Indicadores de crash-free + compatibilidad

Comando:

```bash
python3 tools/observability_indicators.py \
  --state-file config/nozh/state.json \
  --compat-file config/nozh/compat_report.json \
  --output reports/observability_indicators.json
```

Frecuencia sugerida:
- **Cada 15 minutos** para entornos activos (staging/qa/prod).

### 3.2 Reporte de telemetría (performance)

Comando:

```bash
python3 tools/telemetry_report.py \
  --input config/nozh/telemetry_exports \
  --output reports/telemetry_report.md
```

Frecuencia sugerida:
- **Cada 60 minutos** o al final de una sesión de benchmark.

### 3.3 Ejemplo de cron

```
*/15 * * * * /usr/bin/python3 /path/to/repo/tools/observability_indicators.py --state-file /path/to/config/nozh/state.json --compat-file /path/to/config/nozh/compat_report.json --output /path/to/repo/reports/observability_indicators.json
0 * * * * /usr/bin/python3 /path/to/repo/tools/telemetry_report.py --input /path/to/config/nozh/telemetry_exports --output /path/to/repo/reports/telemetry_report.md
```

## 4) Acceso, fuente de datos y frecuencia

| Indicador | Fuente | Acceso | Frecuencia |
| --- | --- | --- | --- |
| crash_free_percent | `config/nozh/state.json` | Archivo local en cliente/instancia | 15 min |
| safe_mode_* | `config/nozh/state.json` | Archivo local en cliente/instancia | 15 min |
| compat_score | `/nozh selfcheck` ➜ `compat_report.json` | Archivo local exportado | 15 min |
| conflict_count | `/nozh selfcheck` ➜ `compat_report.json` | Archivo local exportado | 15 min |
| telemetry_report | `config/nozh/telemetry_exports/` | Export CSV/JSON | 60 min |

> Nota: si el entorno no puede exportar `compat_report.json`, el job de indicadores reportará `compat_score=null`.
