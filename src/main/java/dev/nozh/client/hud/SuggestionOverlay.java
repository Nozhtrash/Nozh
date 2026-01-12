package dev.nozh.client.hud;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Shows smart suggestions to improve performance.
 * Non-intrusive, actionable advice.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class SuggestionOverlay {

    /**
     * Suggestion priority levels.
     */
    public enum SuggestionPriority {
        LOW("Nice to have", 0x888888),
        MEDIUM("Recommended", 0x00AAFF),
        HIGH("Strongly recommended", 0xFFAA00),
        CRITICAL("You should really do this", 0xFF0000);

        public final String description;
        public final int color;

        SuggestionPriority(String description, int color) {
            this.description = description;
            this.color = color;
        }
    }

    /**
     * A performance suggestion.
     */
    public record Suggestion(
            String id,
            String title,
            String description,
            String action,
            SuggestionPriority priority,
            Runnable onAccept) {
        /**
         * Creates a simple suggestion without action.
         */
        public static Suggestion info(String id, String title, String desc, SuggestionPriority priority) {
            return new Suggestion(id, title, desc, null, priority, null);
        }
    }

    private final Map<String, Suggestion> activeSuggestions;
    private final Set<String> dismissedIds;
    private boolean enabled;
    private int maxVisibleSuggestions;

    /**
     * Constructs a new SuggestionOverlay.
     */
    public SuggestionOverlay() {
        this.activeSuggestions = new LinkedHashMap<>();
        this.dismissedIds = new HashSet<>();
        this.enabled = true;
        this.maxVisibleSuggestions = 3;

        // Add default suggestions based on common issues
        initializeDefaultSuggestions();
    }

    /**
     * Initializes common suggestions.
     */
    private void initializeDefaultSuggestions() {
        // These would be dynamically added based on detected conditions
    }

    /**
     * Adds a suggestion.
     * 
     * @param suggestion suggestion to add
     */
    public void addSuggestion(Suggestion suggestion) {
        if (dismissedIds.contains(suggestion.id())) {
            return; // Already dismissed
        }

        activeSuggestions.put(suggestion.id(), suggestion);
        NozhConstants.LOGGER.debug("Added suggestion: {}", suggestion.title());
    }

    /**
     * Suggests installing a mod.
     * 
     * @param modName      mod name
     * @param expectedGain expected FPS gain
     */
    public void suggestMod(String modName, int expectedGain) {
        String id = "mod_" + modName.toLowerCase().replace(" ", "_");

        addSuggestion(new Suggestion(
                id,
                "Install " + modName,
                String.format("Expected +%d%% FPS improvement", expectedGain),
                "Click to open download page",
                expectedGain > 20 ? SuggestionPriority.HIGH : SuggestionPriority.MEDIUM,
                null));
    }

    /**
     * Suggests reducing a setting.
     * 
     * @param setting          setting name
     * @param currentValue     current value
     * @param recommendedValue recommended value
     */
    public void suggestReduceSetting(String setting, int currentValue, int recommendedValue) {
        String id = "reduce_" + setting.toLowerCase().replace(" ", "_");

        addSuggestion(new Suggestion(
                id,
                "Reduce " + setting,
                String.format("From %d to %d", currentValue, recommendedValue),
                "Click to apply",
                SuggestionPriority.MEDIUM,
                null));
    }

    /**
     * Gets active suggestions sorted by priority.
     * 
     * @return list of active suggestions
     */
    public List<Suggestion> getActiveSuggestions() {
        return activeSuggestions.values().stream()
                .sorted((a, b) -> Integer.compare(b.priority().ordinal(), a.priority().ordinal()))
                .limit(maxVisibleSuggestions)
                .toList();
    }

    /**
     * Dismisses a suggestion.
     * 
     * @param id suggestion ID
     */
    public void dismissSuggestion(String id) {
        activeSuggestions.remove(id);
        dismissedIds.add(id);
        NozhConstants.LOGGER.debug("Dismissed suggestion: {}", id);
    }

    /**
     * Accepts a suggestion.
     * 
     * @param id suggestion ID
     */
    public void acceptSuggestion(String id) {
        Suggestion suggestion = activeSuggestions.get(id);
        if (suggestion != null && suggestion.onAccept() != null) {
            try {
                suggestion.onAccept().run();
                NozhConstants.LOGGER.info("Accepted suggestion: {}", suggestion.title());
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to apply suggestion: {}", e.getMessage());
            }
        }
        activeSuggestions.remove(id);
    }

    /**
     * Clears all suggestions.
     */
    public void clearAll() {
        activeSuggestions.clear();
    }

    /**
     * Enables or disables suggestions.
     * 
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if suggestions are enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets max visible suggestions.
     * 
     * @param max maximum number
     */
    public void setMaxVisible(int max) {
        this.maxVisibleSuggestions = Math.max(1, Math.min(10, max));
    }

    /**
     * Gets count of active suggestions.
     * 
     * @return count
     */
    public int getSuggestionCount() {
        return activeSuggestions.size();
    }
}
