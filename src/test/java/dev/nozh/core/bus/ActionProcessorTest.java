package dev.nozh.core.bus;

import dev.nozh.core.NoOpLogger;
import dev.nozh.core.state.RuntimeState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ActionProcessor tests with FakeCapabilityExecutor (Contract 2, Paso 2.5).
 * 
 * Tests:
 * 1. Success path
 * 2. Executor throws exception
 * 3. Throw + rollback success
 * 4. Executor "lies" (success without effects)
 * 5. Timestamps coherent
 */
class ActionProcessorTest {

        @Test
        void testSuccessPath() {
                // Setup
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();

                // Execute
                processor.process(cmd, reportRef::set);

                // Verify
                CommandExecutionReport report = reportRef.get();
                assertNotNull(report);
                assertEquals(CommandLifecycle.SUCCESS, report.finalState());
                assertTrue(report.error().isEmpty());
                assertTrue(report.succeeded());
        }

        @Test
        void testExecutorThrows() {
                // Setup
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.THROW);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();

                // Execute (should NOT throw upward)
                assertDoesNotThrow(() -> processor.process(cmd, reportRef::set));

                // Verify
                CommandExecutionReport report = reportRef.get();
                assertNotNull(report);
                assertEquals(CommandLifecycle.FAILED, report.finalState()); // No rollback value set
                assertTrue(report.error().isPresent());
                assertTrue(report.failed());
        }

        @Test
        void testThrowWithRollbackSuccess() {
                // Setup
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .withCurrentValue(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("ALL"))
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.THROW);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();

                // Execute
                processor.process(cmd, reportRef::set);

                // Verify
                CommandExecutionReport report = reportRef.get();
                assertNotNull(report);
                assertEquals(CommandLifecycle.ROLLED_BACK, report.finalState());
                assertTrue(report.error().isPresent());
                assertTrue(report.rollbackReason().isPresent());
                assertTrue(report.wasRolledBack());
        }

        @Test
        void testExecutorLies() {
                // Executor reports SUCCESS but doesn't actually do anything
                // Processor MUST NOT decide - it trusts executor

                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();

                // Execute
                processor.process(cmd, reportRef::set);

                // Verify: Processor TRUSTS executor, reports SUCCESS
                CommandExecutionReport report = reportRef.get();
                assertEquals(CommandLifecycle.SUCCESS, report.finalState());
                // Processor does NOT verify actual effects - that's not its job
        }

        @Test
        void testTimestampsCoherent() {
                FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

                StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());

                Command cmd = new Command.ApplyCapability(
                                CapabilityId.PARTICLES,
                                new CapabilityValue.EnumValue("DECREASED"));

                AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();

                long before = System.currentTimeMillis();
                processor.process(cmd, reportRef::set);
                long after = System.currentTimeMillis();

                CommandExecutionReport report = reportRef.get();

                // Verify timestamp coherence
                assertTrue(report.startedAtMillis() >= before);
                assertTrue(report.finishedAtMillis() <= after);
                assertTrue(report.startedAtMillis() <= report.finishedAtMillis());
        }
}
