#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


@dataclass(frozen=True)
class TelemetryDiagnostics:
    gc_recent_ms: float | None
    gc_pressure: float | None
    pause_count: int | None
    pause_max_ms: float | None
    stutter_cause: str | None
    stutter_confidence: float | None
    hottest_phase: str | None
    hottest_phase_max_ms: float | None


@dataclass(frozen=True)
class TelemetryMetrics:
    source: Path
    avg_ms: float | None
    p95_ms: float | None
    spikes: int | None
    samples: int | None
    window_seconds: float | None
    tick_avg_ms: float | None
    tick_p95_ms: float | None
    tick_samples: int | None
    samples_ms: list[float]
    diagnostics: TelemetryDiagnostics


@dataclass(frozen=True)
class Finding:
    title: str
    impact: str
    evidence: str
    proposal: str
    effort: str
    category: str
    priority: str


def parse_float(value: str) -> float | None:
    if value.strip() in {"--", ""}:
        return None
    try:
        return float(value)
    except ValueError:
        return None


def parse_int(value: str) -> int | None:
    if value.strip() in {"--", ""}:
        return None
    try:
        return int(value)
    except ValueError:
        return None


def parse_csv(path: Path) -> TelemetryMetrics:
    lines = path.read_text(encoding="utf-8").splitlines()
    samples_ms: list[float] = []
    metrics: dict[str, str] = {}

    in_samples = False
    for line in lines:
        if not line:
            in_samples = False
            continue
        if line.startswith("index,frametime_ms"):
            in_samples = True
            continue
        if in_samples:
            parts = line.split(",")
            if len(parts) >= 2:
                try:
                    samples_ms.append(float(parts[1]))
                except ValueError:
                    pass
            continue
        key, _, value = line.partition(",")
        if key:
            metrics[key.strip()] = value.strip()

    diagnostics = TelemetryDiagnostics(
        gc_recent_ms=parse_float(metrics.get("gc_recent_ms", "")),
        gc_pressure=parse_float(metrics.get("gc_pressure", "")),
        pause_count=parse_int(metrics.get("pause_count", "")),
        pause_max_ms=parse_float(metrics.get("pause_max_ms", "")),
        stutter_cause=metrics.get("stutter_cause"),
        stutter_confidence=parse_float(metrics.get("stutter_confidence", "")),
        hottest_phase=metrics.get("hottest_phase"),
        hottest_phase_max_ms=parse_float(metrics.get("hottest_phase_max_ms", "")),
    )

    return TelemetryMetrics(
        source=path,
        avg_ms=parse_float(metrics.get("avg_ms", "")),
        p95_ms=parse_float(metrics.get("p95_ms", "")),
        spikes=parse_int(metrics.get("spikes", "")),
        samples=parse_int(metrics.get("samples", "")),
        window_seconds=parse_float(metrics.get("window_seconds", "")),
        tick_avg_ms=parse_float(metrics.get("tick_avg_ms", "")),
        tick_p95_ms=parse_float(metrics.get("tick_p95_ms", "")),
        tick_samples=parse_int(metrics.get("tick_samples", "")),
        samples_ms=samples_ms,
        diagnostics=diagnostics,
    )


def parse_json(path: Path) -> TelemetryMetrics:
    raw: dict[str, Any] = json.loads(path.read_text(encoding="utf-8"))
    frame = raw.get("frame", {})
    tick = raw.get("tick", {})
    diagnostics_raw = raw.get("diagnostics", {})

    diagnostics = TelemetryDiagnostics(
        gc_recent_ms=diagnostics_raw.get("gcRecentMs"),
        gc_pressure=diagnostics_raw.get("gcPressure"),
        pause_count=diagnostics_raw.get("pauseCount"),
        pause_max_ms=diagnostics_raw.get("pauseMaxMs"),
        stutter_cause=diagnostics_raw.get("stutterCause"),
        stutter_confidence=diagnostics_raw.get("stutterConfidence"),
        hottest_phase=diagnostics_raw.get("hottestPhase"),
        hottest_phase_max_ms=diagnostics_raw.get("hottestPhaseMaxMs"),
    )

    samples_ms = [float(value) for value in raw.get("samplesMs", [])]

    return TelemetryMetrics(
        source=path,
        avg_ms=frame.get("avgFrametimeMs"),
        p95_ms=frame.get("p95FrametimeMs"),
        spikes=frame.get("spikeCount"),
        samples=frame.get("sampleCount"),
        window_seconds=frame.get("windowSeconds"),
        tick_avg_ms=tick.get("avgTickMs"),
        tick_p95_ms=tick.get("p95TickMs"),
        tick_samples=tick.get("sampleCount"),
        samples_ms=samples_ms,
        diagnostics=diagnostics,
    )


