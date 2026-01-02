package dev.nozh.core.monitoring;

/**
 * System resource monitor for intelligent optimization decisions.
 * 
 * Monitors:
 * - JVM memory pressure
 * - CPU usage (future)
 * - System overhead
 * 
 * Intelligence: Prioritize memory-saving actions when under pressure.
 */
public final class SystemMonitor {

    private static final double MEMORY_PRESSURE_THRESHOLD = 0.85; // 85% usage
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.95; // 95% usage

    /**
     * Check if system is under memory pressure.
     * 
     * @return true if memory usage > 85%
     */
    public boolean isMemoryPressure() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        double usage = (double) used / max;
        return usage > MEMORY_PRESSURE_THRESHOLD;
    }

    /**
     * Check if memory situation is critical (OOM risk).
     * 
     * @return true if memory usage > 95%
     */
    public boolean isMemoryCritical() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        double usage = (double) used / max;
        return usage > MEMORY_CRITICAL_THRESHOLD;
    }

    /**
     * Get current memory usage percentage.
     */
    public double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        return (double) used / max;
    }

    /**
     * Get memory usage in human-readable format.
     */
    public String getMemoryUsageString() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        long usedMB = used / (1024 * 1024);
        long maxMB = max / (1024 * 1024);

        return String.format("%dMB / %dMB (%.1f%%)",
                usedMB, maxMB, getMemoryUsage() * 100);
    }
}
