<div align="center">
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>Intelligent Performance Orchestrator for Minecraft (Fabric)</b><br>
    <i>Orquestador inteligente de rendimiento para Minecraft (Fabric)</i>
  </p>

  <p>
    <a href="https://github.com/NozhMod/Nozh/actions"><img src="https://img.shields.io/badge/Status-ALPHA%200.2.0-ff8c00?style=for-the-badge&logo=appveyor" alt="Status"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Minecraft-1.20.1-FFA500?style=for-the-badge&logo=minecraft" alt="Version"></a>
    <a href="#"><img src="https://img.shields.io/badge/Arch-Event%20Driven-00c853?style=for-the-badge" alt="Mode"></a>
  </p>
</div>

---

# 🇬🇧 English

## What is NOZH?

**NOZH** is an **event-driven adaptive performance orchestrator** for Minecraft. Instead of using static configuration, it implements a **3-layer architecture**:

1. **Core Layer** - Pure business logic (capabilities, telemetry, orchestration)
2. **Bus Layer** - Event-driven communication between components
3. **Integration Layer** - Fabric/Minecraft bindings and mod adapters

## Architecture Overview (v0.2.0-alpha)

### Core Components

#### Capability System
- **CapabilityProvider Interface** - Defines how settings are read/modified safely
- **CapabilityRegistry** - Central registry for all capability providers
- **Isolation Guarantee** - One broken provider cannot crash the entire system
- **Rollback Support** - Failed changes can be reverted via state snapshots

#### Orchestration
- **IntegratedGovernor** - Main decision-making engine
- **Scenario Detection** - Context awareness (combat, building, exploring, AFK)
- **ActionMatrix** - Available actions per scenario with priorities
- **EffectivenessTracker** - Learns which actions work on your hardware

#### Telemetry
- **TelemetryCollector** - Gathers FPS, frametime, entity count, chunk loading
- **RingBuffer** - Efficient circular buffer for temporal analysis
- **Spike Detection** - Identifies P95/P99 frametime anomalies
- **No Synthetic Data** - Returns null if measurement is invalid

#### Analysis
- **BottleneckDetector** - CPU vs GPU bottleneck identification
- **HostileEntityTracker** - Combat detection based on nearby threats
- **ActionWindowAnalyzer** - Temporal pattern analysis (30-second windows)

### Bus Layer (Event System)

```
┌─────────────────────────────────────────┐
│         EVENT BUS (Central)             │
│  - CapabilityChangeEvent                │
│  - TelemetrySampleEvent                 │
│  - GovernorDecisionEvent                │
│  - ScenarioTransitionEvent              │
└─────────────────────────────────────────┘
         ▲              │
         │              ▼
┌────────┴───────┐  ┌───────────────┐
│   Publishers   │  │  Subscribers  │
│  - Governor    │  │  - HUD        │
│  - Telemetry   │  │  - Logger     │
│  - Providers   │  │  - Analytics  │
└────────────────┘  └───────────────┘
```

### Integration Layer

- **FabricNozhClient** - Mod initialization and lifecycle
- **Mixins** - Targeted Minecraft hooks for action tracking
- **Mod Adapters** - Compatibility with Sodium, Iris, LambDynamicLights, etc.

## Current State (January 2026)

### ✅ Fully Implemented

#### Core Architecture
- [x] CapabilityProvider interface with isolation guarantees
- [x] CapabilityRegistry with safe execution and rollback
- [x] StateSnapshot system for reversible changes
- [x] ActionResult type system (Success/Failed/Invalid/NoChange)
- [x] Event bus infrastructure
- [x] Telemetry collection with validation (no synthetic data)

#### Orchestration
- [x] IntegratedGovernor decision loop
- [x] Scenario detection (Combat, Building, Exploring, AFK, Loading)
- [x] EffectivenessTracker with TTL and decay
- [x] Blacklist system for failed actions
- [x] Session learning persistence

#### Analysis
- [x] BottleneckDetector (CPU vs GPU identification)
- [x] HostileEntityTracker (combat context)
- [x] ActionWindowAnalyzer (temporal patterns)
- [x] Spike detection (P95/P99 frametime)

#### Commands
- [x] `/nozh status` - System overview
- [x] `/nozh selfcheck` - Diagnostic report
- [x] `/nozh perf` - Performance metrics
- [x] `/nozh history` - Action history
- [x] `/nozh learning` - Learning statistics
- [x] `/nozh telemetry export` - Data export

### 🚧 In Progress

#### Capability Providers (0/10 implemented)
- [ ] RenderDistanceProvider
- [ ] SimulationDistanceProvider
- [ ] ParticlesProvider
- [ ] EntityDistanceProvider
- [ ] GraphicsModeProvider
- [ ] MipmapLevelsProvider
- [ ] SmoothLightingProvider
- [ ] CloudsProvider
- [ ] VsyncProvider
- [ ] MaxFpsProvider

> **Note**: Previous implementations were incompatible with v2 interface and removed in PR #149.
> New implementations following the CapabilityProvider v2 contract are in development.

#### Advanced Features
- [ ] TransactionalExecutor with automatic rollback
- [ ] ImprovedQLearning with epsilon-greedy exploration
- [ ] Director V2 orchestration layer
- [ ] Enhanced mod adapter coverage

### 📋 Planned

- **Predictive Analysis** - Pre-emptive optimization before spikes
- **ML-Driven Decisions** - More sophisticated action selection
- **Dimension Context** - Per-dimension learning profiles
- **Multiplayer Optimizations** - Server-aware adjustments
- **Advanced Shader Orchestration** - Deeper Iris/Optifine coordination

## Why Use NOZH Now?

