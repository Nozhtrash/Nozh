package dev.nozh.core.config;

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
                "  \"hudAnchor\": \"" + (c.hudAnchor == null ? "TOP_LEFT" : c.hudAnchor) + "\",\n" +
                "  \"hudOffsetX\": " + c.hudOffsetX + ",\n" +
                "  \"hudOffsetY\": " + c.hudOffsetY + ",\n" +

                "  \"targetFps\": " + c.targetFps + ",\n" +

                "  \"allowAutoTuning\": " + c.allowAutoTuning + ",\n" +
                "  \"allowGameplayImpactActions\": " + c.allowGameplayImpactActions + ",\n" +
                "  \"safeModeForce\": " + c.safeModeForce + ",\n" +
                "  \"rollbackEnabled\": " + c.rollbackEnabled + ",\n" +

                "  \"rollbackWindowMillis\": " + c.rollbackWindowMillis + ",\n" +
                "  \"improvementEpsilonAvgMs\": " + c.improvementEpsilonAvgMs + ",\n" +
                "  \"improvementEpsilonP95Ms\": " + c.improvementEpsilonP95Ms + ",\n" +

                "  \"historyMaxEntries\": " + c.historyMaxEntries + ",\n" +
                "  \"historyCommandLimit\": " + c.historyCommandLimit + ",\n" +
                "  \"cooldownActionMillis\": " + c.cooldownActionMillis + ",\n" +
                "  \"cooldownGlobalMinIntervalMillis\": " + c.cooldownGlobalMinIntervalMillis + ",\n" +
                "  \"maxChangesPerSession\": " + c.maxChangesPerSession + ",\n" +
                "  \"evalPeriodTicks\": " + c.evalPeriodTicks + "\n" +
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
            c.hudAnchor = getString(json, "hudAnchor", c.hudAnchor);
            c.hudOffsetX = getInt(json, "hudOffsetX", c.hudOffsetX);
            c.hudOffsetY = getInt(json, "hudOffsetY", c.hudOffsetY);

            // Targets
            c.targetFps = getInt(json, "targetFps", c.targetFps);

            // Toggles
            c.allowAutoTuning = getBool(json, "allowAutoTuning", c.allowAutoTuning);
            c.allowGameplayImpactActions = getBool(json, "allowGameplayImpactActions", c.allowGameplayImpactActions);
            c.safeModeForce = getBool(json, "safeModeForce", c.safeModeForce);
            c.rollbackEnabled = getBool(json, "rollbackEnabled", c.rollbackEnabled);

            // Rollback Tuning
            c.rollbackWindowMillis = getInt(json, "rollbackWindowMillis", c.rollbackWindowMillis);
            c.improvementEpsilonAvgMs = getDouble(json, "improvementEpsilonAvgMs", c.improvementEpsilonAvgMs);
            c.improvementEpsilonP95Ms = getDouble(json, "improvementEpsilonP95Ms", c.improvementEpsilonP95Ms);

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
}
