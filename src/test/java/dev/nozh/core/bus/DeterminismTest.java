package dev.nozh.core.bus;

import dev.nozh.core.NoOpLogger;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.TestStates;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Determinism test for Contract 2 (mandatory for closure).
 * 
 * Validates that ActionBus + Processor produce deterministic results:
 * - Same input sequence → same structural output
 * - Only timestamps may vary
 * - Command order preserved
 * - Report states identical
 * 
 * This test is CRITICAL for Governor reliability.
 */
class DeterminismTest {

    private static final RuntimeState FAKE_STATE = TestStates.autoTuningEnabled();

    @Test
    void testRepeatedExecutionIsDeterministic() {
        // Define test sequence
        List<Command> sequence = List.of(
                new Command.ApplyCapability(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED")),
                new Command.ApplyCapability(CapabilityId.CLOUDS, new CapabilityValue.EnumValue("FAST")),
                new Command.ApplyCapability(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false)),
                new Command.ApplyCapability(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(12)));

        // Run 1
        List<CommandExecutionReport> run1 = executeSequence(sequence);

        // Run 2
        List<CommandExecutionReport> run2 = executeSequence(sequence);

        // Verify determinism
        assertEquals(run1.size(), run2.size(), "Report count must be identical");

        for (int i = 0; i < run1.size(); i++) {
            CommandExecutionReport r1 = run1.get(i);
            CommandExecutionReport r2 = run2.get(i);

            // Structural data must be identical
            assertEquals(r1.commandId(), r2.commandId(), "Command ID must match");
            assertEquals(r1.type(), r2.type(), "Command type must match");
            assertEquals(r1.capability(), r2.capability(), "Capability must match");
            assertEquals(r1.finalState(), r2.finalState(), "Final state must match");
            assertEquals(r1.error(), r2.error(), "Error must match");
            assertEquals(r1.rollbackReason(), r2.rollbackReason(), "Rollback reason must match");

            // Timestamps MAY differ (system-dependent)
            // But relative order must be preserved
            assertTrue(r1.queuedAtMillis() <= r1.startedAtMillis());
            assertTrue(r2.queuedAtMillis() <= r2.startedAtMillis());
        }
    }

    @Test
    void testExecutorBehaviorDoesNotAffectDeterminism() {
        // Same sequence, different executor behaviors
        FakeCapabilityExecutor successExecutor = new FakeCapabilityExecutor()
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS);

        FakeCapabilityExecutor throwExecutor = new FakeCapabilityExecutor()
                .withCurrentValue(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("ALL"))
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.THROW);

        StandardActionProcessor successProcessor = new StandardActionProcessor(successExecutor, new NoOpLogger());
        StandardActionProcessor throwProcessor = new StandardActionProcessor(throwExecutor, new NoOpLogger());

        ActionBus bus1 = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);
        ActionBus bus2 = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        Command cmd = new Command.ApplyCapability(
                CapabilityId.PARTICLES,
                new CapabilityValue.EnumValue("DECREASED"));

        List<CommandExecutionReport> reports1 = new ArrayList<>();
        List<CommandExecutionReport> reports2 = new ArrayList<>();

        bus1.dispatch(cmd, reports1::add);
        bus1.tick(successProcessor);

        bus2.dispatch(cmd, reports2::add);
        bus2.tick(throwProcessor);

        // Verify different outcomes are consistent with executor behavior
        assertEquals(CommandLifecycle.SUCCESS, reports1.get(0).finalState());
        assertEquals(CommandLifecycle.ROLLED_BACK, reports2.get(0).finalState());

        // But structural properties are deterministic
        assertEquals(reports1.get(0).commandId(), reports2.get(0).commandId());
        assertEquals(reports1.get(0).type(), reports2.get(0).type());
    }

    @Test
    void testOrderingIsDeterministic() {
        // Multiple runs with same sequence
        List<Command> sequence = List.of(
                new Command.ApplyCapability(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("MINIMAL")),
                new Command.ApplyCapability(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("DECREASED")),
                new Command.ApplyCapability(CapabilityId.PARTICLES, new CapabilityValue.EnumValue("ALL")));

        List<CommandExecutionReport> run1 = executeSequence(sequence);
        List<CommandExecutionReport> run2 = executeSequence(sequence);
        List<CommandExecutionReport> run3 = executeSequence(sequence);

        // All runs must have identical command order
        for (int i = 0; i < sequence.size(); i++) {
            assertEquals(run1.get(i).commandId(), run2.get(i).commandId());
            assertEquals(run2.get(i).commandId(), run3.get(i).commandId());
            assertEquals(sequence.get(i).id(), run1.get(i).commandId());
        }
    }

    private List<CommandExecutionReport> executeSequence(List<Command> commands) {
        FakeCapabilityExecutor executor = new FakeCapabilityExecutor()
                .when(CapabilityId.PARTICLES, FakeCapabilityExecutor.Behavior.SUCCESS)
                .when(CapabilityId.CLOUDS, FakeCapabilityExecutor.Behavior.SUCCESS)
                .when(CapabilityId.ENTITY_SHADOWS, FakeCapabilityExecutor.Behavior.SUCCESS)
                .when(CapabilityId.RENDER_DISTANCE, FakeCapabilityExecutor.Behavior.SUCCESS);

        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> FAKE_STATE);

        List<CommandExecutionReport> reports = new ArrayList<>();

        commands.forEach(cmd -> bus.dispatch(cmd, reports::add));

        // Process all commands
        for (int i = 0; i < commands.size(); i++) {
            bus.tick(processor);
        }

        return reports;
    }
}
