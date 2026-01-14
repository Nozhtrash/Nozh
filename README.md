# NOZH: Intelligent Frame Pacing Engine

> **[LEER EN ESPAÑOL / READ IN SPANISH](README_ES.md)**

**NOZH** is a client-side optimization mod for Minecraft Fabric 1.20.1. It is designed to stabilize frame times by dynamically adjusting render settings based on real-time performance telemetry.

Unlike general optimization mods that aim for "maximum FPS" (often at the cost of stability), NOZH prioritizes **consistency** (P99 frametimes). It achieves this by selectively reducing graphical fidelity during high-load scenarios and restoring it when the load decreases.

## 🛠 How It Works (Technical Deep Dive)

NOZH does not perform magic. It manages a trade-off: **Visual Fidelity vs. Input Latency**.

### 1. The Perceptron Predictor

NOZH uses a single-layer neural network (Perceptron) to forecast the probability of a lag spike in the *next* frame.

- **Inputs:** Entity Density (normalized), Particle Count, Chunk Updates, Player Velocity.
- **Output:** A strict probability (0.0 to 1.0) of the next frame exceeding the target frametime (e.g., >8.33ms for 120 FPS).
- **Training:** The model learns online. If it predicts a spike and one occurs, weights are reinforced. If it predicts a spike but the frame is smooth, it penalizes the weight. This allows it to adapt to *your* specific hardware over time (approx. 30-120 seconds of gameplay).

### 2. Transactional Governor

Any change made to your game settings (e.g., "Set Sodium Clouds to Fast") is executed as a **Transaction**.

1. **Capture:** The current state of Sodium is recorded.
2. **Execute:** The setting is changed.
3. **Audit:** The system monitors performance for the next 40-200 ticks.
4. **Rollback:** If the change does not statistically improve P99 frametimes (or makes them worse), the transaction is **rolled back**, restoring your original setting.

This ensures NOZH doesn't just "turn everything off" blindly. It only keeps changes that actually help your specific situation.

### 3. Integrated Ring Buffer

We store the last 600 frames of telemetry in a zero-allocation ring buffer. This allows us to calculate Standard Deviation and Mean in O(1) time, providing a statistically significant view of "smoothness" rather than reacting to single noise spikes.

---

## ⚠️ Realistic Expectations

**NOZH is NOT for you if:**

- You want 2000 FPS for screenshots.
- You play vanilla Minecraft on a high-end PC (you likely don't need dynamic adjustment).
- You want a mod that "just boosts FPS" without changing visuals. NOZH *will* change visuals (clouds, particles) to save frames.

**NOZH IS for you if:**

- You experience "micro-stutter" or "hitching" when loading chunks or fighting mobs.
- You play heavily modded packs where entity counts fluctuate wildly.
- You prefer a consistent 60/120/144 FPS over a fluctuating 400 FPS.

---

## 🎮 Usage Guide

### Installation

1. Install **Fabric Loader**.
2. Install **Sodium** (Required). NOZH orchestrates Sodium settings; without it, NOZH does very little.
3. Drop `nozh-2.0.0.jar` into your `mods` folder.

### Configuration

The mod works out-of-the-box (`config/nozh.json`).

- `targetFps`: Set this to your monitor's refresh rate (e.g., 60, 144).
- `allowedDegradations`: List of features NOZH is allowed to touch. If you *really* love clouds, remove `"CLOUDS"` from this list, and NOZH will never touch them, even if you lag.

### Commands

- `/nozh status` - View current neural weights and Governor state.
- `/nozh profile` - Run a distinct 10-second benchmark.
- `/nozh toggle` - Disable/Enable the mod on the fly.

---

## 🤝 Compatibility

- **Compatible:** Sodium, Lithium, ImmediatelyFast, FerriteCore, ModernFix.
- **Incompatible:** Any other "dynamic settings" mod (e.g., Dynamic FPS, Adrenalin). Using two dynamic optimizers will cause them to fight over settings, resulting in flickering.

---

## Open Source & Transparency

This project is open source. There is no telemetry sent to external servers. All learning data (weights) is stored locally on your machine and deleted when you restart the game (unless persisted in future updates).

We believe in honest optimization. NOZH trades visual quality for performance *only when necessary*, and strictly validates that trade-off.
