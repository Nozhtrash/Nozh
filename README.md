<div align="center">
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>Intelligent Performance Orchestrator for Minecraft (Fabric)</b><br>
    <i>The First "Smart" Optimizer that Adapts to Your Hardware (v2.0 Professional)</i>
  </p>

  <p>
    <a href="https://github.com/NozhMod/Nozh/actions"><img src="https://img.shields.io/badge/Status-PROFESSIONAL%20v2.0-00FF00?style=for-the-badge&logo=appveyor" alt="Status"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Minecraft-1.20.1-FFA500?style=for-the-badge&logo=minecraft" alt="Version"></a>
    <a href="docs/FEATURES.md"><img src="https://img.shields.io/badge/Read-FEATURES-blue?style=for-the-badge" alt="Features"></a>
  </p>
</div>

---

# 📖 Overview

**NOZH** is not just an optimization mod; it is an **AI-driven Performance Governor**. Unlike standard mods (Sodium, FerriteCore) that optimize *rendering code*, NOZH optimizes *behavior*.

It watches your gameplay in real-time. If it detects lag, it intelligently sacrifices specific visual effects (like clouds, particles, or shadows) to restore smooth 60 FPS. When the danger passes, it restores them.

### 🧠 Why is it "Intelligent"?

- **Neural Lag Prediction**: Uses a Perceptron AI to predict lag spikes *before* they happen.
- **Scenario Awareness**: Knows the difference between **PvP** (where fps matters) and **Building** (where visuals matter).
- **Hardware Profiling**: Identifies if you are running on a "Potato" laptop or a High-End Rig and adjusts automatically.
- **Mod Knowledge**: Automatically detects modpacks (e.g., Tech Mods, Magic Mods) and tunes settings to prevent crashes.

---

# ✨ Features at a Glance

| Feature | Description |
|---------|-------------|
| **Premium Dashboard** | A AAA-quality in-game menu (`/nozh gui`) with real-time telemetry graphs. |
| **Neural AI** | Learns from your gameplay. If `Action A` fixed lag last time, it prioritizes it next time. |
| **Potato Mode** | A special `EXTREME` profile for PCs with 2GB RAM / Intel HD Graphics. |
| **System Tools** | Built-in **Factory Reset**, **Config Backup**, and **One-Click Repair** tools. |
| **Hybrid AI** | Configurable decision engine: Choose between **Neural**, **Heuristic**, or **Hybrid** logic. |
| **Cloud Rules** | Fetches live compatibility updates from the cloud to prevent conflicts with new mods. |
| **CrashGuard** | Detects boot loops and automatically isolates the problem to let you launch the game. |

👉 **[Read the Full Feature Guide (For Dummies & Experts)](docs/FEATURES.md)**

---

# 🤖 How It Works (The Orchestrator)

NOZH acts as a **Supervisor** for your Minecraft client.

1. **Monitor**: `VitalsRecorder` measures Frame Times (ms) and Network Latency (Ping).
2. **Analyze**: `AnomalyDetector` determines if a lag spike is caused by **Graphics** (GPU) or **Server** (Network).
3. **Decide**: The `Governor` (Brain) checks the **Action Matrix** to find the best solution for the current **Scenario**.
    - *Example: "Player is in COMPAT. FPS is low. Disable PARTICLES."*
4. **Execute**: Applies the change instantly.
5. **Verify**: If FPS doesn't improve within 45 seconds, the **Rollback System** undoes the change.

👉 **[Technical Deep Dive (Architecture)](docs/ARCHITECTURE.md)**

---

# 🚀 Getting Started

### Installation

1. Install **Fabric Loader** (1.20.1).
2. Install **Fabric API**.
3. Drop `nozh-2.0.0.jar` into your `mods` folder.
4. Launch the game!

### First Run

On your first launch, NOZH will open the **Setup Wizard**.

- Choose **"Potato Mode"** if you have a slow PC.
- Choose **"High Fidelity"** if you have a strong GPU.

### Commands

- `/nozh gui` - Open the Main Dashboard.
- `/nozh hud <mode>` - Change the onscreen info (Minimal/Compact/Expert).
- `/nozh status` - Check what the AI is thinking.

---

<div align="center">
  <p><i>Made with ❤️ by the Nozhtrash Team</i></p>
  <p><b>Transparency • Intelligence • Performance</b></p>
</div>
