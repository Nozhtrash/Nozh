# NOZH v0.2-beta - CRITICAL INTEGRATION NOTES

**Date:** 2025-12-31  
**Author:** CTO Review Notes  
**Status:** PRE-BETA WARNINGS (PLANIFICADO, NO IMPLEMENTADO)

---

## ⚠️ CRITICAL POINT 1: Governor + RuntimeState Wiring

**What:** Connecting the 11 integration TODOs (HudViewModelBuilder, GovernorRunner, ActionMatrix)

**Risks:**

- **Immutability breakage**: RuntimeState snapshots could become mutable references
- **Timestamp flapping**: lastGovernorActionTimestamp logic can reintroduce cascade bugs
- **HUD state mutation**: UI code accidentally mutating state instead of reading snapshots

**Safeguards Required:**

```java
// GOOD: Defensive copy
RuntimeState snapshot = state.snapshotSafe(); // Returns immutable copy

// BAD: Direct reference
RuntimeState current = stateStore.getCurrent(); // Mutable!
```

**Tests to Add:**

- `RuntimeStateImmutabilityTest` - verify snapshots are truly immutable
- `GovernorTimestampNoFlapTest` - rapid timestamp updates don't trigger cascade
- `HudNoStateMutationTest` - HUD ViewModel builder has zero side effects

**Contract Reminder:**
> RuntimeState is append-only. Mutations create new instances. Snapshots are defensive copies.

---

## ⚠️ CRITICAL POINT 2: Chaos Tests - "Stub → Real"

**What:** Implementing the 8 ChaosScenario stubs with actual malicious behavior

**Current:** All scenarios return `pass()` (documented stubs)  
**v0.2-beta:** Full execution with FakeCapabilityProvider, real ActionBus, real StateStore

**Mindset Shift Required:**
> Don't make tests "reasonable" - make them MALICIOUS.

**Real-World Scenarios:**

```java
// Scenario: Provider Init Failure
// NOT: Provider throws once, system logs, continues
// BUT: Provider throws intermittently, 50% of the time, with different exceptions

// Scenario: Queue Overflow  
// NOT: Fill queue to capacity + 1
// BUT: 10 threads hammering queue with 1000 commands each, concurrently

// Scenario: Telemetry Starvation
// NOT: Add 513 samples (1 over capacity)
// BUT: 100k samples in tight loop, verify zero crashes
```

**Reality Check:**
> "300 mods + 4GB RAM + integrated graphics + shaders + DH" = the true chaos test

**Tests to Create:**

- `ChaosProviderIntermittentFailure` - 50% throw rate
- `ChaosQueueConcurrentHammer` - 10 threads × 1000 commands
- `ChaosTelemetryMassiveOverflow` - 100k samples, verify no OOM
- `ChaosStateInvariantViolationBarrage` - 1000 invalid updates rapid-fire

---

## ⚠️ CRITICAL POINT 3: Anti-Theft - Subtle Escalation

**Current Protection (Strong):**
✅ Contract-based architecture (requires deep understanding)  
✅ 42 tests (copiers won't run tests)  
✅ Professional docs (harder to cargo-cult)

**Recommended Additions (Non-Paranoid):**

### A. Copyright Headers (Lightweight)

Add to core files only (not everything):

```java
/**
 * NOZH - Adaptive Performance Optimization
 * Copyright (c) 2025 NOZH Project
 * 
 * This source code is licensed under the MIT license.
 * See LICENSE file in the project root for details.
 * 
 * Architecture: Contract-based immutability with rollback guarantees
 */
package dev.nozh.core.state;
```

**Files Priority:**

- `RuntimeState.java`
- `StateStore.java`
- `ActionBus.java`
- `SimulationGovernor.java`
- `CapabilityProvider.java`

### B. Intent-Revealing Tests

Rename generic tests to reveal "why":

```java
// BEFORE
@Test void testBufferOverflow() { ... }

// AFTER  
@Test void bufferOverflowDropsSamplesInsteadOfCrashingToPreventMemoryLeak() { ... }
```

**Effect:** Makes copy-paste harder because test names explain architectural decisions.

### C. "Why" Comments (Strategic)

Not everywhere, just at critical decision points:

```java
// Why volatile + synchronized: TelemetryBuffer is accessed from render thread (add) 
// and governor thread (snapshot) concurrently. volatile ensures visibility, 
// synchronized prevents torn reads during snapshot copy. Pure locks would block 
// render thread → frametime spike. This specific pattern is frametime-critical.
private volatile int writeIndex = 0;
```

**Effect:** Thief copies code but loses context → breaks when modifying.

---

## 🎯 v0.2-beta Success Criteria

**Governor Wiring:**

- [ ] All 11 TODOs resolved
- [ ] RuntimeStateImmutabilityTest passing
- [ ] NO new flapping bugs introduced
- [ ] HUD still pure (no side effects)

**Chaos Execution:**

- [ ] All 8 scenarios fully implemented
- [ ] Each scenario tests "worst case + 10%"
- [ ] ChaosTestRunner survives 10k iterations
- [ ] Zero crashes, zero OOMs, zero deadlocks

**Anti-Theft:**

- [ ] Copyright headers on 5 core files
- [ ] 10+ tests renamed with intent
- [ ] 5+ "why" comments at critical decisions

---

## 📊 Quality Gates (DO NOT MERGE WITHOUT)

✅ All new integration code has corresponding tests  
✅ `./gradlew clean test` GREEN  
✅ No regressions in StateStore immutability  
✅ Chaos suite passes 100/100 runs  
✅ HUD performance still <0.5ms when open  

**If ANY of these fail → revert and redesign.**

---

## 🧠 Architectural Mantras (Never Forget)

> "RuntimeState is append-only. Mutations create new instances."

> "Chaos tests should make you uncomfortable. If they seem reasonable, they're not testing the real world."

> "HUD reads snapshots. HUD never mutates. HUD never holds references."

> "Governor decides. Executor acts. NO exceptions."

> "Tests that explain 'why' are harder to break than tests that check 'what'."

---

**This document is the difference between v0.2-beta shipping clean vs. accumulating debt.**

Keep it visible. Review before touching RuntimeState or Chaos.
