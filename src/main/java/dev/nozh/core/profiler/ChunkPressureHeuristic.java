package dev.nozh.core.profiler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Heuristic for chunk loading pressure based on player movement.
 * Fast movement = more chunks loading = more IO pressure.
 * 
 * This is a simple proxy since we don't have direct access to chunk loading metrics.
 */
public class ChunkPressureHeuristic {
    
    private Vec3d lastPosition = null;
    private double recentSpeed = 0;
    private double pressureScore = 0;
    
    // Speed thresholds (blocks per tick)
    private static final double WALKING_SPEED = 0.2;  // ~4 blocks/sec
    private static final double SPRINTING_SPEED = 0.3; // ~6 blocks/sec  
    private static final double FLYING_SPEED = 0.5;   // ~10 blocks/sec
    private static final double ELYTRA_SPEED = 1.5;   // ~30 blocks/sec
    
    // Smoothing factor for speed (0-1, higher = more responsive)
    private static final double SPEED_SMOOTHING = 0.3;
    
    /**
     * Update the heuristic. Call once per tick.
     */
    public void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        
        PlayerEntity player = client.player;
        Vec3d currentPos = player.getPos();
        
        if (lastPosition != null) {
            // Calculate horizontal speed (XZ plane, ignore Y for chunk loading)
            double dx = currentPos.x - lastPosition.x;
            double dz = currentPos.z - lastPosition.z;
            double instantSpeed = Math.sqrt(dx * dx + dz * dz);
            
            // Smooth the speed measurement
            recentSpeed = (recentSpeed * (1 - SPEED_SMOOTHING)) + (instantSpeed * SPEED_SMOOTHING);
            
            // Calculate pressure score based on speed
            // Faster movement = more new chunks = more pressure
            pressureScore = calculatePressureFromSpeed(recentSpeed);
        }
        
        lastPosition = currentPos;
    }
    
    private double calculatePressureFromSpeed(double speed) {
        // Scale speed to pressure (0-1)
        if (speed < WALKING_SPEED) {
            return 0;
        } else if (speed < SPRINTING_SPEED) {
            return 0.2;
        } else if (speed < FLYING_SPEED) {
            return 0.4;
        } else if (speed < ELYTRA_SPEED) {
            return 0.6;
        } else {
            // Very fast (teleport, extreme elytra)
            return Math.min(1.0, 0.6 + (speed - ELYTRA_SPEED) * 0.2);
        }
    }
    
    /**
     * Get current chunk pressure score (0-1)
     * 0 = stationary, low pressure
     * 1 = very fast movement, high chunk loading
     */
    public double getPressureScore() {
        return pressureScore;
    }
    
    /**
     * Get recent player speed (blocks per tick)
     */
    public double getRecentSpeed() {
        return recentSpeed;
    }
    
    /**
     * Check if player is moving fast enough to cause IO pressure
     */
    public boolean isHighPressure() {
        return pressureScore > 0.5;
    }
    
    /**
     * Reset tracking
     */
    public void reset() {
        lastPosition = null;
        recentSpeed = 0;
        pressureScore = 0;
    }
}
