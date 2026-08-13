package net.rebel459.music_and_melody.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Event {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_DIR = Path.of("config", MusicAndMelody.MOD_ID, "events");

    private static final List<Source> RESOURCE_SOURCES = new ArrayList<>();
    private static final List<Source> CONFIG_SOURCES = new ArrayList<>();
    private static final List<Source> SOURCES = new ArrayList<>();

    public static Set<Event> VERY_HIGH_PRIORITY = new HashSet<>();
    public static Set<Event> HIGH_PRIORITY = new HashSet<>();
    public static Set<Event> MEDIUM_PRIORITY = new HashSet<>();
    public static Set<Event> LOW_PRIORITY = new HashSet<>();
    public static Set<Event> VERY_LOW_PRIORITY = new HashSet<>();

    public final Identifier source;
    public CategoryType category;
    public SafeIdentifier music;
    public List<Condition> conditions;
    public PriorityType priority;
    public boolean sustain;
    public boolean constant;
    public int weight;

    public Event(Identifier source, CategoryType category, SafeIdentifier music, List<Condition> conditions, PriorityType priority, boolean sustain, boolean constant, int weight) {
        this.source = source;
        this.category = category;
        this.music = music;
        this.conditions = conditions;
        this.priority = priority;
        this.sustain = sustain;
        this.constant = constant;
        this.weight = weight;

        switch (priority) {
            case PriorityType.VERY_HIGH -> VERY_HIGH_PRIORITY.add(this);
            case PriorityType.HIGH -> HIGH_PRIORITY.add(this);
            case PriorityType.MEDIUM -> MEDIUM_PRIORITY.add(this);
            case PriorityType.LOW -> LOW_PRIORITY.add(this);
            case PriorityType.VERY_LOW -> VERY_LOW_PRIORITY.add(this);
        }
    }

    public static synchronized void reloadResourceEvents(Map<Identifier, Record> records) {
        RESOURCE_SOURCES.clear();
        if (!MaMClientConfig.get().allow_events) {
            clearLoadedEvents();
            return;
        }
        records.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString(), String.CASE_INSENSITIVE_ORDER))
                .map(entry -> new Source(entry.getKey(), entry.getValue(), null))
                .forEach(record -> {
                    boolean shouldLoad = true;
                    for (String mod : record.record.dependencies()) {
                        if (!MaMPlatform.PLATFORM.isModLoaded(mod)) {
                            shouldLoad = false;
                            break;
                        }
                    }
                    if (shouldLoad) RESOURCE_SOURCES.add(record);
                });
        reloadConfigEvents();
    }

    public static synchronized void reloadConfigEvents() {
        CONFIG_SOURCES.clear();
        if (!MaMClientConfig.get().allow_events) {
            clearLoadedEvents();
            return;
        }

        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to create event music config directory: " + CONFIG_DIR, exception);
            rebuildEvents();
            return;
        }

        Set<String> usedPaths = new HashSet<>();
        for (Path file : configFiles()) {
            Identifier id = Identifier.fromNamespaceAndPath("config", "events/" + uniquePath(configPath(file), usedPaths));
            Record record = readRecord(file, shortName(id));
            if (record == null) continue;
            boolean shouldLoad = true;
            for (String mod : record.dependencies()) {
                if (!MaMPlatform.PLATFORM.isModLoaded(mod)) {
                    shouldLoad = false;
                    break;
                }
            }
            if (shouldLoad) CONFIG_SOURCES.add(new Source(id, record, file));
        }

        rebuildEvents();
    }

    private static void clearLoadedEvents() {
        RESOURCE_SOURCES.clear();
        CONFIG_SOURCES.clear();
        rebuildEvents();
    }

    public static synchronized List<Source> sources() {
        return List.copyOf(SOURCES);
    }

    public static synchronized List<ScreenEntry> screenEntries() {
        List<ScreenEntry> entries = new ArrayList<>();
        for (Source source : SOURCES) {
            for (int i = 0; i < source.record.entries().size(); i++) {
                entries.add(new ScreenEntry(source, i, source.record.entries().get(i)));
            }
        }
        return entries;
    }

    public static synchronized boolean canCreateConfigSource(String name, String pathOverride) {
        if (name.trim().isEmpty()) return false;
        Path target = configTarget(name, pathOverride);
        return target != null && !Files.exists(target);
    }

    public static synchronized String previewConfigSourcePath(String name) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) return "";
        Path target = configTarget(trimmedName, "");
        if (target == null) return "";
        String path = CONFIG_DIR.relativize(target).toString().replace('\\', '/');
        return path.endsWith(".json") ? path.substring(0, path.length() - ".json".length()) : path;
    }

    public static synchronized Source createConfigSource(String name, String description, String pathOverride) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) return null;
        Path target = configTarget(trimmedName, pathOverride);
        if (target == null || Files.exists(target)) return null;
        Component descriptionComponent = description.trim().isEmpty() ? CommonComponents.EMPTY : Component.literal(description.trim());
        if (!writeConfigRecord(target, new Record(Component.literal(trimmedName), descriptionComponent, List.of(), new ArrayList<>()))) return null;
        reloadConfigEvents();
        for (Source source : CONFIG_SOURCES) {
            if (source.path.equals(target)) return source;
        }
        return CONFIG_SOURCES.isEmpty() ? null : CONFIG_SOURCES.getLast();
    }

    public static synchronized boolean saveSourceEntries(Source source, List<Record.Entry> entries) {
        if (source == null || !source.isConfig()) return false;
        if (!writeConfigRecord(source.path, new Record(source.record.name(), source.record.description(), entries, source.record.dependencies(), source.record.defaultState(), source.record.hasName()))) return false;
        reloadConfigEvents();
        return true;
    }

    public static synchronized boolean importConfigFile(Path file) {
        Record record = readRecord(file, sanitize(stem(file)));
        if (record == null) return false;

        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to create event music config directory: " + CONFIG_DIR, exception);
            return false;
        }

        Path target = uniqueConfigFile(sanitize(stem(file)));
        if (!writeConfigRecord(target, record)) return false;
        reloadConfigEvents();
        return true;
    }

    public static Optional<Event> create(Record.Entry entry, Source source) {
        CategoryType category = category(entry.category());
        SafeIdentifier music = SafeIdentifier.parse(entry.music());
        PriorityType priority = priority(entry.priority());

        if (category == null || music == null || priority == null) {
            LogUtils.getLogger().warn("Invalid event music entry in " + source.id + ": " + entry);
            return Optional.empty();
        }

        List<Condition> conditions = new ArrayList<>();
        for (Record.Condition condition : entry.conditions()) {
            Optional<Condition> parsed = condition(condition);
            if (parsed.isEmpty()) return Optional.empty();
            conditions.add(parsed.get());
        }

        return Optional.of(new Event(source.id, category, music, conditions, priority, entry.sustain, entry.constant, Math.max(1, entry.weight())));
    }

    public static String categoryName(CategoryType category) {
        return category.name().toLowerCase(Locale.ROOT);
    }

    public static String priorityName(PriorityType priority) {
        return priority.name().toLowerCase(Locale.ROOT);
    }

    public static Optional<List<Record.Condition>> parseRecordConditions(String value) {
        try {
            JsonElement json = JsonParser.parseString(value);
            return Record.Condition.CODEC.listOf().parse(JsonOps.INSTANCE, json).result();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static void rebuildEvents() {
        VERY_HIGH_PRIORITY.clear();
        HIGH_PRIORITY.clear();
        MEDIUM_PRIORITY.clear();
        LOW_PRIORITY.clear();
        VERY_LOW_PRIORITY.clear();
        SOURCES.clear();
        SOURCES.addAll(RESOURCE_SOURCES);
        SOURCES.addAll(CONFIG_SOURCES);

        for (Source source : SOURCES) {
            if (!source.isEnabled()) continue;
            for (Record.Entry entry : source.record.entries()) {
                create(entry, source);
            }
        }
    }

    private static List<Path> configFiles() {
        if (!Files.isDirectory(CONFIG_DIR)) return new ArrayList<>();
        try (var stream = Files.walk(CONFIG_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(Event::isJson)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to read event music config directory: " + CONFIG_DIR, exception);
            return new ArrayList<>();
        }
    }

    private static Record readRecord(Path file, String fallbackName) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            Record record = Record.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
            return record == null || record.hasName() ? record : new Record(Component.literal(fallbackName), record.description(), record.entries(), record.dependencies(), record.defaultState(), false);
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Failed to read event music config: " + file, exception);
            return null;
        }
    }

    private static boolean writeConfigRecord(Path file, Record record) {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to create event music config directory: " + file.getParent(), exception);
            return false;
        }

        JsonElement json = Record.CODEC.encodeStart(JsonOps.INSTANCE, record).result().orElse(null);
        if (json == null) {
            LogUtils.getLogger().warn("Failed to encode event music config: " + file);
            return false;
        }

        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
            return true;
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to write event music config: " + file, exception);
            return false;
        }
    }

    private static Path uniqueConfigFile(String stem) {
        Path target = CONFIG_DIR.resolve(stem + ".json");
        if (!Files.exists(target)) return target;
        for (int i = 2; ; i++) {
            Path suffixed = CONFIG_DIR.resolve(stem + "_" + i + ".json");
            if (!Files.exists(suffixed)) return suffixed;
        }
    }

    private static Path configTarget(String name, String pathOverride) {
        String rawPath = pathOverride.trim().isEmpty() ? sanitize(name) : sanitizePath(pathOverride.trim());
        if (rawPath.isBlank()) return null;
        if (!rawPath.toLowerCase(Locale.ROOT).endsWith(".json")) rawPath += ".json";
        Path target = CONFIG_DIR.resolve(rawPath).normalize();
        Path root = CONFIG_DIR.toAbsolutePath().normalize();
        return target.toAbsolutePath().normalize().startsWith(root) ? target : null;
    }

    private static Optional<Condition> condition(Record.Condition condition) {
        ConditionType type = conditionType(condition.type());

        if (type == null) {
            LogUtils.getLogger().warn("Invalid event music condition: " + condition.type());
            return Optional.empty();
        }

        if (type == ConditionType.ALL_OF || type == ConditionType.ANY_OF || type == ConditionType.NOT) {
            if (!(condition.value instanceof Record.Condition.Value.Conditions(List<Record.Condition> value)) || value.isEmpty()) {
                LogUtils.getLogger().warn("Missing nested event music conditions: " + condition.type());
                return Optional.empty();
            }

            List<Condition> conditions = new ArrayList<>();
            for (Record.Condition nested : value) {
                Optional<Condition> parsed = condition(nested);
                if (parsed.isEmpty()) return Optional.empty();
                conditions.add(parsed.get());
            }
            return Optional.of(new Condition(type, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), conditions));
        }

        Record.Condition.Value value = condition.value();
        Optional<String> stringValue = Optional.empty();
        Optional<Identifier> idValue = Optional.empty();
        Optional<Integer> intValue = Optional.empty();
        Optional<Float> floatValue = Optional.empty();
        Optional<TimeCondition> timeValue = Optional.empty();
        Optional<WeatherCondition> weatherValue = Optional.empty();
        Optional<GameModeCondition> gameModeValue = Optional.empty();
        Optional<SpecialCondition> eventValue = Optional.empty();

        if (type == ConditionType.AT_LEAST_Y || type == ConditionType.BELOW_Y) {
            if (!(value instanceof Record.Condition.Value.Integer(int integer))) return Optional.empty();
            intValue = Optional.of(integer);
        }
        else if (type == ConditionType.RANDOM_CHANCE) {
            if (!(value instanceof Record.Condition.Value.Float(float conditionFloat))) return Optional.empty();
            if (conditionFloat < 0F || conditionFloat > 1F) {
                LogUtils.getLogger().warn("Random chance must be a percentage (0-100%), was: " + intValue.get() + "%");
                return Optional.empty();
            }
            floatValue = Optional.of(conditionFloat);
        } else {
            if (!(value instanceof Record.Condition.Value.String(String string))) return Optional.empty();

            if (type == ConditionType.MOD_LOADED || type == ConditionType.BOSSBAR || type == ConditionType.AT_LEAST_VERSION || type == ConditionType.BELOW_VERSION) {
                if (string.isBlank()) return Optional.empty();
                stringValue = Optional.of(string);
            } else if (type == ConditionType.TIME) {
                TimeCondition time = time(string);
                if (time == null) return Optional.empty();
                timeValue = Optional.of(time);
            } else if (type == ConditionType.WEATHER) {
                WeatherCondition weather = weather(string);
                if (weather == null) return Optional.empty();
                weatherValue = Optional.of(weather);
            } else if (type == ConditionType.GAME_MODE) {
                GameModeCondition gameMode = gameMode(string);
                if (gameMode == null) return Optional.empty();
                gameModeValue = Optional.of(gameMode);
            } else if (type == ConditionType.SPECIAL) {
                SpecialCondition event = event(string);
                if (event == null) return Optional.empty();
                eventValue = Optional.of(event);
            } else {
                Identifier id = Identifier.tryParse(string);
                if (id == null) return Optional.empty();
                idValue = Optional.of(id);
            }
        }

        return Optional.of(new Condition(type, stringValue, idValue, intValue, floatValue, timeValue, weatherValue, gameModeValue, eventValue, List.of()));
    }

    private static CategoryType category(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "album" -> CategoryType.ALBUM;
            case "playlist" -> CategoryType.PLAYLIST;
            case "pool" -> CategoryType.POOL;
            case "track" -> CategoryType.TRACK;
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

    private static DefaultState defaultState(String defaultState) {
        return switch (defaultState.toLowerCase(Locale.ROOT)) {
            case "disabled" -> DefaultState.DISABLED;
            case "enabled" -> DefaultState.ENABLED;
            default -> DefaultState.ENABLED;
        };
    }

    private static ConditionType conditionType(String condition) {
        return switch (condition.toLowerCase(Locale.ROOT)) {
            case "dimension" -> ConditionType.DIMENSION;
            case "biome" -> ConditionType.BIOME;
            case "biome_tag" -> ConditionType.BIOME_TAG;
            case "structure" -> ConditionType.STRUCTURE;
            case "structure_tag" -> ConditionType.STRUCTURE_TAG;
            case "time" -> ConditionType.TIME;
            case "weather" -> ConditionType.WEATHER;
            case "game_mode" -> ConditionType.GAME_MODE;
            case "special" -> ConditionType.SPECIAL;
            case "all_of" -> ConditionType.ALL_OF;
            case "any_of" -> ConditionType.ANY_OF;
            case "not" -> ConditionType.NOT;
            case "below_y" -> ConditionType.BELOW_Y;
            case "at_least_y" -> ConditionType.AT_LEAST_Y;
            case "mod_loaded" -> ConditionType.MOD_LOADED;
            case "random_chance" -> ConditionType.RANDOM_CHANCE;
            case "bossbar" -> ConditionType.BOSSBAR;
            case "at_least_version" -> ConditionType.AT_LEAST_VERSION;
            case "below_version" -> ConditionType.BELOW_VERSION;
            case "album_loaded" -> ConditionType.ALBUM_LOADED;
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

    private static SpecialCondition event(String event) {
        return switch (event.toLowerCase(Locale.ROOT)) {
            case "menu" -> SpecialCondition.MENU;
            case "credits" -> SpecialCondition.CREDITS;
            case "end_portal" -> SpecialCondition.END_PORTAL;
            case "under_water" -> SpecialCondition.UNDER_WATER;
            default -> null;
        };
    }

    private static boolean isJson(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static String stem(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".json".length());
    }

    private static String configPath(Path file) {
        String value = CONFIG_DIR.relativize(file).toString().replace('\\', '/');
        return sanitizePath(value.substring(0, value.length() - ".json".length()));
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("[^a-z0-9._-]", "");
        if (sanitized.isBlank()) return "events";
        return sanitized;
    }

    private static String sanitizePath(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replace('\\', '/').replaceAll("\\s+", "_").replaceAll("[^a-z0-9._/-]", "");
        while (sanitized.startsWith("/")) sanitized = sanitized.substring(1);
        return Arrays.stream(sanitized.split("/"))
                .filter(part -> !part.isBlank() && !part.equals(".") && !part.equals(".."))
                .map(Event::sanitize)
                .collect(java.util.stream.Collectors.joining("/"));
    }

    private static String uniquePath(String path, Set<String> usedPaths) {
        if (usedPaths.add(path)) return path;
        for (int i = 2; ; i++) {
            String suffixed = path + "_" + i;
            if (usedPaths.add(suffixed)) return suffixed;
        }
    }

    private static String shortName(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
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
        TRACK,
        DISC
    }

    public enum ConditionType {
        DIMENSION,
        BIOME,
        BIOME_TAG,
        STRUCTURE,
        STRUCTURE_TAG,
        TIME,
        WEATHER,
        GAME_MODE,
        BELOW_Y,
        AT_LEAST_Y,
        SPECIAL,
        ALL_OF,
        ANY_OF,
        NOT,
        MOD_LOADED,
        RANDOM_CHANCE,
        BOSSBAR,
        AT_LEAST_VERSION,
        BELOW_VERSION,
        ALBUM_LOADED
    }

    private enum DefaultState {
        ENABLED,
        DISABLED
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

    public enum SpecialCondition {
        MENU,
        CREDITS,
        END_PORTAL,
        UNDER_WATER
    }

    public static class Source {
        public final Identifier id;
        public final Record record;
        public final Path path;

        private Source(Identifier id, Record record, Path path) {
            this.id = id;
            this.record = record;
            this.path = path;
        }

        public boolean isConfig() {
            return this.path != null;
        }

        public boolean isEnabled() {
            MaMDataConfig.Events events = MaMDataConfig.get().events;
            String id = this.id.toString();
            if (isDefaultDisabled()) {
                return events.enabled_events.contains(id);
            }
            return !events.disabled_events.contains(id);
        }

        public void setEnabled(boolean enabled) {
            MaMDataConfig config = MaMDataConfig.get();
            String id = this.id.toString();
            config.events.enabled_events.remove(id);
            config.events.disabled_events.remove(id);
            if (enabled != defaultEnabled()) {
                if (enabled) {
                    config.events.enabled_events.add(id);
                } else {
                    config.events.disabled_events.add(id);
                }
            }
            AutoConfig.getConfigHolder(MaMDataConfig.class).save();
            rebuildEvents();
        }

        public boolean deleteConfig() {
            if (!isConfig()) return false;
            try {
                if (Files.deleteIfExists(this.path)) {
                    MaMDataConfig config = MaMDataConfig.get();
                    String id = this.id.toString();
                    config.events.disabled_events.remove(id);
                    config.events.enabled_events.remove(id);
                    AutoConfig.getConfigHolder(MaMDataConfig.class).save();
                    reloadConfigEvents();
                    return true;
                }
            } catch (IOException exception) {
                LogUtils.getLogger().warn("Failed to delete event music config: " + this.path, exception);
            }
            return false;
        }

        private boolean isDefaultDisabled() {
            return !defaultEnabled();
        }

        private boolean defaultEnabled() {
            return defaultState(this.record.defaultState()) != DefaultState.DISABLED;
        }
    }

    public record ScreenEntry(Source source, int index, Record.Entry entry) {}

    public record Record(Component name, Component description, List<Entry> entries, List<String> dependencies, String defaultState, boolean hasName) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(record -> record.hasName ? Optional.of(record.name) : Optional.empty()),
                ComponentSerialization.CODEC.optionalFieldOf("description", CommonComponents.EMPTY).forGetter(Record::description),
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Record::entries),
                Codec.STRING.listOf().optionalFieldOf("dependencies", List.of()).forGetter(Record::dependencies),
                Codec.STRING.optionalFieldOf("default", "enabled").forGetter(Record::defaultState)
        ).apply(instance, (name, description, entries, dependencies, defaultState) -> new Record(name.orElse(Component.empty()), description, entries, dependencies, defaultState, name.isPresent())));

        public Record(Component name, Component description, List<String> dependencies, List<Entry> entries) {
            this(name, description, entries, dependencies, "enabled", true);
        }

        public record Entry(String category, String music, List<Condition> conditions, String priority, boolean sustain, boolean constant, int weight) {
            private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("category").forGetter(Entry::category),
                    Codec.STRING.fieldOf("music").forGetter(Entry::music),
                    Condition.CODEC.listOf().fieldOf("conditions").forGetter(Entry::conditions),
                    Codec.STRING.optionalFieldOf("priority", "low").forGetter(Entry::priority),
                    Codec.BOOL.optionalFieldOf("sustain", true).forGetter(Entry::sustain),
                    Codec.BOOL.optionalFieldOf("constant", false).forGetter(Entry::constant),
                    Codec.INT.optionalFieldOf("weight", 1).forGetter(Entry::weight)
            ).apply(instance, Entry::new));
        }

        public record Condition(String type, Value value) {
            public static final Codec<Condition> CODEC = Codec.recursive("SpecialCondition", self -> {
                Codec<Value> valueCodec = Codec.either(
                        Codec.either(
                                Codec.either(Codec.STRING, Codec.INT),
                                Codec.FLOAT
                        ),
                        self.listOf()
                ).xmap(
                        either -> either.map(
                                scalar -> scalar.map(
                                        stringOrInt -> stringOrInt.map(Value.String::new, Value.Integer::new),
                                        Value.Float::new
                                ),
                                Value.Conditions::new
                        ),
                        value -> {
                            if (value instanceof Value.String(String stringValue)) {
                                return Either.left(Either.left(Either.left(stringValue)));
                            }
                            if (value instanceof Value.Integer(int integerValue)) {
                                return Either.left(Either.left(Either.right(integerValue)));
                            }
                            if (value instanceof Value.Float(float floatValue)) {
                                return Either.left(Either.right(floatValue));
                            }
                            if (value instanceof Value.Conditions(List<Condition> conditionsValue)) {
                                return Either.right(conditionsValue);
                            }
                            throw new IllegalStateException("Unknown condition value: " + value);
                        }
                );

                return RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("type").forGetter(Condition::type),
                        valueCodec.fieldOf("value").forGetter(Condition::value)
                ).apply(instance, Condition::new));
            });

            public sealed interface Value permits Value.String, Value.Integer, Value.Float, Value.Conditions {
                record String(java.lang.String value) implements Value {}
                record Integer(int value) implements Value {}
                record Float(float value) implements Value {}
                record Conditions(List<Condition> value) implements Value {}
            }
        }
    }

    public record Condition(
            ConditionType type,
            Optional<String> stringValue,
            Optional<Identifier> idValue,
            Optional<Integer> intValue,
            Optional<Float> floatValue,
            Optional<TimeCondition> timeValue,
            Optional<WeatherCondition> weatherValue,
            Optional<GameModeCondition> gameModeValue,
            Optional<SpecialCondition> eventValue,
            List<Condition> conditions
    ) {}
}
