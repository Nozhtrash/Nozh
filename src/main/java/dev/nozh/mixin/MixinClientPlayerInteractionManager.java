package dev.nozh.mixin;

import dev.nozh.client.NozhModClient;
import dev.nozh.fabric.context.FabricScenarioDetector;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void nozh$attackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        FabricScenarioDetector detector = NozhModClient.getScenarioDetector();
        if (detector != null) {
            detector.recordAttack();
        }
    }

    @Inject(method = "attackBlock", at = @At("RETURN"))
    private void nozh$attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        if (!Boolean.TRUE.equals(info.getReturnValue())) {
            return;
        }
        FabricScenarioDetector detector = NozhModClient.getScenarioDetector();
        if (detector != null) {
            detector.recordBlockBroken();
        }
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void nozh$interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                    CallbackInfoReturnable<ActionResult> info) {
        ActionResult result = info.getReturnValue();
        if (result == null || !result.isAccepted()) {
            return;
        }
        FabricScenarioDetector detector = NozhModClient.getScenarioDetector();
        if (detector != null) {
            detector.recordBlockPlaced();
        }
    }
}
