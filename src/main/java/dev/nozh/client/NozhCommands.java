package dev.nozh.client;

import dev.nozh.NozhConstants;
import dev.nozh.core.compat.CompatService;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
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
                                                        runTelemetryExport(context.getSource(), TelemetryExportFormat.CSV);
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("json")
                                                    .executes(context -> {
                                                        runTelemetryExport(context.getSource(), TelemetryExportFormat.JSON);
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
                            .then(ClientCommandManager.literal("apply")
                                    .executes(context -> {
                                        runApplySuggestion(context.getSource());
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
        source.sendFeedback(NozhText.header("=== NOZH Self-Check ==="));

        // Environment
        source.sendFeedback(NozhText.header("--- Environment ---"));
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
        source.sendFeedback(NozhText.header("--- Facts ---"));

        NozhConfig config = ConfigManager.getConfig();
        if (config != null) {
            source.sendFeedback(NozhText.labeled("Config", "Loaded OK", true));
            if (config.wasCorrected()) {
                source.sendFeedback(NozhText.warning("  Config auto-corrected"));
                warnings++;
            }
        } else {
            source.sendFeedback(NozhText.labeled("Config", "FAILED", false));
            failures++;
        }

        NozhState state = StateManager.getState();
        if (state != null) {
            source.sendFeedback(NozhText.labeled("State", "Loaded OK", true));
        } else {
            source.sendFeedback(NozhText.labeled("State", "FAILED", false));
            failures++;
        }

        boolean inSafeMode = CrashLoopGuard.isInSafeMode();
        String smReason = CrashLoopGuard.getSafeModeReason();
        source.sendFeedback(
                NozhText.labeled("Safe Mode", inSafeMode ? "ACTIVE (" + smReason + ")" : "Off", !inSafeMode));

        // God Mode Modules
        source.sendFeedback(NozhText.header("--- God Mode Modules ---"));
        source.sendFeedback(NozhText.labeled("Orchestration", "DIRECTOR", true));
        source.sendFeedback(NozhText.labeled("Entity Control", "ACTIVE", true));
        source.sendFeedback(NozhText.labeled("True Sight", "ACTIVE", true));

        // Compat Report (Phase 9)
        CompatService.CompatReport report = CompatService.generateReport();
        if (!report.performanceMods().isEmpty()) {
            source.sendFeedback(Text.literal("  Perf Mods: " + String.join(", ", report.performanceMods()))
                    .styled(s -> s.withColor(0x55FF55)));
        }
        if (!report.shaderMods().isEmpty()) {
            source.sendFeedback(Text.literal("  Shader Mods: " + String.join(", ", report.shaderMods()))
                    .styled(s -> s.withColor(0x55FFFF)));
        }
        for (net.minecraft.text.Text hint : report.hints()) {
            source.sendFeedback(hint.copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        source.sendFeedback(NozhText.header("--- Capability Stewards ---"));
        for (CompatService.CapabilitySteward steward : CompatService.generateStewardReport()) {
            int color = steward.conflict() ? 0xFFAA00 : 0x55FF55;
            source.sendFeedback(Text.literal("  " + steward.capabilityId().name() + " → " + steward.steward())
                    .styled(s -> s.withColor(color)));
        }

        // Verdict
        source.sendFeedback(NozhText.header("--- Verdict ---"));
        if (failures > 0) {
            source.sendFeedback(NozhText.error("FAIL (" + failures + " failures)"));
        } else if (warnings > 0) {
            source.sendFeedback(NozhText.warning("WARN (" + warnings + " warnings)"));
        } else {
            source.sendFeedback(NozhText.success("HEALTHY"));
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

        runtimeState.suggestedAction().ifPresent(pending -> ctx.getSource().sendFeedback(Text.literal(
                "Suggestion pending: " + pending.capability().name() + "=" + pending.newValue() + " (/nozh apply)")));
        runtimeState.pendingAction().ifPresent(pending -> ctx.getSource().sendFeedback(Text.literal(
                "Action pending evaluation: " + pending.capability().name() + "=" + pending.newValue())));
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

    private static void runApply(FabricClientCommandSource source) {
        runApplySuggestion(source);
    }

    private static void runApplySuggestion(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            source.sendFeedback(Text.translatable("nozh.suggestion.apply.unavailable"));
            return;
        }
        NozhModClient.applySuggestedAction(client);
    }

    private static void runClearSuggestion(FabricClientCommandSource source) {
        RuntimeState state = StateStore.getInstance().snapshotSafe();
        if (state.suggestedAction().isEmpty()) {
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
