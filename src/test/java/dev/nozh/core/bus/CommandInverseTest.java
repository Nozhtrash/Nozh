package dev.nozh.core.bus;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandInverseTest {

    @Test
    void inverseUsesBaselineWhenPreviousMissing() {
        Command.ApplyCapability command = new Command.ApplyCapability(
                CapabilityId.RENDER_DISTANCE,
                new CapabilityValue.IntValue(8));

        Optional<Command> inverse = command.inverse(
                Optional.empty(),
                Optional.of(new CapabilityValue.IntValue(12)));

        assertTrue(inverse.isPresent());
        Command.ApplyCapability rollback = (Command.ApplyCapability) inverse.get();
        assertEquals(CapabilityId.RENDER_DISTANCE, rollback.capability());
        assertEquals(12, ((CapabilityValue.IntValue) rollback.value()).value());
    }
}
