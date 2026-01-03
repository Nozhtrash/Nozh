package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;

/**
 * Controls adaptive window sizing based on volatility in performance metrics.
 *
 * Goal: Increase window size when metrics fluctuate heavily, decrease when stable.
 */
public final class PerfWindowController {

    private static final long CHANGE_WINDOW_MILLIS = 10_000L;
    private static final long STABLE_WINDOW_MILLIS = 20_000L;
    private static final int MAX_CHANGES_BEFORE_GROW = 4;
    private static final double CHANGE_THRESHOLD_MS = 1.0;

    private final int minWindowSeconds;
    private final int maxWindowSeconds;

    private PerfSnapshot lastSnapshot;
    private long changeWindowStart = 0L;
    private int changeCount = 0;
    private long lastChangeMillis = 0L;

    public PerfWindowController(int minWindowSeconds, int maxWindowSeconds) {
        this.minWindowSeconds = minWindowSeconds;
        this.maxWindowSeconds = maxWindowSeconds;
    }

    /**
     * Evaluate recent snapshots and return new window seconds if adjustment needed.
     */
    public int evaluate(PerfSnapshot snapshot, int currentWindowSeconds, long nowMillis) {
        if (snapshot == null || !snapshot.sufficientData()) {
            return currentWindowSeconds;
        }

        if (lastSnapshot == null) {
            lastSnapshot = snapshot;
            changeWindowStart = nowMillis;
            lastChangeMillis = nowMillis;
            return currentWindowSeconds;
        }

        if (hasMeaningfulChange(snapshot, lastSnapshot)) {
            if (nowMillis - changeWindowStart > CHANGE_WINDOW_MILLIS) {
                changeWindowStart = nowMillis;
                changeCount = 0;
            }
            changeCount++;
            lastChangeMillis = nowMillis;
        }

        lastSnapshot = snapshot;

        if (changeCount >= MAX_CHANGES_BEFORE_GROW && currentWindowSeconds < maxWindowSeconds) {
            changeCount = 0;
            changeWindowStart = nowMillis;
            return currentWindowSeconds + 1;
        }

        if (nowMillis - lastChangeMillis > STABLE_WINDOW_MILLIS && currentWindowSeconds > minWindowSeconds) {
            return currentWindowSeconds - 1;
        }

        return currentWindowSeconds;
    }

    private boolean hasMeaningfulChange(PerfSnapshot current, PerfSnapshot previous) {
        return deltaMs(current.avgFrametimeMs(), previous.avgFrametimeMs()) > CHANGE_THRESHOLD_MS
                || deltaMs(current.p95FrametimeMs(), previous.p95FrametimeMs()) > CHANGE_THRESHOLD_MS
                || deltaMs(current.p99FrametimeMs(), previous.p99FrametimeMs()) > CHANGE_THRESHOLD_MS;
    }

    private double deltaMs(double current, double previous) {
        if (!Double.isFinite(current) || !Double.isFinite(previous)) {
            return 0;
        }
        return Math.abs(current - previous);
    }
}
