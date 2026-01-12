package dev.nozh.core.potato;

import dev.nozh.NozhConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reduces Minecraft startup time and initial lag.
 * Especially important for modpacks with 200+ mods.
 * 
 * <p>
 * Features:
 * <ul>
 * <li>Startup phase measurement</li>
 * <li>Deferred non-essential initialization</li>
 * <li>System pre-warming</li>
 * <li>Startup report generation</li>
 * </ul>
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class StartupOptimizer {

    /**
     * Startup phase definitions.
     */
    public enum StartupPhase {
        PRE_INIT("Pre-initialization"),
        INIT("Initialization"),
        POST_INIT("Post-initialization"),
        WORLD_LOAD("World Loading"),
        FIRST_RENDER("First Render"),
        READY("Ready to Play");

        public final String displayName;

        StartupPhase(String displayName) {
            this.displayName = displayName;
        }
    }

    private final Map<StartupPhase, Long> phaseDurations;
    private final Map<String, Long> customPhases;
    private final AtomicLong totalStartupTime;

    private StartupPhase currentPhase;
    private long phaseStartTime;
    private long overallStartTime;
    private boolean startupComplete;

    /**
     * Constructs a new StartupOptimizer.
     */
    public StartupOptimizer() {
        this.phaseDurations = new HashMap<>();
        this.customPhases = new HashMap<>();
        this.totalStartupTime = new AtomicLong(0);
        this.startupComplete = false;
    }

    /**
     * Marks the beginning of startup tracking.
     */
    public void beginStartup() {
        overallStartTime = System.currentTimeMillis();
        NozhConstants.LOGGER.info("Startup optimization tracking started");
    }

    /**
     * Begins a new startup phase.
     *
     * @param phase the phase to begin
     */
    public void beginPhase(StartupPhase phase) {
        long now = System.currentTimeMillis();

        if (currentPhase != null) {
            long duration = now - phaseStartTime;
            phaseDurations.put(currentPhase, duration);
            NozhConstants.LOGGER.debug("Phase {} completed in {}ms", currentPhase.displayName, duration);
        }

        currentPhase = phase;
        phaseStartTime = now;
        NozhConstants.LOGGER.info("Entering phase: {}", phase.displayName);
    }

    /**
     * Begins a custom named phase for mod-specific tracking.
     *
     * @param phaseName custom phase name
     */
    public void beginCustomPhase(String phaseName) {
        customPhases.put(phaseName, System.currentTimeMillis());
    }

    /**
     * Ends a custom named phase.
     *
     * @param phaseName custom phase name
     */
    public void endCustomPhase(String phaseName) {
        Long startTime = customPhases.get(phaseName);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            customPhases.put(phaseName, duration);
            NozhConstants.LOGGER.debug("Custom phase '{}' completed in {}ms", phaseName, duration);
        }
    }

    /**
     * Marks startup as complete.
     */
    public void completeStartup() {
        if (startupComplete)
            return;

        long now = System.currentTimeMillis();

        if (currentPhase != null) {
            phaseDurations.put(currentPhase, now - phaseStartTime);
        }

        long total = now - overallStartTime;
        totalStartupTime.set(total);
        startupComplete = true;

        NozhConstants.LOGGER.info("Startup complete in {}ms", total);
        logStartupReport();
    }

    /**
     * Logs a detailed startup report.
     */
    private void logStartupReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n=== NOZH Startup Report ===\n");
        report.append(String.format("Total Startup Time: %dms\n", totalStartupTime.get()));
        report.append("\nPhase Breakdown:\n");

        phaseDurations.forEach(
                (phase, duration) -> report.append(String.format("  %s: %dms\n", phase.displayName, duration)));

        if (!customPhases.isEmpty()) {
            report.append("\nCustom Phases:\n");
            customPhases.forEach((name, duration) -> report.append(String.format("  %s: %dms\n", name, duration)));
        }

        NozhConstants.LOGGER.info(report.toString());
    }

    /**
     * Gets measured startup times for all phases.
     *
     * @return map of phase to duration in milliseconds
     */
    public Map<StartupPhase, Long> getPhaseTimings() {
        return Map.copyOf(phaseDurations);
    }

    /**
     * Gets total startup time.
     *
     * @return total time in milliseconds
     */
    public long getTotalStartupTime() {
        return totalStartupTime.get();
    }

    /**
     * Checks if startup is complete.
     *
     * @return true if startup tracking is complete
     */
    public boolean isStartupComplete() {
        return startupComplete;
    }

    /**
     * Defers non-essential initialization to after startup.
     * Returns immediately, actual work is done later.
     *
     * @param taskName name of the deferred task
     * @param task     the runnable to execute later
     */
    public void deferNonEssential(String taskName, Runnable task) {
        NozhConstants.LOGGER.debug("Deferring task: {}", taskName);

        Thread deferredThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds after startup
                if (startupComplete) {
                    NozhConstants.LOGGER.debug("Executing deferred task: {}", taskName);
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                NozhConstants.LOGGER.warn("Deferred task '{}' failed: {}", taskName, e.getMessage());
            }
        }, "NOZH-Deferred-" + taskName);

        deferredThread.setDaemon(true);
        deferredThread.start();
    }

    /**
     * Pre-warms critical systems to reduce first-frame lag.
     */
    public void preWarmSystems() {
        NozhConstants.LOGGER.debug("Pre-warming critical systems...");

        // Force class loading for hot paths
        try {
            Class.forName("dev.nozh.core.optimization.FrametimeOptimizer");
            Class.forName("dev.nozh.core.optimization.AdaptiveQualityScaler");
        } catch (ClassNotFoundException e) {
            // Ignore - class not available
        }

        NozhConstants.LOGGER.debug("Pre-warming complete");
    }

    /**
     * Gets a human-readable startup report.
     *
     * @return formatted report string
     */
    public String getStartupReport() {
        if (!startupComplete) {
            return "Startup not yet complete";
        }

        StringBuilder report = new StringBuilder();
        report.append(String.format("Total: %dms\n", totalStartupTime.get()));

        phaseDurations.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> report.append(String.format("  %s: %dms\n", e.getKey().displayName, e.getValue())));

        return report.toString();
    }
}
