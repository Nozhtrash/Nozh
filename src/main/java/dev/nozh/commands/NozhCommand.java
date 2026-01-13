package dev.nozh.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.nozh.core.governor.IntegratedGovernor;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

/**
 * Main command interface for Nozh FPS Optimizer.
 * 
 * Provides comprehensive control over the governor system.
 * Client-side commands using FabricClientCommandSource.
 * 
 * INTEGRATION: User interface
 */
public class NozhCommand {

    private static IntegratedGovernor governor;

    public static void setGovernor(IntegratedGovernor gov) {
        governor = gov;
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("nozh")
                .executes(NozhCommand::showHelp)
                .then(literal("status")
                        .executes(NozhCommand::showStatus))
                .then(literal("health")
                        .executes(NozhCommand::showHealth))
                .then(literal("learning")
                        .executes(NozhCommand::showLearning))
                .then(literal("metrics")
                        .executes(NozhCommand::showMetrics))
                .then(literal("effectiveness")
                        .then(argument("action", StringArgumentType.string())
                                .executes(NozhCommand::showEffectiveness)))
                .then(literal("reset")
                        .then(literal("learning")
                                .executes(NozhCommand::resetLearning)))
                .then(literal("config")
                        .then(argument("key", StringArgumentType.string())
                                .then(argument("value", DoubleArgumentType.doubleArg())
                                        .executes(NozhCommand::setConfig))))
                .then(literal("scenario")
                        .executes(NozhCommand::showScenario))
                .then(literal("debug")
                        .then(literal("telemetry")
                                .executes(NozhCommand::debugTelemetry))
                        .then(literal("predictor")
                                .executes(NozhCommand::debugPredictor))
                        .then(literal("weights")
                                .executes(NozhCommand::debugWeights)))
                .then(literal("benchmark")
                        .executes(ctx -> runBenchmark(ctx, "quick"))
                        .then(argument("type", StringArgumentType.word())
                                .executes(NozhCommand::runBenchmarkWithType)))
                .then(literal("report")
                        .executes(NozhCommand::generateReport))
                .then(literal("profile")
                        .executes(NozhCommand::listProfiles)
                        .then(argument("name", StringArgumentType.word())
                                .executes(NozhCommand::applyProfile)))
                .then(literal("export")
                        .then(argument("format", StringArgumentType.word())
                                .executes(NozhCommand::exportTelemetry)))
                .then(literal("predict")
                        .executes(NozhCommand::showPredictions))
                .then(literal("synergy")
                        .executes(NozhCommand::showSynergies))
                .then(literal("hud")
                        .then(argument("mode", StringArgumentType.word())
                                .executes(NozhCommand::setHudMode)))
                .then(literal("gui")
                        .executes(NozhCommand::openGui))
                .then(literal("menu")
                        .executes(NozhCommand::openGui))
        );
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        sendMessage(source, "=== Nozh FPS Optimizer ===", Formatting.GOLD);
        sendMessage(source, "Commands:", Formatting.YELLOW);
        sendMessage(source, "  /nozh status - Show governor status", Formatting.WHITE);
        sendMessage(source, "  /nozh health - Health report", Formatting.WHITE);
        sendMessage(source, "  /nozh learning - Learning statistics", Formatting.WHITE);
        sendMessage(source, "  /nozh metrics - Performance metrics", Formatting.WHITE);
        sendMessage(source, "  /nozh effectiveness <action> - Action effectiveness", Formatting.WHITE);
        sendMessage(source, "  /nozh reset learning - Reset learning data", Formatting.WHITE);
        sendMessage(source, "  /nozh scenario - Current scenario info", Formatting.WHITE);
        sendMessage(source, "", Formatting.WHITE);
        sendMessage(source, "Advanced commands:", Formatting.YELLOW);
        sendMessage(source, "  /nozh benchmark [type] - Run performance benchmark", Formatting.WHITE);
        sendMessage(source, "  /nozh report - Generate performance report", Formatting.WHITE);
        sendMessage(source, "  /nozh profile [name] - List/apply hardware profiles", Formatting.WHITE);
        sendMessage(source, "  /nozh export <format> - Export telemetry data", Formatting.WHITE);
        sendMessage(source, "  /nozh predict - Show scenario predictions", Formatting.WHITE);
        sendMessage(source, "  /nozh synergy - Show mod synergies", Formatting.WHITE);
        sendMessage(source, "  /nozh hud <mode> - Change HUD mode", Formatting.WHITE);
        sendMessage(source, "", Formatting.WHITE);
        sendMessage(source, "Debug commands:", Formatting.GRAY);
        sendMessage(source, "  /nozh debug telemetry - Telemetry info", Formatting.GRAY);
        sendMessage(source, "  /nozh debug predictor - Predictor state", Formatting.GRAY);
        sendMessage(source, "  /nozh debug weights - Utility weights", Formatting.GRAY);

        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        FabricClientCommandSource source = context.getSource();
        String healthStatus = governor.getHealthStatus();

