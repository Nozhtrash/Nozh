package dev.nozh.core.governor;

import dev.nozh.core.context.Scenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DecisionTreeModelDeterminismTest {

    @Test
    void evaluationIsDeterministic() {
        DecisionTreeModel model = DecisionTreeModel.fromSpecs(
                1,
                "2025-01-15T00:00:00Z",
                0,
                List.of(
                        new DecisionTreeModel.NodeSpec(0, "p95FrametimeMs", 20.0, 1, 2, null, 0.0),
                        new DecisionTreeModel.NodeSpec(1, "spikeCount", 2.0, 3, 4, null, 0.0),
                        new DecisionTreeModel.NodeSpec(2, null, 0.0, null, null,
                                DecisionTreeModel.DecisionLabel.BLOCK, 0.72),
                        new DecisionTreeModel.NodeSpec(3, null, 0.0, null, null,
                                DecisionTreeModel.DecisionLabel.ALLOW, 0.78),
                        new DecisionTreeModel.NodeSpec(4, null, 0.0, null, null,
                                DecisionTreeModel.DecisionLabel.BLOCK, 0.63)));

        assertNotNull(model);

        DecisionFeatures features = new DecisionFeatures(
                16.0,
                18.0,
                1,
                "GPU",
                GovernorMode.AUTO_CONSERVATIVE,
                Scenario.EXPLORING);

        DecisionTreeModel.ModelDecision first = model.evaluate(features);
        for (int i = 0; i < 10; i++) {
            DecisionTreeModel.ModelDecision next = model.evaluate(features);
            assertEquals(first.label(), next.label());
            assertEquals(first.confidence(), next.confidence(), 0.0001);
        }
    }
}
