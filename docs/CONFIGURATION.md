# ⚙️ NOZH Configuration Guide

This document explains every single option found in `config/nozh.json` or the in-game GUI.

## 1. General Settings

| Option | Default | Description |
|:---|:---|:---|
| `enabled` | `true` | Master switch. If `false`, NOZH does absolutely nothing. |
| `governorMode` | `HEURISTIC` | The brain of the mod. Options: `NEURAL` (AI), `HEURISTIC` (Rules), `HYBRID`. |
| `potatoMode` | `false` | If `true`, overrides almost all visual settings to boost FPS. |

## 2. Thresholds (When to Act)

| Option | Default | Description |
|:---|:---|:---|
| `minAcceptableFps` | `60` | If FPS drops below this, NOZH starts optimizing. |
| `maxAcceptablePing` | `100` | Used by Anomaly Detector. If Ping > 100, NOZH assumes lag is network-related, not GPU-related. |
| `hysteresisTicks` | `100` | (5 seconds). How long to wait before switching states (prevent flickering). |

## 3. Heuristic Rules (Manual Tuning)

These settings control what NOZH turns off when lag occurs.

| Option | Effect | Impact |
|:---|:---|:---|
| `decreaseRenderDistance` | Reduces chunk distance dynamically (e.g., 12 -> 8). | High |
| `cullParticles` | Reduces particle rendering distance. | Medium |
| `disableClouds` | Turns off cloud rendering. | Low |
| `disableAnimations` | Stops texture animations (water, fire). | Low |
| `cullEntities` | Hides entities that are far away. | High |

## 4. Stewardship (Compatibility)

| Option | Default | Description |
|:---|:---|:---|
| `respectExternalMods` | `true` | If `true`, NOZH will disable its own features if it finds a better mod (e.g., Sodium) handling them. |
| `enableCloudConfig` | `true` | Allows NOZH to download `compatibility.json` from GitHub on startup. |

## 5. Debugging

| Option | Default | Description |
|:---|:---|:---|
| `debugLogging` | `false` | Spams the log file with decision data. Only use if reporting a bug. |
| `showHud` | `true` | Toggles the in-game overlay. |

> **Pro Tip**: You can edit `config/nozh.json` while the game is running and press "Hot Reload" in the System tab to apply changes instantly.
