package dev.nozh.core.executor;

import dev.nozh.api.governor.Decision;

public interface ActionExecutor {
    ExecutionResult execute(Decision decision);

    /**
     * Phase 6.5: Revert the last executed action if possible.
     */
    void revertLast(net.minecraft.client.MinecraftClient client);
}
