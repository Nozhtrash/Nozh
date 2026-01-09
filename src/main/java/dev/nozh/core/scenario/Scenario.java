package dev.nozh.core.scenario;

/**
 * Game scenario classification.
 * 
 * ROADMAP: Phase 2, Sprint 3 - Scenario Detection
 */
public enum Scenario {
    STANDARD("Standard gameplay"),
    COMBAT("Active combat"),
    BUILDING("Building/construction"),
    EXPLORING("Exploration/travel"),
    ORGANIZING("Inventory management"),
    AFK("Away from keyboard");
    
    private final String description;
    
    Scenario(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}