package com.freskshell.aerobridge.mixin;

import com.freskshell.aerobridge.AeroBridge;
import com.freskshell.aerobridge.portal.CrossingDetector;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.breakinblocks.aeroportals.portal.PortalTeleport", remap = false)
public class MixinPortalTeleport {

    @Inject(method = "executeChainMove", at = @At("HEAD"), remap = false)
    private static void onExecuteChainMove(
            ServerLevel sourceLevel,
            ServerSubLevel subLevel,
            ServerLevel destLevel,
            Vec3 destPos,
            boolean preserveVelocity,
            String portalId,
            CallbackInfoReturnable<Boolean> cir) {

        // APENAS LOGAR, NUNCA BLOQUEAR
        boolean crossing = CrossingDetector.isSubLevelCrossing(subLevel.getUniqueId());
        AeroBridge.LOGGER.info("[AeroBridge] executeChainMove called for sub {} | crossing={} | dest={}",
                subLevel.getUniqueId(), crossing, destLevel.dimension().location());
    }
}
