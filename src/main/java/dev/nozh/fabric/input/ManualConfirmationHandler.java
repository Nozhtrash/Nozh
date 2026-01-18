package dev.nozh.fabric.input;

import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.state.ActionHistoryEntry;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.fabric.hud.NozhHudRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * PRIORITY 2: Manual mode confirmation handler.
 * 
 * Features:
 * - Keybind K to confirm suggested optimizations
 * - Keybind N to dismiss current suggestion
 * - Automatic timeout (60s)
 * - Queue management (max 3 suggestions)
 * - Visual feedback via HUD and chat
 * 
 * UX: Non-intrusive, clear feedback, easy to use.
 */
public final class ManualConfirmationHandler {

    private static final long SUGGESTION_TIMEOUT_MS = 60000; // 60s
    private static final int MAX_SUGGESTION_QUEUE = 3;

    private final StateStore stateStore;
    private final ActionBus actionBus;
    private final NozhHudRenderer hudRenderer;

    private final KeyBinding confirmKey;
    private final KeyBinding dismissKey;

    public ManualConfirmationHandler(
            StateStore stateStore,
            ActionBus actionBus,
            NozhHudRenderer hudRenderer,
            KeyBinding confirmKey,
            KeyBinding dismissKey) {

        this.stateStore = stateStore;
        this.actionBus = actionBus;
        this.hudRenderer = hudRenderer;

        // Register keybinds
        this.confirmKey = confirmKey != null
                ? confirmKey
                : KeyBindingHelper.registerKeyBinding(new KeyBinding(
                        "key.nozh.confirm_suggestion",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN, // Unbound - use QuickMenu (K) instead
                        "category.nozh"));

        this.dismissKey = dismissKey != null
                ? dismissKey
                : KeyBindingHelper.registerKeyBinding(new KeyBinding(
                        "key.nozh.dismiss_suggestion",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN, // Unbound - use QuickMenu (K) instead
                        "category.nozh"));

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick();
        });
    }

    private void tick() {
        // Clean up expired suggestions
        cleanupExpiredSuggestions();

        // Handle keybinds
        if (confirmKey.wasPressed()) {
            handleConfirm();
        }
        if (dismissKey.wasPressed()) {
            handleDismiss();
        }
    }

    private void handleConfirm() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();

        if (suggestions == null || suggestions.isEmpty()) {
            notifyClient(client,
                    Text.translatable("nozh.suggestion.apply.none"),
                    Text.translatable("nozh.suggestion.apply.none_detail"));
            return;
        }

        // Apply first suggestion
        PendingAction suggestion = suggestions.get(0);
        applySuggestion(client, suggestion);
    }

    private void handleDismiss() {
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();

        if (suggestions == null || suggestions.isEmpty()) {
            showMessage(Text.translatable("nozh.suggestion.dismiss.none"), true);
            return;
        }

        // FIXED: Remove first suggestion using existing method
        PendingAction suggestion = suggestions.get(0);
        try {
            stateStore.update(currentState -> {
                List<PendingAction> updated = new ArrayList<>(currentState.suggestedActions());
                updated.remove(suggestion);
                return createStateWithUpdatedSuggestions(currentState, updated);
            });
            showMessage(Text.translatable("nozh.suggestion.dismiss.success", suggestion.capability().name()), false);
        } catch (Exception e) {
            showMessage(Text.translatable("nozh.suggestion.dismiss.failed"), true);
        }
    }

    private void cleanupExpiredSuggestions() {
        long now = System.currentTimeMillis();
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();

        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }

        boolean needsCleanup = false;
        for (PendingAction suggestion : suggestions) {
            if (now - suggestion.timestampMillis() > SUGGESTION_TIMEOUT_MS) {
                needsCleanup = true;
                break;
            }
        }

        if (needsCleanup) {
            try {
                stateStore.update(currentState -> {
                    List<PendingAction> filtered = new ArrayList<>();
                    for (PendingAction s : currentState.suggestedActions()) {
                        if (now - s.timestampMillis() <= SUGGESTION_TIMEOUT_MS) {
                            filtered.add(s);
                        }
                    }
                    // FIXED: Use helper method to create new state
                    return createStateWithUpdatedSuggestions(currentState, filtered);
                });
            } catch (Exception e) {
                // Ignore update failures
            }
        }
    }

    /**
     * FIXED: Helper method to create a new RuntimeState with updated suggestions.
     * Uses existing RuntimeState constructor to rebuild the state.
     */
    private RuntimeState createStateWithUpdatedSuggestions(RuntimeState currentState, List<PendingAction> suggestions) {
        return new RuntimeState(
                currentState.enabled(),
                currentState.safeMode(),
                currentState.autoTuning(),
                currentState.debugLogs(),
                currentState.governorDisabled(),
                currentState.governorCooldownActive(),
                currentState.governorLastActionTimestamp(),
                currentState.benchmarkRunning(),
                currentState.benchmarkValidity(),
                currentState.benchmarkStartTimestamp(),
                currentState.pendingAction(),
                suggestions, // Updated suggestions list
                currentState.pendingActionsCount(),
                currentState.executionHistorySize(),
                currentState.lastSnapshotHistorySize(),
                currentState.actionHistory(),
                currentState.sessionChangesCount(),
                currentState.avgFrametimeMs(),
                currentState.p95FrametimeMs(),
                currentState.p99FrametimeMs(),
                currentState.frametimeStddevMs(),
                currentState.tickTimeAvg(),
                currentState.tickTimeP95(),
                currentState.spikeCount(),
                currentState.stabilityStats(),
                currentState.lastDecisionReason(),
                currentState.lastDecisionTimestamp(),
                currentState.lastImpactMs(),
                currentState.lastOutcome(),
                currentState.lastDecisionAccepted(),
                currentState.sessionStartTime(),
                currentState.stateVersion(),
                currentState.currentScenario(),
                currentState.scenarioConfidence(),
                currentState.lastScenarioChangeTimestamp(),
                currentState.scenarioChangeCount(),
                currentState.rapidScenarioChangeCount(),
                currentState.combatAfkFlipCount(),
                currentState.scenarioHistory(),
                currentState.baselineSettings(),
                currentState.currentSettings());
    }

    private void showMessage(Text message, boolean error) {
        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

            if (client.player != null) {
                Text prefix = Text.literal("[NOZH] ").formatted(Formatting.GRAY);
                Text styledMessage = message.copy().formatted(error ? Formatting.RED : Formatting.YELLOW);
                client.player.sendMessage(prefix.copy().append(styledMessage), true);
            }
        } catch (Exception e) {
            // Ignore messaging failures
        }
    }

    /**
     * Add a suggestion to the queue.
     * Returns false if queue is full.
     */
    public boolean addSuggestion(PendingAction suggestion) {
        if (!hasCapacity()) {
            return false;
        }

        try {
            stateStore.update(currentState -> {
                List<PendingAction> current = new ArrayList<>(currentState.suggestedActions());
                current.add(suggestion);
                return createStateWithUpdatedSuggestions(currentState, current);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if suggestion queue has capacity.
     */
    public boolean hasCapacity() {
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();
        return suggestions == null || suggestions.size() < MAX_SUGGESTION_QUEUE;
    }

    /**
     * Get current queue size.
     */
    public int getQueueSize() {
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();
        return suggestions == null ? 0 : suggestions.size();
    }

    /**
     * Clear all suggestions.
     */
    public void clearAll() {
        try {
            stateStore.update(currentState -> createStateWithUpdatedSuggestions(currentState, List.of()));
            showMessage(Text.translatable("nozh.suggestion.clear.success"), false);
        } catch (Exception e) {
            showMessage(Text.translatable("nozh.suggestion.clear.failed"), true);
        }
    }

    /**
     * Apply the next suggestion via external trigger (keybind or command).
     */
    public void requestApply() {
        handleConfirm();
    }

    /**
     * Get confirmation keybind for display.
     */
    public String getConfirmKeyName() {
        return confirmKey.getBoundKeyLocalizedText().getString();
    }

    /**
     * Get dismiss keybind for display.
     */
    public String getDismissKeyName() {
        return dismissKey.getBoundKeyLocalizedText().getString();
    }

    private void applySuggestion(MinecraftClient client, PendingAction pending) {
        if (actionBus == null) {
            notifyClient(client,
                    Text.translatable("nozh.suggestion.apply.failed"),
                    Text.translatable("nozh.suggestion.apply.unavailable"));
            return;
        }

        long now = System.currentTimeMillis();
        int maxHistoryEntries = ConfigManager.getConfig() != null ? ConfigManager.getConfig().historyMaxEntries : 50;
        ActionHistoryEntry actionEntry = new ActionHistoryEntry(
                now,
                pending.capability().name() + "=" + pending.newValue(),
                pending.scenario(),
                pending.scenarioConfidence(),
                pending.baselineSnapshot(),
                dev.nozh.api.PerfSnapshot.empty(),
                0.0,
                0,
                0,
                ActionOutcome.NEUTRAL,
                false);

        actionBus.dispatch(pending.command(), report -> {
            if (report.succeeded()) {
                notifyClient(client,
                        Text.translatable("nozh.suggestion.apply.success"),
                        Text.translatable("nozh.suggestion.apply.success_detail",
                                pending.capability().name(), pending.newValue().toString()));
                if (hudRenderer != null) {
                    hudRenderer.notifyActionApplied(
                            pending.capability().name(),
                            0.0);
                }
            } else {
                String reason = report.error().orElse("unknown");
                notifyClient(client,
                        Text.translatable("nozh.suggestion.apply.failed"),
                        Text.translatable("nozh.suggestion.apply.failed_detail", reason));
                stateStore.update(RuntimeState::withPendingActionCleared);
            }
        });

        stateStore.update(currentState -> currentState
                .withAppliedSuggestion(now, pending, actionEntry, maxHistoryEntries));
    }

    private void notifyClient(MinecraftClient client, Text title, Text message) {
        if (client == null) {
            return;
        }
        if (client.getToastManager() != null) {
            SystemToast.add(client.getToastManager(), SystemToast.Type.TUTORIAL_HINT, title, message);
        }
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(title.copy().append(Text.literal(" ")).append(message));
        }
        // Play sound for feedback
        client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
