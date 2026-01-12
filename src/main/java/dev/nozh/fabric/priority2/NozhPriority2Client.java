package dev.nozh.fabric.priority2;

import dev.nozh.NozhConstants;
import dev.nozh.core.director.DirectorBiasHints;
import dev.nozh.core.director.DirectorModeV2;
import dev.nozh.core.manual.ManualConfirmKeybind;
import dev.nozh.core.manual.PendingSuggestionQueue;
import dev.nozh.core.priority2.Priority2Signals;
import dev.nozh.core.priority2.Priority2Suggestion;
import dev.nozh.core.priority2.Priority2SuggestionEngine;
import dev.nozh.core.scenario.DeepScenarioSnapshot;
import dev.nozh.core.scenario.DeepScenarioTracker;
import dev.nozh.core.system.BottleneckSnapshot;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;
import dev.nozh.core.system.SystemLoadSampler;
import dev.nozh.fabric.telemetry.FabricFrameTickSampler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Priority 2 (v0.2) integration entrypoint.
 */
public final class NozhPriority2Client implements ClientModInitializer {

    public static final AtomicReference<DeepScenarioSnapshot> LAST_DEEP_SCENARIO = new AtomicReference<>();
    public static final AtomicReference<CpuGpuBottleneckClassifier.Result> LAST_BOTTLENECK = new AtomicReference<>();
    public static final AtomicReference<DirectorBiasHints> LAST_DIRECTOR_HINTS = new AtomicReference<>();

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

        // (5) Bottleneck classification.
        final CpuGpuBottleneckClassifier classifier = new CpuGpuBottleneckClassifier();

        // (6) Suggestion engine (manual mode building block).
        final Priority2SuggestionEngine suggestionEngine = new Priority2SuggestionEngine();

        // (7) Action applier: maps suggestions to real settings.
        final Priority2ActionApplier actionApplier = new Priority2ActionApplier();

        // HUD overlay for visibility (big step: no need to modify existing HUD).
        Priority2HudOverlay.register(client, PENDING);

        // Track block placements: best-effort signal.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world == null || !world.isClient()) return ActionResult.PASS;
            if (player == null || client.player == null) return ActionResult.PASS;
            if (!player.getUuid().equals(client.player.getUuid())) return ActionResult.PASS;

            var stack = player.getStackInHand(hand);
            if (stack != null && stack.getItem() instanceof BlockItem) {
                deepScenario.recordBlockPlaced();
            }
            return ActionResult.PASS;
        });

        // Manual confirm: pop queue, apply real changes, and notify.
        new ManualConfirmKeybind(client, () -> {
            var next = PENDING.poll();
            if (next == null) {
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("[NOZH] No pending suggestions."));
                }
                return;
            }

            Priority2ActionApplier.Result r = actionApplier.applySuggestion(client, next.id);

            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal("[NOZH] Confirmed: " + next.id + " -> " + r.result));
                if (next.reason != null && !next.reason.isBlank()) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("Reason: " + next.reason));
                }
                if (r.message != null && !r.message.isBlank()) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("Result: " + r.message));
                }
            }
        });

        // Runtime signal refresh.
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c != client) return;
            if (client.player == null) return;

            DeepScenarioSnapshot scenarioSnap = deepScenario.snapshot();
            DirectorBiasHints hints = director.computeBiasHints();

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

            CpuGpuBottleneckClassifier.Result bottleneck = classifier.classify(snap);

            LAST_DEEP_SCENARIO.set(scenarioSnap);
            LAST_DIRECTOR_HINTS.set(hints);
            LAST_BOTTLENECK.set(bottleneck);

            // Publish stable core signals.
            Priority2Signals.deepScenario.set(scenarioSnap);
            Priority2Signals.directorHints.set(hints);
            Priority2Signals.bottleneck.set(bottleneck);

            // Suggestion queueing (defensive + de-dup).
            Priority2Suggestion s = suggestionEngine.compute(scenarioSnap, bottleneck, hints);
            if (s != null) {
                PENDING.add(s.id, s.reason);
            }

            // v0.3-ish: gradual recovery if we are stable and no pending suggestions.
            if (PENDING.size() == 0 && Priority2PerfGate.performanceVeryGood(tickMs, renderMs, bottleneck)) {
                try {
                    actionApplier.tryGradualRecovery(client, true);
                } catch (Throwable t) {
                    NozhConstants.LOGGER.debug("Gradual recovery skipped due to exception", t);
                }
            }
        });

        NozhConstants.LOGGER.info("Priority2 mega actions loaded: suggestions -> real settings + gradual recovery");
    }
}
