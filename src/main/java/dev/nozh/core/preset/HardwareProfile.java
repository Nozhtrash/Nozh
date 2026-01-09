package dev.nozh.core.preset;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record HardwareProfile(
        HardwareTier cpuTier,
        HardwareTier gpuTier,
        HardwareTier ramTier,
        StorageType storageType,
        HardwareTier overallTier,
        int cpuCores,
        long ramGb,
        String gpuName) {

    public static HardwareProfile unknown() {
        return new HardwareProfile(
                HardwareTier.MEDIUM,
                HardwareTier.MEDIUM,
                HardwareTier.MEDIUM,
                StorageType.UNKNOWN,
                HardwareTier.MEDIUM,
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024),
                "unknown");
    }

    public String toProfileString() {
        return String.format(Locale.ROOT,
                "cpuCores=%d;cpuTier=%s;gpu=%s;gpuTier=%s;ramGb=%d;ramTier=%s;storage=%s;overall=%s",
                cpuCores,
                cpuTier,
                sanitize(gpuName),
                gpuTier,
                ramGb,
                ramTier,
                storageType,
                overallTier);
    }

    public static Optional<HardwareProfile> parse(String profile) {
        if (profile == null || profile.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> tokens = ProfileTokenParser.parse(profile);
        HardwareTier cpuTier = parseTier(tokens.get("cpuTier")).orElse(null);
        HardwareTier gpuTier = parseTier(tokens.get("gpuTier")).orElse(null);
        HardwareTier ramTier = parseTier(tokens.get("ramTier")).orElse(null);
        HardwareTier overallTier = parseTier(tokens.get("overall")).orElse(null);
        StorageType storageType = parseStorage(tokens.get("storage")).orElse(null);
        int cpuCores = parseInt(tokens.get("cpuCores")).orElse(Runtime.getRuntime().availableProcessors());
        long ramGb = parseLong(tokens.get("ramGb")).orElse(Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024));
        String gpuName = tokens.getOrDefault("gpu", "unknown");

        if (cpuTier == null || gpuTier == null || ramTier == null || overallTier == null || storageType == null) {
            return Optional.empty();
        }

        return Optional.of(new HardwareProfile(
                cpuTier,
                gpuTier,
                ramTier,
                storageType,
                overallTier,
                cpuCores,
                ramGb,
                gpuName));
    }

    private static Optional<HardwareTier> parseTier(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(HardwareTier.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<StorageType> parseStorage(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(StorageType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Long> parseLong(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(';', '_').replace('\n', '_').trim();
    }
}
