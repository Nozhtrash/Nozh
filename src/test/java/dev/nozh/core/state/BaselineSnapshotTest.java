package dev.nozh.core.state;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineSnapshotTest {

    @Test
    void clampsRollbackToBaseline() {
        BaselineSnapshot baseline = new BaselineSnapshot(Map.of(
                CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(10)));
        CapabilityValue candidate = new CapabilityValue.IntValue(16);

        assertTrue(baseline.exceedsBaseline(CapabilityId.RENDER_DISTANCE, candidate));
        assertEquals(new CapabilityValue.IntValue(10),
                baseline.clampToBaseline(CapabilityId.RENDER_DISTANCE, candidate));
    }

    @Test
    void keepsCandidateWhenWithinBaseline() {
        BaselineSnapshot baseline = new BaselineSnapshot(Map.of(
                CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED")));
        CapabilityValue candidate = new CapabilityValue.EnumValue("MINIMAL");

        assertFalse(baseline.exceedsBaseline(CapabilityId.PARTICLES, candidate));
        assertEquals(candidate, baseline.clampToBaseline(CapabilityId.PARTICLES, candidate));
    }
}
