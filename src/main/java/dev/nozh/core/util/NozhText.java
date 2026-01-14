package dev.nozh.core.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Centralized text generation for NOZH.
 * 
 * Phase 2 Iteration 1: Single point of entry for all user-facing text.
 * Designed for i18n even though not implemented yet - NO text should bypass
 * this.
 * 
 * CRITICAL RULE: All NOZH messages MUST go through NozhText, not direct
 * Text.literal()
 * 
 * Future-ready pattern:
 * - Keys can be replaced with translation lookups
 * - Global style changes happen in one place
 * - Consistency is enforced by design
 */
public final class NozhText {

    private NozhText() {
    }

    // Color scheme (centralized)
    private static final Formatting HEADER = Formatting.GOLD;
    private static final Formatting LABEL = Formatting.GRAY;
    private static final Formatting VALUE_GOOD = Formatting.GREEN;
    private static final Formatting VALUE_BAD = Formatting.RED;
    private static final Formatting VALUE_WARN = Formatting.YELLOW;
    private static final Formatting VALUE_NORMAL = Formatting.WHITE;

    /**
     * Info message: "NOZH: {message}"
     * Usage: NozhText.info("Safe mode reset")
     */
    public static MutableText info(String message) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.literal(message).formatted(VALUE_NORMAL));
    }

    /**
     * Success message: "NOZH: {message}" (green)
     * Usage: NozhText.success("Config validated successfully")
     */
    public static MutableText success(String message) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.literal(message).formatted(VALUE_GOOD));
    }

    /**
     * Warning message: "NOZH: {message}" (yellow)
     * Usage: NozhText.warning("Config had invalid values, corrected")
     */
    public static MutableText warning(String message) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.literal(message).formatted(VALUE_WARN));
    }

    /**
     * Error message: "NOZH: {message}" (red)
     * Usage: NozhText.error("Failed to load config")
     */
    public static MutableText error(String message) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.literal(message).formatted(VALUE_BAD));
    }

    /**
     * Header text (gold, for section titles)
     * Usage: NozhText.header("=== NOZH Status ===")
     */
    public static MutableText header(String text) {
        return Text.literal(text).formatted(HEADER);
    }

    /**
     * Label-value pair (gray label + colored value)
     * Usage: NozhText.labeled("Safe Mode", "Active", false)
     */
    public static MutableText labeled(String label, String value, boolean isGood) {
        return Text.literal(label + ": ").formatted(LABEL)
                .append(Text.literal(value).formatted(isGood ? VALUE_GOOD : VALUE_BAD));
    }

    /**
     * Label-value pair (neutral white value)
     * Usage: NozhText.labeled("Version", "0.1.0")
     */
    /**
     * Label-value pair (neutral white value)
     * Usage: NozhText.labeled("Version", "0.1.0")
     */
    public static MutableText labeled(String label, String value) {
        return Text.literal(label + ": ").formatted(LABEL)
                .append(Text.literal(value).formatted(VALUE_NORMAL));
    }

    // === Phase 6: Multi-Language Support ===

    /**
     * Translatable info message.
     * Key: nozh.info.{key}
     */
    public static MutableText translatableInfo(String key, Object... args) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.translatable(key, args).formatted(VALUE_NORMAL));
    }

    public static MutableText translatableSuccess(String key, Object... args) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.translatable(key, args).formatted(VALUE_GOOD));
    }

    public static MutableText translatableWarning(String key, Object... args) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.translatable(key, args).formatted(VALUE_WARN));
    }

    public static MutableText translatableError(String key, Object... args) {
        return Text.literal("NOZH: ").formatted(HEADER)
                .append(Text.translatable(key, args).formatted(VALUE_BAD));
    }

    public static MutableText translatableHeader(String key) {
        return Text.translatable(key).formatted(HEADER);
    }

    /**
     * Translatable label with value.
     */
    public static MutableText translatableLabeled(String labelKey, Object value) {
        return Text.translatable(labelKey).append(": ").formatted(LABEL)
                .append(Text.literal(String.valueOf(value)).formatted(VALUE_NORMAL));
    }
}
