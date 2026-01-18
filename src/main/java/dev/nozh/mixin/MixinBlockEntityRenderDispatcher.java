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

        // 1. Hard Switch (existing)
        if (!NozhRenderSettings.isBlockEntitiesVisible()) {
            if (perfManager != null) {
                perfManager.onRenderPhaseEnd(RenderPhase.BLOCK_ENTITIES);
            }
            ci.cancel();
            return;
        }

        // 2. Potato Mode Deep Culling (Phase 3)
        // We check Config directly or via a cached helper.
        // For speed, we assume ConfigManager access is fast or cached.
        if (dev.nozh.core.config.ConfigManager.getConfig().potatoModeEnabled) {
            // Calculate distance
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.player != null) {
                double distSq = blockEntity.getPos().getSquaredDistance(client.player.getPos());
                // Threshold: configurable blocks squared for culling
                double thresholdSq = dev.nozh.core.config.ConfigManager.getConfig()
                        .potatoModeBlockEntityCullDistanceSq();
                if (distSq > thresholdSq) {
                    // Exception: Beacons, End Gateways, Portals (keep visible further)
                    // Using instanceof for faster type-safe checks
                    if (!(blockEntity instanceof net.minecraft.block.entity.BeaconBlockEntity)
                            && !(blockEntity instanceof net.minecraft.block.entity.EndGatewayBlockEntity)
                            && !(blockEntity instanceof net.minecraft.block.entity.EndPortalBlockEntity)) {
                        if (perfManager != null)
                            perfManager.onRenderPhaseEnd(RenderPhase.BLOCK_ENTITIES);
                        ci.cancel();
                        return;
                    }
                }
            }
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
