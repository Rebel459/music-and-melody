package net.rebel459.music_and_melody.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.util.GsonHelper;
import net.rebel459.music_and_melody.MusicAndMelody;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class CustomSounds {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path FILE = Path.of("config", MusicAndMelody.MOD_ID, "sounds.json");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer())
            .create();
    private static final TypeToken<Map<String, SoundEventRegistration>> TYPE = new TypeToken<>() {};

    private CustomSounds() {}

    public static Map<String, SoundEventRegistration> load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.notExists(FILE)) Files.writeString(FILE, "{}\n", StandardCharsets.UTF_8);
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                Map<String, SoundEventRegistration> registrations = GsonHelper.fromJson(GSON, reader, TYPE);
                return registrations == null ? Map.of() : registrations;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not load config sounds file '{}'.", FILE, exception);
            return Map.of();
        }
    }
}
