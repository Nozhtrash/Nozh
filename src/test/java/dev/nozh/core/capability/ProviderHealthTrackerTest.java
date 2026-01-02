package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProviderHealthTracker (Contract 3).
 * 
 * Validates:
 * - Health state transitions
 * - Status tracking
 * - Reason capture
 * - Isolation guarantees
 */
class ProviderHealthTrackerTest {

    @Test
    void testInitiallyHealthy() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();

        // Unknown providers default to HEALTHY (optimistic)
        assertTrue(tracker.isHealthy(CapabilityId.PARTICLES));
        assertEquals(ProviderStatus.HEALTHY, tracker.getStatus(CapabilityId.PARTICLES));
        assertTrue(tracker.getStatusReason(CapabilityId.PARTICLES).isEmpty());
    }

    @Test
    void testMarkDegraded() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();

        tracker.markDegraded(CapabilityId.PARTICLES, "Rollback unavailable");

        assertEquals(ProviderStatus.DEGRADED, tracker.getStatus(CapabilityId.PARTICLES));
        assertFalse(tracker.isHealthy(CapabilityId.PARTICLES));
        assertFalse(tracker.isBroken(CapabilityId.PARTICLES));

        var reason = tracker.getStatusReason(CapabilityId.PARTICLES);
        assertTrue(reason.isPresent());
        assertEquals("Rollback unavailable", reason.get());
    }

    @Test
    void testMarkBroken() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();

        tracker.markBroken(CapabilityId.PARTICLES, "Init threw exception");

        assertEquals(ProviderStatus.BROKEN, tracker.getStatus(CapabilityId.PARTICLES));
        assertFalse(tracker.isHealthy(CapabilityId.PARTICLES));
        assertTrue(tracker.isBroken(CapabilityId.PARTICLES));

        var reason = tracker.getStatusReason(CapabilityId.PARTICLES);
        assertTrue(reason.isPresent());
        assertEquals("Init threw exception", reason.get());
    }

    @Test
    void testHealthyToDegradedToBroken() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();

        // Start healthy
        tracker.markHealthy(CapabilityId.PARTICLES);
        assertTrue(tracker.isHealthy(CapabilityId.PARTICLES));

        // Degrade
        tracker.markDegraded(CapabilityId.PARTICLES, "Slow response");
        assertEquals(ProviderStatus.DEGRADED, tracker.getStatus(CapabilityId.PARTICLES));

        // Break
        tracker.markBroken(CapabilityId.PARTICLES, "Total failure");
        assertTrue(tracker.isBroken(CapabilityId.PARTICLES));
    }

    @Test
    void testRecoveryFromBroken() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();

        tracker.markBroken(CapabilityId.PARTICLES, "Init failed");
        assertTrue(tracker.isBroken(CapabilityId.PARTICLES));

        // Recover
        tracker.markHealthy(CapabilityId.PARTICLES);
        assertTrue(tracker.isHealthy(CapabilityId.PARTICLES));
        assertTrue(tracker.getStatusReason(CapabilityId.PARTICLES).isEmpty());
    }

    @Test
    void testMultipleProvidersIsolated() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();

        tracker.markHealthy(CapabilityId.PARTICLES);
        tracker.markBroken(CapabilityId.CLOUDS, "Module missing");
        tracker.markDegraded(CapabilityId.ENTITY_SHADOWS, "Partial failure");

        // Each provider independent
        assertTrue(tracker.isHealthy(CapabilityId.PARTICLES));
        assertTrue(tracker.isBroken(CapabilityId.CLOUDS));
        assertEquals(ProviderStatus.DEGRADED, tracker.getStatus(CapabilityId.ENTITY_SHADOWS));

        // Tracked providers
        var tracked = tracker.getTrackedProviders();
        assertEquals(3, tracked.size());
        assertTrue(tracked.contains(CapabilityId.PARTICLES));
        assertTrue(tracked.contains(CapabilityId.CLOUDS));
        assertTrue(tracked.contains(CapabilityId.ENTITY_SHADOWS));
    }
}
