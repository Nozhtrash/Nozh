#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class CrashFreeIndicator:
    crash_free_percent: float
    crash_loop_detected: bool
    safe_mode_active: bool
    safe_mode_causes: list[str] | None
    boot_attempts: int | None
    session_stable: bool | None
    last_failure_context: dict[str, Any] | None


@dataclass(frozen=True)
class CompatibilityIndicator:
    compat_score: float | None
    conflict_count: int | None
    loaded_mods: int | None
    recommendations: list[str] | None


@dataclass(frozen=True)
class ObservabilityIndicators:
    generated_at: str
    crash_free: CrashFreeIndicator
    compatibility: CompatibilityIndicator


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compute crash-free and compatibility indicators for observability dashboards."
    )
    parser.add_argument(
        "--state-file",
        type=Path,
        default=Path("config/nozh/state.json"),
        help="Path to state.json",
    )
    parser.add_argument(
        "--compat-file",
        type=Path,
        default=Path("config/nozh/compat_report.json"),
        help="Path to compatibility report JSON",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("reports/observability_indicators.json"),
        help="Output JSON path",
    )
    return parser.parse_args()


def load_json(path: Path) -> dict[str, Any] | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def detect_crash_loop(state: dict[str, Any]) -> bool:
    safe_mode_causes = state.get("safeModeCauses") or []
    boot_attempts = state.get("bootAttempts", 0)
    session_stable = state.get("sessionStable", False)
    return "CRASH_LOOP" in safe_mode_causes or (boot_attempts >= 3 and not session_stable)


def build_crash_free_indicator(state: dict[str, Any] | None) -> CrashFreeIndicator:
    if not state:
        return CrashFreeIndicator(
            crash_free_percent=100.0,
            crash_loop_detected=False,
            safe_mode_active=False,
            safe_mode_causes=None,
            boot_attempts=None,
            session_stable=None,
            last_failure_context=None,
        )
    crash_loop_detected = detect_crash_loop(state)
    crash_free_percent = 0.0 if crash_loop_detected else 100.0
    return CrashFreeIndicator(
        crash_free_percent=crash_free_percent,
        crash_loop_detected=crash_loop_detected,
        safe_mode_active=bool(state.get("safeModeCauses")),
        safe_mode_causes=state.get("safeModeCauses"),
        boot_attempts=state.get("bootAttempts"),
        session_stable=state.get("sessionStable"),
        last_failure_context=state.get("lastFailureContext"),
    )


def extract_compat_score(payload: dict[str, Any]) -> float | None:
    for key in ("compatScore", "compatibilityScore", "score"):
        value = payload.get(key)
        if isinstance(value, (int, float)):
            return float(value)
    return None


def build_compat_indicator(compat: dict[str, Any] | None) -> CompatibilityIndicator:
    if not compat:
        return CompatibilityIndicator(
            compat_score=None,
            conflict_count=None,
            loaded_mods=None,
            recommendations=None,
        )
    conflicts = compat.get("conflicts")
    loaded_mods = compat.get("loadedMods")
    recommendations = compat.get("recommendations")
    return CompatibilityIndicator(
        compat_score=extract_compat_score(compat),
        conflict_count=len(conflicts) if isinstance(conflicts, list) else None,
        loaded_mods=len(loaded_mods) if isinstance(loaded_mods, list) else None,
        recommendations=recommendations if isinstance(recommendations, list) else None,
    )


def main() -> None:
    args = parse_args()
    state = load_json(args.state_file)
    compat = load_json(args.compat_file)

    indicators = ObservabilityIndicators(
        generated_at=datetime.now(timezone.utc).isoformat(),
        crash_free=build_crash_free_indicator(state),
        compatibility=build_compat_indicator(compat),
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(asdict(indicators), indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