def load_exports(export_dir: Path) -> list[TelemetryMetrics]:
    exports: list[TelemetryMetrics] = []
    for path in sorted(export_dir.glob("*.csv")):
        exports.append(parse_csv(path))
    for path in sorted(export_dir.glob("*.json")):
        exports.append(parse_json(path))
    return exports


def format_metric(value: float | int | None, suffix: str = "") -> str:
    if value is None:
        return "N/D"
    if isinstance(value, float):
        return f"{value:.3f}{suffix}" if suffix else f"{value:.3f}"
    return f"{value}{suffix}"


def compute_threshold(p95: float | None, avg: float | None) -> float:
    baseline = p95 if p95 is not None else avg if avg is not None else 16.0
    return max(33.3, baseline * 1.5)


def summarize_peaks(samples: list[float], threshold: float) -> tuple[list[float], int]:
    peaks = sorted([value for value in samples if value >= threshold], reverse=True)
    return peaks[:5], len(peaks)


def build_findings(metrics: TelemetryMetrics) -> list[Finding]:
    findings: list[Finding] = []
    threshold = compute_threshold(metrics.p95_ms, metrics.avg_ms)
    peak_samples, peak_count = summarize_peaks(metrics.samples_ms, threshold)
    peak_summary = ", ".join(f"{value:.3f}ms" for value in peak_samples) or "Sin picos sobre umbral"

    findings.append(
        Finding(
            title="Picos de frametime y stutter",
            impact=(
                f"{peak_count} muestras sobre {threshold:.1f}ms; "
                "stutter perceptible cuando el usuario cruza el umbral de suavidad."
            ),
            evidence=(
                f"Archivo: {metrics.source.name}; avg={format_metric(metrics.avg_ms, 'ms')}, "
                f"p95={format_metric(metrics.p95_ms, 'ms')}, spikes={format_metric(metrics.spikes)}; "
                f"picos: {peak_summary}."
            ),
            proposal=(
                "Ajustar buffers/ventanas de observación si el spikeCount es alto y revisar "
                "los eventos asociados a los picos más altos."
            ),
            effort="Bajo",
            category="quick win",
            priority="P1",
        )
    )

    diagnostics = metrics.diagnostics
    if diagnostics.hottest_phase and diagnostics.hottest_phase != "UNKNOWN":
        findings.append(
            Finding(
                title="Fase de render más lenta",
                impact=(
                    f"La fase {diagnostics.hottest_phase} concentra la mayor latencia "
                    "en la tubería de render, aumentando el riesgo de stutter sostenido."
                ),
                evidence=(
                    f"Archivo: {metrics.source.name}; hottest_phase={diagnostics.hottest_phase}, "
                    f"max={format_metric(diagnostics.hottest_phase_max_ms, 'ms')}."
                ),
                proposal=(
                    "Revisar el diseño de la fase indicada (batching, culling, shaders) y "
                    "considerar reordenar o paralelizar pasos críticos."
                ),
                effort="Alto",
                category="cambio estructural",
                priority="P0",
            )
        )

    if diagnostics.gc_pressure and diagnostics.gc_pressure >= 0.6:
        findings.append(
            Finding(
                title="Presión de GC elevada",
                impact="Pausas de GC elevan la latencia de frames y ticks durante el muestreo.",
                evidence=(
                    f"Archivo: {metrics.source.name}; gc_pressure={format_metric(diagnostics.gc_pressure)}, "
                    f"gc_recent_ms={format_metric(diagnostics.gc_recent_ms, 'ms')}, "
                    f"pause_max_ms={format_metric(diagnostics.pause_max_ms, 'ms')}."
                ),
                proposal="Reducir asignaciones por frame y ajustar parámetros de GC/heap.",
                effort="Medio",
                category="quick win",
                priority="P1",
            )
        )

    if diagnostics.stutter_cause and diagnostics.stutter_cause != "nozh.hud.stutter.unknown":
        findings.append(
            Finding(
                title="Causa de stutter detectada",
                impact="El motor ya identifica una causa dominante de stutter para priorizar mitigación.",
                evidence=(
                    f"Archivo: {metrics.source.name}; stutter_cause={diagnostics.stutter_cause}, "
                    f"confidence={format_metric(diagnostics.stutter_confidence)}."
                ),
                proposal="Validar la causa con perfiladores y atacar el subsistema señalado.",
                effort="Medio",
                category="cambio estructural",
                priority="P1",
            )
        )

    return findings


