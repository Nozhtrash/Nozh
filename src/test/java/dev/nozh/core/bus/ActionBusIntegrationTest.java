package dev.nozh.core.bus;

import dev.nozh.core.NoOpLogger;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.TestStates;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ActionBus integration test (Contract 2, mandatory for closure).
 * 
 * Tests Bus → Processor interaction:
 * 1. dispatch → tick → processor invoked
 * 2. Lifecycle complete
 * 3. Timestamps coherent (queued ≤ started ≤ finished)
 * 4. Order preserved with multiple dispatch
 * 5. tick without commands = no-op
 */
class ActionBusIntegrationTest {

        // Fake state provider for tests (Contract 2 purity)
        private static final RuntimeState FAKE_STATE = TestStates.autoTuningEnabled();

        @Test
        void testDispatchTickProcessorFlow() {
                // Setup
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
                ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                List<CommandExecutionReport> reports = new ArrayList<>();

                // Dispatch
                bus.dispatch(cmd, reports::add);

                // Verify queued but not executed yet
                assertEquals(1, bus.getQueueSize());
                assertTrue(reports.isEmpty());

                // Tick (processes command)
                bus.tick(processor);

                // Verify executed
                assertEquals(0, bus.getQueueSize());
                assertEquals(1, reports.size());

                CommandExecutionReport report = reports.get(0);
                assertEquals(CommandLifecycle.SUCCESS, report.finalState());
                assertEquals(cmd.id(), report.commandId());
        }

        @Test
        void testTimestampsCoherent() {
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
                ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                List<CommandExecutionReport> reports = new ArrayList<>();

                long beforeDispatch = System.currentTimeMillis();
                bus.dispatch(cmd, reports::add);
                bus.tick(processor);
                long afterTick = System.currentTimeMillis();

                CommandExecutionReport report = reports.get(0);

                // Verify timestamp coherence: queued ≤ started ≤ finished
                assertTrue(report.queuedAtMillis() >= beforeDispatch);
                assertTrue(report.queuedAtMillis() <= report.startedAtMillis());
                assertTrue(report.startedAtMillis() <= report.finishedAtMillis());
                assertTrue(report.finishedAtMillis() <= afterTick);

                // Verify latency methods
                assertTrue(report.queueLatencyMillis() >= 0);
                assertTrue(report.executionDurationMillis() >= 0);
                assertTrue(report.totalLatencyMillis() >= 0);
        }

        @Test
        void testOrderPreservedWithMultipleDispatch() {
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS)
                                .when(CapabilityId.CLOUDS, FakeCapabilityExecutor.Behavior.SUCCESS)
                                .when(CapabilityId.ENTITY_SHADOWS, FakeCapabilityExecutor.Behavior.SUCCESS);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
                ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

                List<Command> commands = List.of(
                                new Command.ApplyCapability(CapabilityId.PARTICLES,
                                                new CapabilityValue.EnumValue("DECREASED")),
                                new Command.ApplyCapability(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FAST")),
                                new Command.ApplyCapability(CapabilityId.ENTITY_SHADOWS,
                                                new CapabilityValue.BoolValue(false)));

                List<CommandExecutionReport> reports = new ArrayList<>();

                // Dispatch all
                commands.forEach(cmd -> bus.dispatch(cmd, reports::add));

                assertEquals(3, bus.getQueueSize());

                // Tick 3 times (1 command per tick)
                bus.tick(processor);
                assertEquals(2, bus.getQueueSize());
                assertEquals(1, reports.size());

                bus.tick(processor);
                assertEquals(1, bus.getQueueSize());
                assertEquals(2, reports.size());

                bus.tick(processor);
                assertEquals(0, bus.getQueueSize());
                assertEquals(3, reports.size());

                // Verify order preserved
                assertEquals(commands.get(0).id(), reports.get(0).commandId());
                assertEquals(commands.get(1).id(), reports.get(1).commandId());
                assertEquals(commands.get(2).id(), reports.get(2).commandId());
        }

        @Test
        void testTickWithoutCommands() {
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor();
                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
                ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

                // Tick with empty queue (should be no-op, no crash)
                assertDoesNotThrow(() -> bus.tick(processor));
                assertEquals(0, bus.getQueueSize());
                assertFalse(bus.isExecuting());
        }

        @Test
        void testInvalidCommandRejectedImmediately() {
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor();
                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
                ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

                // Command with invalid value (particles needs EnumValue, not IntValue)
                Command invalidCmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.IntValue(5) // Wrong type!
                );

                List<CommandExecutionReport> reports = new ArrayList<>();

                // Dispatch invalid command
                bus.dispatch(invalidCmd, reports::add);

                // Verify immediate rejection (not queued)
                assertEquals(0, bus.getQueueSize());
                assertEquals(1, reports.size());

                CommandExecutionReport report = reports.get(0);
                assertEquals(CommandLifecycle.ABORTED, report.finalState());
                assertTrue(report.error().isPresent());
                assertTrue(report.error().get().contains("requires EnumValue"));
        }
}
