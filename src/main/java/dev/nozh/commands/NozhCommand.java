package dev.nozh.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.nozh.core.governor.IntegratedGovernor;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

/**
 * Main command interface for Nozh FPS Optimizer.
 * 
 * Provides comprehensive control over the governor system.
 * 
 * INTEGRATION: User interface
 */
public class NozhCommand {

    private static IntegratedGovernor governor;

    public static void setGovernor(IntegratedGovernor gov) {
        governor = gov;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("nozh")
                .executes(NozhCommand::showHelp)
                .then(CommandManager.literal("status")
                        .executes(NozhCommand::showStatus))
                .then(CommandManager.literal("health")
                        .executes(NozhCommand::showHealth))
                .then(CommandManager.literal("learning")
                        .executes(NozhCommand::showLearning))
                .then(CommandManager.literal("metrics")
                        .executes(NozhCommand::showMetrics))
                .then(CommandManager.literal("effectiveness")
                        .then(CommandManager.argument("action", StringArgumentType.string())
                                .executes(NozhCommand::showEffectiveness)))
                .then(CommandManager.literal("reset")
                        .then(CommandManager.literal("learning")
                                .executes(NozhCommand::resetLearning)))
                .then(CommandManager.literal("config")
                        .then(CommandManager.argument("key", StringArgumentType.string())
                                .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(NozhCommand::setConfig))))
                .then(CommandManager.literal("scenario")
                        .executes(NozhCommand::showScenario))
                .then(CommandManager.literal("debug")
                        .then(CommandManager.literal("telemetry")
                                .executes(NozhCommand::debugTelemetry))
                        .then(CommandManager.literal("predictor")
                                .executes(NozhCommand::debugPredictor))
                        .then(CommandManager.literal("weights")
                                .executes(NozhCommand::debugWeights)))
        );
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
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
        sendMessage(source, "Debug commands:", Formatting.GRAY);
        sendMessage(source, "  /nozh debug telemetry - Telemetry info", Formatting.GRAY);
        sendMessage(source, "  /nozh debug predictor - Predictor state", Formatting.GRAY);
        sendMessage(source, "  /nozh debug weights - Utility weights", Formatting.GRAY);
        
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        ServerCommandSource source = context.getSource();
        
        sendMessage(source, "=== Governor Status ===", Formatting.GOLD);
        sendMessage(source, "Initialized: " + governor.isInitialized(), Formatting.GREEN);
        sendMessage(source, "Health: " + governor.getHealthStatus(), getHealthColor(governor.getHealthStatus()));
        
        return 1;
    }

    private static int showHealth(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        ServerCommandSource source = context.getSource();
        String report = governor.getHealthReport();
        
        for (String line : report.split("\n")) {
            sendMessage(source, line, Formatting.WHITE);
        }
        
        return 1;
    }

    private static int showLearning(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        ServerCommandSource source = context.getSource();
        Map<String, Object> stats = governor.getLearningStats();
        
        sendMessage(source, "=== Learning Statistics ===", Formatting.GOLD);
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            sendMessage(source, entry.getKey() + ": " + entry.getValue(), Formatting.WHITE);
        }
        
        return 1;
    }

    private static int showMetrics(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        ServerCommandSource source = context.getSource();
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

    private static int showEffectiveness(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        String action = StringArgumentType.getString(context, "action");
        double effectiveness = governor.getActionEffectiveness(action);
        
        ServerCommandSource source = context.getSource();
        sendMessage(source, String.format("Effectiveness of '%s': %.2f", action, effectiveness), 
                effectiveness > 0.7 ? Formatting.GREEN : effectiveness > 0.4 ? Formatting.YELLOW : Formatting.RED);
        
        return 1;
    }

    private static int resetLearning(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        governor.resetLearning();
        sendMessage(context.getSource(), "Learning data reset successfully", Formatting.GREEN);
        
        return 1;
    }

    private static int setConfig(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        String key = StringArgumentType.getString(context, "key");
        double value = DoubleArgumentType.getDouble(context, "value");
        
        // TODO: Implement config setter through governor
        sendMessage(context.getSource(), 
                String.format("Config '%s' set to %.2f (not yet implemented)", key, value), 
                Formatting.YELLOW);
        
        return 1;
    }

    private static int showScenario(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get current scenario from governor
        sendMessage(context.getSource(), "Current scenario info (not yet implemented)", Formatting.YELLOW);
        
        return 1;
    }

    private static int debugTelemetry(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get telemetry debug info
        sendMessage(context.getSource(), "Telemetry debug info (not yet implemented)", Formatting.GRAY);
        
        return 1;
    }

    private static int debugPredictor(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get predictor debug info
        sendMessage(context.getSource(), "Predictor debug info (not yet implemented)", Formatting.GRAY);
        
        return 1;
    }

    private static int debugWeights(CommandContext<ServerCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get current weights
        sendMessage(context.getSource(), "Utility weights info (not yet implemented)", Formatting.GRAY);
        
        return 1;
    }

    private static boolean checkGovernor(ServerCommandSource source) {
        if (governor == null || !governor.isInitialized()) {
            sendMessage(source, "Governor not initialized!", Formatting.RED);
            return false;
        }
        return true;
    }

    private static void sendMessage(ServerCommandSource source, String message, Formatting formatting) {
        source.sendFeedback(() -> Text.literal(message).formatted(formatting), false);
    }

    private static Formatting getHealthColor(dev.nozh.core.monitoring.SystemHealthMonitor.HealthStatus status) {
        return switch (status) {
            case HEALTHY -> Formatting.GREEN;
            case DEGRADED -> Formatting.YELLOW;
            case UNHEALTHY -> Formatting.RED;
            case CRITICAL -> Formatting.DARK_RED;
        };
    }
}
