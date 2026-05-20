package net.rebel459.music_and_melody.client;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EventMusic {

    public static int currentBreak = -1;
    public static int targetBreak = -1;

    public static int createMusicBreak() {
        return SoundInstance.createUnseededRandom().nextIntBetweenInclusive(MaMClientConfig.get().event_music_min * 20, MaMClientConfig.get().event_music_max * 20);
    }

    public static void resetMusicBreak() {
        currentBreak = -1;
        targetBreak = -1;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_FILE = Path.of("config", MusicAndMelody.MOD_ID, "event_music.json");
    private static final Set<EventMusic> CONFIG_EVENTS = new HashSet<>();

    public static Set<EventMusic> VERY_HIGH_PRIORITY = new HashSet<>();
    public static Set<EventMusic> HIGH_PRIORITY = new HashSet<>();
    public static Set<EventMusic> MEDIUM_PRIORITY = new HashSet<>();
    public static Set<EventMusic> LOW_PRIORITY = new HashSet<>();
    public static Set<EventMusic> VERY_LOW_PRIORITY = new HashSet<>();

    public CategoryType category;
    public Identifier music;
    public List<Condition> conditions;
    public PriorityType priority;
    public int weight;

    public EventMusic(CategoryType category, Identifier music, List<Condition> conditions, PriorityType priority, int weight) {
        this.category = category;
        this.music = music;
        this.conditions = conditions;
        this.priority = priority;
        this.weight = weight;

        switch (priority) {
            case PriorityType.VERY_HIGH -> VERY_HIGH_PRIORITY.add(this);
            case PriorityType.HIGH -> HIGH_PRIORITY.add(this);
            case PriorityType.MEDIUM -> MEDIUM_PRIORITY.add(this);
            case PriorityType.LOW -> LOW_PRIORITY.add(this);
            case PriorityType.VERY_LOW -> VERY_LOW_PRIORITY.add(this);
        }
    }

    public static synchronized void reloadConfigEvents() {
        VERY_HIGH_PRIORITY.removeAll(CONFIG_EVENTS);
        HIGH_PRIORITY.removeAll(CONFIG_EVENTS);
        MEDIUM_PRIORITY.removeAll(CONFIG_EVENTS);
        LOW_PRIORITY.removeAll(CONFIG_EVENTS);
        VERY_LOW_PRIORITY.removeAll(CONFIG_EVENTS);
        CONFIG_EVENTS.clear();

        Record record = readConfigRecord();
        if (record == null) return;

        for (Record.Entry entry : record.entries()) {
            create(entry).ifPresent(CONFIG_EVENTS::add);
        }
    }

    public static synchronized List<Record.Entry> readConfigEntries() {
        Record record = readConfigRecord();
        return record == null ? new ArrayList<>() : new ArrayList<>(record.entries());
    }

    public static synchronized boolean saveConfigEntries(List<Record.Entry> entries) {
        boolean success = writeConfigRecord(new Record(entries));
        if (!success) return false;

        reloadConfigEvents();
        return true;
    }

    public static Optional<EventMusic> create(Record.Entry entry) {
        CategoryType category = category(entry.category());
        Identifier music = Identifier.tryParse(entry.music());
        PriorityType priority = priority(entry.priority());

        if (category == null || music == null || priority == null) {
            LogUtils.getLogger().warn("Invalid event music entry: " + entry);
            return Optional.empty();
        }

        List<Condition> conditions = new ArrayList<>();
        for (Record.Condition condition : entry.conditions()) {
            Optional<Condition> parsed = condition(condition);
            if (parsed.isEmpty()) return Optional.empty();
            conditions.add(parsed.get());
        }

        return Optional.of(new EventMusic(category, music, conditions, priority, Math.max(1, entry.weight())));
    }

    public static String categoryName(CategoryType category) {
        return category.name().toLowerCase(Locale.ROOT);
    }

    public static String priorityName(PriorityType priority) {
        return priority.name().toLowerCase(Locale.ROOT);
    }

    private static Record readConfigRecord() {
        if (!Files.isRegularFile(CONFIG_FILE)) {
            Record defaults = defaultEventMusic();
            writeConfigRecord(defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonElement json = JsonParser.parseReader(reader);
            return Record.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Failed to read event music config: " + CONFIG_FILE, exception);
            return null;
        }
    }

    private static boolean writeConfigRecord(Record record) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to create event music config directory: " + CONFIG_FILE.getParent(), exception);
            return false;
        }

        JsonElement json = Record.CODEC.encodeStart(JsonOps.INSTANCE, record).result().orElse(null);
        if (json == null) {
            LogUtils.getLogger().warn("Failed to encode event music config: " + CONFIG_FILE);
            return false;
        }

        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
            return true;
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to write event music config: " + CONFIG_FILE, exception);
            return false;
        }
    }

    private static Record defaultEventMusic() {
        return new Record(List.of(
                new Record.Entry(
                        "song",
                        "minecraft:music.overworld.forest",
                        List.of(
                                new Record.Condition(
                                        "biome_tag",
                                        Optional.of(Either.left("minecraft:is_forest"))
                                )
                        ),
                        "medium",
                        1
                ),
                new Record.Entry(
                        "song",
                        "minecraft:music.overworld.dripstone_caves",
                        List.of(
                                new Record.Condition(
                                        "biome",
                                        Optional.of(Either.left("minecraft:dripstone_caves"))
                                )
                        ),
                        "medium",
                        1
                ),
                new Record.Entry(
                        "song",
                        "minecraft:music.menu",
                        List.of(
                                new Record.Condition(
                                        "event",
                                        Optional.of(Either.left("menu"))
                                )
                        ),
                        "low",
                        1
                )
        ));
    }

    private static Optional<Condition> condition(Record.Condition condition) {
        ConditionType type = conditionType(condition.type());

        if (type == null) {
            LogUtils.getLogger().warn("Invalid event music condition: " + condition.type());
            return Optional.empty();
        }

        if (condition.value().isEmpty()) {
            LogUtils.getLogger().warn("Missing event music condition value: " + condition.type());
            return Optional.empty();
        }

        Either<String, Integer> value = condition.value().get();
        Optional<Identifier> idValue = Optional.empty();
        Optional<Integer> intValue = Optional.empty();
        Optional<TimeCondition> timeValue = Optional.empty();
        Optional<WeatherCondition> weatherValue = Optional.empty();
        Optional<GameModeCondition> gameModeValue = Optional.empty();
        Optional<EventCondition> eventValue = Optional.empty();

        if (type == ConditionType.ABOVE_Y || type == ConditionType.BELOW_Y) {
            if (value.right().isEmpty()) return Optional.empty();
            intValue = value.right();
        } else {
            if (value.left().isEmpty()) return Optional.empty();

            String stringValue = value.left().get();

            if (type == ConditionType.TIME) {
                TimeCondition time = time(stringValue);
                if (time == null) return Optional.empty();
                timeValue = Optional.of(time);
            } else if (type == ConditionType.WEATHER) {
                WeatherCondition weather = weather(stringValue);
                if (weather == null) return Optional.empty();
                weatherValue = Optional.of(weather);
            } else if (type == ConditionType.GAME_MODE) {
                GameModeCondition gameMode = gameMode(stringValue);
                if (gameMode == null) return Optional.empty();
                gameModeValue = Optional.of(gameMode);
            }  else if (type == ConditionType.EVENT) {
                EventCondition event = event(stringValue);
                if (event == null) return Optional.empty();
                eventValue = Optional.of(event);
            } else {
                Identifier id = Identifier.tryParse(stringValue);
                if (id == null) return Optional.empty();
                idValue = Optional.of(id);
            }
        }

        return Optional.of(new Condition(type, idValue, intValue, timeValue, weatherValue, gameModeValue, eventValue));
    }

    private static CategoryType category(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "album" -> CategoryType.ALBUM;
            case "playlist" -> CategoryType.PLAYLIST;
            case "pool" -> CategoryType.POOL;
            case "song" -> CategoryType.SONG;
            case "disc" -> CategoryType.DISC;
            default -> null;
        };
    }

    private static PriorityType priority(String priority) {
        return switch (priority.toLowerCase(Locale.ROOT)) {
            case "very_low" -> PriorityType.VERY_LOW;
            case "low" -> PriorityType.LOW;
            case "medium" -> PriorityType.MEDIUM;
            case "high" -> PriorityType.HIGH;
            case "very_high" -> PriorityType.VERY_HIGH;
            default -> null;
        };
    }

    private static ConditionType conditionType(String condition) {
        return switch (condition.toLowerCase(Locale.ROOT)) {
            case "dimension" -> ConditionType.DIMENSION;
            case "biome" -> ConditionType.BIOME;
            case "biome_tag" -> ConditionType.BIOME_TAG;
            case "structure" -> ConditionType.STRUCTURE;
            case "time" -> ConditionType.TIME;
            case "weather" -> ConditionType.WEATHER;
            case "game_mode" -> ConditionType.GAME_MODE;
            case "event" -> ConditionType.EVENT;
            case "below_y" -> ConditionType.BELOW_Y;
            case "above_y" -> ConditionType.ABOVE_Y;
            default -> null;
        };
    }

    private static TimeCondition time(String time) {
        return switch (time.toLowerCase(Locale.ROOT)) {
            case "day" -> TimeCondition.DAY;
            case "night" -> TimeCondition.NIGHT;
            case "sunrise" -> TimeCondition.SUNRISE;
            case "sunset" -> TimeCondition.SUNSET;
            default -> null;
        };
    }

    private static WeatherCondition weather(String weather) {
        return switch (weather.toLowerCase(Locale.ROOT)) {
            case "clear" -> WeatherCondition.CLEAR;
            case "rain" -> WeatherCondition.RAIN;
            case "thunder" -> WeatherCondition.THUNDER;
            default -> null;
        };
    }

    private static GameModeCondition gameMode(String gameMode) {
        return switch (gameMode.toLowerCase(Locale.ROOT)) {
            case "survival" -> GameModeCondition.SURVIVAL;
            case "creative" -> GameModeCondition.CREATIVE;
            case "adventure" -> GameModeCondition.ADVENTURE;
            case "spectator" -> GameModeCondition.SPECTATOR;
            default -> null;
        };
    }

    private static EventCondition event(String event) {
        return switch (event.toLowerCase(Locale.ROOT)) {
            case "menu" -> EventCondition.MENU;
            case "dragon" -> EventCondition.DRAGON;
            case "wither" -> EventCondition.WITHER;
            case "end_portal" -> EventCondition.END_PORTAL;
            case "under_water" -> EventCondition.UNDER_WATER;
            default -> null;
        };
    }

    public enum PriorityType {
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }

    public enum CategoryType {
        ALBUM,
        PLAYLIST,
        POOL,
        SONG,
        DISC
    }

    public enum ConditionType {
        DIMENSION,
        BIOME,
        BIOME_TAG,
        STRUCTURE,
        TIME,
        WEATHER,
        GAME_MODE,
        BELOW_Y,
        ABOVE_Y,
        EVENT
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

    public enum GameModeCondition {
        SURVIVAL,
        CREATIVE,
        ADVENTURE,
        SPECTATOR
    }

    public enum EventCondition {
        MENU,
        DRAGON,
        WITHER,
        END_PORTAL,
        UNDER_WATER
    }

    public record Record(List<Entry> entries) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Record::entries)
        ).apply(instance, Record::new));

        public record Entry(String category, String music, List<Condition> conditions, String priority, int weight) {
            private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("category").forGetter(Entry::category),
                    Codec.STRING.fieldOf("music").forGetter(Entry::music),
                    Condition.CODEC.listOf().fieldOf("conditions").forGetter(Entry::conditions),
                    Codec.STRING.optionalFieldOf("priority", "medium").forGetter(Entry::priority),
                    Codec.INT.optionalFieldOf("weight", 1).forGetter(Entry::weight)
            ).apply(instance, Entry::new));
        }

        public record Condition(String type, Optional<Either<String, Integer>> value) {
            private static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("type").forGetter(Condition::type),
                    Codec.either(Codec.STRING, Codec.INT).optionalFieldOf("value").forGetter(Condition::value)
            ).apply(instance, Condition::new));
        }
    }

    public record Condition(
            ConditionType type,
            Optional<Identifier> idValue,
            Optional<Integer> intValue,
            Optional<TimeCondition> timeValue,
            Optional<WeatherCondition> weatherValue,
            Optional<GameModeCondition> gameModeValue,
            Optional<EventCondition> eventValue
    ) {}
}
