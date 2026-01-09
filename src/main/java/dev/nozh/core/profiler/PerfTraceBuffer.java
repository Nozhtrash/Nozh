package dev.nozh.core.profiler;

import java.util.ArrayList;
import java.util.List;

public class PerfTraceBuffer {
    private static final int MAX_EVENTS = 512;
    private final PerfTraceEvent[] events = new PerfTraceEvent[MAX_EVENTS];
    private int startIndex = 0;
    private int size = 0;

    public synchronized void record(PerfTraceEvent event) {
        if (event == null) {
            return;
        }
        if (size < MAX_EVENTS) {
            int index = (startIndex + size) % MAX_EVENTS;
            events[index] = event;
            size++;
        } else {
            events[startIndex] = event;
            startIndex = (startIndex + 1) % MAX_EVENTS;
        }
    }

    public synchronized PerfTraceSnapshot snapshot(long windowMillis) {
        if (size == 0 || windowMillis <= 0) {
            return PerfTraceSnapshot.empty();
        }
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;
        pruneBefore(windowStart);
        if (size == 0) {
            return PerfTraceSnapshot.empty();
        }
        List<PerfTraceEvent> recent = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = (startIndex + i) % MAX_EVENTS;
            PerfTraceEvent event = events[index];
            if (event != null && event.timestampMillis() >= windowStart) {
                recent.add(event);
            }
        }
        return new PerfTraceSnapshot(windowStart, now, recent);
    }

    public synchronized void reset() {
        for (int i = 0; i < size; i++) {
            int index = (startIndex + i) % MAX_EVENTS;
            events[index] = null;
        }
        startIndex = 0;
        size = 0;
    }

    private void pruneBefore(long cutoffMillis) {
        int removed = 0;
        while (removed < size) {
            int index = (startIndex + removed) % MAX_EVENTS;
            PerfTraceEvent oldest = events[index];
            if (oldest == null || oldest.timestampMillis() >= cutoffMillis) {
                break;
            }
            events[index] = null;
            removed++;
        }
        if (removed > 0) {
            startIndex = (startIndex + removed) % MAX_EVENTS;
            size -= removed;
        }
    }
}
