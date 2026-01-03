package dev.nozh.fabric.context;

import dev.nozh.core.context.ScenarioDetector;
import dev.nozh.core.context.ScenarioSnapshot;
import dev.nozh.core.input.InputActivityTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Fabric implementation of ScenarioDetector.
 * Uses Minecraft client state to determine context.
 */
public class FabricScenarioDetector implements ScenarioDetector {

    private final MinecraftClient client;

    private Vec3d lastPos = Vec3d.ZERO;
    private int stationaryTicks = 0;
    private long lastEntitySampleTick = -1L;
    private int cachedEntityCount = 0;

    // AFK threshold: 30 seconds
    private static final int AFK_THRESHOLD_TICKS = 20 * 30;
    private static final long INPUT_RECENT_THRESHOLD_MS = 5_000L;
    private static final long INPUT_IDLE_THRESHOLD_MS = 15_000L;
    private static final int ENTITY_CHECK_INTERVAL_TICKS = 20;
    private static final double ENTITY_RADIUS = 12.0;
    private static final int COMBAT_ENTITY_THRESHOLD = 8;
    private static final double MOVEMENT_FAST_DISTANCE_SQ = 0.06 * 0.06;
    private static final double MOVEMENT_SLOW_DISTANCE_SQ = 0.02 * 0.02;

