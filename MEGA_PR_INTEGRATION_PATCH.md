# MEGA PR INTEGRATION PATCH

## Overview
This document details the critical integration changes needed in `IntegratedGovernor.java` to use the new provider system.

## Key Integration Points

### 1. Initialize CapabilityProviderRegistry

In the constructor, add:

```java
// Initialize provider registry (CRITICAL)
CapabilityProviderRegistry.initialize();
NozhConstants.LOGGER.info("Capability providers initialized: " + 
                          CapabilityProviderRegistry.getRegisteredActions().length);
```

### 2. Replace Stub Action Execution

Replace this code in `executeAction()`:

```java
// OLD (STUB):
boolean executionSuccess = true; // TODO: Execute actual provider action

// NEW (REAL):
ActionResult providerResult = CapabilityProviderRegistry.execute(
    actionId,
    client,
    calculateTargetValue(actionId, state, fpsBefore)
);

boolean executionSuccess = providerResult.isSuccess();

// Store snapshot for rollback
if (providerResult.canRollback()) {
    actionSnapshots.put(actionId, providerResult.getSnapshot());
}

if (!executionSuccess) {
    NozhConstants.LOGGER.warn("Provider execution failed: {}" , providerResult.getError());
    // Record failure immediately
    if (effectivenessTracker != null) {
        effectivenessTracker.recordActionResult(actionId, 0.0, false);
    }
    return;
}
```

### 3. Add calculateTargetValue Helper

```java
private Object calculateTargetValue(String actionId, PerformanceLearningEngine.GameState state, double currentFps) {
    // Map actions to appropriate target values based on scenario and FPS
    
    switch (actionId) {
        case "reduce_render_distance":
            // More aggressive reduction for lower FPS
            if (currentFps < 30) return 6;
            if (currentFps < 45) return 10;
            return 14;
            
        case "lower_particles":
            return ParticlesMode.DECREASED;
            
        case "disable_clouds":
            return CloudRenderMode.OFF;
            
        case "reduce_entity_distance":
            if (currentFps < 30) return 0.5;
            return 0.75;
            
        case "lower_graphics":
            return GraphicsMode.FAST;
            
        default:
            NozhConstants.LOGGER.warn("Unknown action for target calculation: " + actionId);
            return null;
    }
}
```

### 4. Replace Telemetry Collection

Replace `collectTelemetry()` method:

```java
private final EnhancedTelemetryCollector telemetryCollector = new EnhancedTelemetryCollector();

private TelemetrySample collectTelemetry() {
    // Use enhanced collector - returns null if invalid
    return telemetryCollector.collectValidated(client);
}
```

### 5. Integrate TransactionalExecutor

Update `executeAction()` to use transactional execution:

```java
// Instead of simple execution, use transactional wrapper
transactionalExecutor.executeWithRollback(
    actionId,
    () -> CapabilityProviderRegistry.execute(actionId, client, targetValue),
    Duration.ofSeconds(2) // Stabilization period
).thenAccept(txResult -> {
    if (txResult.wasRolledBack()) {
        NozhConstants.LOGGER.warn("Action {} rolled back: {}", actionId, txResult.getMessage());
        effectivenessTracker.recordActionResult(actionId, 0.0, false);
    } else if (txResult.isSuccess()) {
        // Success - continue with learning
        measureAndLearnFromAction(...);
    }
});
```

### 6. Add Action Window Analyzer

In constructor:

```java
private final ActionWindowAnalyzer actionAnalyzer = new ActionWindowAnalyzer();
```

In tick(), add action recording via mixins:

```java
// Note: This requires mixin hooks - see MIXIN_INTEGRATION.md
```

### 7. Add Hostile Entity Tracking

In constructor:

```java
private final HostileEntityTracker hostileTracker = new HostileEntityTracker();
```

In `detectScenario()`:

