package dev.nozh.core.scenario;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Tracks hostile entities near the player.
 * 
 * ROADMAP: Phase 2, Sprint 3 - Hostile Entity Detection
 * 
 * Calculates danger scores based on entity types, distances, and targeting.
 */
public class HostileEntityTracker {
    
    private static final double DANGER_RADIUS = 16.0; // blocks
    private static final double COMBAT_RADIUS = 8.0;  // blocks
    
    /**
     * Analyze current hostile entity context.
     */
    public HostileContext analyze(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            return HostileContext.safe();
        }
        
        Vec3d playerPos = client.player.getPos();
        Box searchBox = Box.of(playerPos, DANGER_RADIUS * 2, 
                               DANGER_RADIUS * 2, DANGER_RADIUS * 2);
        
        List<Entity> nearbyEntities = client.world.getOtherEntities(
            client.player, 
            searchBox, 
            e -> e instanceof HostileEntity
        );
        
        int hostilesNearby = nearbyEntities.size();
        
        // Count hostiles actively targeting player
        int hostilesInCombat = (int) nearbyEntities.stream()
            .filter(e -> e.distanceTo(client.player) < COMBAT_RADIUS)
            .filter(e -> e instanceof MobEntity && 
                        ((MobEntity) e).getTarget() == client.player)
            .count();
        
        double dangerScore = calculateDangerScore(nearbyEntities, client);
        
        boolean activeCombat = hostilesInCombat > 0;
        
        return new HostileContext(
            hostilesNearby,
            hostilesInCombat,
            dangerScore,
            activeCombat
        );
    }
    
    /**
     * Calculate weighted danger score.
     */
    private double calculateDangerScore(List<Entity> hostiles, MinecraftClient client) {
        return hostiles.stream()
            .mapToDouble(e -> {
                double distance = e.distanceTo(client.player);
                double threat = getThreatLevel(e);
                // Closer enemies are more dangerous
                return threat / Math.max(1.0, distance);
            })
            .sum();
    }
    
    /**
     * Get threat level for entity type.
     */
    private double getThreatLevel(Entity entity) {
        // Boss mobs
        if (entity instanceof WitherEntity || entity instanceof EnderDragonEntity) {
            return 5.0;
        }
        
        // High threat
        if (entity instanceof CreeperEntity) {
            return 2.0; // Creepers can one-shot
        }
        
        // Medium-high threat  
        if (entity instanceof EndermanEntity || entity instanceof WitherSkeletonEntity) {
            return 1.5;
        }
        
        // Standard threat (zombies, skeletons, etc.)
        return 1.0;
    }
    
    /**
     * Container for hostile entity context.
     */
    public static class HostileContext {
        private final int hostilesNearby;
        private final int hostilesInCombat;
        private final double dangerScore;
        private final boolean activeCombat;
        
        public HostileContext(int nearby, int inCombat, double danger, boolean active) {
            this.hostilesNearby = nearby;
            this.hostilesInCombat = inCombat;
            this.dangerScore = danger;
            this.activeCombat = active;
        }
        
        public static HostileContext safe() {
            return new HostileContext(0, 0, 0.0, false);
        }
        
        public int getHostilesNearby() { return hostilesNearby; }
        public int getHostilesInCombat() { return hostilesInCombat; }
        public double getDangerScore() { return dangerScore; }
        public boolean isActiveCombat() { return activeCombat; }
    }
}