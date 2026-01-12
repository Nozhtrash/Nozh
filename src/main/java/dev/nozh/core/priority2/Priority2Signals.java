package dev.nozh.core.priority2;

import dev.nozh.core.director.DirectorBiasHints;
import dev.nozh.core.scenario.DeepScenarioSnapshot;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Public, stable access points for Priority 2 (v0.2) signals.
 */
public final class Priority2Signals {

    private Priority2Signals() {
    }

    public static final AtomicReference<DeepScenarioSnapshot> deepScenario = new AtomicReference<>();
    public static final AtomicReference<CpuGpuBottleneckClassifier.Result> bottleneck = new AtomicReference<>();
    public static final AtomicReference<DirectorBiasHints> directorHints = new AtomicReference<>();
}
