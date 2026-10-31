package net.rebel459.music_and_melody.client.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.util.GsonHelper;
import net.rebel459.music_and_melody.MusicAndMelody;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class CustomSounds {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path FILE = Path.of("config", MusicAndMelody.MOD_ID, "sounds.json");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer())
            .setPrettyPrinting()
            .create();
    private static final TypeToken<Map<String, SoundEventRegistration>> TYPE = new TypeToken<>() {};

    private CustomSounds() {}

    public static Map<String, SoundEventRegistration> load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.notExists(FILE)) Files.writeString(FILE, "{}\n", StandardCharsets.UTF_8);
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                return GsonHelper.fromJson(GSON, reader, TYPE);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not load config sounds file '{}'.", FILE, exception);
            return Map.of();
        }
    }

    public static JsonObject loadEditorJson() {
        ensureFile();
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonObject()) return json.getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not load config sounds editor file '{}'.", FILE, exception);
        }
        backUpUnreadableFile();
        return new JsonObject();
    }

    public static boolean saveEditorJson(JsonObject json) {
        try {
            ensureFile();
            AtomicFiles.write(FILE, writer -> GSON.toJson(json, writer));
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Could not save config sounds editor file '{}'.", FILE, exception);
            return false;
        }
    }

    private static void backUpUnreadableFile() {
        Path backup = FILE.resolveSibling(FILE.getFileName() + ".backup");
        try {
            if (Files.isRegularFile(FILE) && Files.notExists(backup)) {
                Files.copy(FILE, backup);
                LOGGER.warn("Backed up unreadable config sounds file to '{}'.", backup);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not back up unreadable config sounds file '{}'.", FILE, exception);
        }
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.notExists(FILE)) Files.writeString(FILE, "{}\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Could not create config sounds file '{}'.", FILE, exception);
        }
    }
}
