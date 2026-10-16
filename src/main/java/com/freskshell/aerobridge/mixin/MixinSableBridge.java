package com.freskshell.aerobridge.mixin;

import com.freskshell.aerobridge.AeroBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin para interceptar SableBridge do Sable.
 * Aqui vamos controlar quando grupos de subníveis podem ser movidos.
 */
@Mixin(targets = "dev.ryanhcode.sable.api.SableBridge", remap = false)
public class MixinSableBridge {

    /**
     * Intercepta o método moveGroup para controlar transferências.
     */
    @Inject(method = "moveGroup", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onMoveGroup(CallbackInfoReturnable<Boolean> cir) {
        AeroBridge.LOGGER.debug("[AeroBridge] Intercepted SableBridge.moveGroup");
        // TODO: Implementar lógica para controlar quando moveGroup é permitido
    }
}
