package dev.nozh.core.analytics;

import dev.nozh.api.Scenario;
import dev.nozh.NozhConstants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analyzes play session for performance insights.
 * Tracks FPS,spikes, scenarios, and optimizations applied.
 * 
 * <p>
 * Generates comprehensive session summaries with:
 * <ul>
 * <li>Performance metrics (FPS statistics)</li>
 * <li>Spike analysis</li>
 * <li>Time per scenario</li>
 * <li>Applied optimizations and their impact</li>
 * <li>Recommendations for future sessions</li>
 * </ul>
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class SessionAnalyzer {

    /**
     * Comprehensive session summary.
     * 
     * @param duration             session duration in milliseconds
     * @param avgFps               average frames per second
     * @param minFps               minimum FPS encountered
     * @param maxFps               maximum FPS encountered
     * @param p95Fps               P95 FPS (95th percentile)
     * @param spikeCount           number of performance spikes
     * @param timePerScenario      time spent in each scenario (ms)
     * @param appliedOptimizations list of optimizations applied
     * @param totalFpsGained       total FPS gained from optimizations
     * @param recommendations      suggestions for future sessions
     */
    public record SessionSummary(
            long duration,
            double avgFps,
            double minFps,
            double maxFps,
            double p95Fps,
            int spikeCount,
            Map<Scenario, Long> timePerScenario,
            List<String> appliedOptimizations,
            double totalFpsGained,
            List<String> recommendations) {
        /**
         * Formats summary as human-readable string.
         * 
         * @return formatted summary
         */
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Session Summary ===\n");
            sb.append(String.format("Duration: %s\n", formatDuration(duration)));
            sb.append(String.format("FPS: avg=%.1f, min=%.1f, max=%.1f, P95=%.1f\n",
                    avgFps, minFps, maxFps, p95Fps));
            sb.append(String.format("Spikes: %d\n", spikeCount));
            sb.append(String.format("FPS Gained: +%.1f\n", totalFpsGained));
            sb.append(String.format("Optimizations Applied: %d\n", appliedOptimizations.size()));

            if (!timePerScenario.isEmpty()) {
                sb.append("\nTime Per Scenario:\n");
                timePerScenario.entrySet().stream()
                        .sorted(Map.Entry.<Scenario, Long>comparingByValue().reversed())
                        .forEach(e -> sb.append(String.format("  %s: %s\n",
                                e.getKey(), formatDuration(e.getValue()))));
            }

            if (!recommendations.isEmpty()) {
                sb.append("\nRecommendations:\n");
                recommendations.forEach(r -> sb.append("  - ").append(r).append("\n"));
            }

            return sb.toString();
        }

        private static String formatDuration(long ms) {
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            seconds %= 60;

            if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds);
            }
            return String.format("%ds", seconds);
        }
    }

    // Session tracking
    private long sessionStartTime;
    private boolean sessionActive;

    // FPS tracking
    private final List<Double> fpsHistory;
    private double fpsSum;
    private double minFps;
    private double maxFps;
    private int sampleCount;

    // Spike tracking
    private int spikeCount;
    private static final double SPIKE_THRESHOLD = 50.0; // 50ms frametime = FPS drop

    // Scenario tracking
    private final Map<Scenario, Long> scenarioDurations;
    private Scenario currentScenario;
    private long scenarioStartTime;

    // Optimization tracking
    private final List<String> appliedOptimizations;
    private double totalFpsGained;

    /**
     * Constructs a new SessionAnalyzer.
     */
    public SessionAnalyzer() {
        this.fpsHistory = Collections.synchronizedList(new ArrayList<>(3600)); // 1 min at 60fps
        this.scenarioDurations = new ConcurrentHashMap<>();
        this.appliedOptimizations = Collections.synchronizedList(new ArrayList<>());
        this.sessionActive = false;
        reset();
    }

    /**
     * Starts a new session.
     */
    public void startSession() {
        reset();
        sessionStartTime = System.currentTimeMillis();
        sessionActive = true;

        NozhConstants.LOGGER.info("Session analysis started");
    }

    /**
     * Ends the current session.
     */
    public void endSession() {
        if (!sessionActive)
            return;

        sessionActive = false;

        if (currentScenario != null) {
            recordScenarioDuration(currentScenario);
        }

        NozhConstants.LOGGER.info("Session analysis ended");
    }

    /**
     * Records an FPS sample.
     * 
     * @param fps current FPS
     */
    public void recordFps(double fps) {
        if (!sessionActive || fps <= 0)
            return;

        fpsHistory.add(fps);
        fpsSum += fps;
        sampleCount++;

        if (fps < minFps)
            minFps = fps;
        if (fps > maxFps)
            maxFps = fps;

        // Check for spike (low FPS = high frametime)
        double frametime = 1000.0 / fps;
        if (frametime > SPIKE_THRESHOLD) {
            spikeCount++;
        }
    }

    /**
     * Records scenario change.
     * 
     * @param scenario new scenario
     */
    public void recordScenario(Scenario scenario) {
        if (!sessionActive || scenario == currentScenario)
            return;

        // Record duration of previous scenario
        if (currentScenario != null) {
            recordScenarioDuration(currentScenario);
        }

        currentScenario = scenario;
        scenarioStartTime = System.currentTimeMillis();
    }

    /**
     * Records duration for a scenario.
     */
    private void recordScenarioDuration(Scenario scenario) {
        long duration = System.currentTimeMillis() - scenarioStartTime;
        scenarioDurations.merge(scenario, duration, Long::sum);
    }

    /**
     * Records an applied optimization.
     * 
     * @param optimization optimization description
     * @param fpsGain      estimated FPS gain
     */
    public void recordOptimization(String optimization, double fpsGain) {
        if (!sessionActive)
            return;

        appliedOptimizations.add(String.format("%s (+%.1f FPS)", optimization, fpsGain));
        totalFpsGained += fpsGain;

        NozhConstants.LOGGER.info("Optimization recorded: {} (+{} FPS)", optimization, fpsGain);
    }

    /**
     * Analyzes current session and generates summary.
     * 
     * @return session summary
     */
    public SessionSummary analyzeCurrent() {
        long duration = sessionActive ? System.currentTimeMillis() - sessionStartTime : 0;

        double avgFps = sampleCount > 0 ? fpsSum / sampleCount : 0;
        double p95Fps = calculateP95Fps();

        List<String> recommendations = generateRecommendations(
                avgFps, minFps, spikeCount, scenarioDurations);

        return new SessionSummary(
                duration,
                avgFps,
                minFps,
                maxFps,
                p95Fps,
                spikeCount,
                Map.copyOf(scenarioDurations),
                List.copyOf(appliedOptimizations),
                totalFpsGained,
                recommendations);
    }

    /**
     * Calculates P95 FPS (95th percentile).
     * 
     * @return P95 FPS value
     */
    private double calculateP95Fps() {
        if (fpsHistory.isEmpty())
            return 0;

        List<Double> sorted = new ArrayList<>(fpsHistory);
        sorted.sort(Double::compareTo);

        int p95Index = (int) (sorted.size() * 0.95);
        return sorted.get(Math.min(p95Index, sorted.size() - 1));
    }

    /**
     * Generates recommendations based on session data.
     * 
     * @param avgFps    average FPS
     * @param minFps    minimum FPS
     * @param spikes    spike count
     * @param scenarios scenario durations
     * @return list of recommendations
     */
    private List<String> generateRecommendations(
            double avgFps,
            double minFps,
            int spikes,
            Map<Scenario, Long> scenarios) {
        List<String> recs = new ArrayList<>();

        // FPS-based recommendations
        if (avgFps < 45) {
            recs.add("Consider enabling Potato Mode for better performance");
        } else if (avgFps > 100) {
            recs.add("Performance is excellent, you can increase quality settings");
        }

        // Spike recommendations
        if (spikes > 10) {
            recs.add(String.format("High spike count (%d) - check for mod conflicts", spikes));
        }

        // Scenario-based recommendations
        Scenario dominantScenario = scenarios.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (dominantScenario == Scenario.COMBAT) {
            recs.add("Optimize for combat: reduce particle effects and entity shadows");
        } else if (dominantScenario == Scenario.EXPLORATION) {
            recs.add("Optimize for exploration: reduce render distance slightly");
        }

        // Minimum FPS warning
        if (minFps < 20) {
            recs.add("Minimum FPS is very low - enable aggressive optimizations");
        }

        if (recs.isEmpty()) {
            recs.add("No major issues detected - performance is stable");
        }

        return recs;
    }

    /**
     * Resets all session data.
     */
    private void reset() {
        fpsHistory.clear();
        scenarioDurations.clear();
        appliedOptimizations.clear();

        fpsSum = 0;
        minFps = Double.MAX_VALUE;
        maxFps = 0;
        sampleCount = 0;
        spikeCount = 0;
        totalFpsGained = 0;

        currentScenario = null;
        scenarioStartTime = 0;
    }

    /**
     * Checks if session is active.
     * 
     * @return true if active
     */
    public boolean isSessionActive() {
        return sessionActive;
    }

    /**
     * Gets current session duration.
     * 
     * @return duration in milliseconds
     */
    public long getSessionDuration() {
        return sessionActive ? System.currentTimeMillis() - sessionStartTime : 0;
    }
}
