package com.worldbattles.loadingscreens.network;

import com.worldbattles.loadingscreens.WorldBattlesLoadingScreens;
import com.worldbattles.loadingscreens.config.ServerImageConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = WorldBattlesLoadingScreens.MOD_ID)
public final class LoadingScreenNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WorldBattlesLoadingScreens.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final Map<Path, FileStamp> SENT_FILES = new HashMap<>();
    private static boolean registered;
    private static int tickCounter;

    private record FileStamp(long modified, long size) {}

    private LoadingScreenNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(ImageSyncPacket.class, 0)
                .encoder(ImageSyncPacket::encode)
                .decoder(ImageSyncPacket::decode)
                .consumerMainThread(ImageSyncPacket::handle)
                .add();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendAllImages(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tickCounter < 100) return;
        tickCounter = 0;
        Map<ResourceLocation, Path> images = ServerImageConfig.load();
        for (Map.Entry<ResourceLocation, Path> entry : images.entrySet()) {
            Path file = entry.getValue();
            try {
                if (!Files.isRegularFile(file)) continue;
                FileStamp stamp = new FileStamp(Files.getLastModifiedTime(file).toMillis(), Files.size(file));
                FileStamp previous = SENT_FILES.put(file, stamp);
                if (!stamp.equals(previous)) {
                    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) sendImage(player, entry.getKey(), file);
                }
            } catch (Exception exception) {
                WorldBattlesLoadingScreens.LOGGER.error("Could not check loading screen image {}", file, exception);
            }
        }
    }

    private static void sendAllImages(ServerPlayer player) {
        ServerImageConfig.load().forEach((dimension, path) -> sendImage(player, dimension, path));
    }

    private static void sendImage(ServerPlayer player, ResourceLocation dimension, Path image) {
        try {
            if (!Files.isRegularFile(image)) return;
            long size = Files.size(image);
            if (size <= 0 || size > ImageSyncPacket.MAX_IMAGE_BYTES) return;
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new ImageSyncPacket(dimension, Files.readAllBytes(image)));
            SENT_FILES.put(image, new FileStamp(Files.getLastModifiedTime(image).toMillis(), size));
        } catch (Exception exception) {
            WorldBattlesLoadingScreens.LOGGER.error("Could not send loading screen image {}", image, exception);
        }
    }
}
