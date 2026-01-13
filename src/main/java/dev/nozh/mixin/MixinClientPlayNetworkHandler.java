package dev.nozh.mixin;

import dev.nozh.NozhConstants;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept server time updates for TPS estimation.
 * 
 * The server sends WorldTimeUpdateS2CPacket every tick (20 times per second).
 * By measuring the time between these packets, we can estimate server TPS.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    /**
     * Intercept world time update packets to track server tick timing.
     */
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private void nozh$onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        try {
            // Notify the server performance detector
            NozhConstants.notifyServerTick();
        } catch (Exception e) {
            // Silently ignore - don't crash the game
        }
    }
}
