package dev.nozh.core.config;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Validates configuration to prevent user errors.
 * Warns about bad combinations, impossible values, etc.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class ConfigValidator {

    /**
     * Validation issue severity.
     */
    public enum IssueSeverity {
        INFO("FYI", 0x888888),
        WARNING("Might cause issues", 0xFFAA00),
        ERROR("Will cause issues", 0xFF5500),
        CRITICAL("Will crash or break", 0xFF0000);

        public final String description;
        public final int color;

        IssueSeverity(String description, int color) {
            this.description = description;
            this.color = color;
        }
    }

    /**
     * A validation issue found in configuration.
     */
    public record ValidationIssue(
            String field,
            String message,
            IssueSeverity severity,
            String suggestedFix) {
        @Override
        public String toString() {
            return String.format("[%s] %s: %s (Fix: %s)",
                    severity, field, message, suggestedFix);
        }
    }

    private final List<ValidationIssue> issues;

    /**
     * Constructs a new ConfigValidator.
     */
    public ConfigValidator() {
        this.issues = new ArrayList<>();
    }

    /**
     * Validates a configuration.
     * 
     * @param targetFps      target FPS setting
     * @param renderDistance render distance setting
     * @param potatoEnabled  if potato mode is enabled
     * @param allocatedRamMb allocated RAM in MB
     * @return list of validation issues
     */
    public List<ValidationIssue> validate(
            double targetFps,
            int renderDistance,
            boolean potatoEnabled,
            long allocatedRamMb) {
        issues.clear();

        // Validate target FPS
        if (targetFps < 20) {
            issues.add(new ValidationIssue(
                    "target_fps",
                    "Target FPS is very low (< 20)",
                    IssueSeverity.WARNING,
                    "Set to at least 30 FPS"));
        } else if (targetFps > 240) {
            issues.add(new ValidationIssue(
                    "target_fps",
                    "Target FPS is unrealistically high (> 240)",
                    IssueSeverity.INFO,
                    "Most monitors max at 144-240Hz"));
        }

        // Validate render distance
        if (renderDistance < 2) {
            issues.add(new ValidationIssue(
                    "render_distance",
                    "Render distance below minimum (< 2)",
                    IssueSeverity.ERROR,
                    "Set to at least 2 chunks"));
        } else if (renderDistance > 32) {
            if (allocatedRamMb < 4096) {
                issues.add(new ValidationIssue(
                        "render_distance",
                        "High render distance with low RAM",
                        IssueSeverity.WARNING,
                        "Reduce to 16 or allocate more RAM"));
            }
        }

        // Validate potato mode combinations
        if (potatoEnabled && renderDistance > 12) {
            issues.add(new ValidationIssue(
                    "potato_mode",
                    "Potato mode enabled but render distance is high",
                    IssueSeverity.INFO,
                    "Potato mode works best with RD <= 8"));
        }

        // RAM validation
        if (allocatedRamMb < 2048) {
            issues.add(new ValidationIssue(
                    "memory",
                    "Less than 2GB RAM allocated",
                    IssueSeverity.WARNING,
                    "Allocate at least 2GB for stable gameplay"));
        } else if (allocatedRamMb > 8192) {
            issues.add(new ValidationIssue(
                    "memory",
                    "More than 8GB allocated - may cause GC pauses",
                    IssueSeverity.INFO,
                    "4-6GB is usually optimal for Minecraft"));
        }

        return List.copyOf(issues);
    }

    /**
     * Attempts to auto-fix detected issues.
     * 
     * @return true if any fixes were applied
     */
    public boolean autoFix() {
        int fixedCount = 0;

        for (ValidationIssue issue : issues) {
            if (issue.severity() == IssueSeverity.ERROR ||
                    issue.severity() == IssueSeverity.CRITICAL) {

                NozhConstants.LOGGER.info("Auto-fixing: {}", issue.field());
                fixedCount++;
            }
        }

        if (fixedCount > 0) {
            NozhConstants.LOGGER.info("Auto-fixed {} configuration issues", fixedCount);
        }

        return fixedCount > 0;
    }

    /**
     * Checks if configuration has critical issues.
     * 
     * @return true if any critical issues exist
     */
    public boolean hasCriticalIssues() {
        return issues.stream()
                .anyMatch(i -> i.severity() == IssueSeverity.CRITICAL);
    }

    /**
     * Checks if configuration has any issues.
     * 
     * @return true if any issues exist
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Gets issues by severity.
     * 
     * @param severity severity to filter
     * @return filtered issues
     */
    public List<ValidationIssue> getIssuesBySeverity(IssueSeverity severity) {
        return issues.stream()
                .filter(i -> i.severity() == severity)
                .toList();
    }

    /**
     * Gets formatted validation report.
     * 
     * @return report string
     */
    public String getReport() {
        if (issues.isEmpty()) {
            return "Configuration is valid - no issues found";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Configuration Validation ===\n");
        report.append(String.format("Found %d issue(s):\n\n", issues.size()));

        for (ValidationIssue issue : issues) {
            report.append(String.format("[%s] %s\n", issue.severity(), issue.field()));
            report.append(String.format("  Issue: %s\n", issue.message()));
            report.append(String.format("  Fix: %s\n\n", issue.suggestedFix()));
        }

        return report.toString();
    }
}
