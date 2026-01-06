package dev.nozh.core.context;

/**
 * Game context scenarios.
 * Used to adapt optimization strategy dynamically.
 */
public enum Scenario {
    /** Standard gameplay (exploring, survival) */
    STANDARD,

    /** High motion, intense combat, many entities */
    COMBAT,

    /** Dense blocks, low movement, creative/building */
    BUILDING,

    /** Underground, low render distance needed */
    MINING,

    /** Active exploration (fast movement, chunk loading) */
    EXPLORING,

    /** No input for extended time */
    AFK,

    /** Menu/Inventory open */
    MENU,

    /** Singleplayer world load or server join */
    LOADING;

    public String translationKey() {
        return "nozh.scenario." + name().toLowerCase();
    }
}
