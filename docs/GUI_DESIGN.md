# 🎨 In-Game Configuration GUI Design

> **Goal**: Create a premium, "Hypixel-style" or "Badlion-style" configuration menu that feels native yet modern.

## 1. Design Philosophy
- **Dark & Glassy**: Semi-transparent black backgrounds with blur (where supported).
- **Motion**: Smooth transitions (200ms ease-out) for all hovers and clicks.
- **Immediate Feedback**: Don't force users to "Save & Quit" to see changes. Apply instantly.

---

## 2. Wireframes

### A. Main Dashboard
The entry point when pressing `N`.

```
+-------------------------------------------------------------+
|  NOZH OPTIMIZER v1.1                                  [X]   |
+-------------------------------------------------------------+
| [ PERF ] [ QLTY ] [ AUTO ]      |  SYSTEM VITALS            |
|                                 |                           |
|  Current Mode: GOD MODE         |  FPS: 144  (Avg: 120)     |
|  > Everything is maxed out.     |  [|||||||||||||......]    |
|                                 |                           |
|  [ ENABLE SAFEMODE ]            |  Prediction: STABLE       |
|                                 |                           |
+--------------------------------------+----------------------+
|  CATEGORIES                          |  PREVIEW             |
|                                      |                      |
|  > ⚡ General Performance            |                      |
|    🌊 Fluidity & Motion              |  (Live 3D Player     |
|    👁️ Visual Quality                 |   Model Here)        |
|    🧠 AI & Learning                  |                      |
|    ☁️ Cloud Profiles                 |                      |
|                                      |                      |
+--------------------------------------+----------------------+
|  [ RESET ]          [ UNDO ]          [ APPLY PRESET v ]    |
+-------------------------------------------------------------+
```

### B. Category: General Performance
When clicking "General Performance".

```
+-------------------------------------------------------------+
|  < BACK        ⚡ GENERAL PERFORMANCE                 [?]   |
+-------------------------------------------------------------+
|                                                             |
|  Render Distance Optimization                               |
|  [====O================] 12 Chunks (Dynamic)                |
|  *Lowers to 8 during combat automatically.                  |
|                                                             |
|  Entity Culling                                             |
|  [ ON ]  Hide entities behind walls                         |
|  [ ON ]  Hide entities too far away                         |
|                                                             |
|  Particle Limits                                            |
|  ( ) All   (•) Decreased   ( ) Minimal                      |
|                                                             |
|  Animation Speed w/ Lag                                     |
|  [=======O=============] 1.0x                               |
|                                                             |
+-------------------------------------------------------------+
```

---

## 3. Interaction Specs

### Hover Effects
- **Buttons**: Scale up 1.05x, brighten background color.
- **Sliders**: Handle glows white.
- **Toggles**: Smooth slide animation from left (grey) to right (green).

### "Safe Mode" Prompt
If the user selects a setting that causes a massive FPS drop (>30% variance) within 2 seconds:
> ⚠️ **Performance Warning**
> Your last change caused significant lag. Reverting in 3... 2... 1...
> [ CANCEL REVERT ]

---

## 4. Technical Stack
- **Library**: `LibGui` or `OwoLib` (Fabric standards) preferred for complex widgets.
- **Fallback**: Native `DrawContext` for zero dependencies if needed (simpler look).
- **Rendering**: 
    - Use `MatrixStack` for all positioning.
    - Textures: Use vanilla textures tinted dark for backgrounds to save file size.

## 5. Implementation Stages
1.  **Skeleton**: Just the boxes and navigation logic.
2.  **Widgets**: Custom sliders and toggles implementation.
3.  **Binding**: Connecting widgets to `NozhConfig`.
4.  **Polish**: Animations and sounds.
