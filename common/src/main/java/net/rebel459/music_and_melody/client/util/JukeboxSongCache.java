package net.rebel459.music_and_melody.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class JukeboxSongCache {

    private static final String DATA_PREFIX = "data/";
    private static final String JUKEBOX_DIRECTORY = "/jukebox_song/";
    private static final String JSON_SUFFIX = ".json";
    private static final Map<ResourceLocation, ResourceLocation> SOUND_EVENTS = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> RESOLVED_SOUNDS = new HashMap<>();

    private JukeboxSongCache() {}

    public static synchronized void clear() {
        SOUND_EVENTS.clear();
        RESOLVED_SOUNDS.clear();
    }

    public static synchronized void clearResolvedSounds() {
        RESOLVED_SOUNDS.clear();
    }

    public static synchronized void loadFromRoot(Path root) {
        if (Files.isDirectory(root)) {
            loadFromDirectory(root);
        } else if (Files.isRegularFile(root)) {
            loadFromZip(root);
        }
    }

    public static synchronized Optional<ResourceLocation> soundId(Minecraft minecraft, ResourceLocation jukeboxSongId) {
        ResourceLocation cached = RESOLVED_SOUNDS.get(jukeboxSongId);
        if (cached != null) return Optional.of(cached);

        ResourceLocation soundEvent = SOUND_EVENTS.get(jukeboxSongId);
        if (soundEvent == null) return Optional.empty();

        ResourceLocation resolved = resolveSoundEvent(minecraft, soundEvent).orElse(soundEvent);
        RESOLVED_SOUNDS.put(jukeboxSongId, resolved);
        return Optional.of(resolved);
    }

    private static void loadFromDirectory(Path root) {
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) return;

        try (var files = Files.walk(data)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(JSON_SUFFIX))
                    .filter(path -> path.toString().replace('\\', '/').contains(JUKEBOX_DIRECTORY))
                    .forEach(path -> loadDirectoryFile(data, path));
        } catch (IOException ignored) {
        }
    }

    private static void loadDirectoryFile(Path data, Path file) {
        String relative = data.relativize(file).toString().replace('\\', '/');
        ResourceLocation jukeboxSong = jukeboxSongId(relative);
        if (jukeboxSong == null) return;

        try (Reader reader = Files.newBufferedReader(file)) {
            add(jukeboxSong, JsonParser.parseReader(reader));
        } catch (Exception ignored) {
        }
    }

    private static void loadFromZip(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (!name.startsWith(DATA_PREFIX) || !name.endsWith(JSON_SUFFIX) || !name.contains(JUKEBOX_DIRECTORY)) {
                    continue;
                }

                ResourceLocation jukeboxSong = jukeboxSongId(name.substring(DATA_PREFIX.length()));
                if (jukeboxSong == null) continue;

                try (Reader reader = new java.io.InputStreamReader(zip.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8)) {
                    add(jukeboxSong, JsonParser.parseReader(reader));
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static ResourceLocation jukeboxSongId(String path) {
        int separator = path.indexOf(JUKEBOX_DIRECTORY);
        if (separator <= 0 || !path.endsWith(JSON_SUFFIX)) return null;

        String namespace = path.substring(0, separator);
        String songPath = path.substring(separator + JUKEBOX_DIRECTORY.length(), path.length() - JSON_SUFFIX.length());
        return ResourceLocation.tryParse(namespace + ":" + songPath);
    }

    private static void add(ResourceLocation jukeboxSong, JsonElement element) {
        if (!element.isJsonObject()) return;
        JsonObject json = element.getAsJsonObject();
        JsonElement soundEvent = json.get("sound_event");
        if (soundEvent == null || !soundEvent.isJsonPrimitive()) return;

        ResourceLocation soundEventId = ResourceLocation.tryParse(soundEvent.getAsString());
        if (soundEventId != null) {
            SOUND_EVENTS.put(jukeboxSong, soundEventId);
            RESOLVED_SOUNDS.remove(jukeboxSong);
        }
    }

    private static Optional<ResourceLocation> resolveSoundEvent(Minecraft minecraft, ResourceLocation eventId) {
        var soundEvent = minecraft.getSoundManager().getSoundEvent(eventId);
        if (soundEvent == null) return Optional.empty();

        Sound sound = soundEvent.getSound(SoundInstance.createUnseededRandom());
        if (sound == SoundManager.EMPTY_SOUND) return Optional.empty();
        return Optional.of(sound.getLocation());
    }
}
