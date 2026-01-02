package dev.nozh.mixin;

import dev.nozh.core.settings.NozhRenderSettings;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}
