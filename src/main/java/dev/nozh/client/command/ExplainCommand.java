package dev.nozh.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.nozh.core.governor.DecisionReasoning;
import dev.nozh.core.governor.IntegratedGovernor;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client command to explain the last governor decision.
 * Usage: /nozh explain
 * 
 * NOTE: This command requires a reference to IntegratedGovernor.
 * In a real implementation, you'd get this from your mod's client entry point.
 * For now, this is a placeholder that shows the structure.
 */
public class ExplainCommand {
    
    // TODO: Replace with actual governor instance from your mod
    private static IntegratedGovernor governor = null;
    
    /**
     * Set the governor instance (call this from your mod initialization).
     */
    public static void setGovernor(IntegratedGovernor gov) {
        governor = gov;
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("nozh")
                        .then(literal("explain")
                                .executes(ExplainCommand::explain)
                        )
        );
    }

    private static int explain(CommandContext<FabricClientCommandSource> ctx) {
        if (governor == null) {
            ctx.getSource().sendFeedback(Text.literal("§cGovernor not initialized. Please start the game first."));
            return Command.SINGLE_SUCCESS;
        }
        
        DecisionReasoning reasoning = governor.getLastDecisionReasoning();

        if (reasoning == null) {
            ctx.getSource().sendFeedback(Text.literal("§cNo decisions made yet."));
            return Command.SINGLE_SUCCESS;
        }

        // Use record accessor methods
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
}
