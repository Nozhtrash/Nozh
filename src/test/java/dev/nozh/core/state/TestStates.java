package dev.nozh.core.state;

public final class TestStates {

    private TestStates() {
    }

    public static RuntimeState autoTuningEnabled() {
        RuntimeState state = RuntimeState.defaults();
        return new RuntimeState(
                state.enabled(),
                state.safeMode(),
                true,
                state.debugLogs(),
                state.governorDisabled(),
                state.governorCooldownActive(),
                state.governorLastActionTimestamp(),
                state.benchmarkRunning(),
                state.benchmarkValidity(),
                state.benchmarkStartTimestamp(),
                state.pendingAction(),
                state.suggestedActions(),
                state.pendingActionsCount(),
                state.executionHistorySize(),
                state.lastSnapshotHistorySize(),
                state.actionHistory(),
                state.sessionChangesCount(),
                state.avgFrametimeMs(),
                state.p95FrametimeMs(),
                state.p99FrametimeMs(),
                state.frametimeStddevMs(),
                state.tickTimeAvg(),
                state.tickTimeP95(),
                state.spikeCount(),
                state.lastDecisionReason(),
                state.lastDecisionTimestamp(),
                state.sessionStartTime(),
                state.stateVersion(),
                state.currentScenario(),
                state.scenarioConfidence(),
                state.lastScenarioChangeTimestamp(),
                state.scenarioChangeCount(),
                state.rapidScenarioChangeCount(),
                state.combatAfkFlipCount(),
                state.baselineSettings(),
                state.currentSettings());
    }
}
