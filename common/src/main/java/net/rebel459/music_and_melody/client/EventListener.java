package net.rebel459.music_and_melody.client;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.Map;

public class EventListener extends SimpleJsonResourceReloadListener<Event.Record> {

    public static final Identifier ID = MusicAndMelody.id("events");

    public EventListener() {
        super(Event.Record.CODEC, FileToIdConverter.json("events"));
    }

    @Override
    protected void apply(Map<Identifier, Event.Record> identifierRecordMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Event.reloadResourceEvents(identifierRecordMap);
    }
}
