# NOZH Configuration Guide

Configuration is located at `config/nozh/nozh.json`.
NOZH avoids complex configuration where possible, preferring smart defaults.

## Core

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Main toggle. If false, NOZH does nothing. |
| `debugLogs` | `false` | Enable verbose logging for analysis. |
| `language` | `"auto"` | `"auto"`, `"en_us"`, or `"es_cl"`. "auto" selects the game language if supported, otherwise falls back to en_us. |

## Safety & Limits

| Key | Default | Description |
| --- | --- | --- |
| `safeModeForce` | `false` | If true, forces NOZH into Safe Mode immediately. |
| `maxChangesPerSession` | `2` | Maximum number of tuning actions allowed per game session. |
| `cooldownActionMillis` | `120000` | (2 mins) Minimum time between repeating the SAME action. |
| `cooldownGlobalMinIntervalMillis` | `60000` | (1 min) Minimum time between ANY actions. |

> [!NOTE]
> **safeModeForce** does not persist and overrides runtime state. Use only for debugging or emergency lockdown.

## Tuning Rules

| Key | Default | Description |
| --- | --- | --- |
| `targetFps` | `60` | The frametime target NOZH aims for (e.g. 60 FPS = 16.6ms). |
| `allowAutoTuning` | `false` | **Required true** for NOZH to make changes. Default is monitoring only. |

## Automated Rollback (Phase 6.5)

| Key | Default | Description |
| --- | --- | --- |
| `rollbackEnabled` | `true` | Enables automatic revert if an action does not improve performance. |
| `rollbackWindowMillis` | `45000` | Time to wait before evaluating improvement. |
| `improvementEpsilonAvgMs` | `0.5` | Minimum Avg frametime improvement to count as success (noise tolerance). |
| `improvementEpsilonP95Ms` | `1.0` | Minimum P95 frametime improvement to count as success. |

> [!TIP]
> **Rollback Behavior**: If data is insufficient or noisy, rollback defaults to revert (conservative behavior). NOZH prefers undoing a change rather than risking a degrade.
