package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.client.util.SafeMusicHelper;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Set<Identifier> registeredDiscs = new HashSet<>();

        for (Map.Entry<Identifier, Album.Record> entry : identifierRecordMap.entrySet()) {
            Identifier albumId = entry.getKey();
            Album.Record record = entry.getValue();

            List<String> tracks = expandTracks(albumId, record.tracks(), resourceManager);
            Set<String> forcedEnabledTracks = forcedEnabledTracks(albumId, record.tracks(), resourceManager);

            List<String> discs = record.discs()
                    .stream()
                    .map(Album.Disc::disc)
                    .toList();

            Set<String> forcedUnlockedDiscs = record.discs()
                    .stream()
                    .filter(Album.Disc::unlocked)
                    .map(Album.Disc::disc)
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

    private static List<String> expandTracks(
            Identifier albumId,
            List<Album.Track> entries,
            ResourceManager resourceManager
    ) {
        LinkedHashSet<String> tracks = new LinkedHashSet<>();

        for (Album.Track entry : entries) {
            if (entry.folder()) {
                tracks.addAll(folderTracks(albumId.getNamespace(), entry.track(), resourceManager));
            } else {
                tracks.add(entry.track());
            }
        }

        return new ArrayList<>(tracks);
    }

    private static Set<String> forcedEnabledTracks(
            Identifier albumId,
            List<Album.Track> entries,
            ResourceManager resourceManager
    ) {
        Set<String> tracks = new HashSet<>();

        for (Album.Track entry : entries) {
            if (!entry.enabled()) {
                continue;
            }

            if (entry.folder()) {
                tracks.addAll(folderTracks(albumId.getNamespace(), entry.track(), resourceManager));
            } else {
                tracks.add(entry.track());
            }
        }

        return tracks;
    }

    private static List<String> folderTracks(
            String albumNamespace,
            String folder,
            ResourceManager resourceManager
    ) {
        SafeIdentifier folderId = folder.contains(":")
                ? SafeIdentifier.parse(folder)
                : SafeIdentifier.fromNamespaceAndPath(albumNamespace, folder);

        LinkedHashSet<String> tracks = new LinkedHashSet<>();

        tracks.addAll(resourceFolderTracks(folderId, resourceManager));
        tracks.addAll(SafeMusicHelper.tracksInFolder(folderId));

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

        String normalized = SafeMusicHelper.normalize(validFolderId.getPath());

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
}