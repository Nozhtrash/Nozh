package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.NozhLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class DecisionTreeModelStore {

    private static final int CURRENT_VERSION = 1;
    private final NozhLogger logger;
    private final Path modelDir;
    private final Path modelFile;
    private DecisionTreeModel cached;

    public DecisionTreeModelStore(NozhLogger logger) {
        this(logger, NozhConstants.MODEL_DIR, NozhConstants.MODEL_FILE);
    }

    DecisionTreeModelStore(NozhLogger logger, Path modelDir, Path modelFile) {
        this.logger = logger;
        this.modelDir = modelDir;
        this.modelFile = modelFile;
    }

    public Optional<DecisionTreeModel> loadModel() {
        if (cached != null) {
            return Optional.of(cached);
        }
        DecisionTreeModel loaded = readModel();
        if (loaded != null) {
            cached = loaded;
            return Optional.of(loaded);
        }
        if (ensureDefaultModel()) {
            cached = readModel();
            if (cached != null) {
                return Optional.of(cached);
            }
        }
        return Optional.empty();
    }

    private DecisionTreeModel readModel() {
        try {
            if (!Files.exists(modelFile)) {
                return null;
            }
            String json = Files.readString(modelFile, StandardCharsets.UTF_8);
            DecisionTreeModel model = DecisionTreeModelJson.fromJson(json);
            if (model == null) {
                logger.warn("Hybrid model parse failed, falling back to rules");
                return null;
            }
            if (model.modelVersion() != CURRENT_VERSION) {
                logger.warn("Hybrid model version mismatch (expected %d got %d), falling back to rules",
                        CURRENT_VERSION, model.modelVersion());
                return null;
            }
            return model;
        } catch (Exception e) {
            logger.warn("Hybrid model read failed, falling back to rules: " + e.getMessage());
            return null;
        }
    }

    private boolean ensureDefaultModel() {
        try {
            Files.createDirectories(modelDir);
            if (Files.exists(modelFile)) {
                return true;
            }
            DecisionTreeModel model = defaultModel();
            String json = DecisionTreeModelJson.toJson(model);
            Files.writeString(modelFile, json, StandardCharsets.UTF_8);
            logger.info("Hybrid model default persisted at " + modelFile);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to persist hybrid model default: " + e.getMessage());
            return false;
        }
    }

    private DecisionTreeModel defaultModel() {
        List<DecisionTreeModel.NodeSpec> specs = List.of(
                new DecisionTreeModel.NodeSpec(0, "p95FrametimeMs", 20.0, 1, 2, null, 0.0),
                new DecisionTreeModel.NodeSpec(1, "spikeCount", 2.0, 3, 4, null, 0.0),
                new DecisionTreeModel.NodeSpec(2, null, 0.0, null, null,
                        DecisionTreeModel.DecisionLabel.BLOCK, 0.72),
                new DecisionTreeModel.NodeSpec(3, null, 0.0, null, null,
                        DecisionTreeModel.DecisionLabel.ALLOW, 0.78),
                new DecisionTreeModel.NodeSpec(4, null, 0.0, null, null,
                        DecisionTreeModel.DecisionLabel.BLOCK, 0.63));
        return DecisionTreeModel.fromSpecs(CURRENT_VERSION, Instant.now().toString(), 0, specs);
    }
}
