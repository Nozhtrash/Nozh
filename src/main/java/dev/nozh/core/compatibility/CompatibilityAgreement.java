package dev.nozh.core.compatibility;

import dev.nozh.api.capability.CapabilityContract;
import dev.nozh.api.metrics.ModMetricsDeclaration;
import dev.nozh.core.capability.CapabilityId;

import java.util.EnumSet;
import java.util.List;

public record CompatibilityAgreement(
        String modId,
        String displayName,
        EnumSet<CapabilityId> exclusiveCapabilities,
        EnumSet<CapabilityId> sharedCapabilities,
        String stewardshipReason,
        List<CapabilityContract> contracts,
        String contractNotes,
        EnumSet<CapabilityId> metricCapabilities,
        List<ModMetricsDeclaration.MetricDefinition> metrics,
        String metricsNotes) {
}
