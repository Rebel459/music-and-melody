package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.util.HashSet;
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
    protected void apply(Map<Identifier, Album.Record> identifierRecordMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Album.ALBUMS.removeAll(loadedAlbums);
        Album.DISABLED_ALBUMS.removeAll(loadedAlbums);
        loadedAlbums.clear();

        Set<Identifier> registeredDiscs = new HashSet<>();
        for (Map.Entry<Identifier, Album.Record> entry : identifierRecordMap.entrySet()) {
            Album.Record record = entry.getValue();
            List<String> tracks = record.tracks().stream().map(Album.Track::track).toList();
            Set<String> forcedEnabledTracks = record.tracks().stream()
                    .filter(Album.Track::enabled)
                    .map(Album.Track::track)
                    .collect(Collectors.toSet());
            List<String> discs = record.discs().stream().map(Album.Disc::disc).toList();
            Set<String> forcedUnlockedDiscs = record.discs().stream()
                    .filter(Album.Disc::unlocked)
                    .map(Album.Disc::disc)
                    .collect(Collectors.toSet());

            for (String disc : discs) {
                Identifier discId = disc.contains(":") ? Identifier.tryParse(disc) : Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(), disc);
                if (discId != null) registeredDiscs.add(discId);
            }
            Album album = new Album(entry.getKey(), record.name(), record.icon(), tracks, forcedEnabledTracks, discs, forcedUnlockedDiscs);
            loadedAlbums.add(album);
        }

        if (MaMClientConfig.get().config_album) {
            Album configAlbum = ConfigAlbum.createAlbum(registeredDiscs);
            if (configAlbum != null) loadedAlbums.add(configAlbum);
        }
    }
}
