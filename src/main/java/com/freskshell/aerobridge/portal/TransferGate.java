package com.freskshell.aerobridge.portal;

import com.breakinblocks.aeroportals.api.SubLevelPreTransferEvent;
import com.freskshell.aerobridge.AeroBridge;
import com.freskshell.aerobridge.config.AeroBridgeConfig;
import net.minecraft.world.phys.Vec3;

/**
 * Porteiro da transferência do AeroPortals. Com holdTransferUntilCenterCrosses ligado,
 * a transferência só é liberada quando o centro da OBB do sublevel passa do plano do portal
 * (antes disso o AeroPortals transfere no primeiro contato).
 * Só age em portais do Nether que o PortalRegistry conhece; outros tipos passam direto.
 */
public final class TransferGate {

    private TransferGate() {}

    public static void onPreTransfer(SubLevelPreTransferEvent event) {
        SubLevelTracker.TrackedSubLevel t = SubLevelTracker.get(event.sub().getUniqueId());
        if (t == null) return;

        long now = event.srcLevel().getGameTime();
        boolean knowsPortal = t.enteredRect != null
                || !PortalRegistry.peek(t.dimension, t.id).isEmpty();
        boolean centerCrossed = CrossingDetector.centerCrossed(t);
        Vec3 pos = t.currentPosition;
        String where = String.format("pos=%s dest=%s originalDest=%s crossed=%.0f%% centerCrossed=%s",
                pos, event.destination(), event.originalDestination(),
                CrossingDetector.crossedFraction(t) * 100.0, centerCrossed);

        if (!AeroBridgeConfig.HOLD_TRANSFER_UNTIL_CENTER_CROSSES.get() || !knowsPortal) {
            AeroBridge.LOGGER.info("[AeroBridge] PreTransfer sub {} {} -> {} ({}) | liberado (sem hold) | {}",
                    t.id, event.srcLevel().dimension().location(), event.dstLevel().dimension().location(),
                    event.label(), where);
            return;
        }

        if (centerCrossed) {
            t.holdSinceTick = -1;
            AeroBridge.LOGGER.info("[AeroBridge] PreTransfer sub {} -> {} | liberado: centro cruzou | {}",
                    t.id, event.dstLevel().dimension().location(), where);
            return;
        }

        if (t.holdSinceTick < 0) t.holdSinceTick = now;
        if (now - t.holdSinceTick > AeroBridgeConfig.MAX_HOLD_TICKS.get()) {
            t.holdSinceTick = -1;
            AeroBridge.LOGGER.warn("[AeroBridge] PreTransfer sub {} | hold excedeu {} ticks, liberando | {}",
                    t.id, AeroBridgeConfig.MAX_HOLD_TICKS.get(), where);
            return;
        }

        AeroBridge.LOGGER.debug("[AeroBridge] PreTransfer sub {} | segurando | {}", t.id, where);
        event.cancel("aerobridge: aguardando o centro cruzar o portal");
    }
}
