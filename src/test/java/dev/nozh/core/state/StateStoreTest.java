package dev.nozh.core.state;

import dev.nozh.core.config.NozhConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void supportsConcurrentSnapshotsAndUpdates() throws InterruptedException {
        StateStore store = StateStore.getInstance();
        store.reset();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        Runnable updater = () -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    store.update(state -> state.withDecision("thread-" + Thread.currentThread().getId(),
                            System.nanoTime()));
                }
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable reader = () -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    store.snapshot();
                    store.snapshotSafe();
                }
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                doneLatch.countDown();
            }
        };

        executor.execute(updater);
        executor.execute(updater);
        executor.execute(reader);
        executor.execute(reader);

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        try {
            assertTrue(errors.isEmpty());
        } finally {
            store.reset();
        }
    }

    @Test
    void updateRejectsInvalidState() {
        StateStore store = StateStore.getInstance();
        store.reset();

        NozhConfig config = new NozhConfig();
        config.safeModeForce = true;
        config.allowAutoTuning = true;

        try {
            assertThrows(StateInvariantViolationException.class,
                    () -> store.update(state -> RuntimeState.fromConfig(config)));
        } finally {
            store.reset();
        }
    }
}
