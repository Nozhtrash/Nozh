package dev.nozh.client;

import dev.nozh.NozhConstants;
import dev.nozh.core.compat.CompatService;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.safety.CrashLoopGuard;
import dev.nozh.core.safety.StateManager;
import dev.nozh.core.safety.NozhState;
import dev.nozh.core.telemetry.TelemetryExportFormat;
import dev.nozh.core.util.NozhText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.SharedConstants;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.text.Text;

/**
 * Client-side commands for NOZH.
 * 
 * Phase 2 Iteration 3: Professional-grade selfcheck with verdict system
 * Phase 8: UX Polish
 * Phase 9: CompatService Integration
 * Phase 11: i18n
 */
@Environment(EnvType.CLIENT)
public final class NozhCommands {

    private NozhCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("nozh")
                            .then(ClientCommandManager.literal("status")
                                    .executes(context -> {
                                        runStatus(context);
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("selfcheck")
                                    .executes(context -> {
                                        runSelfCheck(context.getSource());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("perf")
                                    .executes(context -> {
                                        runPerfCheck(context.getSource());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("history")
                                    .executes(context -> {
                                        runHistory(context);
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("apply")
                                    .executes(context -> {
                                        runApplySuggestion(context.getSource());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("suggestion")
                                    .then(ClientCommandManager.literal("clear")
                                            .executes(context -> {
                                                runClearSuggestion(context.getSource());
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("telemetry")
                                    .then(ClientCommandManager.literal("export")
                                            .then(ClientCommandManager.literal("csv")
                                                    .executes(context -> {
                                                        runTelemetryExport(context.getSource(),
                                                                TelemetryExportFormat.CSV);
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("json")
                                                    .executes(context -> {
                                                        runTelemetryExport(context.getSource(),
                                                                TelemetryExportFormat.JSON);
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("compact-csv")
                                                    .executes(context -> {
                                                        runTelemetryExport(context.getSource(),
                                                                TelemetryExportFormat.COMPACT_CSV);
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("compact-json")
                                                    .executes(context -> {
                                                        runTelemetryExport(context.getSource(),
                                                                TelemetryExportFormat.COMPACT_JSON);
                                                        return 1;
                                                    }))))
                            .then(ClientCommandManager.literal("enable")
                                    .executes(context -> {
                                        runEnable(context.getSource());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("disable")
                                    .executes(context -> {
                                        runDisable(context.getSource());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("spy")
                                    .executes(context -> {
                                        runSpy(context.getSource());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("safemode")
                                    .then(ClientCommandManager.literal("reset")
                                            .executes(context -> {
                                                resetSafeMode(context.getSource());
                                                return 1;
                                            }))
                                    .then(ClientCommandManager.literal("on")
                                            .executes(context -> {
                                                enableSafeMode(context.getSource());
                                                return 1;
                                            }))
                                    .then(ClientCommandManager.literal("off")
                                            .executes(context -> {
                                                resetSafeMode(context.getSource());
                                                return 1;
                                            })))
                            .executes(context -> {
                                runStatus(context);
                                return 1;
                            }));
        });
    }

    private static void runSelfCheck(FabricClientCommandSource source) {
        int warnings = 0;
        int failures = 0;

        // Header
        source.sendFeedback(NozhText.translatableHeader("nozh.selfcheck.header"));

        // Environment
        source.sendFeedback(NozhText.translatableHeader("nozh.selfcheck.env.header"));
        String mcVersion = SharedConstants.getGameVersion().getName();
        String fabricVersion = FabricLoader.getInstance().getModContainer("fabricloader")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        source.sendFeedback(
                Text.translatable("nozh.selfcheck.env.minecraft", mcVersion).styled(s -> s.withColor(0xAAAAAA)));
        source.sendFeedback(
                Text.translatable("nozh.selfcheck.env.fabric", fabricVersion).styled(s -> s.withColor(0xAAAAAA)));
        source.sendFeedback(Text.translatable("nozh.selfcheck.env.nozh", NozhConstants.getVersion())
                .styled(s -> s.withColor(0xAAAAAA)));

        // Facts
        source.sendFeedback(NozhText.translatableHeader("nozh.selfcheck.facts.header"));

        NozhConfig config = ConfigManager.getConfig();
        if (config != null) {
            source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.config",
                    Text.translatable("nozh.selfcheck.status.ok").getString())); // HACK: reusing labeled for now which
                                                                                 // takes String value, but ideally we'd
                                                                                 // pass Text
            if (config.wasCorrected()) {
                source.sendFeedback(NozhText.translatableWarning("nozh.command.selfcheck.config.corrected"));
                warnings++;
            }
        } else {
            source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.config", "FAILED")); // Fallback for failed
            failures++;
        }

        NozhState state = StateManager.getState();
        if (state != null) {
            source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.state",
                    Text.translatable("nozh.selfcheck.status.ok").getString()));
        } else {
            source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.state", "FAILED"));
            failures++;
        }

        boolean inSafeMode = CrashLoopGuard.isInSafeMode();
        String smReason = CrashLoopGuard.getSafeModeReason();
        String smStatus = inSafeMode
                ? Text.translatable("nozh.selfcheck.status.active").getString() + " (" + smReason + ")"
                : Text.translatable("nozh.command.selfcheck.safemode.off").getString();
        // Manually constructing labeled here because NozhText.labeled expects explicit
        // isGood boolean logic which we want to keep
        source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.safemode", smStatus));

        // God Mode Modules
        source.sendFeedback(NozhText.translatableHeader("nozh.selfcheck.modules.header"));
        source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.title.orchestration",
                Text.translatable("nozh.selfcheck.value.director").getString()));
        source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.title.entity_control",
                Text.translatable("nozh.selfcheck.status.active").getString()));
        source.sendFeedback(NozhText.translatableLabeled("nozh.selfcheck.title.true_sight",
                Text.translatable("nozh.selfcheck.status.active").getString()));

        // Compat Report (Phase 9)
        CompatService.CompatReport report = CompatService.generateReport();
        if (!report.performanceMods().isEmpty()) {
            source.sendFeedback(Text
                    .translatable("nozh.command.selfcheck.compat.perf_mods",
                            String.join(", ", report.performanceMods()))
                    .styled(s -> s.withColor(0x55FF55)));
        }
        if (!report.shaderMods().isEmpty()) {
            source.sendFeedback(Text
                    .translatable("nozh.command.selfcheck.compat.shader_mods", String.join(", ", report.shaderMods()))
                    .styled(s -> s.withColor(0x55FFFF)));
        }
        for (net.minecraft.text.Text hint : report.hints()) {
            source.sendFeedback(hint.copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        source.sendFeedback(NozhText.translatableHeader("nozh.selfcheck.stewards.header"));
        for (CompatService.CapabilitySteward steward : CompatService.generateStewardReport()) {
            int color = steward.conflict() ? 0xFFAA00 : 0x55FF55;
            source.sendFeedback(Text.literal("  " + steward.capabilityId().name() + " → " + steward.steward())
                    .styled(s -> s.withColor(color)));
        }

        // Verdict
        source.sendFeedback(NozhText.translatableHeader("nozh.selfcheck.verdict.header"));
        if (failures > 0) {
            source.sendFeedback(NozhText.translatableError("nozh.selfcheck.verdict.fail", failures));
        } else if (warnings > 0) {
            source.sendFeedback(NozhText.translatableWarning("nozh.selfcheck.verdict.warn", warnings));
        } else {
            source.sendFeedback(NozhText.translatableSuccess("nozh.selfcheck.verdict.healthy"));
        }
    }

    private static void resetSafeMode(FabricClientCommandSource source) {
        CrashLoopGuard.resetSafeMode();
        source.sendFeedback(
                Text.translatable("nozh.command.safemode.reset_success").styled(s -> s.withColor(0x55FF55)));
    }

    private static void enableSafeMode(FabricClientCommandSource source) {
        CrashLoopGuard.enableSafeMode();
        source.sendFeedback(Text.translatable("nozh.command.safemode.enabled").styled(s -> s.withColor(0xFFAA00)));
    }

    private static void runStatus(CommandContext<FabricClientCommandSource> ctx) {
        NozhConfig config = ConfigManager.getConfig();
        boolean safeMode = CrashLoopGuard.isInSafeMode();
        long uptimeSec = (System.currentTimeMillis() - StateManager.getState().sessionStartTime) / 1000;
        RuntimeState runtimeState = StateStore.getInstance().snapshotSafe();

        String modeKey = safeMode ? "nozh.status.mode.safemode"
                : (config.allowAutoTuning ? "nozh.status.mode.active" : "nozh.status.mode.passive");
        int color = safeMode ? 0xFFAAAA : (config.allowAutoTuning ? 0xAAFFAA : 0xAAAAAA);

        ctx.getSource().sendFeedback(Text.translatable("nozh.status.header")
                .append(Text.translatable(modeKey).styled(s -> s.withColor(color))));
        ctx.getSource().sendFeedback(Text.translatable("nozh.status.uptime", uptimeSec));
        ctx.getSource().sendFeedback(Text.translatable("nozh.status.target", config.targetFps));

        if (runtimeState.suggestedActions() != null && !runtimeState.suggestedActions().isEmpty()) {
            PendingAction pending = runtimeState.suggestedActions().get(0);
            int remaining = runtimeState.suggestedActions().size();
            String summary = pending.capability().name() + "=" + pending.newValue();
            ctx.getSource().sendFeedback(Text.translatable(
                    "nozh.status.suggestions.pending",
                    remaining,
                    summary,
                    "/nozh apply"));
        }
        runtimeState.pendingAction().ifPresent(pending -> {
            String summary = pending.capability().name() + "=" + pending.newValue();
            ctx.getSource().sendFeedback(Text.translatable("nozh.status.action.pending", summary));
        });
    }

    private static void runHistory(CommandContext<FabricClientCommandSource> ctx) {
        var history = StateManager.getState().executionHistory;
        if (history.isEmpty()) {
            ctx.getSource().sendFeedback(Text.translatable("nozh.history.empty"));
            return;
        }

        ctx.getSource()
                .sendFeedback(Text.translatable("nozh.history.header", history.size()).styled(s -> s.withBold(true)));
        // Limit output to last N entries
        int limit = ConfigManager.getConfig().historyCommandLimit;
        int start = Math.max(0, history.size() - limit);

        for (int i = start; i < history.size(); i++) {
            var action = history.get(i);
            long ago = (System.currentTimeMillis() - action.timestamp()) / 1000;
            ctx.getSource().sendFeedback(Text.translatable("nozh.history.entry",
                    ago, action.type(), action.oldValue(), action.newValue()));
        }
    }

    private static int runPerfCheck(FabricClientCommandSource source) {
        var pm = dev.nozh.client.NozhModClient.getPerfManager();
        if (pm == null) {
            source.sendError(Text.translatable("nozh.perf.error.not_initialized"));
            return 0;
        }

        var s = pm.getSnapshot();
        if (!s.sufficientData()) {
            int collected = s.sampleCount();
            int needed = 60; // Minimum samples for reliable data
            int remaining = needed - collected;

            source.sendFeedback(Text.translatable("nozh.perf.collecting",
                    collected, needed, remaining));
            return 1;
        }

        // Display performance metrics
        String color = s.p95FrametimeMs() > 50.0 ? "§c" : (s.p95FrametimeMs() > 33.3 ? "§e" : "§a");
        source.sendFeedback(Text.translatable("nozh.perf.header"));
        source.sendFeedback(Text.translatable("nozh.perf.average", String.format("%.2f", s.avgFrametimeMs())));
        source.sendFeedback(Text.literal(color)
                .append(Text.translatable("nozh.perf.p95", String.format("%.2f", s.p95FrametimeMs()))));
        source.sendFeedback(Text.translatable("nozh.perf.spikes", s.spikeCount()));
        source.sendFeedback(Text.translatable("nozh.perf.samples", s.sampleCount()));
        return 1;
    }

    private static void runEnable(FabricClientCommandSource source) {
        NozhConfig config = ConfigManager.getConfig();
        config.enabled = true;
        config.allowAutoTuning = true;
        ConfigManager.saveAndNotify();
        StateStore.getInstance().update(state -> state.withConfig(config));

        source.sendFeedback(Text.translatable("nozh.enable.success"));
    }

    private static void runDisable(FabricClientCommandSource source) {
        NozhConfig config = ConfigManager.getConfig();
        config.enabled = false;
        ConfigManager.saveAndNotify();
        StateStore.getInstance().update(state -> state.withConfig(config));

        source.sendFeedback(Text.translatable("nozh.disable.success"));
    }

    private static void runSpy(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            source.sendFeedback(Text.translatable("nozh.spy.no_world").styled(s -> s.withColor(0xFFAA00)));
            return;
        }

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        int total = 0;

        for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
            String name = entity.getType().getUntranslatedName();
            counts.put(name, counts.getOrDefault(name, 0) + 1);
            total++;
        }

        source.sendFeedback(NozhText.translatableHeader("nozh.spy.header"));
        source.sendFeedback(NozhText.translatableLabeled("nozh.spy.total", String.valueOf(total)));

        counts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    String name = Text.translatable(entry.getKey()).getString();
                    int count = entry.getValue();
                    int color = count > 50 ? 0xFF5555 : (count > 20 ? 0xFFAA00 : 0x55FF55);
                    source.sendFeedback(Text.literal(String.format("  %dx %s", count, name))
                            .styled(s -> s.withColor(color)));
                });
    }

    private static void runApplySuggestion(FabricClientCommandSource source) {
        if (MinecraftClient.getInstance() == null) {
            source.sendFeedback(Text.translatable("nozh.suggestion.apply.unavailable"));
            return;
        }
        NozhModClient.requestSuggestedAction();
    }

    private static void runClearSuggestion(FabricClientCommandSource source) {
        RuntimeState state = StateStore.getInstance().snapshotSafe();
        if (state.suggestedActions() == null || state.suggestedActions().isEmpty()) {
            source.sendFeedback(Text.translatable("nozh.suggestion.clear.none"));
            return;
        }
        StateStore.getInstance().update(RuntimeState::withSuggestedActionCleared);
        source.sendFeedback(Text.translatable("nozh.suggestion.clear.success"));
    }

    private static void runTelemetryExport(FabricClientCommandSource source, TelemetryExportFormat format) {
        var perfManager = NozhModClient.getPerfManager();
        if (perfManager == null) {
            source.sendFeedback(Text.translatable("nozh.telemetry.export.unavailable"));
            return;
        }
        try {
            var output = perfManager.exportTelemetry(
                    NozhConstants.CONFIG_DIR.resolve("telemetry_exports"),
                    format);
            source.sendFeedback(Text.translatable("nozh.telemetry.export.success", output.toString()));
        } catch (Exception e) {
            source.sendFeedback(Text.translatable("nozh.telemetry.export.failed", e.getMessage()));
        }
    }
}
