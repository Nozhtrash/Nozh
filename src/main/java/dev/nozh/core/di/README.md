# Dependency Injection (DI) Interfaces

## Overview

Interfaces to enable Dependency Injection pattern for improved testability and flexibility.

## Why Dependency Injection?

### ❌ Without DI (Tight Coupling)

```java
public class IntegratedGovernor {
    // Hard-coded dependencies
    private final TelemetryManager telemetry = new TelemetryManager(...);
    private final MonitoringFacade monitoring = new MonitoringFacade(...);
    
    // ❌ Cannot mock in tests
    // ❌ Cannot swap implementations
    // ❌ High coupling
}
```

### ✅ With DI (Loose Coupling)

```java
public class IntegratedGovernor {
    private final ITelemetryCollector telemetry;
    private final IHealthMonitor monitoring;
    
    // Constructor Injection
    public IntegratedGovernor(
            ITelemetryCollector telemetry,
            IHealthMonitor monitoring) {
        this.telemetry = telemetry;
        this.monitoring = monitoring;
    }
    
    // ✅ Easy to mock
    // ✅ Flexible implementations
    // ✅ Low coupling
}
```

## Interfaces

### ITelemetryCollector

Abstracts telemetry collection and buffering.

```java
public interface ITelemetryCollector {
    TelemetrySample collectAndStore();
    TelemetrySnapshot getSnapshot();
    int getDroppedCount();
    void clear();
}
```

**Implementations:**
- `TelemetryManager` - Production implementation
- `MockTelemetryCollector` - For testing

### IHealthMonitor

Abstracts health monitoring and metrics.

```java
public interface IHealthMonitor {
    void updateFromTelemetry(TelemetrySnapshot snapshot);
    void recordError(String errorMessage);
    boolean isHealthy();
    String getHealthStatus();
    String getHealthReport();
    void shutdown();
}
```

**Implementations:**
- `MonitoringFacade` - Production implementation
- `MockHealthMonitor` - For testing

### IActionExecutor

Abstracts async action execution.

```java
public interface IActionExecutor {
    void executeAsync(
        String actionId,
        DecisionReasoning reasoning,
        Scenario scenario,
        GameState state,
        double fpsBefore
    );
    int getPendingCount();
    void shutdown();
}
```

**Implementations:**
- `ActionExecutor` - Production implementation
- `MockActionExecutor` - For testing

## Usage Examples

### Production Code

```java
// Create dependencies
ITelemetryCollector telemetry = new TelemetryManager(client, 512, 60);
IHealthMonitor monitoring = new MonitoringFacade(logPath);
IActionExecutor executor = new ActionExecutor(
    tracker, learningEngine, weightTuner,
    (TelemetryManager) telemetry,
    (MonitoringFacade) monitoring
);

// Inject into governor
IntegratedGovernor governor = new IntegratedGovernor(
    client,
    telemetry,
    monitoring,
    executor
);
```

### Unit Testing

#### Before (Impossible to Test)

```java
@Test
void testGovernor() {
    // ❌ Cannot mock TelemetryManager
    // ❌ Test depends on real Minecraft client
    // ❌ Slow, brittle tests
    IntegratedGovernor governor = new IntegratedGovernor(client, logPath);
    // ...
}
```

#### After (Easy to Test)

```java
@Test
void testGovernorTick() {
    // ✅ Create mocks
    ITelemetryCollector mockTelemetry = mock(ITelemetryCollector.class);
    IHealthMonitor mockMonitoring = mock(IHealthMonitor.class);
    IActionExecutor mockExecutor = mock(IActionExecutor.class);
    
    // ✅ Setup behavior
    TelemetrySample fakeSample = new TelemetrySample(...);
    when(mockTelemetry.collectAndStore()).thenReturn(fakeSample);
    
    TelemetrySnapshot fakeSnapshot = new TelemetrySnapshot(...);
    when(mockTelemetry.getSnapshot()).thenReturn(fakeSnapshot);
    
    // ✅ Inject mocks
    IntegratedGovernor governor = new IntegratedGovernor(
        mockClient,
        mockTelemetry,
        mockMonitoring,
        mockExecutor
    );
    
    // ✅ Test behavior
    governor.tick();
    
    // ✅ Verify interactions
    verify(mockTelemetry, times(1)).collectAndStore();
    verify(mockMonitoring, times(1)).updateFromTelemetry(any());
}
```

