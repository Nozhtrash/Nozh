package dev.nozh.core.telemetry;

/**
 * Telemetry snapshot (Contract 4).
 * 
 * Cheap copy of aggregated telemetry data for HUD/Governor consumption.
 * PURE - no mutable state, no nested objects.
 * 
 * PHASE 2 UPDATE: Added 1% lows (p99), 0.1% lows (p999), and variance.
 */
public record TelemetrySnapshot(
                double avgFrametimeMs,
                double p95FrametimeMs,
                double p99FrametimeMs, // 1% lows
                double p999FrametimeMs, // 0.1% lows
                double frametimeVariance,
                int spikeCount,
                int sampleCount,
                int droppedSamples,
                int consecutiveSlowFrames, // Phase 3: Immediate trigger
                int maxChunkEntityCount, // Phase 4: Density
                int denseChunkCount, // Phase 4: Density
                boolean sufficientData) {

        /**
         * Empty snapshot for when no data is available.
         */
        public static TelemetrySnapshot EMPTY = new TelemetrySnapshot(
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);

        /**
         * Minimum samples required for sufficient data.
         */
        private static final int MIN_SAMPLES = 30;

        /**
         * Create snapshot with automatic sufficiency check (Phase 2 Full Metrics).
         */
        public static TelemetrySnapshot of(
                        double avgFrametimeMs,
                        double p95FrametimeMs,
                        double p99FrametimeMs,
                        double p999FrametimeMs,
                        double frametimeVariance,
                        int spikeCount,
                        int sampleCount,
                        int droppedSamples,
                        int consecutiveSlowFrames,
                        int maxChunkEntityCount,
                        int denseChunkCount) {
                boolean sufficient = sampleCount >= MIN_SAMPLES;
                return new TelemetrySnapshot(
                                avgFrametimeMs,
                                p95FrametimeMs,
                                p99FrametimeMs,
                                p999FrametimeMs,
                                frametimeVariance,
                                spikeCount,
                                sampleCount,
                                droppedSamples,
                                consecutiveSlowFrames,
                                maxChunkEntityCount,
                                denseChunkCount,
                                sufficient);
        }

        /**
         * Legacy factory for backward compatibility.
         */
        public static TelemetrySnapshot of(
                        double avgFrametimeMs,
                        double p95FrametimeMs,
                        int spikeCount,
                        int sampleCount,
                        int droppedSamples) {
                return of(avgFrametimeMs, p95FrametimeMs, 0.0, 0.0, 0.0, spikeCount, sampleCount, droppedSamples, 0, 0,
                                0);
        }

        // --- Helpers (Derived Data) ---

        public double avgFps() {
                return (avgFrametimeMs > 0) ? 1000.0 / avgFrametimeMs : 0.0;
        }

        public double fpsFromP95() {
                return (p95FrametimeMs > 0) ? 1000.0 / p95FrametimeMs : 0.0;
        }

        public double fpsFromP99() {
                return (p99FrametimeMs > 0) ? 1000.0 / p99FrametimeMs : 0.0;
        }

        public double fpsFromP999() {
                return (p999FrametimeMs > 0) ? 1000.0 / p999FrametimeMs : 0.0;
        }
}
