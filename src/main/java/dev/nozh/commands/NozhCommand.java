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
        sendMessage(source, "Debug commands:", Formatting.GRAY);
        sendMessage(source, "  /nozh debug telemetry - Telemetry info", Formatting.GRAY);
        sendMessage(source, "  /nozh debug predictor - Predictor state", Formatting.GRAY);
        sendMessage(source, "  /nozh debug weights - Utility weights", Formatting.GRAY);
        
        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        
        sendMessage(source, "=== Governor Status ===", Formatting.GOLD);
        sendMessage(source, "Initialized: " + governor.isInitialized(), Formatting.GREEN);
        sendMessage(source, "Health: " + governor.getHealthStatus(), getHealthColor(governor.getHealthStatus()));
        
        return 1;
    }

    private static int showHealth(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        String report = governor.getHealthReport();
        
        for (String line : report.split("\n")) {
            sendMessage(source, line, Formatting.WHITE);
        }
        
        return 1;
    }

    private static int showLearning(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        FabricClientCommandSource source = context.getSource();
        Map<String, Object> stats = governor.getLearningStats();
        
        sendMessage(source, "=== Learning Statistics ===", Formatting.GOLD);
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            sendMessage(source, entry.getKey() + ": " + entry.getValue(), Formatting.WHITE);
        }
        
        return 1;
    }

    private static int showMetrics(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
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
        if (!checkGovernor(context.getSource())) return 0;
        
        String action = StringArgumentType.getString(context, "action");
        double effectiveness = governor.getActionEffectiveness(action);
        
        FabricClientCommandSource source = context.getSource();
        sendMessage(source, String.format("Effectiveness of '%s': %.2f", action, effectiveness), 
                effectiveness > 0.7 ? Formatting.GREEN : effectiveness > 0.4 ? Formatting.YELLOW : Formatting.RED);
        
        return 1;
    }

    private static int resetLearning(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        governor.resetLearning();
        sendMessage(context.getSource(), "Learning data reset successfully", Formatting.GREEN);
        
        return 1;
    }

    private static int setConfig(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        String key = StringArgumentType.getString(context, "key");
        double value = DoubleArgumentType.getDouble(context, "value");
        
        // TODO: Implement config setter through governor
        sendMessage(context.getSource(), 
                String.format("Config '%s' set to %.2f (not yet implemented)", key, value), 
                Formatting.YELLOW);
        
        return 1;
    }

    private static int showScenario(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get current scenario from governor
        sendMessage(context.getSource(), "Current scenario info (not yet implemented)", Formatting.YELLOW);
        
        return 1;
    }

    private static int debugTelemetry(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get telemetry debug info
        sendMessage(context.getSource(), "Telemetry debug info (not yet implemented)", Formatting.GRAY);
        
        return 1;
    }

    private static int debugPredictor(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get predictor debug info
        sendMessage(context.getSource(), "Predictor debug info (not yet implemented)", Formatting.GRAY);
        
        return 1;
    }

    private static int debugWeights(CommandContext<FabricClientCommandSource> context) {
        if (!checkGovernor(context.getSource())) return 0;
        
        // TODO: Get current weights
        sendMessage(context.getSource(), "Utility weights info (not yet implemented)", Formatting.GRAY);
        
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

    private static Formatting getHealthColor(dev.nozh.core.monitoring.SystemHealthMonitor.HealthStatus status) {
        return switch (status) {
            case HEALTHY -> Formatting.GREEN;
            case DEGRADED -> Formatting.YELLOW;
            case UNHEALTHY -> Formatting.RED;
            case CRITICAL -> Formatting.DARK_RED;
        };
    }
}
