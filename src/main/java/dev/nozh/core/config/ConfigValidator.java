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
    /**
     * Validates a configuration object.
     * 
     * @param config the configuration to validate
     * @return list of validation issues
     */
    public List<ValidationIssue> validate(NozhConfig config) {
        issues.clear();

        if (config == null) {
            issues.add(new ValidationIssue("config", "Config is null", IssueSeverity.CRITICAL, "Recreate config"));
            return List.copyOf(issues);
        }

        // Validate target FPS
        if (config.targetFps < 20) {
            issues.add(new ValidationIssue(
                    "target_fps",
                    "Target FPS is very low (< 20)",
                    IssueSeverity.WARNING,
                    "Set to at least 30 FPS"));
        } else if (config.targetFps > 240) {
            issues.add(new ValidationIssue(
                    "target_fps",
                    "Target FPS is unrealistically high (> 240)",
                    IssueSeverity.INFO,
                    "Most monitors max at 144-240Hz"));
        }

        // Validate potato mode combinations
        boolean potatoEnabled = "POTATO".equalsIgnoreCase(config.optimizationProfile); // Implicit check
        // Render distance isn't directly in NozhConfig yet (except via preset override
        // logic),
        // effectively we might not be able to validate it unless it becomes a core
        // field.
        // For now we skip render distance checks or assume defaults.

        if (potatoEnabled && !config.adaptiveVisualQualityEnabled) {
            issues.add(new ValidationIssue(
                    "potato_mode",
                    "Potato mode enabled but Adaptive Quality is OFF",
                    IssueSeverity.WARNING,
                    "Enable Adaptive Quality for best results"));
        }

        // Check for safe mode conflicts
        if (config.safeModeForce && config.targetFps > 60) {
            issues.add(new ValidationIssue(
                    "safe_mode",
                    "Safe Mode is ON but Target FPS > 60",
                    IssueSeverity.WARNING,
                    "Cap FPS to 60 for stability"));
        }

        // Check headless
        if (java.awt.GraphicsEnvironment.isHeadless() && config.showHud) {
            issues.add(new ValidationIssue(
                    "headless",
                    "HUD enabled in headless environment",
                    IssueSeverity.ERROR,
                    "Disable HUD"));
        }

        return List.copyOf(issues);
    }

    /**
     * Attempts to auto-fix detected issues.
     * 
     * @return true if any fixes were applied
     */
    /**
     * Attempts to auto-fix detected issues in the provided config.
     * 
     * @param config config to fix
     * @return true if any fixes were applied
     */
    public boolean autoFix(NozhConfig config) {
        if (config == null)
            return false;
        int fixedCount = 0;

        for (ValidationIssue issue : issues) {
            if (issue.severity() == IssueSeverity.ERROR ||
                    issue.severity() == IssueSeverity.CRITICAL ||
                    (issue.severity() == IssueSeverity.WARNING && "safe_mode".equals(issue.field()))) {

                NozhConstants.LOGGER.info("Auto-fixing: {}", issue.field());

                // Logic to apply specific fixes
                if ("headless".equals(issue.field())) {
                    config.showHud = false;
                    fixedCount++;
                } else if ("safe_mode".equals(issue.field())) {
                    config.targetFps = 60;
                    fixedCount++;
                } else if ("potato_mode".equals(issue.field())) {
                    config.adaptiveVisualQualityEnabled = true;
                    fixedCount++;
                }
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
