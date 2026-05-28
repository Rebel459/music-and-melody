package net.rebel459.music_and_melody.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.util.SafeLocation;

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

    public static final ResourceLocation ALBUM_ID = ResourceLocation.fromNamespaceAndPath("config", "album");
    private static final Path DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "album");
    private static final String SOUND_PATH = "album/";
    private static final Map<SafeLocation, Path> FILES = new LinkedHashMap<>();
    private static final Map<SafeLocation, String> NAMES = new HashMap<>();

    private ConfigAlbum() {}

    public static synchronized Album createAlbum(Set<ResourceLocation> registeredDiscs) {
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
                Component.literal("Config Album"),
                ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png"),
                FILES.keySet().stream().map(SafeLocation::getPath).collect(Collectors.toSet()),
                discs
        );
    }

    public static synchronized void addSoundResources(Map<ResourceLocation, Resource> soundCache) {
        reload();
        FILES.forEach((id, path) -> soundCache.put(idToFile(id), new Resource(null, IoSupplier.create(path))));
    }

    public static ResourceLocation idToFile(SafeLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "sounds/" + id.getPath() + ".ogg");
    }

    public static synchronized String displayName(SafeLocation id) {
        return NAMES.get(playableId(id));
    }

    public static synchronized Optional<Path> file(SafeLocation id) {
        reload();
        return Optional.ofNullable(FILES.get(playableId(id)));
    }

    public static SafeLocation playableId(SafeLocation id) {
        if (id.getNamespace().equals(MusicAndMelody.MOD_ID) && id.getPath().startsWith(SOUND_PATH)) {
            return SafeLocation.fromNamespaceAndPath("config", id.getPath());
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
            String path = uniquePath(SOUND_PATH + sanitize(stem(fileName)), usedPaths);
            SafeLocation id = SafeLocation.fromNamespaceAndPath("config", path);
            FILES.put(id, file);
            NAMES.put(id, stem(fileName));
        }
    }

    private static boolean isOgg(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg");
    }

    private static Set<String> unregisteredDiscs(Set<ResourceLocation> registeredDiscs) {
        List<ResourceLocation> discs = new ArrayList<>();
        for (ResourceLocation itemId : BuiltInRegistries.ITEM.keySet()) {
            String path = itemId.getPath();
            if (!path.startsWith("music_disc_")) continue;
            ResourceLocation jukeboxSong = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), path.substring("music_disc_".length()));
            if (!registeredDiscs.contains(jukeboxSong)) discs.add(jukeboxSong);
        }
        discs.sort(Comparator.comparing(ResourceLocation::toString));
        return discs.stream().map(ResourceLocation::toString).collect(Collectors.toSet());
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

    private static String stem(String fileName) {
        return fileName.substring(0, fileName.length() - ".ogg".length());
    }
}
