package net.rebel459.music_and_melody.config;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.stream.Collectors;

public final class ConfigAlbum {

    public static final Identifier ALBUM_ID = Identifier.fromNamespaceAndPath("config", "album");
    private static final Path DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "album");
    private static final String INTERNAL_SOUND_PATH = "album/";
    private static final Map<SafeIdentifier, Path> FILES = new LinkedHashMap<>();
    private static final Map<SafeIdentifier, String> NAMES = new HashMap<>();

    private ConfigAlbum() {}

    public static synchronized Album createAlbum(Set<Identifier> registeredDiscs) {
        reload();
        Set<Album.StoredDisc> discs = new  HashSet<>();
        if (MaMClientConfig.get().automatic_discs) {
            discs = unregisteredDiscs(registeredDiscs).stream()
                    .map(path -> new Album.StoredDisc(path, Optional.empty(), Optional.empty()))
                    .collect(Collectors.toSet());
        }
        if (FILES.isEmpty() && discs.isEmpty()) return null;
        return new Album(
                ALBUM_ID,
                Component.translatable("album.music_and_melody.config_album"),
                Identifier.withDefaultNamespace("textures/misc/unknown_pack.png"),
                FILES.keySet().stream().map(SafeIdentifier::getPath).collect(Collectors.toSet()),
                discs
        );
    }

    public static synchronized void addSoundResources(Map<Identifier, Resource> soundCache) {
        reload();
        FILES.forEach((id, path) -> soundCache.put(idToFile(id), new Resource(null, IoSupplier.create(path))));
    }

    public static Identifier idToFile(SafeIdentifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "sounds/" + id.getPath() + ".ogg");
    }

    public static synchronized String displayName(SafeIdentifier id) {
        return NAMES.get(playableId(id));
    }

    public static synchronized Optional<Path> file(SafeIdentifier id) {
        reload();
        return Optional.ofNullable(FILES.get(playableId(id)));
    }

    public static SafeIdentifier playableId(SafeIdentifier id) {
        if (id.getNamespace().equals(MusicAndMelody.MOD_ID) && id.getPath().startsWith(INTERNAL_SOUND_PATH)) {
            return SafeIdentifier.fromNamespaceAndPath("config", id.getPath().substring(INTERNAL_SOUND_PATH.length()));
        }
        // Keep existing playlists and saved state readable after config sound
        // identifiers lose their redundant `album/` path segment.
        if (id.getNamespace().equals("config") && id.getPath().startsWith(INTERNAL_SOUND_PATH)) {
            return SafeIdentifier.fromNamespaceAndPath("config", id.getPath().substring(INTERNAL_SOUND_PATH.length()));
        }
        return id;
    }

    private static void reload() {
        FILES.clear();
        NAMES.clear();
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
                    .filter(ConfigAlbum::isOgg)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException ignored) {
            return;
        }

        Set<String> usedPaths = new HashSet<>();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String path = uniquePath(sanitize(stem(fileName)), usedPaths);
            SafeIdentifier id = SafeIdentifier.fromNamespaceAndPath("config", path);
            FILES.put(id, file);
            NAMES.put(id, stem(fileName));
        }
    }

    private static boolean isOgg(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg");
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
        return fileName.substring(0, fileName.length() - ".ogg".length());
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        if (sanitized.isBlank()) return "disc";
        return sanitized;
    }

    private static String uniquePath(String path, Set<String> usedPaths) {
        if (usedPaths.add(path)) return path;
        for (int i = 2; ; i++) {
            String suffixed = path + "_" + i;
            if (usedPaths.add(suffixed)) return suffixed;
        }
    }
}
