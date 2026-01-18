package dev.nozh.fabric.priority2;

import dev.nozh.NozhConstants;
import dev.nozh.core.director.DirectorBiasHints;
import dev.nozh.core.director.DirectorModeV2;
import dev.nozh.core.manual.ManualConfirmKeybind;
import dev.nozh.core.manual.PendingSuggestionQueue;
import dev.nozh.core.priority2.Priority2Signals;
import dev.nozh.core.priority2.Priority2Suggestion;
import dev.nozh.core.priority2.Priority2SuggestionEngine;
import dev.nozh.core.priority3.EfficiencyScorer;
import dev.nozh.core.priority3.PredictiveAnalyzer;
import dev.nozh.core.priority3.SpikePrediction;
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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Priority 2 (v0.2) integration entrypoint.
 */
public final class NozhPriority2Client implements ClientModInitializer {

    public static final AtomicReference<DeepScenarioSnapshot> LAST_DEEP_SCENARIO = new AtomicReference<>();
    public static final AtomicReference<CpuGpuBottleneckClassifier.Result> LAST_BOTTLENECK = new AtomicReference<>();
    public static final AtomicReference<DirectorBiasHints> LAST_DIRECTOR_HINTS = new AtomicReference<>();

    public static final PendingSuggestionQueue PENDING = new PendingSuggestionQueue();

    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if (initialized) {
            NozhConstants.LOGGER.warn("NozhPriority2Client (v0.2) already initialized! Skipping.");
            return;
        }
        initialized = true;
        final MinecraftClient client = MinecraftClient.getInstance();

        final FabricFrameTickSampler frameSampler = new FabricFrameTickSampler(client);
        final SystemLoadSampler loadSampler = new SystemLoadSampler();
        final DeepScenarioTracker deepScenario = new DeepScenarioTracker(client);
        final DirectorModeV2 director = new DirectorModeV2(FabricLoader.getInstance());
        final CpuGpuBottleneckClassifier classifier = new CpuGpuBottleneckClassifier();
        final Priority2SuggestionEngine suggestionEngine = new Priority2SuggestionEngine();
        final Priority2ActionApplier actionApplier = new Priority2ActionApplier();

        // v0.3 bundle:
        final PredictiveAnalyzer predictive = new PredictiveAnalyzer(40);
        final EfficiencyScorer scorer = new EfficiencyScorer();

        Priority2HudOverlay.register(client, PENDING);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world == null || !world.isClient())
                return ActionResult.PASS;
            if (player == null || client.player == null)
                return ActionResult.PASS;
            if (!player.getUuid().equals(client.player.getUuid()))
                return ActionResult.PASS;

            var stack = player.getStackInHand(hand);
            if (stack != null && stack.getItem() instanceof BlockItem) {
                deepScenario.recordBlockPlaced();
            }
            return ActionResult.PASS;
        });

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
                client.inGameHud.getChatHud()
                        .addMessage(Text.literal("[NOZH] Confirmed: " + next.id + " -> " + r.result));
                if (next.reason != null && !next.reason.isBlank()) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("Reason: " + next.reason));
                }
                if (r.message != null && !r.message.isBlank()) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("Result: " + r.message));
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c != client)
                return;
            if (client.player == null)
                return;

            DeepScenarioSnapshot scenarioSnap = deepScenario.snapshot();
            DirectorBiasHints hints = director.computeBiasHints();

            double tickMs = frameSampler.getLastTickMs();
            double renderMs = frameSampler.getLastRenderMs();

            double frameMs = safe(tickMs) + safe(renderMs);
            predictive.addSampleMs(frameMs);

            double procLoad = loadSampler.getProcessCpuLoad().orElse(-1.0);
            double sysLoad = loadSampler.getSystemCpuLoad().orElse(-1.0);

            boolean shaders = FabricLoader.getInstance().isModLoaded("iris");

            BottleneckSnapshot snap = new BottleneckSnapshot(
                    tickMs,
                    renderMs,
                    procLoad,
                    sysLoad,
                    shaders,
                    0);

            CpuGpuBottleneckClassifier.Result bottleneck = classifier.classify(snap);

            LAST_DEEP_SCENARIO.set(scenarioSnap);
            LAST_DIRECTOR_HINTS.set(hints);
            LAST_BOTTLENECK.set(bottleneck);

            Priority2Signals.deepScenario.set(scenarioSnap);
            Priority2Signals.directorHints.set(hints);
            Priority2Signals.bottleneck.set(bottleneck);

            // Predictive gating: if recovery is already expected, avoid enqueuing new
            // suggestions.
            if (PENDING.size() == 0 && predictive.shouldWaitForRecovery()) {
                return;
            }

            SpikePrediction spike = predictive.predictSpike();

            Priority2Suggestion s = suggestionEngine.compute(scenarioSnap, bottleneck, hints);
            if (s != null) {
                double conf = bottleneck == null ? 0.0 : bottleneck.confidence01;
                EfficiencyScorer.Score score = scorer.score(s, conf);

                String extra = " score=" + String.format(Locale.ROOT, "%.2f", score.finalScore)
                        + " eff=" + String.format(Locale.ROOT, "%.2f", score.efficiency);
                if (spike.likely) {
                    extra += " spike(p=" + String.format(Locale.ROOT, "%.2f", spike.probability01) + ")";
                }

                PENDING.add(s.id, (s.reason == null ? "" : s.reason) + extra);
            }

            if (PENDING.size() == 0 && Priority2PerfGate.performanceVeryGood(tickMs, renderMs, bottleneck)) {
                try {
                    actionApplier.tryGradualRecovery(client, true);
                } catch (Exception e) {
                    NozhConstants.LOGGER.debug("Gradual recovery skipped due to exception", e);
                }
            }
        });

        NozhConstants.LOGGER.info("Priority2 client initialized: predictive analysis + efficiency scoring enabled");
    }

    private static double safe(double ms) {
        if (!Double.isFinite(ms) || ms < 0.0)
            return 0.0;
        if (ms > 60_000.0)
            return 60_000.0;
        return ms;
    }
}
