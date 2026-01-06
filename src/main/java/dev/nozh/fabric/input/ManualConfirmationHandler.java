package dev.nozh.fabric.input;

import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.Command;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.fabric.hud.NozhHudRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
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
            NozhHudRenderer hudRenderer) {
        
        this.stateStore = stateStore;
        this.actionBus = actionBus;
        this.hudRenderer = hudRenderer;

        // Register keybinds
        this.confirmKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nozh.confirm_suggestion",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.nozh"
        ));

        this.dismissKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nozh.dismiss_suggestion",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.nozh"
        ));

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
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();

        if (suggestions == null || suggestions.isEmpty()) {
            showMessage("⚠️ No pending suggestions", true);
            return;
        }

        // Apply first suggestion
        PendingAction suggestion = suggestions.get(0);
        Command command = suggestion.command();

        showMessage("✓ Applying: " + suggestion.capability().name(), false);

        // Dispatch action
        actionBus.dispatch(command, report -> {
            if (report.succeeded()) {
                showMessage("✓ Applied successfully", false);
                if (hudRenderer != null) {
                    hudRenderer.notifyActionApplied(
                        suggestion.capability().name(), 
                        0.0
                    );
                }
            } else {
                showMessage("❌ Failed: " + report.error().orElse("unknown"), true);
            }
        });

        // FIXED: Remove from suggestion queue using existing method
        try {
            stateStore.update(currentState -> {
                List<PendingAction> updated = new ArrayList<>(currentState.suggestedActions());
                updated.remove(suggestion);
                return createStateWithUpdatedSuggestions(currentState, updated);
            });
        } catch (Exception e) {
            // Ignore update failures
        }
    }

    private void handleDismiss() {
        RuntimeState state = stateStore.snapshotSafe();
        List<PendingAction> suggestions = state.suggestedActions();

        if (suggestions == null || suggestions.isEmpty()) {
            showMessage("⚠️ No pending suggestions", true);
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
            showMessage("❌ Dismissed: " + suggestion.capability().name(), false);
        } catch (Exception e) {
            showMessage("❌ Failed to dismiss", true);
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
            currentState.currentSettings()
        );
    }

    private void showMessage(String message, boolean error) {
        try {
            net.minecraft.client.MinecraftClient client = 
                net.minecraft.client.MinecraftClient.getInstance();
            
            if (client.player != null) {
                String color = error ? "\u00a7c" : "\u00a7e";
                Text text = Text.literal(color + "[NOZH] \u00a7r" + message);
                client.player.sendMessage(text, true); // Actionbar
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
            stateStore.update(currentState -> 
                createStateWithUpdatedSuggestions(currentState, List.of())
            );
            showMessage("All suggestions cleared", false);
        } catch (Exception e) {
            showMessage("Failed to clear suggestions", true);
        }
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
}
