# Testing Guidelines for Nozh-Testing

## Table of Contents
1. [Creating Test Data](#creating-test-data)
2. [Common Pitfalls](#common-pitfalls)
3. [Annotation Guidelines](#annotation-guidelines)
4. [CI/CD Best Practices](#cicd-best-practices)

---

## Creating Test Data

### TelemetrySample Best Practices

#### ✅ CORRECT - Use Factory Methods

```java
// Simple usage (uses sensible defaults)
TelemetrySample sample = TelemetrySample.forTesting(16.0);

// Full control over all parameters
TelemetrySample sample = TelemetrySample.forTesting(
    20.0,  // frametimeMs
    18.0,  // tickMs
    55,    // fps
    120,   // entities
    60,    // chunks
    1500,  // drawCalls
    0      // droppedSamples
);
```

**Why?**
- ✅ Automatically uses valid `System.currentTimeMillis()` timestamps
- ✅ Passes all validation checks
- ✅ Future-proof against validation changes
- ✅ More readable and maintainable

#### ❌ INCORRECT - Manual Timestamp Construction

```java
// DON'T DO THIS - Timestamps may be invalid
TelemetrySample sample = new TelemetrySample(
    1,      // ❌ Timestamp from 1970 - rejected by validation
    16.0, 16.0, 60, 100, 50, 0, 0
);

// DON'T DO THIS - Incrementing counters as timestamps
long timestamp = 0;
for (int i = 0; i < 100; i++) {
    buffer.add(new TelemetrySample(
        timestamp++,  // ❌ Invalid timestamps
        16.0, 16.0, 60, 100, 50, 0, 0
    ));
}
```

**Why it fails:**
- ❌ Timestamps must be within 24 hours of current time
- ❌ Small values (1, 2, 3) are interpreted as epoch timestamps from 1970
- ❌ Tests will fail with `IllegalArgumentException`

---

## Common Pitfalls

### 1. Invalid Test Data

**Problem:** Creating samples with invalid values that violate validation rules.

```java
// ❌ WRONG - Negative frametime
TelemetrySample sample = new TelemetrySample(
    System.currentTimeMillis(),
    -5.0,  // ❌ Invalid unless -1 (sentinel)
    16.0, 60, 100, 50, 0, 0
);

// ✅ CORRECT - Use sentinel or valid values
TelemetrySample sample = TelemetrySample.forTesting(
    -1  // ✅ Sentinel for "unavailable"
);

TelemetrySample sample = TelemetrySample.forTesting(
    20.0  // ✅ Valid positive value
);
```

### 2. Race Conditions in Tests

**Problem:** Tests that fail intermittently due to timing issues.

```java
// ❌ WRONG - Assumes specific timing
@Test
void testConcurrency() throws InterruptedException {
    Thread t1 = new Thread(() -> buffer.add(sample));
    t1.start();
    Thread.sleep(10);  // ❌ Fragile timing assumption
    TelemetrySnapshot snap = buffer.snapshot();
    assertEquals(1, snap.count());  // ❌ May fail due to race
}

// ✅ CORRECT - Use proper synchronization
@Test
void testConcurrency() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Thread t1 = new Thread(() -> {
        buffer.add(sample);
        latch.countDown();
    });
    t1.start();
    assertTrue(latch.await(5, TimeUnit.SECONDS));  // ✅ Proper sync
    TelemetrySnapshot snap = buffer.snapshot();
    assertEquals(1, snap.count());
}
```

### 3. Memory Leaks in Tests

**Problem:** Tests that don't clean up resources.

```java
// ❌ WRONG - Resources not cleaned
@Test
void testTracker() {
    ActionEffectivenessTracker tracker = new ActionEffectivenessTracker();
    tracker.recordActionStart("test", 10.0, reasoning);
    // ❌ No cleanup - executor keeps running
}

// ✅ CORRECT - Use @AfterEach or try-with-resources pattern
@Test
void testTracker() {
    ActionEffectivenessTracker tracker = new ActionEffectivenessTracker();
    try {
        tracker.recordActionStart("test", 10.0, reasoning);
        // ... test logic
    } finally {
        tracker.shutdown();  // ✅ Proper cleanup
    }
}
```

---

## Annotation Guidelines

### Built-in JDK Annotations (Always Safe)

These annotations are **part of the JDK** and require **no extra dependencies**:

```java
@Override           // ✅ Always safe
@Deprecated         // ✅ Always safe
@SuppressWarnings   // ✅ Always safe
@FunctionalInterface // ✅ Always safe
@SafeVarargs        // ✅ Always safe
```

### External Annotations (Require Dependencies)

**Before using these, add the dependency to `build.gradle`:**

#### ErrorProne Annotations

```java
// ❌ WRONG - Will cause compilation errors
@CanIgnoreReturnValue  // Requires error_prone_annotations
public String method() { return "test"; }

// ✅ CORRECT - Add dependency first
// In build.gradle:
dependencies {
    compileOnly 'com.google.errorprone:error_prone_annotations:2.23.0'
}

// Then use the annotation:
@CanIgnoreReturnValue
public String method() { return "test"; }
```

#### Nullability Annotations

```java
// Requires JSR-305 annotations
dependencies {
    compileOnly 'com.google.code.findbugs:jsr305:3.0.2'
}

@Nullable
public String maybeNull() { ... }

@NonNull
public String neverNull() { ... }
```

**Rule of Thumb:**
- If the annotation is in `java.*` or `javax.*` → Safe to use
- If the annotation is in any other package → Check if dependency is in `build.gradle`

---

## CI/CD Best Practices

### Debugging Failed Builds

#### 1. Compilation Errors

**Symptoms:**
- `Build / build` fails
- `CodeQL Analysis` fails with "could not process code"
- Multiple checks fail simultaneously

**Common Causes:**
- Missing dependencies for annotations
- Syntax errors
- Type mismatches

**How to Debug:**
```bash
# Run locally first
./gradlew build --stacktrace

# Check for missing dependencies
./gradlew dependencies | grep -i "not found"
```

#### 2. Test Failures

**Symptoms:**
- `Build / build` succeeds
- `Build / chaos-tests` fails
- Specific test methods shown as failed

**Common Causes:**
- Invalid test data (see [Creating Test Data](#creating-test-data))
- Race conditions
- Environment-specific issues

**How to Debug:**
```bash
# Run specific test locally
./gradlew test --tests "TelemetryDropTest.bufferDropsWhenFull" --stacktrace

# Run with debug output
./gradlew test --debug > test-output.log 2>&1
```

#### 3. CodeQL/Security Scan Failures

**Symptoms:**
- Build succeeds
- Tests pass
- `Security Scan / CodeQL Analysis` fails

**Common Causes:**
- SARIF file generation issues
- Code patterns that trigger false positives

**How to Debug:**
- Check if build succeeds first (CodeQL requires successful compilation)
- Review CodeQL alerts in GitHub Security tab
- Add `// lgtm[java/...]` comments for false positives

---

## Test Organization

### Naming Conventions

```java
// ✅ CORRECT - Descriptive test names
@Test
void shouldDropOldestSampleWhenBufferIsFull() { ... }

@Test
void shouldRejectNegativeFrametime() { ... }

@Test
void shouldCleanupStaleActionsAfter30Seconds() { ... }

// ❌ WRONG - Vague names
@Test
void test1() { ... }

@Test
void testBuffer() { ... }

@Test
void works() { ... }
```

### Test Structure (AAA Pattern)

```java
@Test
void shouldCalculateCorrectAverage() {
    // ARRANGE - Set up test data
    RingTelemetryBuffer buffer = new RingTelemetryBuffer(10);
    buffer.add(TelemetrySample.forTesting(10.0));
    buffer.add(TelemetrySample.forTesting(20.0));
    buffer.add(TelemetrySample.forTesting(30.0));
    
    // ACT - Execute the operation
    TelemetrySnapshot snapshot = buffer.snapshot();
    
    // ASSERT - Verify results
    assertEquals(20.0, snapshot.avgFrametimeMs(), 0.01);
}
```

---

## Performance Testing

### Benchmarking Guidelines

```java
@Test
void telemetrySamplingOverheadShouldBeLow() {
    int iterations = 1000000;
    
    // Warm-up JIT
    for (int i = 0; i < 10000; i++) {
        TelemetrySample.forTesting(16.0);
    }
    
    // Actual benchmark
    long start = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
        TelemetrySample sample = TelemetrySample.forTesting(16.0);
    }
    long duration = System.nanoTime() - start;
    
    // Should be under 100ns per sample
    double avgNanos = (double) duration / iterations;
    assertTrue(avgNanos < 100.0, 
        "Average time per sample: " + avgNanos + "ns");
}
```

---

## Checklist Before Committing

- [ ] All tests pass locally: `./gradlew test`
- [ ] Code compiles without warnings: `./gradlew build`
- [ ] No new dependencies added without updating `build.gradle`
- [ ] Used `TelemetrySample.forTesting()` for test data
- [ ] Test names are descriptive
- [ ] Resources are cleaned up (executors, threads, files)
- [ ] No hardcoded timing assumptions (use timeouts/latches)
- [ ] Annotations used are either JDK built-in or have dependencies

---

## Getting Help

If you encounter issues:

1. Check this guide for common solutions
2. Review recent CI/CD failures in GitHub Actions
3. Run tests locally with `--stacktrace` for detailed errors
4. Check PR comments for automated feedback
5. Ask for review from maintainers

---

**Last Updated:** January 2026  
**Maintainer:** @Nozhtrash
