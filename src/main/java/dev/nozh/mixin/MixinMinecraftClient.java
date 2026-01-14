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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /**
     * Dynamic FPS Limiter (Background Saver).
     * Reduces FPS to 5 when the window is not focused to save system resources.
     */
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void nozh$getFramerateLimit(CallbackInfoReturnable<Integer> circles) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!client.isWindowFocused()) {
            // Background FPS limit: 5 FPS
            circles.setReturnValue(5);
        }
    }
}
