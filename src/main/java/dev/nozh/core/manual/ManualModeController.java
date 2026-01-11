package dev.nozh.core.manual;

import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.NozhLogger;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * Manual mode controller for suggestion-based optimization.
 * 
 * In manual mode, NOZH suggests actions but waits for user confirmation
 * via keybind (default: K) before applying them.
 * 
 * Features:
 * - Queue of up to 3 pending suggestions
 * - 60-second timeout per suggestion
 * - Auto-cleanup of expired suggestions
 * - Thread-safe operations
 * 
 * PRIORITY 2 - Manual Mode with Confirmation (Task 3)
 */
public final class ManualModeController {

    /**
     * Maximum number of pending suggestions in the queue.
     */
    public static final int MAX_PENDING_SUGGESTIONS = 3;

    private final Queue<PendingSuggestion> pendingQueue = new LinkedList<>();
    private final NozhLogger logger;
    private volatile boolean enabled = false;

    public ManualModeController(NozhLogger logger) {
        this.logger = logger;
    }

    /**
     * Enable or disable manual mode.
     * 
     * @param enabled true to enable manual mode
     */
    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (!enabled) {
                // Clear queue when disabling manual mode
                pendingQueue.clear();
                logger.info("Manual mode disabled, cleared {} pending suggestions", pendingQueue.size());
            } else {
                logger.info("Manual mode enabled");
            }
        }
    }

    /**
     * Check if manual mode is enabled.
     * 
     * @return true if manual mode is active
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Suggest an action for manual approval.
     * 
     * @param candidate The action candidate to suggest
     */
    public synchronized void suggestAction(ActionCandidate candidate) {
        if (!enabled) {
            logger.debug("Manual mode disabled, ignoring suggestion for {}", candidate.capabilityId());
            return;
        }

        cleanupExpiredSuggestions();

        // Check if queue is full
        if (pendingQueue.size() >= MAX_PENDING_SUGGESTIONS) {
            logger.warn("Suggestion queue full, dropping oldest suggestion");
            pendingQueue.poll(); // Remove oldest
        }

        PendingSuggestion suggestion = PendingSuggestion.create(
            candidate.capabilityId(),
            candidate.targetValue(),
            candidate.reason()
        );

        pendingQueue.offer(suggestion);
        logger.info("Queued suggestion: {} → {} ({})",
            candidate.capabilityId(),
            candidate.targetValue(),
            candidate.reason()
        );
    }

    /**
     * Apply the current (oldest) suggestion.
     * Called when user presses the confirmation keybind (K).
     * 
     * @return The applied suggestion, or empty if no suggestions available
     */
    public synchronized Optional<PendingSuggestion> applyCurrentSuggestion() {
        cleanupExpiredSuggestions();

        PendingSuggestion suggestion = pendingQueue.poll();
        if (suggestion == null) {
            logger.debug("No pending suggestions to apply");
            return Optional.empty();
        }

        logger.info("Applied suggestion: {} → {}",
            suggestion.capability(),
            suggestion.suggestedValue()
        );

        return Optional.of(suggestion);
    }

    /**
     * Dismiss the current (oldest) suggestion without applying it.
     * 
     * @return The dismissed suggestion, or empty if no suggestions available
     */
    public synchronized Optional<PendingSuggestion> dismissCurrentSuggestion() {
        cleanupExpiredSuggestions();

        PendingSuggestion suggestion = pendingQueue.poll();
        if (suggestion == null) {
            logger.debug("No pending suggestions to dismiss");
            return Optional.empty();
        }

        logger.info("Dismissed suggestion: {} → {}",
            suggestion.capability(),
            suggestion.suggestedValue()
        );

        return Optional.of(suggestion);
    }

    /**
     * Get the current (oldest) pending suggestion without removing it.
     * 
     * @return The current suggestion, or empty if no suggestions available
     */
    public synchronized Optional<PendingSuggestion> getCurrentSuggestion() {
        cleanupExpiredSuggestions();
        return Optional.ofNullable(pendingQueue.peek());
    }

    /**
     * Get the number of pending suggestions.
     * 
     * @return Number of suggestions in queue
     */
    public synchronized int getPendingCount() {
        cleanupExpiredSuggestions();
        return pendingQueue.size();
    }

    /**
     * Clear all pending suggestions.
     */
    public synchronized void clearAll() {
        int count = pendingQueue.size();
        pendingQueue.clear();
        if (count > 0) {
            logger.info("Cleared {} pending suggestions", count);
        }
    }

    /**
     * Remove expired suggestions from the queue.
     */
    private void cleanupExpiredSuggestions() {
        int removed = 0;
        while (!pendingQueue.isEmpty() && pendingQueue.peek().isExpired()) {
            PendingSuggestion expired = pendingQueue.poll();
            removed++;
            logger.debug("Removed expired suggestion: {} → {}",
                expired.capability(),
                expired.suggestedValue()
            );
        }
        
        if (removed > 0) {
            logger.info("Cleaned up {} expired suggestions", removed);
        }
    }

    /**
     * Get all pending suggestions (for HUD display).
     * 
     * @return Array of pending suggestions
     */
    public synchronized PendingSuggestion[] getAllSuggestions() {
        cleanupExpiredSuggestions();
        return pendingQueue.toArray(new PendingSuggestion[0]);
    }
}
