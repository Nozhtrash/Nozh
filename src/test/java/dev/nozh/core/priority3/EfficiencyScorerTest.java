package dev.nozh.core.priority3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

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
    void score_knownSuggestionIds_matchExpectedWeights() throws Exception {
        EfficiencyScorer scorer = new EfficiencyScorer();

        // Invoke score(Priority2Suggestion, double) without compile-time dependency on Priority2Suggestion shape.
        Class<?> suggestionClass = Class.forName("dev.nozh.core.priority2.Priority2Suggestion");
        Method scoreMethod = EfficiencyScorer.class.getMethod("score", suggestionClass, double.class);

        EfficiencyScorer.Score p = (EfficiencyScorer.Score) scoreMethod.invoke(scorer, newSuggestionWithId(suggestionClass, "gpu.reduce_particles"), 0.8);
        assertEquals(6.0, p.expectedGainMs, EPS);
        assertEquals(1.5, p.visualCost, EPS);
        assertEquals(0.5, p.gameplayCost, EPS);

        EfficiencyScorer.Score sh = (EfficiencyScorer.Score) scoreMethod.invoke(scorer, newSuggestionWithId(suggestionClass, "gpu.reduce_shaders"), 1.0);
        assertEquals(10.0, sh.expectedGainMs, EPS);
        assertEquals(4.0, sh.visualCost, EPS);
        assertEquals(0.5, sh.gameplayCost, EPS);

        EfficiencyScorer.Score cpu = (EfficiencyScorer.Score) scoreMethod.invoke(scorer, newSuggestionWithId(suggestionClass, "cpu.reduce_entities"), 1.0);
        assertEquals(9.0, cpu.expectedGainMs, EPS);
        assertEquals(2.0, cpu.visualCost, EPS);
        assertEquals(3.0, cpu.gameplayCost, EPS);

        EfficiencyScorer.Score combat = (EfficiencyScorer.Score) scoreMethod.invoke(scorer, newSuggestionWithId(suggestionClass, "scenario.combat_stabilize"), 1.0);
        assertEquals(7.0, combat.expectedGainMs, EPS);
        assertEquals(2.5, combat.visualCost, EPS);
        assertEquals(1.0, combat.gameplayCost, EPS);

        EfficiencyScorer.Score def = (EfficiencyScorer.Score) scoreMethod.invoke(scorer, newSuggestionWithId(suggestionClass, "some.unknown.id"), 1.0);
        assertEquals(4.0, def.expectedGainMs, EPS);
        assertEquals(3.0, def.visualCost, EPS);
        assertEquals(3.0, def.gameplayCost, EPS);
    }

    private static Object newSuggestionWithId(Class<?> suggestionClass, String id) throws Exception {
        Object s = tryConstructSuggestion(suggestionClass);
        assertNotNull(s, "Could not construct Priority2Suggestion instance via reflection");

        // Prefer field named "id".
        Field idField = null;
        try {
            idField = suggestionClass.getField("id");
        } catch (NoSuchFieldException ignored) {
            // fall through
        }
        if (idField == null) {
            try {
                idField = suggestionClass.getDeclaredField("id");
                idField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                fail("Priority2Suggestion has no 'id' field; update test helper to match class shape.");
            }
        }

        idField.set(s, id);
        return s;
    }

    private static Object tryConstructSuggestion(Class<?> suggestionClass) throws Exception {
        // 1) No-arg ctor
        try {
            Constructor<?> c = suggestionClass.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (NoSuchMethodException ignored) {
        }

        // 2) Single-String ctor
        try {
            Constructor<?> c = suggestionClass.getDeclaredConstructor(String.class);
            c.setAccessible(true);
            return c.newInstance((String) null);
        } catch (NoSuchMethodException ignored) {
        }

        // 3) Fallback: first ctor, fill null/0
        Constructor<?>[] ctors = suggestionClass.getDeclaredConstructors();
        if (ctors.length == 0) return null;

        Constructor<?> c = ctors[0];
        c.setAccessible(true);
        Class<?>[] p = c.getParameterTypes();
        Object[] args = new Object[p.length];
        for (int i = 0; i < p.length; i++) {
            if (!p[i].isPrimitive()) {
                args[i] = null;
            } else if (p[i] == boolean.class) {
                args[i] = false;
            } else if (p[i] == byte.class) {
                args[i] = (byte) 0;
            } else if (p[i] == short.class) {
                args[i] = (short) 0;
            } else if (p[i] == int.class) {
                args[i] = 0;
            } else if (p[i] == long.class) {
                args[i] = 0L;
            } else if (p[i] == float.class) {
                args[i] = 0f;
            } else if (p[i] == double.class) {
                args[i] = 0d;
            } else if (p[i] == char.class) {
                args[i] = (char) 0;
            }
        }
        return c.newInstance(args);
    }
}
