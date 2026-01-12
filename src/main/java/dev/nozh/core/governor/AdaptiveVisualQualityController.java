package dev.nozh.core.governor;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.state.BaselineSnapshot;
import dev.nozh.core.state.RuntimeState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptive visual quality controller tied to frametime.
 * Applies step-based visual adjustments with hysteresis for stability.
 */
public final class AdaptiveVisualQualityController {

    public record QualityChange(
            CapabilityId capabilityId,
            CapabilityValue targetValue,
            int nextStep,
            String reason) {
    }

    private record QualityStep(CapabilityId capabilityId, CapabilityValue targetValue) {
    }

    // Timing constants
    private static final long MIN_DOWNSHIFT_INTERVAL_MS = 10_000L; // 10s between quality reductions
    private static final long MIN_UPSHIFT_INTERVAL_MS = 30_000L;   // 30s of stability before quality increase
    private static final long STABILITY_WINDOW_MS = 60_000L;       // 1 minute stability for full recovery
    
    // Streak requirements (hysteresis)
    private static final int REQUIRED_DOWNSHIFT_STREAK = 2;  // Quick to reduce quality
    private static final int REQUIRED_UPSHIFT_STREAK = 4;    // Slow to restore quality (hysteresis)
    
    // Threshold multipliers (hysteresis: easier to drop, harder to raise)
    private static final double DOWNSHIFT_THRESHOLD_MULT = 1.0;  // Standard threshold to drop
    private static final double UPSHIFT_THRESHOLD_MULT = 1.5;    // 50% more headroom needed to raise

