package dev.nozh.core.profiler;

public interface CriticalEventSink {
    void recordCriticalEvent(String severity, String category, String message);
}
