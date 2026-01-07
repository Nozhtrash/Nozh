package dev.nozh.core.config;

import dev.nozh.core.safety.CrashFailureContext;
import dev.nozh.core.safety.NozhState;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON parser/serializer for NOZH config and state.
 * Conservative approach: never throws, returns null on parse failure.
 * MVP-only - no external dependencies.
 */
public final class JsonMini {

    private JsonMini() {
    }

    // --- Serialization ---

    public static String toJson(NozhConfig c) {
        return "{\n" +
                "  \"configVersion\": " + c.configVersion + ",\n" +
                "  \"enabled\": " + c.enabled + ",\n" +
                "  \"debugLogs\": " + c.debugLogs + ",\n" +
                "  \"language\": \"" + (c.language == null ? "auto" : c.language) + "\",\n" +
                "  \"showHud\": " + c.showHud + ",\n" +
                "  \"showHudSuggestions\": " + c.showHudSuggestions + ",\n" +
                "  \"hudMode\": \"" + (c.hudMode == null ? "ANALYST" : c.hudMode) + "\",\n" +
                "  \"hudAnchor\": \"" + (c.hudAnchor == null ? "TOP_LEFT" : c.hudAnchor) + "\",\n" +
                "  \"hudOffsetX\": " + c.hudOffsetX + ",\n" +
                "  \"hudOffsetY\": " + c.hudOffsetY + ",\n" +
                "  \"hudScale\": " + c.hudScale + ",\n" +
                "  \"tutorialStep\": " + c.tutorialStep + ",\n" +

                "  \"targetFps\": " + c.targetFps + ",\n" +
                "  \"optimizationProfile\": \"" + (c.optimizationProfile == null ? "BALANCED" : c.optimizationProfile)
                + "\",\n" +
                "  \"reverseEpsilonMs\": " + c.reverseEpsilonMs + ",\n" +

                "  \"allowAutoTuning\": " + c.allowAutoTuning + ",\n" +
                "  \"allowGameplayImpactActions\": " + c.allowGameplayImpactActions + ",\n" +
                "  \"safeModeForce\": " + c.safeModeForce + ",\n" +
                "  \"rollbackEnabled\": " + c.rollbackEnabled + ",\n" +
                "  \"hybridModelEnabled\": " + c.hybridModelEnabled + ",\n" +

                "  \"rollbackWindowMillis\": " + c.rollbackWindowMillis + ",\n" +
                "  \"improvementEpsilonAvgMs\": " + c.improvementEpsilonAvgMs + ",\n" +
                "  \"improvementEpsilonP95Ms\": " + c.improvementEpsilonP95Ms + ",\n" +
                "  \"rollbackEvaluationTicks\": " + c.rollbackEvaluationTicks + ",\n" +
                "  \"rollbackCooldownMillis\": " + c.rollbackCooldownMillis + ",\n" +
                "  \"observationWindowSeconds\": " + c.observationWindowSeconds + ",\n" +
                "  \"hybridModelBlockConfidence\": " + c.hybridModelBlockConfidence + ",\n" +
                "  \"governorDecisionBudgetMs\": " + c.governorDecisionBudgetMs + ",\n" +
                "  \"banditExplorationRate\": " + c.banditExplorationRate + ",\n" +

                "  \"historyMaxEntries\": " + c.historyMaxEntries + ",\n" +
                "  \"historyCommandLimit\": " + c.historyCommandLimit + ",\n" +
                "  \"cooldownActionMillis\": " + c.cooldownActionMillis + ",\n" +
                "  \"cooldownGlobalMinIntervalMillis\": " + c.cooldownGlobalMinIntervalMillis + ",\n" +
                "  \"maxChangesPerSession\": " + c.maxChangesPerSession + ",\n" +
                "  \"evalPeriodTicks\": " + c.evalPeriodTicks + ",\n" +
                "  \"benchmarkModeEnabled\": " + c.benchmarkModeEnabled + ",\n" +
                "  \"benchmarkMicroIntervalMillis\": " + c.benchmarkMicroIntervalMillis + ",\n" +
                "  \"hardwareProfile\": \"" + (c.hardwareProfile == null ? "" : c.hardwareProfile) + "\",\n" +

                "  \"adaptiveVisualQualityEnabled\": " + c.adaptiveVisualQualityEnabled + ",\n" +
                "  \"adaptiveVisualQualitySensitivityMs\": " + c.adaptiveVisualQualitySensitivityMs + ",\n" +
                "  \"adaptiveVisualQualityMinStep\": " + c.adaptiveVisualQualityMinStep + ",\n" +
                "  \"adaptiveVisualQualityMaxStep\": " + c.adaptiveVisualQualityMaxStep + "\n" +
                "}\n";
    }

