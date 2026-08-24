package net.rebel459.music_and_melody.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.Album;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class CustomAlbums {

    public static final Identifier MOD_DISCS_ID = Identifier.fromNamespaceAndPath("config", "mod_discs");
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final Path CONFIG_DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID);
    private static final Path DIRECTORY = CONFIG_DIRECTORY.resolve("albums");
    private static final Path ICON_DIRECTORY = CONFIG_DIRECTORY.resolve("icons");
    private static final String ALBUM_FILE = "album.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<SafeIdentifier, Path> FILES = new LinkedHashMap<>();
    private static final Map<SafeIdentifier, String> NAMES = new HashMap<>();
    private static final Map<Identifier, Metadata> METADATA = new LinkedHashMap<>();
    private static final Map<Identifier, List<String>> TRACK_FILES = new HashMap<>();
    private static final Map<Identifier, Identifier> DYNAMIC_ICONS = new HashMap<>();

    private CustomAlbums() {}

    public static synchronized List<Album> createAlbums(Set<Identifier> registeredDiscs) {
        reload();
        List<Album> albums = new ArrayList<>();
        for (Metadata metadata : METADATA.values()) {
            Set<String> tracks = tracks(metadata.id(), metadata.record());
            albums.add(new Album(metadata.id(), metadata.name(), metadata.icon(), tracks, Set.of()));
        }

        if (MaMClientConfig.get().automatic_discs) {
            Set<Album.StoredDisc> discs = unregisteredDiscs(registeredDiscs).stream()
                    .map(path -> new Album.StoredDisc(path, Optional.empty(), Optional.empty()))
                    .collect(Collectors.toSet());
            if (!discs.isEmpty()) albums.add(new Album(MOD_DISCS_ID, Component.translatable("album.music_and_melody.mod_discs"), DEFAULT_ICON, Set.of(), discs));
        }
        return albums;
    }

    public static synchronized void addSoundResources(Map<Identifier, Resource> soundCache) {
        reload();
        DYNAMIC_ICONS.clear();
        FILES.forEach((id, path) -> soundCache.put(idToFile(id), new Resource(null, IoSupplier.create(path))));
    }

    public static synchronized Identifier idToFile(SafeIdentifier id) {
        Path file = FILES.get(id);
        String suffix = file == null ? ".ogg" : extension(file);
        String path = id.getPath();
        if (!path.toLowerCase(Locale.ROOT).endsWith(suffix)) path += suffix;
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "sounds/" + path);
    }

    public static synchronized String displayName(SafeIdentifier id) {
        reload();
        return NAMES.get(id);
    }

    public static synchronized Optional<Path> file(SafeIdentifier id) {
        reload();
        return Optional.ofNullable(FILES.get(id));
    }

    public static synchronized List<String> tracksInFolder(SafeIdentifier folder) {
        reload();
        return tracksInFolderLoaded(folder);
    }

    private static List<String> tracksInFolderLoaded(SafeIdentifier folder) {
        String prefix = folder.getPath().endsWith("/") ? folder.getPath() : folder.getPath() + "/";
        return FILES.keySet().stream()
                .filter(id -> id.getNamespace().equals(folder.getNamespace()) && id.getPath().startsWith(prefix))
                .map(SafeIdentifier::toString)
                .sorted()
                .toList();
    }

    public static SafeIdentifier playableId(SafeIdentifier id) {
        return id;
    }

    public static synchronized boolean isConfigAlbum(Album album) {
        return album != null && isConfigAlbum(album.album);
    }

    public static synchronized boolean isConfigAlbum(Identifier id) {
        return id != null && METADATA.containsKey(id);
    }

    public static synchronized Optional<Metadata> metadata(Identifier id) {
        reload();
        return Optional.ofNullable(METADATA.get(id));
    }

    public static synchronized List<String> trackFiles(Identifier id) {
        reload();
        return List.copyOf(TRACK_FILES.getOrDefault(id, List.of()));
    }

    public static synchronized boolean exists(String identifier) {
        String path = sanitize(identifier);
        return !path.isBlank() && Files.exists(albumDirectory(path));
    }

    public static synchronized String previewIdentifier(String name) {
        return sanitize(name);
    }

    public static synchronized boolean canUseIdentifier(String identifier) {
        return !sanitize(identifier).isBlank();
    }

    public static boolean validIconInput(String icon) {
        String value = icon.trim();
        return value.isEmpty() || Identifier.tryParse(value) != null;
    }

    public static synchronized boolean create(String name, String icon, String identifier, List<Path> tracks) {
        String idPath = sanitize(identifier.isBlank() ? name : identifier);
        if (name.trim().isEmpty() || idPath.isBlank() || tracks.isEmpty() || exists(idPath)) return false;
        return write(idPath, name, icon, tracks);
    }

    public static synchronized boolean update(Identifier id, String name, String icon, List<Path> addedTracks, Set<String> removedTracks) {
        Metadata metadata = METADATA.get(id);
        if (metadata == null || name.trim().isEmpty()) return false;
        Path directory = albumDirectory(id.getPath());
        try {
            for (String track : removedTracks) {
                Path file = directory.resolve(track).normalize();
                if (file.startsWith(directory) && Files.isRegularFile(file)) Files.delete(file);
            }
        } catch (IOException ignored) {
            return false;
        }
        return write(id.getPath(), name, icon, addedTracks);
    }

    public static synchronized boolean delete(Identifier id) {
        if (!METADATA.containsKey(id)) return false;
        Path directory = albumDirectory(id.getPath());
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) {
            return false;
        }
        METADATA.remove(id);
        MaMDataConfig data = MaMDataConfig.get();
        data.albums.favourites.remove(id.toString());
        data.albums.disabled_albums.remove(id.toString());
        data.albums.disabled_tracks.removeIf(track -> track.startsWith(id + "/"));
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        return true;
    }

    public static Identifier resolveIcon(Minecraft minecraft, Identifier icon) {
        if (icon == null) return DEFAULT_ICON;
        if (!icon.getNamespace().equals("config") || !icon.getPath().toLowerCase(Locale.ROOT).endsWith(".png")) return icon;
        Identifier loaded;
        synchronized (CustomAlbums.class) {
            loaded = DYNAMIC_ICONS.get(icon);
        }
        if (loaded != null) return loaded;
        Path iconFile = ICON_DIRECTORY.resolve(icon.getPath()).normalize();
        if (!iconFile.toAbsolutePath().normalize().startsWith(ICON_DIRECTORY.toAbsolutePath().normalize()) || !Files.isRegularFile(iconFile)) return DEFAULT_ICON;
        try {
            NativeImage image = NativeImage.read(Files.newInputStream(iconFile));
            Identifier dynamic = Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "config_icons/" + icon.getPath());
            minecraft.getTextureManager().register(dynamic, new DynamicTexture(() -> "Config icon " + icon, image));
            synchronized (CustomAlbums.class) {
                DYNAMIC_ICONS.put(icon, dynamic);
            }
            return dynamic;
        } catch (IOException ignored) {
            return DEFAULT_ICON;
        }
    }

    public static synchronized boolean isDynamicIcon(Identifier icon) {
        return DYNAMIC_ICONS.containsValue(icon);
    }

    public static void chooseAudioFiles(Consumer<List<Path>> callback) {
        chooseFiles("Select music files", true, callback);
    }

    private static void chooseFiles(String title, boolean multiple, Consumer<List<Path>> callback) {
        Thread chooser = new Thread(() -> {
            List<Path> selected = new ArrayList<>();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String[] extensions = new String[] {"*.ogg", "*.mp3", "*.flac", "*.wav"};
                PointerBuffer filters = stack.mallocPointer(extensions.length);
                for (String extension : extensions) filters.put(stack.UTF8(extension));
                filters.flip();
                String result = TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, ".ogg, .mp3, .flac or .wav", multiple);
                if (result != null && !result.isBlank()) {
                    for (String file : result.split("\\|")) {
                        Path path = Path.of(file);
                        if (Files.isRegularFile(path) && isSupportedAudio(path)) {
                            selected.add(path);
                        }
                    }
                }
            } catch (RuntimeException ignored) {
            }
            Minecraft.getInstance().execute(() -> callback.accept(selected));
        }, "Music & Melody file picker");
        chooser.setDaemon(true);
        chooser.start();
    }

    private static boolean write(String idPath, String name, String icon, List<Path> tracks) {
        Path directory = albumDirectory(idPath);
        try {
            Files.createDirectories(directory);
            copyTracks(directory, tracks);
            Identifier iconId = icon.trim().isEmpty() ? DEFAULT_ICON : Identifier.tryParse(icon.trim());
            if (iconId == null) return false;
            Album.Record record = new Album.Record(Component.literal(name.trim()), iconId,
                    List.of(new Album.Track(idPath, true)), List.of());
            try (Writer writer = Files.newBufferedWriter(directory.resolve(ALBUM_FILE))) {
                Album.Record.CODEC.encodeStart(JsonOps.INSTANCE, record).result()
                        .ifPresent(json -> GSON.toJson(JsonParser.parseString(json.toString()), writer));
            }
            reload();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void copyTracks(Path directory, List<Path> tracks) throws IOException {
        Set<String> used = new HashSet<>();
        try (var files = Files.list(directory)) {
            files.filter(Files::isRegularFile).map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)).forEach(used::add);
        }
        for (Path source : tracks) {
            if (!Files.isRegularFile(source) || !isSupportedAudio(source)) continue;
            String name = source.getFileName().toString();
            String targetName = uniqueFileName(name, used);
            Files.copy(source, directory.resolve(targetName), StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private static void reload() {
        FILES.clear();
        NAMES.clear();
        METADATA.clear();
        TRACK_FILES.clear();
        try {
            Files.createDirectories(DIRECTORY);
            Files.createDirectories(ICON_DIRECTORY);
        } catch (IOException ignored) {
            return;
        }
        try (var files = Files.walk(DIRECTORY)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(ALBUM_FILE))
                    .map(Path::getParent)
                    .sorted(Comparator.comparing(path -> path.toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(CustomAlbums::loadAlbum);
        } catch (IOException ignored) {
        }
    }

    private static void loadAlbum(Path directory) {
        String rawIdPath = DIRECTORY.relativize(directory).toString().replace('\\', '/');
        String idPath = sanitize(rawIdPath);
        if (idPath.isBlank() || !idPath.equals(rawIdPath)) return;
        Metadata metadata = readMetadata(directory, idPath);
        if (metadata == null) return;
        METADATA.put(metadata.id(), metadata);
        List<Path> files;
        try (var stream = Files.walk(directory)) {
            files = stream.filter(Files::isRegularFile).filter(CustomAlbums::isSupportedAudio)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)).toList();
        } catch (IOException ignored) {
            return;
        }
        Set<String> used = new HashSet<>();
        List<String> trackFiles = new ArrayList<>();
        for (Path file : files) {
            String relative = directory.relativize(file).toString().replace('\\', '/');
            // The source filename is not necessarily a valid resource path (for
            // example, "01. Earth.mp3"). Keep it only for display/file access;
            // the runtime config identifier must always use safe characters.
            String path = uniquePath(sanitize(playablePath(relative)), used);
            SafeIdentifier id = SafeIdentifier.fromNamespaceAndPath("config", idPath + "/" + path);
            FILES.put(id, file);
            NAMES.put(id, stem(file.getFileName().toString()));
            trackFiles.add(relative);
        }
        TRACK_FILES.put(metadata.id(), List.copyOf(trackFiles));
    }

    private static Metadata readMetadata(Path directory, String idPath) {
        Path json = directory.resolve(ALBUM_FILE);
        if (!Files.isRegularFile(json)) return null;
        try (Reader reader = Files.newBufferedReader(json)) {
            Album.Record record = Album.Record.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).result().orElse(null);
            if (record == null) return null;
            return new Metadata(Identifier.fromNamespaceAndPath("config", idPath), record.name(), record.icon(), record);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path albumDirectory(String path) {
        return DIRECTORY.resolve(path).normalize();
    }

    private static boolean isSupportedAudio(Path path) {
        String extension = extension(path);
        return extension.equals(".ogg") || extension.equals(".mp3") || extension.equals(".flac") || extension.equals(".wav");
    }

    private static Set<String> unregisteredDiscs(Set<Identifier> registeredDiscs) {
        List<Identifier> discs = new ArrayList<>();
        for (Identifier itemId : BuiltInRegistries.ITEM.keySet()) {
            String path = itemId.getPath();
            if (!path.startsWith("music_disc_")) continue;
            Identifier jukeboxSong = Identifier.fromNamespaceAndPath(itemId.getNamespace(), path.substring("music_disc_".length()));
            if (!registeredDiscs.contains(jukeboxSong)) discs.add(jukeboxSong);
        }
        discs.sort(Comparator.comparing(Identifier::toString));
        return discs.stream().map(Identifier::toString).collect(Collectors.toSet());
    }

    private static String stem(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static String playablePath(String fileName) {
        return extension(fileName).equals(".ogg") ? stem(fileName) : fileName;
    }

    private static String extension(Path path) {
        return extension(path.getFileName().toString());
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('\\', '/').replaceAll("\\s+", "_");
        return java.util.Arrays.stream(normalized.split("/"))
                .filter(part -> !part.isBlank() && !part.equals(".") && !part.equals(".."))
                .map(part -> part.replaceAll("[^a-z0-9._-]", ""))
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("/"));
    }

    private static String uniquePath(String path, Set<String> usedPaths) {
        if (usedPaths.add(path)) return path;
        for (int i = 2; ; i++) {
            String suffix = path + "_" + i;
            if (usedPaths.add(suffix)) return suffix;
        }
    }

    private static String uniqueFileName(String name, Set<String> used) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (used.add(lower)) return name;
        String stem = stem(name);
        String extension = extension(name);
        for (int i = 2; ; i++) {
            String candidate = stem + "_" + i + extension;
            if (used.add(candidate.toLowerCase(Locale.ROOT))) return candidate;
        }
    }

    private static Set<String> tracks(Identifier albumId, Album.Record record) {
        Set<String> tracks = new java.util.LinkedHashSet<>();
        for (Album.Track entry : record.tracks()) {
            SafeIdentifier track = entry.path().contains(":")
                    ? SafeIdentifier.parse(entry.path())
                    : SafeIdentifier.fromNamespaceAndPath(albumId.getNamespace(), entry.path());
            if (entry.folder()) {
                tracksInFolderLoaded(track).stream().map(SafeIdentifier::parse).map(SafeIdentifier::getPath).forEach(tracks::add);
            } else {
                tracks.add(track.getPath());
            }
        }
        return tracks;
    }

    public record Metadata(Identifier id, Component name, Identifier icon, Album.Record record) {}
}
