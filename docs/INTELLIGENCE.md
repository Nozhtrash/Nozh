# 🧠 Intelligence & Compatibility Guide

NOZH distinguishes itself from other optimizers by having **"Intelligence"**. It relies on data, not hardcoded assumptions.

This document details:
1.  **Mod Awareness** (How it handles other mods).
2.  **Cloud Sync** (How it stays updated).
3.  **Neural Prediction** (How it learns).

---

## 1. Mod Awareness System

Minecraft is rarely played vanilla. NOZH includes a **Mod Knowledge Base** to ensure it plays nicely with others.

### The Problem
If NOZH lowers render distance while a mod like **Create** needs to render a distant train, visual glitches occur. If NOZH changes fog while **Iris** is handling shaders, the game might crash.

### The Solution: `ModKnowledgeBase`
NOZH scans your `mods` folder on startup. It tags mods with attributes:

| Mod Category | Examples | NOZH Behavior |
|--------------|----------|---------------|
| **Rendering** | Sodium, Iris, Canvas | **PASSIVE MODE**: NOZH disables its own rendering tweaks and lets these mods handle it. |
| **Heavy Tech** | Create, Mekanism, AE2 | **AGGRESSIVE CULLING**: NOZH enables aggressive BlockEntity culling to save FPS in factories. |
| **World Gen** | Terralith, Biomes O Plenty | **PRELOAD STRATEGY**: NOZH allows chunk pre-generation to prevent stutter while exploring. |

### Technical Implementation
- **Class**: `dev.nozh.core.knowledge.ModKnowledgeBase`
- **Method**: `detect()` scans Fabric Loader's mod container.
- **Result**: Generates a `detected_environment` flag set (e.g., `ENV_HEAVY_TECH`).

---

## 2. Cloud Intelligence (Remote Config)

Mod compatibility changes daily. We cannot release a new JAR file every time a mod updates.

### The Solution: `RemoteConfigFetcher`
On startup, NOZH makes a lightweight `GET` request to our GitHub Repository.
- **URL**: `raw.githubusercontent.com/TrxyyPC/nozh-rules/main/compatibility.json`
- **Payload**: A JSON list of "Bad Mods" or "Conflict Rules".

### How it works
1.  **Boot**: Game starts.
2.  **Fetch**: NOZH downloads latest rules (Async, doesn't slow down boot).
3.  **Apply**: If a new conflict rules says *"Mod X causes crash with Action Y"*, NOZH hot-patches its `ActionMatrix` to disable Action Y.
4.  **Cache**: If you are offline, it uses the last downloaded version from `compatibility_cache.json`.

---

## 3. The Neural Predictor (AI)

This is the crown jewel of NOZH v2.0.

### The Problem
Traditional optimizers are **Reactive**. They wait for FPS to drop, THEN they fix it. This means you still feel the lag spike.

### The Solution: **Proactive Prediction**
NOZH uses a simple **Perceptron Neural Network** to predict lag *before* a frame is dropped.

### How it Works
The AI monitors 3 inputs (Neurons) continuously:
1.  **Chunk Loading Rate**: Are we generating new terrain quickly?
2.  **Entity Delta**: Did 50 zombies just spawn?
3.  **GC Pressure**: Is Java about to run Garbage Collection?

**The Calculation**:
```
Trigger = (ChunkRate * WeightA) + (EntityDelta * WeightB) + (GCPressure * WeightC)
```

If `Trigger > Threshold`, the AI assumes a lag spike is impending.
It performs a **Pre-emptive Strike**: It lowers settings *milliseconds before* the lag happens.

### Training (Online Learning)
1.  **Predict**: AI says "Lag is coming!" -> Lowers settings.
2.  **Observe**: Did lag happen?
3.  **Feedback**:
    - If Lag **DID NOT** happen: Good prediction. Weights are strengthened.
    - If Lag **DID** happen anyway: Prediction was too weak. Weights are adjusted.
    - If AI predicted lag, but everything was super smooth: False Positive. Weights are reduced.

This means NOZH **learns your specific computer**. If your CPU handles entities well but struggles with Chunk Loading, the AI learns to ignore Entities and panic on Chunk Loading.
