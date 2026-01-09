/**
 * NOZH - Adaptive Performance Optimization
 * Copyright (c) 2025 NOZH Project
 * 
 * Licensed under the MIT License.
 * 
 * This file defines a CORE ARCHITECTURAL CONTRACT.
 * Changes here affect system-wide invariants.
 * 
 * Read docs/v0.2-alpha.md before modifying.
 */
package dev.nozh.core.governor;

import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.api.PerfSnapshot;

import java.util.Map;

import java.util.List;
import java.util.Optional;

/**
 * Simulation governor (Contract 6).
 * 
 * WHY THIS EXISTS:
 * SimulationGovernor makes decisions WITHOUT side effects (pure function).
 * It takes state + mode + bound, returns Optional<ActionCandidate>.
 * NO execution happens here. This separation enables:
 * - Testing without Minecraft
 * - Deterministic replays for debugging
 * - Confidence scoring based on past decisions
 * 
 * CRITICAL RULE: NO CASCADE
 * 
 * WHY NO CASCADE MATTERS:
 * "Cascade" = multiple actions in quick succession. This is FATAL because:
 * 1. Can't measure individual action effects (frametime needs time to
 * stabilize)
 * 2. Causes flapping (system oscillates: particles on → off → on → off)
 * 3. User sees rapid changes, blames mod for instability
 * 4. Confidence scoring breaks (can't tell which action helped/hurt)
 * 
 * HOW NO CASCADE WORKS:
 * - Maximum 1 action per observation window (45s)
 * - Must wait for observation window before next decision
 * - Never acts if cooldown active
 * 
 * Example scenario if NO CASCADE was removed:
 * - Tick 1: Reduce particles (FPS improves slightly)
 * - Tick 2: Governor sees improvement, tries to reduce clouds
 * - Tick 3: Clouds reduction tanks FPS (unexpected shader interaction)
 * - Tick 4: Governor panics, tries to restore particles
 * - Result: Flapping, user confusion, system looks broken
 * 
 * PURE - no MC dependencies.
 */
public final class SimulationGovernor {

        private final ActionMatrix actionMatrix;
        private final AdaptiveWindowCalculator windowCalculator;
        private static final double OUTCOME_EPSILON_MS = 0.75;

        public SimulationGovernor(ActionMatrix actionMatrix) {
                this.actionMatrix = actionMatrix;
                this.windowCalculator = new AdaptiveWindowCalculator();
        }

        /**
         * Make a governor decision.
         * 
         * @param state        Current runtime state
         * @param mode         Governor mode
         * @param currentBound Performance bound
         * @param nowMillis    Current timestamp
         * @return Best action candidate, or empty if no valid action
         */
        public Optional<ActionCandidate> decide(
                        RuntimeState state,
                        GovernorMode mode,
                        String currentBound,
                        long nowMillis,
                        OptimizationProfile profile,
                        int targetFps,
                        double reverseEpsilonMs,
                        boolean reverseReady,
                        dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
                        Map<dev.nozh.core.capability.CapabilityId, dev.nozh.core.capability.CapabilityValue> currentSettings) {
                // OFF mode → no decisions
                if (mode == GovernorMode.OFF) {
                        return Optional.empty();
                }

                // Get policy
                ModePolicy policy = ModePolicy.forMode(mode);

                // Generate candidates
                if (reverseReady
                                && shouldReverseOptimize(state, targetFps, reverseEpsilonMs, baselineSnapshot,
                                                currentSettings)) {
                        List<ActionCandidate> reverseCandidates = actionMatrix.generateReverseCandidates(
                                        policy,
                                        state.currentScenario(),
                                        profile,
                                        baselineSnapshot,
                                        currentSettings);
                        if (!reverseCandidates.isEmpty()) {
                                return Optional.of(reverseCandidates.get(0));
                        }
                }

                List<ActionCandidate> candidates = actionMatrix.generateCandidates(
                                policy,
                                currentBound,
                                state.currentScenario(),
                                profile,
                                state.p95FrametimeMs(),
                                state.spikeCount());

                if (candidates.isEmpty()) {
                        return Optional.empty();
                }

                // Return best candidate (already sorted by ActionMatrix)
                return Optional.of(candidates.get(0));
        }

        private boolean shouldReverseOptimize(
                        RuntimeState state,
                        int targetFps,
                        double reverseEpsilonMs,
                        dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
                        Map<dev.nozh.core.capability.CapabilityId, dev.nozh.core.capability.CapabilityValue> currentSettings) {
                if (baselineSnapshot == null || baselineSnapshot.isEmpty()) {
                        return false;
                }
                if (currentSettings == null || currentSettings.isEmpty()) {
                        return false;
                }
                double avg = state.avgFrametimeMs();
                double p95 = state.p95FrametimeMs();
                if (avg <= 0 || p95 <= 0) {
                        return false;
                }
                int safeTargetFps = Math.max(1, targetFps);
                double targetFrameMs = 1000.0 / safeTargetFps;
                double headroomTarget = targetFrameMs - reverseEpsilonMs;
                return avg <= headroomTarget && p95 <= targetFrameMs * 1.05;
        }

        /**
         * Check if governor can act (not in cooldown).
         * Uses adaptive observation window based on FPS stability.
         */
        public boolean canAct(RuntimeState state, long lastActionTimestamp, long nowMillis, boolean benchmarkMode,
                        long benchmarkIntervalMillis) {
                if (lastActionTimestamp == 0) {
                        return true; // Never acted before
                }

                // Calculate adaptive window based on FPS variance
                long adaptiveWindow = windowCalculator.calculateWindow(state);
                if (benchmarkMode) {
                        adaptiveWindow = Math.max(1000L, benchmarkIntervalMillis);
                }

                long timeSinceLastAction = nowMillis - lastActionTimestamp;
                return timeSinceLastAction >= adaptiveWindow;
        }

        /**
         * Get current observation window (for logging/debugging).
         */
        public long getObservationWindow(RuntimeState state) {
                return windowCalculator.calculateWindow(state);
        }

        /**
         * Evaluate the outcome of an action based on performance snapshots.
         */
        public ActionOutcome evaluateOutcome(PerfSnapshot previousSnapshot, PerfSnapshot newSnapshot) {
                if (previousSnapshot == null || newSnapshot == null) {
                        return ActionOutcome.NEUTRAL;
                }
                if (!previousSnapshot.sufficientData() || !newSnapshot.sufficientData()) {
                        return ActionOutcome.NEUTRAL;
                }

                double avgDelta = newSnapshot.avgFrametimeMs() - previousSnapshot.avgFrametimeMs();
                double p95Delta = newSnapshot.p95FrametimeMs() - previousSnapshot.p95FrametimeMs();
                double p99Delta = newSnapshot.p99FrametimeMs() - previousSnapshot.p99FrametimeMs();

                boolean worsened = avgDelta > OUTCOME_EPSILON_MS
                                || p95Delta > OUTCOME_EPSILON_MS
                                || p99Delta > OUTCOME_EPSILON_MS;
                if (worsened) {
                        return ActionOutcome.NEGATIVE;
                }

                boolean improved = avgDelta < -OUTCOME_EPSILON_MS
                                && p95Delta < -OUTCOME_EPSILON_MS
                                && p99Delta < -OUTCOME_EPSILON_MS;
                if (improved) {
                        return ActionOutcome.POSITIVE;
                }

                return ActionOutcome.NEUTRAL;
        }
}
