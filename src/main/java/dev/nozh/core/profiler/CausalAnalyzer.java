package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CausalAnalyzer {

    private static final int[] LAG_SECONDS = { 1, 2, 3 };
    private static final int MAX_SECONDS = 12;
    private static final double MIN_CORRELATION = 0.25;

    private final SpikeCausalityAnalyzer immediateAnalyzer = new SpikeCausalityAnalyzer();
    private final Map<Long, CausalSample> samples = new LinkedHashMap<>();
    private int lastSpikeCount = -1;

    public SpikeCausalityReport analyze(PerfSnapshot frameSnapshot,
            PerfSnapshot tickSnapshot,
            GcMetricsSnapshot gcMetrics,
            FramePauseSnapshot pauses,
            RenderPipelineSnapshot renderSnapshot,
            PerfTraceSnapshot traceSnapshot) {
        SpikeCausalityReport immediate = immediateAnalyzer.analyze(frameSnapshot, tickSnapshot, gcMetrics,
                pauses, renderSnapshot, traceSnapshot);

        updateSamples(frameSnapshot, tickSnapshot, gcMetrics, pauses, renderSnapshot);

        CausalityScore delayed = resolveDelayedCause();
        if (delayed != null && delayed.confidence > immediate.confidence()) {
            return new SpikeCausalityReport(delayed.cause, delayed.confidence, delayed.detail);
        }
        return immediate != null ? immediate : SpikeCausalityReport.unknown();
    }

    public void reset() {
        samples.clear();
        lastSpikeCount = -1;
    }

    private void updateSamples(PerfSnapshot frameSnapshot,
            PerfSnapshot tickSnapshot,
            GcMetricsSnapshot gcMetrics,
            FramePauseSnapshot pauses,
            RenderPipelineSnapshot renderSnapshot) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        int spikeDelta = resolveSpikeDelta(frameSnapshot);
        double gcMs = gcMetrics != null ? gcMetrics.recentGcMs() : 0.0;
        double tickP95 = tickSnapshot != null ? tickSnapshot.p95FrametimeMs() : 0.0;
        double renderMax = 0.0;
        if (renderSnapshot != null && renderSnapshot.hottestPhase() != null) {
            renderMax = renderSnapshot.hottestPhase().maxMs();
        }
        double frameP95 = frameSnapshot != null ? frameSnapshot.p95FrametimeMs() : 0.0;
        double pauseMax = pauses != null ? pauses.maxPauseMs() : 0.0;

        CausalSample sample = samples.get(nowSeconds);
        if (sample == null) {
            sample = new CausalSample(nowSeconds);
            samples.put(nowSeconds, sample);
        }
        sample.update(spikeDelta, gcMs, tickP95, renderMax, frameP95, pauseMax);
        pruneOld(nowSeconds);
    }

    private int resolveSpikeDelta(PerfSnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        int current = snapshot.spikeCount();
        int delta = 0;
        if (lastSpikeCount >= 0) {
            delta = Math.max(0, current - lastSpikeCount);
        }
        lastSpikeCount = current;
        return delta;
    }

    private void pruneOld(long nowSeconds) {
        long cutoff = nowSeconds - MAX_SECONDS;
        List<Long> toRemove = new ArrayList<>();
        for (Long second : samples.keySet()) {
            if (second < cutoff) {
                toRemove.add(second);
            } else {
                break;
            }
        }
        for (Long second : toRemove) {
            samples.remove(second);
        }
    }

    private CausalityScore resolveDelayedCause() {
        CausalityScore best = null;
        for (SpikeCauseType cause : new SpikeCauseType[] { SpikeCauseType.GC, SpikeCauseType.TICK,
                SpikeCauseType.RENDER, SpikeCauseType.FRAME }) {
            for (int lag : LAG_SECONDS) {
                double correlation = calculateCorrelation(cause, lag);
                if (correlation <= MIN_CORRELATION) {
                    continue;
                }
                double confidence = Math.min(0.95, 0.25 + 0.7 * correlation);
                String detail = String.format("lag %ds corr %.2f", lag, correlation);
                if (best == null || confidence > best.confidence) {
                    best = new CausalityScore(cause, confidence, detail);
                }
            }
        }
        return best;
    }

    private double calculateCorrelation(SpikeCauseType cause, int lagSeconds) {
        if (samples.size() < 4) {
            return 0.0;
        }
        List<Double> metrics = new ArrayList<>();
        List<Double> spikes = new ArrayList<>();
        for (Map.Entry<Long, CausalSample> entry : samples.entrySet()) {
            long second = entry.getKey();
            CausalSample spikeSample = entry.getValue();
            CausalSample metricSample = samples.get(second - lagSeconds);
            if (metricSample == null) {
                continue;
            }
            metrics.add(metricSample.metricForCause(cause));
            spikes.add((double) spikeSample.spikeDelta);
        }
        if (metrics.size() < 3) {
            return 0.0;
        }
        return Math.max(0.0, pearson(metrics, spikes));
    }

    private double pearson(List<Double> xs, List<Double> ys) {
        int size = xs.size();
        if (size == 0 || size != ys.size()) {
            return 0.0;
        }
        double meanX = 0.0;
        double meanY = 0.0;
        for (int i = 0; i < size; i++) {
            meanX += xs.get(i);
            meanY += ys.get(i);
        }
        meanX /= size;
        meanY /= size;
        double numerator = 0.0;
        double sumSqX = 0.0;
        double sumSqY = 0.0;
        for (int i = 0; i < size; i++) {
            double dx = xs.get(i) - meanX;
            double dy = ys.get(i) - meanY;
            numerator += dx * dy;
            sumSqX += dx * dx;
            sumSqY += dy * dy;
        }
        double denominator = Math.sqrt(sumSqX * sumSqY);
        if (denominator <= 0.0) {
            return 0.0;
        }
        return numerator / denominator;
    }

    private static final class CausalSample {
        private final long second;
        private int spikeDelta;
        private double gcMs;
        private double tickP95;
        private double renderMax;
        private double frameP95;
        private double pauseMax;

        private CausalSample(long second) {
            this.second = second;
        }

        private void update(int spikeDelta, double gcMs, double tickP95, double renderMax, double frameP95,
                double pauseMax) {
            this.spikeDelta += spikeDelta;
            this.gcMs = Math.max(this.gcMs, gcMs);
            this.tickP95 = Math.max(this.tickP95, tickP95);
            this.renderMax = Math.max(this.renderMax, renderMax);
            this.frameP95 = Math.max(this.frameP95, frameP95);
            this.pauseMax = Math.max(this.pauseMax, pauseMax);
        }

        private double metricForCause(SpikeCauseType cause) {
            return switch (cause) {
                case GC -> gcMs;
                case TICK -> tickP95;
                case RENDER -> renderMax;
                case FRAME -> Math.max(frameP95, pauseMax);
                case CRITICAL_EVENT, UNKNOWN -> 0.0;
            };
        }
    }

    private static final class CausalityScore {
        private final SpikeCauseType cause;
        private final double confidence;
        private final String detail;

        private CausalityScore(SpikeCauseType cause, double confidence, String detail) {
            this.cause = cause;
            this.confidence = confidence;
            this.detail = detail;
        }
    }
}
