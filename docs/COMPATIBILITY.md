# 🤝 Mod Compatibility Registry (Stewardship)

NOZH is aware of the following mods. It will automatically adjust its behavior to prevent conflicts if these are detected.

## Rendering Mods (High Priority)

| Mod | NOZH Action |
|:---|:---|
| **Sodium / Embeddium** | Disables NOZH Chunk Optimizations. Delegates Render Distance handling. |
| **Iris / Oculus** | Disables Cloud & Shadow optimizations (Shaders behave unexpectedly with disabled clouds). |
| **Canvas** | Full delegation. |
| **Nvidium** | Detected. NOZH respects Nvidium's frustum culling. |
| **VulkanMod** | Detected. Critical rendering hooks are disabled. |

## Engine Optimizers

| Mod | NOZH Action |
|:---|:---|
| **C2ME (Concurrent Chunk Management Engine)** | NOZH relaxes chunk priority rules to allow C2ME to handle threading. |
| **Lithium** | Fully Compatible. |
| **Phosphor / Starlight** | Fully Compatible. |
| **FerriteCore** | Fully Compatible. |
| **ModernFix** | Fully Compatible. |
| **Krypton** | Fully Compatible. |
| **ImmediatelyFast** | Detected. Animation culling is coordinated. |

## Utility Mods

| Mod | NOZH Action |
|:---|:---|
| **ModMenu** | NOZH injects its configuration button into the mod list. |
| **Cloth Config** | Not used by NOZH (we use our own engine), but compatible if installed. |
| **Dynamic FPS** | NOZH detects this and won't fight for FPS limits when the window is unfocused. |

## Known Incompatibilities

* NONE currently known for v2.0.0.

> **Note**: This list is updated live via the Cloud Config system (`compatibility.json`) on game launch.
