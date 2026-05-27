package net.rebel459.music_and_melody.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class JukeboxSongCache {

    private static final String DATA_PREFIX = "data/";
    private static final String JUKEBOX_DIRECTORY = "/jukebox_song/";
    private static final String JSON_SUFFIX = ".json";
    private static final List<Path> ROOTS = new ArrayList<>();
    private static final Map<Identifier, Identifier> SOUND_EVENTS = new HashMap<>();
    private static final Map<Identifier, Identifier> RESOLVED_SOUNDS = new HashMap<>();
    private static final Set<Identifier> MISSES = new HashSet<>();

    private JukeboxSongCache() {}

    public static synchronized void clear() {
        ROOTS.clear();
        SOUND_EVENTS.clear();
        RESOLVED_SOUNDS.clear();
        MISSES.clear();
    }

    public static synchronized void clearResolvedSounds() {
        RESOLVED_SOUNDS.clear();
    }

    public static synchronized void loadFromRoot(Path root) {
        if ((Files.isDirectory(root) || Files.isRegularFile(root)) && !ROOTS.contains(root)) {
            ROOTS.add(root);
        }
    }

    public static synchronized Optional<Identifier> soundId(Minecraft minecraft, Identifier jukeboxSongId) {
        Identifier cached = RESOLVED_SOUNDS.get(jukeboxSongId);
        if (cached != null) return Optional.of(cached);

        Identifier soundEvent = SOUND_EVENTS.get(jukeboxSongId);
        if (soundEvent == null && !MISSES.contains(jukeboxSongId)) {
            load(jukeboxSongId);
            soundEvent = SOUND_EVENTS.get(jukeboxSongId);
        }
        if (soundEvent == null) return Optional.empty();

        Identifier resolved = resolveSoundEvent(minecraft, soundEvent).orElse(soundEvent);
        RESOLVED_SOUNDS.put(jukeboxSongId, resolved);
        return Optional.of(resolved);
    }

    private static void load(Identifier jukeboxSong) {
        for (int i = ROOTS.size() - 1; i >= 0; i--) {
            Path root = ROOTS.get(i);
            boolean loaded = Files.isDirectory(root)
                    ? loadFromDirectory(root, jukeboxSong)
                    : Files.isRegularFile(root) && loadFromZip(root, jukeboxSong);
            if (loaded) return;
        }
        MISSES.add(jukeboxSong);
    }

    private static boolean loadFromDirectory(Path root, Identifier jukeboxSong) {
        Path file = root.resolve(jukeboxSongPath(jukeboxSong));
        if (!Files.isRegularFile(file)) return false;

        try (Reader reader = Files.newBufferedReader(file)) {
            return add(jukeboxSong, JsonParser.parseReader(reader));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean loadFromZip(Path file, Identifier jukeboxSong) {
        String target = jukeboxSongPath(jukeboxSong);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(target)) {
                    try (Reader reader = new java.io.InputStreamReader(zip, java.nio.charset.StandardCharsets.UTF_8)) {
                        return add(jukeboxSong, JsonParser.parseReader(reader));
                    } catch (Exception ignored) {
                        return false;
                    }
                }
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private static String jukeboxSongPath(Identifier jukeboxSong) {
        return DATA_PREFIX + jukeboxSong.getNamespace() + "/jukebox_song/" + jukeboxSong.getPath() + JSON_SUFFIX;
    }

    private static boolean add(Identifier jukeboxSong, JsonElement element) {
        if (!element.isJsonObject()) return false;
        JsonObject json = element.getAsJsonObject();
        JsonElement soundEvent = json.get("sound_event");
        if (soundEvent == null || !soundEvent.isJsonPrimitive()) return false;

        Identifier soundEventId = Identifier.tryParse(soundEvent.getAsString());
        if (soundEventId != null) {
            SOUND_EVENTS.put(jukeboxSong, soundEventId);
            RESOLVED_SOUNDS.remove(jukeboxSong);
            MISSES.remove(jukeboxSong);
            return true;
        }
        return false;
    }

    private static Optional<Identifier> resolveSoundEvent(Minecraft minecraft, Identifier eventId) {
        var soundEvent = minecraft.getSoundManager().getSoundEvent(eventId);
        if (soundEvent == null) return Optional.empty();

        Sound sound = soundEvent.getSound(SoundInstance.createUnseededRandom());
        if (sound == SoundManager.EMPTY_SOUND) return Optional.empty();
        return Optional.of(sound.getLocation());
    }
}
