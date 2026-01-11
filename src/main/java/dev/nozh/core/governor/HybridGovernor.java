package dev.nozh.core.governor;

import dev.nozh.core.NozhLogger;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.config.OptimizationProfile;
import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.matrix.ActionMatrixTuning;
import dev.nozh.core.state.RuntimeState;

import java.util.Map;
import java.util.Optional;

public final class HybridGovernor {

    private final SimulationGovernor rulesGovernor;
    private final DecisionTreeModelStore modelStore;
    private final NozhLogger logger;

    public HybridGovernor(ActionMatrix actionMatrix, NozhLogger logger) {
        this.rulesGovernor = new SimulationGovernor(actionMatrix);
        this.modelStore = new DecisionTreeModelStore(logger);
        this.logger = logger;
    }

    public Optional<ActionCandidate> decide(
            RuntimeState state,
            GovernorMode mode,
            String currentBound,
            long nowMillis,
            OptimizationProfile profile,
            int targetFps,
            double reverseEpsilonMs,
            boolean reverseReady,
            dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
            Map<dev.nozh.core.capability.CapabilityId, dev.nozh.core.capability.CapabilityValue> currentSettings,
            NozhConfig config,
            ActionMatrixTuning tuning,
            dev.nozh.core.profiler.SpikeCausalityReport spikeCausality) {
        Optional<ActionCandidate> candidate = rulesGovernor.decide(
                state,
                mode,
                currentBound,
                nowMillis,
                profile,
                targetFps,
                reverseEpsilonMs,
                reverseReady,
                baselineSnapshot,
                currentSettings);
        if (candidate.isEmpty()) {
            return candidate;
        }
        if (config == null || !config.hybridModelEnabled) {
            return candidate;
        }
        Optional<DecisionTreeModel> modelOpt = modelStore.loadModel();
        if (modelOpt.isEmpty()) {
            return candidate;
        }
        DecisionTreeModel model = modelOpt.get();
        DecisionFeatures features = new DecisionFeatures(
                state.avgFrametimeMs(),
                state.p95FrametimeMs(),
                state.spikeCount(),
                currentBound,
                mode,
                state.currentScenario());
        DecisionTreeModel.ModelDecision decision = model.evaluate(features);
        if (decision.label() == DecisionTreeModel.DecisionLabel.BLOCK
                && decision.confidence() >= config.hybridModelBlockConfidence) {
            logger.debug(String.format(
                    "Hybrid model blocked action (confidence=%.2f)",
                    decision.confidence()));
            return Optional.empty();
        }
        return candidate;
    }

    public Optional<ActionCandidate> decide(
            RuntimeState state,
            GovernorMode mode,
            String currentBound,
            long nowMillis,
            OptimizationProfile profile,
            int targetFps,
            double reverseEpsilonMs,
            boolean reverseReady,
            dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
            Map<dev.nozh.core.capability.CapabilityId, dev.nozh.core.capability.CapabilityValue> currentSettings,
            NozhConfig config,
            ActionMatrixTuning tuning) {
        return decide(state, mode, currentBound, nowMillis, profile, targetFps, reverseEpsilonMs, reverseReady,
                baselineSnapshot, currentSettings, config, tuning, null);
    }

    public boolean canAct(RuntimeState state, long lastActionTimestamp, long nowMillis, boolean benchmarkMode,
            long benchmarkIntervalMillis) {
        return rulesGovernor.canAct(state, lastActionTimestamp, nowMillis, benchmarkMode, benchmarkIntervalMillis);
    }

    public long getObservationWindow(RuntimeState state) {
        return rulesGovernor.getObservationWindow(state);
    }

    public ActionOutcome evaluateOutcome(dev.nozh.api.PerfSnapshot previousSnapshot,
            dev.nozh.api.PerfSnapshot newSnapshot) {
        return rulesGovernor.evaluateOutcome(previousSnapshot, newSnapshot);
    }
}
