package dev.nozh.core.optimization;

import dev.nozh.api.Scenario;
import dev.nozh.NozhConstants;

/**
 * Dynamic quality scaling like modern games.
 * Adjusts quality in real-time to maintain target FPS.
 * 
 * <p>
 * Quality levels affect multiple systems:
 * <ul>
 * <li><b>Primary</b>: Render distance (biggest FPS impact)</li>
 * <li><b>Secondary</b>: Entity distance simulations</li>
 * <li><b>Tertiary</b>: Particle quality, shadow detail</li>
 * </ul>
 * 
 * <p>
 * Scaling modes balance visual quality vs performance:
 * <ul>
 * <li><b>QUALITY</b>: Prioritize visuals, accept FPS drops</li>
 * <li><b>BALANCED</b>: 50/50 trade-off (default)</li>
 * <li><b>PERFORMANCE</b>: Prioritize FPS, reduce quality</li>
 * <li><b>POTATO</b>: Minimum quality for maximum FPS</li>
 * </ul>
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class AdaptiveQualityScaler {

    /**
     * Scaling mode determining quality vs performance trade-off.
     */
    public enum ScalingMode {
        /** Prioritize visual quality, accept FPS drops */
        QUALITY(1.2, 0.8),

        /** Balanced trade-off between quality and performance */
        BALANCED(1.0, 1.0),

        /** Prioritize performance, reduce quality aggressively */
        PERFORMANCE(0.8, 1.3),

        /** Minimum quality for maximum FPS on weak hardware */
        POTATO(0.5, 2.0);

        /** Quality multiplier (higher = better visuals) */
        public final double qualityMultiplier;

        /** Performance multiplier (higher = more aggressive optimizations) */
        public final double performanceMultiplier;

        ScalingMode(double qualityMult, double perfMult) {
            this.qualityMultiplier = qualityMult;
            this.performanceMultiplier = perfMult;
        }
    }

    /**
     * Quality level configuration.
     * 
     * @param renderDistance view distance in chunks
     * @param entityDistance entity simulation distance in chunks
     * @param particleLevel  particle quality (0.0 = off, 1.0 = all)
     * @param shadowQuality  shadow detail (0.0 = off, 1.0 = highest)
     * @param scaleFactor    overall quality scale (0.0 to 1.0)
     */
    public record QualityLevel(
            int renderDistance,
            int entityDistance,
            double particleLevel,
            double shadowQuality,
            double scaleFactor) {
        /**
         * Creates quality level from scale factor.
         * 
         * @param factor quality factor (0.0 to 1.0)
         * @return quality level
         */
        public static QualityLevel fromScale(double factor) {
            factor = Math.max(0.0, Math.min(1.0, factor));

            // Map scale to render distance (4 to 32 chunks)
            int renderDist = (int) (4 + factor * 28);

            // Entity distance scales with render distance
            int entityDist = Math.max(3, renderDist - 4);

            // Particle quality scales linearly
            double particles = factor;

            // Shadow quality has threshold (off below 0.3)
            double shadows = factor < 0.3 ? 0.0 : (factor - 0.3) / 0.7;

            return new QualityLevel(renderDist, entityDist, particles, shadows, factor);
        }

        /**
         * Lerps between two quality levels.
         * 
         * @param other target quality level
         * @param t     interpolation factor (0.0 to 1.0)
         * @return interpolated quality level
         */
        public QualityLevel lerp(QualityLevel other, double t) {
            t = Math.max(0.0, Math.min(1.0, t));

            return new QualityLevel(
                    (int) (renderDistance + (other.renderDistance - renderDistance) * t),
                    (int) (entityDistance + (other.entityDistance - entityDistance) * t),
                    particleLevel + (other.particleLevel - particleLevel) * t,
                    shadowQuality + (other.shadowQuality - shadowQuality) * t,
                    scaleFactor + (other.scaleFactor - scaleFactor) * t);
        }
    }

    // Current state
    private ScalingMode mode;
    private QualityLevel currentQuality;
    private QualityLevel targetQuality;

    // Interpolation state
    private double interpolationProgress;
    private static final double INTERPOLATION_SPEED = 0.05; // 5% per frame

    // Performance tracking
    private double recentFps;
    private double targetFps;
    private int framesAboveTarget;
    private int framesBelowTarget;

    // Hysteresis to prevent oscillation
    private static final int HYSTERESIS_THRESHOLD = 60; // 1 second at 60fps
    private static final double FPS_TOLERANCE = 0.95; // 5% tolerance

    /**
     * Constructs a new AdaptiveQualityScaler.
     * 
     * @param mode      scaling mode
     * @param targetFps target frames per second
     */
    public AdaptiveQualityScaler(ScalingMode mode, double targetFps) {
        this.mode = mode;
        this.targetFps = targetFps;
        this.currentQuality = QualityLevel.fromScale(0.7); // Start at 70%
        this.targetQuality = currentQuality;
        this.interpolationProgress = 1.0;
        this.recentFps = targetFps;
        this.framesAboveTarget = 0;
        this.framesBelowTarget = 0;
    }

    /**
     * Updates scaler with current performance metrics.
     * 
     * <p>
     * Call this every frame to track performance and adjust quality.
     * 
     * @param currentFps current frames per second
     */
    public void update(double currentFps) {
        this.recentFps = currentFps;

        // Track FPS consistency
        if (currentFps >= targetFps * FPS_TOLERANCE) {
            framesAboveTarget++;
            framesBelowTarget = 0;
        } else {
            framesBelowTarget++;
            framesAboveTarget = 0;
        }

        // Update interpolation
        if (interpolationProgress < 1.0) {
            interpolationProgress = Math.min(1.0, interpolationProgress + INTERPOLATION_SPEED);
        }

        // Check if quality adjustment needed
        if (framesBelowTarget >= HYSTERESIS_THRESHOLD) {
            // Performance is bad, reduce quality
            decreaseQuality();
        } else if (framesAboveTarget >= HYSTERESIS_THRESHOLD * 2) {
            // Performance is good, can increase quality
            increaseQuality();
        }
    }

    /**
     * Calculates optimal quality for current performance.
     * 
     * @return optimal quality level
     */
    public QualityLevel calculateOptimalQuality() {
        // Calculate ideal scale based on FPS ratio
        double fpsRatio = recentFps / targetFps;
        double idealScale = currentQuality.scaleFactor();

        if (fpsRatio < 0.9) {
            // Reduce quality proportionally to FPS deficit
            idealScale *= Math.pow(fpsRatio / 0.9, mode.performanceMultiplier);
        } else if (fpsRatio > 1.1) {
            // Can afford to increase quality
            idealScale *= Math.pow(Math.min(fpsRatio / 1.1, 1.2), mode.qualityMultiplier);
        }

        // Apply mode-specific constraints
        idealScale = applyModeConstraints(idealScale);

        return QualityLevel.fromScale(idealScale);
    }

    /**
     * Applies scaling mode constraints to quality scale.
     * 
     * @param scale raw quality scale
     * @return constrained scale
     */
    private double applyModeConstraints(double scale) {
        return switch (mode) {
            case QUALITY -> Math.max(0.6, Math.min(1.0, scale)); // Never below 60%
            case BALANCED -> Math.max(0.4, Math.min(1.0, scale)); // 40-100%
            case PERFORMANCE -> Math.max(0.2, Math.min(0.8, scale)); // 20-80%
            case POTATO -> Math.max(0.1, Math.min(0.5, scale)); // 10-50%
        };
    }

    /**
     * Gets smoothly interpolated quality between current and target.
     * 
     * @return interpolated quality level
     */
    public QualityLevel getInterpolatedQuality() {
        if (interpolationProgress >= 1.0) {
            return currentQuality;
        }

        return currentQuality.lerp(targetQuality, interpolationProgress);
    }

    /**
     * Predicts quality needed for upcoming scenario.
     * 
     * <p>
     * Certain scenarios are more demanding:
     * <ul>
     * <li>COMBAT: High entity density, fast movement</li>
     * <li>HIGH_ENTITY_DENSITY: Many entities to render</li>
     * <li>EXPLORATION: New chunks loading</li>
     * </ul>
     * 
     * @param scenario upcoming scenario
     * @return predicted quality level
     */
    public QualityLevel predictRequiredQuality(Scenario scenario) {
        double baseFactor = currentQuality.scaleFactor();

        // Adjust based on scenario demands
        double adjustment = switch (scenario) {
            case COMBAT -> -0.15; // Reduce quality 15% for combat
            case HIGH_ENTITY_DENSITY -> -0.20; // Reduce 20% for many entities
            case WORLD_LOADING -> -0.10; // Reduce 10% during loading
            case EXPLORATION -> -0.05; // Slight reduction for exploration
            case BUILDING, MINING -> 0.05; // Can increase slightly
            case IDLE, AFK, MENU -> 0.10; // Can increase when idle
            default -> 0.0;
        };

        double predictedScale = applyModeConstraints(baseFactor + adjustment);
        return QualityLevel.fromScale(predictedScale);
    }

    /**
     * Decreases quality level.
     */
    private void decreaseQuality() {
        double newScale = currentQuality.scaleFactor() * 0.9; // 10% reduction
        newScale = applyModeConstraints(newScale);

        if (Math.abs(newScale - currentQuality.scaleFactor()) > 0.01) {
            targetQuality = QualityLevel.fromScale(newScale);
            interpolationProgress = 0.0;

            NozhConstants.LOGGER.info("Decreasing quality: {} -> {}",
                    currentQuality.scaleFactor(), newScale);
        }
    }

    /**
     * Increases quality level.
     */
    private void increaseQuality() {
        double newScale = currentQuality.scaleFactor() * 1.05; // 5% increase (conservative)
        newScale = applyModeConstraints(newScale);

        if (Math.abs(newScale - currentQuality.scaleFactor()) > 0.01) {
            targetQuality = QualityLevel.fromScale(newScale);
            interpolationProgress = 0.0;

            NozhConstants.LOGGER.info("Increasing quality: {} -> {}",
                    currentQuality.scaleFactor(), newScale);
        }
    }

    /**
     * Sets scaling mode.
     * 
     * @param mode new scaling mode
     */
    public void setMode(ScalingMode mode) {
        if (this.mode != mode) {
            this.mode = mode;

            // Recalculate quality with new mode
            double newScale = applyModeConstraints(currentQuality.scaleFactor());
            targetQuality = QualityLevel.fromScale(newScale);
            interpolationProgress = 0.0;

            NozhConstants.LOGGER.info("Scaling mode changed to: {}", mode);
        }
    }

    /**
     * Sets target FPS.
     * 
     * @param targetFps new target FPS
     */
    public void setTargetFps(double targetFps) {
        this.targetFps = Math.max(30, Math.min(240, targetFps));
    }

    /**
     * Gets current scaling mode.
     * 
     * @return current mode
     */
    public ScalingMode getMode() {
        return mode;
    }

    /**
     * Gets current quality level.
     * 
     * @return current quality
     */
    public QualityLevel getCurrentQuality() {
        return currentQuality;
    }

    /**
     * Gets target quality level.
     * 
     * @return target quality
     */
    public QualityLevel getTargetQuality() {
        return targetQuality;
    }

    /**
     * Advances to target quality immediately (no interpolation).
     */
    public void snapToTarget() {
        currentQuality = targetQuality;
        interpolationProgress = 1.0;
    }

    /**
     * Resets quality to default (70%).
     */
    public void reset() {
        currentQuality = QualityLevel.fromScale(0.7);
        targetQuality = currentQuality;
        interpolationProgress = 1.0;
        framesAboveTarget = 0;
        framesBelowTarget = 0;

        NozhConstants.LOGGER.info("Quality scaler reset to defaults");
    }
}
