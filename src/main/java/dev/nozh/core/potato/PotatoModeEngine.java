package dev.nozh.core.potato;

import dev.nozh.NozhConstants;
import dev.nozh.core.config.HardwareProfiler;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * Special optimizations for low-end PCs.
 * Makes Minecraft playable on almost any hardware.
 * 
 * <p>
 * Potato mode features:
 * <ul>
 * <li>Aggressive entity culling</li>
 * <li>Minimal particles</li>
 * <li>Reduced render distance</li>
 * <li>Disabled animations</li>
 * <li>Simplified lighting</li>
 * </ul>
 * 
 * <p>
 * <b>Auto-detection</b>: Activates automatically on systems with:
 * <ul>
 * <li>Less than 4GB RAM</li>
 * <li>Integrated GPU</li>
 * <li>Less than 4 CPU cores</li>
 * </ul>
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class PotatoModeEngine {

    /**
     * Potato optimization levels from slight to extreme.
     */
    public enum PotatoLevel {
        /** Slight reductions, still visually pleasing */
        LEVEL_1(0.8, 10, "Mild optimizations for budget PCs"),

        /** Noticeable reductions, remains playable */
        LEVEL_2(0.6, 20, "Moderate optimizations for weak PCs"),

        /** Aggressive reductions, functional gameplay */
        LEVEL_3(0.4, 35, "Aggressive optimizations for potato PCs"),

        /** Minimum viable game, bare essentials */
        LEVEL_4(0.2, 50, "Extreme optimizations for ancient hardware"),

        /** Emergency mode, absolute minimum */
        SURVIVAL(0.1, 70, "Emergency mode - playability over everything");

        /** Quality multiplier (0.0 = minimum, 1.0 = normal) */
        public final double qualityMultiplier;

        /** Estimated FPS gain percentage */
        public final int estimatedFpsGainPercent;

        /** Human-readable description */
        public final String description;

        PotatoLevel(double qualityMult, int fpsGain, String desc) {
            this.qualityMultiplier = qualityMult;
            this.estimatedFpsGainPercent = fpsGain;
            this.description = desc;
        }
    }

    /**
     * Potato mode configuration.
     * 
     * @param level              potato optimization level
     * @param renderDistance     maximum render distance in chunks
     * @param entityDistance     maximum entity distance in chunks
     * @param particleMultiplier particle spawn multiplier (0.0 to 1.0)
     * @param animationsEnabled  whether animations are enabled
     * @param smoothLighting     whether smooth lighting is enabled
     * @param cloudRendering     whether clouds are rendered
     * @param entityShadows      whether entity shadows are enabled
     */
    public record PotatoConfig(
            PotatoLevel level,
            int renderDistance,
            int entityDistance,
            double particleMultiplier,
            boolean animationsEnabled,
            boolean smoothLighting,
            boolean cloudRendering,
            boolean entityShadows) {
        /**
         * Creates config from potato level.
         * 
         * @param level potato level
         * @return potato configuration
         */
        public static PotatoConfig fromLevel(PotatoLevel level) {
            return switch (level) {
                case LEVEL_1 -> new PotatoConfig(
                        level,
                        12, // render distance
                        8, // entity distance
                        0.75, // 75% particles
                        true, // animations on
                        true, // smooth lighting on
                        true, // clouds on
                        true // shadows on
                    );
                case LEVEL_2 -> new PotatoConfig(
                        level,
                        8, // render distance
                        6, // entity distance
                        0.5, // 50% particles
                        true, // animations on
                        true, // smooth lighting on
                        false, // clouds off
                        false // shadows off
                    );
                case LEVEL_3 -> new PotatoConfig(
                        level,
                        6, // render distance
                        4, // entity distance
                        0.25, // 25% particles
                        false, // animations off
                        false, // smooth lighting off
                        false, // clouds off
                        false // shadows off
                    );
                case LEVEL_4 -> new PotatoConfig(
                        level,
                        4, // render distance
                        3, // entity distance
                        0.1, // 10% particles
                        false, // animations off
                        false, // smooth lighting off
                        false, // clouds off
                        false // shadows off
                    );
                case SURVIVAL -> new PotatoConfig(
                        level,
                        3, // minimum viable render distance
                        2, // minimum entity distance
                        0.0, // no particles
                        false, // animations off
                        false, // smooth lighting off
                        false, // clouds off
                        false // shadows off
                    );
            };
        }
    }

    // Current state
    private boolean active;
    private PotatoLevel currentLevel;
    private PotatoConfig currentConfig;

    // Hardware info
    private final HardwareProfiler hardwareProfiler;
    private long totalRamMb;
    private int cpuCores;
    private boolean integratedGpu;

    /**
     * Constructs a new PotatoModeEngine.
     * 
     * @param hardwareProfiler hardware profiler for detection
     */
    public PotatoModeEngine(HardwareProfiler hardwareProfiler) {
        this.hardwareProfiler = hardwareProfiler;
        this.active = false;
        this.currentLevel = PotatoLevel.LEVEL_1;
        this.currentConfig = PotatoConfig.fromLevel(currentLevel);

        detectHardware();
    }

    /**
     * Detects hardware capabilities.
     */
    @SuppressWarnings("deprecation") // No alternative in newer Java - method still works
    private void detectHardware() {
        try {
            // Get RAM - using deprecated method as no replacement exists
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            totalRamMb = osBean.getTotalPhysicalMemorySize() / 1024 / 1024;

            // Get CPU cores
            cpuCores = Runtime.getRuntime().availableProcessors();

            // Check if GPU is integrated (simple heuristic based on RAM)
            integratedGpu = totalRamMb < 8192; // Systems with <8GB likely have integrated GPU

            NozhConstants.LOGGER.info("Hardware detected: {}MB RAM, {} cores, likely integrated GPU: {}",
                    totalRamMb, cpuCores, integratedGpu);

        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to detect hardware: {}", e.getMessage());
            totalRamMb = 4096; // Assume 4GB as fallback
            cpuCores = 4;
            integratedGpu = true;
        }
    }

    /**
     * Checks if potato mode should activate automatically.
     * 
     * <p>
     * Criteria:
     * <ul>
     * <li>Less than 4GB RAM</li>
     * <li>Integrated GPU</li>
     * <li>Less than 4 CPU cores</li>
     * </ul>
     * 
     * @return true if potato mode recommended
     */
    public boolean shouldActivate() {
        if (totalRamMb < 4096) {
            NozhConstants.LOGGER.info("Potato mode recommended: Low RAM ({}MB)", totalRamMb);
            return true;
        }

        if (cpuCores < 4 && integratedGpu) {
            NozhConstants.LOGGER.info("Potato mode recommended: Weak CPU+GPU ({} cores, integrated)",
                    cpuCores);
            return true;
        }

        return false;
    }

    /**
     * Gets recommended potato level based on hardware.
     * 
     * @return recommended level
     */
    public PotatoLevel getRecommendedLevel() {
        int score = 0;

        // RAM scoring
        if (totalRamMb < 2048)
            score += 4; // Less than 2GB = critical
        else if (totalRamMb < 4096)
            score += 3; // Less than 4GB = severe
        else if (totalRamMb < 8192)
            score += 1; // Less than 8GB = mild

        // CPU scoring
        if (cpuCores < 2)
            score += 3;
        else if (cpuCores < 4)
            score += 2;
        else if (cpuCores < 6)
            score += 1;

        // GPU scoring
        if (integratedGpu)
            score += 2;

        // FPS scoring (placeholder - would need actual FPS tracking)
        // Assuming moderate FPS for now

        // Map score to level
        return switch (score) {
            case 0, 1, 2 -> PotatoLevel.LEVEL_1;
            case 3, 4, 5 -> PotatoLevel.LEVEL_2;
            case 6, 7, 8 -> PotatoLevel.LEVEL_3;
            case 9, 10, 11 -> PotatoLevel.LEVEL_4;
            default -> PotatoLevel.SURVIVAL;
        };
    }

    /**
     * Applies potato mode optimizations.
     * 
     * @param level potato level to apply
     */
    public void applyPotatoMode(PotatoLevel level) {
        this.currentLevel = level;
        this.currentConfig = PotatoConfig.fromLevel(level);
        this.active = true;

        NozhConstants.LOGGER.info("Potato mode activated: {} - {}",
                level, level.description);
        NozhConstants.LOGGER.info("Estimated FPS gain: +{}%", level.estimatedFpsGainPercent);
        NozhConstants.LOGGER.info("Config: RD={}, ED={}, particles={}%",
                currentConfig.renderDistance,
                currentConfig.entityDistance,
                (int) (currentConfig.particleMultiplier * 100));
    }

    public int estimateFpsGain(PotatoLevel level) {
        // Assume current FPS based on hardware
        double currentFps = 30; // Conservative estimate
        if (totalRamMb >= 8192 && cpuCores >= 4) {
            currentFps = 45;
        }

        double gainPercent = level.estimatedFpsGainPercent / 100.0;
        return (int) (currentFps * gainPercent);
    }

    /**
     * Deactivates potato mode.
     */
    public void deactivate() {
        this.active = false;
        NozhConstants.LOGGER.info("Potato mode deactivated");
    }

    /**
     * Checks if potato mode is active.
     * 
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets current potato level.
     * 
     * @return current level, or null if inactive
     */
    public PotatoLevel getCurrentLevel() {
        return active ? currentLevel : null;
    }

    /**
     * Gets current potato configuration.
     * 
     * @return current config, or null if inactive
     */
    public PotatoConfig getCurrentConfig() {
        return active ? currentConfig : null;
    }

    public String getHardwareSummary() {
        return String.format("%dMB RAM | %d cores | %s GPU",
                totalRamMb,
                cpuCores,
                integratedGpu ? "Integrated" : "Dedicated");
    }
}
