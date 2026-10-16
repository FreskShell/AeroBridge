package com.freskshell.aerobridge.mixin;

import com.freskshell.aerobridge.AeroBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para interceptar teletransporte de jogadores.
 * Aqui vamos implementar o teletransporte instantâneo.
 */
@Mixin(ServerPlayer.class)
public class MixinServerPlayer {

    /**
     * Intercepta o método teleportTo para implementar teletransporte instantâneo.
     */
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"))
    private void onTeleportTo(double x, double y, double z, CallbackInfo ci) {
        AeroBridge.LOGGER.debug("[AeroBridge] Player teleporting to {}, {}, {}", x, y, z);
        // TODO: Implementar lógica de teletransporte instantâneo
    }
}
