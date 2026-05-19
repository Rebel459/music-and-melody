package net.rebel459.music_and_melody.client;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.*;

public class EventMusic {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Set<EventMusic> LOADED = new HashSet<>();

    public CategoryType category;
    public Identifier music;
    public List<Condition> conditions;

    public EventMusic(CategoryType category, Identifier music, List<Condition> conditions) {
        this.category = category;
        this.music = music;
        this.conditions = conditions;
        LOADED.add(this);
    }

    public enum CategoryType {
        ALBUM,
        PLAYLIST,
        SONG,
        DISC
    }

    public enum ConditionType {
        DIMENSION,
        BIOME,
        BIOME_TAG,
        TIME,
        WEATHER,
        MENU,
        BELOW_Y,
        ABOVE_Y
    }

    public enum TimeCondition {
        DAY,
        NIGHT,
        SUNRISE,
        SUNSET
    }

    public enum WeatherCondition {
        CLEAR,
        RAIN,
        THUNDER
    }

    public record Record(List<Entry> entries) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Record::entries)
        ).apply(instance, Record::new));

        public record Entry(String category, String music, List<Condition> conditions) {
            private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("category").forGetter(type -> type.category),
                    Codec.STRING.fieldOf("music").forGetter(type -> type.music),
                    Condition.CODEC.listOf().fieldOf("conditions").forGetter(Entry::conditions)
            ).apply(instance, Entry::new));
        }

        public record Condition(String type, Optional<Either<String, Integer>> value) {
            private static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("type").forGetter(type -> type.type),
                    Codec.either(Codec.STRING, Codec.INT).optionalFieldOf("value").forGetter(func -> func.value)
            ).apply(instance, Condition::new));
        }
    }

    public record Condition(ConditionType type, Optional<Identifier> idValue, Optional<Integer> intValue, Optional<TimeCondition> timeValue, Optional<WeatherCondition> weatherValue) {}
}
