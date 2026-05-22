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
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AlbumListener extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = MusicAndMelody.id("albums");

    private final Set<Album> loadedAlbums = new HashSet<>();

    public AlbumListener() {
        super(new Gson(), "albums");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Album.ALBUMS.removeAll(loadedAlbums);
        Album.DISABLED_ALBUMS.removeAll(loadedAlbums);
        loadedAlbums.clear();

        Set<ResourceLocation> registeredDiscs = new HashSet<>();
        Map<ResourceLocation, Album.Record> recordMap = decode(jsonMap);
        for (Map.Entry<ResourceLocation, Album.Record> entry : recordMap.entrySet()) {
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
                ResourceLocation discId = disc.contains(":") ? ResourceLocation.tryParse(disc) : ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), disc);
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

    private static Map<ResourceLocation, Album.Record> decode(Map<ResourceLocation, JsonElement> jsonMap) {
        Map<ResourceLocation, Album.Record> recordMap = new java.util.HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            Album.Record.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LogUtils.getLogger().warn("Failed to parse album {}: {}", entry.getKey(), error))
                    .ifPresent(record -> recordMap.put(entry.getKey(), record));
        }
        return recordMap;
    }
}
