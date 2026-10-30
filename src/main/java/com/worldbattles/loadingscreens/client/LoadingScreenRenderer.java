package com.worldbattles.loadingscreens.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.worldbattles.loadingscreens.WorldBattlesLoadingScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = WorldBattlesLoadingScreens.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LoadingScreenRenderer {
    private static final Map<ResourceLocation, ResourceLocation> TEXTURES = new HashMap<>();
    private static ResourceLocation activeDimension;

    private LoadingScreenRenderer() {}

    public static void invalidate(ResourceLocation dimension) {
        ResourceLocation texture = TEXTURES.remove(dimension);
        if (texture != null) Minecraft.getInstance().getTextureManager().release(texture);
    }

    @SubscribeEvent
    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        ResourceLocation dimension = minecraft.level.dimension().location();
        activeDimension = dimension;
        ResourceLocation texture = getOrLoadTexture(dimension);
        if (texture == null) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        RenderSystem.enableBlend();
        event.getGuiGraphics().blit(texture, 0, 0, 0, 0, width, height, width, height);
        RenderSystem.disableBlend();
    }

    private static ResourceLocation getOrLoadTexture(ResourceLocation dimension) {
        ResourceLocation cached = TEXTURES.get(dimension);
        if (cached != null) return cached;

        String safeName = dimension.getNamespace() + "_" + dimension.getPath().replace('/', '_');
        Path file = Path.of("config", WorldBattlesLoadingScreens.MOD_ID, "cache", safeName + ".png");
        if (!Files.isRegularFile(file)) return null;

        try (InputStream input = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() > 4096 || image.getHeight() > 4096) {
                image.close();
                throw new IllegalArgumentException("Loading screen image dimensions exceed 4096x4096");
            }
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation id = new ResourceLocation(WorldBattlesLoadingScreens.MOD_ID, "synced/" + safeName);
            Minecraft.getInstance().getTextureManager().register(id, dynamicTexture);
            TEXTURES.put(dimension, id);
            return id;
        } catch (Exception exception) {
            WorldBattlesLoadingScreens.LOGGER.error("Could not load custom loading screen for {}", dimension, exception);
            return null;
        }
    }
}
