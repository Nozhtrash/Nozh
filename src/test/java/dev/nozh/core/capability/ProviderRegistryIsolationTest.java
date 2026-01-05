package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.CostLevel;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProviderRegistry isolation (Contract 3).
 * 
 * CRITICAL: Validates that one broken provider does NOT crash registry.
 * This is the core isolation guarantee of Contract 3.
 */
class ProviderRegistryIsolationTest {

    @Test
    void testBrokenProviderDuringRegistrationDoesNotCrash() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();
        ProviderRegistry registry = new ProviderRegistry(tracker);

        // Provider that throws during id()
        CapabilityProvider brokenProvider = new CapabilityProvider() {
            @Override
            public CapabilityId id() {
                throw new RuntimeException("Simulated init crash");
            }

            @Override
            public ProviderMetadata metadata() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ProviderStatus status() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<String> statusReason() {
                return Optional.empty();
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Optional<CapabilityValue> getCurrentValueSafe() {
                return Optional.empty();
            }

            @Override
            public ApplyResult apply(CapabilityValue value) {
                return new ApplyResult.Rejected("Broken");
            }
        };

        // Registry MUST NOT crash
        assertDoesNotThrow(() -> registry.register(brokenProvider));

        // Registry should still be operational
        assertEquals(0, registry.getRegisteredIds().size());
    }

    @Test
    void testUnavailableProviderNotRegistered() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();
        ProviderRegistry registry = new ProviderRegistry(tracker);

        CapabilityProvider unavailableProvider = new FakeProvider(
                CapabilityId.PARTICLES,
                false, // Not available
                ProviderStatus.HEALTHY);

        registry.register(unavailableProvider);

        // Should not be registered
        assertTrue(registry.get(CapabilityId.PARTICLES).isEmpty());

        // Should be marked BROKEN
        assertTrue(tracker.isBroken(CapabilityId.PARTICLES));
    }

    @Test
    void testBrokenProviderNotRegistered() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();
        ProviderRegistry registry = new ProviderRegistry(tracker);

        CapabilityProvider brokenProvider = new FakeProvider(
                CapabilityId.PARTICLES,
                true, // Available
                ProviderStatus.BROKEN // But self-reports BROKEN
        );

        registry.register(brokenProvider);

        // Should not be registered
        assertTrue(registry.get(CapabilityId.PARTICLES).isEmpty());

        // Should be tracked as BROKEN
        assertTrue(tracker.isBroken(CapabilityId.PARTICLES));
    }

    @Test
    void testHealthyProviderRegistered() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();
        ProviderRegistry registry = new ProviderRegistry(tracker);

        CapabilityProvider healthyProvider = new FakeProvider(
                CapabilityId.PARTICLES,
                true,
                ProviderStatus.HEALTHY);

        registry.register(healthyProvider);

        // Should be registered
        var provider = registry.get(CapabilityId.PARTICLES);
        assertTrue(provider.isPresent());
        assertEquals(CapabilityId.PARTICLES, provider.get().id());

        // Should be tracked as HEALTHY
        assertTrue(tracker.isHealthy(CapabilityId.PARTICLES));
    }

    @Test
    void testMultipleProvidersIsolated() {
        ProviderHealthTracker tracker = new ProviderHealthTracker();
        ProviderRegistry registry = new ProviderRegistry(tracker);

        // Register 3 providers: 1 healthy, 1 broken, 1 unavailable
        registry.register(new FakeProvider(CapabilityId.PARTICLES, true, ProviderStatus.HEALTHY));
        registry.register(new FakeProvider(CapabilityId.CLOUDS, true, ProviderStatus.BROKEN));
        registry.register(new FakeProvider(CapabilityId.ENTITY_SHADOWS, false, ProviderStatus.HEALTHY));

        // Only PARTICLES should be registered
        assertEquals(1, registry.getRegisteredIds().size());
        assertTrue(registry.get(CapabilityId.PARTICLES).isPresent());
        assertTrue(registry.get(CapabilityId.CLOUDS).isEmpty());
        assertTrue(registry.get(CapabilityId.ENTITY_SHADOWS).isEmpty());

        // Health tracker should reflect all
        assertTrue(tracker.isHealthy(CapabilityId.PARTICLES));
        assertTrue(tracker.isBroken(CapabilityId.CLOUDS));
        assertTrue(tracker.isBroken(CapabilityId.ENTITY_SHADOWS));
    }

    /**
     * Fake provider for testing.
     */
    private static class FakeProvider implements CapabilityProvider {
        private final CapabilityId capabilityId;
        private final boolean available;
        private final ProviderStatus providerStatus;

        FakeProvider(CapabilityId id, boolean available, ProviderStatus status) {
            this.capabilityId = id;
            this.available = available;
            this.providerStatus = status;
        }

        @Override
        public CapabilityId id() {
            return capabilityId;
        }

        @Override
        public ProviderMetadata metadata() {
            return new ProviderMetadata() {
                public SideEffects sideEffects() {
                    return SideEffects.none();
                }

                public SafetyLevel safetyLevel() {
                    return SafetyLevel.SAFE;
                }

                public RollbackGuarantee rollbackGuarantee() {
                    return RollbackGuarantee.STRONG;
                }

                public ImpactLevel gameplayImpact() {
                    return ImpactLevel.NONE;
                }

                public CostLevel costLevel() {
                    return CostLevel.LOW;
                }

                public ImpactLevel visualImpact() {
                    return ImpactLevel.NONE;
                }

                public double expectedGainMs() {
                    return 0.0;
                }

                public Set<String> requiredMods() {
                    return Set.of();
                }

                public Set<String> conflictingMods() {
                    return Set.of();
                }
            };
        }

        @Override
        public ProviderStatus status() {
            return providerStatus;
        }

        @Override
        public Optional<String> statusReason() {
            return providerStatus == ProviderStatus.BROKEN ? Optional.of("Test broken") : Optional.empty();
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
            return new ApplyResult.Rejected("Fake provider");
        }
    }
}