### Mock Implementations

```java
public class MockTelemetryCollector implements ITelemetryCollector {
    private final Queue<TelemetrySample> samples = new LinkedList<>();
    
    public void addSample(TelemetrySample sample) {
        samples.add(sample);
    }
    
    @Override
    public TelemetrySample collectAndStore() {
        return samples.poll();
    }
    
    @Override
    public TelemetrySnapshot getSnapshot() {
        return TelemetrySnapshot.EMPTY;
    }
    
    @Override
    public int getDroppedCount() {
        return 0;
    }
    
    @Override
    public void clear() {
        samples.clear();
    }
}
```

## Benefits

### 1. Testability 🧪

```java
// Easy to create mocks with Mockito
ITelemetryCollector mock = mock(ITelemetryCollector.class);
when(mock.collectAndStore()).thenReturn(sample);
```

### 2. Flexibility 🔄

```java
// Production
ITelemetryCollector prod = new TelemetryManager(...);

// Testing
ITelemetryCollector test = new MockTelemetryCollector();

// Debugging (Decorator Pattern)
ITelemetryCollector debug = new LoggingTelemetryDecorator(
    new TelemetryManager(...)
);
```

### 3. SOLID Compliance 🎯

- **D**ependency Inversion: Depend on abstractions
- **O**pen/Closed: Open for extension, closed for modification
- **L**iskov Substitution: Any implementation works
- **I**nterface Segregation: Small, focused interfaces
- **S**ingle Responsibility: One purpose per interface

### 4. Future-Ready 🔮

Ready for Spring Framework integration:

```java
@Configuration
public class NozhConfig {
    
    @Bean
    public ITelemetryCollector telemetryCollector(MinecraftClient client) {
        return new TelemetryManager(client, 512, 60);
    }
    
    @Bean
    public IHealthMonitor healthMonitor(Path logPath) {
        return new MonitoringFacade(logPath);
    }
    
    @Bean
    public IntegratedGovernor governor(
            MinecraftClient client,
            ITelemetryCollector telemetry,
            IHealthMonitor monitoring) {
        return new IntegratedGovernor(client, telemetry, monitoring);
    }
}
```

## Migration Guide

### Step 1: Implement Interfaces

```java
public class TelemetryManager implements ITelemetryCollector {
    // ... existing code ...
}

public class MonitoringFacade implements IHealthMonitor {
    // ... existing code ...
}
```

### Step 2: Update Constructor

```java
public class IntegratedGovernor {
    // Change from concrete to interface
    private final ITelemetryCollector telemetry;
    private final IHealthMonitor monitoring;
    
    public IntegratedGovernor(
            ITelemetryCollector telemetry,
            IHealthMonitor monitoring) {
        this.telemetry = telemetry;
        this.monitoring = monitoring;
    }
}
```

### Step 3: Update Factory/Creation

```java
// Production factory
public static IntegratedGovernor createProduction(
        MinecraftClient client,
        Path logPath) {
    
    ITelemetryCollector telemetry = new TelemetryManager(client, 512, 60);
    IHealthMonitor monitoring = new MonitoringFacade(logPath);
    
    return new IntegratedGovernor(telemetry, monitoring);
}

// Testing factory
public static IntegratedGovernor createForTesting(
        ITelemetryCollector mockTelemetry,
        IHealthMonitor mockMonitoring) {
    
    return new IntegratedGovernor(mockTelemetry, mockMonitoring);
}
```

## Testing Strategy

### Unit Tests
Test each component in isolation using mocks:
```java
@Test
void testComponentBehavior() {
    ITelemetryCollector mock = mock(ITelemetryCollector.class);
    // Test with mock
}
```

### Integration Tests
Test with real implementations:
```java
@Test
void testFullIntegration() {
    ITelemetryCollector real = new TelemetryManager(...);
    // Test with real implementation
}
```

## References

- [Dependency Injection - Martin Fowler](https://martinfowler.com/articles/injection.html)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Mockito Framework](https://site.mockito.org/)
- [Spring Framework DI](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-dependencies)
