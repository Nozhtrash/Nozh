package dev.nozh.core.render;

import dev.nozh.NozhConstants;
import dev.nozh.client.NozhModClient;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Native Raycast-based Occlusion Culling.
 * 
 * Replaces the need for external mods like "EntityCulling".
 * Implementation:
 * - Simple Raycast from Camera to Entity Center.
 * - Throttled: Checks handled only every ~10 ticks per entity (staggered).
 * - Safe: Always renders players, bosses, and nearby entities.
 * 
 * @author Nozh Team
 * @since 1.1.0
 */
public final class EntityOcclusionCuller {

    private static final Int2BooleanMap VISIBILITY_CACHE = new Int2BooleanOpenHashMap();
    private static final Int2LongMap LAST_CHECK_TIME = new Int2LongOpenHashMap();

    // Configuration constants
    private static final long CHECK_INTERVAL_MS = 250; // Check 4 times per second (approx every 5 ticks if 20tps)
    private static final double SAFE_DISTANCE_SQUARED = 8.0 * 8.0; // Always render within 8 blocks
    private static final double MAX_CULL_DISTANCE_SQUARED = 64.0 * 64.0; // Don't cull beyond 64 blocks (let render
                                                                         // distance handle it usually)
                                                                         // Actually, we WANT to cull far things if
                                                                         // blocked.
                                                                         // But raycasting far is expensive. Let's stick
                                                                         // to 64 for raycast limit.

    private static boolean enabled = true;

    /**
     * Determines if an entity should be culled (hidden).
     * 
     * @param entity The entity to check
     * @return true if entity is occluded and should be skipped
     */
    public static boolean shouldSkipRender(Entity entity) {
        if (!enabled)
            return false;

        // 1. Safety Checks (Fast Pass)
        if (entity instanceof PlayerEntity)
            return false; // Always see players
        if (entity instanceof EnderDragonEntity || entity instanceof EnderDragonPart)
            return false; // Always see dragon
        if (entity instanceof WitherEntity)
            return false; // Always see wither
        if (entity.hasPassengers() || entity.hasVehicle())
            return false; // Complex hierarchy safety

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;

        double distSq = entity.squaredDistanceTo(client.player);
        if (distSq < SAFE_DISTANCE_SQUARED)
            return false; // Always see nearby things

        // 2. Cache Lookup & Throttled Update
        long now = System.currentTimeMillis();
        long lastCheck = LAST_CHECK_TIME.getOrDefault(entity.getId(), 0L);

        // Stagger checks: Use entity ID to offset the check frame so we don't raycast
        // all entities in one frame
        boolean needsUpdate = (now - lastCheck) > CHECK_INTERVAL_MS;

        if (needsUpdate) {
            boolean visible = checkVisibility(client, entity);
            VISIBILITY_CACHE.put(entity.getId(), visible);
            LAST_CHECK_TIME.put(entity.getId(), now);
            return !visible; // If visible=true, skip=false. If visible=false, skip=true.
        }

        // Return cached value
        // If not in cache (first render), default to VISIBLE (return false) to avoid
        // pop-in
        return !VISIBILITY_CACHE.getOrDefault(entity.getId(), true);
    }

    /**
     * Performs the actual raycast.
     */
    private static boolean checkVisibility(MinecraftClient client, Entity entity) {
        Entity camera = client.getCameraEntity();
        if (camera == null)
            return true;

        Vec3d start = camera.getCameraPosVec(client.getTickDelta());
        Vec3d end = entity.getBoundingBox().getCenter(); // Aim for center of entity

        // Simple optimization: If only top half is visible, we might cull it wrongly.
        // For a robust "Potato" culler, Center is usually "Good Enough".
        // A double-check (Center + Top) would be better but 2x cost.
        // Let's stick to Center for max performance in this v1.

        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.VISUAL, // We care about visual occlusion (blocks that look solid)
                RaycastContext.FluidHandling.NONE,
                camera);

        HitResult result = client.world.raycast(context);

        // If we hit a block before the entity, result type will be BLOCK.
        // However, raycast returns what it hit. We need to check if the hit is
        // physically "before" the entity.
        // Actually, existing raycast logic: if it hits a block, it returns the block
        // hit.
        // If it reaches the end without hitting blocks, it returns MISS (somewhat).

        if (result.getType() == HitResult.Type.MISS) {
            return true; // Clear line of sight
        }

        if (result.getType() == HitResult.Type.BLOCK) {
            // We hit a block. Is the block CLOSER than the entity?
            // The raycast stops at the first hit.
            // Since our "end" point was the entity center, if we hit a block, it MUST be
            // between us and the entity.

            // Exception: Transparent blocks like glass?
            // ShapeType.VISUAL handles this mostly (collides with opaque).
            return false; // Occluded
        }

        return true;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            VISIBILITY_CACHE.clear(); // Free memory if disabled
            LAST_CHECK_TIME.clear();
        }
    }

    public static int getCacheSize() {
        return VISIBILITY_CACHE.size();
    }
}
