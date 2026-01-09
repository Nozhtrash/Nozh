package dev.nozh.mixin;

import dev.nozh.core.render.RenderVisibilityDecider;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public class MixinItemFrameEntity {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void nozh$forceInvisible(CallbackInfoReturnable<Boolean> info) {
        if (!RenderVisibilityDecider.isItemFrameVisible()) {
            info.setReturnValue(true);
        }
    }
}
