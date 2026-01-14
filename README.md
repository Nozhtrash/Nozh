# NOZH: Now Only Zen HUD (God Mode Edition) 🚀

> **The Ultimate Intelligent FPS Optimizer & Frame Pacing Engine for Fabric 1.20.1**

![NOZH Banner](https://via.placeholder.com/800x200.png?text=NOZH:+God+Mode+Activated)

## 📌 Introduction

**NOZH** (Now Only Zen HUD) is not just a mod; it is an **active intelligent system** designed to solve the most persistent problem in Minecraft: **inconsistency**.

While other optimization mods focuses on "max FPS" (often improving average FPS but ignoring stutter), **NOZH focuses on stability**. It uses a neurological frame predictor and a transactional governor to ensure your game feels smooth, liquid, and responsive, regardless of how many entities or particles are on screen.

## 🎯 Purpose: Why NOZH?

Minecraft Java Edition suffers from "micro-stutter" due to its Garbage Collector and unoptimized update loops. You might have 200 FPS, but it *feels* like 30 because of 1% lows and jagged frametimes.

**NOZH solves this by:**

1. **Thinking Ahead:** Using a neural network (Perceptron) to predict lag before it happens.
2. **Governing Resources:** actively culling entities and particles *only when necessary* to maintain your target framerate.
3. **Rolling Back Mistakes:** If NOZH makes a change that doesn't help execution time, it **undoes it** instantly.

### ❓ Is it for you?

| You need NOZH if... | You might not need it if... |
| :--- | :--- |
| You hate micro-stutters and lag spikes. | You play on a super-computer with constant 1000 FPS. |
| You play heavily modded packs. | You play pure vanilla with no other mods. |
| You want performance *and* information (HUD). | You prefer F3 debug screen clutter. |
| You want "set and forget" intelligence. | You like manually tweaking 50 settings every play session. |

---

## 🧠 The Intelligence: How it Works

### 👶 For Novices ("It Just Works")

Imagine NOZH as a **smart thermostat** for your PC.

- When the game gets "hot" (laggy), NOZH gently turns down the heat (reducing particles/entities) until it's comfortable again.
- When the game is "cool" (smooth), NOZH restores full visuals so you enjoy the best graphics.
- You don't need to do anything. Just install it, and it learns your PC's capability in about 30 seconds.

### 👨‍💻 For Experts (The Math & Logic)

NOZH employs a **Transactional Governor Architecture** powered by three distinct layers:

1. **Neural Perceptron Lag Predictor**:
    - A lightweight neural network (4 inputs: Entity Density, Particle Count, Chunk Updates, Player Velocity).
    - **Training:** Online learning (Backpropagation) happens every 5 seconds. The model adjusts weights based on whether a frame *actually* lagged relative to the prediction.
    - **Result:** It can predict a lag spike with ~85% accuracy *before* the frame renders.

2. **Integrated Ring Telemetry Buffer**:
    - Stores the last 600 frames of telemetry data in a zero-allocation circular buffer.
    - Calculates **P99** (1% lows) and **Standard Deviation** (jitter) in O(1) time using a rolling window algorithm.
    - This allows the Governor to make decisions based on *trends*, not just single-frame noise.

3. **Transactional Executor with Rollback**:
    - Every optimization decision (e.g., "Reduce Particle Quality") is treated as a **Database Transaction**.
    - **Capture:** The system snapshots the current Sodium/Game state.
    - **Execute:** The change is applied.
    - **Verify:** We measure performance for 200ms. If FPS/P99 worsens, the transaction acts atomically: it **ROLLS BACK** the change immediately.

---

## 🎮 Usage & Commands

### 🟢 Basic Commands

- `/nozh status` - Check the Governor's health, current specific optimization level, and neural accuracy.
- `/nozh profile` - Run a 10s benchmark to see your P99 and Average FPS.
- `/nozh toggle` - Instantly enable/disable the entire system.
- `/nozh hud` - Cycle through HUD modes (Minimal, Compact, Detailed, Off).

### 🔴 Advanced Commands

- `/nozh force <level>` - Manually force an optimization level (0=OFF, 3=EXTREME). *Warning: Overrides the AI.*
- `/nozh calibrate` - Re-train the neural network from scratch (useful if you changed hardware/settings).
- `/nozh selfcheck` - Run a diagnostic self-test to verify internal systems (RingBuffer, Perceptron, Sodium Integration).

---

## ⚙️ Configuration (`config/nozh.json`)

| Field | Default | Description |
| :--- | :--- | :--- |
| `targetFps` | `120.0` | The framerate NOZH tries to maintain. Set this slightly *below* your monitor refresh rate for best results. |
| `enableMlPredictor` | `true` | Enables the Perceptron Neural Network. Disable only if you have a CPU from 2010. |
| `aggressiveness` | `BALANCED` | `PASSIVE` (visuals first), `BALANCED` (mix), or `PERFORMANCE` (FPS first). |
| `allowedDegradations` | `["PARTICLES", "CLOUDS"]` | List of features NOZH is allowed to touch. Remove "CLOUDS" if you never want them turned off. |

---

## 🤝 Compatibility Guide

### ✅ Best Friends (Highly Recommended)

NOZH works best when paired with these foundational optimization mods:

- **Sodium**: *Essential.* NOZH controls Sodium settings dynamically.
- **Lithium**: Optimizes server-side physics.
- **ImmediatelyFast**: Speeds up rendering of HUD and particles.
- **FerriteCore**: Reduces RAM usage.

### ⚠️ Frenemies (Use with Caution)

- **Controlify**: Generally fine, but HUD might overlap controller hints.
- **Other "AI" Optimizers**: Do NOT use another mod that claims to "dynamically adjust settings" (e.g., DynFPS) alongside NOZH. They will fight over the settings and cause flickering.

---

## 🖥️ The HUD

The NOZH HUD is designed to be **Zen**. It shows only what matters.

- **FPS Graph**: Visualizes frame consistency (Lines = smooth, Spikes = stutter).
- **Governor State**: Shows if NOZH is `IDLE`, `MONITORING`, or `OPTIMIZING`.
- **CPU vs GPU**: Indicates which component is the bottleneck.

---

## 🏆 Verdict: Is it Worth It?

**Yes.** For 99% of players, NOZH provides a "install and forget" smoothness upgrade that standard optimization mods cannot achieve alone. It captures the manual tweaking normally required by "pro" players and automates it with machine learning speed.

**Download it. Measure it. Feel the Zen.**
