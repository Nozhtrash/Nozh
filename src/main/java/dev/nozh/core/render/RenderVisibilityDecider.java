package dev.nozh.core.render;

import dev.nozh.NozhConstants;
import dev.nozh.core.settings.NozhRenderSettings;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

public final class RenderVisibilityDecider {

    private static final Set<String> warnedLabels = ConcurrentHashMap.newKeySet();
    private static final AtomicLong lastGameRendererFrameNanos = new AtomicLong();

    private RenderVisibilityDecider() {
    }

    public static boolean isArmorStandVisible() {
        return resolveVisibility(NozhRenderSettings::isArmorStandsVisible, "armor stand");
    }

    public static boolean isItemFrameVisible() {
        return resolveVisibility(NozhRenderSettings::isItemFramesVisible, "item frame");
    }

    public static boolean isAllAnimationsVisible() {
        return resolveVisibility(NozhRenderSettings::isAllAnimationsVisible, "animations");
    }

    public static void recordGameRendererFrame() {
        try {
            lastGameRendererFrameNanos.set(System.nanoTime());
        } catch (RuntimeException ex) {
            warnOnce("game renderer frame", ex);
        }
    }

    public static boolean resolveVisibility(BooleanSupplier supplier, String label) {
        try {
            return supplier.getAsBoolean();
        } catch (RuntimeException ex) {
            warnOnce(label, ex);
            return true;
        }
    }

    private static void warnOnce(String label, RuntimeException ex) {
        if (warnedLabels.add(label)) {
            NozhConstants.LOGGER.warn("Render visibility check failed for {}. Falling back to visible.", label, ex);
        }
    }
}
