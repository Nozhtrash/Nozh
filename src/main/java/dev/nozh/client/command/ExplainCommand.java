package dev.nozh.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.nozh.core.governor.DecisionLogger;
import dev.nozh.core.governor.DecisionReasoning;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * /nozh explain command - shows governor decision reasoning.
 * 
 * Usage:
 * - /nozh explain - Show latest decision
 * - /nozh explain latest - Same as above
 * - /nozh explain history - Show last 10 decisions
 * - /nozh explain history <count> - Show last N decisions
 * 
 * TASK 6: Explainable decisions - user interface
 */
public final class ExplainCommand {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return literal("explain")
                .executes(ExplainCommand::executeLatest)
                .then(literal("latest")
                        .executes(ExplainCommand::executeLatest))
                .then(literal("history")
                        .executes(ctx -> executeHistory(ctx, 10))
                        .then(argument("count", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> executeHistory(ctx, 
                                        IntegerArgumentType.getInteger(ctx, "count")))));
    }

    private static int executeLatest(CommandContext<FabricClientCommandSource> ctx) {
        DecisionLogger.DecisionEntry latest = DecisionLogger.getLatest();
        
        if (latest == null) {
            ctx.getSource().sendFeedback(Text.literal("§c[NOZH] No decisions recorded yet"));
            return Command.SINGLE_SUCCESS;
        }

        DecisionReasoning reasoning = latest.reasoning();
        
        ctx.getSource().sendFeedback(Text.literal("§e=== Latest Decision ==="));
        ctx.getSource().sendFeedback(Text.literal("§7Age: §f" + formatAge(latest.getAgeMs())));
        ctx.getSource().sendFeedback(Text.literal("§7Action: §a" + reasoning.getActionId()));
        ctx.getSource().sendFeedback(Text.literal("§7Scenario: §b" + reasoning.getScenario()));
        ctx.getSource().sendFeedback(Text.literal("§7Confidence: §f" + 
                String.format("%.1f%%", reasoning.getConfidenceScore() * 100)));
        
        if (!reasoning.getTriggers().isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§7Triggers:"));
            for (String trigger : reasoning.getTriggers()) {
                ctx.getSource().sendFeedback(Text.literal("  §7• §f" + trigger));
            }
        }
        
        if (!reasoning.getSignals().isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§7Signals:"));
            for (String signal : reasoning.getSignals()) {
                ctx.getSource().sendFeedback(Text.literal("  §7• §f" + signal));
            }
        }
        
        ctx.getSource().sendFeedback(Text.literal("§7Expected: §a" + reasoning.getExpectedOutcome()));
        
        if (!reasoning.getAlternatives().isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§7Alternatives: §f" + 
                    reasoning.getAlternatives().size() + " considered"));
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private static int executeHistory(CommandContext<FabricClientCommandSource> ctx, int count) {
        List<DecisionLogger.DecisionEntry> history = DecisionLogger.getRecentHistory(count);
        
        if (history.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§c[NOZH] No decision history"));
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendFeedback(Text.literal(
                "§e=== Decision History (§f" + history.size() + "§e) ==="));
        
        for (int i = 0; i < history.size(); i++) {
            DecisionLogger.DecisionEntry entry = history.get(i);
            DecisionReasoning reasoning = entry.reasoning();
            
            String prefix = (i == history.size() - 1) ? "§a▶" : "§7•";
            
            ctx.getSource().sendFeedback(Text.literal(
                    prefix + " §f" + formatAge(entry.getAgeMs()) + " ago: " +
                    "§a" + reasoning.getActionId() + " §7(§b" + 
                    reasoning.getScenario() + "§7)"));
        }
        
        ctx.getSource().sendFeedback(Text.literal(
                "§7Tip: Use §f/nozh explain latest§7 for details"));
        
        return Command.SINGLE_SUCCESS;
    }

    private static String formatAge(long ageMs) {
        if (ageMs < 1000) {
            return ageMs + "ms";
        } else if (ageMs < 60000) {
            return (ageMs / 1000) + "s";
        } else {
            return (ageMs / 60000) + "m" + ((ageMs % 60000) / 1000) + "s";
        }
    }
}
