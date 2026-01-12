package dev.nozh.fabric.priority2;

import dev.nozh.NozhConstants;
import dev.nozh.core.director.DirectorBiasHints;
import dev.nozh.core.director.DirectorModeV2;
import dev.nozh.core.manual.ManualConfirmKeybind;
import dev.nozh.core.manual.PendingSuggestionQueue;
import dev.nozh.core.scenario.DeepScenarioSnapshot;
import dev.nozh.core.scenario.DeepScenarioTracker;
import dev.nozh.core.system.BottleneckSnapshot;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;
import dev.nozh.core.system.SystemLoadSampler;
import dev.nozh.fabric.telemetry.FabricFrameTickSampler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Priority 2 (v0.2) integration entrypoint.
 *
 * <p>Big-step wiring that activates the new modules without rewriting the existing governor core.
 * It intentionally focuses on:
 * - capturing signals (tick/render + OS load)
 * - tracking deep scenarios
 * - enabling manual confirmation input (keybind K)
 * - exposing computed snapshots for other components to consume
 */
public final class NozhPriority2Client implements ClientModInitializer {

    /**
     * Exposed, last-known deep scenario snapshot.
     */
    public static final AtomicReference<DeepScenarioSnapshot> LAST_DEEP_SCENARIO = new AtomicReference<>();

    /**
     * Exposed, last-known CPU/GPU classification result.
     */
    public static final AtomicReference<CpuGpuBottleneckClassifier.Result> LAST_BOTTLENECK = new AtomicReference<>();

    /**
     * Exposed, last-known Director bias hints.
     */
    public static final AtomicReference<DirectorBiasHints> LAST_DIRECTOR_HINTS = new AtomicReference<>();

    /**
     * Manual suggestion queue (max 3, 60s TTL).
     */
    public static final PendingSuggestionQueue PENDING = new PendingSuggestionQueue();

    @Override
    public void onInitializeClient() {
        final MinecraftClient client = MinecraftClient.getInstance();

        // (1) Telemetry: tick + render time.
        final FabricFrameTickSampler frameSampler = new FabricFrameTickSampler(client);

        // (2) OS load sampling.
        final SystemLoadSampler loadSampler = new SystemLoadSampler();

        // (3) Deep scenario tracker.
        final DeepScenarioTracker deepScenario = new DeepScenarioTracker(client);

        // (4) Director Mode V2 hints.
        final DirectorModeV2 director = new DirectorModeV2(FabricLoader.getInstance());
        LAST_DIRECTOR_HINTS.set(director.computeBiasHints());

        // (5) Bottleneck classification.
        final CpuGpuBottleneckClassifier classifier = new CpuGpuBottleneckClassifier();

        // Track block placements: best-effort signal.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world == null || !world.isClient()) return ActionResult.PASS;
            if (player == null || client.player == null) return ActionResult.PASS;
            if (!player.getUuid().equals(client.player.getUuid())) return ActionResult.PASS;

            var stack = player.getStackInHand(hand);
            if (stack != null && stack.getItem() instanceof BlockItem) {
                // This callback does not guarantee actual placement, but it is a useful proxy.
                deepScenario.recordBlockPlaced();
            }
            return ActionResult.PASS;
        });

        // Manual confirm: pop queue and notify.
        new ManualConfirmKeybind(client, () -> {
            var next = PENDING.poll();
            if (next == null) {
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("[NOZH] No pending suggestions."));
                }
                return;
            }

            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal("[NOZH] Applying suggestion: " + next.id));
                client.inGameHud.getChatHud().addMessage(Text.literal("Reason: " + next.reason));
            }

            // Integration point:
            // In the next wiring PR, this should trigger the real ActionBus / governor apply path.
        });

        // Low-frequency update loop: refresh snapshots and publish as atomics.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c != client) return;
            if (client.player == null) return;

            LAST_DEEP_SCENARIO.set(deepScenario.snapshot());
            LAST_DIRECTOR_HINTS.set(director.computeBiasHints());

            double tickMs = frameSampler.getLastTickMs();
            double renderMs = frameSampler.getLastRenderMs();

            double procLoad = loadSampler.getProcessCpuLoad().orElse(-1.0);
            double sysLoad = loadSampler.getSystemCpuLoad().orElse(-1.0);

            boolean shaders = FabricLoader.getInstance().isModLoaded("iris");

            BottleneckSnapshot snap = new BottleneckSnapshot(
                    tickMs,
                    renderMs,
                    procLoad,
                    sysLoad,
                    shaders,
                    0
            );

            LAST_BOTTLENECK.set(classifier.classify(snap));
        });

        NozhConstants.LOGGER.info("Priority2 integration loaded: deep-scenarios + bottleneck + manual-confirm + director-v2");
    }
}
