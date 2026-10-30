package com.worldbattles.loadingscreens;

import com.mojang.logging.LogUtils;
import com.worldbattles.loadingscreens.network.LoadingScreenNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(WorldBattlesLoadingScreens.MOD_ID)
public final class WorldBattlesLoadingScreens {
    public static final String MOD_ID = "worldbattles_loading_screens";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldBattlesLoadingScreens() {
        LoadingScreenNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("WorldBattles Loading Screens is ready. Server image folder: config/worldbattles_loading_screens");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("WorldBattles Loading Screens stopped.");
    }
}
