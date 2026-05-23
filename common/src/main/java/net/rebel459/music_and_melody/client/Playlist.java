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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
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

    public ResourceLocation playlist;
    public Component name;
    public ResourceLocation icon;
    public List<ResourceLocation> tracks;
    public List<ResourceLocation> discs;
    public boolean hidden;
    public Path source;

    public Playlist(ResourceLocation playlist, Component name, ResourceLocation icon, List<ResourceLocation> tracks, List<ResourceLocation> discs, boolean hidden, Path source) {
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
        return MaMDataConfig.get().playlists.favourites.contains(this.playlist.toString());
    }

    public void setFavourite(boolean favourite) {
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
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("config", "playlists/" + uniquePath(sanitize(stem(file)), usedPaths));
            Playlist playlist = create(id, record, file);
            CONFIG_PLAYLISTS.add(playlist);
        }
    }

    public static synchronized String previewConfigPlaylistPath(String playlistName) {
        if (playlistName.trim().isEmpty()) return "";
        Path target = configTarget(playlistName, "");
        if (target == null) return "";
        String path = DIRECTORY.relativize(target).toString().replace('\\', '/');
        return path.endsWith(".json") ? path.substring(0, path.length() - ".json".length()) : path;
    }

    public static synchronized boolean configPlaylistExists(String playlistName, String pathOverride) {
        Path target = configTarget(playlistName, pathOverride);
        return target != null && Files.exists(target);
    }

    public static synchronized boolean canWriteConfigPlaylist(String playlistName, String pathOverride) {
        return configTarget(playlistName, pathOverride) != null;
    }

    public static synchronized boolean saveCurrentQueue(Minecraft minecraft, String playlistName, String iconPath, String pathOverride) {
        List<ResourceLocation> queuedSongs = PlaylistHelper.queuedSongs();
        String trimmedName = playlistName.trim();
        if (queuedSongs.isEmpty() || trimmedName.isEmpty()) return false;

        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException ignored) {
            return false;
        }

        Path path = configTarget(trimmedName, pathOverride);
        if (path == null) return false;
        ResourceLocation icon = iconPath.isBlank()
                ? ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png")
                : ResourceLocation.tryParse(iconPath.trim());
        if (icon == null) return false;

        JsonObject root = new JsonObject();
        JsonObject nameObject = new JsonObject();
        nameObject.addProperty("text", trimmedName);
        root.add("name", nameObject);
        root.addProperty("icon", icon.toString());
        root.add("entries", entries(groupTracks(minecraft, queuedSongs, false), groupTracks(minecraft, queuedSongs, true)));

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ignored) {
            return false;
        }

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        } catch (IOException ignored) {
            return false;
        }

        reloadConfigPlaylists();
        return true;
    }

    public static Playlist create(ResourceLocation id, Record record, Path source) {
        List<ResourceLocation> tracks = new ArrayList<>();
        List<ResourceLocation> discs = new ArrayList<>();
        record.entries().forEach(entry -> {
            entry.tracks().forEach(track -> tracks.add(ResourceLocation.fromNamespaceAndPath(entry.namespace(), track)));
            entry.discs().forEach(disc -> discs.add(ResourceLocation.fromNamespaceAndPath(entry.namespace(), disc)));
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

    private static Map<String, List<String>> groupTracks(Minecraft minecraft, List<ResourceLocation> queuedSongs, boolean discs) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (ResourceLocation queuedSong : queuedSongs) {
            ResourceLocation id;
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

    private static ResourceLocation jukeboxSongForSound(Minecraft minecraft, ResourceLocation sound) {
        var albumMatch = MusicDiscHelper.matchSound(minecraft, sound);
        if (albumMatch.isPresent()) return albumMatch.get().jukeboxSong();
        for (Playlist playlist : PLAYLISTS) {
            for (ResourceLocation disc : playlist.discs) {
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
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("[^a-z0-9._-]", "");
        if (sanitized.isBlank()) return "playlist";
        return sanitized;
    }

    private static Path configTarget(String name, String pathOverride) {
        String rawPath = pathOverride.trim().isEmpty() ? sanitize(name) : sanitizePath(pathOverride.trim());
        if (rawPath.isBlank()) return null;
        if (!rawPath.toLowerCase(Locale.ROOT).endsWith(".json")) rawPath += ".json";
        Path target = DIRECTORY.resolve(rawPath).normalize();
        Path root = DIRECTORY.toAbsolutePath().normalize();
        return target.toAbsolutePath().normalize().startsWith(root) ? target : null;
    }

    private static String sanitizePath(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replace('\\', '/').replaceAll("\\s+", "_").replaceAll("[^a-z0-9._/-]", "");
        while (sanitized.startsWith("/")) sanitized = sanitized.substring(1);
        return java.util.Arrays.stream(sanitized.split("/"))
                .filter(part -> !part.isBlank() && !part.equals(".") && !part.equals(".."))
                .map(Playlist::sanitize)
                .collect(java.util.stream.Collectors.joining("/"));
    }

    private static String uniquePath(String path, Set<String> usedPaths) {
        if (usedPaths.add(path)) return path;
        for (int i = 2; ; i++) {
            String suffixed = path + "_" + i;
            if (usedPaths.add(suffixed)) return suffixed;
        }
    }

    public record Record(Component name, ResourceLocation icon, List<Entry> entries, List<String> dependencies, boolean hidden) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Record::entries),
                Codec.STRING.listOf().optionalFieldOf("dependencies", List.of()).forGetter(Record::dependencies),
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
