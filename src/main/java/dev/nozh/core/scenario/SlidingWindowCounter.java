package dev.nozh.core.scenario;

import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowCounter {

    private static final class Entry {
        final long tMs;
        final int delta;

        Entry(long tMs, int delta) {
            this.tMs = tMs;
            this.delta = delta;
        }
    }

    private final long windowMs;
    private final Deque<Entry> q = new ArrayDeque<>();
    private int sum;

    SlidingWindowCounter(long windowMs) {
        this.windowMs = Math.max(1L, windowMs);
    }

    void add(long nowMs, int delta) {
        if (delta == 0) return;
        q.addLast(new Entry(nowMs, delta));
        sum += delta;
        trim(nowMs);
    }

    double perMinute(long nowMs) {
        trim(nowMs);
        double minutes = windowMs / 60000.0;
        if (minutes <= 0.0) return 0.0;
        return sum / minutes;
    }

    private void trim(long nowMs) {
        long minT = nowMs - windowMs;
        while (!q.isEmpty() && q.peekFirst().tMs < minT) {
            Entry e = q.removeFirst();
            sum -= e.delta;
        }
    }
}
