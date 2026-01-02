package dev.nozh.core.bus;

import dev.nozh.core.state.RuntimeState;

/**
 * Command validator (Contract 2, Rule 2.4: DETERMINISTIC).
 * 
 * NEVER accesses:
 * - Minecraft classes
 * - Capability providers
 * - Mutable state (except mode flags from RuntimeState)
 * 
 * ONLY validates:
 * - Types (CapabilityValue correct for CapabilityId)
 * - Ranges (value within valid bounds)
 * - Invariants (SafeMode → NO auto-tuning commands)
 * - Mode compatibility (GovernorMode restrictions)
 * 
 * If validation needs MC state → check belongs in executor, NOT here.
 */
public final class CommandValidator {

    private CommandValidator() {
        throw new AssertionError("No instances");
    }

    /**
     * Validate a command against current state.
     * 
     * @param command Command to validate
     * @param state   Current runtime state (for mode checks)
     * @return ValidationResult (Valid or Invalid with reason)
     */
    public static ValidationResult validate(Command command, RuntimeState state) {
        // 1. Type validation
        ValidationResult typeResult = validateType(command);
        if (typeResult.isInvalid()) {
            return typeResult;
        }

        // 2. Range validation
        ValidationResult rangeResult = validateRange(command);
        if (rangeResult.isInvalid()) {
            return rangeResult;
        }

        // 3. Mode validation
        ValidationResult modeResult = validateMode(command, state);
        if (modeResult.isInvalid()) {
            return modeResult;
        }

        return new ValidationResult.Valid();
    }

    /**
     * Validate command type matches capability requirements.
     */
    private static ValidationResult validateType(Command command) {
        if (command instanceof Command.ApplyCapability) {
            return validateApplyType((Command.ApplyCapability) command);
        } else if (command instanceof Command.ResetCapability) {
            return new ValidationResult.Valid(); // Reset has no type constraints
        } else if (command instanceof Command.PreviewCapability) {
            Command.PreviewCapability preview = (Command.PreviewCapability) command;
            return validateApplyType(toApply(preview)); // Same as apply
        } else if (command instanceof Command.RunBenchmark) {
            return validateBenchmarkType((Command.RunBenchmark) command);
        }
        return new ValidationResult.Invalid("Unknown command type");
    }

    private static ValidationResult validateApplyType(Command.ApplyCapability cmd) {
        CapabilityId cap = cmd.capability();
        CapabilityValue val = cmd.value();

        // Integer capabilities
        if (cap == CapabilityId.RENDER_DISTANCE || cap == CapabilityId.SIMULATION_DISTANCE ||
                cap == CapabilityId.ENTITY_DISTANCE || cap == CapabilityId.BIOME_BLEND ||
                cap == CapabilityId.MIPMAP_LEVEL || cap == CapabilityId.FPS_CAP) {
            if (!(val instanceof CapabilityValue.IntValue)) {
                return new ValidationResult.Invalid(
                        cap + " requires IntValue, got " + val.getClass().getSimpleName());
            }
            return new ValidationResult.Valid();
        }

        // Enum capabilities
        if (cap == CapabilityId.PARTICLES || cap == CapabilityId.CLOUDS ||
                cap == CapabilityId.GRAPHICS_MODE) {
            if (!(val instanceof CapabilityValue.EnumValue)) {
                return new ValidationResult.Invalid(
                        cap + " requires EnumValue, got " + val.getClass().getSimpleName());
            }
            return new ValidationResult.Valid();
        }

        // Boolean capabilities
        if (cap == CapabilityId.ENTITY_SHADOWS || cap == CapabilityId.VSYNC ||
                cap == CapabilityId.SMOOTH_LIGHTING) {
            if (!(val instanceof CapabilityValue.BoolValue)) {
                return new ValidationResult.Invalid(
                        cap + " requires BoolValue, got " + val.getClass().getSimpleName());
            }
            return new ValidationResult.Valid();
        }

        // Float capabilities
        if (cap == CapabilityId.RESOLUTION_SCALE) {
            if (!(val instanceof CapabilityValue.FloatValue)) {
                return new ValidationResult.Invalid(
                        cap + " requires FloatValue, got " + val.getClass().getSimpleName());
            }
            return new ValidationResult.Valid();
        }

        // FOG is special (depends on mod)
        if (cap == CapabilityId.FOG) {
            return new ValidationResult.Valid(); // Type determined by provider
        }

        return new ValidationResult.Invalid("Unknown capability: " + cap);
    }

