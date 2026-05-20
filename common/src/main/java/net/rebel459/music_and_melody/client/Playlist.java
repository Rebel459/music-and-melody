package net.rebel459.music_and_melody.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Playlist {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "playlists");
    private static final Set<Playlist> CONFIG_PLAYLISTS = new HashSet<>();

    public static Set<Playlist> PLAYLISTS = new HashSet<>();

    public Identifier playlist;
    public Component name;
    public Identifier icon;
    public List<Identifier> tracks;
    public List<Identifier> discs;
    public boolean hidden;
    public Path source;

    public Playlist(Identifier playlist, Component name, Identifier icon, List<Identifier> tracks, List<Identifier> discs, boolean hidden, Path source) {
        this.playlist = playlist;
        this.name = name;
        this.icon = icon;
        this.tracks = tracks;
        this.discs = discs;
        this.hidden = hidden;
        this.source = source;
        PLAYLISTS.add(this);
    }

    public boolean isFavourite() {
        return isCustom() || MaMDataConfig.get().playlists.favourites.contains(this.playlist.toString());
    }

    public void setFavourite(boolean favourite) {
        if (isCustom()) return;
        String id = this.playlist.toString();
        MaMDataConfig config = MaMDataConfig.get();

        if (favourite) {
            if (!config.playlists.favourites.contains(id)) {
                config.playlists.favourites.add(id);
            }
        } else {
            config.playlists.favourites.remove(id);
        }

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public boolean isCustom() {
        return this.source != null;
    }

    public boolean deleteCustom() {
        if (this.source == null) return false;
        try {
            if (Files.deleteIfExists(this.source)) {
                MaMDataConfig.get().playlists.favourites.remove(this.playlist.toString());
                AutoConfig.getConfigHolder(MaMDataConfig.class).save();
                reloadConfigPlaylists();
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public static synchronized void reloadConfigPlaylists() {
        PLAYLISTS.removeAll(CONFIG_PLAYLISTS);
        CONFIG_PLAYLISTS.clear();

        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException ignored) {
            return;
        }
        if (!Files.isDirectory(DIRECTORY)) return;

        List<Path> files;
        try (var stream = Files.walk(DIRECTORY)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(Playlist::isJson)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException ignored) {
            return;
        }

        Set<String> usedPaths = new HashSet<>();
        for (Path file : files) {
            Record record = readRecord(file);
            if (record == null) continue;
            Identifier id = Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "playlists/" + uniquePath(sanitize(stem(file)), usedPaths));
            Playlist playlist = create(id, record, file);
            CONFIG_PLAYLISTS.add(playlist);
        }
    }

    public static synchronized boolean saveCurrentQueue(Minecraft minecraft, String playlistName, String iconPath) {
        List<Identifier> queuedSongs = PlaylistHelper.queuedSongs();
        String trimmedName = playlistName.trim();
        if (queuedSongs.isEmpty() || trimmedName.isEmpty()) return false;

        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException ignored) {
            return false;
        }

        Path path = DIRECTORY.resolve(sanitize(trimmedName) + ".json");
        Identifier icon = iconPath.isBlank()
                ? Identifier.withDefaultNamespace("textures/misc/unknown_pack.png")
                : Identifier.tryParse(iconPath.trim());
        if (icon == null) return false;

        JsonObject root = new JsonObject();
        JsonObject nameObject = new JsonObject();
        nameObject.addProperty("text", trimmedName);
        root.add("name", nameObject);
        root.addProperty("icon", icon.toString());
        root.add("entries", entries(groupTracks(minecraft, queuedSongs, false), groupTracks(minecraft, queuedSongs, true)));

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        } catch (IOException ignored) {
            return false;
        }

        reloadConfigPlaylists();
        return true;
    }

    public static Playlist create(Identifier id, Record record, Path source) {
        List<Identifier> tracks = new ArrayList<>();
        List<Identifier> discs = new ArrayList<>();
        record.entries().forEach(entry -> {
            entry.tracks().forEach(track -> tracks.add(Identifier.fromNamespaceAndPath(entry.namespace(), track)));
            entry.discs().forEach(disc -> discs.add(Identifier.fromNamespaceAndPath(entry.namespace(), disc)));
        });
        return new Playlist(id, record.name(), record.icon(), tracks, discs, record.hidden, source);
    }

    private static Record readRecord(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            return Record.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Map<String, List<String>> groupTracks(Minecraft minecraft, List<Identifier> queuedSongs, boolean discs) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Identifier queuedSong : queuedSongs) {
            Identifier id;
            if (discs) {
                id = jukeboxSongForSound(minecraft, queuedSong);
                if (id == null) continue;
            } else {
                if (jukeboxSongForSound(minecraft, queuedSong) != null) continue;
                id = queuedSong;
            }
            grouped.computeIfAbsent(id.getNamespace(), namespace -> new ArrayList<>()).add(id.getPath());
        }
        return grouped;
    }

    private static Identifier jukeboxSongForSound(Minecraft minecraft, Identifier sound) {
        var albumMatch = MusicDiscHelper.matchSound(minecraft, sound);
        if (albumMatch.isPresent()) return albumMatch.get().jukeboxSong();
        for (Playlist playlist : PLAYLISTS) {
            for (Identifier disc : playlist.discs) {
                if (MusicDiscHelper.discSoundId(minecraft, disc).equals(sound)) return disc;
            }
        }
        return null;
    }

    private static JsonArray entries(Map<String, List<String>> tracks, Map<String, List<String>> discs) {
        JsonArray entries = new JsonArray();
        Set<String> namespaces = new HashSet<>();
        namespaces.addAll(tracks.keySet());
        namespaces.addAll(discs.keySet());
        namespaces.stream().sorted(String.CASE_INSENSITIVE_ORDER).forEach(namespace -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("namespace", namespace);
            addPaths(entry, "tracks", tracks.getOrDefault(namespace, List.of()));
            addPaths(entry, "discs", discs.getOrDefault(namespace, List.of()));
            entries.add(entry);
        });
        return entries;
    }

    private static void addPaths(JsonObject entry, String key, List<String> paths) {
        if (paths.isEmpty()) return;
        JsonArray values = new JsonArray();
        paths.forEach(values::add);
        entry.add(key, values);
    }

    private static boolean isJson(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static String stem(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".json".length());
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        if (sanitized.isBlank()) return "playlist";
        return sanitized;
    }

    private static String uniquePath(String path, Set<String> usedPaths) {
        if (usedPaths.add(path)) return path;
        for (int i = 2; ; i++) {
            String suffixed = path + "_" + i;
            if (usedPaths.add(suffixed)) return suffixed;
        }
    }

    public record Record(Component name, Identifier icon, List<Entry> entries, boolean hidden) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Record::entries),
                Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Record::hidden)
        ).apply(instance, Record::new));
    }

    public record Entry(String namespace, List<String> tracks, List<String> discs) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("namespace").forGetter(Entry::namespace),
                Codec.STRING.listOf().optionalFieldOf("tracks", List.of()).forGetter(Entry::tracks),
                Codec.STRING.listOf().optionalFieldOf("discs", List.of()).forGetter(Entry::discs)
        ).apply(instance, Entry::new));
    }
}
