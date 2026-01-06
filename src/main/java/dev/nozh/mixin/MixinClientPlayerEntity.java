package dev.nozh.mixin;

import dev.nozh.client.NozhModClient;
import dev.nozh.fabric.context.FabricScenarioDetector;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "damage", at = @At("HEAD"))
    private void nozh$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (amount <= 0) {
            return;
        }
        FabricScenarioDetector detector = NozhModClient.getScenarioDetector();
        if (detector != null) {
            detector.recordDamage();
        }
    }
}
