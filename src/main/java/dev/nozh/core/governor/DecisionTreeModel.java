package dev.nozh.core.governor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DecisionTreeModel {

    public enum DecisionLabel {
        ALLOW,
        BLOCK
    }

    public record ModelDecision(DecisionLabel label, double confidence) {
    }

    public record NodeSpec(
            int id,
            String feature,
            double threshold,
            Integer leftId,
            Integer rightId,
            DecisionLabel label,
            double confidence) {

        public boolean isLeaf() {
            return label != null;
        }
    }

    private sealed interface Node permits DecisionNode, LeafNode {
        ModelDecision evaluate(DecisionFeatures features);
    }

    private static final class DecisionNode implements Node {
        private final String feature;
        private final double threshold;
        private final Node left;
        private final Node right;

        private DecisionNode(String feature, double threshold, Node left, Node right) {
            this.feature = feature;
            this.threshold = threshold;
            this.left = left;
            this.right = right;
        }

        @Override
        public ModelDecision evaluate(DecisionFeatures features) {
            double value = features.featureValue(feature);
            return value <= threshold ? left.evaluate(features) : right.evaluate(features);
        }
    }

    private static final class LeafNode implements Node {
        private final ModelDecision decision;

        private LeafNode(ModelDecision decision) {
            this.decision = decision;
        }

        @Override
        public ModelDecision evaluate(DecisionFeatures features) {
            return decision;
        }
    }

    private final int modelVersion;
    private final String trainedAt;
    private final int rootId;
    private final Map<Integer, NodeSpec> nodeSpecs;
    private final Node root;

    private DecisionTreeModel(int modelVersion, String trainedAt, int rootId, Map<Integer, NodeSpec> nodeSpecs, Node root) {
        this.modelVersion = modelVersion;
        this.trainedAt = trainedAt;
        this.rootId = rootId;
        this.nodeSpecs = nodeSpecs;
        this.root = root;
    }

    public static DecisionTreeModel fromSpecs(int modelVersion, String trainedAt, int rootId, List<NodeSpec> specs) {
        Objects.requireNonNull(specs, "specs");
        Map<Integer, NodeSpec> specMap = new HashMap<>();
        for (NodeSpec spec : specs) {
            specMap.put(spec.id(), spec);
        }
        Node root = buildNode(rootId, specMap, new ArrayList<>());
        if (root == null) {
            return null;
        }
        return new DecisionTreeModel(modelVersion, trainedAt, rootId, specMap, root);
    }

    private static Node buildNode(int nodeId, Map<Integer, NodeSpec> specMap, List<Integer> visited) {
        if (visited.contains(nodeId)) {
            return null;
        }
        NodeSpec spec = specMap.get(nodeId);
        if (spec == null) {
            return null;
        }
        if (spec.isLeaf()) {
            return new LeafNode(new ModelDecision(spec.label(), spec.confidence()));
        }
        if (spec.leftId() == null || spec.rightId() == null || spec.feature() == null) {
            return null;
        }
        visited.add(nodeId);
        Node left = buildNode(spec.leftId(), specMap, visited);
        Node right = buildNode(spec.rightId(), specMap, visited);
        visited.remove(visited.size() - 1);
        if (left == null || right == null) {
            return null;
        }
        return new DecisionNode(spec.feature(), spec.threshold(), left, right);
    }

    public ModelDecision evaluate(DecisionFeatures features) {
        return root.evaluate(features);
    }

    public int modelVersion() {
        return modelVersion;
    }

    public String trainedAt() {
        return trainedAt;
    }

    public int rootId() {
        return rootId;
    }

    public List<NodeSpec> nodeSpecs() {
        List<NodeSpec> specs = new ArrayList<>(nodeSpecs.values());
        specs.sort((a, b) -> Integer.compare(a.id(), b.id()));
        return Collections.unmodifiableList(specs);
    }
}
