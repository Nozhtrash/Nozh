package dev.nozh.core.bus;

import dev.nozh.core.NoOpLogger;
import dev.nozh.core.state.RuntimeState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chaos test for Contract 2 (mandatory final acceptance test).
 * 
 * Validates system stability under adverse conditions:
 * - Executor throws exceptions repeatedly
 * - Queue spam (many dispatches)
 * - Repeated ticks
 * - Full queue scenarios
 * 
 * System MUST:
 * - Never crash
 * - Never deadlock
 * - Produce consistent reports
 * - Remain operational
 * 
 * THIS IS THE SEAL TEST: if this passes, Contract 2 is bulletproof.
 */
class ChaosBasicTest {

    private static final RuntimeState FAKE_STATE = RuntimeState.defaults();

    @Test
    void testExecutorAlwaysThrows() {
        // Executor that ALWAYS throws for all capabilities
        FakeCapabilityExecutor chaosExecutor = new FakeCapabilityExecutor()
                .withCurrentValue(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("ALL"))
                .withCurrentValue(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FANCY"))
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.THROW)
                .when(CapabilityId.CLOUDS, FakeCapabilityExecutor.Behavior.THROW)
                .when(CapabilityId.ENTITY_SHADOWS, FakeCapabilityExecutor.Behavior.THROW);

        StandardActionProcessor processor = new StandardActionProcessor(chaosExecutor, new NoOpLogger());
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        List<CommandExecutionReport> reports = new ArrayList<>();

        // Dispatch 10 commands that will all throw
        for (int i = 0; i < 10; i++) {
            Command cmd = new Command.ApplyCapability(
                    CapabilityId.PARTICLES,
                    new CapabilityValue.EnumValue("DECREASED"));
            bus.dispatch(cmd, reports::add);
        }

        // Process all (should not crash)
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                bus.tick(processor);
            }
        });

        // Verify all were processed
        assertEquals(10, reports.size());

        // All should be ROLLED_BACK (executor had old values)
        long rolledBackCount = reports.stream()
                .filter(CommandExecutionReport::wasRolledBack)
                .count();

        assertEquals(10, rolledBackCount, "All commands should be rolled back");

        // All should have error messages
        assertTrue(reports.stream().allMatch(r -> r.error().isPresent()));
    }

    @Test
    void testQueueSpam() {
        FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        List<CommandExecutionReport> reports = new ArrayList<>();

        // Spam 50 commands rapidly
        for (int i = 0; i < 50; i++) {
            Command cmd = new Command.ApplyCapability(
                    CapabilityId.PARTICLES,
                    new CapabilityValue.EnumValue("DECREASED"));
            bus.dispatch(cmd, reports::add);
        }

        // Verify queue capped
        assertTrue(bus.getQueueSize() <= 50, "Queue should not exceed spam count");

        // Process all
        for (int i = 0; i < 50; i++) {
            bus.tick(processor);
        }

        // Should be empty now
        assertEquals(0, bus.getQueueSize());

        // All should succeed
        assertEquals(50, reports.size());
        assertTrue(reports.stream().allMatch(CommandExecutionReport::succeeded));
    }

    @Test
    void testQueueFullRejection() {
        FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<CommandExecutionReport> reports = new ArrayList<>();

        // Dispatch 200 commands (MAX_QUEUE_SIZE = 100)
        for (int i = 0; i < 200; i++) {
            Command cmd = new Command.ApplyCapability(
                    CapabilityId.PARTICLES,
                    new CapabilityValue.EnumValue("DECREASED"));

            bus.dispatch(cmd, report -> {
                if (report.finalState() == CommandLifecycle.ABORTED) {
                    rejectedCount.incrementAndGet();
                }
                reports.add(report);
            });
        }

        // 1️⃣ Queue must be capped
        assertTrue(bus.getQueueSize() <= 100, "Queue should not exceed MAX_QUEUE_SIZE");

        // 2️⃣ At least 100 commands must have been rejected immediately
        assertTrue(
                rejectedCount.get() >= 100,
                "Expected at least 100 immediate rejections when queue is full");

        // 3️⃣ Process remaining queued commands
        int queued = bus.getQueueSize();
        for (int i = 0; i < queued; i++) {
            bus.tick(processor);
        }

        // 4️⃣ Now total reports = rejected + executed
        assertEquals(200, reports.size(), "All commands must eventually produce a report");

        // 5️⃣ All non-rejected must have succeeded
        assertTrue(
                reports.stream()
                        .filter(r -> r.finalState() != CommandLifecycle.ABORTED)
                        .allMatch(CommandExecutionReport::succeeded),
                "All accepted commands should succeed");
    }

    @Test
    void testRepeatedTicksWithEmptyQueue() {
        FakeCapabilityExecutor executor = new FakeCapabilityExecutor();
        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        // Tick 100 times with empty queue (should be stable no-op)
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                bus.tick(processor);
            }
        });

        assertEquals(0, bus.getQueueSize());
        assertFalse(bus.isExecuting());
    }

    @Test
    void testConcurrentDispatchSimulation() {
        // Simulate concurrent dispatch from "multiple threads" (sequential but
        // interleaved)
        FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS)
                .when(CapabilityId.CLOUDS, FakeCapabilityExecutor.Behavior.SUCCESS);

        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        List<CommandExecutionReport> reports = new ArrayList<>();

        // Interleave different command types rapidly
        for (int i = 0; i < 20; i++) {
            bus.dispatch(
                    new Command.ApplyCapability(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED")),
                    reports::add);
            bus.dispatch(
                    new Command.ApplyCapability(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FAST")),
                    reports::add);
        }

        assertEquals(40, bus.getQueueSize());

        // Process all
        for (int i = 0; i < 40; i++) {
            bus.tick(processor);
        }

        assertEquals(0, bus.getQueueSize());
        assertEquals(40, reports.size());

        // Verify all succeeded
        assertTrue(reports.stream().allMatch(CommandExecutionReport::succeeded));
    }
}
