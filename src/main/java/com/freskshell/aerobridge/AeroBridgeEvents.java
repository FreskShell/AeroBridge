package com.freskshell.aerobridge;

import com.breakinblocks.aeroportals.api.SubLevelPreTransferEvent;
import com.freskshell.aerobridge.portal.PortalRegistry;
import com.freskshell.aerobridge.portal.SubLevelTracker;
import com.freskshell.aerobridge.portal.TransferGate;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Listener principal. Registrado SOMENTE por @EventBusSubscriber (não registrar de novo
 * no construtor, senão cada tick era processado duas vezes e previous == current).
 */
@EventBusSubscriber(modid = AeroBridge.MOD_ID)
public class AeroBridgeEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SubLevelTracker.tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPreTransfer(SubLevelPreTransferEvent event) {
        TransferGate.onPreTransfer(event);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AeroBridgeCommands.register(event.getDispatcher());
    }

    /** Limpa só o estado da dimensão descarregada. */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SubLevelTracker.clearDimension(serverLevel);
            PortalRegistry.invalidate(serverLevel);
            AeroBridge.LOGGER.debug("[AeroBridge] Level unloaded: {}", serverLevel.dimension().location());
        }
    }

    /** Estáticos sobrevivem entre mundos no singleplayer: zera tudo ao parar o servidor. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SubLevelTracker.clearAll();
        PortalRegistry.clearAll();
    }
}
