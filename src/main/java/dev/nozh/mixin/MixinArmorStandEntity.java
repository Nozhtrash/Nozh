package dev.nozh.mixin;

import dev.nozh.core.render.RenderVisibilityDecider;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.Entity.class)
public class MixinArmorStandEntity {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void nozh$forceInvisible(CallbackInfoReturnable<Boolean> info) {
        if (!((Object) this instanceof ArmorStandEntity)) {
            return;
        }
        if (!RenderVisibilityDecider.isArmorStandVisible()) {
            info.setReturnValue(true);
        }
    }
}
