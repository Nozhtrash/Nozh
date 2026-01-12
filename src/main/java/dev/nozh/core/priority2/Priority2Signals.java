package dev.nozh.core.priority2;

import dev.nozh.core.director.DirectorBiasHints;
import dev.nozh.core.scenario.DeepScenarioSnapshot;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Public, stable access points for Priority 2 (v0.2) signals.
 *
 * <p>This prevents tight coupling between the Fabric-side sampling code and the core governor/hud.
 * The governor can consume these atomics without owning Fabric events.</p>
 */
public final class Priority2Signals {

    private Priority2Signals() {
    }

    public static final AtomicReference<DeepScenarioSnapshot> deepScenario = new AtomicReference<>();
    public static final AtomicReference<CpuGpuBottleneckClassifier.Result> bottleneck = new AtomicReference<>();
    public static final AtomicReference<DirectorBiasHints> directorHints = new AtomicReference<>();
}
