package dev.nozh.core.optimization;

import dev.nozh.api.Scenario;
import dev.nozh.NozhConstants;

/**
 * Dynamic quality scaling like modern games.
 * Adjusts quality in real-time to maintain target FPS.
 * 
 * <p>
 * <b>New Features (Ultimate):</b>
 * <ul>
 * <li><b>Asymmetric Hysteresis</b> - Harder to upgrade quality than to
 * downgrade (prevents flapping)</li>
 * <li><b>Stability Window</b> - Requires sustained stability before
 * upgrading</li>
 * <li><b>Smart Interpolation</b> - Non-linear smoothing for seamless
 * transitions</li>
 * </ul>
 * 
 * @since 0.3.1
 * @author NOZH Team
 */
public final class AdaptiveQualityScaler {

    public enum ScalingMode {
        QUALITY(1.2, 0.8),
        BALANCED(1.0, 1.0),
        PERFORMANCE(0.8, 1.3),
        POTATO(0.5, 2.0);

        public final double qualityMultiplier;
        public final double performanceMultiplier;

        ScalingMode(double qualityMult, double perfMult) {
            this.qualityMultiplier = qualityMult;
            this.performanceMultiplier = perfMult;
        }
    }

    public record QualityLevel(
            int renderDistance,
            int entityDistance,
            double particleLevel,
            double shadowQuality,
            double scaleFactor) {

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

        public QualityLevel lerp(QualityLevel other, double t) {
            t = dev.nozh.core.util.MathOptimizer.clamp(t, 0.0, 1.0);

            return new QualityLevel(
                    (int) dev.nozh.core.util.MathOptimizer.lerp(renderDistance, other.renderDistance, t),
                    (int) dev.nozh.core.util.MathOptimizer.lerp(entityDistance, other.entityDistance, t),
                    dev.nozh.core.util.MathOptimizer.lerp(particleLevel, other.particleLevel, t),
                    dev.nozh.core.util.MathOptimizer.lerp(shadowQuality, other.shadowQuality, t),
                    dev.nozh.core.util.MathOptimizer.lerp(scaleFactor, other.scaleFactor, t));
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

    // --- Advanced Hysteresis State ---
    private int stabilityCounter; // Counts continuous frames of good performance
    private int stressCounter; // Counts continuous frames of bad performance

    // Asymmetric Thresholds (in frames @ 60fps)
    // Harder to upgrade than to downgrade to be conservative
    private static final int UPGRADE_THRESHOLD = 180; // 3 seconds stable
    private static final int DOWNGRADE_THRESHOLD = 30; // 0.5 seconds bad

    // FPS Tolerances
    private static final double DOWNGRADE_FPS_RATIO = 0.90; // Drop if < 90% target
    private static final double UPGRADE_FPS_RATIO = 1.15; // Upgrade only if > 115% target (Headroom)

    public AdaptiveQualityScaler(ScalingMode mode, double targetFps) {
        this.mode = mode;
        this.targetFps = targetFps;
        this.currentQuality = QualityLevel.fromScale(0.7); // Start at 70%
        this.targetQuality = currentQuality;
        this.interpolationProgress = 1.0;
        this.recentFps = targetFps;
        this.stabilityCounter = 0;
        this.stressCounter = 0;
    }

    public void update(double currentFps) {
        this.recentFps = currentFps;

        // 1. Analyze Performance vs Target
        double ratio = currentFps / targetFps;

        if (ratio < DOWNGRADE_FPS_RATIO) {
            // Bad performance
            stressCounter++;
            stabilityCounter = 0; // Reset stability
        } else if (ratio > UPGRADE_FPS_RATIO) {
            // Good performance (with headroom)
            stabilityCounter++;
            stressCounter = Math.max(0, stressCounter - 1); // Decay stress
        } else {
            // Neutral zone - maintenance
            // Slowly decay both counters
            if (stabilityCounter > 0)
                stabilityCounter--;
            if (stressCounter > 0)
                stressCounter--;
        }

        // 2. Trigger Quality Updates based on Hysteresis
        if (stressCounter >= DOWNGRADE_THRESHOLD) {
            decreaseQuality();
            stressCounter = 0; // Reset after action
        } else if (stabilityCounter >= UPGRADE_THRESHOLD) {
            increaseQuality();
            stabilityCounter = 0; // Reset after action
        }

        // 3. Update Smooth Interpolation
        if (interpolationProgress < 1.0) {
            // Use smoothstep for nicer transitions
            interpolationProgress = Math.min(1.0, interpolationProgress + INTERPOLATION_SPEED);
        }
    }

    public QualityLevel calculateOptimalQuality() {
        // This method is now mostly for "what if" scenarios or initialization
        // since real-time updates happen in update() via hysteresis

        double fpsRatio = recentFps / targetFps;
        double idealScale = currentQuality.scaleFactor();

        if (fpsRatio < 0.9) {
            idealScale *= Math.pow(fpsRatio / 0.9, mode.performanceMultiplier);
        } else if (fpsRatio > 1.1) {
            idealScale *= Math.pow(Math.min(fpsRatio / 1.1, 1.2), mode.qualityMultiplier);
        }

        idealScale = applyModeConstraints(idealScale);
        return QualityLevel.fromScale(idealScale);
    }

    private double applyModeConstraints(double scale) {
        return switch (mode) {
            case QUALITY -> Math.max(0.6, Math.min(1.0, scale));
            case BALANCED -> Math.max(0.4, Math.min(1.0, scale));
            case PERFORMANCE -> Math.max(0.2, Math.min(0.8, scale));
            case POTATO -> Math.max(0.1, Math.min(0.5, scale));
        };
    }

    public QualityLevel getInterpolatedQuality() {
        if (interpolationProgress >= 1.0) {
            return currentQuality;
        }

        // Smoothstep interpolation
        double t = interpolationProgress;
        double smoothT = t * t * (3 - 2 * t);

        return currentQuality.lerp(targetQuality, smoothT);
    }

    public QualityLevel predictRequiredQuality(Scenario scenario) {
        double baseFactor = currentQuality.scaleFactor();

        // Adjust based on scenario demands
        double adjustment = switch (scenario) {
            case COMBAT -> -0.15;
            case HIGH_ENTITY_DENSITY -> -0.20;
            case WORLD_LOADING -> -0.10;
            case EXPLORATION -> -0.05;
            case BUILDING, MINING -> 0.05;
            case IDLE, AFK, MENU -> 0.10;
            default -> 0.0;
        };

        double predictedScale = applyModeConstraints(baseFactor + adjustment);
        return QualityLevel.fromScale(predictedScale);
    }

    private void decreaseQuality() {
        // Drop quality faster than raising it (Safety first)
        double newScale = currentQuality.scaleFactor() * 0.90; // -10%
        newScale = applyModeConstraints(newScale);

        if (Math.abs(newScale - currentQuality.scaleFactor()) > 0.01) {
            // Only update if change is significant

            // Note: In a real engine, we might want to "latch" the current interpolated
            // value
            // as the new start point to avoid jumps, but here we update target

            // If we were already interpolating towards a target, assume we reached it to
            // avoid complex math
            // or just update target and reset progress

            // Optimization: If we are currently upgrading, CANCEL it immediately
            if (targetQuality.scaleFactor() > currentQuality.scaleFactor()) {
                targetQuality = currentQuality; // Stop upgrading
            }

            // Set new lower target
            QualityLevel newTarget = QualityLevel.fromScale(newScale);

            // If we are already lower than current target, go lower
            if (newTarget.scaleFactor() < targetQuality.scaleFactor()) {
                currentQuality = getInterpolatedQuality(); // Snapshot current state
                targetQuality = newTarget;
                interpolationProgress = 0.0;

                NozhConstants.LOGGER.info("Decreasing quality (perf drop): {} -> {}",
                        currentQuality.scaleFactor(), targetQuality.scaleFactor());
            }
        }
    }

    private void increaseQuality() {
        // Raise quality slowly (Conservative)
        double newScale = currentQuality.scaleFactor() * 1.05; // +5%
        newScale = applyModeConstraints(newScale);

        if (Math.abs(newScale - currentQuality.scaleFactor()) > 0.01) {
            QualityLevel newTarget = QualityLevel.fromScale(newScale);

            // Only apply if higher than current target
            if (newTarget.scaleFactor() > targetQuality.scaleFactor()) {
                currentQuality = getInterpolatedQuality(); // Snapshot
                targetQuality = newTarget;
                interpolationProgress = 0.0;

                NozhConstants.LOGGER.info("Increasing quality (stable): {} -> {}",
                        currentQuality.scaleFactor(), targetQuality.scaleFactor());
            }
        }
    }

    public void setMode(ScalingMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            // Recalculate limits immediately
            double newScale = applyModeConstraints(currentQuality.scaleFactor());
            targetQuality = QualityLevel.fromScale(newScale);
            interpolationProgress = 0.0;
            NozhConstants.LOGGER.info("Scaling mode changed to: {}", mode);
        }
    }

    public void setTargetFps(double targetFps) {
        this.targetFps = Math.max(30, Math.min(240, targetFps));
    }

    public ScalingMode getMode() {
        return mode;
    }

    public QualityLevel getCurrentQuality() {
        return currentQuality;
    }

    public QualityLevel getTargetQuality() {
        return targetQuality;
    }

    public void snapToTarget() {
        currentQuality = targetQuality;
        interpolationProgress = 1.0;
    }

    public void reset() {
        currentQuality = QualityLevel.fromScale(0.7);
        targetQuality = currentQuality;
        interpolationProgress = 1.0;
        stabilityCounter = 0;
        stressCounter = 0;

        NozhConstants.LOGGER.info("Quality scaler reset");
    }
}
