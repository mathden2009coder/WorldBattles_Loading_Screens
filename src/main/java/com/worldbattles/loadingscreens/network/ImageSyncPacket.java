package com.worldbattles.loadingscreens.network;

import com.worldbattles.loadingscreens.WorldBattlesLoadingScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ImageSyncPacket {
    public static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;
    private final ResourceLocation dimension;
    private final byte[] image;

    public ImageSyncPacket(ResourceLocation dimension, byte[] image) {
        if (image.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image too large");
        this.dimension = dimension;
        this.image = image;
    }

    public static void encode(ImageSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeByteArray(packet.image);
    }

    public static ImageSyncPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        return new ImageSyncPacket(dimension, buffer.readByteArray(MAX_IMAGE_BYTES));
    }

    public static void handle(ImageSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> saveOnClient(packet)));
        context.setPacketHandled(true);
    }

    private static void saveOnClient(ImageSyncPacket packet) {
        try {
            Path directory = Path.of("config", WorldBattlesLoadingScreens.MOD_ID, "cache");
            Files.createDirectories(directory);
            String safeName = packet.dimension.getNamespace() + "_" + packet.dimension.getPath().replace('/', '_');
            Files.write(directory.resolve(safeName + ".png"), packet.image);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.worldbattles.loadingscreens.client.LoadingScreenRenderer.invalidate(packet.dimension));
            WorldBattlesLoadingScreens.LOGGER.info("Received loading screen image for {} ({} bytes)", packet.dimension, packet.image.length);
        } catch (IOException e) {
            WorldBattlesLoadingScreens.LOGGER.error("Could not save loading screen image for {}", packet.dimension, e);
        }
    }
}
