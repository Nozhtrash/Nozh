package dev.nozh.mixin;

import dev.nozh.client.NozhModClient;
import dev.nozh.core.profiler.PerfManager;
import dev.nozh.core.profiler.RenderPhase;
import dev.nozh.core.settings.NozhRenderSettings;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.Camera;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class MixinParticleManager {

    /**
     * Prevents particles from being added if animations/particles are globally
     * disabled.
     * This saves huge resources in mass-event scenarios.
     */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    public void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, CallbackInfoReturnable<?> cir) {
        if (!NozhRenderSettings.isAllAnimationsVisible()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void nozh$renderParticlesStart(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta, CallbackInfo ci) {
        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderPhaseStart(RenderPhase.PARTICLES);
        }
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void nozh$renderParticlesEnd(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta, CallbackInfo ci) {
        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderPhaseEnd(RenderPhase.PARTICLES);
        }
    }
}