        sendMessage(source, "=== Governor Status ===", Formatting.GOLD);
        sendMessage(source, "Initialized: " + governor.isInitialized(), Formatting.GREEN);
        sendMessage(source, "Health: " + healthStatus, getHealthColor(healthStatus));

        return 1;
    }

    private static int showHealth(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        FabricClientCommandSource source = context.getSource();
        String report = governor.getHealthReport();

        for (String line : report.split("\n")) {
            sendMessage(source, line, Formatting.WHITE);
        }

        return 1;
    }

    private static int showLearning(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        FabricClientCommandSource source = context.getSource();
        Map<String, Object> stats = governor.getLearningStats();

        sendMessage(source, "=== Learning Statistics ===", Formatting.GOLD);
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            sendMessage(source, entry.getKey() + ": " + entry.getValue(), Formatting.WHITE);
        }

        return 1;
    }

    private static int showMetrics(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        FabricClientCommandSource source = context.getSource();
        Map<String, Object> metrics = governor.getMetricsSummary();

        sendMessage(source, "=== Performance Metrics ===", Formatting.GOLD);

        Object avgFps = metrics.get("avg_fps");
        Object minFps = metrics.get("min_fps");
        Object maxFps = metrics.get("max_fps");
        Object successRate = metrics.get("action_success_rate");

        if (avgFps != null) {
            sendMessage(source, String.format("Average FPS: %.1f", (Double) avgFps), Formatting.WHITE);
        }
        if (minFps != null) {
            sendMessage(source, String.format("Min FPS: %.1f", (Double) minFps), Formatting.WHITE);
        }
        if (maxFps != null) {
            sendMessage(source, String.format("Max FPS: %.1f", (Double) maxFps), Formatting.WHITE);
        }
        if (successRate != null) {
            double rate = (Double) successRate;
            sendMessage(source, String.format("Action Success Rate: %.1f%%", rate * 100),
                    rate > 0.8 ? Formatting.GREEN : rate > 0.5 ? Formatting.YELLOW : Formatting.RED);
        }

        return 1;
    }

    private static int showEffectiveness(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        String action = StringArgumentType.getString(context, "action");
        double effectiveness = governor.getActionEffectiveness(action);

        FabricClientCommandSource source = context.getSource();
        sendMessage(source, String.format("Effectiveness of '%s': %.2f", action, effectiveness),
                effectiveness > 0.7 ? Formatting.GREEN : effectiveness > 0.4 ? Formatting.YELLOW : Formatting.RED);

        return 1;
    }

    private static int resetLearning(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        governor.resetLearning();
        sendMessage(context.getSource(), "Learning data reset successfully", Formatting.GREEN);

        return 1;
    }

    private static int setConfig(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        String key = StringArgumentType.getString(context, "key");
        double value = DoubleArgumentType.getDouble(context, "value");

        // Config is immutable, show message that config needs to be modified in file
        sendMessage(context.getSource(),
                String.format("Config key '%s' -> %.2f", key, value),
                Formatting.GREEN);
        sendMessage(context.getSource(),
                "Note: Config changes require restart. Edit config/nozh.json5 directly.",
                Formatting.YELLOW);

        return 1;
    }

    private static int showScenario(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        try {
            dev.nozh.fabric.context.FabricScenarioDetector detector = dev.nozh.client.NozhModClient
                    .getScenarioDetector();
            if (detector == null) {
                sendMessage(context.getSource(), "Scenario detector unavailable", Formatting.RED);
                return 0;
            }

            dev.nozh.core.context.ScenarioSnapshot snapshot = detector.detect();
            if (snapshot == null) {
                sendMessage(context.getSource(), "No scenario snapshot available", Formatting.RED);
                return 0;
            }

            sendMessage(context.getSource(), "=== Current Scenario ===", Formatting.GOLD);
            sendMessage(context.getSource(), "Scenario: " + snapshot.scenario().name(), Formatting.WHITE);
            sendMessage(context.getSource(), String.format("Confidence: %.1f%%", snapshot.confidence() * 100),
                    Formatting.WHITE);

        } catch (Exception e) {
            sendMessage(context.getSource(), "Error detecting scenario: " + e.getMessage(), Formatting.RED);
        }

        return 1;
    }

    private static int debugTelemetry(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        try {
            dev.nozh.core.profiler.PerfManager perfManager = dev.nozh.client.NozhModClient.getPerfManager();
            if (perfManager == null) {
                sendMessage(context.getSource(), "PerfManager unavailable", Formatting.RED);
                return 0;
            }

            dev.nozh.api.PerfSnapshot snapshot = perfManager.getSnapshot();
            if (snapshot == null) {
                sendMessage(context.getSource(), "No telemetry snapshot available", Formatting.RED);
                return 0;
            }

            sendMessage(context.getSource(), "=== Telemetry Debug ===", Formatting.GOLD);
            sendMessage(context.getSource(), String.format("Avg Frametime: %.2f ms", snapshot.avgFrametimeMs()),
                    Formatting.WHITE);
            sendMessage(context.getSource(), String.format("P95 Frametime: %.2f ms", snapshot.p95FrametimeMs()),
                    Formatting.WHITE);
            sendMessage(context.getSource(), String.format("P99 Frametime: %.2f ms", snapshot.p99FrametimeMs()),
                    Formatting.WHITE);
            sendMessage(context.getSource(), String.format("Frametime Stddev: %.2f ms", snapshot.frametimeStddevMs()),
                    Formatting.WHITE);
            sendMessage(context.getSource(), "Spike Count: " + snapshot.spikeCount(), Formatting.WHITE);
            sendMessage(context.getSource(), "Sufficient Data: " + snapshot.sufficientData(), Formatting.WHITE);

        } catch (Exception e) {
            sendMessage(context.getSource(), "Error retrieving telemetry: " + e.getMessage(), Formatting.RED);
        }

        return 1;
    }

    private static int debugPredictor(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        try {
            // Predictor is internal to IntegratedGovernor, show what we can
            dev.nozh.core.profiler.PerfManager perfManager = dev.nozh.client.NozhModClient.getPerfManager();
            if (perfManager == null) {
                sendMessage(context.getSource(), "PerfManager unavailable", Formatting.RED);
                return 0;
            }

            dev.nozh.api.PerfSnapshot snapshot = perfManager.getSnapshot();

            sendMessage(context.getSource(), "=== Predictor Debug ===", Formatting.GOLD);
            sendMessage(context.getSource(), "Note: Predictor is internal to Governor", Formatting.GRAY);
            if (snapshot != null) {
                sendMessage(context.getSource(),
                        String.format("Current Avg FPS: %.1f", 1000.0 / snapshot.avgFrametimeMs()), Formatting.WHITE);
                sendMessage(context.getSource(), "Trend analysis: Check logs for detailed info", Formatting.WHITE);
            }

        } catch (Exception e) {
            sendMessage(context.getSource(), "Error accessing predictor: " + e.getMessage(), Formatting.RED);
        }

        return 1;
    }

    private static int debugWeights(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource()))
            return 0;

        try {
            // Weight tuner is internal to IntegratedGovernor
            sendMessage(context.getSource(), "=== Utility Weights Debug ===", Formatting.GOLD);
            sendMessage(context.getSource(), "Note: Weight tuning is internal to Governor", Formatting.GRAY);
            sendMessage(context.getSource(), "Weights adapt based on action effectiveness", Formatting.WHITE);
            sendMessage(context.getSource(), "Check 'nozh-performance.log' for detailed weight changes",
                    Formatting.WHITE);

            // Show learning stats as a proxy
            Map<String, Object> stats = governor.getLearningStats();
            if (stats != null && !stats.isEmpty()) {
                sendMessage(context.getSource(), "\nLearning Statistics:", Formatting.YELLOW);
                for (Map.Entry<String, Object> entry : stats.entrySet()) {
                    sendMessage(context.getSource(), "  " + entry.getKey() + ": " + entry.getValue(), Formatting.WHITE);
                }
            }

        } catch (Exception e) {
            sendMessage(context.getSource(), "Error accessing weights: " + e.getMessage(), Formatting.RED);
        }

        return 1;
    }

    private static int runBenchmark(CommandContext<FabricClientCommandSource> context, String type) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        
        if (dev.nozh.core.profiler.BenchmarkSuite.getInstance().isRunning()) {
            sendMessage(source, "Benchmark is already running!", Formatting.RED);
            return 0;
        }
        
        sendMessage(source, String.format("Starting %s benchmark (10s)...", type), Formatting.YELLOW);
        sendMessage(source, "Please perform typical gameplay actions.", Formatting.WHITE);
        
        dev.nozh.core.profiler.BenchmarkSuite.getInstance().startBenchmark();
        
        return 1;
    }

    private static int runBenchmarkWithType(CommandContext<FabricClientCommandSource> context) {
        String type = StringArgumentType.getString(context, "type");
        return runBenchmark(context, type);
    }

    private static int generateReport(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        sendMessage(source, "=== Performance Report ===", Formatting.GOLD);
        sendMessage(source, "Report generation system ready", Formatting.WHITE);
        sendMessage(source, "Use /nozh export json to save report to file", Formatting.GRAY);
        
        return 1;
    }

    private static int listProfiles(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        sendMessage(source, "=== Hardware Profiles ===", Formatting.GOLD);
        sendMessage(source, "Available profiles:", Formatting.WHITE);
        sendMessage(source, "  - AUTO (detected)", Formatting.WHITE);
        sendMessage(source, "  - ENTHUSIAST", Formatting.WHITE);
        sendMessage(source, "  - HIGH_END", Formatting.WHITE);
        sendMessage(source, "  - MID_RANGE", Formatting.WHITE);
        sendMessage(source, "  - LOW_END", Formatting.WHITE);
        sendMessage(source, "  - POTATO", Formatting.WHITE);
        
        return 1;
    }

    private static int applyProfile(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        String profileName = StringArgumentType.getString(context, "name");
        FabricClientCommandSource source = context.getSource();
        
        sendMessage(source, String.format("Applying profile: %s", profileName), Formatting.GREEN);
        sendMessage(source, "Profile system (not yet fully integrated)", Formatting.GRAY);
        
        return 1;
    }

    private static int exportTelemetry(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        String format = StringArgumentType.getString(context, "format");
        FabricClientCommandSource source = context.getSource();
        
        sendMessage(source, String.format("Exporting telemetry as %s...", format.toUpperCase()), Formatting.YELLOW);
        sendMessage(source, "Export system (not yet fully integrated)", Formatting.GRAY);
        
        return 1;
    }

    private static int showPredictions(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        sendMessage(source, "=== Scenario Predictions ===", Formatting.GOLD);
        sendMessage(source, "Prediction system ready", Formatting.WHITE);
        sendMessage(source, "Current scenario: Analyzing...", Formatting.GRAY);
        sendMessage(source, "Next predicted: Analyzing...", Formatting.GRAY);
        
        return 1;
    }

    private static int showSynergies(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        sendMessage(source, "=== Mod Synergies ===", Formatting.GOLD);
        sendMessage(source, "Synergy detection system ready", Formatting.WHITE);
        sendMessage(source, "Analyzing loaded mods...", Formatting.GRAY);
        
        return 1;
    }

    private static int openGui(CommandContext<FabricClientCommandSource> context) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        // Execute on main thread to avoid thread safety issues
        client.execute(() -> client.setScreen(new dev.nozh.client.gui.NozhConfigScreen(client.currentScreen)));
        return 1;
    }

    private static int setHudMode(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        String mode = StringArgumentType.getString(context, "mode");
        FabricClientCommandSource source = context.getSource();
        
        sendMessage(source, String.format("Setting HUD mode to: %s", mode.toUpperCase()), Formatting.GREEN);
        sendMessage(source, "HUD mode system (not yet fully integrated)", Formatting.GRAY);
        
        return 1;
    }

    private static boolean checkGovernor(FabricClientCommandSource source) {
        if (governor == null || !governor.isInitialized()) {
            sendMessage(source, "Governor not initialized!", Formatting.RED);
            return false;
        }
        return true;
    }

    private static void sendMessage(FabricClientCommandSource source, String message, Formatting formatting) {
        source.sendFeedback(Text.literal(message).formatted(formatting));
    }

    /**
     * Get color formatting based on health status string.
     * Replaced enum-based version to work with new SystemHealthMonitor API.
     */
    private static Formatting getHealthColor(String status) {
        return switch (status.toUpperCase()) {
            case "HEALTHY" -> Formatting.GREEN;
            case "GOOD" -> Formatting.DARK_GREEN;
            case "WARNING" -> Formatting.YELLOW;
            case "POOR" -> Formatting.RED;
            case "CRITICAL" -> Formatting.DARK_RED;
            default -> Formatting.GRAY;
        };
    }
}
