package dev.nozh.core.settings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom render settings for NOZH features that don't exist in vanilla options.
 * 
 * Thread-safe singleton-ish pattern (static fields for speed).
 */
public class NozhRenderSettings {

    // Default: TRUE (Visible)
    private static final AtomicBoolean armorStandsVisible = new AtomicBoolean(true);
    private static final AtomicBoolean itemFramesVisible = new AtomicBoolean(true);
    private static final AtomicBoolean blockEntitiesVisible = new AtomicBoolean(true);
    private static final AtomicBoolean allAnimationsVisible = new AtomicBoolean(true);

    public static boolean isArmorStandsVisible() {
        return armorStandsVisible.get();
    }

    public static void setArmorStandsVisible(boolean visible) {
        armorStandsVisible.set(visible);
    }

    public static boolean isItemFramesVisible() {
        return itemFramesVisible.get();
    }

    public static void setItemFramesVisible(boolean visible) {
        itemFramesVisible.set(visible);
    }

    public static boolean isBlockEntitiesVisible() {
        return blockEntitiesVisible.get();
    }

    public static void setBlockEntitiesVisible(boolean visible) {
        blockEntitiesVisible.set(visible);
    }

    public static boolean isAllAnimationsVisible() {
        return allAnimationsVisible.get();
    }

    public static void setAllAnimationsVisible(boolean visible) {
        allAnimationsVisible.set(visible);
    }
}
