package dev.nozh.api;

/**
 * Sealed hierarchy of actions NOZH can apply.
 * 
 * Type-safe, compile-time checked, auditable.
 * NO generic Map<String,Object> params - everything is typed.
 * 
 * Phase 2: Sealed interface definition
 * Phase 2 Iteration 1: GainEstimate instead of double (honesty)
 * Phase 6: Execution
 */
public sealed interface Action permits
        Action.ToggleEntityShadows,
        Action.ToggleClouds,
        Action.SetParticles {

    /**
     * Estimated frametime gain (heuristic, not measured).
     */
    GainEstimate estimatedGain();

    /**
     * Human-readable description for logs.
     */
    String description();

    /**
     * Toggle entity shadows on/off.
     * Rough estimate: 5-8ms GPU impact
     */
    record ToggleEntityShadows(boolean enabled, GainEstimate estimatedGain) implements Action {
        public ToggleEntityShadows(boolean enabled) {
            this(enabled, new GainEstimate.Rough(5, 8));
        }

        @Override
        public String description() {
            return (enabled ? "Enable" : "Disable") + " entity shadows (est. " + estimatedGain + ")";
        }
    }

    /**
     * Toggle cloud rendering on/off.
     * Rough estimate: 2-4ms GPU impact
     */
    record ToggleClouds(boolean enabled, GainEstimate estimatedGain) implements Action {
        public ToggleClouds(boolean enabled) {
            this(enabled, new GainEstimate.Rough(2, 4));
        }

        @Override
        public String description() {
            return (enabled ? "Enable" : "Disable") + " clouds (est. " + estimatedGain + ")";
        }
    }

    /**
     * Set particle rendering level.
     * Rough estimate: 3-6ms GPU impact
     */
    record SetParticles(ParticleLevel level, GainEstimate estimatedGain) implements Action {
        public SetParticles(ParticleLevel level) {
            this(level, new GainEstimate.Rough(3, 6));
        }

        @Override
        public String description() {
            return "Set particles to " + level + " (est. " + estimatedGain + ")";
        }
    }

    /**
     * Particle rendering levels.
     */
    enum ParticleLevel {
        ALL,
        DECREASED,
        MINIMAL
    }
}
