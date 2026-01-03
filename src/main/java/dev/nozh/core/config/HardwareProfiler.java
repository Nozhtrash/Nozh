package dev.nozh.core.config;

import java.util.Locale;

public final class HardwareProfiler {

    private HardwareProfiler() {
    }

    public static String buildProfile() {
        int cores = Runtime.getRuntime().availableProcessors();
        long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String os = System.getProperty("os.name", "unknown");
        String arch = System.getProperty("os.arch", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");

        return String.format(Locale.ROOT, "cores=%d;maxMemoryMb=%d;os=%s;arch=%s;java=%s",
                cores,
                maxMemoryMb,
                sanitize(os),
                sanitize(arch),
                sanitize(javaVersion));
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace(';', '_').replace('\n', '_').trim();
    }
}
