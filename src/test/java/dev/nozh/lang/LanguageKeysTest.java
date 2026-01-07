package dev.nozh.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageKeysTest {
    private static final Path LANG_DIR = Paths.get("src/main/resources/assets/nozh/lang");
    private static final String BASE_LANGUAGE = "en_us.json";
    private static final Set<String> VALIDATED_LANGUAGES = Set.of(
            "en_us.json",
            "pt_br.json",
            "fr_fr.json",
            "de_de.json",
            "it_it.json",
            "ja_jp.json"
    );
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Gson GSON = new Gson();

    @Test
    void allLanguageFilesContainBaseKeys() throws IOException {
        Path basePath = LANG_DIR.resolve(BASE_LANGUAGE);
        Map<String, String> baseMap = readLanguageFile(basePath);
        Set<String> baseKeys = baseMap.keySet();

        List<Path> languageFiles = listLanguageFiles();
        for (Path languageFile : languageFiles) {
            String filename = languageFile.getFileName().toString();
            if (!VALIDATED_LANGUAGES.contains(filename) || filename.equals(BASE_LANGUAGE)) {
                continue;
            }
            Map<String, String> languageMap = readLanguageFile(languageFile);
            Set<String> missingKeys = new HashSet<>(baseKeys);
            missingKeys.removeAll(languageMap.keySet());

            Set<String> extraKeys = new HashSet<>(languageMap.keySet());
            extraKeys.removeAll(baseKeys);

            assertTrue(missingKeys.isEmpty(), filename + " is missing keys: " + missingKeys);
            assertTrue(extraKeys.isEmpty(), filename + " has extra keys: " + extraKeys);
        }
    }

    private static Map<String, String> readLanguageFile(Path path) throws IOException {
        String json = Files.readString(path);
        return GSON.fromJson(json, MAP_TYPE);
    }

    private static List<Path> listLanguageFiles() throws IOException {
        try (Stream<Path> paths = Files.list(LANG_DIR)) {
            return paths
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
