package dev.nozh.core.safety;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a blacklist of actions that have repeatedly failed.
 * Prevents the governor from attempting actions that are known to cause issues.
 * 
 * <p>Actions can be temporarily blacklisted after multiple failures,
 * or permanently blacklisted for safety reasons.
 * 
 * <p>Thread-safe implementation using ConcurrentHashMap.
 * 
 * @author Nozh Team
 * @since 0.2.0
 */
public class ActionBlacklist {
    private final Set<String> permanentBlacklist;
    private final Set<String> temporaryBlacklist;
    
    public ActionBlacklist() {
        this.permanentBlacklist = ConcurrentHashMap.newKeySet();
        this.temporaryBlacklist = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Initializes the blacklist with default unsafe actions.
     * Called during governor initialization.
     */
    public void initializeDefaults() {
        // Add actions that are known to be risky or unstable
        // Currently empty - will be populated based on testing
    }
    
    /**
     * Checks if an action is blacklisted (either permanent or temporary).
     * 
     * @param actionId the action ID to check
     * @return true if the action is blacklisted
     */
    public boolean isBlacklisted(String actionId) {
        return permanentBlacklist.contains(actionId) || 
               temporaryBlacklist.contains(actionId);
    }
    
    /**
     * Permanently blacklists an action.
     * Use this for actions that cause crashes or severe issues.
     * 
     * @param actionId the action ID to blacklist
     */
    public void addToPermanentBlacklist(String actionId) {
        permanentBlacklist.add(actionId);
        temporaryBlacklist.remove(actionId); // Remove from temporary if present
    }
    
    /**
     * Temporarily blacklists an action.
     * Use this for actions that have failed multiple times.
     * 
     * @param actionId the action ID to temporarily blacklist
     */
    public void addToTemporaryBlacklist(String actionId) {
        if (!permanentBlacklist.contains(actionId)) {
            temporaryBlacklist.add(actionId);
        }
    }
    
    /**
     * Removes an action from the temporary blacklist.
     * Used when retrying previously failed actions.
     * 
     * @param actionId the action ID to remove
     */
    public void removeFromTemporaryBlacklist(String actionId) {
        temporaryBlacklist.remove(actionId);
    }
    
    /**
     * Clears the temporary blacklist.
     * Permanent blacklist remains unchanged.
     */
    public void clearTemporary() {
        temporaryBlacklist.clear();
    }
    
    /**
     * Gets the number of blacklisted actions (permanent + temporary).
     * 
     * @return total count of blacklisted actions
     */
    public int size() {
        return permanentBlacklist.size() + temporaryBlacklist.size();
    }
    
    /**
     * Gets the number of permanently blacklisted actions.
     * 
     * @return count of permanent blacklist entries
     */
    public int permanentSize() {
        return permanentBlacklist.size();
    }
    
    /**
     * Gets the number of temporarily blacklisted actions.
     * 
     * @return count of temporary blacklist entries
     */
    public int temporarySize() {
        return temporaryBlacklist.size();
    }
}
