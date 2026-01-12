package dev.nozh.core.potato;

import dev.nozh.NozhConstants;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Memory-focused optimizations for low-RAM systems.
 * Prevents OutOfMemory crashes and reduces GC pauses.
 * 
 * <p>
 * Features:
 * <ul>
 * <li>Memory pressure detection</li>
 * <li>JVM arguments suggestions</li>
 * <li>Emergency memory cleanup</li>
 * <li>Low memory mode</li>
 * </ul>
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class MemoryOptimizer {

    private final MemoryMXBean memoryBean;
    private boolean lowMemoryMode;
    private long lastCleanupTime;

    // Thresholds
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.85; // 85% usage
    private static final double CRITICAL_MEMORY_THRESHOLD = 0.95; // 95% usage
    private static final long MIN_CLEANUP_INTERVAL_MS = 30000; // 30 seconds

    /**
     * Constructs a new MemoryOptimizer.
     */
    public MemoryOptimizer() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.lowMemoryMode = false;
        this.lastCleanupTime = 0;
    }

    /**
     * Gets current memory usage percentage.
     * 
     * @return memory usage from 0.0 to 1.0
     */
    public double getMemoryUsagePercent() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();

        if (max <= 0)
            return 0.0;
        return (double) used / max;
    }

    /**
     * Checks if system is under memory pressure.
     * 
     * <p>
     * Memory pressure indicates high RAM usage that may soon
     * lead to OutOfMemory errors or heavy GC pauses.
     * 
     * @return true if memory usage exceeds threshold
     */
    public boolean isMemoryPressure() {
        return getMemoryUsagePercent() >= MEMORY_PRESSURE_THRESHOLD;
    }

    /**
     * Checks if memory is critically low.
     * 
     * @return true if memory usage is critical
     */
    public boolean isCriticalMemory() {
        return getMemoryUsagePercent() >= CRITICAL_MEMORY_THRESHOLD;
    }

    /**
     * Suggests optimal JVM arguments based on available RAM.
     * 
     * <p>
     * Recommendations:
     * <ul>
     * <li><b>Less than 4GB</b>: Minimal heap, aggressive GC</li>
     * <li><b>4-8GB</b>: Moderate heap, balanced GC</li>
     * <li><b>8GB+</b>: Generous heap, low-pause GC</li>
     * </ul>
     * 
     * @param availableRamMb total available RAM in megabytes
     * @return suggested JVM arguments
     */
    public String suggestJvmArgs(long availableRamMb) {
        StringBuilder args = new StringBuilder();

        if (availableRamMb < 4096) {
            // Low RAM: Aggressive GC, minimal heap
            args.append("-Xms512M -Xmx2G ");
            args.append("-XX:+UseG1GC ");
            args.append("-XX:MaxGCPauseMillis=50 ");
            args.append("-XX:G1HeapRegionSize=16M ");
            args.append("-XX:G1NewSizePercent=20 ");
            args.append("-XX:G1ReservePercent=15 ");
            args.append("-XX:InitiatingHeapOccupancyPercent=60");
        } else if (availableRamMb < 8192) {
            // Medium RAM: Balanced
            args.append("-Xms1G -Xmx4G ");
            args.append("-XX:+UseG1GC ");
            args.append("-XX:MaxGCPauseMillis=50 ");
            args.append("-XX:G1HeapRegionSize=32M ");
            args.append("-XX:G1NewSizePercent=30 ");
            args.append("-XX:G1ReservePercent=20 ");
            args.append("-XX:InitiatingHeapOccupancyPercent=45");
        } else {
            // High RAM: Low-pause GC, generous heap
            args.append("-Xms2G -Xmx6G ");
            args.append("-XX:+UseG1GC ");
            args.append("-XX:MaxGCPauseMillis=50 ");
            args.append("-XX:G1HeapRegionSize=32M ");
            args.append("-XX:G1NewSizePercent=40 ");
            args.append("-XX:G1ReservePercent=20 ");
            args.append("-XX:InitiatingHeapOccupancyPercent=35");
        }

        return args.toString();
    }

    /**
     * Performs emergency memory cleanup.
     * 
     * <p>
     * Actions taken:
     * <ol>
     * <li>Suggests JVM garbage collection</li>
     * <li>Logs memory state</li>
     * <li>Activates low memory mode if needed</li>
     * </ol>
     * 
     * <p>
     * <b>Note:</b> This method has a cooldown to prevent spam.
     * It will only execute once every 30 seconds.
     */
    public void emergencyCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < MIN_CLEANUP_INTERVAL_MS) {
            return; // Cooldown active
        }

        NozhConstants.LOGGER.warn("Emergency memory cleanup triggered");

        // Log current memory state
        MemoryUsage heapBefore = memoryBean.getHeapMemoryUsage();
        NozhConstants.LOGGER.info("Memory before cleanup: used={}MB, max={}MB ({}%)",
                heapBefore.getUsed() / 1024 / 1024,
                heapBefore.getMax() / 1024 / 1024,
                (int) (getMemoryUsagePercent() * 100));

        // Suggest GC (JVM may ignore this hint)
        System.gc();

        // Wait briefly for GC
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Log after cleanup
        MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();
        long freed = heapBefore.getUsed() - heapAfter.getUsed();
        NozhConstants.LOGGER.info("Memory after cleanup: freed={}MB, current={}MB ({}%)",
                freed / 1024 / 1024,
                heapAfter.getUsed() / 1024 / 1024,
                (int) (getMemoryUsagePercent() * 100));

        // Activate low memory mode if still critical
        if (isCriticalMemory() && !lowMemoryMode) {
            enableLowMemoryMode();
        }

        lastCleanupTime = now;
    }

    /**
     * Enables low memory mode.
     * 
     * <p>
     * Low memory mode reduces memory footprint by:
     * <ul>
     * <li>Reducing cache sizes</li>
     * <li>Disabling non-essential features</li>
     * <li>More aggressive cleanup</li>
     * </ul>
     * 
     * <p>
     * This mode is automatically enabled during memory pressure
     * and can be manually enabled for very weak systems.
     */
    public void enableLowMemoryMode() {
        if (lowMemoryMode)
            return;

        lowMemoryMode = true;
        NozhConstants.LOGGER.warn("Low memory mode ENABLED");
        NozhConstants.LOGGER.info("Non-essential features will be limited");
    }

    /**
     * Disables low memory mode.
     */
    public void disableLowMemoryMode() {
        if (!lowMemoryMode)
            return;

        lowMemoryMode = false;
        NozhConstants.LOGGER.info("Low memory mode disabled");
    }

    /**
     * Checks if low memory mode is active.
     * 
     * @return true if active
     */
    public boolean isLowMemoryMode() {
        return lowMemoryMode;
    }

    /**
     * Gets memory statistics summary.
     * 
     * @return human-readable memory stats
     */
    public String getMemoryStats() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        return String.format(
                "Heap: %dMB / %dMB (%.1f%%) | Non-Heap: %dMB | Low Memory: %s",
                heap.getUsed() / 1024 / 1024,
                heap.getMax() / 1024 / 1024,
                getMemoryUsagePercent() * 100,
                nonHeap.getUsed() / 1024 / 1024,
                lowMemoryMode ? "ON" : "OFF");
    }

    /**
     * Monitors memory and performs automatic actions if needed.
     * 
     * <p>
     * Call this periodically (e.g., every second) to enable
     * automatic memory management.
     * 
     * <p>
     * Actions:
     * <ul>
     * <li>Enables low memory mode if pressure detected</li>
     * <li>Performs emergency cleanup if critical</li>
     * <li>Logs warnings</li>
     * </ul>
     */
    public void tick() {
        if (isCriticalMemory()) {
            NozhConstants.LOGGER.error("CRITICAL MEMORY: {}%",
                    (int) (getMemoryUsagePercent() * 100));
            emergencyCleanup();
        } else if (isMemoryPressure()) {
            if (!lowMemoryMode) {
                NozhConstants.LOGGER.warn("Memory pressure detected: {}%",
                        (int) (getMemoryUsagePercent() * 100));
                enableLowMemoryMode();
            }
        } else {
            // Memory is fine, can disable low memory mode
            if (lowMemoryMode && getMemoryUsagePercent() < 0.7) {
                disableLowMemoryMode();
            }
        }
    }
}
