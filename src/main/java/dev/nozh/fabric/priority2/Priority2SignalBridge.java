package dev.nozh.fabric.priority2;

import dev.nozh.core.priority2.Priority2Signals;

/**
 * Bridges Fabric-side publishers to core, stable signal holders.
 */
final class Priority2SignalBridge {

    private Priority2SignalBridge() {
    }

    static void publishFromEntryPoint() {
        Priority2Signals.deepScenario.set(NozhPriority2Client.LAST_DEEP_SCENARIO.get());
        Priority2Signals.bottleneck.set(NozhPriority2Client.LAST_BOTTLENECK.get());
        Priority2Signals.directorHints.set(NozhPriority2Client.LAST_DIRECTOR_HINTS.get());
    }
}
