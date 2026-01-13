# 🗺️ NOZH: Next-Generation Roadmap (2026)

> **Objective:** Evolve from a "Smart Optimizer" to a "Professional Performance Platform". Focus on Stability, User Experience, Neural Intelligence, and Ecosystem Integration.

---

## 🧭 Strategic Pillars

1.  **User Experience (UX) First**: Transition from "Command-line/Config-file" to "Interactive Visuals".
2.  **True Artificial Intelligence**: Move from Statistical Models (Bayesian/EMA) to Machine Learning (Neural Networks).
3.  **Professional Engineering**: CI/CD, Automated Benchmarking, Public API, and Enterprise-grade stability.
4.  **Ecosystem & Community**: Cloud profiles, Leaderboards, and Modpack integration.

---

## 📅 H1 2026: The "Professional" Update (v1.1 - v1.5)

### 🎨 Phase 1: Visual Interaction (v1.1)
*Making the mod feel accessible and premium.*

*   **[ ] Feature: In-Game Configuration GUI**
    *   **Concept**: Replace config files with a sleek, heavily animated menu.
    *   **Tech**: Custom GUI engine (not just Cloth Config) or highly styled YACL.
    *   **Details**: Real-time graphs of FPS impact per setting. "Apply & Test" button that reverts if performance drops.
*   **[ ] Feature: "First-Run" Calibration Wizard**
    *   **Concept**: A 30-second guided setup when installed for the first time.
    *   **Steps**: Detect Hardware -> Run Micro-Benchmark -> Ask "Quality vs Performance" -> Apply Profile.
*   **[ ] Polish: Detailed Metrics Overlay**
    *   **Concept**: Expand the HUD with a "Vitals" graph (frametime variance over last 60s).
    *   **Tech**: GL-rendered line graphs with gradient fills (like MSI Afterburner).

### 🧠 Phase 2: The Neural Core (v1.2)
*True AI prediction to replace/augment heuristics.*

*   **[ ] Feature: `NeuralLagPredictor` (TNN)**
    *   **Concept**: A specific, lightweight Tensor Neural Network trained *locally* on the user's gameplay.
    *   **Inputs**: Entity counts, Particle counts, Chunk updates, Player velocity, Dimension.
    *   **Outputs**: Probability of frame drop in next 2 seconds.
    *   **Action**: Pre-emptive culling (e.g., stop rendering particles *before* the explosion lands).
*   **[ ] Feature: Anomaly Detection**
    *   **Concept**: Identify "Normal" vs "Abnormal" lag spikes.
    *   **Value**: Don't optimize if the lag is network-related or GC-related; only optimize rendering lag.

### 🛡️ Phase 3: Stability & Hygiene (v1.3)
*Ensuring the mod is bulletproof for modpacks.*

*   **[ ] Infrastructure: Automated Regression Testing**
    *   **Concept**: A CI pipeline that runs Minecraft headless and measures 'tick' times on standard maps.
    *   **Goal**: Zero performance regressions on updates.
*   **[ ] Feature: "Safe Mode" Bootstrapper**
    *   **Concept**: If NOZH detects a crash during startup 2x in a row, it auto-disables itself or resets config to "Safe".
    *   **UX**: Windows-style "The game crashed recently. Start in Safe Mode?" dialog.
*   **[ ] Documentation: Developer API & Javadocs**
    *   **Concept**: Allow other mods to say `NozhApi.requestHighPerformanceMode()` during intense cutscenes.

---

## 📅 H2 2026: The Ecosystem Update (v2.0+)

### ☁️ Phase 4: Cloud & Community
*Connecting users to share optimizations.*

*   **[ ] Feature: NOZH Cloud Profiles (Anonymous)**
    *   **Concept**: Upload "Hardware Fingerprint + Config + FPS Gain".
    *   **Value**: "Users with your GPU (RTX 3060) usually get +25% FPS with *this* profile. Download?"
*   **[ ] Feature: Verified Modpack Presets**
    *   **Concept**: Curated configs for big packs (All The Mods, Better Minecraft).
    *   **Tech**: Auto-detect Modpack ID and fetch tuning from GitHub.

### 🔌 Phase 5: Expand Horizons

*   **[ ] Port: NeoForge / Forge Support**
    *   **Concept**: Bring NOZH to the other 50% of the playerbase.
    *   **Strategy**: Architect "Core" vs "Loader" abstraction layer.
*   **[ ] Companion: Mobile Dashboard (Web/App)**
    *   **Concept**: View PC's performance graph on phone while playing full screen.
    *   **Tech**: Tiny internal HTTP server exposing a JSON endpoint + React/PWA frontend.

---

## 💡 "Blue Sky" Ideas (Backlog)

*   **The "Streamer" Mode**: OBS integration to auto-detect when streaming and prioritize encoding stability over raw FPS.
*   **Voice Assistant**: "Hey Nozh, I'm lagging" -> triggers aggressive clean-up.
*   **Generative Settings**: Use LLM logic (offline/small) to *explain* why a setting was changed in natural language in the chat. "I lowered render distance because I detected 500 zombies nearby."

---

## 🛠️ Immediate Task List (Next 48h)

1.  [ ] **Design**: Sketch the In-Game GUI wireframe.
2.  [ ] **Tech**: Prototype the `NeuralLagPredictor` using simple perceptrons (no heavy DL libs).
3.  [ ] **Infra**: Fix the Gradle Proxy issue to unblock builds.
