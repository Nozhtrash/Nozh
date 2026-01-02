package dev.nozh.core.config;

/**
 * Default configuration values.
 * Kept minimal for MVP.
 */
public final class NozhDefaults {

    private NozhDefaults() {
    }

    public static NozhConfig create() {
        return new NozhConfig();
    }
}
