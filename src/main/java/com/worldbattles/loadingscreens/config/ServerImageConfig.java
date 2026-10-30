package com.worldbattles.loadingscreens.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerImageConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
    private static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("worldbattles_loading_screens");
    private static final Path JSON = ROOT.resolve("config.json");

    private ServerImageConfig() {}

    public static Map<ResourceLocation, Path> load() {
        try {
            Files.createDirectories(ROOT);
            if (!Files.exists(JSON)) {
                Map<String, String> defaults = new LinkedHashMap<>();
                defaults.put("minecraft:overworld", "overworld.png");
                defaults.put("minecraft:the_nether", "nether.png");
                defaults.put("minecraft:the_end", "end.png");
                defaults.put("secondworld:overworld", "freelands.png");
                defaults.put("secondworld:the_nether", "freelands_nether.png");
                defaults.put("worldbattles:minage", "minage.png");
                defaults.put("worldbattles:minage_nether", "minage_nether.png");
                Files.writeString(JSON, GSON.toJson(defaults));
            }
            try (Reader reader = Files.newBufferedReader(JSON)) {
                Map<String, String> raw = GSON.fromJson(reader, MAP_TYPE);
                Map<ResourceLocation, Path> result = new LinkedHashMap<>();
                if (raw != null) raw.forEach((dimension, file) -> {
                    try {
                        Path resolved = ROOT.resolve(file).normalize();
                        if (!resolved.startsWith(ROOT)) throw new IllegalArgumentException("Image path escapes config directory");
                        result.put(new ResourceLocation(dimension), resolved);
                    } catch (Exception ignored) { }
                });
                return result;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read WorldBattles loading-screen config", e);
        }
    }
}
