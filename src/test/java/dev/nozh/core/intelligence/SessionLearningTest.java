package dev.nozh.core.intelligence;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.context.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLearningTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsStatsAndReloads() {
        SessionLearning learning = new SessionLearning(tempDir.toFile());
        learning.recordSuccess(CapabilityId.PARTICLES, 1.0);
        learning.recordSuccess(CapabilityId.PARTICLES, 3.0);
        learning.recordFailure(CapabilityId.PARTICLES);
        learning.save();

        SessionLearning reloaded = new SessionLearning(tempDir.toFile());

        assertEquals(3, reloaded.getTotalAttempts(CapabilityId.PARTICLES));
        assertEquals(2.0 / 3.0, reloaded.getSuccessRate(CapabilityId.PARTICLES), 1e-6);
        assertEquals(2.0, reloaded.getAvgFpsGain(CapabilityId.PARTICLES), 1e-6);
    }

    @Test
    void avoidsLowSuccessRateAfterThreshold() {
        SessionLearning learning = new SessionLearning(tempDir.toFile());

        learning.recordFailure(CapabilityId.CLOUDS);
        learning.recordFailure(CapabilityId.CLOUDS);
        assertFalse(learning.shouldAvoid(CapabilityId.CLOUDS));

        learning.recordFailure(CapabilityId.CLOUDS);
        assertTrue(learning.shouldAvoid(CapabilityId.CLOUDS));

        learning.recordSuccess(CapabilityId.PARTICLES, 2.0);
        learning.recordFailure(CapabilityId.PARTICLES);
        learning.recordFailure(CapabilityId.PARTICLES);
        assertFalse(learning.shouldAvoid(CapabilityId.PARTICLES));
    }

    @Test
    void rankingReflectsSuccessAndGain() {
        SessionLearning learning = new SessionLearning(tempDir.toFile());

        learning.recordSuccess(CapabilityId.RENDER_DISTANCE, 2.0);
        learning.recordSuccess(CapabilityId.RENDER_DISTANCE, 1.0);

        learning.recordSuccess(CapabilityId.CLOUDS, 0.2);
        learning.recordFailure(CapabilityId.CLOUDS);

        assertTrue(learning.getRanking(CapabilityId.RENDER_DISTANCE)
                > learning.getRanking(CapabilityId.CLOUDS));
    }

    @Test
    void resetForSessionClearsScenarioHistory() {
        SessionLearning learning = new SessionLearning(tempDir.toFile());

        learning.recordSuccess(CapabilityId.PARTICLES, Scenario.COMBAT, 1.0);
        learning.recordFailure(CapabilityId.CLOUDS);
        learning.recordFailure(CapabilityId.CLOUDS);
        learning.recordFailure(CapabilityId.CLOUDS);

        assertTrue(learning.shouldAvoid(CapabilityId.CLOUDS, Scenario.COMBAT));
        assertTrue(learning.getSuccessRate(CapabilityId.PARTICLES, Scenario.COMBAT) > 0.5);

        learning.resetForSession("new-session");

        assertEquals(0.5, learning.getSuccessRate(CapabilityId.PARTICLES, Scenario.COMBAT), 1e-6);
        assertFalse(learning.shouldAvoid(CapabilityId.CLOUDS, Scenario.COMBAT));
    }

    @Test
    void tracksPredictionAccuracyAcrossSessions() {
        SessionLearning learning = new SessionLearning(tempDir.toFile());
        learning.recordPredictionOutcome(true, true, 0.8);
        learning.recordPredictionOutcome(false, true, 0.4);
        learning.save();

        SessionLearning reloaded = new SessionLearning(tempDir.toFile());
        assertTrue(reloaded.getPredictionAccuracy() > 0.0);
    }
}