    public static String toJson(NozhState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"bootAttempts\": ").append(state.bootAttempts).append(",\n");
        sb.append("  \"lastCleanShutdown\": ").append(state.lastCleanShutdown).append(",\n");

        sb.append("  \"safeModeCauses\": [");
        boolean first = true;
        for (dev.nozh.core.safety.SafeModeCause cause : state.safeModeCauses) {
            if (!first)
                sb.append(", ");
            sb.append("\"").append(cause.name()).append("\"");
            first = false;
        }
        sb.append("],\n");

        sb.append("  \"sessionStable\": ").append(state.sessionStable).append(",\n");
        sb.append("  \"safeModeActivatedAt\": ").append(state.safeModeActivatedAt).append(",\n");
        sb.append("  \"sessionStartTime\": ").append(state.sessionStartTime).append(",\n");

        sb.append("  \"lastFailureContext\": ");
        if (state.lastFailureContext == null) {
            sb.append("null,\n");
        } else {
            CrashFailureContext ctx = state.lastFailureContext;
            sb.append("{ ");
            sb.append("\"timestamp\": ").append(ctx.timestamp()).append(", ");
            sb.append("\"source\": \"").append(ctx.source() == null ? "" : ctx.source()).append("\", ");
            sb.append("\"capabilityId\": \"").append(ctx.capabilityId() == null ? "" : ctx.capabilityId())
                    .append("\", ");
            sb.append("\"commandType\": \"").append(ctx.commandType() == null ? "" : ctx.commandType()).append("\", ");
            sb.append("\"requestedValue\": \"").append(ctx.requestedValue() == null ? "" : ctx.requestedValue())
                    .append("\", ");
            sb.append("\"errorMessage\": \"").append(ctx.errorMessage() == null ? "" : ctx.errorMessage())
                    .append("\", ");
            sb.append("\"exceptionType\": \"").append(ctx.exceptionType() == null ? "" : ctx.exceptionType())
                    .append("\"");
            sb.append(" },\n");
        }

        sb.append("  \"quarantinedCapabilities\": [\n");
        boolean firstQuarantine = true;
        for (var entry : state.quarantinedCapabilities.entrySet()) {
            if (!firstQuarantine) {
                sb.append(",\n");
            }
            sb.append("    { ");
            sb.append("\"id\": \"").append(entry.getKey().name()).append("\", ");
            sb.append("\"retryAt\": ").append(entry.getValue());
            sb.append(" }");
            firstQuarantine = false;
        }
        sb.append("\n  ],\n");

