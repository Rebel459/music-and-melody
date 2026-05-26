package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Event;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class EventScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.event_editor");
    private static final Event.CategoryType[] CATEGORIES = Event.CategoryType.values();
    private static final Event.PriorityType[] PRIORITIES = Event.PriorityType.values();
    private static final int CONDITIONS_Y = 114;
    private static final int CONDITIONS_ONE_LINE_HEIGHT = 18;
    private static final int CONDITIONS_TWO_LINE_HEIGHT = 36;
    private static final int CONDITIONS_LIST_GAP = 4;
    private static final int BOTTOM_BUTTON_AREA_HEIGHT = 60;

    private final Screen parent;
    private final List<Event.ScreenEntry> entries = new ArrayList<>();
    private EventList list;
    private EditBox musicField;
    private EditBox weightField;
    private MultiLineEditBox conditionsField;
    private Button categoryButton;
    private Button priorityButton;
    private Button sustainButton;
    private Button addButton;
    private Button saveButton;
    private Button removeButton;
    private Button browseButton;
    private Button expandButton;
    private int selectedIndex = -1;
    private int categoryIndex = Event.CategoryType.PLAYLIST.ordinal();
    private int priorityIndex = Event.PriorityType.LOW.ordinal();
    private boolean sustain = true;
    private boolean listExpanded;
    private boolean loadingEditor;
    private boolean savedChanges;
    private boolean openSourcesOnInit;
    private boolean closeToSources;
    private Screen sourceBrowserParent;
    private ResourceLocation activeSourceId;

    public EventScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        reloadEntries();
    }

    public EventScreen(Screen parent, boolean openSourcesOnInit) {
        this(parent);
        this.openSourcesOnInit = openSourcesOnInit;
    }

    private EventScreen(Screen parent, int selectedIndex, boolean listExpanded, boolean savedChanges, ResourceLocation activeSourceId) {
        super(TITLE);
        this.parent = parent;
        this.selectedIndex = selectedIndex;
        this.listExpanded = listExpanded;
        this.savedChanges = savedChanges;
        this.activeSourceId = activeSourceId;
        reloadEntries();
    }

    private EventScreen(Screen parent, int selectedIndex, boolean listExpanded, boolean savedChanges, ResourceLocation activeSourceId, boolean closeToSources, Screen sourceBrowserParent) {
        this(parent, selectedIndex, listExpanded, savedChanges, activeSourceId);
        this.closeToSources = closeToSources;
        this.sourceBrowserParent = sourceBrowserParent;
    }

    @Override
    protected void init() {
        if (this.openSourcesOnInit) {
            this.openSourcesOnInit = false;
            this.minecraft.setScreen(new EventSourceScreen(this, this.parent));
            return;
        }

        int fieldWidth = Math.min(420, this.width - 20);
        int fieldX = this.width / 2 - fieldWidth / 2;
        int smallWidth = (fieldWidth - 12) / 4;

        if (!this.listExpanded) {
            this.categoryButton = this.addRenderableWidget(Button.builder(categoryMessage(), button -> {
                this.categoryIndex = (this.categoryIndex + 1) % CATEGORIES.length;
                markDirty();
            }).bounds(fieldX, 42, smallWidth, 20).build());
            this.priorityButton = this.addRenderableWidget(Button.builder(priorityMessage(), button -> {
                this.priorityIndex = (this.priorityIndex + 1) % PRIORITIES.length;
                markDirty();
            }).bounds(fieldX + smallWidth + 4, 42, smallWidth, 20).build());
            this.sustainButton = this.addRenderableWidget(Button.builder(sustainMessage(), button -> {
                this.sustain = !this.sustain;
                markDirty();
            }).bounds(fieldX + (smallWidth + 4) * 2, 42, smallWidth, 20).build());
            this.weightField = this.addRenderableWidget(new EditBox(this.font, fieldX + (smallWidth + 4) * 3, 42, smallWidth, 20, Component.translatable("screen.music_and_melody.event_editor.weight")));
            this.weightField.setMaxLength(8);
            this.weightField.setResponder(value -> markDirty());

            this.musicField = this.addRenderableWidget(new EditBox(this.font, fieldX, 78, fieldWidth, 20, Component.translatable("screen.music_and_melody.event_editor.music")));
            this.musicField.setMaxLength(256);
            this.musicField.setResponder(value -> markDirty());
            this.conditionsField = this.addRenderableWidget(new MultiLineEditBox(
                    this.font,
                    fieldX,
                    CONDITIONS_Y,
                    fieldWidth,
                    CONDITIONS_ONE_LINE_HEIGHT,
                    Component.literal("eg. biome=minecraft:forest, time=night, event=menu").withStyle(ChatFormatting.DARK_GRAY),
                    CommonComponents.EMPTY
            ));
            this.conditionsField.setValueListener(value -> markDirty());
            this.conditionsField.setValueListener(value -> markDirty());
        }

        int listY = this.listExpanded ? 32 : conditionsListY(CONDITIONS_ONE_LINE_HEIGHT);
        int listHeight = this.listExpanded ? Math.max(34, this.height - 92) : Math.max(34, this.height - listY - BOTTOM_BUTTON_AREA_HEIGHT);
        this.list = this.addRenderableWidget(new EventList(this, this.minecraft, this.width, listHeight, listY));

        int rowWidth = Math.min(AlbumScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
        int rowX = this.width / 2 - rowWidth / 2;
        int topY = this.height - 51;
        int bottomY = this.height - 27;
        int topWidth = (rowWidth - 8) / 3;
        int bottomWidth = (rowWidth - 8) / 3;

        this.addButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.add"), button -> addEntry())
                .bounds(rowX, topY, topWidth, 20)
                .build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.save"), button -> saveEntry())
                .bounds(rowX + topWidth + 4, topY, topWidth, 20)
                .build());
        this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.remove"), button -> removeSelected())
                .bounds(rowX + (topWidth + 4) * 2, topY, topWidth, 20)
                .build());

        this.browseButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.browse"), button -> browseSources())
                .bounds(rowX, bottomY, bottomWidth, 20)
                .build());
        this.expandButton = this.addRenderableWidget(Button.builder(expandMessage(), button -> toggleExpanded())
                .bounds(rowX + bottomWidth + 4, bottomY, bottomWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + (bottomWidth + 4) * 2, bottomY, bottomWidth, 20)
                .build());

        if (this.listExpanded) {
            refreshEditorState();
        } else if (this.selectedIndex >= 0 && this.selectedIndex < this.entries.size()) {
            select(this.selectedIndex);
        } else {
            clearEditor();
        }
        MusicScreenHelper.addSocialButtons(this);
        refreshEditorState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);
        graphics.drawCenteredString(this.font, title(), this.width / 2, 15, 0xFFFFFFFF);
        if (this.listExpanded) {
            refreshEditorState();
            return;
        }
        int fieldX = this.musicField.getX();
        graphics.drawString(this.font, Component.translatable("screen.music_and_melody.event_editor.type"), fieldX, 30, 0xFFAAAAAA);
        graphics.drawString(this.font, Component.translatable("screen.music_and_melody.event_editor.priority"), this.priorityButton.getX(), 30, 0xFFAAAAAA);
        graphics.drawString(this.font, Component.translatable("screen.music_and_melody.event_editor.sustain"), this.sustainButton.getX(), 30, 0xFFAAAAAA);
        graphics.drawString(this.font, Component.translatable("screen.music_and_melody.event_editor.weight"), this.weightField.getX(), 30, 0xFFAAAAAA);
        graphics.drawString(this.font, Component.translatable("screen.music_and_melody.event_editor.music"), fieldX, 66, 0xFFAAAAAA);
        graphics.drawString(this.font, Component.translatable("screen.music_and_melody.event_editor.conditions"), fieldX, 102, 0xFFAAAAAA);
        refreshEditorState();
    }

    @Override
    public void onClose() {
        if (this.savedChanges) {
            EventHelper.resetMusicBreak();
        }
        if (this.closeToSources) this.minecraft.setScreen(new EventSourceScreen(this, this.sourceBrowserParent == null ? this.parent : this.sourceBrowserParent));
        else this.minecraft.setScreen(this.parent);
    }

    private void reloadEntries() {
        if (this.activeSourceId != null && activeSource() == null) {
            this.activeSourceId = null;
        }
        if (this.activeSourceId == null) {
            List<Event.Source> sources = Event.sources();
            if (!sources.isEmpty()) this.activeSourceId = sources.getFirst().id;
        }
        this.entries.clear();
        for (Event.ScreenEntry entry : Event.screenEntries()) {
            if (entry.source().id.equals(this.activeSourceId)) {
                this.entries.add(entry);
            }
        }
        if (this.selectedIndex >= this.entries.size()) this.selectedIndex = this.entries.size() - 1;
    }

    private void select(int index) {
        if (index < 0 || index >= this.entries.size()) {
            clearEditor();
            return;
        }

        this.selectedIndex = index;
        Event.Record.Entry entry = this.entries.get(index).entry();
        this.loadingEditor = true;
        this.categoryIndex = categoryIndex(entry.category());
        this.priorityIndex = priorityIndex(entry.priority());
        this.sustain = entry.sustain();
        if (this.musicField != null) this.musicField.setValue(entry.music());
        if (this.weightField != null) this.weightField.setValue(Integer.toString(entry.weight()));
        if (this.conditionsField != null) this.conditionsField.setValue(conditionsText(entry.conditions()));
        this.loadingEditor = false;
        refreshEditorState();
    }

    private void clearEditor() {
        this.selectedIndex = -1;
        this.loadingEditor = true;
        this.categoryIndex = Event.CategoryType.PLAYLIST.ordinal();
        this.priorityIndex = Event.PriorityType.LOW.ordinal();
        this.sustain = true;
        if (this.musicField != null) this.musicField.setValue("");
        if (this.weightField != null) this.weightField.setValue("1");
        if (this.conditionsField != null) this.conditionsField.setValue("");
        this.loadingEditor = false;
    }

    private void toggleExpanded() {
        this.minecraft.setScreen(new EventScreen(this.parent, this.selectedIndex, !this.listExpanded, this.savedChanges, this.activeSourceId, this.closeToSources, this.sourceBrowserParent));
    }

    private void addEntry() {
        Event.Source source = activeSource();
        if (source == null || !source.isConfig()) return;
        clearEditor();
        refreshList();
        refreshEditorState();
    }

    private void saveEntry() {
        Event.Record.Entry entry = editorEntry();
        if (entry == null) return;
        Event.ScreenEntry selected = selectedEntry();
        Event.Source source = selected == null ? activeSource() : selected.source();
        if (source == null || !source.isConfig()) return;

        List<Event.Record.Entry> sourceEntries = new ArrayList<>(source.record.entries());
        int nextIndex;
        if (selected == null) {
            sourceEntries.add(entry);
            nextIndex = sourceEntries.size() - 1;
        } else {
            sourceEntries.set(selected.index(), entry);
            nextIndex = this.selectedIndex;
        }

        if (saveSourceEntries(source, sourceEntries)) {
            ResourceLocation sourceId = source.id;
            reloadEntries();
            if (!sourceId.equals(this.activeSourceId)) this.activeSourceId = sourceId;
            select(Math.min(nextIndex, this.entries.size() - 1));
            refreshList();
        }
    }

    private void removeSelected() {
        Event.ScreenEntry selected = selectedEntry();
        if (selected == null || !selected.source().isConfig()) return;

        List<Event.Record.Entry> sourceEntries = new ArrayList<>(selected.source().record.entries());
        sourceEntries.remove(selected.index());
        if (saveSourceEntries(selected.source(), sourceEntries)) {
            int nextIndex = Math.min(this.selectedIndex, this.entries.size() - 2);
            reloadEntries();
            if (this.entries.isEmpty()) clearEditor();
            else select(Math.max(0, nextIndex));
            refreshList();
        }
    }

    private void browseSources() {
        this.minecraft.setScreen(new EventSourceScreen(this));
    }

    private void loadSource(ResourceLocation sourceId) {
        this.activeSourceId = sourceId;
        this.selectedIndex = -1;
        reloadEntries();
        if (this.musicField == null || this.weightField == null || this.conditionsField == null) {
            this.selectedIndex = this.entries.isEmpty() ? -1 : 0;
            return;
        }
        if (this.entries.isEmpty()) clearEditor();
        else select(0);
        refreshList();
        refreshEditorState();
    }

    private Event.Record.Entry editorEntry() {
        if (this.musicField == null || this.weightField == null || this.conditionsField == null) return null;
        String music = this.musicField.getValue().trim();
        if (ResourceLocation.tryParse(music) == null) return null;
        int weight;
        try {
            weight = Math.max(1, Integer.parseInt(this.weightField.getValue().trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
        List<Event.Record.Condition> conditions = parseConditions(this.conditionsField.getValue());
        if (conditions == null) return null;
        return new Event.Record.Entry(
                Event.categoryName(CATEGORIES[this.categoryIndex]),
                music,
                conditions,
                Event.priorityName(PRIORITIES[this.priorityIndex]),
                this.sustain,
                weight
        );
    }

    private void markDirty() {
        if (this.loadingEditor) return;
        refreshEditorState();
    }

    private void refreshList() {
        if (this.list != null) this.list.refresh();
    }

    private void refreshEditorState() {
        Event.ScreenEntry selected = selectedEntry();
        boolean configSelected = selected != null && selected.source().isConfig();
        if (this.listExpanded) {
            if (this.addButton != null) this.addButton.active = false;
            if (this.saveButton != null) this.saveButton.active = false;
            if (this.removeButton != null) this.removeButton.active = configSelected;
            if (this.expandButton != null) this.expandButton.setMessage(expandMessage());
            return;
        }
        if (this.categoryButton != null) this.categoryButton.setMessage(categoryMessage());
        if (this.priorityButton != null) this.priorityButton.setMessage(priorityMessage());
        if (this.sustainButton != null) this.sustainButton.setMessage(sustainMessage());
        if (this.musicField != null) this.musicField.setHint(Component.literal("eg. " + musicExample()).withStyle(ChatFormatting.DARK_GRAY));
        updateConditionsFieldLayout();

        Event.Record.Entry draft = editorEntry();
        Event.Source source = selectedSource();
        boolean editable = source != null && source.isConfig();
        if (this.categoryButton != null) this.categoryButton.active = editable;
        if (this.priorityButton != null) this.priorityButton.active = editable;
        if (this.weightField != null) this.weightField.active = editable;
        if (this.sustainButton != null) this.sustainButton.active = editable;
        if (this.musicField != null) this.musicField.active = editable;
        if (this.conditionsField != null) this.conditionsField.active = editable;
        if (this.addButton != null) this.addButton.active = editable;
        if (this.saveButton != null) {
            boolean changed = selected == null || draft != null && !draft.equals(selected.entry());
            this.saveButton.active = editable && draft != null && changed;
        }
        if (this.removeButton != null) this.removeButton.active = configSelected;
        if (this.expandButton != null) this.expandButton.setMessage(expandMessage());
    }

    private void updateConditionsFieldLayout() {
        if (this.conditionsField == null || this.list == null) return;
        int height = conditionsFieldHeight();
        if (this.conditionsField.getHeight() != height) this.conditionsField.setHeight(height);
        int listY = conditionsListY(height);
        this.list.setY(listY);
        this.list.setHeight(Math.max(34, this.height - listY - BOTTOM_BUTTON_AREA_HEIGHT));
    }

    private int conditionsFieldHeight() {
        if (this.conditionsField == null) return CONDITIONS_ONE_LINE_HEIGHT;
        String value = this.conditionsField.getValue();
        if (value.isBlank()) return CONDITIONS_ONE_LINE_HEIGHT;
        int maxWidth = Math.max(1, this.conditionsField.getWidth() - 8);
        int lines = 0;
        for (String line : value.split("\n", -1)) {
            lines += Math.max(1, this.font.split(Component.literal(line), maxWidth).size());
            if (lines > 1) return CONDITIONS_TWO_LINE_HEIGHT;
        }
        return CONDITIONS_ONE_LINE_HEIGHT;
    }

    private static int conditionsListY(int conditionsHeight) {
        return CONDITIONS_Y + conditionsHeight + CONDITIONS_LIST_GAP;
    }

    private boolean saveSourceEntries(Event.Source source, List<Event.Record.Entry> entries) {
        if (!Event.saveSourceEntries(source, entries)) return false;
        this.savedChanges = true;
        return true;
    }

    private void markSavedChanges() {
        this.savedChanges = true;
    }

    private Component title() {
        Event.Source source = activeSource();
        return source == null ? this.title : source.record.name();
    }

    private Event.ScreenEntry selectedEntry() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.entries.size() ? this.entries.get(this.selectedIndex) : null;
    }

    private Event.Source selectedSource() {
        Event.ScreenEntry selected = selectedEntry();
        return selected == null ? activeSource() : selected.source();
    }

    private Event.Source activeSource() {
        if (this.activeSourceId == null) return null;
        for (Event.Source source : Event.sources()) {
            if (source.id.equals(this.activeSourceId)) return source;
        }
        return null;
    }

    private Component categoryMessage() {
        return Component.translatable("screen.music_and_melody.event_editor.category." + Event.categoryName(CATEGORIES[this.categoryIndex]));
    }

    private Component priorityMessage() {
        return Component.translatable("screen.music_and_melody.event_editor.priority." + Event.priorityName(PRIORITIES[this.priorityIndex]));
    }

    private Component sustainMessage() {
        return Component.translatable("screen.music_and_melody.event_editor.sustain")
                .append(": ")
                .append(CommonComponents.optionStatus(this.sustain));
    }

    private Component expandMessage() {
        return Component.translatable(this.listExpanded ? "button.music_and_melody.edit" : "button.music_and_melody.expand");
    }

    private String musicExample() {
        return switch (CATEGORIES[this.categoryIndex]) {
            case ALBUM -> "minecraft:volume_alpha";
            case PLAYLIST -> "config:playlists/example";
            case POOL -> "minecraft:music.overworld.forest";
            case TRACK -> "music_and_melody:music/overworld/alpha";
            case DISC -> "minecraft:cat";
        };
    }

    private static int categoryIndex(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (Event.categoryName(CATEGORIES[i]).equals(category.toLowerCase(Locale.ROOT))) return i;
        }
        return Event.CategoryType.PLAYLIST.ordinal();
    }

    private static int priorityIndex(String priority) {
        for (int i = 0; i < PRIORITIES.length; i++) {
            if (Event.priorityName(PRIORITIES[i]).equals(priority.toLowerCase(Locale.ROOT))) return i;
        }
        return Event.PriorityType.LOW.ordinal();
    }

    private static List<Event.Record.Condition> parseConditions(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return new ArrayList<>();
        if (trimmed.startsWith("[")) return Event.parseRecordConditions(trimmed).orElse(null);
        return parseConditionList(trimmed);
    }

    private static List<Event.Record.Condition> parseConditionList(String value) {
        List<Event.Record.Condition> conditions = new ArrayList<>();
        for (String rawPart : splitConditionParts(value)) {
            String part = rawPart.trim();
            if (part.isEmpty()) continue;
            Event.Record.Condition condition = parseConditionPart(part);
            if (condition == null) return null;
            conditions.add(condition);
        }
        return conditions;
    }

    private static Event.Record.Condition parseConditionPart(String part) {
        String lower = part.toLowerCase(Locale.ROOT);

        if (lower.startsWith("all_of") || lower.startsWith("any_of") || lower.startsWith("not")) {
            int separator = groupSeparator(part);
            if (separator < 0) return null;

            String type = part.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!type.equals("all_of") && !type.equals("any_of") && !type.equals("not")) return null;

            String body = part.substring(separator + 1).trim();
            if (!body.startsWith("[") || !body.endsWith("]")) return null;

            List<Event.Record.Condition> nested = parseConditionList(body.substring(1, body.length() - 1));
            return nested == null ? null : new Event.Record.Condition(type, new Event.Record.Condition.Value.Conditions(nested));
        }

        int separator = part.indexOf('=');
        String type = (separator < 0 ? part : part.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
        String conditionValue = separator < 0 ? "" : part.substring(separator + 1).trim();

        Event.Record.Condition.Value parsedValue;

        if (type.equals("above_y") || type.equals("below_y")) {
            try {
                parsedValue = new Event.Record.Condition.Value.Integer(Integer.parseInt(conditionValue));
            } catch (NumberFormatException exception) {
                return null;
            }
        } else if (type.equals("random_chance")) {
            try {
                float floatValue = Float.parseFloat(conditionValue);
                if (floatValue < 0.0F || floatValue > 1.0F) return null;
                parsedValue = new Event.Record.Condition.Value.Float(floatValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        } else if (isStringCondition(type) || ResourceLocation.tryParse(conditionValue) != null) {
            if (conditionValue.isEmpty()) return null;
            parsedValue = new Event.Record.Condition.Value.String(conditionValue);
        } else {
            return null;
        }

        return new Event.Record.Condition(type, parsedValue);
    }

    private static boolean isStringCondition(String type) {
        return type.equals("time")
                || type.equals("weather")
                || type.equals("game_mode")
                || type.equals("event")
                || type.equals("mod_loaded");
    }

    private static List<String> splitConditionParts(String value) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '[') depth++;
            else if (character == ']') depth = Math.max(0, depth - 1);
            else if (character == ',' && depth == 0) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private static int groupSeparator(String value) {
        return value.indexOf('=');
    }

    private static String conditionsText(List<Event.Record.Condition> conditions) {
        return conditions.stream().map(EventScreen::conditionText).collect(Collectors.joining(", "));
    }

    private static String conditionText(Event.Record.Condition condition) {
        if (condition.value() instanceof Event.Record.Condition.Value.Conditions(List<Event.Record.Condition> value)) {
            return condition.type().toLowerCase(Locale.ROOT) + "=[" + conditionsText(value) + "]";
        }

        return condition.type() + "=" + switch (condition.value()) {
            case Event.Record.Condition.Value.String string -> string.value();
            case Event.Record.Condition.Value.Integer integer -> Integer.toString(integer.value());
            case Event.Record.Condition.Value.Float floatValue -> Float.toString(floatValue.value());
            case Event.Record.Condition.Value.Conditions ignored -> throw new IllegalStateException("Handled above");
        };
    }

    private static class EventList extends ObjectSelectionList<EventEntry> {

        private final EventScreen screen;

        EventList(EventScreen screen, Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 34);
            this.screen = screen;
            this.centerListVertically = false;
            refresh();
        }

        private void refresh() {
            this.clearEntries();
            for (int i = 0; i < this.screen.entries.size(); i++) {
                this.addEntry(new EventEntry(this.screen, this.minecraft, i, this.screen.entries.get(i)));
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowRight() + 6;
        }
    }

    private static class EventEntry extends MusicListEntry<EventEntry> {

        private final EventScreen screen;
        private final Minecraft minecraft;
        private final int index;
        private final Event.ScreenEntry row;

        EventEntry(EventScreen screen, Minecraft minecraft, int index, Event.ScreenEntry row) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.index = index;
            this.row = row;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.row.entry().music());
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int color = this.index == this.screen.selectedIndex ? 0xFFFFFF55 : this.row.source().isEnabled() ? 0xFFFFFFFF : 0xFF888888;
            Event.Record.Entry entry = this.row.entry();
            String first = entry.category() + " | " + entry.music() + " | " + entry.priority() + " | sustain=" + entry.sustain() + " | " + entry.weight();
            String second = conditionsText(entry.conditions());
            int maxWidth = this.getContentWidth() - 2;
            graphics.drawString(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(first, maxWidth), this.getContentX() + 1, this.getContentY() + 5, color);
            graphics.drawString(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(second, maxWidth), this.getContentX() + 1, this.getContentY() + 17, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.screen.select(this.index);
            return true;
        }
    }

    public static class EventSourceScreen extends Screen {

        private static final Component TITLE = Component.translatable("screen.music_and_melody.events");
        private final EventScreen editor;
        private final Screen parent;
        private final Set<ResourceLocation> deletePendingSources = new HashSet<>();
        private SourceList list;

        public EventSourceScreen(Screen parent) {
            this(new EventScreen(parent), parent);
        }

        public EventSourceScreen(EventScreen parent) {
            this(parent, parent);
        }

        public EventSourceScreen(EventScreen editor, Screen parent) {
            super(TITLE);
            this.editor = editor;
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.list = this.addRenderableWidget(new SourceList(this, this.minecraft, this.width, this.height - 64));
            int rowWidth = Math.min(AlbumScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
            int buttonWidth = (rowWidth - 8) / 3;
            int rowX = this.width / 2 - rowWidth / 2;
            int buttonY = this.height - 27;
            this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.filter"), button -> {
                        this.minecraft.setScreen(new EventFilterScreen(this));
                    })
                    .bounds(rowX, buttonY, buttonWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.new"), button -> newSource())
                    .bounds(rowX + buttonWidth + 4, buttonY, buttonWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                    .bounds(rowX + (buttonWidth + 4) * 2, buttonY, buttonWidth, 20)
                    .build());
            MusicScreenHelper.addSocialButtons(this);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
            super.render(graphics, mouseX, mouseY, tickDelta);
            graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        }

        @Override
        public void onClose() {
            boolean deletedActiveSource = false;
            if (!this.deletePendingSources.isEmpty()) {
                for (Event.Source source : Event.sources()) {
                    if (!this.deletePendingSources.contains(source.id)) continue;
                    deletedActiveSource |= source.id.equals(this.editor.activeSourceId);
                    source.deleteConfig();
                }
                this.deletePendingSources.clear();
                if (deletedActiveSource) {
                    this.editor.loadSource(null);
                } else {
                    this.editor.reloadEntries();
                    this.editor.refreshList();
                }
            }
            if (this.editor.savedChanges) {
                EventHelper.resetMusicBreak();
            }
            this.minecraft.setScreen(this.parent);
        }

        private void choose(ResourceLocation sourceId) {
            this.editor.loadSource(sourceId);
            this.editor.closeToSources = true;
            this.editor.sourceBrowserParent = this.parent;
            this.minecraft.setScreen(this.editor);
        }

        private void newSource() {
            this.minecraft.setScreen(new NewEventSourceScreen(this));
        }

        void refreshList() {
            if (this.list != null) this.list.refresh();
        }

        private boolean isDeletePending(Event.Source source) {
            return this.deletePendingSources.contains(source.id);
        }

        private void toggleDeletePending(Event.Source source) {
            if (!source.isConfig()) return;
            if (!this.deletePendingSources.remove(source.id)) {
                this.deletePendingSources.add(source.id);
            }
            this.editor.markSavedChanges();
            refreshList();
        }

        private boolean visible(Event.Source source) {
            MaMDataConfig.Events filter = MaMDataConfig.get().events;

            boolean shouldShow = (filter.show_custom && source.isConfig()) || (filter.show_built_in && !source.isConfig());

            if (filter.visibility == MaMDataConfig.EventVisibility.ENABLED) {
                return source.isEnabled() && shouldShow;
            }
            if (filter.visibility == MaMDataConfig.EventVisibility.DISABLED) {
                return !source.isEnabled() && shouldShow;
            }

            return shouldShow;
        }
    }

    private static class SourceList extends ObjectSelectionList<SourceEntry> {

        private final EventSourceScreen screen;

        SourceList(EventSourceScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 32, 38);
            this.screen = screen;
            this.centerListVertically = false;
            refresh();
        }

        private void refresh() {
            this.clearEntries();
            for (Event.Source source : Event.sources()) {
                if (this.screen.visible(source)) {
                    this.addEntry(new SourceEntry(this.screen, this.minecraft, source));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(520, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowRight() + 6;
        }
    }

    private static class SourceEntry extends MusicListEntry<SourceEntry> {

        private static final int BUTTON_WIDTH = 64;
        private static final int BUTTON_GAP = 4;
        private final EventSourceScreen screen;
        private final Minecraft minecraft;
        private final Event.Source source;
        private final Button loadButton;
        private final Button toggleButton;
        private final IconButton deleteButton;

        SourceEntry(EventSourceScreen screen, Minecraft minecraft, Event.Source source) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.source = source;
            this.loadButton = Button.builder(loadMessage(), button -> this.screen.choose(this.source.id))
                    .size(BUTTON_WIDTH, 20)
                    .build();
            this.toggleButton = Button.builder(toggleMessage(), button -> toggleSource())
                    .size(BUTTON_WIDTH, 20)
                    .build();
            this.deleteButton = source.isConfig() ? new IconButton(deleteMessage(), deleteIcon(), button -> deleteSource()) : null;
        }

        @Override
        public Component getNarration() {
            return this.source.record.name();
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX() + 1;
            int buttonsWidth = BUTTON_WIDTH * 2 + IconButton.SIZE + BUTTON_GAP * 2;
            int maxWidth = this.getContentWidth() - buttonsWidth - 12;
            int color = this.screen.isDeletePending(this.source) ? 0xFFFF8888 : this.source.isEnabled() ? 0xFFFFFFFF : 0xFF888888;
            FormattedCharSequence name = this.minecraft.font.split(this.source.record.name(), maxWidth).getFirst();
            graphics.drawString(this.minecraft.font, name, x, this.getContentYMiddle() - this.minecraft.font.lineHeight - 1, color);
            graphics.drawString(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(this.source.id.toString(), maxWidth), x, this.getContentYMiddle() + 2, 0xFFAAAAAA);
            if (hovered && hasDescription() && mouseX < this.getContentRight() - buttonsWidth) {
                graphics.renderTooltip(this.minecraft.font, this.minecraft.font.split(this.source.record.description(), 240), mouseX, mouseY);
            }
            renderButtons(graphics, mouseX, mouseY, tickDelta);
        }

        private boolean hasDescription() {
            return !this.source.record.description().getString().isBlank();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.loadButton.mouseClicked(mouseX, mouseY, button)
                    || this.toggleButton.mouseClicked(mouseX, mouseY, button)
                    || this.deleteButton != null && this.deleteButton.mouseClicked(mouseX, mouseY, button)
                    || super.mouseClicked(mouseX, mouseY, button);
        }

        private void renderButtons(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
            int buttonX = this.getContentRight() - (BUTTON_WIDTH * 2 + IconButton.SIZE + BUTTON_GAP * 2);
            int buttonY = this.getContentYMiddle() - 10;
            this.loadButton.setX(buttonX);
            this.loadButton.setY(buttonY);
            this.loadButton.render(graphics, mouseX, mouseY, tickDelta);

            this.toggleButton.setMessage(toggleMessage());
            this.toggleButton.setX(buttonX + BUTTON_WIDTH + BUTTON_GAP);
            this.toggleButton.setY(buttonY);
            this.toggleButton.render(graphics, mouseX, mouseY, tickDelta);

            int thirdX = buttonX + (BUTTON_WIDTH + BUTTON_GAP) * 2;
            if (this.deleteButton != null) {
                this.deleteButton.setIconAndTooltip(deleteIcon(), deleteMessage());
                this.deleteButton.setX(thirdX);
                this.deleteButton.setY(buttonY);
                this.deleteButton.render(graphics, mouseX, mouseY, tickDelta);
            } else if (this.source != null) {
                IconButton.renderIconWithTooltip(graphics, IconButton.icon("built_in"), thirdX, buttonY, Component.translatable("screen.music_and_melody.events.built_in"), mouseX, mouseY);
            }
        }

        private Component loadMessage() {
            if (this.source.isConfig()) return Component.translatable("button.music_and_melody.edit");
            return Component.translatable("button.music_and_melody.view");
        }

        private Component toggleMessage() {
            return Component.translatable(this.source.isEnabled() ? "button.music_and_melody.disable" : "button.music_and_melody.enable");
        }

        private Component deleteMessage() {
            return Component.translatable(this.screen.isDeletePending(this.source) ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
        }

        private ResourceLocation deleteIcon() {
            return IconButton.icon(this.screen.isDeletePending(this.source) ? "restore" : "delete");
        }

        private void toggleSource() {
            this.source.setEnabled(!this.source.isEnabled());
            this.screen.editor.markSavedChanges();
            this.screen.editor.reloadEntries();
            this.screen.editor.refreshList();
            this.screen.refreshList();
        }

        private void deleteSource() {
            this.screen.toggleDeletePending(this.source);
        }
    }

    private static class NewEventSourceScreen extends Screen {

        private static final Component TITLE = Component.translatable("screen.music_and_melody.create_event");
        private final EventSourceScreen parent;
        private EditBox nameField;
        private EditBox descriptionField;
        private EditBox pathField;
        private Button createButton;

        NewEventSourceScreen(EventSourceScreen parent) {
            super(TITLE);
            this.parent = parent;
        }

        @Override
        protected void init() {
            int fieldWidth = Math.min(300, this.width - 40);
            int fieldX = this.width / 2 - fieldWidth / 2;
            this.nameField = this.addRenderableWidget(new EditBox(this.font, fieldX, 62, fieldWidth, 20, Component.translatable("screen.music_and_melody.create_event.name")));
            this.nameField.setMaxLength(80);
            this.nameField.setResponder(value -> {
                updatePathHint();
                refreshCreateState();
            });
            this.descriptionField = this.addRenderableWidget(new EditBox(this.font, fieldX, 104, fieldWidth, 20, Component.translatable("screen.music_and_melody.create_event.description")));
            this.descriptionField.setMaxLength(256);
            this.pathField = this.addRenderableWidget(new EditBox(this.font, fieldX, 146, fieldWidth, 20, Component.translatable("screen.music_and_melody.create_event.path")));
            this.pathField.setMaxLength(256);
            this.pathField.setResponder(value -> refreshCreateState());
            updatePathHint();

            int buttonY = this.height - 27;
            int rowWidth = Math.min(AlbumScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
            int rowX = this.width / 2 - rowWidth / 2;
            int buttonWidth = (rowWidth - 4) / 2;
            this.createButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.create"), button -> create())
                    .bounds(rowX, buttonY, buttonWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                    .bounds(rowX + buttonWidth + 4, buttonY, buttonWidth, 20)
                    .build());
            MusicScreenHelper.addSocialButtons(this);
            this.setInitialFocus(this.nameField);
            refreshCreateState();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
            super.render(graphics, mouseX, mouseY, tickDelta);
            graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
            int fieldX = this.nameField.getX();
            graphics.drawString(this.font, Component.translatable("screen.music_and_melody.create_event.name"), fieldX, 50, 0xFFAAAAAA);
            graphics.drawString(this.font, Component.translatable("screen.music_and_melody.create_event.description"), fieldX, 92, 0xFFAAAAAA);
            graphics.drawString(this.font, Component.translatable("screen.music_and_melody.create_event.path"), fieldX, 134, 0xFFAAAAAA);
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }

        private void create() {
            Event.Source source = Event.createConfigSource(this.nameField.getValue(), this.descriptionField.getValue(), this.pathField.getValue());
            if (source == null) return;
            this.parent.editor.markSavedChanges();
            this.parent.editor.loadSource(source.id);
            this.parent.editor.closeToSources = true;
            this.parent.editor.sourceBrowserParent = this.parent.parent;
            this.minecraft.setScreen(this.parent.editor);
        }

        private void refreshCreateState() {
            if (this.createButton == null) return;
            this.createButton.active = Event.canCreateConfigSource(this.nameField.getValue(), this.pathField.getValue());
        }

        private void updatePathHint() {
            if (this.pathField == null) return;
            String preview = Event.previewConfigSourcePath(this.nameField.getValue());
            this.pathField.setHint(preview.isEmpty() ? Component.empty() : Component.literal(preview).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
