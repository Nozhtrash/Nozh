package dev.nozh.core.i18n;

import dev.nozh.NozhConstants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-language support manager for NOZH.
 * Supports 15+ languages with fallback system.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class LanguageManager {

    /**
     * Supported languages.
     */
    public enum Language {
        EN_US("en_us", "English (US)", "English"),
        EN_GB("en_gb", "English (UK)", "English"),
        ES_ES("es_es", "Español (España)", "Spanish"),
        ES_MX("es_mx", "Español (México)", "Spanish"),
        PT_BR("pt_br", "Português (Brasil)", "Portuguese"),
        FR_FR("fr_fr", "Français", "French"),
        DE_DE("de_de", "Deutsch", "German"),
        IT_IT("it_it", "Italiano", "Italian"),
        RU_RU("ru_ru", "Русский", "Russian"),
        ZH_CN("zh_cn", "简体中文", "Chinese Simplified"),
        ZH_TW("zh_tw", "繁體中文", "Chinese Traditional"),
        JA_JP("ja_jp", "日本語", "Japanese"),
        KO_KR("ko_kr", "한국어", "Korean"),
        PL_PL("pl_pl", "Polski", "Polish"),
        UK_UA("uk_ua", "Українська", "Ukrainian");

        public final String code;
        public final String nativeName;
        public final String englishName;

        Language(String code, String nativeName, String englishName) {
            this.code = code;
            this.nativeName = nativeName;
            this.englishName = englishName;
        }

        public static Language fromCode(String code) {
            for (Language lang : values()) {
                if (lang.code.equalsIgnoreCase(code)) {
                    return lang;
                }
            }
            return EN_US;
        }
    }

    // Translation keys
    public static final String KEY_MOD_NAME = "nozh.name";
    public static final String KEY_MOD_DESC = "nozh.description";
    public static final String KEY_ENABLED = "nozh.enabled";
    public static final String KEY_DISABLED = "nozh.disabled";
    public static final String KEY_FPS = "nozh.fps";
    public static final String KEY_FRAMETIME = "nozh.frametime";
    public static final String KEY_POTATO_MODE = "nozh.potato_mode";
    public static final String KEY_LOW_MEMORY = "nozh.low_memory";
    public static final String KEY_OPTIMIZATION_APPLIED = "nozh.optimization_applied";
    public static final String KEY_CONFIG_SAVED = "nozh.config_saved";
    public static final String KEY_CONFIG_RESET = "nozh.config_reset";

    private Language currentLanguage;
    private final Map<Language, Map<String, String>> translations;

    /**
     * Constructs a new LanguageManager.
     */
    public LanguageManager() {
        this.currentLanguage = Language.EN_US;
        this.translations = new ConcurrentHashMap<>();
        initializeTranslations();
    }

    /**
     * Initializes all translations.
     */
    private void initializeTranslations() {
        // English (US) - Default
        Map<String, String> en = new HashMap<>();
        en.put(KEY_MOD_NAME, "NOZH Performance Optimizer");
        en.put(KEY_MOD_DESC, "Intelligent adaptive performance optimization");
        en.put(KEY_ENABLED, "Enabled");
        en.put(KEY_DISABLED, "Disabled");
        en.put(KEY_FPS, "FPS");
        en.put(KEY_FRAMETIME, "Frame Time");
        en.put(KEY_POTATO_MODE, "Potato Mode");
        en.put(KEY_LOW_MEMORY, "Low Memory Mode");
        en.put(KEY_OPTIMIZATION_APPLIED, "Optimization Applied");
        en.put(KEY_CONFIG_SAVED, "Configuration Saved");
        en.put(KEY_CONFIG_RESET, "Configuration Reset");
        translations.put(Language.EN_US, en);
        translations.put(Language.EN_GB, en);

        // Spanish
        Map<String, String> es = new HashMap<>();
        es.put(KEY_MOD_NAME, "NOZH Optimizador de Rendimiento");
        es.put(KEY_MOD_DESC, "Optimización de rendimiento adaptativa inteligente");
        es.put(KEY_ENABLED, "Activado");
        es.put(KEY_DISABLED, "Desactivado");
        es.put(KEY_FPS, "FPS");
        es.put(KEY_FRAMETIME, "Tiempo de Cuadro");
        es.put(KEY_POTATO_MODE, "Modo Patata");
        es.put(KEY_LOW_MEMORY, "Modo Baja Memoria");
        es.put(KEY_OPTIMIZATION_APPLIED, "Optimización Aplicada");
        es.put(KEY_CONFIG_SAVED, "Configuración Guardada");
        es.put(KEY_CONFIG_RESET, "Configuración Restablecida");
        translations.put(Language.ES_ES, es);
        translations.put(Language.ES_MX, es);

        // Portuguese (Brazil)
        Map<String, String> pt = new HashMap<>();
        pt.put(KEY_MOD_NAME, "NOZH Otimizador de Desempenho");
        pt.put(KEY_MOD_DESC, "Otimização de desempenho adaptativa inteligente");
        pt.put(KEY_ENABLED, "Ativado");
        pt.put(KEY_DISABLED, "Desativado");
        pt.put(KEY_FPS, "FPS");
        pt.put(KEY_FRAMETIME, "Tempo de Quadro");
        pt.put(KEY_POTATO_MODE, "Modo Batata");
        pt.put(KEY_LOW_MEMORY, "Modo Pouca Memória");
        pt.put(KEY_OPTIMIZATION_APPLIED, "Otimização Aplicada");
        pt.put(KEY_CONFIG_SAVED, "Configuração Salva");
        pt.put(KEY_CONFIG_RESET, "Configuração Redefinida");
        translations.put(Language.PT_BR, pt);

        // French
        Map<String, String> fr = new HashMap<>();
        fr.put(KEY_MOD_NAME, "NOZH Optimiseur de Performance");
        fr.put(KEY_MOD_DESC, "Optimisation adaptative intelligente des performances");
        fr.put(KEY_ENABLED, "Activé");
        fr.put(KEY_DISABLED, "Désactivé");
        fr.put(KEY_FPS, "IPS");
        fr.put(KEY_FRAMETIME, "Temps d'image");
        fr.put(KEY_POTATO_MODE, "Mode Patate");
        fr.put(KEY_LOW_MEMORY, "Mode Mémoire Faible");
        fr.put(KEY_OPTIMIZATION_APPLIED, "Optimisation Appliquée");
        fr.put(KEY_CONFIG_SAVED, "Configuration Enregistrée");
        fr.put(KEY_CONFIG_RESET, "Configuration Réinitialisée");
        translations.put(Language.FR_FR, fr);

        // German
        Map<String, String> de = new HashMap<>();
        de.put(KEY_MOD_NAME, "NOZH Leistungsoptimierer");
        de.put(KEY_MOD_DESC, "Intelligente adaptive Leistungsoptimierung");
        de.put(KEY_ENABLED, "Aktiviert");
        de.put(KEY_DISABLED, "Deaktiviert");
        de.put(KEY_FPS, "FPS");
        de.put(KEY_FRAMETIME, "Bildzeit");
        de.put(KEY_POTATO_MODE, "Kartoffelmodus");
        de.put(KEY_LOW_MEMORY, "Wenig-Speicher-Modus");
        de.put(KEY_OPTIMIZATION_APPLIED, "Optimierung Angewandt");
        de.put(KEY_CONFIG_SAVED, "Konfiguration Gespeichert");
        de.put(KEY_CONFIG_RESET, "Konfiguration Zurückgesetzt");
        translations.put(Language.DE_DE, de);

        // Russian
        Map<String, String> ru = new HashMap<>();
        ru.put(KEY_MOD_NAME, "NOZH Оптимизатор Производительности");
        ru.put(KEY_MOD_DESC, "Интеллектуальная адаптивная оптимизация");
        ru.put(KEY_ENABLED, "Включено");
        ru.put(KEY_DISABLED, "Выключено");
        ru.put(KEY_FPS, "FPS");
        ru.put(KEY_FRAMETIME, "Время кадра");
        ru.put(KEY_POTATO_MODE, "Режим картошки");
        ru.put(KEY_LOW_MEMORY, "Режим низкой памяти");
        ru.put(KEY_OPTIMIZATION_APPLIED, "Оптимизация применена");
        ru.put(KEY_CONFIG_SAVED, "Конфигурация сохранена");
        ru.put(KEY_CONFIG_RESET, "Конфигурация сброшена");
        translations.put(Language.RU_RU, ru);

        // Chinese Simplified
        Map<String, String> zh = new HashMap<>();
        zh.put(KEY_MOD_NAME, "NOZH 性能优化器");
        zh.put(KEY_MOD_DESC, "智能自适应性能优化");
        zh.put(KEY_ENABLED, "已启用");
        zh.put(KEY_DISABLED, "已禁用");
        zh.put(KEY_FPS, "帧率");
        zh.put(KEY_FRAMETIME, "帧时间");
        zh.put(KEY_POTATO_MODE, "土豆模式");
        zh.put(KEY_LOW_MEMORY, "低内存模式");
        zh.put(KEY_OPTIMIZATION_APPLIED, "已应用优化");
        zh.put(KEY_CONFIG_SAVED, "配置已保存");
        zh.put(KEY_CONFIG_RESET, "配置已重置");
        translations.put(Language.ZH_CN, zh);
        translations.put(Language.ZH_TW, zh);

        // Japanese
        Map<String, String> ja = new HashMap<>();
        ja.put(KEY_MOD_NAME, "NOZH パフォーマンス最適化");
        ja.put(KEY_MOD_DESC, "インテリジェント適応型パフォーマンス最適化");
        ja.put(KEY_ENABLED, "有効");
        ja.put(KEY_DISABLED, "無効");
        ja.put(KEY_FPS, "FPS");
        ja.put(KEY_FRAMETIME, "フレームタイム");
        ja.put(KEY_POTATO_MODE, "ポテトモード");
        ja.put(KEY_LOW_MEMORY, "低メモリモード");
        ja.put(KEY_OPTIMIZATION_APPLIED, "最適化が適用されました");
        ja.put(KEY_CONFIG_SAVED, "設定が保存されました");
        ja.put(KEY_CONFIG_RESET, "設定がリセットされました");
        translations.put(Language.JA_JP, ja);

        // Korean
        Map<String, String> ko = new HashMap<>();
        ko.put(KEY_MOD_NAME, "NOZH 성능 최적화");
        ko.put(KEY_MOD_DESC, "지능형 적응 성능 최적화");
        ko.put(KEY_ENABLED, "활성화됨");
        ko.put(KEY_DISABLED, "비활성화됨");
        ko.put(KEY_FPS, "FPS");
        ko.put(KEY_FRAMETIME, "프레임 타임");
        ko.put(KEY_POTATO_MODE, "감자 모드");
        ko.put(KEY_LOW_MEMORY, "저메모리 모드");
        ko.put(KEY_OPTIMIZATION_APPLIED, "최적화 적용됨");
        ko.put(KEY_CONFIG_SAVED, "설정 저장됨");
        ko.put(KEY_CONFIG_RESET, "설정 초기화됨");
        translations.put(Language.KO_KR, ko);
    }

    /**
     * Gets a translated string.
     * 
     * @param key translation key
     * @return translated string or key if not found
     */
    public String get(String key) {
        Map<String, String> langMap = translations.get(currentLanguage);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }

        // Fallback to English
        Map<String, String> fallback = translations.get(Language.EN_US);
        if (fallback != null && fallback.containsKey(key)) {
            return fallback.get(key);
        }

        return key;
    }

    /**
     * Gets a formatted translated string.
     * 
     * @param key  translation key
     * @param args format arguments
     * @return formatted translated string
     */
    public String getFormatted(String key, Object... args) {
        return String.format(get(key), args);
    }

    /**
     * Sets the current language.
     * 
     * @param language language to set
     */
    public void setLanguage(Language language) {
        this.currentLanguage = language;
        NozhConstants.LOGGER.info("Language changed to: {} ({})",
                language.nativeName, language.code);
    }

    /**
     * Gets current language.
     * 
     * @return current language
     */
    public Language getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Gets all available languages.
     * 
     * @return array of languages
     */
    public static Language[] getAvailableLanguages() {
        return Language.values();
    }

    /**
     * Detects language from Minecraft's language setting.
     * 
     * @param mcLangCode Minecraft language code
     * @return detected language
     */
    public Language detectFromMinecraft(String mcLangCode) {
        return Language.fromCode(mcLangCode);
    }
}
