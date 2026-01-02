package dev.nozh.core.hardening;

import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.telemetry.TelemetryBuffer;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.preset.PresetRegistry;
import dev.nozh.core.issues.Issue;
import dev.nozh.core.issues.IssueType;
import dev.nozh.core.issues.IssueSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Startup selfcheck (Release Hardening).
 * 
 * Validates critical system components at startup.
 * Failure → SafeMode + CRITICAL issue.
 * 
 * PURE - returns diagnostic results, doesn't modify state.
 */
public final class StartupSelfcheck {

    /**
     * Run selfcheck on critical components.
     * 
     * @param registry   Provider registry to check
     * @param telemetry  Telemetry buffer to check
     * @param stateStore State store to check
     * @param preset     Current preset tier
     * @return List of issues (empty if all OK)
     */
    public static List<Issue> run(
            ProviderRegistry registry,
            TelemetryBuffer telemetry,
            StateStore stateStore,
            HardwareTier preset) {
        List<Issue> issues = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Check providers registered
        if (registry == null || registry.getAllProviders().isEmpty()) {
            issues.add(Issue.create(
                    IssueType.PROVIDER_BROKEN,
                    IssueSeverity.CRITICAL,
                    "nozh.selfcheck.providers.empty",
                    now));
        }

        // Check telemetry buffer alive
        if (telemetry == null) {
            issues.add(Issue.create(
                    IssueType.TELEMETRY_STARVATION,
                    IssueSeverity.CRITICAL,
                    "nozh.selfcheck.telemetry.null",
                    now));
        }

        // Check state store
        if (stateStore == null) {
            issues.add(Issue.create(
                    IssueType.UNKNOWN,
                    IssueSeverity.CRITICAL,
                    "nozh.selfcheck.state.null",
                    now));
        } else {
            // Check state invariants
            RuntimeState state = stateStore.snapshotSafe();
            // Simple validation: state should exist
            if (state == null) {
                issues.add(Issue.create(
                        IssueType.UNKNOWN,
                        IssueSeverity.CRITICAL,
                        "nozh.selfcheck.state.invalid",
                        now));
            }
        }

        // Check preset loaded
        if (preset == null) {
            issues.add(Issue.create(
                    IssueType.UNKNOWN,
                    IssueSeverity.WARNING,
                    "nozh.selfcheck.preset.null",
                    now));
        } else {
            try {
                PresetRegistry.get(preset);
            } catch (Exception e) {
                issues.add(Issue.create(
                        IssueType.UNKNOWN,
                        IssueSeverity.CRITICAL,
                        "nozh.selfcheck.preset.invalid",
                        now));
            }
        }

        return issues;
    }

    private StartupSelfcheck() {
        // Static utility
    }
}
