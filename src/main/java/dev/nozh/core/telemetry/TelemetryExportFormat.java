package dev.nozh.core.telemetry;

public enum TelemetryExportFormat {
    CSV("csv", ""),
    JSON("json", ""),
    COMPACT_CSV("csv", "compact"),
    COMPACT_JSON("json", "compact");

    private final String fileExtension;
    private final String fileSuffix;

    TelemetryExportFormat(String fileExtension, String fileSuffix) {
        this.fileExtension = fileExtension;
        this.fileSuffix = fileSuffix;
    }

    public String fileExtension() {
        return fileExtension;
    }

    public String fileSuffix() {
        return fileSuffix;
    }

    public boolean isCsv() {
        return this == CSV || this == COMPACT_CSV;
    }

    public boolean isCompact() {
        return this == COMPACT_CSV || this == COMPACT_JSON;
    }
}
