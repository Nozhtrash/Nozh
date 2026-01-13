# Changelog

All notable changes to NOZH will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] - v0.6.0-alpha (Cloud & Community)

### Added

- **Cloud Infrastructure**: `CloudManager` for coordinating async network operations.
- **Hardware Database**: `HardwareBenchmarker` for anonymous system profiling (CPU score, RAM, GPU).
- **Mod Compatibility Cloud**: `RemoteConfigFetcher` for hot-reloading compatibility rules from GitHub without mod updates.
- **Performance Leaderboards**: `LeaderboardCollector` for tracking personal best FPS gains and session history (stored locally).
- **Privacy First**: All cloud features are opt-in or strictly anonymous (no PII collected).

## [Unreleased] - v0.5.0-alpha (Server-Aware Optimization)

### 🧠 Added - Intelligence Layer

- **Bayesian Confidence Calculator** - Smart confidence scoring with:
  - Prediction accuracy-based updates
  - Scenario-specific modifiers (combat: 0.85, idle: 1.1)
  - Success streak bonuses (up to +15%)
  - Gradient anti-flapping (smooth 0.3→1.0 recovery)
  - Adaptive decay based on action history

- **Enhanced Performance Predictor**:
  - Dual EMA system (fast α=0.4, slow α=0.1) for trend detection
  - EMA crossover signal for degradation detection
  - Micro-stutter tracking (30%+ frame increases)
  - Rolling variance-based stability scoring
  - Comprehensive `EnhancedPrediction` record combining all signals

- **Anti-False-Positive System** (`ActionValidator`):
  - Statistical significance testing using Cohen's d
  - Sustained improvement validation (3+ consecutive samples)
  - Related action cooldowns (e.g., particles + clouds share cooldown)
  - Improvement consistency tracking per capability

- **Math Utilities**:
  - `ExponentialMovingAverage` - Zero-allocation EMA with half-life factory
  - `RollingVariance` - Welford's algorithm for O(1) memory variance

### 📈 Changed - Performance Improvements

- **SessionLearning** - Memory-efficient with:
  - Maximum 500 history entries
  - Automatic compaction on size limit
  - Stale entry cleanup (24h threshold)
  - Value-based entry prioritization

- **SystemMonitor** - Enhanced bottleneck detection:
  - New `MEMORY` bound type for high memory pressure
  - Chunk loading awareness in scoring
  - Granular entity count thresholds (150/300/500)
  - `BottleneckReport` record for detailed analysis

- **AdaptiveVisualQualityController** - Gradual recovery:
  - Asymmetric hysteresis (1.0x down, 1.5x up thresholds)
  - Separate timing intervals (10s down, 30s up)
  - 60-second stability window for quality recovery
  - Different streak requirements (2 down, 4 up)

### 🔧 Fixed

- Replaced all `catch (Throwable)` with specific exception types
- Fixed duplicate log message in NozhPriority2Client
- Updated deprecated API documentation in CapabilityProviderRegistry
- Removed duplicate CrashLoopGuard.recordFailureContext call
- Fixed profile serialization/deserialization in SmartProfileManager
- Stabilized Loom version from SNAPSHOT to 1.6

---

## [0.2.0-alpha] - Chaos Testing & CI

### Added

- Chaos test CI job with JSON/CSV reporting and report metadata exports.
- New chaos stress scenarios (entity/chunk/shader) and benchmark scenario artifact recording.
- Automated modpack quick-test runner and quick-test documentation.
- Telemetry metrics checklist documentation and architecture freeze policy documentation.
- First-run tutorial flow with expanded localization coverage.

### Changed

- Strengthened chaos test malicious thresholds and CI reporting behavior.

---

## [0.1.0] - "The Foundation" (Golden Master)

**Release Type**: Initial Release  
**Stability**: Production Ready

### Key Features

- **Safe Architecture**: Mod is split into Profiler, Governor, and Executor for safety.
- **Honest Profiler**: Measures Frametime P95 and Average accurately. Reports `UNKNOWN` if data is insufficient.
- **Safe Mode ("The Bouncer")**: Automatically locks the mod if a crash loop is detected.
- **Automated Rollback**: If a change doesn't improve performance within 45 seconds, it is undone.
- **Diagnostics**: `/nozh selfcheck` command for health auditing.
- **Compat**: Detects Sodium, Iris, etc. and logs compatibility hints.
- **Localization**: English (US) and Spanish (CL) support.

### Non-Goals (v0.1.0)

- NOZH does NOT increase FPS artificially.
- NOZH does NOT override user preferences without consent.
- NOZH does NOT change render distance, simulation distance, or entities in this version.
- NOZH does NOT guess when data is insufficient.

### Stability Guarantees

- NOZH guarantees reversibility for every automatic action performed in v0.1.0.

### Technical Details

- Zero-allocation hot paths (no garbage generation in `onFrame`).
- Strict config validation and safe clamping.
- Action Cooldowns to prevent "fighting" with the user.

---

## Expected Performance Impact

| Scenario | Expected FPS Gain | Confidence |
|----------|-------------------|------------|
| Lobby/Hub (many players) | +30-50% | High |
| Farms (entities) | +40-60% | High |
| Combat (PvP/PvE) | +20-35% | Medium |
| Exploration (chunk loading) | +15-25% | Medium |
| Building (static) | +10-20% | Low (already efficient) |
| AFK Mode | +100% (capped to 30 FPS) | High |

**Note**: Actual gains depend heavily on hardware, modpack, and base FPS.
Low-end systems typically see larger percentage improvements.
