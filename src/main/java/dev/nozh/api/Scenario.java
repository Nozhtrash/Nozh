package dev.nozh.api;

public enum Scenario {
    IDLE,
    MENU,
    WORLD_LOADING,
    EXPLORATION,
    COMBAT,
    BUILDING,
    MINING,
    AFK,
    HIGH_ENTITY_DENSITY,
    UNKNOWN;
    
    public boolean isHighLoad() {
        return this == COMBAT || this == HIGH_ENTITY_DENSITY || this == WORLD_LOADING;
    }
    
    public boolean isLowLoad() {
        return this == IDLE || this == AFK || this == MENU;
    }
}
