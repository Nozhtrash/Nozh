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
     * Reduces FPS when the window is not focused to save system resources.
     * Configurable via nozh config 'backgroundFpsLimit'.
     */
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void nozh$getFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!client.isWindowFocused()) {
            int limit = dev.nozh.core.config.ConfigManager.getConfig().backgroundFpsLimit;
            cir.setReturnValue(limit);
        }
    }
}
