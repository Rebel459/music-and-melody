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
import net.rebel459.music_and_melody.platform.MaMPlatform;

import java.util.*;

public class PlaylistListener extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = MusicAndMelody.id("playlists");

    private final Set<Playlist> loadedPlaylists = new HashSet<>();

    public PlaylistListener() {
        super(new Gson(), "playlists");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Playlist.PLAYLISTS.removeAll(loadedPlaylists);
        loadedPlaylists.clear();

        Map<ResourceLocation, Playlist.Record> recordMap = decode(jsonMap);
        for (Map.Entry<ResourceLocation, Playlist.Record> entry : recordMap.entrySet()) {
            boolean shouldLoad = true;
            for (String mod : entry.getValue().dependencies()) {
                if (!MaMPlatform.PLATFORM.isModLoaded(mod)) {
                    shouldLoad = false;
                    break;
                }
            }
            if (!shouldLoad) continue;
            Playlist playlist = Playlist.create(entry.getKey(), entry.getValue(), null);
            loadedPlaylists.add(playlist);
        }

        Playlist.reloadConfigPlaylists();
    }

    private static Map<ResourceLocation, Playlist.Record> decode(Map<ResourceLocation, JsonElement> jsonMap) {
        Map<ResourceLocation, Playlist.Record> recordMap = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            Playlist.Record.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LogUtils.getLogger().warn("Failed to parse playlist {}: {}", entry.getKey(), error))
                    .ifPresent(record -> recordMap.put(entry.getKey(), record));
        }
        return recordMap;
    }
}
