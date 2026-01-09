package dev.nozh.mixin;

import dev.nozh.client.NozhModClient;
import dev.nozh.fabric.context.FabricScenarioDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void nozh$setScreen(Screen screen, CallbackInfo info) {
        if (!(screen instanceof HandledScreen<?>)) {
            return;
        }
        FabricScenarioDetector detector = NozhModClient.getScenarioDetector();
        if (detector != null) {
            detector.recordInventoryOpen();
        }
    }
}
