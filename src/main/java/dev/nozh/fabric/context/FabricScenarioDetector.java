package dev.nozh.fabric.context;

import dev.nozh.core.context.Scenario;
import dev.nozh.core.context.ScenarioDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * Fabric implementation of ScenarioDetector.
 * Uses Minecraft client state to determine context.
 */
public class FabricScenarioDetector implements ScenarioDetector {

    private final MinecraftClient client;

    private Vec3d lastPos = Vec3d.ZERO;
    private int stationaryTicks = 0;

    // AFK threshold: 30 seconds
    private static final int AFK_THRESHOLD_TICKS = 20 * 30;

    public FabricScenarioDetector() {
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public Scenario detect() {
        if (client.player == null || client.world == null) {
            return Scenario.LOADING;
        }

        if (client.currentScreen != null) {
            return Scenario.MENU;
        }

        PlayerEntity player = client.player;

        // Detect AFK (Stationary + No Input)
        // Note: Real input detection needs mixins, here we use position heuristic
        Vec3d currentPos = player.getPos();
        if (currentPos.squaredDistanceTo(lastPos) < 0.0001) {
            stationaryTicks++;
        } else {
            stationaryTicks = 0;
            // Also update input time logic if we could hook inputs,
            // but movement is a good proxy.
        }
        lastPos = currentPos;

        if (stationaryTicks > AFK_THRESHOLD_TICKS) {
            return Scenario.AFK;
        }

        // Detect Mining (Underground + Pickaxe)
        if (currentPos.y < 50 && !client.world.isSkyVisible(player.getBlockPos())) {
            if (player.getMainHandStack().getItem() == Items.DIAMOND_PICKAXE ||
                    player.getMainHandStack().getItem() == Items.NETHERITE_PICKAXE ||
                    player.getMainHandStack().getItem() == Items.IRON_PICKAXE) {
                return Scenario.MINING;
            }
        }

        // Detect Combat (Holding weapon or recent damage)
        // Simple heuristic: Holding sword/axe
        if (player.getMainHandStack().getItem() == Items.DIAMOND_SWORD ||
                player.getMainHandStack().getItem() == Items.NETHERITE_SWORD ||
                player.getMainHandStack().getItem() == Items.NETHERITE_AXE) {
            return Scenario.COMBAT;
        }

        // If many entities nearby, also consider combat/crowd
        // (Expensive check, maybe skip or optimize)
        // int nearbyEntities = client.world.getEntitiesByClass(...)

        // Detect Building (Creative + Holding blocks)
        if (player.isCreative() && player.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem) {
            return Scenario.BUILDING;
        }

        return Scenario.STANDARD;
    }
}
