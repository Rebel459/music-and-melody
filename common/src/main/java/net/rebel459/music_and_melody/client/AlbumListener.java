package net.rebel459.music_and_melody.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.util.SafeLocation;
import net.rebel459.music_and_melody.client.util.SafeMusicHelper;
import net.rebel459.music_and_melody.config.ConfigAlbum;

import java.util.*;
import java.util.stream.Collectors;

public class AlbumListener extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = MusicAndMelody.id("albums");

    private final Set<Album> loadedAlbums = new HashSet<>();

    public AlbumListener() {
        super(new Gson(), "albums");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Album.ALBUMS.removeAll(this.loadedAlbums);
        Album.DISABLED_ALBUMS.removeAll(this.loadedAlbums);
        this.loadedAlbums.clear();

        Set<ResourceLocation> registeredDiscs = new HashSet<>();
        Map<ResourceLocation, Album.Record> recordMap = decode(jsonMap);
        for (Map.Entry<ResourceLocation, Album.Record> entry : recordMap.entrySet()) {
            ResourceLocation albumId = entry.getKey();
            Album.Record record = entry.getValue();

            TrackSet trackSet = expandTracks(albumId, record.tracks(), resourceManager);
            Set<String> tracks = new HashSet<>();
            for (String track : trackSet.tracks()) {
                tracks.add(SafeLocation.parse(track).getPath());
            }
            Set<String> forcedEnabledTracks = trackSet.forcedEnabledTracks();

            Set<Album.StoredDisc> discs = record.discs()
                    .stream()
                    .map(disc -> new Album.StoredDisc(disc.path(), disc.soundEvent(), disc.description()))
                    .collect(Collectors.toSet());

            for (Album.StoredDisc disc : discs) {
                ResourceLocation discId = disc.path().contains(":")
                        ? ResourceLocation.tryParse(disc.path())
                        : ResourceLocation.fromNamespaceAndPath(albumId.getNamespace(), disc.path());

                if (discId != null) {
                    registeredDiscs.add(discId);
                }
            }

            Album album = new Album(
                    albumId,
                    record.name(),
                    record.icon(),
                    tracks,
                    forcedEnabledTracks,
                    discs
            );

            this.loadedAlbums.add(album);
        }

        Album configAlbum = ConfigAlbum.createAlbum(registeredDiscs);

        if (configAlbum != null) {
            this.loadedAlbums.add(configAlbum);
        }
    }

    private static TrackSet expandTracks(
            ResourceLocation albumId,
            List<Album.Track> entries,
            ResourceManager resourceManager
    ) {
        LinkedHashMap<String, String> tracksById = new LinkedHashMap<>();
        Set<String> forcedEnabledTracks = new HashSet<>();

        for (Album.Track entry : entries) {
            List<String> expandedTracks = entry.folder()
                    ? folderTracks(albumId, entry.path(), resourceManager)
                    : List.of(entry.path());

            for (String song : expandedTracks) {
                String id = trackId(albumId, song).toString();

                if (entry.enabled()) {
                    forcedEnabledTracks.add(id);
                    tracksById.put(id, song);
                } else {
                    tracksById.putIfAbsent(id, song);
                }
            }
        }

        return new TrackSet(
                new LinkedHashSet<>(tracksById.values()),
                forcedEnabledTracks
        );
    }

    private static SafeLocation trackId(ResourceLocation albumId, String song) {
        return song.contains(":")
                ? SafeLocation.parse(song)
                : SafeLocation.fromNamespaceAndPath(albumId.getNamespace(), song);
    }

    private record TrackSet(Set<String> tracks, Set<String> forcedEnabledTracks) {}

    private static List<String> folderTracks(
            ResourceLocation albumId,
            String folder,
            ResourceManager resourceManager
    ) {
        SafeLocation folderId = folder.contains(":")
                ? SafeLocation.parse(folder)
                : SafeLocation.fromNamespaceAndPath(albumId.getNamespace(), folder);

        LinkedHashSet<String> tracks = new LinkedHashSet<>();

        tracks.addAll(resourceFolderTracks(folderId, resourceManager));
        if (RemoteContentManager.isDownloadedAlbum(albumId)) {
            tracks.addAll(SafeMusicHelper.downloadTracksInFolder(folderId));
        }

        return new ArrayList<>(tracks);
    }

    private static List<String> resourceFolderTracks(
            SafeLocation folderId,
            ResourceManager resourceManager
    ) {
        ResourceLocation validFolderId = ResourceLocation.tryParse(folderId.toString());

        if (validFolderId == null) {
            return List.of();
        }

        String normalized = normalize(validFolderId.getPath());

        if (normalized.isBlank()) {
            return List.of();
        }

        String directory = "sounds/" + normalized;
        String prefix = directory + "/";

        return resourceManager.listResources(directory, id ->
                        id.getNamespace().equals(validFolderId.getNamespace())
                                && id.getPath().startsWith(prefix)
                                && id.getPath().endsWith(".ogg")
                )
                .keySet()
                .stream()
                .map(id -> id.getNamespace() + ":" + id.getPath().substring(
                        "sounds/".length(),
                        id.getPath().length() - ".ogg".length()
                ))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static Map<ResourceLocation, Album.Record> decode(Map<ResourceLocation, JsonElement> jsonMap) {
        Map<ResourceLocation, Album.Record> recordMap = new java.util.HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            Album.Record.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LogUtils.getLogger().warn("Failed to parse album {}: {}", entry.getKey(), error))
                    .ifPresent(record -> recordMap.put(entry.getKey(), record));
        }
        return recordMap;
    }
    
    private static String normalize(String value) {
        String result = value.replace('\\', '/');

        while (result.startsWith("/")) {
            result = result.substring(1);
        }

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        if (result.startsWith("sounds/")) {
            result = result.substring("sounds/".length());
        }

        while (result.contains("//")) {
            result = result.replace("//", "/");
        }

        return result;
    }
}
