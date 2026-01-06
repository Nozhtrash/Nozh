package dev.nozh.core.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderVisibilityDeciderTest {

    @Test
    void resolveVisibilityUsesSupplierValue() {
        assertTrue(RenderVisibilityDecider.resolveVisibility(() -> true, "test-true"));
        assertFalse(RenderVisibilityDecider.resolveVisibility(() -> false, "test-false"));
    }

    @Test
    void resolveVisibilityFallsBackWhenSupplierThrows() {
        assertTrue(RenderVisibilityDecider.resolveVisibility(() -> {
            throw new IllegalStateException("boom");
        }, "test-exception"));
    }
}
