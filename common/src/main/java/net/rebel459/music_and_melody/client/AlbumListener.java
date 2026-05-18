package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        for (Map.Entry<Identifier, Album.Record> entry : identifierRecordMap.entrySet()) {
            Album.Record record = entry.getValue();
            Album album = new Album(entry.getKey(), record.name(), record.icon(), record.composers(), record.songs());
            loadedAlbums.add(album);
        }
    }
}
