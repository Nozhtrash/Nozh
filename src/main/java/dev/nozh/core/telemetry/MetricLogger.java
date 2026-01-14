package dev.nozh.core.telemetry;

import dev.nozh.NozhConstants;
import dev.nozh.core.governor.GovernorState;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.Instant;

/**
 * Silent CSV Logger for tuning NOZH.
 * Writes minimalist data to 'nozh_metrics.csv' in the game directory.
 */
public class MetricLogger {
    private static final String HEADER = "timestamp,avg_ms,p99_ms,variance,spikes,state,action,decision_age_ms";
    private static final DecimalFormat DF = new DecimalFormat("#.##");
    private final Path csvPath;
    private boolean initialized = false;

    public MetricLogger() {
        this.csvPath = FabricLoader.getInstance().getGameDir().resolve("nozh_metrics.csv");
    }

    public void log(TelemetrySnapshot snapshot, GovernorState state, String action, long decisionAgeMs) {
        if (!initialized) {
            initialize();
        }

        if (snapshot == null)
            return;

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            StringBuilder line = new StringBuilder();
            line.append(System.currentTimeMillis()).append(",");
            line.append(DF.format(snapshot.avgFrametimeMs())).append(",");
            line.append(DF.format(snapshot.p99FrametimeMs())).append(",");
            line.append(DF.format(snapshot.frametimeVariance())).append(",");
            line.append(snapshot.spikeCount()).append(",");
            line.append(state != null ? state.name() : "UNKNOWN").append(",");
            line.append(action != null ? action : "").append(",");
            line.append(decisionAgeMs);

            writer.write(line.toString());
            writer.newLine();
        } catch (IOException e) {
            // Silent failure
        }
    }

    private synchronized void initialize() {
        if (initialized)
            return;
        try {
            if (!Files.exists(csvPath)) {
                Files.writeString(csvPath, HEADER + System.lineSeparator());
            }
            initialized = true;
        } catch (IOException e) {
            NozhConstants.LOGGER.error("Failed to init metric logger", e);
        }
    }
}
