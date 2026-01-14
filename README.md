<div align="center">
  <img src="https://via.placeholder.com/150/000000/FFFFFF/?text=NOZH" width="128" height="128" alt="NOZH Logo" />
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>The Intelligent Performance Orchestrator for Minecraft (Fabric 1.20.1)</b><br>
    <i>"A Governor, not just an Optimizer."</i>
  </p>

  <p>
    <a href="https://github.com/Nozhtrash/Nozh/releases"><img src="https://img.shields.io/badge/Version-2.0.0_God_Mode-00FF00?style=for-the-badge&logo=appveyor" alt="Version"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Platform-Fabric-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="https://discord.gg/nozh"><img src="https://img.shields.io/discord/1234567890?label=Discord&style=for-the-badge&logo=discord&color=5865F2" alt="Discord"></a>
  </p>
  
  <p>
    <a href="README_ES.md">🇪🇸 <b>LEER EN ESPAÑOL</b></a>
  </p>
</div>

---

# 📖 What is NOZH?

**NOZH** (Novus Optima Zen HUD) is NOT another "FPS Boost" mod like Sodium. It is a **Behavioral Orchestrator**.

While Sodium optimizes *how* the game renders, NOZH optimizes *what* the game renders. It acts as a manager that watches your gameplay in real-time and makes decisions to keep your frame rate stable.

### 🧠 The Core Philosophy: "Orchestration"

Most mods are static: you set `Particles: All` and they stay there forever, even if you are lagging to death in a raid.

**NOZH is dynamic**:

1. **Watches**: Uses a `Perceptron` (Simple Neural Network) to monitor Chunk Updates, Entity Count, and Frame Time.
2. **Decides**: If lag is predicted, it *temporarily* reduces quality (e.g., disables clouds, trims particle distance).
3. **Restores**: As soon as performance stabilizes, it restores your visuals to maximum quality.

---

# ✨ Top Features

| Feature | Reality Check (Transparency) |
|:--- |:--- |
| **Potato Mode** | A specialized profile for hardware with <4GB RAM or Integrated Graphics. It locks render distance to 2-6 chunks and aggressively culls entities. **It makes the game look worse to make it playable.** |
| **Neural Governor** | Uses a configurable algorithm (Neural, Heuristic, or Hybrid) to predict lag. It is not ChatGPT; it is a mathematical model trained on your gameplay session to balance detailed vs. smooth gameplay. |
| **System Resilience** | Includes a **Factory Reset** and **Config Backup** tool directly in the menu. If you mess up your settings, you can fix them without deleting files manually. |
| **Cloud Intelligence** | Downloads a JSON file from our GitHub on launch (`compatibility.json`). This tells NOZH about new mods so it doesn't break them (e.g., it auto-disables shader tweaks if Iris is found). |
| **Premium HUD** | A zero-garbage HUD that shows real-time graphs of your Frame Time (ms). Green = Good, Red = Bad. |

👉 **[Read the Full Feature Guide (Detailed)](docs/FEATURES.md)**

---

# 🤖 "Is it Compatible?"

**YES.** NOZH is designed to be a "Good Citizen".

It actively detects:

* **Sodium / Iris**: Delegates rendering tasks to them. NOZH manages *logic*, Sodium manages *graphics*.
* **Lithium / Starlight**: Fully compatible.
* **C2ME**: NOZH adjusts chunk priorities to avoid conflicts.
* **VulkanMod**: Detected and respected.

Check `docs/COMPATIBILITY.md` for the full list of 50+ known mods.

---

# 🚀 Installation & Usage

### 1. Installation

1. Requires **Fabric Loader** and **Fabric API** for Minecraft 1.20.1.
2. Drop `nozh-2.0.0.jar` into `.minecraft/mods`.
3. (Optional) Install [ModMenu](https://modrinth.com/mod/modmenu) to access the config screen easily.

### 2. First Launch

NOZH will run a **Hardware Scan**.

* **Weak PC**: Auto-enables `Potato Mode`.
* **Strong PC**: Defaults to `Survival Mode` (High Quality).

### 3. Configuration

Press the **NOZH** button in the pause menu (via ModMenu) or type `/nozh gui`.

* **System Tab**: Reset or Backup your config.
* **Advanced Tab**: Switch between Neural (AI) or Heuristic (Rules) governor.

---

# ⚠️ Honesty Disclaimer

NOZH cannot download more RAM for you.

* If your PC is a toaster, **Potato Mode** will help, but it won't make it run shaders at 120 FPS.
* The "AI" is a local algorithm. It does not send your data to OpenAI or Google. It runs entirely on your CPU (impact: <0.1ms per tick).

---

<div align="center">
  <p><i>Made with ❤️ by the Nozhtrash Team. Open Source. Transparent.</i></p>
</div>
