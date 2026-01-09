package dev.nozh.core.scenario;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Tracks hostile entities near the player.
 * Provides context for combat scenario detection.
 */
public class HostileEntityTracker {
    
    private static final double DANGER_RADIUS = 16.0; // blocks
    private static final double COMBAT_RADIUS = 8.0;  // active combat range
    
    /**
     * Analyze hostile entity context around player.
     */
    public HostileContext analyze(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return HostileContext.safe();
        }
        
        try {
            Vec3d playerPos = client.player.getPos();
            Box searchBox = Box.of(playerPos, DANGER_RADIUS * 2, DANGER_RADIUS * 2, DANGER_RADIUS * 2);
            
            // Get all hostile entities nearby
            List<Entity> nearbyHostiles = client.world.getOtherEntities(
                client.player, 
                searchBox, 
                e -> e instanceof HostileEntity && e.isAlive()
            );
            
            int hostilesNearby = nearbyHostiles.size();
            
            // Count hostiles in active combat
            long hostilesInCombat = nearbyHostiles.stream()
                .filter(e -> e.distanceTo(client.player) < COMBAT_RADIUS)
                .filter(e -> {
                    if (e instanceof MobEntity) {
                        MobEntity mob = (MobEntity) e;
                        return mob.getTarget() == client.player;
                    }
                    return false;
                })
                .count();
            
            // Calculate danger score
            double dangerScore = calculateDangerScore(nearbyHostiles, client);
            
            boolean activeCombat = hostilesInCombat > 0;
            
            return new HostileContext(
                hostilesNearby,
                (int) hostilesInCombat,
                dangerScore,
                activeCombat
            );
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to analyze hostile entities", e);
            return HostileContext.safe();
        }
    }
    
    /**
     * Calculate overall danger score based on hostile entities.
     */
    private double calculateDangerScore(List<Entity> hostiles, MinecraftClient client) {
        return hostiles.stream()
            .mapToDouble(e -> {
                double distance = e.distanceTo(client.player);
                double threat = getThreatLevel(e);
                
                // Closer = more dangerous (inverse square)
                double distanceFactor = 1.0 / Math.max(1.0, distance * distance / 16.0);
                
                return threat * distanceFactor;
            })
            .sum();
    }
    
    /**
     * Get threat level for specific entity types.
     */
    private double getThreatLevel(Entity entity) {
        // Bosses - extreme threat
        if (entity instanceof WitherEntity || entity instanceof EnderDragonEntity) {
            return 5.0;
        }
        
        // Creepers - high threat (explosion damage)
        if (entity instanceof CreeperEntity) {
            CreeperEntity creeper = (CreeperEntity) entity;
            // Charged creepers are extra dangerous
            return creeper.shouldRenderOverlay() ? 3.0 : 2.0;
        }
        
        // Endermen - moderate-high threat
        if (entity instanceof EndermanEntity) {
            return 1.5;
        }
        
        // Generic hostile - base threat
        if (entity instanceof HostileEntity) {
            return 1.0;
        }
        
        return 0.5; // Unknown entity
    }
    
    /**
     * Context about hostile entities.
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
        
        public int getHostilesNearby() {
            return hostilesNearby;
        }
        
        public int getHostilesInCombat() {
            return hostilesInCombat;
        }
        
        public double getDangerScore() {
            return dangerScore;
        }
        
        public boolean isActiveCombat() {
            return activeCombat;
        }
        
        public boolean isSafe() {
            return hostilesNearby == 0 && dangerScore < 0.1;
        }
        
        @Override
        public String toString() {
            return String.format("HostileContext{nearby=%d, combat=%d, danger=%.2f, active=%s}",
                               hostilesNearby, hostilesInCombat, dangerScore, activeCombat);
        }
    }
}
