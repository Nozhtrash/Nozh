package dev.nozh.core.compatibility;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.api.capability.CapabilityContract;
import dev.nozh.api.capability.CapabilityContractDeclaration;
import dev.nozh.api.compat.StewardshipDeclaration;
import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.api.metrics.ModMetricsDeclaration;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.fabric.compat.CompatRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility matrix for external mod stewardship.
 */
public final class CompatibilityMatrix {

    private final ModConflictDetector conflictDetector;
    private final CompatRegistry compatRegistry;

    public CompatibilityMatrix() {
        this.conflictDetector = new ModConflictDetector();
        this.compatRegistry = new CompatRegistry();
    }

    public boolean isExternallyManaged(CapabilityId capability, ProviderMetadata metadata) {
        if (conflictDetector.isNozhSpecialty(capability)) {
            return false;
        }
        if (metadata != null && !metadata.conflictingMods().isEmpty()) {
            for (String mod : metadata.conflictingMods()) {
                if (isModLoaded(mod)) {
                    return true;
                }
            }
        }
        StewardshipDecision decision = getStewardshipDecision(capability);
        if (decision != null && decision.mode() == StewardshipMode.EXCLUSIVE) {
            return true;
        }
        if (compatRegistry.isExternallyManaged(capability)) {
            return true;
        }
        return conflictDetector.hasConflict(capability);
    }

    public String getSteward(CapabilityId capability) {
        StewardshipDecision decision = getStewardshipDecision(capability);
        if (decision != null && decision.mode() != null) {
            return decision.steward();
        }
        return "NOZH";
    }

    public boolean isBlockedByDependencies(ProviderMetadata metadata) {
        if (metadata == null) {
            return false;
        }
        Set<String> required = metadata.requiredMods();
        if (required == null || required.isEmpty()) {
            return false;
        }
        for (String mod : required) {
            if (!isModLoaded(mod)) {
                return true;
            }
        }
        return false;
    }

    public String getDependencySteward(ProviderMetadata metadata) {
        if (metadata == null || metadata.requiredMods().isEmpty()) {
            return "External Mod";
        }
        return String.join(", ", metadata.requiredMods());
    }

    private boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }

    public StewardshipDecision getStewardshipDecision(CapabilityId capability) {
        StewardshipDecision registryDecision = compatRegistry.getStewardshipDecision(capability);
        if (registryDecision != null && registryDecision.mode() != null) {
            return registryDecision;
        }
        return conflictDetector.getStewardshipDecision(capability);
    }

    public StewardshipDecision getProviderStewardshipDecision(CapabilityId capability, ProviderMetadata metadata) {
        if (metadata != null && !metadata.conflictingMods().isEmpty()) {
            for (String mod : metadata.conflictingMods()) {
                if (isModLoaded(mod)) {
                    return new StewardshipDecision(
                            capability,
                            mod,
                            StewardshipMode.EXCLUSIVE,
                            "Provider conflicts with " + mod);
                }
            }
        }
        return getStewardshipDecision(capability);
    }

    public List<StewardshipDecision> getStewardshipTraces() {
        List<StewardshipDecision> traces = StewardshipHandshakeRegistry.getTraces();
        for (CapabilityId capability : CapabilityId.values()) {
            StewardshipDecision decision = getStewardshipDecision(capability);
            if (decision == null) {
                continue;
            }
            boolean alreadyTracked = false;
            for (StewardshipDecision existing : traces) {
                if (existing.capability() == capability) {
                    alreadyTracked = true;
                    break;
                }
            }
            if (!alreadyTracked && decision.mode() != null && !decision.steward().equals("NOZH")) {
                traces.add(decision);
            }
        }
        return traces;
    }

    public List<CompatibilityAgreement> getAgreements() {
        Map<String, AgreementBuilder> builders = new HashMap<>();

        for (StewardshipDeclaration declaration : StewardshipHandshakeRegistry.getDeclarations()) {
            AgreementBuilder builder = builders.computeIfAbsent(
                    declaration.modId(),
                    key -> new AgreementBuilder(declaration.modId(), declaration.displayName()));
            builder.stewardshipReason = declaration.reason();
            builder.sharedCapabilities.addAll(declaration.sharedCapabilities());
            builder.exclusiveCapabilities.addAll(declaration.exclusiveCapabilities());
        }

        for (CapabilityContractDeclaration declaration : CapabilityContractRegistry.getDeclarations()) {
            AgreementBuilder builder = builders.computeIfAbsent(
                    declaration.modId(),
                    key -> new AgreementBuilder(declaration.modId(), declaration.displayName()));
            builder.contractNotes = declaration.notes();
            builder.contracts.addAll(declaration.contracts());
        }

        for (ModMetricsDeclaration declaration : ModMetricsRegistry.getDeclarations()) {
            AgreementBuilder builder = builders.computeIfAbsent(
                    declaration.modId(),
                    key -> new AgreementBuilder(declaration.modId(), declaration.displayName()));
            builder.metricsNotes = declaration.notes();
            builder.metrics.addAll(declaration.metrics());
            builder.metricCapabilities.addAll(declaration.capabilities());
        }

        List<CompatibilityAgreement> agreements = new ArrayList<>();
        for (AgreementBuilder builder : builders.values()) {
            agreements.add(builder.build());
        }
        return agreements;
    }

    private static final class AgreementBuilder {
        private final String modId;
        private final String displayName;
        private final EnumSet<CapabilityId> exclusiveCapabilities = EnumSet.noneOf(CapabilityId.class);
        private final EnumSet<CapabilityId> sharedCapabilities = EnumSet.noneOf(CapabilityId.class);
        private String stewardshipReason = "";
        private final List<CapabilityContract> contracts = new ArrayList<>();
        private String contractNotes = "";
        private final EnumSet<CapabilityId> metricCapabilities = EnumSet.noneOf(CapabilityId.class);
        private final List<ModMetricsDeclaration.MetricDefinition> metrics = new ArrayList<>();
        private String metricsNotes = "";

        private AgreementBuilder(String modId, String displayName) {
            this.modId = modId;
            this.displayName = displayName;
        }

        private CompatibilityAgreement build() {
            return new CompatibilityAgreement(
                    modId,
                    displayName,
                    EnumSet.copyOf(exclusiveCapabilities),
                    EnumSet.copyOf(sharedCapabilities),
                    stewardshipReason,
                    List.copyOf(contracts),
                    contractNotes,
                    EnumSet.copyOf(metricCapabilities),
                    List.copyOf(metrics),
                    metricsNotes);
        }
    }
}
