package dev.nozh.core.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateStoreTest {

    @Test
    void updateMarksDirtyAndCanBePersisted() {
        StateStore store = StateStore.getInstance();
        store.reset();

        try {
            store.update(state -> state.withDecision("unit-test", System.currentTimeMillis()));
            assertTrue(store.isDirty());

            store.markPersisted();
            assertFalse(store.isDirty());
        } finally {
            store.reset();
        }
    }
}
