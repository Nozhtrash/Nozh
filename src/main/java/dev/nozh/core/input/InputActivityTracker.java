package dev.nozh.core.input;

/**
 * Tracks recent input activity for scenario detection.
 */
public final class InputActivityTracker {
    private static volatile long lastKeyboardInputMs = 0L;
    private static volatile long lastMouseInputMs = 0L;

    private InputActivityTracker() {
        throw new AssertionError("No instances");
    }

    public static void recordKeyboardInput() {
        lastKeyboardInputMs = System.currentTimeMillis();
    }

    public static void recordMouseInput() {
        lastMouseInputMs = System.currentTimeMillis();
    }

    public static boolean hasRecentInput(long thresholdMs) {
        long now = System.currentTimeMillis();
        return now - Math.max(lastKeyboardInputMs, lastMouseInputMs) <= thresholdMs;
    }

    public static long getLastInputAgeMs() {
        long now = System.currentTimeMillis();
        long lastInput = Math.max(lastKeyboardInputMs, lastMouseInputMs);
        if (lastInput == 0L) {
            return Long.MAX_VALUE;
        }
        return now - lastInput;
    }
}
