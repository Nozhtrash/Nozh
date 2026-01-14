package dev.nozh.mixin;

import dev.nozh.client.NozhModClient;
import dev.nozh.core.profiler.PerfManager;
import dev.nozh.core.profiler.RenderPhase;
import dev.nozh.core.render.RenderVisibilityDecider;
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
        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderPhaseStart(RenderPhase.ENTITIES);
        }

        // Armor Stands
        if (entity instanceof ArmorStandEntity && !RenderVisibilityDecider.isArmorStandVisible()) {
            if (perfManager != null) {
                perfManager.onRenderPhaseEnd(RenderPhase.ENTITIES);
            }
            ci.cancel();
            return;
        }

        // Item Frames (and Glowing Item Frames which inherit)
        if (entity instanceof ItemFrameEntity && !RenderVisibilityDecider.isItemFrameVisible()) {
            if (perfManager != null) {
                perfManager.onRenderPhaseEnd(RenderPhase.ENTITIES);
            }
            ci.cancel();
            return;
        }

        // Native Occlusion Culling (New in v1.1.0)
        // If external "EntityCulling" mod is NOT present, we handle it natively.
        if (!dev.nozh.fabric.compat.EntityCullingAdapter.shouldDeferCulling()) {
            if (dev.nozh.core.render.EntityOcclusionCuller.shouldSkipRender(entity)) {
                if (perfManager != null) {
                    perfManager.onRenderPhaseEnd(RenderPhase.ENTITIES);
                }
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private <E extends Entity> void onRenderReturn(E entity, double x, double y, double z, float yaw, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderPhaseEnd(RenderPhase.ENTITIES);
        }
    }
}
