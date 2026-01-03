package dev.nozh.core.config;

@FunctionalInterface
public interface ConfigListener {
    void onConfigUpdated(NozhConfig config);
}
