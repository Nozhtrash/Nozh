# NOZH: Intelligent Frame Pacing Engine (God Mode)

> **[LEER EN ESPAÑOL / READ IN SPANISH](README_ES.md)**

**NOZH** is a next-generation client-side optimization mod for **Minecraft Fabric 1.20.1**. Unlike traditional FPS boosters that blindly lower settings, NOZH uses a **neural-hybrid engine** to stabilize frame times, eradicate micro-stutters (`1% lows`), and dynamically balance visual quality against performance.

If your game stutters during combat or chunk loading, NOZH fixes it by sacrificing visuals *only when necessary* and restoring them instantly when the load clears.

---

## 🚀 Key Features (v2.1.0)

### 🧠 Intelligent Optimization

- **Perceptron Predictor**: A lightweight neural network that predicts frame time spikes before they happen.
- **Dynamic Quality**: Automatically adjusts Sodium settings (render distance, particles, entity culling) in real-time.
- **Scenario Detection**: Identifies what you are doing (Combat, exploring, AFK, Building) and switches profiles automatically.

### 🥔 Potato Mode (New!)

- **Emergency Protocol**: If your FPS drops below a critical threshold (e.g., <20 FPS), NOZH activates "Potato Mode".
- **Aggressive Tactics**: Instantly minimizes particles, shadows, and unnecessary rendering to restore playability.
- **One-Key Toggle**: Manually toggle manually with `[K]` (configurable).

### 🛡️ Safety Systems

- **Crisis Rollback**: If NOZH makes a change and your FPS gets *worse*, it automatically reverts that change within 45 seconds.
- **Safe Mode**: If the game crashes or becomes unstable, NOZH locks itself into a "Safe Mode" to prevent loops.
- **Self-Check**: Run `/nozh selfcheck` to audit your installation, detect mod conflicts (e.g., Sodium + OptiFabric), and verify system health.

### 📊 Professional HUD 2.0

- **Advanced Metrics**:
  - **P99 Lows**: The true measure of stutter.
  - **Variance (ms²)**: How "jittery" your game feels.
  - **Bottleneck**: Tells you if you are CPU-bound or GPU-bound.
- **Visual Graph**: Zero-allocation real-time frametime graph.
- **Quick Menu**: Press `[H]` to open a fast overlay for toggles and profiles.

### 🌍 Global Language Support

Fully translated into:

- 🇺🇸 English (US)
- 🇪🇸 Spanish (ES)
- 🇧🇷 Portuguese (BR)
- 🇫🇷 French (FR)
- 🇩🇪 German (DE)
- 🇮🇹 Italian (IT)
- 🇯🇵 Japanese (JP)

---

## 🛠️ Installation

1. **Install Fabric Loader** for Minecraft 1.20.1.
2. **Install Sodium** (Required). NOZH acts as an intelligent "conductor" for Sodium's rendering engine.
3. Download the latest `nozh-x.x.x.jar` release.
4. Drop it into your `.minecraft/mods` folder.

**Recommended:** Pair with `Indium` and `Lithium` for best results.

---

## ⚙️ Configuration

Press `[K]` or use ModMenu to open the **NOZH Dashboard**.

### Tabs

1. **General**: Master switch, target FPS, and Preset Profiles (Potato, Low, Mid, Ultra).
2. **Automation**: Configure how aggressive the neural network should be (`Decision Budget`, `History Size`).
3. **Visuals**: Fine-tune what NOZH is allowed to downgrade (e.g., only particles, but keep render distance).
4. **System**: View logs, export telemetry, or Factory Reset the mod.

### Commands

- `/nozh status`: View current AI confidence and active profile.
- `/nozh selfcheck`: Run a system diagnostic.
- `/nozh profile`: Run a 10-second benchmark to calibrate the engine.
- `/nozh toggle`: Enable/Disable the mod on the fly.

---

## ❓ FAQ

**Q: Will this increase my maximum FPS?**
A: Maybe. But NOZH's goal is **consistency**, not peak numbers. 60 FPS with 0 stutters feels smoother than 400 FPS that drops to 20 every few seconds.

**Q: Is it compatible with shaders?**
A: Yes! NOZH detects Iris/Shaders and switches to a "Conservative Mode" to avoid breaking visual effects.

**Q: Can I use it on a server?**
A: Yes. NOZH is strictly client-side. It works on any server (Vanilla, Spigot, Modded) without needing to be installed on the server.

---

## 📝 License

This project is licensed under the MIT License.
NOZH is open-source and free forever.
