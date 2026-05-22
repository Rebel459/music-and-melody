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

import java.util.Map;

public class EventListener extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = MusicAndMelody.id("events");

    public EventListener() {
        super(new Gson(), "events");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Event.reloadResourceEvents(decode(jsonMap));
    }

    private static Map<ResourceLocation, Event.Record> decode(Map<ResourceLocation, JsonElement> jsonMap) {
        Map<ResourceLocation, Event.Record> recordMap = new java.util.HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            Event.Record.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> LogUtils.getLogger().warn("Failed to parse event {}: {}", entry.getKey(), error))
                    .ifPresent(record -> recordMap.put(entry.getKey(), record));
        }
        return recordMap;
    }
}
