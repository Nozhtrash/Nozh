package dev.nozh.core.profiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PerfTraceBuffer {
    private static final int MAX_EVENTS = 512;
    private final Deque<PerfTraceEvent> events = new ArrayDeque<>();

    public synchronized void record(PerfTraceEvent event) {
        if (event == null) {
            return;
        }
        events.addLast(event);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
    }

    public synchronized PerfTraceSnapshot snapshot(long windowMillis) {
        if (events.isEmpty() || windowMillis <= 0) {
            return PerfTraceSnapshot.empty();
        }
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;
        pruneBefore(windowStart);
        List<PerfTraceEvent> recent = new ArrayList<>();
        for (PerfTraceEvent event : events) {
            if (event.timestampMillis() >= windowStart) {
                recent.add(event);
            }
        }
        return new PerfTraceSnapshot(windowStart, now, recent);
    }

    public synchronized void reset() {
        events.clear();
    }

    private void pruneBefore(long cutoffMillis) {
        while (!events.isEmpty()) {
            PerfTraceEvent oldest = events.peekFirst();
            if (oldest == null || oldest.timestampMillis() >= cutoffMillis) {
                break;
            }
            events.removeFirst();
        }
    }
}
