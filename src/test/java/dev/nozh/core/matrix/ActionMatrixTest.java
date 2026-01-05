package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.ApplyResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.CostLevel;
import dev.nozh.core.capability.ImpactLevel;
import dev.nozh.core.capability.ProviderHealthTracker;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.capability.RollbackGuarantee;
import dev.nozh.core.capability.SafetyLevel;
import dev.nozh.core.capability.SideEffects;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.governor.ModePolicy;
import dev.nozh.core.governor.OptimizationProfile;
import dev.nozh.core.intelligence.SessionLearning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionMatrixTest {

    @TempDir
    Path tempDir;

    @Test
    void cpuBoundGeneratesExpectedTargets() {
        ProviderRegistry registry = registryWith(
                provider(CapabilityId.PARTICLES, metadata(ImpactLevel.LOW, ImpactLevel.MED, SafetyLevel.SAFE, 4.0)),
                provider(CapabilityId.CLOUDS, metadata(ImpactLevel.NONE, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)),
                provider(CapabilityId.ENTITY_SHADOWS,
                        metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 1.0)));

        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTrackerWith(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS),
                new ConfidenceCalculator(),
                new SessionLearning(tempDir.toFile()));

        List<ActionCandidate> candidates = matrix.generateCandidates(ModePolicy.manualAssist(), "CPU",
                Scenario.STANDARD, OptimizationProfile.BALANCED, -1.0, 0);

        Map<CapabilityId, ActionCandidate> byId = byId(candidates);
        assertEquals(3, byId.size());
        assertEquals("MINIMAL", candidateValue(byId.get(CapabilityId.PARTICLES)));
        assertEquals("OFF", candidateValue(byId.get(CapabilityId.CLOUDS)));
        assertEquals("false", candidateValue(byId.get(CapabilityId.ENTITY_SHADOWS)));
    }

    @Test
    void gpuBoundGeneratesExpectedTargets() {
        ProviderRegistry registry = registryWith(
                provider(CapabilityId.RENDER_DISTANCE,
                        metadata(ImpactLevel.HIGH, ImpactLevel.HIGH, SafetyLevel.RISKY, 6.0)),
                provider(CapabilityId.ENTITY_SHADOWS,
                        metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)),
                provider(CapabilityId.PARTICLES, metadata(ImpactLevel.LOW, ImpactLevel.MED, SafetyLevel.SAFE, 3.0)));

        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTrackerWith(CapabilityId.RENDER_DISTANCE, CapabilityId.ENTITY_SHADOWS, CapabilityId.PARTICLES),
                new ConfidenceCalculator(),
                new SessionLearning(tempDir.toFile()));

        List<ActionCandidate> candidates = matrix.generateCandidates(ModePolicy.manualAssist(), "GPU",
                Scenario.STANDARD, OptimizationProfile.BALANCED, -1.0, 0);

        Map<CapabilityId, ActionCandidate> byId = byId(candidates);
        assertEquals(3, byId.size());
        assertEquals("8", candidateValue(byId.get(CapabilityId.RENDER_DISTANCE)));
        assertEquals("false", candidateValue(byId.get(CapabilityId.ENTITY_SHADOWS)));
        assertEquals("DECREASED", candidateValue(byId.get(CapabilityId.PARTICLES)));
    }

    @Test
    void balancedBoundGeneratesExpectedTargets() {
        ProviderRegistry registry = registryWith(
                provider(CapabilityId.PARTICLES, metadata(ImpactLevel.LOW, ImpactLevel.MED, SafetyLevel.SAFE, 4.0)),
                provider(CapabilityId.CLOUDS, metadata(ImpactLevel.NONE, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)));

        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTrackerWith(CapabilityId.PARTICLES, CapabilityId.CLOUDS),
                new ConfidenceCalculator(),
                new SessionLearning(tempDir.toFile()));

        List<ActionCandidate> candidates = matrix.generateCandidates(ModePolicy.manualAssist(), "BALANCED",
                Scenario.STANDARD, OptimizationProfile.BALANCED, -1.0, 0);

        Map<CapabilityId, ActionCandidate> byId = byId(candidates);
        assertEquals(2, byId.size());
        assertEquals("DECREASED", candidateValue(byId.get(CapabilityId.PARTICLES)));
        assertEquals("FAST", candidateValue(byId.get(CapabilityId.CLOUDS)));
    }

    @Test
    void filtersByConfidenceAndSessionLearning() {
        ProviderRegistry registry = registryWith(
                provider(CapabilityId.PARTICLES, metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)),
                provider(CapabilityId.CLOUDS, metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)),
                provider(CapabilityId.ENTITY_SHADOWS,
                        metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)));

        ActionSuccessTracker tracker = new ActionSuccessTracker("env");
        tracker.recordFailure(CapabilityId.CLOUDS);
        tracker.recordFailure(CapabilityId.CLOUDS);
        tracker.recordFailure(CapabilityId.CLOUDS);
        tracker.recordSuccess(CapabilityId.ENTITY_SHADOWS);

        SessionLearning learning = new SessionLearning(tempDir.toFile());
        learning.recordFailure(CapabilityId.PARTICLES);
        learning.recordFailure(CapabilityId.PARTICLES);
        learning.recordFailure(CapabilityId.PARTICLES);

        ActionMatrix matrix = new ActionMatrix(
                registry,
                tracker,
                new ConfidenceCalculator(),
                learning);

        List<ActionCandidate> candidates = matrix.generateCandidates(ModePolicy.manualAssist(), "CPU",
                Scenario.STANDARD, OptimizationProfile.BALANCED, -1.0, 0);

        Map<CapabilityId, ActionCandidate> byId = byId(candidates);
        assertEquals(1, byId.size());
        assertTrue(byId.containsKey(CapabilityId.ENTITY_SHADOWS));
        assertFalse(byId.containsKey(CapabilityId.PARTICLES));
        assertFalse(byId.containsKey(CapabilityId.CLOUDS));
    }

    @Test
    void combatScenarioPrioritizesCombatTargets() {
        ProviderRegistry registry = registryWith(
                provider(CapabilityId.PARTICLES, metadata(ImpactLevel.LOW, ImpactLevel.MED, SafetyLevel.SAFE, 3.0)),
                provider(CapabilityId.CLOUDS, metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0)));

        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTrackerWith(CapabilityId.PARTICLES, CapabilityId.CLOUDS),
                new ConfidenceCalculator(),
                new SessionLearning(tempDir.toFile()));

        List<ActionCandidate> candidates = matrix.generateCandidates(ModePolicy.manualAssist(), "BALANCED",
                Scenario.COMBAT, OptimizationProfile.BALANCED, -1.0, 0);

        Map<CapabilityId, ActionCandidate> byId = byId(candidates);
        assertEquals("DECREASED", candidateValue(byId.get(CapabilityId.PARTICLES)));
        assertEquals("OFF", candidateValue(byId.get(CapabilityId.CLOUDS)));
    }

    @Test
    void ranksCandidatesBySessionLearningAndPreservesRollbackGuarantee() {
        ProviderMetadata cloudsMetadata = metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0,
                RollbackGuarantee.BEST_EFFORT);
        ProviderMetadata particlesMetadata = metadata(ImpactLevel.LOW, ImpactLevel.LOW, SafetyLevel.SAFE, 2.0,
                RollbackGuarantee.STRONG);
        ProviderRegistry registry = registryWith(
                provider(CapabilityId.CLOUDS, cloudsMetadata),
                provider(CapabilityId.PARTICLES, particlesMetadata));

        ActionSuccessTracker tracker = successTrackerWith(CapabilityId.CLOUDS, CapabilityId.PARTICLES);
        SessionLearning learning = new SessionLearning(tempDir.toFile());
        learning.recordSuccess(CapabilityId.CLOUDS, Scenario.STANDARD, 2.0);
        learning.recordSuccess(CapabilityId.CLOUDS, Scenario.STANDARD, 2.5);
        learning.recordSuccess(CapabilityId.PARTICLES, Scenario.STANDARD, 0.1);

        ActionMatrix matrix = new ActionMatrix(
                registry,
                tracker,
                new ConfidenceCalculator(),
                learning);

        List<ActionCandidate> candidates = matrix.generateCandidates(ModePolicy.manualAssist(), "BALANCED",
                Scenario.STANDARD, OptimizationProfile.BALANCED, -1.0, 0);

        assertEquals(2, candidates.size());
        assertEquals(CapabilityId.CLOUDS, candidates.get(0).capabilityId());
        assertEquals(RollbackGuarantee.BEST_EFFORT, candidates.get(0).rollbackGuarantee());
        assertEquals(RollbackGuarantee.STRONG, candidates.get(1).rollbackGuarantee());
    }

    private ProviderRegistry registryWith(CapabilityProvider... providers) {
        ProviderRegistry registry = new ProviderRegistry(new ProviderHealthTracker());
        for (CapabilityProvider provider : providers) {
            registry.register(provider);
        }
        return registry;
    }

    private ActionSuccessTracker successTrackerWith(CapabilityId... ids) {
        ActionSuccessTracker tracker = new ActionSuccessTracker("env");
        for (CapabilityId id : ids) {
            tracker.recordSuccess(id);
        }
        return tracker;
    }

    private Map<CapabilityId, ActionCandidate> byId(List<ActionCandidate> candidates) {
        Map<CapabilityId, ActionCandidate> byId = new EnumMap<>(CapabilityId.class);
        for (ActionCandidate candidate : candidates) {
            byId.put(candidate.capabilityId(), candidate);
        }
        return byId;
    }

    private String candidateValue(ActionCandidate candidate) {
        CapabilityValue value = candidate.targetValue();
        if (value instanceof CapabilityValue.EnumValue enumValue) {
            return enumValue.name();
        } else if (value instanceof CapabilityValue.IntValue intValue) {
            return String.valueOf(intValue.value());
        } else if (value instanceof CapabilityValue.BoolValue boolValue) {
            return String.valueOf(boolValue.value());
        }
        return value.toString();
    }

    private CapabilityProvider provider(CapabilityId id, ProviderMetadata metadata) {
        return new TestProvider(id, metadata, ProviderStatus.HEALTHY, true);
    }

    private ProviderMetadata metadata(
            ImpactLevel gameplayImpact,
            ImpactLevel visualImpact,
            SafetyLevel safetyLevel,
            double expectedGain) {
        return metadata(gameplayImpact, visualImpact, safetyLevel, expectedGain, RollbackGuarantee.STRONG);
    }

    private ProviderMetadata metadata(
            ImpactLevel gameplayImpact,
            ImpactLevel visualImpact,
            SafetyLevel safetyLevel,
            double expectedGain,
            RollbackGuarantee rollbackGuarantee) {
        return new TestProviderMetadata(
                SideEffects.none(),
                safetyLevel,
                rollbackGuarantee,
                gameplayImpact,
                CostLevel.LOW,
                visualImpact,
                expectedGain,
                Set.of(),
                Set.of());
    }

    private record TestProviderMetadata(
            SideEffects sideEffects,
            SafetyLevel safetyLevel,
            RollbackGuarantee rollbackGuarantee,
            ImpactLevel gameplayImpact,
            dev.nozh.core.capability.CostLevel costLevel,
            ImpactLevel visualImpact,
            double expectedGainMs,
            Set<String> requiredMods,
            Set<String> conflictingMods) implements ProviderMetadata {
    }

    private static final class TestProvider implements CapabilityProvider {
        private final CapabilityId id;
        private final ProviderMetadata metadata;
        private final ProviderStatus status;
        private final boolean available;

        private TestProvider(CapabilityId id, ProviderMetadata metadata, ProviderStatus status, boolean available) {
            this.id = id;
            this.metadata = metadata;
            this.status = status;
            this.available = available;
        }

        @Override
        public CapabilityId id() {
            return id;
        }

        @Override
        public ProviderMetadata metadata() {
            return metadata;
        }

        @Override
        public ProviderStatus status() {
            return status;
        }

        @Override
        public Optional<String> statusReason() {
            return Optional.empty();
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public Optional<CapabilityValue> getCurrentValueSafe() {
            return Optional.empty();
        }

        @Override
        public ApplyResult apply(CapabilityValue value) {
            return new ApplyResult.Success(value, value);
        }
    }
}