### Advantages
- **Context-Aware** - Adapts to what you're doing (combat needs different settings than building)
- **Hardware Learning** - Remembers what works on YOUR specific setup
- **Safe Orchestration** - Coordinates with other performance mods instead of fighting them
- **Full Visibility** - HUD shows you exactly what's being adjusted and why
- **Reversible Changes** - Failed optimizations are automatically rolled back

### Current Limitations
- **Alpha Software** - Expect bugs and incomplete features
- **Provider System Incomplete** - Most capability providers are not yet implemented
- **Mod Compatibility** - Some reflection-based adapters may break with mod updates
- **Performance Gains Not Guaranteed** - Effectiveness depends on your specific hardware/mods

## Installation

1. Download `nozh-x.x.x.jar` from releases
2. Place in your `.minecraft/mods` folder
3. Launch Minecraft 1.20.1 with Fabric Loader
4. Run `/nozh selfcheck` to verify installation

## Configuration

Default config is tuned for 60 Hz monitors. Adjust in `config/nozh.json5`:

```json5
{
  targetFps: 60,              // Set to your monitor refresh rate
  observationWindowSeconds: 5, // Telemetry collection window
  cooldownActionMillis: 120000, // Per-action cooldown (2 minutes)
  cooldownGlobalMinIntervalMillis: 60000, // Global minimum (1 minute)
  mode: "AUTO"                // AUTO or ASSISTED
}
```

## Development Progress

| Component | Completion | Status |
|-----------|------------|--------|
| Core Architecture | 85% | ✅ Stable |
| Telemetry System | 80% | ✅ Functional |
| Scenario Detection | 70% | ✅ Working |
| Capability Providers | 0% | 🚧 In Development |
| Mod Compatibility | 40% | 🚧 Partial |
| Advanced Features | 15% | 📋 Planned |
| **Overall** | **~48%** | 🟡 Alpha |

---

# 🇪🇸 Español

## ¿Qué es NOZH?

**NOZH** es un **orquestador adaptativo de rendimiento basado en eventos** para Minecraft. En lugar de usar configuración estática, implementa una **arquitectura de 3 capas**:

1. **Capa Core** - Lógica de negocio pura (capacidades, telemetría, orquestación)
2. **Capa Bus** - Comunicación por eventos entre componentes
3. **Capa Integration** - Bindings con Fabric/Minecraft y adaptadores de mods

## Estado Actual (Enero 2026)

### ✅ Completamente Implementado

- Arquitectura core con sistema de eventos
- Detección de escenarios contextual
- Sistema de telemetría validado
- Detección de cuellos de botella (CPU vs GPU)
- Análisis de patrones temporales
- Comandos de diagnóstico y exportación

### 🚧 En Progreso

- **Providers de capacidades (0/10)** - Implementaciones anteriores incompatibles con interfaz v2 fueron eliminadas
- Sistema de aprendizaje Q-Learning mejorado
- Executor transaccional con rollback automático
- Mayor cobertura de adaptadores de mods

### 📋 Planeado

- Análisis predictivo
- Decisiones basadas en ML
- Contexto por dimensión
- Optimizaciones para multijugador
- Orquestación avanzada de shaders

## Por Qué Usar NOZH Ahora

### Ventajas
- **Consciente del Contexto** - Se adapta a lo que estás haciendo
- **Aprende de tu Hardware** - Recuerda qué funciona en TU setup específico
- **Orquestación Segura** - Se coordina con otros mods de rendimiento
- **Visibilidad Completa** - HUD te muestra qué se ajusta y por qué
- **Cambios Reversibles** - Optimizaciones fallidas se revierten automáticamente

### Limitaciones Actuales
- **Software Alpha** - Espera bugs y funciones incompletas
- **Sistema de Providers Incompleto** - La mayoría de providers aún no están implementados
- **Compatibilidad de Mods** - Algunos adapters basados en reflection pueden romperse
- **Mejoras No Garantizadas** - Depende de tu hardware/mods específicos

## Instalación

1. Descarga `nozh-x.x.x.jar` desde releases
2. Coloca en tu carpeta `.minecraft/mods`
3. Inicia Minecraft 1.20.1 con Fabric Loader
4. Ejecuta `/nozh selfcheck` para verificar instalación

## Configuración

La config por defecto está ajustada para monitores de 60 Hz. Modifica en `config/nozh.json5`:

```json5
{
  targetFps: 60,              // Ajusta a la tasa de refresco de tu monitor
  observationWindowSeconds: 5, // Ventana de recolección de telemetría
  cooldownActionMillis: 120000, // Cooldown por acción (2 minutos)
  cooldownGlobalMinIntervalMillis: 60000, // Mínimo global (1 minuto)
  mode: "AUTO"                // AUTO o ASSISTED
}
```

## Progreso de Desarrollo

| Componente | Completitud | Estado |
|-----------|------------|--------|
| Arquitectura Core | 85% | ✅ Estable |
| Sistema de Telemetría | 80% | ✅ Funcional |
| Detección de Escenarios | 70% | ✅ Operativo |
| Providers de Capacidades | 0% | 🚧 En Desarrollo |
| Compatibilidad Mods | 40% | 🚧 Parcial |
| Features Avanzadas | 15% | 📋 Planeadas |
| **Total** | **~48%** | 🟡 Alpha |

---

## Contributing

We welcome contributions! Please:

1. Read `docs/v0.2-alpha.md` for architecture overview
2. Check `CONTRACTS.md` for interface contracts
3. Follow the 3-layer separation (core/bus/integration)
4. Add tests for new capability providers
5. Update documentation for new features

## License

MIT License - See LICENSE file for details

---

<div align="center">
  <p><i>Context-aware optimization beats static tweaks.</i></p>
  <p><b>v0.2.0-alpha</b> • Built with 🧠 for adaptive performance</p>
</div>