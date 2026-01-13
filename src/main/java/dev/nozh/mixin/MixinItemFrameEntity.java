package dev.nozh.mixin;

import dev.nozh.core.render.RenderVisibilityDecider;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.Entity.class)
public class MixinItemFrameEntity {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void nozh$forceInvisible(CallbackInfoReturnable<Boolean> info) {
        if (!((Object) this instanceof ItemFrameEntity)) {
            return;
        }
        if (!RenderVisibilityDecider.isItemFrameVisible()) {
            info.setReturnValue(true);
        }
    }
}
