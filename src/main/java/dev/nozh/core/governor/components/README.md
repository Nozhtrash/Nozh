# Governor Components Package

## Overview

This package contains components extracted from `IntegratedGovernor` as part of the P1 God Class refactoring.

## Components

### 1. TelemetryManager
**Responsibility:** Telemetry collection and buffering

```java
TelemetryManager telemetry = new TelemetryManager(client, 512, 60);
TelemetrySample sample = telemetry.collectAndStore();
TelemetrySnapshot snapshot = telemetry.getSnapshot();
```

### 2. MonitoringFacade
**Responsibility:** Health monitoring and logging

```java
MonitoringFacade monitoring = new MonitoringFacade(logPath);
monitoring.updateFromTelemetry(snapshot);
String report = monitoring.getHealthReport();
```

### 3. ActionExecutor
**Responsibility:** Async action execution and learning

```java
ActionExecutor executor = new ActionExecutor(
    tracker, learningEngine, weightTuner,
    telemetry, monitoring
);

executor.executeAsync(actionId, reasoning, scenario, state, fpsBefore);
int pending = executor.getPendingCount();
```

## Benefits

- ✅ Single Responsibility Principle
- ✅ Easy to test in isolation
- ✅ Clear interfaces
- ✅ Reduced coupling

## Migration from IntegratedGovernor

**Before:**
```java
public class IntegratedGovernor {
    // 600+ lines of mixed responsibilities
}
```

**After:**
```java
public class IntegratedGovernor {
    private final TelemetryManager telemetry;
    private final MonitoringFacade monitoring;
    private final ActionExecutor executor;
    
    // Clean, focused orchestration
}
```

## Testing

Each component can be tested independently:

```java
@Test
void testTelemetryCollection() {
    TelemetryManager tm = new TelemetryManager(mockClient, 10, 60);
    TelemetrySample sample = tm.collectAndStore();
    assertNotNull(sample);
}
```
