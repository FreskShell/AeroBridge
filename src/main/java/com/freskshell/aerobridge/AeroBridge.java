package com.freskshell.aerobridge;

import com.freskshell.aerobridge.config.AeroBridgeConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AeroBridge.MOD_ID)
public class AeroBridge {
    public static final String MOD_ID = "aerobridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public AeroBridge(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("AeroBridge initializing...");

        modContainer.registerConfig(ModConfig.Type.COMMON, AeroBridgeConfig.SPEC);

        modEventBus.addListener(this::onCommonSetup);

        LOGGER.info("AeroBridge initialized successfully");
    }

    private void onCommonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("AeroBridge common setup");
    }
}