def gather_findings(exports: Iterable[TelemetryMetrics]) -> list[Finding]:
    all_findings: list[Finding] = []
    for export in exports:
        all_findings.extend(build_findings(export))
    return all_findings


def format_report(exports: list[TelemetryMetrics], findings: list[Finding]) -> str:
    lines = [
        "# Informe de telemetría (NOZH)",
        "",
        "## Resumen",
    ]
    if not exports:
        lines.extend(
            [
                "No se encontraron archivos CSV/JSON de telemetría en el directorio indicado.",
                "",
                "## Hallazgos",
                "- Hallazgo: Sin exportaciones disponibles",
                "  - Impacto: No es posible identificar picos, fases lentas o causas de stutter.",
                "  - Evidencia: Directorio vacío.",
                "  - Propuesta: Exportar telemetría con `/nozh telemetry export csv|json`.",
                "  - Esfuerzo: Bajo",
                "",
            ]
        )
        return "\n".join(lines)

    lines.append(f"Archivos analizados: {len(exports)}")
    lines.append("")
    lines.append("## Hallazgos")

    for finding in findings:
        lines.extend(
            [
                f"### {finding.title}",
                f"- Hallazgo: {finding.title}",
                f"- Impacto: {finding.impact}",
                f"- Evidencia: {finding.evidence}",
                f"- Propuesta: {finding.proposal}",
                f"- Esfuerzo: {finding.effort}",
                "",
            ]
        )

    return "\n".join(lines)


def format_backlog(findings: list[Finding]) -> str:
    if not findings:
        return "# Backlog de telemetría\n\n- P0: Exportar telemetría (CSV/JSON) para habilitar análisis."

    sorted_items = sorted(
        findings,
        key=lambda item: (item.priority, item.category),
    )

    lines = ["# Backlog de telemetría", ""]
    for index, item in enumerate(sorted_items, start=1):
        lines.extend(
            [
                f"{index}. [{item.priority}] {item.title} ({item.category})",
                f"   - Evidencia: {item.evidence}",
                f"   - Propuesta: {item.proposal}",
                f"   - Esfuerzo: {item.effort}",
                "",
            ]
        )

    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Genera informe y backlog de telemetría.")
    parser.add_argument(
        "--export-dir",
        type=Path,
        default=Path("config/nozh/telemetry_exports"),
        help="Directorio con exports CSV/JSON",
    )
    parser.add_argument(
        "--report-out",
        type=Path,
        default=Path("reports/telemetry_report.md"),
        help="Ruta de salida del informe",
    )
    parser.add_argument(
        "--backlog-out",
        type=Path,
        default=Path("reports/telemetry_backlog.md"),
        help="Ruta de salida del backlog",
    )

    args = parser.parse_args()
    export_dir: Path = args.export_dir
    exports: list[TelemetryMetrics] = []

    if export_dir.exists():
        exports = load_exports(export_dir)

    findings = gather_findings(exports)
    report_content = format_report(exports, findings)
    backlog_content = format_backlog(findings)

    args.report_out.parent.mkdir(parents=True, exist_ok=True)
    args.backlog_out.parent.mkdir(parents=True, exist_ok=True)

    args.report_out.write_text(report_content, encoding="utf-8")
    args.backlog_out.write_text(backlog_content, encoding="utf-8")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