    private static final List<QualityStep> DEFAULT_STEPS = List.of(
            new QualityStep(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false)),
            new QualityStep(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FAST")),
            new QualityStep(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED")),
            new QualityStep(CapabilityId.DISTORTION_EFFECT_SCALE, new CapabilityValue.FloatValue(0.5f)),
            new QualityStep(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false)),
            new QualityStep(CapabilityId.SMOOTH_LIGHTING, new CapabilityValue.BoolValue(false)),
            new QualityStep(CapabilityId.MIPMAP_LEVEL, new CapabilityValue.IntValue(2)),
            new QualityStep(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(10)),
            new QualityStep(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(80)),
            new QualityStep(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(6)),
            new QualityStep(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL")),
            new QualityStep(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("OFF")),
            new QualityStep(CapabilityId.MIPMAP_LEVEL, new CapabilityValue.IntValue(1)),
            new QualityStep(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(8)),
            new QualityStep(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(60)),
            new QualityStep(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(4)),
            new QualityStep(CapabilityId.DISTORTION_EFFECT_SCALE, new CapabilityValue.FloatValue(0.0f)));

    private int currentStep = 0;
    private long lastDownshiftMillis = 0L;
    private long lastUpshiftMillis = 0L;
    private long lastStabilityBreakMillis = 0L;  // Track when stability was last broken
    private int downshiftStreak = 0;
    private int upshiftStreak = 0;
    private boolean initialized = false;

    public static int totalSteps() {
        return DEFAULT_STEPS.size();
    }

    public Optional<QualityChange> evaluate(RuntimeState state, NozhConfig config, ProviderRegistry registry,
            long nowMillis) {
        if (state == null || config == null || registry == null) {
            return Optional.empty();
        }
        if (!config.adaptiveVisualQualityEnabled) {
            return Optional.empty();
        }
        if (state.safeMode() || !state.autoTuning() || !config.allowAutoTuning) {
            return Optional.empty();
        }
        if (state.pendingAction().isPresent()) {
            return Optional.empty();
        }

        double frametimeMs = resolveFrametimeMs(state);
        if (frametimeMs <= 0) {
            return Optional.empty();
        }

        int maxStep = Math.min(config.adaptiveVisualQualityMaxStep, DEFAULT_STEPS.size());
        int minStep = Math.max(0, Math.min(config.adaptiveVisualQualityMinStep, maxStep));
        syncStepFromSettings(state.currentSettings(), minStep, maxStep);

        double targetFrameMs = 1000.0 / Math.max(1, config.targetFps);
        double sensitivityMs = clamp(config.adaptiveVisualQualitySensitivityMs, 0.25, 8.0);
        
        // Asymmetric thresholds (hysteresis)
        double downshiftThreshold = targetFrameMs + (sensitivityMs * DOWNSHIFT_THRESHOLD_MULT);
        double upshiftThreshold = targetFrameMs - (sensitivityMs * UPSHIFT_THRESHOLD_MULT);

        // Track streaks
        if (frametimeMs > downshiftThreshold) {
            downshiftStreak++;
            upshiftStreak = 0;
            lastStabilityBreakMillis = nowMillis;
        } else if (frametimeMs < upshiftThreshold) {
            upshiftStreak++;
            downshiftStreak = 0;
        } else {
            // In neutral zone - decay streaks slowly
            downshiftStreak = Math.max(0, downshiftStreak - 1);
            // Keep upshift streak if still in good range
        }

        // === DOWNSHIFT: Quick response to performance issues ===
        if (downshiftStreak >= REQUIRED_DOWNSHIFT_STREAK && currentStep < maxStep) {
            // Check minimum interval since last downshift
            if (nowMillis - lastDownshiftMillis >= MIN_DOWNSHIFT_INTERVAL_MS) {
                return resolveDownshift(state.currentSettings(), registry, maxStep, nowMillis);
            }
        }

        // === UPSHIFT: Cautious recovery with sustained stability ===
        if (upshiftStreak >= REQUIRED_UPSHIFT_STREAK && currentStep > minStep) {
            // Check minimum interval since last upshift
            if (nowMillis - lastUpshiftMillis < MIN_UPSHIFT_INTERVAL_MS) {
                return Optional.empty();
            }
            
            // Check stability: require no stability breaks for STABILITY_WINDOW_MS
            // (unless we're at a very low quality step)
            if (currentStep > 3 && (nowMillis - lastStabilityBreakMillis) < STABILITY_WINDOW_MS) {
                return Optional.empty(); // Not stable enough yet
            }
            
            return resolveUpshift(state.currentSettings(), state.baselineSnapshot(), registry, minStep, nowMillis);
        }

        return Optional.empty();
    }

    public void onChangeApplied(int nextStep, long nowMillis) {
        boolean wasUpshift = nextStep < currentStep;
        currentStep = nextStep;
        if (wasUpshift) {
            lastUpshiftMillis = nowMillis;
        } else {
            lastDownshiftMillis = nowMillis;
        }
        downshiftStreak = 0;
        upshiftStreak = 0;
        initialized = true;
    }

    private Optional<QualityChange> resolveDownshift(Map<CapabilityId, CapabilityValue> currentSettings,
            ProviderRegistry registry, int maxStep, long nowMillis) {
        for (int index = currentStep; index < Math.min(maxStep, DEFAULT_STEPS.size()); index++) {
            QualityStep step = DEFAULT_STEPS.get(index);
            if (registry.get(step.capabilityId()).isEmpty()) {
                continue;
            }
            if (isAlreadyReduced(currentSettings, step)) {
                continue;
            }
            int nextStep = index + 1;
            String reason = String.format("Adaptive visual quality ↓ (step %d/%d)", nextStep, maxStep);
            return Optional.of(new QualityChange(step.capabilityId(), step.targetValue(), nextStep, reason));
        }
        return Optional.empty();
    }

    private Optional<QualityChange> resolveUpshift(Map<CapabilityId, CapabilityValue> currentSettings,
            BaselineSnapshot baselineSnapshot, ProviderRegistry registry, int minStep, long nowMillis) {
        int undoIndex = Math.min(currentStep - 1, DEFAULT_STEPS.size() - 1);
        for (int index = undoIndex; index >= minStep && index >= 0; index--) {
            QualityStep step = DEFAULT_STEPS.get(index);
            if (registry.get(step.capabilityId()).isEmpty()) {
                continue;
            }
            CapabilityValue target = resolveUpshiftTarget(step.capabilityId(), index - 1, baselineSnapshot);
            if (target == null) {
                continue;
            }
            if (!isHigherQuality(currentSettings, step.capabilityId(), target)) {
                continue;
            }
            int nextStep = index;
            String reason = String.format("Adaptive visual quality ↑ (step %d/%d) - stable %.1fs", 
                nextStep, currentStep, (nowMillis - lastStabilityBreakMillis) / 1000.0);
            return Optional.of(new QualityChange(step.capabilityId(), target, nextStep, reason));
        }
        return Optional.empty();
    }

    private void syncStepFromSettings(Map<CapabilityId, CapabilityValue> currentSettings, int minStep, int maxStep) {
        if (!initialized) {
            currentStep = resolveStepFromSettings(currentSettings);
            initialized = true;
        }
        if (currentStep < minStep) {
            currentStep = minStep;
        }
        if (currentStep > maxStep) {
            currentStep = maxStep;
        }
    }

    private int resolveStepFromSettings(Map<CapabilityId, CapabilityValue> currentSettings) {
        if (currentSettings == null || currentSettings.isEmpty()) {
            return 0;
        }
        int applied = 0;
        for (int i = 0; i < DEFAULT_STEPS.size(); i++) {
            QualityStep step = DEFAULT_STEPS.get(i);
            if (!isAlreadyReduced(currentSettings, step)) {
                break;
            }
            applied = i + 1;
        }
        return applied;
    }

    private boolean isAlreadyReduced(Map<CapabilityId, CapabilityValue> currentSettings, QualityStep step) {
        if (currentSettings == null) {
            return false;
        }
        CapabilityValue current = currentSettings.get(step.capabilityId());
        if (current == null) {
            return false;
        }
        return isLowerOrEqualQuality(step.capabilityId(), current, step.targetValue());
    }

    private boolean isHigherQuality(Map<CapabilityId, CapabilityValue> currentSettings, CapabilityId id,
            CapabilityValue target) {
        CapabilityValue current = currentSettings != null ? currentSettings.get(id) : null;
        if (current == null) {
            return true;
        }
        return isHigherQuality(id, current, target);
    }

    private boolean isLowerOrEqualQuality(CapabilityId id, CapabilityValue current, CapabilityValue target) {
        if (current == null || target == null) {
            return false;
        }
        if (current.equals(target)) {
            return true;
        }
        return switch (id) {
            case PARTICLES -> compareEnum(current, target, List.of("MINIMAL", "DECREASED", "ALL")) <= 0;
            case CLOUDS -> compareEnum(current, target, List.of("OFF", "FAST", "FANCY")) <= 0;
            case GRAPHICS_MODE -> compareEnum(current, target, List.of("FAST", "FANCY", "FABULOUS")) <= 0;
            case ENTITY_SHADOWS, VSYNC, SMOOTH_LIGHTING, DYNAMIC_LIGHTING, ANIMATIONS, ARMOR_STANDS, ITEM_FRAMES,
                    BLOCK_ENTITIES -> compareBool(current, target) <= 0;
            case RENDER_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, BIOME_BLEND, MIPMAP_LEVEL, FOG ->
                    compareInt(current, target) <= 0;
            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> compareFloat(current, target) <= 0;
            default -> false;
        };
    }

    private boolean isHigherQuality(CapabilityId id, CapabilityValue current, CapabilityValue target) {
        if (current == null || target == null) {
            return false;
        }
        if (current.equals(target)) {
            return false;
        }
        return switch (id) {
            case PARTICLES -> compareEnum(current, target, List.of("MINIMAL", "DECREASED", "ALL")) > 0;
            case CLOUDS -> compareEnum(current, target, List.of("OFF", "FAST", "FANCY")) > 0;
            case GRAPHICS_MODE -> compareEnum(current, target, List.of("FAST", "FANCY", "FABULOUS")) > 0;
            case ENTITY_SHADOWS, VSYNC, SMOOTH_LIGHTING, DYNAMIC_LIGHTING, ANIMATIONS, ARMOR_STANDS, ITEM_FRAMES,
                    BLOCK_ENTITIES -> compareBool(current, target) > 0;
            case RENDER_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, BIOME_BLEND, MIPMAP_LEVEL, FOG ->
                    compareInt(current, target) > 0;
            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> compareFloat(current, target) > 0;
            default -> false;
        };
    }

    private int compareEnum(CapabilityValue current, CapabilityValue target, List<String> ordering) {
        if (!(current instanceof CapabilityValue.EnumValue currentEnum)
                || !(target instanceof CapabilityValue.EnumValue targetEnum)) {
            return 0;
        }
        int currentIndex = ordering.indexOf(currentEnum.name());
        int targetIndex = ordering.indexOf(targetEnum.name());
        if (currentIndex < 0 || targetIndex < 0) {
            return 0;
        }
        return Integer.compare(currentIndex, targetIndex);
    }

    private int compareBool(CapabilityValue current, CapabilityValue target) {
        if (!(current instanceof CapabilityValue.BoolValue currentBool)
                || !(target instanceof CapabilityValue.BoolValue targetBool)) {
            return 0;
        }
        return Boolean.compare(currentBool.value(), targetBool.value());
    }

    private int compareInt(CapabilityValue current, CapabilityValue target) {
        if (!(current instanceof CapabilityValue.IntValue currentInt)
                || !(target instanceof CapabilityValue.IntValue targetInt)) {
            return 0;
        }
        return Integer.compare(currentInt.value(), targetInt.value());
    }

    private int compareFloat(CapabilityValue current, CapabilityValue target) {
        if (!(current instanceof CapabilityValue.FloatValue currentFloat)
                || !(target instanceof CapabilityValue.FloatValue targetFloat)) {
            return 0;
        }
        return Double.compare(currentFloat.value(), targetFloat.value());
    }

    private CapabilityValue resolveUpshiftTarget(CapabilityId id, int previousIndex, BaselineSnapshot baselineSnapshot) {
        if (previousIndex >= 0) {
            for (int i = previousIndex; i >= 0; i--) {
                QualityStep step = DEFAULT_STEPS.get(i);
                if (step.capabilityId() == id) {
                    return step.targetValue();
                }
            }
        }
        if (baselineSnapshot != null && !baselineSnapshot.isEmpty()) {
            return baselineSnapshot.get(id).orElse(null);
        }
        return null;
    }

    private double resolveFrametimeMs(RuntimeState state) {
        double avg = state.avgFrametimeMs();
        if (avg > 0) {
            return avg;
        }
        double p95 = state.p95FrametimeMs();
        if (p95 > 0) {
            return p95;
        }
        return -1.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