        sb.append("  \"executionHistory\": [\n");
        java.util.List<dev.nozh.core.executor.ExecutedAction> history = state.executionHistory;
        for (int i = 0; i < history.size(); i++) {
            dev.nozh.core.executor.ExecutedAction action = history.get(i);
            sb.append("    { ");
            sb.append("\"timestamp\": ").append(action.timestamp()).append(", ");
            sb.append("\"type\": \"").append(action.type()).append("\", ");
            sb.append("\"oldValue\": \"").append(action.oldValue()).append("\", ");
            sb.append("\"newValue\": \"").append(action.newValue()).append("\"");
            sb.append(" }");
            if (i < history.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");

        sb.append("}");
        return sb.toString();
    }

    // --- Parsing ---

    public static NozhConfig fromJsonNozhConfig(String json) {
        NozhConfig c = new NozhConfig();
        if (json == null || json.isEmpty())
            return c;

        try {
            // Version tracking
            c.configVersion = getInt(json, "configVersion", c.configVersion);

            // Core
            c.enabled = getBool(json, "enabled", c.enabled);
            // Migration: debug -> debugLogs
            c.debugLogs = getBool(json, "debugLogs", getBool(json, "debug", c.debugLogs));
            c.language = getString(json, "language", c.language);
            c.showHud = getBool(json, "showHud", c.showHud);
            c.showHudSuggestions = getBool(json, "showHudSuggestions", c.showHudSuggestions);
            c.hudMode = getString(json, "hudMode", c.hudMode);
            c.hudAnchor = getString(json, "hudAnchor", c.hudAnchor);
            c.hudOffsetX = getInt(json, "hudOffsetX", c.hudOffsetX);
            c.hudOffsetY = getInt(json, "hudOffsetY", c.hudOffsetY);
            c.hudScale = getDouble(json, "hudScale", c.hudScale);
            c.tutorialStep = getInt(json, "tutorialStep", c.tutorialStep);

            // Targets
            c.targetFps = getInt(json, "targetFps", c.targetFps);
            c.optimizationProfile = getString(json, "optimizationProfile", c.optimizationProfile);
            c.reverseEpsilonMs = getDouble(json, "reverseEpsilonMs", c.reverseEpsilonMs);

            // Toggles
            c.allowAutoTuning = getBool(json, "allowAutoTuning", c.allowAutoTuning);
            c.allowGameplayImpactActions = getBool(json, "allowGameplayImpactActions", c.allowGameplayImpactActions);
            c.safeModeForce = getBool(json, "safeModeForce", c.safeModeForce);
            c.rollbackEnabled = getBool(json, "rollbackEnabled", c.rollbackEnabled);
            c.hybridModelEnabled = getBool(json, "hybridModelEnabled", c.hybridModelEnabled);

            // Rollback Tuning
            c.rollbackWindowMillis = getInt(json, "rollbackWindowMillis", c.rollbackWindowMillis);
            c.improvementEpsilonAvgMs = getDouble(json, "improvementEpsilonAvgMs", c.improvementEpsilonAvgMs);
            c.improvementEpsilonP95Ms = getDouble(json, "improvementEpsilonP95Ms", c.improvementEpsilonP95Ms);
            c.rollbackEvaluationTicks = getInt(json, "rollbackEvaluationTicks", c.rollbackEvaluationTicks);
            c.rollbackCooldownMillis = getInt(json, "rollbackCooldownMillis", c.rollbackCooldownMillis);
            c.observationWindowSeconds = getInt(json, "observationWindowSeconds", c.observationWindowSeconds);
            c.hybridModelBlockConfidence = getDouble(json, "hybridModelBlockConfidence",
                    c.hybridModelBlockConfidence);
            c.governorDecisionBudgetMs = getInt(json, "governorDecisionBudgetMs", c.governorDecisionBudgetMs);
            c.banditExplorationRate = getDouble(json, "banditExplorationRate", c.banditExplorationRate);

            // Limits
            c.historyMaxEntries = getInt(json, "historyMaxEntries", c.historyMaxEntries);
            c.historyCommandLimit = getInt(json, "historyCommandLimit", c.historyCommandLimit);

            // Cooldowns (Migration: Seconds * 1000 if old key found)
            int oldActionCooldown = getInt(json, "actionCooldownSeconds", -1);
            if (oldActionCooldown != -1 && !json.contains("\"cooldownActionMillis\"")) {
                c.cooldownActionMillis = oldActionCooldown * 1000;
            } else {
                c.cooldownActionMillis = getInt(json, "cooldownActionMillis", c.cooldownActionMillis);
            }

            int oldGlobalCooldown = getInt(json, "minSecondsBetweenChanges", -1);
            if (oldGlobalCooldown != -1 && !json.contains("\"cooldownGlobalMinIntervalMillis\"")) {
                c.cooldownGlobalMinIntervalMillis = oldGlobalCooldown * 1000;
            } else {
                c.cooldownGlobalMinIntervalMillis = getInt(json, "cooldownGlobalMinIntervalMillis",
                        c.cooldownGlobalMinIntervalMillis);
            }

            c.maxChangesPerSession = getInt(json, "maxChangesPerSession", c.maxChangesPerSession);
            c.evalPeriodTicks = getInt(json, "evalPeriodTicks", c.evalPeriodTicks);
            c.benchmarkModeEnabled = getBool(json, "benchmarkModeEnabled", c.benchmarkModeEnabled);
            c.benchmarkMicroIntervalMillis = getInt(json, "benchmarkMicroIntervalMillis",
                    c.benchmarkMicroIntervalMillis);
            c.hardwareProfile = getString(json, "hardwareProfile", c.hardwareProfile);
            c.adaptiveVisualQualityEnabled = getBool(json, "adaptiveVisualQualityEnabled",
                    c.adaptiveVisualQualityEnabled);
            c.adaptiveVisualQualitySensitivityMs = getDouble(json, "adaptiveVisualQualitySensitivityMs",
                    c.adaptiveVisualQualitySensitivityMs);
            c.adaptiveVisualQualityMinStep = getInt(json, "adaptiveVisualQualityMinStep",
                    c.adaptiveVisualQualityMinStep);
            c.adaptiveVisualQualityMaxStep = getInt(json, "adaptiveVisualQualityMaxStep",
                    c.adaptiveVisualQualityMaxStep);

        } catch (Exception e) {
            // Log if possible, otherwise rely on validate() default
        }

        return c;
    }

    public static NozhState fromJsonNozhState(String json) {
        try {
            NozhState s = new NozhState();
            if (json == null || json.isEmpty())
                return s;

            s.bootAttempts = getInt(json, "bootAttempts", s.bootAttempts);
            s.lastCleanShutdown = getLong(json, "lastCleanShutdown", s.lastCleanShutdown);

            if (json.contains("\"safeModeCauses\"")) {
                String causesStr = getRawString(json, "safeModeCauses"); // Should be string or need specialized array
                                                                         // parser?
                // Wait, getRawString extracts "value", but array is [ ... ].
                // Use getArrayBlock for array.
                String sub = getArrayBlock(json, "safeModeCauses");
                if (sub != null)
                    parseSafeModeCauses(sub, s);
            } else {
                // Migration
                boolean userEnabled = getBool(json, "safeModeUserEnabled", false);
                boolean crashLoop = getBool(json, "safeModeCrashLoop", false);
                boolean configForce = getBool(json, "safeModeForceConfig", false);
                if (userEnabled)
                    s.safeModeCauses.add(dev.nozh.core.safety.SafeModeCause.USER_ENABLED);
                if (crashLoop)
                    s.safeModeCauses.add(dev.nozh.core.safety.SafeModeCause.CRASH_LOOP);
                if (configForce)
                    s.safeModeCauses.add(dev.nozh.core.safety.SafeModeCause.CONFIG_FORCE);
                if (json.contains("\"safeMode\"") && s.safeModeCauses.isEmpty()) {
                    if (getBool(json, "safeMode", false))
                        s.safeModeCauses.add(dev.nozh.core.safety.SafeModeCause.CRASH_LOOP);
                }
            }

            s.sessionStable = getBool(json, "sessionStable", s.sessionStable);
            s.safeModeActivatedAt = getLong(json, "safeModeActivatedAt", s.safeModeActivatedAt);
            s.sessionStartTime = getLong(json, "sessionStartTime", s.sessionStartTime);

            String failureBlock = getObjectBlock(json, "lastFailureContext");
            if (failureBlock != null) {
                CrashFailureContext context = parseFailureContext(failureBlock);
                if (context != null) {
                    s.lastFailureContext = context;
                }
            }

            String quarantineBlock = getArrayBlock(json, "quarantinedCapabilities");
            if (quarantineBlock != null) {
                parseQuarantinedCapabilities(quarantineBlock, s);
            }

            String historyBlock = getArrayBlock(json, "executionHistory");
            if (historyBlock != null) {
                parseExecutionHistory(historyBlock, s);
            }

            return s;
        } catch (Exception e) {
            return new NozhState();
        }
    }

    // --- Helpers ---

    private static void parseSafeModeCauses(String arrayStr, NozhState state) {
        if (arrayStr == null || arrayStr.isBlank())
            return;
        String cleaned = arrayStr.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (cleaned.isEmpty())
            return;
        for (String cause : cleaned.split(",")) {
            try {
                if (!cause.trim().isEmpty())
                    state.safeModeCauses.add(dev.nozh.core.safety.SafeModeCause.valueOf(cause.trim()));
            } catch (Exception ignored) {
            }
        }
    }

    private static void parseExecutionHistory(String block, NozhState state) {
        if (block.length() < 2)
            return;
        String content = block.substring(1, block.length() - 1);
        int depth = 0;
        int start = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (depth == 0)
                    start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    String objJson = content.substring(start, i + 1);
                    dev.nozh.core.executor.ExecutedAction action = parseActionObject(objJson);
                    if (action != null)
                        state.executionHistory.add(action);
                    start = -1;
                }
            }
        }
    }

    private static CrashFailureContext parseFailureContext(String json) {
        if (json == null || json.isBlank() || json.contains(": null")) {
            return null;
        }
        long timestamp = getLong(json, "timestamp", 0L);
        String source = getRawString(json, "source");
        String capabilityId = getRawString(json, "capabilityId");
        String commandType = getRawString(json, "commandType");
        String requestedValue = getRawString(json, "requestedValue");
        String errorMessage = getRawString(json, "errorMessage");
        String exceptionType = getRawString(json, "exceptionType");
        if (timestamp == 0L && source == null && capabilityId == null) {
            return null;
        }
        return new CrashFailureContext(
                timestamp,
                source,
                capabilityId,
                commandType,
                requestedValue,
                errorMessage,
                exceptionType);
    }

    private static void parseQuarantinedCapabilities(String block, NozhState state) {
        if (block.length() < 2) {
            return;
        }
        String content = block.substring(1, block.length() - 1);
        int depth = 0;
        int start = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    String objJson = content.substring(start, i + 1);
                    String id = getRawString(objJson, "id");
                    long retryAt = getLong(objJson, "retryAt", 0L);
                    if (id != null && retryAt > 0L) {
                        try {
                            dev.nozh.core.bus.CapabilityId capabilityId = dev.nozh.core.bus.CapabilityId.valueOf(id);
                            state.quarantinedCapabilities.put(capabilityId, retryAt);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    start = -1;
                }
            }
        }
    }

    private static dev.nozh.core.executor.ExecutedAction parseActionObject(String json) {
        try {
            long ts = getLong(json, "timestamp", 0);
            String typeStr = getRawString(json, "type");
            String oldVal = getRawString(json, "oldValue");
            String newVal = getRawString(json, "newValue");
            if (ts == 0 || typeStr == null)
                return null;
            return new dev.nozh.core.executor.ExecutedAction(
                    ts, dev.nozh.api.governor.ActionType.valueOf(typeStr), oldVal, newVal);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean getBool(String json, String key, boolean def) {
        String v = getRaw(json, key);
        if (v == null)
            return def;
        if ("true".equalsIgnoreCase(v))
            return true;
        if ("false".equalsIgnoreCase(v))
            return false;
        return def;
    }

    private static int getInt(String json, String key, int def) {
        String v = getRaw(json, key);
        if (v == null)
            return def;
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }

    private static long getLong(String json, String key, long def) {
        String v = getRaw(json, key);
        if (v == null)
            return def;
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return def;
        }
    }

    // New helper
    private static double getDouble(String json, String key, double def) {
        String v = getRaw(json, key);
        if (v == null)
            return def;
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return def;
        }
    }

    // New helper
    private static String getString(String json, String key, String def) {
        String v = getRawString(json, key);
        return v != null ? v : def;
    }

    private static String getRaw(String json, String key) {
        // Simple regex, matches "key": value
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false|-?[0-9.]+)");
        Matcher m = p.matcher(json);
        if (m.find())
            return m.group(1);
        return null;
    }

    private static String getRawString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find())
            return m.group(1);
        return null;
    }

    private static String getArrayBlock(String json, String key) {
        int startIndex = json.indexOf("\"" + key + "\"");
        if (startIndex == -1)
            return null;
        int openBracket = json.indexOf("[", startIndex);
        if (openBracket == -1)
            return null;

        int depth = 0;
        for (int i = openBracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[')
                depth++;
            if (c == ']') {
                depth--;
                if (depth == 0)
                    return json.substring(openBracket, i + 1);
            }
        }
        return null;
    }

    private static String getObjectBlock(String json, String key) {
        int startIndex = json.indexOf("\"" + key + "\"");
        if (startIndex == -1) {
            return null;
        }
        int colonIndex = json.indexOf(":", startIndex);
        if (colonIndex != -1) {
            int cursor = colonIndex + 1;
            while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                cursor++;
            }
            if (json.startsWith("null", cursor)) {
                return null;
            }
        }
        int openBrace = json.indexOf("{", startIndex);
        if (openBrace == -1) {
            return null;
        }

        int depth = 0;
        for (int i = openBrace; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(openBrace, i + 1);
                }
            }
        }
        return null;
    }
}
