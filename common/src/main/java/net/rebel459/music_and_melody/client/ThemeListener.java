package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.unified.api.core.UnifiedInstance;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ThemeListener extends SimpleJsonResourceReloadListener<Playlist.Record> {

    public static final Identifier ID = MusicAndMelody.id("themes");

    private final Set<Playlist> loadedPlaylists = new HashSet<>();

    public ThemeListener() {
        super(Playlist.Record.CODEC, FileToIdConverter.json("themes"));
    }

    @Override
    protected void apply(Map<Identifier, Playlist.Record> identifierRecordMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {}
}
