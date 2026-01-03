package dev.nozh.core.config;

import dev.nozh.NozhConstants;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;

/**
 * Dedicated service to synchronize config changes into RuntimeState.
 */
public final class ConfigSyncService {

    private final StateStore stateStore;
    private final ConfigListener listener;

    private ConfigSyncService(StateStore stateStore) {
        this.stateStore = stateStore;
        this.listener = config -> stateStore.update(state -> state.withConfig(config));
    }

    public static ConfigSyncService start(StateStore stateStore) {
        ConfigSyncService service = new ConfigSyncService(stateStore);
        service.syncFromConfig();
        ConfigManager.addListener(service.listener);
        return service;
    }

    public void stop() {
        ConfigManager.removeListener(listener);
    }

    private void syncFromConfig() {
        NozhConfig config = ConfigManager.getConfig();
        if (config == null) {
            NozhConstants.LOGGER.warn("Config sync skipped: config is null");
            return;
        }
        RuntimeState runtimeState = RuntimeState.fromConfig(config);
        stateStore.replaceState(runtimeState);
        NozhConstants.LOGGER.info("RuntimeState synchronized from config (version {})",
                runtimeState.stateVersion());
    }
}