    private static ValidationResult validateBenchmarkType(Command.RunBenchmark cmd) {
        if (cmd.durationSeconds() <= 0) {
            return new ValidationResult.Invalid("Benchmark duration must be positive");
        }
        return new ValidationResult.Valid();
    }

    /**
     * Validate value ranges.
     */
    private static ValidationResult validateRange(Command command) {
        if (command instanceof Command.ApplyCapability) {
            return validateApplyRange((Command.ApplyCapability) command);
        }
        return new ValidationResult.Valid();
    }

    private static ValidationResult validateApplyRange(Command.ApplyCapability cmd) {
        CapabilityId cap = cmd.capability();
        CapabilityValue val = cmd.value();

        if (cap == CapabilityId.RENDER_DISTANCE) {
            int v = ((CapabilityValue.IntValue) val).value();
            if (v < 2 || v > 32) {
                return new ValidationResult.Invalid("Render distance must be 2-32, got " + v);
            }
            return new ValidationResult.Valid();
        }

        if (cap == CapabilityId.SIMULATION_DISTANCE) {
            int v = ((CapabilityValue.IntValue) val).value();
            if (v < 2 || v > 32) {
                return new ValidationResult.Invalid("Simulation distance must be 2-32, got " + v);
            }
            return new ValidationResult.Valid();
        }

        if (cap == CapabilityId.BIOME_BLEND) {
            int v = ((CapabilityValue.IntValue) val).value();
            if (v < 0 || v > 15 || (v % 2 != 1 && v != 0)) {
                return new ValidationResult.Invalid("Biome blend must be 0 or odd 1-15, got " + v);
            }
            return new ValidationResult.Valid();
        }

        if (cap == CapabilityId.FPS_CAP) {
            int v = ((CapabilityValue.IntValue) val).value();
            if ((v < 30 && v != 0) || v > 260) {
                return new ValidationResult.Invalid("FPS cap must be 0 (unlimited) or 30-260, got " + v);
            }
            return new ValidationResult.Valid();
        }

        if (cap == CapabilityId.RESOLUTION_SCALE) {
            float v = ((CapabilityValue.FloatValue) val).value();
            if (v < 0.25f || v > 1.0f) {
                return new ValidationResult.Invalid("Resolution scale must be 0.25-1.0, got " + v);
            }
            return new ValidationResult.Valid();
        }

        if (cap == CapabilityId.PARTICLES) {
            String v = ((CapabilityValue.EnumValue) val).name();
            if (!v.equals("ALL") && !v.equals("DECREASED") && !v.equals("MINIMAL")) {
                return new ValidationResult.Invalid("Particles must be ALL/DECREASED/MINIMAL, got " + v);
            }
            return new ValidationResult.Valid();
        }

        if (cap == CapabilityId.CLOUDS) {
            String v = ((CapabilityValue.EnumValue) val).name();
            if (!v.equals("FANCY") && !v.equals("FAST") && !v.equals("OFF")) {
                return new ValidationResult.Invalid("Clouds must be FANCY/FAST/OFF, got " + v);
            }
            return new ValidationResult.Valid();
        }

        return new ValidationResult.Valid(); // Other capabilities validated by providers
    }

    /**
     * Validate against current mode (SafeMode, GovernorMode).
     */
    private static ValidationResult validateMode(Command command, RuntimeState state) {
        // SafeMode blocks ALL capability changes
        if (state.safeMode()) {
            if (command instanceof Command.ApplyCapability) {
                return new ValidationResult.Invalid("SafeMode active: capability changes blocked");
            }
            if (command instanceof Command.RunBenchmark) {
                return new ValidationResult.Invalid("SafeMode active: benchmark blocked");
            }
        }

        // Benchmark blocks other commands
        if (state.benchmarkRunning()) {
            if (!(command instanceof Command.RunBenchmark)) {
                return new ValidationResult.Invalid("Benchmark running: other commands blocked");
            }
        }

        return new ValidationResult.Valid();
    }

    // Helper to convert Preview to Apply for type checking
    private static Command.ApplyCapability toApply(Command.PreviewCapability cmd) {
        return new Command.ApplyCapability(cmd.id(), cmd.capability(), cmd.value());
    }
}