```java
private Scenario detectScenario() {
    if (scenarioDetector == null) {
        return Scenario.STANDARD;
    }
    
    try {
        // Get action analysis
        ScenarioAnalysis actionAnalysis = actionAnalyzer.analyze();
        
        // Get hostile context
        HostileContext hostileContext = hostileTracker.analyze(client);
        
        // Priority 1: Combat (hostile entities targeting player)
        if (hostileContext.isActiveCombat() || hostileContext.getDangerScore() > 3.0) {
            return Scenario.COMBAT;
        }
        
        // Priority 2: Building
        if (actionAnalysis.isBuilding()) {
            return Scenario.BUILDING;
        }
        
        // Priority 3: AFK
        if (actionAnalysis.getTotalActions() < 5 && cameraTracker.getIdleTimeMs() > 30000) {
            return Scenario.AFK;
        }
        
        // Priority 4: Exploring
        if (actionAnalysis.isExploring()) {
            return Scenario.EXPLORING;
        }
        
        // Priority 5: Organizing
        if (actionAnalysis.isOrganizing()) {
            return Scenario.ORGANIZING;
        }
        
        // Default
        return Scenario.STANDARD;
        
    } catch (Exception e) {
        NozhConstants.LOGGER.error("Enhanced scenario detection failed", e);
        return Scenario.STANDARD;
    }
}
```

### 8. Integrate BottleneckDetector

In constructor:

```java
private final BottleneckDetector bottleneckDetector = new BottleneckDetector();
```

In tick():

```java
// Sample bottleneck data
bottleneckDetector.sample(client);
```

In `getAvailableActions()`:

```java
private String[] getAvailableActions() {
    String[] allActions = ActionMatrix.getAllActions(); // From ActionMatrix
    
    // Filter by blacklist
    String[] available = Arrays.stream(allActions)
        .filter(action -> blacklist == null || !blacklist.isBlacklisted(action))
        .toArray(String[]::new);
    
    // Filter by bottleneck type
    Bottleneck bottleneck = bottleneckDetector.detect();
    
    switch (bottleneck) {
        case CPU_BOUND:
            // Prioritize CPU optimizations
            return Arrays.stream(available)
                .filter(this::isCPUOptimization)
                .toArray(String[]::new);
                
        case GPU_BOUND:
            // Prioritize GPU optimizations
            return Arrays.stream(available)
                .filter(this::isGPUOptimization)
                .toArray(String[]::new);
                
        default:
            return available;
    }
}

private boolean isCPUOptimization(String action) {
    return action.contains("simulation") || 
           action.contains("entity") || 
           action.contains("tick");
}

private boolean isGPUOptimization(String action) {
    return action.contains("render") || 
           action.contains("particles") || 
           action.contains("graphics") || 
           action.contains("clouds");
}
```

### 9. Integrate Improved Q-Learning

Replace `learningEngine` with:

```java
private final ImprovedQLearning qLearning = new ImprovedQLearning();
```

In action selection:

```java
// Use epsilon-greedy selection
ImprovedQLearning.GameState qState = new ImprovedQLearning.GameState(
    currentScenario.name(),
    (int) currentFps,
    bottleneckDetector.detect().name()
);

String selectedAction = qLearning.selectAction(qState, availableActions);
```

In learning update:

```java
// Update Q-value
double reward = calculateReward(fpsBefore, fpsAfter, actualFpsDelta);
qLearning.updateQValue(stateBefore, actionId, reward, stateAfter);
qLearning.updateAfterEpisode(success);
```

## Summary of Changes

1. ✅ Provider Registry initialization
2. ✅ Real action execution via providers
3. ✅ Transactional rollback on failure
4. ✅ Enhanced telemetry (no synthetic data)
5. ✅ Action window temporal analysis
6. ✅ Hostile entity combat detection
7. ✅ CPU/GPU bottleneck detection and filtering
8. ✅ Improved Q-Learning with exploration
9. ✅ Snapshot-based rollback system
10. ✅ Learning data with TTL and decay

## Testing Checklist

- [ ] Providers execute real changes (verify render distance changes in F3)
- [ ] Rollback works when action fails to improve FPS
- [ ] Combat detection triggers immediately when attacked
- [ ] AFK detection after 30s idle
- [ ] CPU-bound actions prioritized in high entity scenarios
- [ ] GPU-bound actions prioritized with shaders/high render distance
- [ ] Q-Learning explores 30% initially, decays to 5%
- [ ] No synthetic telemetry values in logs
- [ ] Action snapshots restore correctly
- [ ] Learning stats show in `/nozh learning`

## Files Modified

- `IntegratedGovernor.java` - Major refactor
- Added 15+ new files in `core/capability`, `core/scenario`, `core/analysis`, `core/learning`
- ActionMatrix integration (existing file)
- Mixin hooks for action tracking (separate PR)

## Next Steps

1. Apply this patch to IntegratedGovernor.java
2. Update FabricNozhClient to initialize registry
3. Add mixin hooks for action tracking
4. Test in dev environment
5. Create unit tests
6. Submit for review
