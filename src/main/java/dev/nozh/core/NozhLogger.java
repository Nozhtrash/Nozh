package dev.nozh.core;

/**
 * Logger abstraction for Contract 2 purity.
 * 
 * This interface:
 * - Lives in nozh-core
 * - Has ZERO Minecraft dependencies
 * - Is trivially mockable
 * 
 * Implementations:
 * - NoOpLogger (tests)
 * - FabricNozhLogger (integration)
 */
public interface NozhLogger {

    /**
     * Log info message.
     */
    void info(String msg);

    /**
     * Log warning message.
     */
    void warn(String msg);

    /**
     * Log warning with args.
     */
    void warn(String format, Object... args);

    /**
     * Log error message with throwable.
     */
    void error(String msg, Throwable t);

    /**
     * Log error message.
     */
    void error(String msg);

    /**
     * Log debug message.
     */
    void debug(String msg);

    /**
     * Check if debug logging is enabled.
     */
    boolean isDebugEnabled();
}
