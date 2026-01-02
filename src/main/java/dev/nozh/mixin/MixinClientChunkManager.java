package dev.nozh.mixin;

import dev.nozh.core.monitoring.NozhChunkMonitor;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkManager.class)
public class MixinClientChunkManager {

    /*
     * Hook into chunk loading (from packet).
     * Exact method name may vary by mapping, but loadChunkFromPacket is standard
     * intermediary.
     */
    @Inject(method = "loadChunkFromPacket", at = @At("RETURN"))
    private void onChunkLoad(int x, int z, ChunkDataS2CPacket packet, CallbackInfoReturnable<WorldChunk> cir) {
        NozhChunkMonitor.onChunkLoad();
    }

    /*
     * Hook into chunk unloading.
     */
    @Inject(method = "unload", at = @At("HEAD"))
    private void onChunkUnload(int x, int z, CallbackInfo ci) {
        NozhChunkMonitor.onChunkUnload();
    }
}
