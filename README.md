<div align="center">
  <h1>⚡ NOZH ⚡</h1>
  <p>
    <b>Intelligent Performance Orchestrator for Minecraft (Fabric)</b><br>
    <i>Orquestador Inteligente de Rendimiento para Minecraft (Fabric)</i>
  </p>

  <p>
    <a href="https://github.com/NozhMod/Nozh/actions"><img src="https://img.shields.io/badge/Status-ALPHA%200.2.0-ff8c00?style=for-the-badge&logo=appveyor" alt="Status"></a>
    <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-FABRIC-00b8a3?style=for-the-badge&logo=fabric" alt="Fabric"></a>
    <a href="#"><img src="https://img.shields.io/badge/Minecraft-1.20.1-FFA500?style=for-the-badge&logo=minecraft" alt="Version"></a>
    <a href="#"><img src="https://img.shields.io/badge/Arch-Event%20Driven-00c853?style=for-the-badge" alt="Mode"></a>
  </p>
</div>

---

# 🇬🇧 English | 🇪🇸 Español

> **Quick Jump:** [English](#english) | [Español](#español)

---

<a name="english"></a>

# 🇬🇧 English

## What is NOZH?

**NOZH** (Now Only Zen HUD) is an **intelligent, event-driven performance orchestrator** for Minecraft that automatically optimizes your game settings in real-time. Unlike traditional optimization mods that use static configurations, NOZH implements a sophisticated **3-layer architecture** with machine learning capabilities to adapt to your specific hardware and gameplay scenarios.

### 🎯 Core Philosophy

- **Adaptive, Not Static**: NOZH learns from your hardware and adjusts settings dynamically
- **Transparent**: Every action is logged and explained - you always know what changed and why
- **Safe**: Automatic rollback if changes worsen performance
- **Intelligent**: Context-aware decisions based on what you're doing in-game (combat, building, exploring, AFK)

---

## ✨ Key Features

### 🧠 Intelligent Decision Making

- **Scenario Detection**: Automatically detects what you're doing (Combat, Building, Exploring, AFK)
- **Performance Prediction**: ML-based predictor anticipates FPS drops before they happen
- **Learning Engine**: Tracks action effectiveness on YOUR hardware and adapts
- **Context-Aware Actions**: Different optimizations for different scenarios

### 🎮 19 Active Optimization Providers

NOZH can control and optimize:

- **Render Distance** (±10-20 FPS)
- **Simulation Distance** (±5-10 FPS)
- **Particles** (ALL/DECREASED/MINIMAL)
- **Entity Shadows** (±3-5 FPS)
- **Clouds** (Fancy/Fast/Off)
- **Biome Blend Radius** (±2-4 FPS)
- **Entity Distance** (±3-5 FPS)
- **Mipmap Levels** (±1-3 FPS)
- **VSync**, **Graphics Mode**, **Smooth Lighting**
- **Fog**, **Distortion Effects**, **Dynamic Lighting**
- **Plus**: Armor Stands, Item Frames, Block Entities, Animations

### 🛡️ Safety & Reliability

- **STRONG Rollback Guarantee**: Every change can be reverted automatically
- **Crash Loop Guard**: Prevents infinite crash loops from bad configurations
- **Safe Mode**: Activates automatically when compatibility risks detected
- **Isolation**: One broken provider cannot crash the entire system
- **Conservative Defaults**: Minimal changes per session (max 2 by default)

### 📊 Professional Telemetry

- **Ring Buffer**: Efficient circular buffer for temporal analysis
- **P95/P99 Tracking**: Identifies frametime anomalies accurately
- **Spike Detection**: Catches performance issues early
- **No Synthetic Data**: Returns null if measurements are invalid
- **Export Support**: Export telemetry data for analysis

### 🎛️ Full Command Suite

```
/nozh status         - System overview
/nozh selfcheck      - Comprehensive diagnostic report
/nozh perf           - Real-time performance metrics
/nozh history        - Action history with reasons
/nozh learning       - Learning statistics & effectiveness
/nozh scenario       - Current scenario detection
/nozh config <key>   - View configuration values
/nozh debug telemetry  - Detailed telemetry info
/nozh debug predictor  - Predictor state & analysis
/nozh debug weights    - Learning weights & Q-values
/nozh telemetry export - Export data to CSV
```

---

## 🏗️ Architecture (v0.2.0-alpha)

### Three-Layer Design

```
┌─────────────────────────────────────────────────┐
│         Integration Layer (Fabric)              │
│  • ProviderBootstrap (19 providers)             │
│  • FabricScenarioDetector                       │
│  • MinecraftOptionsAdapter                      │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│           Bus Layer (Event-Driven)              │
│  • ActionBus                                    │
│  • StandardActionProcessor                      │
│  • Event Publishing & Subscription              │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│         Core Layer (Business Logic)             │
│  • GovernorRunner (Orchestration)               │
│  • ProviderRegistry (19 providers)              │
│  • TelemetryManager                             │
│  • PerformancePredictor                         │
│  • ActionMatrix & EffectivenessTracker          │
└─────────────────────────────────────────────────┘
```

### How It Works

1. **Detection**: Monitors FPS, frametime, scenario, entity count, CPU/memory load
2. **Analysis**: Predictor identifies performance issues and risks
3. **Decision**: GovernorRunner selects best action based on scenario + learned effectiveness
4. **Execution**: ActionBus → Processor → Provider (with rollback support)
5. **Learning**: Measures impact and updates action weights for future decisions

---

## 📦 Installation

### Requirements

- Minecraft 1.20.1
- Fabric Loader 0.14.0+
- Fabric API 0.83.0+

### Steps

1. Download NOZH from [Releases](https://github.com/NozhMod/Nozh/releases)
2. Place `.jar` in `.minecraft/mods/`
3. Launch Minecraft with Fabric
4. Configure via ModMenu or `/nozh` commands

---

## ⚙️ Configuration

Edit `config/nozh/nozh.json5` or use commands:

### Key Settings

```json5
{
  // Performance
  "targetFps": 60,              // Your target FPS (30-240)
  "optimizationProfile": "BALANCED", // BALANCED or AGGRESSIVE
  
  // Safety
  "allowAutoTuning": false,     // Auto-apply without confirmation
  "rollbackEnabled": true,      // Auto-rollback if performance worsens
  "maxChangesPerSession": 2,    // Max auto changes per session
  
  // Advanced
  "allowGameplayImpactActions": false, // Enable high-impact optimizations
  "hybridModelEnabled": true,          // Use hybrid decision logic
  "debugLogs": false                   // Verbose logging
}
```

---

## 🚀 Roadmap

### ✅ Completed (v0.2.0-alpha)

- [x] 19 capability providers with STRONG rollback
- [x] Event-driven architecture (ActionBus)
- [x] ML-based performance predictor
- [x] Scenario detection (Combat, Building, Exploring, AFK)
- [x] Effectiveness tracking & learning
- [x] Complete command suite
- [x] Crash loop protection
- [x] Telemetry export
- [x] ModMenu integration

### 🚧 In Progress (v0.2.1)

- [ ] Enhanced shader detection (Iris/Optifine)
- [ ] Entity count tracking from world
- [ ] Live config updates via commands
- [ ] CSV telemetry export format
- [ ] Spanish translation completion

### 🔮 Planned (v0.3.0+)

- [ ] GPU vs CPU bound detection
- [ ] Mod conflict detection & orchestration
- [ ] Performance profiles (Low-End, Mid-Range, High-End)
- [ ] Cloud-based effectiveness sharing (opt-in)
- [ ] Advanced HUD customization
- [ ] Multi-language support (English, Spanish, more)

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📜 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Fabric team for the amazing modding framework
- The Minecraft modding community
- All contributors and testers

---

<a name="español"></a>

# 🇪🇸 Español

## ¿Qué es NOZH?

**NOZH** (Now Only Zen HUD) es un **orquestador inteligente de rendimiento** para Minecraft que optimiza automáticamente la configuración de tu juego en tiempo real. A diferencia de mods de optimización tradicionales que usan configuraciones estáticas, NOZH implementa una sofisticada **arquitectura de 3 capas** con capacidades de aprendizaje automático para adaptarse a tu hardware específico y escenarios de juego.

### 🎯 Filosofía Central

- **Adaptativo, No Estático**: NOZH aprende de tu hardware y ajusta configuraciones dinámicamente
- **Transparente**: Cada acción se registra y explica - siempre sabes qué cambió y por qué
- **Seguro**: Rollback automático si los cambios empeoran el rendimiento
- **Inteligente**: Decisiones conscientes del contexto según lo que haces en el juego

---

## ✨ Características Clave

### 🧠 Toma de Decisiones Inteligente

- **Detección de Escenarios**: Detecta automáticamente lo que estás haciendo (Combate, Construcción, Exploración, AFK)
- **Predicción de Rendimiento**: Predictor basado en ML anticipa caídas de FPS antes de que ocurran
- **Motor de Aprendizaje**: Rastrea efectividad de acciones en TU hardware y se adapta
- **Acciones Contextuales**: Diferentes optimizaciones para diferentes escenarios

### 🎮 19 Proveedores de Optimización Activos

NOZH puede controlar y optimizar:

- **Distancia de Renderizado** (±10-20 FPS)
- **Distancia de Simulación** (±5-10 FPS)
- **Partículas** (TODAS/REDUCIDAS/MÍNIMAS)
- **Sombras de Entidades** (±3-5 FPS)
- **Nubes** (Detalladas/Rápidas/Desactivadas)
- **Radio de Mezcla de Biomas** (±2-4 FPS)
- **Distancia de Entidades** (±3-5 FPS)
- **Niveles Mipmap** (±1-3 FPS)
- **VSync**, **Modo Gráficos**, **Iluminación Suave**
- **Niebla**, **Efectos de Distorsión**, **Iluminación Dinámica**
- **Más**: Soportes de Armadura, Marcos de Objetos, Entidades de Bloque, Animaciones

### 🛡️ Seguridad y Confiabilidad

- **Garantía de Rollback FUERTE**: Cada cambio puede revertirse automáticamente
- **Protección de Bucle de Crash**: Previene bucles infinitos de crash por configuraciones malas
- **Modo Seguro**: Se activa automáticamente cuando se detectan riesgos de compatibilidad
- **Aislamiento**: Un proveedor roto no puede crashear todo el sistema
- **Predeterminados Conservadores**: Cambios mínimos por sesión (máx 2 por defecto)

### 📊 Telemetría Profesional

- **Ring Buffer**: Buffer circular eficiente para análisis temporal
- **Seguimiento P95/P99**: Identifica anomalías de frametime con precisión
- **Detección de Picos**: Detecta problemas de rendimiento temprano
- **Sin Datos Sintéticos**: Retorna null si las mediciones son inválidas
- **Soporte de Exportación**: Exporta datos de telemetría para análisis

### 🎛️ Suite Completa de Comandos

```
/nozh status         - Vista general del sistema
/nozh selfcheck      - Reporte de diagnóstico completo
/nozh perf           - Métricas de rendimiento en tiempo real
/nozh history        - Historial de acciones con razones
/nozh learning       - Estadísticas de aprendizaje y efectividad
/nozh scenario       - Detección de escenario actual
/nozh config <clave> - Ver valores de configuración
/nozh debug telemetry  - Info detallada de telemetría
/nozh debug predictor  - Estado y análisis del predictor
/nozh debug weights    - Pesos de aprendizaje y valores Q
/nozh telemetry export - Exportar datos a CSV
```

---

## 🏗️ Arquitectura (v0.2.0-alpha)

### Diseño de Tres Capas

```
┌─────────────────────────────────────────────────┐
│      Capa de Integración (Fabric)               │
│  • ProviderBootstrap (19 proveedores)           │
│  • FabricScenarioDetector                       │
│  • MinecraftOptionsAdapter                      │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│        Capa de Bus (Eventos)                    │
│  • ActionBus                                    │
│  • StandardActionProcessor                      │
│  • Publicación y Suscripción de Eventos         │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│      Capa Core (Lógica de Negocio)             │
│  • GovernorRunner (Orquestación)                │
│  • ProviderRegistry (19 proveedores)            │
│  • TelemetryManager                             │
│  • PerformancePredictor                         │
│  • ActionMatrix y EffectivenessTracker          │
└─────────────────────────────────────────────────┘
```

### Cómo Funciona

1. **Detección**: Monitorea FPS, frametime, escenario, conteo de entidades, carga CPU/memoria
2. **Análisis**: El predictor identifica problemas de rendimiento y riesgos
3. **Decisión**: GovernorRunner selecciona mejor acción basada en escenario + efectividad aprendida
4. **Ejecución**: ActionBus → Processor → Provider (con soporte de rollback)
5. **Aprendizaje**: Mide impacto y actualiza pesos de acción para decisiones futuras

---

## 📦 Instalación

### Requisitos

- Minecraft 1.20.1
- Fabric Loader 0.14.0+
- Fabric API 0.83.0+

### Pasos

1. Descarga NOZH de [Releases](https://github.com/NozhMod/Nozh/releases)
2. Coloca el `.jar` en `.minecraft/mods/`
3. Inicia Minecraft con Fabric
4. Configura vía ModMenu o comandos `/nozh`

---

## ⚙️ Configuración

Edita `config/nozh/nozh.json5` o usa comandos:

### Configuraciones Clave

```json5
{
  // Rendimiento
  "targetFps": 60,              // Tu FPS objetivo (30-240)
  "optimizationProfile": "BALANCED", // BALANCED o AGGRESSIVE
  
  // Seguridad
  "allowAutoTuning": false,     // Auto-aplicar sin confirmación
  "rollbackEnabled": true,      // Rollback automático si empeora rendimiento
  "maxChangesPerSession": 2,    // Máx cambios automáticos por sesión
  
  // Avanzado
  "allowGameplayImpactActions": false, // Habilitar optimizaciones de alto impacto
  "hybridModelEnabled": true,          // Usar lógica de decisión híbrida
  "debugLogs": false                   // Logging detallado
}
```

---

## 🚀 Hoja de Ruta

### ✅ Completado (v0.2.0-alpha)

- [x] 19 proveedores de capacidad con rollback FUERTE
- [x] Arquitectura basada en eventos (ActionBus)
- [x] Predictor de rendimiento basado en ML
- [x] Detección de escenarios (Combate, Construcción, Exploración, AFK)
- [x] Seguimiento de efectividad y aprendizaje
- [x] Suite completa de comandos
- [x] Protección de bucle de crash
- [x] Exportación de telemetría
- [x] Integración con ModMenu

### 🚧 En Progreso (v0.2.1)

- [ ] Detección mejorada de shaders (Iris/Optifine)
- [ ] Seguimiento de conteo de entidades del mundo
- [ ] Actualizaciones de config en vivo vía comandos
- [ ] Formato de exportación CSV de telemetría
- [ ] Completar traducción al español

### 🔮 Planeado (v0.3.0+)

- [ ] Detección de limitación GPU vs CPU
- [ ] Detección y orquestación de conflictos de mods
- [ ] Perfiles de rendimiento (Low-End, Mid-Range, High-End)
- [ ] Compartición de efectividad basada en nube (opt-in)
- [ ] Personalización avanzada del HUD
- [ ] Soporte multiidioma (Inglés, Español, más)

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Por favor:

1. Haz fork del repositorio
2. Crea una rama de característica (`git checkout -b feature/caracteristica-asombrosa`)
3. Commit de cambios (`git commit -m 'Añadir característica asombrosa'`)
4. Push a la rama (`git push origin feature/caracteristica-asombrosa`)
5. Abre un Pull Request

---

## 📜 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver archivo [LICENSE](LICENSE) para detalles.

---

## 🙏 Agradecimientos

- Equipo de Fabric por el increíble framework de modding
- La comunidad de modding de Minecraft
- Todos los contribuidores y testers

---

<div align="center">
  <p>Made with ❤️ by the NOZH Team</p>
  <p>
    <a href="https://github.com/NozhMod/Nozh/issues">Report Bug</a> •
    <a href="https://github.com/NozhMod/Nozh/issues">Request Feature</a> •
    <a href="https://discord.gg/nozh">Join Discord</a>
  </p>
</div>
