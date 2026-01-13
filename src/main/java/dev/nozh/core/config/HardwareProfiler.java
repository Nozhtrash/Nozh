package dev.nozh.core.config;

import dev.nozh.core.preset.HardwareProfile;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.preset.StorageType;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

public final class HardwareProfiler {

    public HardwareProfiler() {
    }

    public static String buildProfile() {
        HardwareProfile profile = detectHardware();
        String os = System.getProperty("os.name", "unknown");
        String arch = System.getProperty("os.arch", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");

        return String.format(Locale.ROOT, "%s;os=%s;arch=%s;java=%s",
                profile.toProfileString(),
                sanitize(os),
                sanitize(arch),
                sanitize(javaVersion));
    }

    public static HardwareProfile detectHardware() {
        int cores = Runtime.getRuntime().availableProcessors();
        long ramGb = detectPhysicalMemoryGb().orElseGet(() -> Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024));
        String gpuName = detectGpuName().orElse("unknown");
        StorageType storageType = detectStorageType();

        HardwareTier cpuTier = classifyCpu(cores);
        HardwareTier ramTier = classifyRam(ramGb);
        HardwareTier gpuTier = classifyGpu(gpuName);
        HardwareTier overall = combineTiers(cpuTier, gpuTier, ramTier, storageType);

        return new HardwareProfile(
                cpuTier,
                gpuTier,
                ramTier,
                storageType,
                overall,
                cores,
                ramGb,
                gpuName);
    }

    @SuppressWarnings("deprecation")
    private static Optional<Long> detectPhysicalMemoryGb() {
        try {
            var osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                long bytes = sunBean.getTotalPhysicalMemorySize();
                if (bytes > 0) {
                    return Optional.of(bytes / (1024 * 1024 * 1024));
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static Optional<String> detectGpuName() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return Optional.empty();
            }
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (device != null) {
                return Optional.ofNullable(device.getIDstring());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static StorageType detectStorageType() {
        Optional<String> device = resolveRootDevice();
        if (device.isPresent()) {
            Optional<Boolean> rotational = readRotational(device.get());
            if (rotational.isPresent()) {
                return rotational.get() ? StorageType.HDD : StorageType.SSD;
            }
        }
        Optional<Boolean> isSsd = detectFileStoreSsd();
        if (isSsd.isPresent()) {
            return isSsd.get() ? StorageType.SSD : StorageType.HDD;
        }
        return StorageType.UNKNOWN;
    }

    private static Optional<Boolean> detectFileStoreSsd() {
        try {
            var store = Files.getFileStore(Paths.get("."));
            Object attribute = store.getAttribute("volume:isSSD");
            if (attribute instanceof Boolean bool) {
                return Optional.of(bool);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static Optional<String> resolveRootDevice() {
        Path mounts = Paths.get("/proc/mounts");
        if (!Files.exists(mounts)) {
            return Optional.empty();
        }
        try (BufferedReader reader = Files.newBufferedReader(mounts)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length > 1 && "/".equals(parts[1])) {
                    String device = parts[0];
                    if (device.startsWith("/dev/")) {
                        device = device.substring("/dev/".length());
                        device = device.replaceAll("\\d+$", "");
                        return Optional.of(device);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private static Optional<Boolean> readRotational(String device) {
        Path rotational = Paths.get("/sys/block", device, "queue", "rotational");
        if (!Files.exists(rotational)) {
            return Optional.empty();
        }
        try {
            String value = Files.readString(rotational).trim();
            if ("0".equals(value)) {
                return Optional.of(false);
            }
            if ("1".equals(value)) {
                return Optional.of(true);
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private static HardwareTier classifyCpu(int cores) {
        if (cores <= 2) {
            return HardwareTier.CAFETERA;
        }
        if (cores <= 4) {
            return HardwareTier.LOW;
        }
        if (cores <= 6) {
            return HardwareTier.MEDIUM;
        }
        if (cores <= 8) {
            return HardwareTier.HIGH;
        }
        if (cores <= 12) {
            return HardwareTier.EXTREME;
        }
        return HardwareTier.NASA;
    }

    private static HardwareTier classifyRam(long ramGb) {
        if (ramGb <= 4) {
            return HardwareTier.CAFETERA;
        }
        if (ramGb <= 8) {
            return HardwareTier.LOW;
        }
        if (ramGb <= 16) {
            return HardwareTier.MEDIUM;
        }
        if (ramGb <= 32) {
            return HardwareTier.HIGH;
        }
        if (ramGb <= 64) {
            return HardwareTier.EXTREME;
        }
        return HardwareTier.NASA;
    }

    private static HardwareTier classifyGpu(String gpuName) {
        if (gpuName == null || gpuName.isBlank()) {
            return HardwareTier.MEDIUM;
        }
        String lower = gpuName.toLowerCase(Locale.ROOT);
        if (lower.contains("rtx 40") || lower.contains("rx 79") || lower.contains("rx 78")) {
            return HardwareTier.EXTREME;
        }
        if (lower.contains("rtx") || lower.contains("rx 6") || lower.contains("rx 7")) {
            return HardwareTier.HIGH;
        }
        if (lower.contains("gtx") || lower.contains("rx 5") || lower.contains("radeon")) {
            return HardwareTier.MEDIUM;
        }
        if (lower.contains("intel") || lower.contains("uhd") || lower.contains("iris") || lower.contains("vega")) {
            return HardwareTier.LOW;
        }
        return HardwareTier.MEDIUM;
    }

    private static HardwareTier combineTiers(HardwareTier cpu, HardwareTier gpu, HardwareTier ram,
            StorageType storage) {
        HardwareTier baseline = minTier(cpu, gpu, ram);
        if (storage == StorageType.HDD) {
            return downgrade(baseline);
        }
        return baseline;
    }

    private static HardwareTier minTier(HardwareTier... tiers) {
        HardwareTier lowest = HardwareTier.NASA;
        for (HardwareTier tier : tiers) {
            if (tier == null) {
                continue;
            }
            if (tier.ordinal() < lowest.ordinal()) {
                lowest = tier;
            }
        }
        return lowest;
    }

    private static HardwareTier downgrade(HardwareTier tier) {
        if (tier == null) {
            return HardwareTier.MEDIUM;
        }
        int ordinal = Math.max(0, tier.ordinal() - 1);
        return HardwareTier.values()[ordinal];
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace(';', '_').replace('\n', '_').trim();
    }
}
