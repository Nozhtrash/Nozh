package dev.nozh.core.governor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DecisionTreeModelJson {

    private DecisionTreeModelJson() {
    }

    public static DecisionTreeModel fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        int version = getInt(json, "modelVersion", -1);
        int rootId = getInt(json, "rootId", -1);
        String trainedAt = getString(json, "trainedAt", "");
        if (version <= 0 || rootId < 0) {
            return null;
        }
        String nodesBlock = getArrayBlock(json, "nodes");
        if (nodesBlock == null) {
            return null;
        }
        List<DecisionTreeModel.NodeSpec> specs = parseNodes(nodesBlock);
        if (specs.isEmpty()) {
            return null;
        }
        return DecisionTreeModel.fromSpecs(version, trainedAt, rootId, specs);
    }

    public static String toJson(DecisionTreeModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"modelVersion\": ").append(model.modelVersion()).append(",\n");
        sb.append("  \"trainedAt\": \"").append(model.trainedAt()).append("\",\n");
        sb.append("  \"modelType\": \"decision_tree\",\n");
        sb.append("  \"rootId\": ").append(model.rootId()).append(",\n");
        sb.append("  \"nodes\": [\n");
        List<DecisionTreeModel.NodeSpec> specs = model.nodeSpecs();
        for (int i = 0; i < specs.size(); i++) {
            DecisionTreeModel.NodeSpec spec = specs.get(i);
            sb.append("    {");
            sb.append("\"id\": ").append(spec.id());
            if (spec.isLeaf()) {
                sb.append(", \"label\": \"").append(spec.label().name()).append("\"");
                sb.append(", \"confidence\": ").append(spec.confidence());
            } else {
                sb.append(", \"feature\": \"").append(spec.feature()).append("\"");
                sb.append(", \"threshold\": ").append(spec.threshold());
                sb.append(", \"left\": ").append(spec.leftId());
                sb.append(", \"right\": ").append(spec.rightId());
            }
            sb.append("}");
            if (i < specs.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static List<DecisionTreeModel.NodeSpec> parseNodes(String nodesBlock) {
        List<DecisionTreeModel.NodeSpec> specs = new ArrayList<>();
        Pattern nodePattern = Pattern.compile("\\{([^{}]+)\\}");
        Matcher matcher = nodePattern.matcher(nodesBlock);
        while (matcher.find()) {
            String nodeJson = matcher.group(1);
            int id = getInt(nodeJson, "id", -1);
            if (id < 0) {
                continue;
            }
            String label = getString(nodeJson, "label", null);
            if (label != null) {
                double confidence = getDouble(nodeJson, "confidence", 0.0);
                DecisionTreeModel.DecisionLabel decisionLabel;
                try {
                    decisionLabel = DecisionTreeModel.DecisionLabel.valueOf(label);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                specs.add(new DecisionTreeModel.NodeSpec(id, null, 0.0, null, null, decisionLabel, confidence));
            } else {
                String feature = getString(nodeJson, "feature", null);
                double threshold = getDouble(nodeJson, "threshold", Double.NaN);
                int left = getInt(nodeJson, "left", -1);
                int right = getInt(nodeJson, "right", -1);
                if (feature == null || !Double.isFinite(threshold) || left < 0 || right < 0) {
                    continue;
                }
                specs.add(new DecisionTreeModel.NodeSpec(id, feature, threshold, left, right, null, 0.0));
            }
        }
        return specs;
    }

    private static String getArrayBlock(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static int getInt(String json, String key, int def) {
        Pattern p = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private static double getDouble(String json, String key, double def) {
        Pattern p = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private static String getString(String json, String key, String def) {
        Pattern p = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return def;
    }
}
