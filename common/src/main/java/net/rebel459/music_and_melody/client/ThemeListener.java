package net.rebel459.music_and_melody.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.client.util.ThemeHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ThemeListener extends SimpleJsonResourceReloadListener<Theme.Record> {

    public static final Identifier ID = MusicAndMelody.id("themes");
    public static final Path CONFIG_DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "themes");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Identifier, Theme.Record> RESOURCE_RECORDS = new LinkedHashMap<>();
    private static final Map<Identifier, Theme.Record> RECORDS = new LinkedHashMap<>();
    private static final Map<Identifier, Path> CONFIG_PATHS = new HashMap<>();
    private static final Map<Identifier, Theme> THEMES = new LinkedHashMap<>();
    private static Theme.Record bundledDefault;

    public ThemeListener() {
        super(Theme.Record.CODEC, FileToIdConverter.json("themes"));
        bootstrapDefault();
    }

    @Override
    protected synchronized void apply(Map<Identifier, Theme.Record> records, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        RESOURCE_RECORDS.clear();
        Theme.Record fallback = bundledDefault();
        if (fallback != null) RESOURCE_RECORDS.put(Theme.DEFAULT_ID, fallback);
        for (Map.Entry<Identifier, Theme.Record> entry : records.entrySet()) {
            RESOURCE_RECORDS.put(entry.getKey(), entry.getValue().withId(entry.getKey()));
        }
        rebuild();
    }

    private static synchronized void bootstrapDefault() {
        if (RESOURCE_RECORDS.containsKey(Theme.DEFAULT_ID) && !THEMES.isEmpty()) return;
        Theme.Record fallback = bundledDefault();
        if (fallback == null) return;
        RESOURCE_RECORDS.put(Theme.DEFAULT_ID, fallback);
        rebuild();
    }

    private static Theme.Record bundledDefault() {
        if (bundledDefault != null) return bundledDefault;
        String path = "/assets/" + MusicAndMelody.MOD_ID + "/themes/default.json";
        try (InputStream stream = ThemeListener.class.getResourceAsStream(path)) {
            if (stream == null) {
                LogUtils.getLogger().error("Missing bundled default theme: " + path);
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                bundledDefault = Theme.Record.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(error ->
                        LogUtils.getLogger().error("Invalid bundled default theme: " + error))
                        .map(record -> record.withId(Theme.DEFAULT_ID))
                        .orElse(null);
            }
        } catch (Exception exception) {
            LogUtils.getLogger().error("Failed to load bundled default theme", exception);
        }
        return bundledDefault;
    }

    public static synchronized List<Theme> themes() {
        return THEMES.values().stream()
                .sorted(Comparator.comparing(theme -> theme.name.getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static synchronized Theme theme(Identifier id) {
        return THEMES.get(id);
    }

    public static synchronized Theme activeTheme() {
        String configuredId = MaMDataConfig.get().active_theme;
        Identifier id = configuredThemeId(configuredId);
        Theme active = id == null ? null : THEMES.get(id);
        if (active == null || !active.valid) {
            active = THEMES.get(Theme.DEFAULT_ID);
            if (active == null || !active.valid) return null;
        }
        return active;
    }

    private static Identifier configuredThemeId(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (!trimmed.contains(":")) return Identifier.tryParse(MusicAndMelody.MOD_ID + ":" + trimmed);
        return Identifier.tryParse(trimmed);
    }

    public static synchronized void preview(Identifier id) {
        Theme selected = THEMES.get(id);
        if (selected != null && selected.valid) ThemeHelper.apply(selected);
    }

    public static synchronized void restoreActive() {
        ThemeHelper.apply(activeTheme());
    }

    public static synchronized boolean apply(Identifier id) {
        Theme selected = THEMES.get(id);
        if (selected == null || !selected.valid) return false;
        MaMDataConfig.get().active_theme = id.toString();
        saveDataConfig();
        ThemeHelper.apply(selected);
        return true;
    }

    public static synchronized boolean isDownloaded(Identifier id) {
        if (id == null) return false;
        return RemoteContentManager.packs().stream()
                .anyMatch(pack -> pack.tags().contains(RemotePack.Tag.THEME)
                        && RemoteContentManager.isDownloaded(id, RemotePack.Tag.THEME));
    }

    public static synchronized Path configPath(Identifier id) {
        return CONFIG_PATHS.get(id);
    }

    public static synchronized Theme createConfigTheme(String name, String description, String icon, String pathOverride) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) return null;
        Path target = configTarget(trimmedName, pathOverride == null ? "" : pathOverride);
        if (target == null || Files.exists(target)) return null;

        String iconValue = icon == null || icon.isBlank() ? Theme.DEFAULT_ICON.toString() : icon.trim();
        Theme.Record record = new Theme.Record(null,
                Component.literal(trimmedName),
                Optional.of(description == null || description.isBlank() ? net.minecraft.network.chat.CommonComponents.EMPTY : Component.literal(description.trim())),
                Optional.of(iconValue),
                Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
        if (!writeConfigRecord(target, record)) return null;
        reloadConfigThemes();
        return CONFIG_PATHS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(target))
                .map(entry -> THEMES.get(entry.getKey()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static synchronized String previewConfigThemePath(String name) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) return "";
        Path target = configTarget(trimmedName, "");
        if (target == null) return "";
        String path = CONFIG_DIRECTORY.relativize(target).toString().replace('\\', '/');
        return path.endsWith(".json") ? path.substring(0, path.length() - ".json".length()) : path;
    }

    public static synchronized boolean saveConfigTheme(Theme.Record record) {
        if (record == null || record.id() == null || !"config".equals(record.id().getNamespace())) return false;
        Path path = CONFIG_PATHS.get(record.id());
        if (path == null || !writeConfigRecord(path, record)) return false;
        reloadConfigThemes();
        return true;
    }

    public static synchronized boolean deleteConfigTheme(Identifier id) {
        Path path = CONFIG_PATHS.get(id);
        if (path == null) return false;
        try {
            if (!Files.deleteIfExists(path)) return false;
            if (Objects.equals(MaMDataConfig.get().active_theme, id.toString())) {
                MaMDataConfig.get().active_theme = Theme.DEFAULT_ID.toString();
                saveDataConfig();
            }
            reloadConfigThemes();
            return true;
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to delete theme config: " + path, exception);
            return false;
        }
    }

    public static synchronized void reloadConfigThemes() {
        rebuild();
    }

    private static void rebuild() {
        RECORDS.clear();
        RECORDS.putAll(RESOURCE_RECORDS);
        CONFIG_PATHS.clear();

        for (Map.Entry<Identifier, Theme.Record> entry : readConfigRecords().entrySet()) {
            RECORDS.put(entry.getKey(), entry.getValue());
        }

        THEMES.clear();
        if (RECORDS.containsKey(Theme.DEFAULT_ID)) resolve(Theme.DEFAULT_ID, new ArrayDeque<>(), new HashSet<>());
        for (Identifier id : RECORDS.keySet()) resolve(id, new ArrayDeque<>(), new HashSet<>());
        ThemeHelper.apply(activeTheme());
    }

    private static Theme resolve(Identifier id, Deque<Identifier> stack, Set<Identifier> resolving) {
        Theme existing = THEMES.get(id);
        if (existing != null) return existing;

        Theme.Record record = RECORDS.get(id);
        if (record == null) return THEMES.get(Theme.DEFAULT_ID);
        if (!resolving.add(id)) return THEMES.get(Theme.DEFAULT_ID);
        stack.push(id);

        Theme inherited = null;
        Identifier parentId = Theme.DEFAULT_ID;
        if (!Theme.DEFAULT_ID.equals(id)) {
            inherited = THEMES.get(Theme.DEFAULT_ID);
            String rawParent = record.parent().orElse(Theme.DEFAULT_ID.toString());
            Identifier parsedParent = Identifier.tryParse(rawParent);
            if (parsedParent != null && !resolving.contains(parsedParent) && RECORDS.containsKey(parsedParent)) {
                parentId = parsedParent;
                inherited = resolve(parsedParent, stack, resolving);
            }
        }

        List<String> errors = new ArrayList<>();
        var panels = record.panels().orElse(null);
        var elements = record.elements().orElse(null);
        var text = record.text().orElse(null);

        net.minecraft.network.chat.Component name = record.name();
        net.minecraft.network.chat.Component description = record.description()
                .orElse(net.minecraft.network.chat.CommonComponents.EMPTY);
        Identifier icon = identifier(record.icon(), null, "icon", errors);

        Theme.Panels resolvedPanels = new Theme.Panels(
                color(panels == null ? Optional.empty() : panels.background(), inherited == null ? null : inherited.panels.background(), "panels.background", errors),
                color(panels == null ? Optional.empty() : panels.panelBackground(), inherited == null ? null : inherited.panels.panelBackground(), "panels.panel_background", errors),
                color(panels == null ? Optional.empty() : panels.panelOutline(), inherited == null ? null : inherited.panels.panelOutline(), "panels.panel_outline", errors),
                color(panels == null ? Optional.empty() : panels.panelHighlighted(), inherited == null ? null : inherited.panels.panelHighlighted(), "panels.panel_highlighted", errors),
                color(panels == null ? Optional.empty() : panels.popupPanelBackground(), inherited == null ? null : inherited.panels.popupPanelBackground(), "panels.popup_panel_background", errors),
                color(panels == null ? Optional.empty() : panels.popupOutline(), inherited == null ? null : inherited.panels.popupOutline(), "panels.popup_outline", errors),
                color(panels == null ? Optional.empty() : panels.popupOverlay(), inherited == null ? null : inherited.panels.popupOverlay(), "panels.popup_overlay", errors)
        );

        Theme.Elements resolvedElements = new Theme.Elements(
                color(elements == null ? Optional.empty() : elements.buttonBackground(), inherited == null ? null : inherited.elements.buttonBackground(), "elements.button_background", errors),
                color(elements == null ? Optional.empty() : elements.buttonHighlighted(), inherited == null ? null : inherited.elements.buttonHighlighted(), "elements.button_highlighted", errors),
                color(elements == null ? Optional.empty() : elements.buttonDisabled(), inherited == null ? null : inherited.elements.buttonDisabled(), "elements.button_disabled", errors),
                color(elements == null ? Optional.empty() : elements.outline(), inherited == null ? null : inherited.elements.outline(), "elements.outline", errors),
                color(elements == null ? Optional.empty() : elements.barBackground(), inherited == null ? null : inherited.elements.barBackground(), "elements.bar_background", errors),
                color(elements == null ? Optional.empty() : elements.barThumb(), inherited == null ? null : inherited.elements.barThumb(), "elements.bar_thumb", errors),
                elements == null
                        ? inherited != null && inherited.elements.buttonTextures()
                        : elements.buttonTextures().orElse(inherited != null && inherited.elements.buttonTextures())
        );

        Theme.Text resolvedText = new Theme.Text(
                color(text == null ? Optional.empty() : text.selected(), inherited == null ? null : inherited.text.selected(), "text.selected", errors),
                color(text == null ? Optional.empty() : text.title(), inherited == null ? null : inherited.text.title(), "text.title", errors),
                color(text == null ? Optional.empty() : text.primary(), inherited == null ? null : inherited.text.primary(), "text.primary", errors),
                color(text == null ? Optional.empty() : text.primaryHighlighted(), inherited == null ? null : inherited.text.primaryHighlighted(), "text.primary_highlighted", errors),
                color(text == null ? Optional.empty() : text.description(), inherited == null ? null : inherited.text.description(), "text.description", errors),
                color(text == null ? Optional.empty() : text.header(), inherited == null ? null : inherited.text.header(), "text.header", errors),
                color(text == null ? Optional.empty() : text.headerSecondary(), inherited == null ? null : inherited.text.headerSecondary(), "text.header_secondary", errors),
                color(text == null ? Optional.empty() : text.favourite(), inherited == null ? null : inherited.text.favourite(), "text.favourite", errors),
                color(text == null ? Optional.empty() : text.example(), inherited == null ? null : inherited.text.example(), "text.example", errors),
                color(text == null ? Optional.empty() : text.disabled(), inherited == null ? null : inherited.text.disabled(), "text.disabled", errors),
                color(text == null ? Optional.empty() : text.warning(), inherited == null ? null : inherited.text.warning(), "text.warning", errors),
                text == null ? inherited == null || inherited.text.shadow()
                        : text.shadow().orElse(inherited == null || inherited.text.shadow())
        );

        Theme resolved = new Theme(id, name, description, icon, parentId, resolvedPanels, resolvedElements,
                resolvedText, record, errors.isEmpty(), errors);
        stack.pop();
        resolving.remove(id);
        THEMES.put(id, resolved);
        return resolved;
    }

    private static String color(Optional<String> value, String inherited, String path, List<String> errors) {
        if (value.isEmpty()) {
            if (inherited != null) return inherited;
            errors.add(path + " is required by the default theme");
            return "";
        }
        if ("panels.popup_overlay".equals(path) && value.get().trim().isEmpty()) {
            return "#00000000";
        }
        Integer parsed = parseColor(value.get());
        if (parsed == null) {
            errors.add(path + " must be an 8-digit ARGB colour");
            return inherited;
        }
        return formatColor(parsed);
    }

    private static Identifier identifier(Optional<String> value, Identifier inherited, String path, List<String> errors) {
        if (value.isEmpty()) {
            return inherited == null ? Theme.DEFAULT_ICON : inherited;
        }
        Identifier parsed = Identifier.tryParse(value.get());
        if (parsed == null) {
            errors.add(path + " must be a valid identifier");
            return inherited == null ? Theme.DEFAULT_ICON : inherited;
        }
        return parsed;
    }

    public static Integer parseColor(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (normalized.length() != 8) return null;
        try {
            long parsed = Long.parseLong(normalized, 16);
            return (int) parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String formatColor(int value) {
        return String.format(Locale.ROOT, "#%08X", value);
    }

    private static Map<Identifier, Theme.Record> readConfigRecords() {
        Map<Identifier, Theme.Record> records = new LinkedHashMap<>();
        try {
            Files.createDirectories(CONFIG_DIRECTORY);
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to create theme config directory: " + CONFIG_DIRECTORY, exception);
            return records;
        }
        if (!Files.isDirectory(CONFIG_DIRECTORY)) return records;

        try (var stream = Files.walk(CONFIG_DIRECTORY)) {
            List<Path> files = stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            Set<String> usedPaths = new HashSet<>();
            for (Path file : files) {
                String relative = CONFIG_DIRECTORY.relativize(file).toString().replace('\\', '/');
                relative = relative.substring(0, relative.length() - ".json".length());
                String unique = uniquePath(sanitizePath(relative), usedPaths);
                if (unique.isBlank()) continue;
                Identifier id = Identifier.fromNamespaceAndPath("config", unique);
                Theme.Record record = readRecord(file, id);
                if (record == null) continue;
                CONFIG_PATHS.put(id, file);
                records.put(id, record);
            }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to read theme config directory: " + CONFIG_DIRECTORY, exception);
        }
        return records;
    }

    private static Theme.Record readRecord(Path path, Identifier id) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement json = JsonParser.parseReader(reader);
            return Theme.Record.CODEC.parse(JsonOps.INSTANCE, json).result().map(record -> record.withId(id)).orElse(null);
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Skipping invalid theme JSON: " + path, exception);
            return null;
        }
    }

    private static boolean writeConfigRecord(Path path, Theme.Record record) {
        try {
            Files.createDirectories(path.getParent());
            JsonElement json = Theme.Record.CODEC.encodeStart(JsonOps.INSTANCE, record).result().orElse(null);
            if (json == null) return false;
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
            return true;
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Failed to write theme config: " + path, exception);
            return false;
        }
    }

    private static void saveDataConfig() {
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static Path configTarget(String name, String pathOverride) {
        String rawPath = pathOverride.trim().isEmpty() ? sanitize(name) : sanitizePath(pathOverride.trim());
        if (rawPath.isBlank()) return null;
        if (!rawPath.toLowerCase(Locale.ROOT).endsWith(".json")) rawPath += ".json";
        Path target = CONFIG_DIRECTORY.resolve(rawPath).normalize();
        Path root = CONFIG_DIRECTORY.toAbsolutePath().normalize();
        return target.toAbsolutePath().normalize().startsWith(root) ? target : null;
    }

    private static String uniquePath(String path, Set<String> usedPaths) {
        if (usedPaths.add(path)) return path;
        for (int i = 2; ; i++) {
            String candidate = path + "_" + i;
            if (usedPaths.add(candidate)) return candidate;
        }
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("[^a-z0-9._-]", "");
    }

    private static String sanitizePath(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.replace('\\', '/').split("/")) {
            String sanitized = sanitize(part);
            if (sanitized.isBlank() || ".".equals(sanitized) || "..".equals(sanitized)) continue;
            if (result.length() > 0) result.append('/');
            result.append(sanitized);
        }
        return result.toString();
    }
}
