package dev.nozh.core.telemetry;

import dev.nozh.core.context.Scenario;
import dev.nozh.core.capability.CapabilityId;
import java.util.*;

/**
 * Live dashboard data for real-time monitoring.
 * Provides snapshot of current system state for display.
 */
public final class TelemetryDashboard {
    private final TelemetryAggregator aggregator;
    private final DecisionTracker decisionTracker;
    private final CapabilityEffectivenessTracker effectivenessTracker;
    private final EventTimeline timeline;
    
    public TelemetryDashboard(
        TelemetryAggregator aggregator,
        DecisionTracker decisionTracker,
        CapabilityEffectivenessTracker effectivenessTracker,
        EventTimeline timeline
    ) {
        this.aggregator = aggregator;
        this.decisionTracker = decisionTracker;
        this.effectivenessTracker = effectivenessTracker;
        this.timeline = timeline;
    }
    
    public DashboardSnapshot createSnapshot() {
        var global = aggregator.getGlobalMetrics();
        var byScenario = aggregator.getMetricsByScenario();
        var recentDecisions = decisionTracker.getRecentDecisions(10);
        var topCapabilities = effectivenessTracker.getRankings().stream()
            .limit(5)
            .toList();
        var recentEvents = timeline.getRecent(20);
        
        return new DashboardSnapshot(
            System.currentTimeMillis(),
            global,
            byScenario,
            recentDecisions,
            topCapabilities,
            recentEvents,
            decisionTracker.getTotalDecisions(),
            decisionTracker.getAverageImpact().orElse(0.0)
        );
    }
    
    public record DashboardSnapshot(
        long timestamp,
        TelemetryAggregator.GlobalMetrics globalMetrics,
        Map<Scenario, TelemetryAggregator.ScenarioMetrics> scenarioMetrics,
        List<DecisionTracker.DecisionRecord> recentDecisions,
        List<CapabilityEffectivenessTracker.CapabilityRanking> topCapabilities,
        List<EventTimeline.Event> recentEvents,
        int totalDecisions,
        double avgDecisionImpact
    ) {}
}