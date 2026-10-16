package com.freskshell.aerobridge;

import com.freskshell.aerobridge.portal.PortalStateManager;
import net.minecraft.server.level.ServerLevel;
import com.freskshell.aerobridge.portal.SubLevelTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Listener principal de eventos do AeroBridge.
 * Responsável por rastrear subníveis e processar travessias de portais.
 */
@EventBusSubscriber(modid = AeroBridge.MOD_ID)
public class AeroBridgeEvents {

    /**
     * Chamado a cada tick do servidor.
     * Aqui vamos rastrear a pose de todos os subníveis ativos.
     */
    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level instanceof ServerLevel serverLevel) {
            // Chama o tracker para atualizar as poses dos subníveis
            SubLevelTracker.tick(serverLevel);
        }
    }

    /**
     * Chamado quando uma entidade entra em um nível.
     * Útil para detectar quando jogadores entram em portais.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();

        if (entity instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            AeroBridge.LOGGER.debug("[AeroBridge] Player {} joined level {}",
                player.getName().getString(),
                serverLevel.dimension().location());
        }
    }

    /**
     * Chamado quando um nível é carregado.
     * Inicializa o estado do PortalStateManager.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();

        if (levelAccessor instanceof ServerLevel serverLevel) {
            AeroBridge.LOGGER.info("[AeroBridge] Level loaded: {}",
                serverLevel.dimension().location());
        }
    }

    /**
     * Chamado quando um nível é descarregado.
     * Limpa o estado do PortalStateManager.
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();

        if (levelAccessor instanceof ServerLevel serverLevel) {
            PortalStateManager.clearAllStates();
            AeroBridge.LOGGER.info("[AeroBridge] Level unloaded: {}",
                serverLevel.dimension().location());
        }
    }
}