    public FabricScenarioDetector() {
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public ScenarioSnapshot detect() {
        if (client.player == null || client.world == null) {
            return new ScenarioSnapshot(dev.nozh.core.context.Scenario.LOADING, 0.2);
        }

        if (client.currentScreen != null) {
            return new ScenarioSnapshot(dev.nozh.core.context.Scenario.MENU, 0.9);
        }

        PlayerEntity player = client.player;

        // Detect AFK (Stationary + No Input)
        Vec3d currentPos = player.getPos();
        double moveDistanceSq = currentPos.squaredDistanceTo(lastPos);
        if (moveDistanceSq < 0.0001) {
            stationaryTicks++;
        } else {
            stationaryTicks = 0;
        }
        lastPos = currentPos;

        boolean inputRecent = InputActivityTracker.hasRecentInput(INPUT_RECENT_THRESHOLD_MS);
        long inputAgeMs = InputActivityTracker.getLastInputAgeMs();

        int nearbyEntities = sampleNearbyEntities(player);
        double tpsDropConfidence = getTpsDropConfidence();
        HitResult.Type targetType = getCrosshairTargetType();
        boolean targetEntity = targetType == HitResult.Type.ENTITY;
        boolean targetBlock = targetType == HitResult.Type.BLOCK;
        boolean handSwinging = player.handSwinging;
        boolean breakingBlock = client.interactionManager != null && client.interactionManager.isBreakingBlock();

        // Detect Mining (Underground + Pickaxe)
        boolean underground = currentPos.y < 50 && !client.world.isSkyVisible(player.getBlockPos());
        boolean holdingPickaxe = isPickaxe(player.getMainHandStack().getItem());
        boolean holdingWeapon = isWeapon(player.getMainHandStack().getItem());
        boolean holdingBlockItem = player.getMainHandStack().getItem() instanceof BlockItem;
        boolean buildingCandidate = holdingBlockItem;
        boolean sprinting = player.isSprinting();

        double combatConfidence = 0.0;
        if (holdingWeapon) {
            combatConfidence += 0.35;
        }
        if (player.hurtTime > 0) {
            combatConfidence += 0.3;
        }
        if (nearbyEntities >= COMBAT_ENTITY_THRESHOLD) {
            combatConfidence += 0.3;
        }
        if (handSwinging) {
            combatConfidence += 0.2;
        }
        if (targetEntity) {
            combatConfidence += 0.2;
        }
        if (sprinting) {
            combatConfidence += 0.1;
        }
        combatConfidence = clamp(combatConfidence + tpsDropConfidence * 0.1);

        double miningConfidence = 0.0;
        if (underground) {
            miningConfidence += 0.3;
        }
        if (holdingPickaxe) {
            miningConfidence += 0.25;
        }
        if (breakingBlock) {
            miningConfidence += 0.25;
        }
        if (holdingPickaxe && (handSwinging || targetBlock)) {
            miningConfidence += 0.2;
        }
        if (!inputRecent && stationaryTicks > 40) {
            miningConfidence += 0.1;
        }
        miningConfidence = clamp(miningConfidence + tpsDropConfidence * 0.05);

        double buildingConfidence = 0.0;
        if (buildingCandidate) {
            buildingConfidence += 0.4;
        }
        if (holdingBlockItem && (handSwinging || targetBlock)) {
            buildingConfidence += 0.25;
        }
        if (player.isCreative()) {
            buildingConfidence += 0.15;
        }
        if (inputRecent) {
            buildingConfidence += 0.1;
        }
        buildingConfidence = clamp(buildingConfidence);

        double afkConfidence = 0.0;
        if (stationaryTicks > AFK_THRESHOLD_TICKS) {
            afkConfidence += 0.6;
        }
        if (inputAgeMs > INPUT_IDLE_THRESHOLD_MS) {
            afkConfidence += 0.3;
        }
        if (inputAgeMs == Long.MAX_VALUE) {
            afkConfidence += 0.1;
        }
        afkConfidence = clamp(afkConfidence - tpsDropConfidence * 0.2);

        double movementConfidence = 0.0;
        if (moveDistanceSq > MOVEMENT_SLOW_DISTANCE_SQ) {
            movementConfidence += 0.15;
        }
        if (moveDistanceSq > MOVEMENT_FAST_DISTANCE_SQ) {
            movementConfidence += 0.2;
        }
        if (sprinting) {
            movementConfidence += 0.15;
        }
        double standardConfidence = inputRecent ? 0.55 : 0.4;
        standardConfidence = clamp(standardConfidence + movementConfidence - tpsDropConfidence * 0.05);

        dev.nozh.core.context.Scenario scenario = dev.nozh.core.context.Scenario.STANDARD;
        double confidence = standardConfidence;

        if (combatConfidence > confidence) {
            scenario = dev.nozh.core.context.Scenario.COMBAT;
            confidence = combatConfidence;
        }

        if (miningConfidence > confidence) {
            scenario = dev.nozh.core.context.Scenario.MINING;
            confidence = miningConfidence;
        }

        if (buildingConfidence > confidence) {
            scenario = dev.nozh.core.context.Scenario.BUILDING;
            confidence = buildingConfidence;
        }

        if (afkConfidence > confidence) {
            scenario = dev.nozh.core.context.Scenario.AFK;
            confidence = afkConfidence;
        }

        return new ScenarioSnapshot(scenario, confidence);
    }

    private int sampleNearbyEntities(PlayerEntity player) {
        long worldTick = client.world.getTime();
        if (lastEntitySampleTick >= 0 && worldTick - lastEntitySampleTick < ENTITY_CHECK_INTERVAL_TICKS) {
            return cachedEntityCount;
        }

        lastEntitySampleTick = worldTick;
        List<net.minecraft.entity.Entity> entities = client.world.getOtherEntities(
                player,
                player.getBoundingBox().expand(ENTITY_RADIUS),
                entity -> entity.isAlive());
        cachedEntityCount = entities.size();
        return cachedEntityCount;
    }

    private boolean isPickaxe(Item item) {
        return item == Items.DIAMOND_PICKAXE ||
                item == Items.NETHERITE_PICKAXE ||
                item == Items.IRON_PICKAXE;
    }

    private boolean isWeapon(Item item) {
        return item == Items.DIAMOND_SWORD ||
                item == Items.NETHERITE_SWORD ||
                item == Items.NETHERITE_AXE;
    }

    private HitResult.Type getCrosshairTargetType() {
        if (client.crosshairTarget == null) {
            return HitResult.Type.MISS;
        }
        return client.crosshairTarget.getType();
    }

    private double getTpsDropConfidence() {
        if (client.getServer() == null) {
            return 0.0;
        }

        try {
            Method avgTickMethod = client.getServer().getClass().getMethod("getAverageTickTime");
            Object result = avgTickMethod.invoke(client.getServer());
            if (!(result instanceof Number)) {
                return 0.0;
            }
            double avgTickTimeMs = ((Number) result).doubleValue();
            if (avgTickTimeMs <= 0) {
                return 0.0;
            }
            double tps = Math.min(20.0, 1000.0 / avgTickTimeMs);
            return clamp((20.0 - tps) / 20.0);
        } catch (ReflectiveOperationException e) {
            return 0.0;
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
