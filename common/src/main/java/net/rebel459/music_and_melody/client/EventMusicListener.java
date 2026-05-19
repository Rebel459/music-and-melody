package net.rebel459.music_and_melody.client;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.util.*;

public class EventMusicListener extends SimpleJsonResourceReloadListener<EventMusic.Record> {

    public static final Identifier ID = MusicAndMelody.id("events");

    private final Set<EventMusic> loaded = new HashSet<>();

    public EventMusicListener() {
        super(EventMusic.Record.CODEC, FileToIdConverter.json("events"));
    }

    @Override
    protected void apply(Map<Identifier, EventMusic.Record> identifierRecordMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        EventMusic.LOADED.removeAll(loaded);
        loaded.clear();

        for (Map.Entry<Identifier, EventMusic.Record> entry : identifierRecordMap.entrySet()) {
            entry.getValue().entries().forEach(dynEntry -> {
                EventMusic.CategoryType category;
                List<EventMusic.Condition> conditions = new ArrayList<>();
                if (Objects.equals(dynEntry.category(), "album")) category = EventMusic.CategoryType.ALBUM;
                else if (Objects.equals(dynEntry.category(), "playlist")) category = EventMusic.CategoryType.PLAYLIST;
                else if (Objects.equals(dynEntry.category(), "song")) category = EventMusic.CategoryType.SONG;
                else if (Objects.equals(dynEntry.category(), "disc")) category = EventMusic.CategoryType.DISC;
                else {
                    LogUtils.getLogger().warn("Invalid category: " + dynEntry.category());
                    return;
                }
                dynEntry.conditions().forEach(condition -> {
                    EventMusic.ConditionType conditionType;
                    Optional<Identifier> idValue = Optional.empty();
                    Optional<Integer> intValue = Optional.empty();
                    Optional< EventMusic.TimeCondition> timeValue = Optional.empty();
                    Optional<EventMusic.WeatherCondition> weatherValue = Optional.empty();
                    if (condition.type().equals("menu")) conditionType = EventMusic.ConditionType.MENU;
                    else if (condition.type().equals("dimension")) {
                        conditionType = EventMusic.ConditionType.DIMENSION;
                        idValue = Optional.of(Identifier.parse(condition.value().get().left().get()));
                    }
                    else if (condition.type().equals("biome")) {
                        conditionType = EventMusic.ConditionType.BIOME;
                        idValue = Optional.of(Identifier.parse(condition.value().get().left().get()));
                    }
                    else if (condition.type().equals("biome_tag")) {
                        conditionType = EventMusic.ConditionType.BIOME_TAG;
                        idValue = Optional.of(Identifier.parse(condition.value().get().left().get()));
                    }
                    else if (condition.type().equals("above_y")) {
                        conditionType = EventMusic.ConditionType.ABOVE_Y;
                        intValue = Optional.of(condition.value().get().right().get());
                    }
                    else if (condition.type().equals("below_y")) {
                        conditionType = EventMusic.ConditionType.BELOW_Y;
                        intValue = Optional.of(condition.value().get().right().get());
                    }
                    else if (condition.type().equals("time")) {
                        conditionType = EventMusic.ConditionType.TIME;
                        String time = condition.value().get().left().get().toLowerCase();
                        if (time.equals("day")) timeValue = Optional.of(EventMusic.TimeCondition.DAY);
                        else if (time.equals("night")) timeValue = Optional.of(EventMusic.TimeCondition.NIGHT);
                        else if (time.equals("sunset")) timeValue = Optional.of(EventMusic.TimeCondition.SUNSET);
                        else if (time.equals("sunrise")) timeValue = Optional.of(EventMusic.TimeCondition.SUNRISE);
                        else {
                            LogUtils.getLogger().warn("Invalid time: " + time);
                            return;
                        }
                    }
                    else if (condition.type().equals("weather")) {
                        conditionType = EventMusic.ConditionType.WEATHER;
                        String weather = condition.value().get().left().get().toLowerCase();
                        if (weather.equals("clear")) weatherValue = Optional.of(EventMusic.WeatherCondition.CLEAR);
                        else if (weather.equals("rain")) weatherValue = Optional.of(EventMusic.WeatherCondition.RAIN);
                        else if (weather.equals("thunder")) weatherValue = Optional.of(EventMusic.WeatherCondition.THUNDER);
                        else {
                            LogUtils.getLogger().warn("Invalid weather: " + weather);
                            return;
                        }
                    }
                    else {
                        LogUtils.getLogger().warn("Invalid condition: " + condition.type());
                        return;
                    }
                    conditions.add(new EventMusic.Condition(conditionType, idValue, intValue, timeValue, weatherValue));
                });
                EventMusic eventMusic = new EventMusic(category, Identifier.parse(dynEntry.music()), conditions);
                loaded.add(eventMusic);
            });
        }
    }
}
