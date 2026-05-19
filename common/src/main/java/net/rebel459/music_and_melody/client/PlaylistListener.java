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
            Playlist.Record record = entry.getValue();
            List<Identifier> tracks = new ArrayList<>();
            List<Identifier> discs = new ArrayList<>();
            record.tracks().forEach(trackEntry -> trackEntry.tracks().forEach(track -> tracks.add(Identifier.fromNamespaceAndPath(trackEntry.namespace(), track))));
            record.discs().forEach(discEntry -> discEntry.discs().forEach(disc -> discs.add(Identifier.fromNamespaceAndPath(discEntry.namespace(), disc))));
            Playlist album = new Playlist(entry.getKey(), record.name(), record.icon(), tracks, discs);
            loadedPlaylists.add(album);
        }

        // also read playlist jsons in the config/music_and_melody/playlists folder
    }
}
