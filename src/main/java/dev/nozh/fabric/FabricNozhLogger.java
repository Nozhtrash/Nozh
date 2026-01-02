package dev.nozh.fabric;

import dev.nozh.NozhConstants;
import dev.nozh.core.NozhLogger;

/**
 * Fabric implementation of NozhLogger.
 * 
 * Delegates to SLF4J logger from NozhConstants.
 */
public final class FabricNozhLogger implements NozhLogger {

    @Override
    public void debug(String message) {
        if (NozhConstants.LOGGER.isDebugEnabled()) {
            NozhConstants.LOGGER.debug(message);
        }
    }

    @Override
    public void info(String message) {
        NozhConstants.LOGGER.info(message);
    }

    @Override
    public void warn(String message) {
        NozhConstants.LOGGER.warn(message);
    }

    @Override
    public void warn(String format, Object... args) {
        NozhConstants.LOGGER.warn(format, args);
    }

    @Override
    public void error(String message) {
        NozhConstants.LOGGER.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        NozhConstants.LOGGER.error(message, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return NozhConstants.LOGGER.isDebugEnabled();
    }
}
