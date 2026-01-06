package dev.nozh.core.preset;

import java.util.LinkedHashMap;
import java.util.Map;

final class ProfileTokenParser {

    private ProfileTokenParser() {
    }

    static Map<String, String> parse(String profile) {
        Map<String, String> tokens = new LinkedHashMap<>();
        if (profile == null || profile.isBlank()) {
            return tokens;
        }
        String[] parts = profile.split(";");
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String value = kv[1].trim();
            if (!key.isBlank()) {
                tokens.put(key, value);
            }
        }
        return tokens;
    }
}
