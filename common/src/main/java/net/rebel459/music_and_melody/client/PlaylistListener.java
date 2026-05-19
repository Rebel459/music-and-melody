package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.*;

public class PlaylistListener extends SimpleJsonResourceReloadListener<Playlist.Record> {

    public static final Identifier ID = MusicAndMelody.id("playlists");

    private final Set<Playlist> loadedPlaylists = new HashSet<>();

    public PlaylistListener() {
        super(Playlist.Record.CODEC, FileToIdConverter.json("playlists"));
    }

    @Override
    protected void apply(Map<Identifier, Playlist.Record> identifierRecordMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Playlist.PLAYLISTS.removeAll(loadedPlaylists);
        loadedPlaylists.clear();

        for (Map.Entry<Identifier, Playlist.Record> entry : identifierRecordMap.entrySet()) {
            Playlist album = Playlist.create(entry.getKey(), entry.getValue(), null);
            loadedPlaylists.add(album);
        }

        Playlist.reloadConfigPlaylists();
    }
}
