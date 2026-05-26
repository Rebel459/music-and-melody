package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.util.JukeboxSongCache;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.client.util.SafeMusicHelper;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.util.*;
import java.util.stream.Collectors;

public class AlbumListener extends SimpleJsonResourceReloadListener<Album.Record> {

    public static final Identifier ID = MusicAndMelody.id("albums");

    private final Set<Album> loadedAlbums = new HashSet<>();

    public AlbumListener() {
        super(Album.Record.CODEC, FileToIdConverter.json("albums"));
    }

    @Override
    protected void apply(
            Map<Identifier, Album.Record> identifierRecordMap,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Album.ALBUMS.removeAll(this.loadedAlbums);
        Album.DISABLED_ALBUMS.removeAll(this.loadedAlbums);
        this.loadedAlbums.clear();
        JukeboxSongCache.clearResolvedSounds();

        Set<Identifier> registeredDiscs = new HashSet<>();

        for (Map.Entry<Identifier, Album.Record> entry : identifierRecordMap.entrySet()) {
            Identifier albumId = entry.getKey();
            Album.Record record = entry.getValue();

            TrackSet trackSet = expandTracks(albumId, record.tracks(), resourceManager);
            Set<String> tracks = trackSet.tracks();
            Set<String> forcedEnabledTracks = trackSet.forcedEnabledTracks();

            Set<String> discs = record.discs()
                    .stream()
                    .map(Album.Disc::path)
                    .collect(Collectors.toSet());

            Set<String> forcedUnlockedDiscs = record.discs()
                    .stream()
                    .filter(Album.Disc::unlocked)
                    .map(Album.Disc::path)
                    .collect(Collectors.toSet());

            for (String disc : discs) {
                Identifier discId = disc.contains(":")
                        ? Identifier.tryParse(disc)
                        : Identifier.fromNamespaceAndPath(albumId.getNamespace(), disc);

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
                    discs,
                    forcedUnlockedDiscs
            );

            this.loadedAlbums.add(album);
        }

        if (MaMClientConfig.get().config_album) {
            Album configAlbum = ConfigAlbum.createAlbum(registeredDiscs);

            if (configAlbum != null) {
                this.loadedAlbums.add(configAlbum);
            }
        }
    }

    private static TrackSet expandTracks(
            Identifier albumId,
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

                    // Forced-enabled version wins if this same resolved track was already added.
                    tracksById.put(id, song);
                } else {
                    // Normal entries do not override forced-enabled entries.
                    tracksById.putIfAbsent(id, song);
                }
            }
        }

        return new TrackSet(
                new LinkedHashSet<>(tracksById.values()),
                forcedEnabledTracks
        );
    }

    private static SafeIdentifier trackId(Identifier albumId, String song) {
        return song.contains(":")
                ? SafeIdentifier.parse(song)
                : SafeIdentifier.fromNamespaceAndPath(albumId.getNamespace(), song);
    }

    private record TrackSet(Set<String> tracks, Set<String> forcedEnabledTracks) {}

    private static List<String> folderTracks(
            Identifier albumId,
            String folder,
            ResourceManager resourceManager
    ) {
        SafeIdentifier folderId = folder.contains(":")
                ? SafeIdentifier.parse(folder)
                : SafeIdentifier.fromNamespaceAndPath(albumId.getNamespace(), folder);

        LinkedHashSet<String> tracks = new LinkedHashSet<>();

        tracks.addAll(resourceFolderTracks(folderId, resourceManager));
        if (RemoteContentManager.isDownloadedAlbum(albumId)) {
            tracks.addAll(SafeMusicHelper.downloadTracksInFolder(folderId));
        }

        return new ArrayList<>(tracks);
    }

    private static List<String> resourceFolderTracks(
            SafeIdentifier folderId,
            ResourceManager resourceManager
    ) {
        Identifier validFolderId = Identifier.tryParse(folderId.toString());

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
