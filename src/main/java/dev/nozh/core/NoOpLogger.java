package dev.nozh.core;

/**
 * No-op logger for tests (Contract 2).
 * 
 * All logging calls are silently ignored.
 * Zero overhead, zero dependencies.
 */
public final class NoOpLogger implements NozhLogger {

    @Override
    public void info(String msg) {
        // No-op
    }

    @Override
    public void warn(String msg) {
        // No-op
    }

    @Override
    public void warn(String format, Object... args) {
        // No-op
    }

    @Override
    public void error(String msg, Throwable t) {
        // No-op
    }

    @Override
    public void error(String msg) {
        // No-op
    }

    @Override
    public void debug(String msg) {
        // No-op
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }
}
