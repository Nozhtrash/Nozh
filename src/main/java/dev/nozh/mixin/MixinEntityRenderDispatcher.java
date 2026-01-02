package dev.nozh.mixin;

import dev.nozh.core.settings.NozhRenderSettings;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRender(E entity, double x, double y, double z, float yaw, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            CallbackInfo ci) {

        // Armor Stands
        if (entity instanceof ArmorStandEntity && !NozhRenderSettings.isArmorStandsVisible()) {
            ci.cancel();
            return;
        }

        // Item Frames (and Glowing Item Frames which inherit)
        if (entity instanceof ItemFrameEntity && !NozhRenderSettings.isItemFramesVisible()) {
            ci.cancel();
            return;
        }
    }
}
