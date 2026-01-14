# ✨ NOZH Features Guide

This document defines every feature in NOZH v2.0, explaining **what it does**, **how it helps you**, and the **technical logic** behind it.

> **Transparency Note**: NOZH is designed to be honest. We never fake FPS numbers. We never delete files without asking. Everything is transparent.

---

## 1. Visual Layer (The Interface)

### 🖥️ Premium Dashboard

**For Dummies**: A beautiful menu where you can change settings.
**For Experts**: A custom GUI built on `DrawContext` with zero-allocation rendering. It bypasses standard Cloth Config bloat for maximum responsiveness.

- **How to access**: Press the "NOZH" button in ModMenu or type `/nozh gui`.
- **Key Feature**: Real-time Tooltips for every button explaining exactly what it does.

### 📈 Live Telemetry Graph

**For Dummies**: A line graph at the top of the menu that shows if your game is lagging (Red) or smooth (Green).
**For Experts**: A `GL_LINE_STRIP` rendered graph visualizing the last 60 seconds of `FrameTime` history.

- **Green**: <16ms (60+ FPS)
- **Yellow**: <33ms (30+ FPS)
- **Red**: >33ms (<30 FPS)

### 🧙‍♂️ First Run & Config Logic

**For Dummies**: NOZH auto-detects your hardware (RAM, GPU) and sets the best profile automatically.
**For Experts**: On first boot, the `PotatoModeEngine` profiles the JVM (`Runtime.maxMemory`) and GPU vendor. If it detects <4GB RAM or Intel Integrated Graphics, it pre-seeds the `PotatoConfig` to aggressive culling mode.

### 🛠️ System Management (New in v2.0)

**For Dummies**: Tools to fix your config if you break it.
**For Experts**:

- **Factory Reset**: Wipes `config/nozh.json` and re-initializes safe defaults.
- **Hot Reload**: Reloads config from disk without restarting Minecraft (useful for editing JSON manually).
- **Clipboard Export**: Serializes current config state to JSON string for efficient support/debugging.

---

## 2. Artificial Intelligence (The Brain)

### 🧠 Neural Lag Predictor

**For Dummies**: NOZH guesses when you are about to lag and fixes it before you notice.
**For Experts**: A Single-Layer Perceptron (SLP) neural network.

- **Inputs**: Chunk Loading Rate, Entity Count Delta, Memory Allocation Rate.
- **Output**: Lag Probability (0.0 - 1.0).
- **Training**: It uses "Online Learning" (Unsupervised). If it predicts lag and lag happens, it strengthens the synaptic weights.

### 🔀 Hybrid Decision Engine

**For Dummies**: Choose how "smart" you want NOZH to be.
**For Experts**: A configurable strategy pattern in the `Governor`.

- **NEURAL**: Pure AI prediction (high accuracy, warm-up time required).
- **HEURISTIC**: Rule-based logic (instant, predictable).
- **HYBRID**: Uses Heuristics for immediate threats and AI for trend prediction (Best of both worlds).

### 🎭 Anomaly Detector

**For Dummies**: Tells the difference between "My PC is slow" and "The Server is lagging".
**For Experts**: Compares Client Frame Time vs Server Ping/TPS via `CrashSafeGuard` telemetry.

- If **FPS is low** but **Ping is low**: GPU/CPU issue -> **Optimize Graphics**.
- If **FPS is high** but **Ping is high**: Network issue -> **Do Nothing** (Graphics tweaks won't help lag).

---

## 3. Optimization Engines (The Muscle)

### 🥔 Extreme Potato Mode

**For Dummies**: The "Emergency Switch" for very old laptops. Makes Minecraft look bad but run fast.
**For Experts**: A rigid profile that overrides user preferences.

- **Render Distance**: Locked to 2 chunks.
- **Simulation Distance**: Locked to 2 chunks.
- **Mipmaps**: 0 (Disabled).
- **Biome Blend**: 0 (Disabled).
- **Particles**: Minimal.
- **Logic**: Bypasses the "Safety Check" to enforce performance at all costs.

### 🛡️ Smart Mod Compatibility

**For Dummies**: NOZH knows if you are playing with other mods and resets itself to not break them.
**For Experts**: A `RemoteConfigFetcher` pulls a JSON file from the cloud on startup.

- **Knowledge Base**: Contains metadata for popular mods (Create, Sodium, Iris).
- **Conflict Resolution**: If `Iris` is detected, NOZH disables its own Shader/Cloud optimizations to prevent rendering glitches.

---

## 4. Safety Systems (The Guardian)

### ↩️ Auto-Rollback

**For Dummies**: If NOZH changes a setting and your game gets SLOWER, it undoes the change automatically.
**For Experts**:

1. Measure `AvgFrameTime` (Baseline).
2. Apply Action (e.g., `DECREASE_RENDER_DISTANCE`).
3. Wait 45 seconds (Window).
4. Measure `AvgFrameTime` (New).
5. If `New > Baseline + Threshold`, call `Executor.revert()`.

### 🚨 Crash Safe Guard

**For Dummies**: If the game crashes 3 times in a row, NOZH turns itself off so you can at least open the game.
**For Experts**:

- Uses a file marker `crash_guard` in the config folder.
- Increments a counter on every boot.
- Clears counter after 5 minutes of stability.
- If counter >= 3, activates **SAFE MODE** (Modules disabled, Listeners unregistered).

---

## 🛠️ Summary of "Orchestration"

NOZH doesn't just "tweak settings". It **Orchestrates** your entire game.

1. It **Watches** (Telemetry).
2. It **Thinks** (AI/Governor).
3. It **Acts** (Executor).
4. It **Learns** (Perceptron).

It is an active participant in your gameplay loop, simpler than a human but faster than one.
