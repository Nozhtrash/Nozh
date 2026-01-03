package dev.nozh.mixin;

import dev.nozh.core.input.InputActivityTracker;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void nozh$onMouseButton(long window, int button, int action, int mods, CallbackInfo info) {
        InputActivityTracker.recordMouseInput();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void nozh$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo info) {
        InputActivityTracker.recordMouseInput();
    }
}
