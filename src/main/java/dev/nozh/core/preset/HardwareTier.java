package dev.nozh.core.preset;

/**
 * Hardware tier classification (Contract 10).
 * 
 * Defines hardware capability levels from weakest to strongest.
 */
public enum HardwareTier {
    /**
     * Very low-end hardware - minimal capabilities.
     * "La cafetera" - extreme performance mode.
     */
    CAFETERA,

    /**
     * Low-end hardware - integrated graphics, limited RAM.
     */
    LOW,

    /**
     * Medium hardware - entry-level dedicated GPU.
     */
    MEDIUM,

    /**
     * High-end hardware - good dedicated GPU, plenty of RAM.
     */
    HIGH,

    /**
     * Extreme hardware - enthusiast-grade components.
     */
    EXTREME,

    /**
     * NASA-grade hardware - unlimited resources mode.
     */
    NASA
}
