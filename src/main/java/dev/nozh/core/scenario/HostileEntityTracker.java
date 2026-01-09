package dev.nozh.core.scenario;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Tracks hostile entities near the player for combat detection.
 * 
 * <p>Calculates danger scores based on entity proximity and threat levels.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 3)
 */
public final class HostileEntityTracker {
    private static final double DANGER_RADIUS = 16.0; // blocks
    private static final double COMBAT_RADIUS = 8.0;
    
    /**
     * Context of hostile entities around player.
     */
    public static class HostileContext {
        private final int hostilesNearby;
        private final int hostilesInCombat;
        private final double dangerScore;
        private final boolean activeCombat;
        
        public HostileContext(int hostilesNearby, int hostilesInCombat, 
                            double dangerScore, boolean activeCombat) {
            this.hostilesNearby = hostilesNearby;
            this.hostilesInCombat = hostilesInCombat;
            this.dangerScore = dangerScore;
            this.activeCombat = activeCombat;
        }
        
        public static HostileContext safe() {
            return new HostileContext(0, 0, 0.0, false);
        }
        
        public int getHostilesNearby() { return hostilesNearby; }
        public int getHostilesInCombat() { return hostilesInCombat; }
        public double getDangerScore() { return dangerScore; }
        public boolean isActiveCombat() { return activeCombat; }
    }
    
    /**
     * Analyze hostile entities around player.
     */
    public HostileContext analyze(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            return HostileContext.safe();
        }
        
        Vec3d playerPos = client.player.getPos();
        Box searchBox = Box.of(playerPos, DANGER_RADIUS * 2, 
                               DANGER_RADIUS * 2, DANGER_RADIUS * 2);
        
        List<Entity> nearbyEntities = client.world.getOtherEntities(
            client.player, searchBox, e -> e instanceof HostileEntity
        );
        
        int hostilesNearby = nearbyEntities.size();
        int hostilesInCombat = (int) nearbyEntities.stream()
            .filter(e -> e.distanceTo(client.player) < COMBAT_RADIUS)
            .filter(e -> {
                if (e instanceof MobEntity mob) {
                    return mob.getTarget() == client.player;
                }
                return false;
            })
            .count();
        
        double dangerScore = calculateDangerScore(nearbyEntities, client);
        
        return new HostileContext(
            hostilesNearby,
            hostilesInCombat,
            dangerScore,
            hostilesInCombat > 0
        );
    }
    
    /**
     * Calculate overall danger score based on nearby hostiles.
     */
    private double calculateDangerScore(List<Entity> hostiles, MinecraftClient client) {
        return hostiles.stream()
            .mapToDouble(e -> {
                double distance = e.distanceTo(client.player);
                double threat = getThreatLevel(e);
                // Inverse distance weighting: closer = more dangerous
                return threat / Math.max(1.0, distance);
            })
            .sum();
    }
    
    /**
     * Get threat level for entity type.
     */
    private double getThreatLevel(Entity entity) {
        // Boss entities
        if (entity instanceof WitherEntity || entity instanceof EnderDragonEntity) {
            return 5.0;
        }
        
        // High threat
        if (entity instanceof CreeperEntity) {
            return 2.0; // Creepers are dangerous due to explosion
        }
        
        if (entity instanceof EndermanEntity || entity instanceof WitherSkeletonEntity) {
            return 1.5;
        }
        
        // Standard threat (zombies, skeletons, spiders, etc.)
        return 1.0;
    }
}
