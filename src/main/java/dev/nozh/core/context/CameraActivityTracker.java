package dev.nozh.core.context;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Tracks camera movement and FOV changes for scenario detection.
 * 
 * High camera activity = combat or exploring
 * Low camera activity = building or AFK
 * FOV changes = sprint/elytra usage
 * 
 * TASK 3: Scenario confidence - camera signals
 */
public final class CameraActivityTracker {

    private final MinecraftClient client;

    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private double lastFov = 70.0;

    private double cameraRotationSpeed = 0.0; // degrees/tick
    private double fovChangeRate = 0.0; // delta/tick

    private static final double HIGH_ROTATION_THRESHOLD = 5.0; // degrees/tick
    private static final double SMOOTHING_FACTOR = 0.3;

    public CameraActivityTracker(MinecraftClient client) {
        this.client = client;
    }

    /**
     * Update camera tracking. Call every tick.
     */
    public void tick() {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();
        double currentFov = client.options.getFov().getValue();

        // Calculate rotation delta
        float yawDelta = Math.abs(angleDifference(currentYaw, lastYaw));
        float pitchDelta = Math.abs(currentPitch - lastPitch);
        double rotationDelta = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);

        // Smooth rotation speed
        cameraRotationSpeed = SMOOTHING_FACTOR * rotationDelta 
                            + (1.0 - SMOOTHING_FACTOR) * cameraRotationSpeed;

        // Calculate FOV change
        double fovDelta = Math.abs(currentFov - lastFov);
        fovChangeRate = SMOOTHING_FACTOR * fovDelta 
                      + (1.0 - SMOOTHING_FACTOR) * fovChangeRate;

        // Update state
        lastYaw = currentYaw;
        lastPitch = currentPitch;
        lastFov = currentFov;
    }

    /**
     * Get current camera rotation speed (degrees/tick).
     */
    public double getRotationSpeed() {
        return cameraRotationSpeed;
    }

    /**
     * Check if camera is moving rapidly (combat/exploring).
     */
    public boolean isHighActivity() {
        return cameraRotationSpeed > HIGH_ROTATION_THRESHOLD;
    }

    /**
     * Check if camera is mostly still (building/AFK).
     */
    public boolean isLowActivity() {
        return cameraRotationSpeed < 0.5;
    }

    /**
     * Get FOV change rate.
     */
    public double getFovChangeRate() {
        return fovChangeRate;
    }

    /**
     * Check if FOV is changing (sprinting/elytra).
     */
    public boolean isFovChanging() {
        return fovChangeRate > 0.1;
    }

    /**
     * Calculate shortest angle difference (handles 360° wrap).
     */
    private float angleDifference(float a, float b) {
        float diff = a - b;
        while (diff > 180.0f) diff -= 360.0f;
        while (diff < -180.0f) diff += 360.0f;
        return diff;
    }

    /**
     * Reset tracker state.
     */
    public void reset() {
        cameraRotationSpeed = 0.0;
        fovChangeRate = 0.0;
    }
}
