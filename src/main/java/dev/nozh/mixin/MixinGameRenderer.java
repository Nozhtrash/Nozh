package dev.nozh.mixin;

import dev.nozh.core.render.RenderVisibilityDecider;
import dev.nozh.client.NozhModClient;
import dev.nozh.core.profiler.PerfManager;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void nozh$recordFrame(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        RenderVisibilityDecider.recordGameRendererFrame();
        // Note: onRenderFrameStart is called by WorldRenderEvents.START callback in
        // NozhModClient
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void nozh$finalizeFrame(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderFrameEnd();
        }
    }
}
