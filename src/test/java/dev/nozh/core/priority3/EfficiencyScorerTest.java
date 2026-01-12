package dev.nozh.core.priority3;

import dev.nozh.core.priority2.Priority2Suggestion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EfficiencyScorerTest {

    private static final double EPS = 1e-9;

    @Test
    void score_sanitizesAndClamps() {
        EfficiencyScorer.Score s1 = new EfficiencyScorer.Score(Double.NaN, -1.0, Double.POSITIVE_INFINITY, Double.NaN);
        assertEquals(0.0, s1.expectedGainMs, EPS);
        assertEquals(0.0, s1.visualCost, EPS);
        assertEquals(0.0, s1.gameplayCost, EPS);
        assertEquals(0.0, s1.confidence01, EPS);
        assertEquals(0.0, s1.finalScore, EPS);

        EfficiencyScorer.Score s2 = new EfficiencyScorer.Score(10.0, 2.0, 3.0, 2.0);
        assertEquals(1.0, s2.confidence01, EPS);
        double expectedEff = 10.0 / (2.0 + 3.0 + 0.1);
        assertEquals(expectedEff, s2.efficiency, EPS);
        assertEquals(expectedEff, s2.finalScore, EPS);

        EfficiencyScorer.Score s3 = new EfficiencyScorer.Score(10.0, 2.0, 3.0, -5.0);
        assertEquals(0.0, s3.confidence01, EPS);
        assertEquals(0.0, s3.finalScore, EPS);
    }

    @Test
    void score_nullSuggestion_returnsPenaltyCosts() {
        EfficiencyScorer scorer = new EfficiencyScorer();
        EfficiencyScorer.Score out = scorer.score(null, 0.5);
        assertEquals(0.0, out.expectedGainMs, EPS);
        assertEquals(10.0, out.visualCost, EPS);
        assertEquals(10.0, out.gameplayCost, EPS);
        assertEquals(0.5, out.confidence01, EPS);
        assertEquals(0.0, out.finalScore, EPS);
    }

    @Test
    void score_nullId_returnsPenaltyCosts() {
        EfficiencyScorer scorer = new EfficiencyScorer();
        Priority2Suggestion s = new Priority2Suggestion(null, "test", Priority2Suggestion.Severity.RECOMMENDED);
        EfficiencyScorer.Score out = scorer.score(s, 0.75);
        assertEquals(0.0, out.expectedGainMs, EPS);
        assertEquals(10.0, out.visualCost, EPS);
        assertEquals(10.0, out.gameplayCost, EPS);
        assertEquals(0.75, out.confidence01, EPS);
        assertEquals(0.0, out.finalScore, EPS);
    }

    @Test
    void score_knownSuggestionIds_matchExpectedWeights() {
        EfficiencyScorer scorer = new EfficiencyScorer();

        EfficiencyScorer.Score p = scorer.score(new Priority2Suggestion("gpu.reduce_particles", "test", Priority2Suggestion.Severity.RECOMMENDED), 0.8);
        assertEquals(6.0, p.expectedGainMs, EPS);
        assertEquals(1.5, p.visualCost, EPS);
        assertEquals(0.5, p.gameplayCost, EPS);

        EfficiencyScorer.Score sh = scorer.score(new Priority2Suggestion("gpu.reduce_shaders", "test", Priority2Suggestion.Severity.RECOMMENDED), 1.0);
        assertEquals(10.0, sh.expectedGainMs, EPS);
        assertEquals(4.0, sh.visualCost, EPS);
        assertEquals(0.5, sh.gameplayCost, EPS);

        EfficiencyScorer.Score cpu = scorer.score(new Priority2Suggestion("cpu.reduce_entities", "test", Priority2Suggestion.Severity.RECOMMENDED), 1.0);
        assertEquals(9.0, cpu.expectedGainMs, EPS);
        assertEquals(2.0, cpu.visualCost, EPS);
        assertEquals(3.0, cpu.gameplayCost, EPS);

        EfficiencyScorer.Score combat = scorer.score(new Priority2Suggestion("scenario.combat_stabilize", "test", Priority2Suggestion.Severity.RECOMMENDED), 1.0);
        assertEquals(7.0, combat.expectedGainMs, EPS);
        assertEquals(2.5, combat.visualCost, EPS);
        assertEquals(1.0, combat.gameplayCost, EPS);

        EfficiencyScorer.Score def = scorer.score(new Priority2Suggestion("some.unknown.id", "test", Priority2Suggestion.Severity.RECOMMENDED), 1.0);
        assertEquals(4.0, def.expectedGainMs, EPS);
        assertEquals(3.0, def.visualCost, EPS);
        assertEquals(3.0, def.gameplayCost, EPS);
    }
}
