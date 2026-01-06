package dev.nozh.mixin;

import dev.nozh.core.render.RenderVisibilityDecider;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public class MixinArmorStandEntity {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void nozh$forceInvisible(CallbackInfoReturnable<Boolean> info) {
        if (!RenderVisibilityDecider.isArmorStandVisible()) {
            info.setReturnValue(true);
        }
    }
}
