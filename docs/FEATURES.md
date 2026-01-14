# 🔍 NOZH v2.0 Deep Feature Guide

This document explains exactly how NOZH works under the hood. No marketing fluff—just mechanics.

## 1. The Neural Governor ("Optimization Engine")

NOZH uses a decision engine to manage game settings dynamically.

### How it works

It runs a loop every 20 ticks (1 second) or when specific events occur (like entering combat).

1. **Input**: It gathers data:
    * **FPS Delta**: Are frames dropping?
    * **Entity Density**: How many mobs are nearby?
    * **Chunk Updates**: Is the world loading fast?
    * **Player Speed**: Are you flying with Elytra?
2. **Process**: It feeds this data into the selected `Algorithm` (Neural or Heuristic).
3. **Output**: It calculates a `PressureScore` (0.0 to 1.0).
4. **Action**:
    * Pressure < 0.2: Do nothing (Relax).
    * Pressure > 0.5: Activate `Level 1` Actions (e.g., Reduce Particle Distance).
    * Pressure > 0.8: Activate `Level 2` Actions (e.g., Disable Clouds, Reduce Render Distance).

### Algorithms

* **Heuristic (Default)**: Uses static rules (If FPS < 30, do X). Fast and predictable.
* **Neural**: Uses a `Perceptron` that adjusts weights based on success. If disabling clouds fixed lag last time, it does it sooner next time.
* **Hybrid**: Uses Heuristic for emergencies (Panic mode) and Neural for background tuning.

---

## 2. Potato Mode Levels

Potato Mode is a hard override for low-end hardware. It bypasses user preferences to ensure the game is playable.

| Level | RAM Trigger | Cores Trigger | Settings Applied |
|:---|:---|:---|:---|
| **LEVEL 1 (Mild)** | < 8GB | < 6 Cores | RD: 12, ED: 8, Particles: 75% |
| **LEVEL 2 (Mod)** | < 4GB | < 4 Cores | RD: 8, ED: 6, Particles: 50%, Clouds: OFF |
| **LEVEL 3 (Aggr)** | -- | -- | RD: 6, ED: 4, Particles: 25%, Animations: OFF |
| **LEVEL 4 (Ext)** | < 2GB | < 2 Cores | RD: 4, ED: 3, Particles: 10%, All FX: OFF |
| **EXTREME** | -- | -- | RD: 2, ED: 2, Particles: 0%, **Minimal HUD** |

* **RD**: Render Distance (Chunks)
* **ED**: Entity Distance (Chunks)

---

## 3. System Tools & Config Management

Located in the **System** tab of the GUI (`/nozh gui`).

### Factory Reset

* **What it does**: Deletes `config/nozh.json` and immediately re-initializes `NozhConfig` with default Java object values.
* **When to use**: If you messed up settings so bad the game creates weird visual glitches (e.g., invisible entities).

### Config Backup

* **What it does**: Saves a copy of your current settings.
* **Clipboard Export**: Copies the entire JSON string to your clipboard. Useful for pasting into Discord for support.

### Hot Reload

* **What it does**: Re-reads the file from disk.
* **Why**: If you edit the `.json` file manually with Notepad, click this to apply changes without restarting the game.

---

## 4. Mod Compatibility (Stewardship)

NOZH follows a "Stewardship" model. It acknowledges that some mods (like Sodium) own certain parts of the game (Rendering).

* **Exclusive Mode**: If Sodium is installed, NOZH **completely disables** its own Chunk Rendering optimizations. It hands over control to Sodium.
* **Co-op Mode**: If ModMenu is installed, NOZH integrates its button into the menu.
* **Conflict Avoidance**: If C2ME is installed, NOZH disables aggressive chunk threading to prevent thread starvation.

We maintain a list of 50+ mods in our internal `CompatRegistry`.

---

## 5. Crash Guard

This system protects you from boot loops.

1. On boot, NOZH writes a `boot_marker` file.
2. If the game crashes before reaching the Main Menu, the marker remains "dirty".
3. If 3 "dirty" boots are detected in a row, NOZH enters **SAFE MODE**.
    * **SAFE MODE**: All complex logic (Neural AI, Potato Engine) is disabled. Only the bare minimum config loader runs. This allows you to open the game and fix the issue.

---
