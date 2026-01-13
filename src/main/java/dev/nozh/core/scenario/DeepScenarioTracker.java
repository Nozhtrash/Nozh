package dev.nozh.core.scenario;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Identifier;

public final class DeepScenarioTracker {

    public static final long DEFAULT_WINDOW_MS = 30_000L;

    private final MinecraftClient client;
    private final long windowMs;

    private final SlidingWindowCounter placed;
    private final SlidingWindowCounter broken;

    private volatile DeepScenarioSnapshot last = new DeepScenarioSnapshot("unknown", 0.0, 0.0, 0);

    private int tickCounter;
    private final int hostileScanEveryTicks;
    private final double hostileRadius;

    public DeepScenarioTracker(MinecraftClient client) {
        this(client, DEFAULT_WINDOW_MS, 20, 32.0);
    }

    public DeepScenarioTracker(MinecraftClient client, long windowMs, int hostileScanEveryTicks, double hostileRadius) {
        if (client == null) throw new NullPointerException("client");
        this.client = client;
        this.windowMs = Math.max(1L, windowMs);
        this.placed = new SlidingWindowCounter(this.windowMs);
        this.broken = new SlidingWindowCounter(this.windowMs);
        this.hostileScanEveryTicks = Math.max(1, hostileScanEveryTicks);
        this.hostileRadius = Math.max(4.0, hostileRadius);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (player == null) return;
            if (this.client.player == null) return;
            if (player.getUuid() == null || !player.getUuid().equals(this.client.player.getUuid())) return;
            broken.add(nowMs(), 1);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public void recordBlockPlaced() {
        placed.add(nowMs(), 1);
    }

    private void onTick(MinecraftClient c) {
        if (c != this.client) return;
        if (client.player == null || client.world == null) return;

        tickCounter++;

        int hostiles = last.hostileMobsNearby;
        if ((tickCounter % hostileScanEveryTicks) == 0) {
            hostiles = countHostilesNearby();
        }

        String dim = "unknown";
        try {
            Identifier id = client.world.getRegistryKey().getValue();
            dim = id != null ? id.toString() : "unknown";
        } catch (Exception e) {
            // Registry key may not be available
        }

        long now = nowMs();
        last = new DeepScenarioSnapshot(
                dim,
                placed.perMinute(now),
                broken.perMinute(now),
                hostiles
        );
    }

    private int countHostilesNearby() {
        if (client.player == null || client.world == null) return 0;
        var player = client.player;
        var box = player.getBoundingBox().expand(hostileRadius);
        return client.world.getEntitiesByClass(HostileEntity.class, box, e -> true).size();
    }

    public DeepScenarioSnapshot snapshot() {
        return last;
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }
}
