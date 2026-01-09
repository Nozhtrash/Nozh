# Circuit Breaker Pattern

## Overview

Complete implementation of the Circuit Breaker pattern to prevent cascading failures.

## States

```
[CLOSED] ──failures─> [OPEN]
    ↑                     │
    │    timeout          │
    │         ↓           │
  success  [HALF_OPEN] ←──┘
```

### CLOSED (Normal)
- All requests allowed
- Failures counted
- Opens at threshold

### OPEN (Fast-Fail)
- Requests rejected immediately
- Throws `CircuitBreakerOpenException`
- Auto-transitions to HALF_OPEN after timeout

### HALF_OPEN (Testing Recovery)
- Limited requests allowed
- Success → CLOSED
- Failure → OPEN

## Usage

### Basic Example

```java
CircuitBreaker breaker = new CircuitBreaker(
    "externalAPI",  // name
    5,              // fail after 5 failures
    60000,          // wait 60s before retry
    10000           // half-open timeout 10s
);

try {
    Result result = breaker.execute(() -> {
        return callExternalService();
    });
    // Success
} catch (CircuitBreakerOpenException e) {
    // Circuit is open, use fallback
    return getCachedResult();
}
```

### Integration with IntegratedGovernor

```java
public class IntegratedGovernor {
    private final CircuitBreaker telemetryBreaker;
    private final CircuitBreaker actionBreaker;
    
    public IntegratedGovernor() {
        this.telemetryBreaker = new CircuitBreaker(
            "telemetry", 3, 30000, 5000
        );
        this.actionBreaker = new CircuitBreaker(
            "actions", 5, 60000, 10000
        );
    }
    
    public void tick() {
        try {
            telemetryBreaker.execute(() -> {
                collectTelemetry();
                return null;
            });
        } catch (CircuitBreakerOpenException e) {
            // Skip telemetry this tick
        }
        
        try {
            actionBreaker.execute(() -> {
                executeAction();
                return null;
            });
        } catch (CircuitBreakerOpenException e) {
            // Skip action execution
        }
    }
}
```

## Monitoring

```java
// Get current state
CircuitBreakerState state = breaker.getState();

// Get metrics
int totalRequests = breaker.getTotalRequests();
int totalFailures = breaker.getTotalFailures();
int totalRejected = breaker.getTotalRejected();

// Get health status
String status = breaker.getHealthStatus();
System.out.println(status);
// Output: "CircuitBreaker 'myService': CLOSED | Failures: 2/5 | Rejected: 0 | Failure Rate: 10.5%"
```

## Testing

### Test State Transitions

```java
@Test
void testCircuitOpensAfterThreshold() {
    CircuitBreaker breaker = new CircuitBreaker("test", 3, 1000, 500);
    
    // Cause 3 failures
    for (int i = 0; i < 3; i++) {
        try {
            breaker.execute(() -> { 
                throw new Exception("fail"); 
            });
        } catch (Exception ignored) {}
    }
    
    // Circuit should be OPEN
    assertEquals(CircuitBreakerState.OPEN, breaker.getState());
    
    // Next request should be rejected
    assertThrows(CircuitBreakerOpenException.class, () -> {
        breaker.execute(() -> "success");
    });
}

@Test
void testCircuitRecovers() throws Exception {
    CircuitBreaker breaker = new CircuitBreaker("test", 2, 100, 50);
    
    // Open the circuit
    for (int i = 0; i < 2; i++) {
        try {
            breaker.execute(() -> { throw new Exception(); });
        } catch (Exception ignored) {}
    }
    assertEquals(CircuitBreakerState.OPEN, breaker.getState());
    
    // Wait for timeout
    Thread.sleep(150);
    
    // Should be in HALF_OPEN, success should close it
    String result = breaker.execute(() -> "success");
    assertEquals("success", result);
    assertEquals(CircuitBreakerState.CLOSED, breaker.getState());
}
```

## Configuration Guidelines

| Service Type | Failure Threshold | Open Timeout | Half-Open Timeout |
|--------------|-------------------|--------------|-------------------|
| Critical | 3 | 30s | 5s |
| Important | 5 | 60s | 10s |
| Non-critical | 10 | 120s | 20s |

## Benefits

- ✅ Prevents cascading failures
- ✅ Fast-fail during outages (1ms vs 5000ms timeout)
- ✅ Automatic recovery testing
- ✅ Thread-safe (atomic operations)
- ✅ Production-ready metrics

## References

- [Circuit Breaker Pattern - Martin Fowler](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Resilience4j](https://resilience4j.readme.io/docs/circuitbreaker)
