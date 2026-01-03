package dev.nozh.core.governor;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.capability.ProviderHealthTracker;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.intelligence.SessionLearning;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.matrix.ConfidenceCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationGovernorTest {

    @TempDir
    Path tempDir;

    @Test
    void evaluateOutcomeDetectsPositiveImpact() {
        SimulationGovernor governor = new SimulationGovernor(matrix());
        PerfSnapshot before = snapshot(20.0, 30.0, 40.0);
        PerfSnapshot after = snapshot(18.0, 28.0, 38.0);

        assertEquals(ActionOutcome.POSITIVE, governor.evaluateOutcome(before, after));
    }

    @Test
    void evaluateOutcomeDetectsNegativeImpact() {
        SimulationGovernor governor = new SimulationGovernor(matrix());
        PerfSnapshot before = snapshot(20.0, 30.0, 40.0);
        PerfSnapshot after = snapshot(22.0, 32.0, 42.0);

        assertEquals(ActionOutcome.NEGATIVE, governor.evaluateOutcome(before, after));
    }

    @Test
    void evaluateOutcomeIgnoresInsufficientData() {
        SimulationGovernor governor = new SimulationGovernor(matrix());
        PerfSnapshot before = new PerfSnapshot(20.0, 30.0, 40.0, 1.0, 20, 0, 3, false, 0L);
        PerfSnapshot after = snapshot(18.0, 28.0, 38.0);

        assertEquals(ActionOutcome.NEUTRAL, governor.evaluateOutcome(before, after));
    }

    private ActionMatrix matrix() {
        ProviderRegistry registry = new ProviderRegistry(new ProviderHealthTracker());
        return new ActionMatrix(
                registry,
                new ActionSuccessTracker("env"),
                new ConfidenceCalculator(),
                new SessionLearning(tempDir.toFile()));
    }

    private PerfSnapshot snapshot(double avg, double p95, double p99) {
        return new PerfSnapshot(avg, p95, p99, 1.0, 120, 0, 5, true, System.currentTimeMillis());
    }
}
