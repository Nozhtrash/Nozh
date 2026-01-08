package dev.nozh.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.nozh.core.governor.DecisionReasoning;
import dev.nozh.fabric.client.NozhClientMod;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client command to explain the last governor decision.
 * Usage: /nozh explain
 */
public class ExplainCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("nozh")
                        .then(literal("explain")
                                .executes(ExplainCommand::explain)
                        )
        );
    }

    private static int explain(CommandContext<FabricClientCommandSource> ctx) {
        DecisionReasoning reasoning = NozhClientMod.getLastDecisionReasoning();

        if (reasoning == null) {
            ctx.getSource().sendFeedback(Text.literal("§cNo decisions made yet."));
            return Command.SINGLE_SUCCESS;
        }

        // FIX: Adapt to new DecisionReasoning record structure
        // Use record accessor methods: scenario(), currentFps(), etc.
        
        ctx.getSource().sendFeedback(Text.literal("§6=== Last Decision Explanation ==="));
        ctx.getSource().sendFeedback(Text.literal("§7Scenario: §f" + reasoning.scenario()));
        ctx.getSource().sendFeedback(Text.literal(
            String.format("§7Performance: §f%.1f FPS / %.0f target",
                reasoning.currentFps(),
                reasoning.targetFps()
            )
        ));
        ctx.getSource().sendFeedback(Text.literal(
            String.format("§7Utility Score: §f%.3f", reasoning.utilityScore())
        ));
        ctx.getSource().sendFeedback(Text.literal(
            String.format("§7Q-Value: §f%.3f", reasoning.qValue())
        ));
        
        if (reasoning.predictedDrop()) {
            ctx.getSource().sendFeedback(Text.literal("§e⚠ Frame drop predicted"));
        }
        
        if (reasoning.spikeCount() > 0) {
            ctx.getSource().sendFeedback(Text.literal(
                String.format("§e⚠ %d frame spikes detected", reasoning.spikeCount())
            ));
        }
        
        // Show full rationale
        ctx.getSource().sendFeedback(Text.literal("§7Rationale: §f" + reasoning.rationale()));
        
        return Command.SINGLE_SUCCESS;
    }

    private static int formatShort(CommandContext<FabricClientCommandSource> ctx) {
        DecisionReasoning reasoning = NozhClientMod.getLastDecisionReasoning();

        if (reasoning == null) {
            ctx.getSource().sendFeedback(Text.literal("§cNo decisions made yet."));
            return Command.SINGLE_SUCCESS;
        }

        // FIX: Use toString() which returns rationale
        String summary = String.format(
            "§7Last decision: %s §8(FPS: %.1f/%.0f)",
            reasoning.scenario(),
            reasoning.currentFps(),
            reasoning.targetFps()
        );

        ctx.getSource().sendFeedback(Text.literal(summary));
        return Command.SINGLE_SUCCESS;
    }
}
