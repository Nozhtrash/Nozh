package dev.nozh.core.manual;

import dev.nozh.NozhConstants;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Controller for manual mode optimization suggestions.
 * 
 * Thread-safe queue with max 3 suggestions.
 * Auto-cleanup of expired suggestions.
 * 
 * PRIORITY 2: Manual Mode Implementation
 */
public final class ManualModeController {

    private static final int MAX_SUGGESTIONS = 3;
    
    private final List<PendingSuggestion> suggestions = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    
    /**
     * Suggest an action to the user.
     * Thread-safe with automatic cleanup of expired suggestions.
     * 
     * @return true if suggestion was added, false if queue is full
     */
    public boolean suggestAction(CapabilityId capability, CapabilityValue value, String reason) {
        lock.lock();
        try {
            // Clean expired suggestions first
            cleanupExpired();
            
            // Check if queue is full
            if (suggestions.size() >= MAX_SUGGESTIONS) {
                safeLog("Manual mode queue full, cannot add suggestion for {}", capability);
                return false;
            }
            
            // Add new suggestion
            PendingSuggestion suggestion = PendingSuggestion.create(capability, value, reason);
            suggestions.add(suggestion);
            safeLog("Manual mode suggestion added: {}", suggestion.toDisplayString());
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Apply the current (oldest) suggestion.
     * 
     * @return the suggestion that was applied, or null if none available
     */
    public PendingSuggestion applyCurrentSuggestion() {
        lock.lock();
        try {
            cleanupExpired();
            
            if (suggestions.isEmpty()) {
                return null;
            }
            
            PendingSuggestion suggestion = suggestions.remove(0);
            safeLog("Manual mode suggestion applied: {}", suggestion.toDisplayString());
            return suggestion;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Dismiss the current (oldest) suggestion.
     * 
     * @return the suggestion that was dismissed, or null if none available
     */
    public PendingSuggestion dismissCurrentSuggestion() {
        lock.lock();
        try {
            cleanupExpired();
            
            if (suggestions.isEmpty()) {
                return null;
            }
            
            PendingSuggestion suggestion = suggestions.remove(0);
            safeLog("Manual mode suggestion dismissed: {}", suggestion.toDisplayString());
            return suggestion;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get the current (oldest) suggestion without removing it.
     * 
     * @return the current suggestion, or null if none available
     */
    public PendingSuggestion getCurrentSuggestion() {
        lock.lock();
        try {
            cleanupExpired();
            return suggestions.isEmpty() ? null : suggestions.get(0);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get all pending suggestions (read-only copy).
     */
    public List<PendingSuggestion> getAllSuggestions() {
        lock.lock();
        try {
            cleanupExpired();
            return new ArrayList<>(suggestions);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Clear all suggestions.
     */
    public void clearAll() {
        lock.lock();
        try {
            suggestions.clear();
            safeLog("Manual mode queue cleared");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get number of pending suggestions.
     */
    public int getCount() {
        lock.lock();
        try {
            cleanupExpired();
            return suggestions.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Auto-cleanup expired suggestions.
     * Must be called with lock held.
     */
    private void cleanupExpired() {
        Iterator<PendingSuggestion> it = suggestions.iterator();
        while (it.hasNext()) {
            PendingSuggestion suggestion = it.next();
            if (suggestion.isExpired()) {
                it.remove();
                safeLog("Manual mode suggestion expired: {}", suggestion.capability());
            }
        }
    }
    
    private void safeLog(String message, Object... args) {
        try {
            if (NozhConstants.LOGGER != null) {
                NozhConstants.LOGGER.info(message, args);
            }
        } catch (Throwable ignored) {
            // Logger may not be initialized
        }
    }
}
