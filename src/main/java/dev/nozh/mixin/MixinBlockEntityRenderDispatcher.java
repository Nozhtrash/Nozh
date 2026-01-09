package dev.nozh.mixin;

import dev.nozh.client.NozhModClient;
import dev.nozh.core.profiler.PerfManager;
import dev.nozh.core.profiler.RenderPhase;
import dev.nozh.core.settings.NozhRenderSettings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    public <E extends BlockEntity> void onRender(E blockEntity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, CallbackInfo ci) {

        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderPhaseStart(RenderPhase.BLOCK_ENTITIES);
        }

        if (!NozhRenderSettings.isBlockEntitiesVisible()) {
            if (perfManager != null) {
                perfManager.onRenderPhaseEnd(RenderPhase.BLOCK_ENTITIES);
            }
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("RETURN"))
    public <E extends BlockEntity> void onRenderReturn(E blockEntity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        PerfManager perfManager = NozhModClient.getPerfManager();
        if (perfManager != null) {
            perfManager.onRenderPhaseEnd(RenderPhase.BLOCK_ENTITIES);
        }
    }
}
