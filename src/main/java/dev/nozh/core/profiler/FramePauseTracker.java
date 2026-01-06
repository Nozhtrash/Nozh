package dev.nozh.core.profiler;

public class FramePauseTracker {

    private static final long WINDOW_MS = 1000L;
    private static final long PAUSE_THRESHOLD_NANOS = 100_000_000L; // 100ms

    private long windowStartMillis = System.currentTimeMillis();
    private int currentPauseCount = 0;
    private long currentPauseTotalNanos = 0L;
    private long currentPauseMaxNanos = 0L;
    private long currentLastPauseNanos = 0L;

    private FramePauseSnapshot lastSnapshot = FramePauseSnapshot.empty();

    public synchronized void recordFrameDuration(long durationNanos) {
        rollWindowIfNeeded(System.currentTimeMillis());
        if (durationNanos >= PAUSE_THRESHOLD_NANOS) {
            currentPauseCount++;
            currentPauseTotalNanos += durationNanos;
            currentPauseMaxNanos = Math.max(currentPauseMaxNanos, durationNanos);
            currentLastPauseNanos = durationNanos;
        }
    }

    public synchronized FramePauseSnapshot snapshot() {
        rollWindowIfNeeded(System.currentTimeMillis());
        if (lastSnapshot.pauseCount() == 0 && currentPauseCount > 0) {
            return currentSnapshot();
        }
        return lastSnapshot;
    }

    private void rollWindowIfNeeded(long nowMillis) {
        if (nowMillis - windowStartMillis < WINDOW_MS) {
            return;
        }
        lastSnapshot = currentSnapshot();
        windowStartMillis = nowMillis;
        currentPauseCount = 0;
        currentPauseTotalNanos = 0L;
        currentPauseMaxNanos = 0L;
        currentLastPauseNanos = 0L;
    }

    private FramePauseSnapshot currentSnapshot() {
        return new FramePauseSnapshot(
                currentPauseCount,
                nanosToMs(currentPauseTotalNanos),
                nanosToMs(currentPauseMaxNanos),
                nanosToMs(currentLastPauseNanos));
    }

    private double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }
}
