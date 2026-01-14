# 🧠 NOZH Intelligence: How the AI Works

> **Honesty Disclaimer**: NOZH uses "Narrow AI" (Statistical Machine Learning). It is not "Generative AI" like ChatGPT. It does not "think" like a human. It calculates probabilities based on patterns.

## Technical Architecture

The core of NOZH's intelligence is the `PerformancePredictor.java` class. It implements a **Single-Layer Perceptron**.

### 1. Inputs (The Senses)

The AI receives a vector of normalized values (-1.0 to 1.0) every second:

* `x1`: **Entity Trend** (Is the number of mobs increasing rapidily?)
* `x2`: **Chunk Load Pressure** (Are we generating new terrain?)
* `x3`: **Frame Time Variance** (Is the FPS unstable?)
* `x4`: **Player Velocity** (Are we moving fast?)

### 2. Processing (The Weights)

Each input corresponds to a "Weight" (`w1` to `w4`).

* Example: If `Entity Trend` historically causes lag on your PC, `w1` will be high (e.g., 0.8).
* Example: If `Player Velocity` never causes lag (you have a fast SSD), `w4` will be low (e.g., 0.1).

The Perception calculates the **Lag Probability**:
`P = Activation( (x1*w1) + (x2*w2) + (x3*w3) + (x4*w4) )`

### 3. Training (The Learning)

This is "Online Unsupervised Learning".

1. **Selection**: NOZH predicts lag (`P > 0.7`).
2. **Action**: It takes an action (e.g., reduces particles).
3. **Feedback**: It waits 5 seconds.
    * If FPS improved: **Reward** (Strengthen the weights that triggered the action).
    * If FPS stayed bad: **Punish** (Weaken the weights; that was a false positive).

## Heuristic Fallback

If the Neural Governor is unsure (`0.3 < P < 0.7`), it falls back to **Heuristics** (Hardcoded Rules).

* *Rule*: "If FPS < 20 for 3 seconds -> EMERGENCY POTATO MODE".
* *Rule*: "If Server TPS < 10 -> Ignore Client FPS (It's a server issue)".

## Limitations

* **Warm-up**: The AI starts with generic weights. It takes about 10-20 minutes of gameplay to "learn" your specific hardware bottlenecks.
* **Local Only**: Training data is stored in `brain/weights.json` on your PC. It is never uploaded.
