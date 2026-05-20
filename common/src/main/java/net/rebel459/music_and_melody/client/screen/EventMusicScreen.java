package net.rebel459.music_and_melody.client.screen;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Event;
import net.rebel459.music_and_melody.client.EventHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventMusicScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.event_editor");
    private static final Event.CategoryType[] CATEGORIES = Event.CategoryType.values();
    private static final Event.PriorityType[] PRIORITIES = Event.PriorityType.values();
    private static final int CONDITIONS_Y = 114;
    private static final int CONDITIONS_ONE_LINE_HEIGHT = 18;
    private static final int CONDITIONS_TWO_LINE_HEIGHT = 28;
    private static final int CONDITIONS_LIST_GAP = 12;
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
    private Identifier activeSourceId;

    public EventMusicScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        reloadEntries();
    }

    public EventMusicScreen(Screen parent, boolean openSourcesOnInit) {
        this(parent);
        this.openSourcesOnInit = openSourcesOnInit;
    }

    private EventMusicScreen(Screen parent, int selectedIndex, boolean listExpanded, boolean savedChanges, Identifier activeSourceId) {
        super(TITLE);
        this.parent = parent;
        this.selectedIndex = selectedIndex;
        this.listExpanded = listExpanded;
        this.savedChanges = savedChanges;
        this.activeSourceId = activeSourceId;
        reloadEntries();
    }

    private EventMusicScreen(Screen parent, int selectedIndex, boolean listExpanded, boolean savedChanges, Identifier activeSourceId, boolean closeToSources, Screen sourceBrowserParent) {
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
            this.conditionsField = this.addRenderableWidget(MultiLineEditBox.builder()
                    .setX(fieldX)
                    .setY(CONDITIONS_Y)
                    .setPlaceholder(Component.literal("eg. biome=minecraft:forest, time=night, event=menu").withStyle(ChatFormatting.DARK_GRAY))
                    .build(this.font, fieldWidth, CONDITIONS_ONE_LINE_HEIGHT, Component.translatable("screen.music_and_melody.event_editor.conditions")));
            this.conditionsField.setValueListener(value -> markDirty());
        }

        int listY = this.listExpanded ? 32 : conditionsListY(CONDITIONS_ONE_LINE_HEIGHT);
        int listHeight = this.listExpanded ? Math.max(34, this.height - 92) : Math.max(34, this.height - listY - BOTTOM_BUTTON_AREA_HEIGHT);
        this.list = this.addRenderableWidget(new EventList(this, this.minecraft, this.width, listHeight, listY));

        int rowWidth = Math.min(360, this.width - 20);
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
        refreshEditorState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, title(), this.width / 2, 15, 0xFFFFFFFF);
        if (this.listExpanded) {
            refreshEditorState();
            return;
        }
        int fieldX = this.musicField.getX();
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.type"), fieldX, 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.priority"), this.priorityButton.getX(), 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.sustain"), this.sustainButton.getX(), 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.weight"), this.weightField.getX(), 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.music"), fieldX, 66, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.conditions"), fieldX, 102, 0xFFAAAAAA);
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
        this.minecraft.setScreen(new EventMusicScreen(this.parent, this.selectedIndex, !this.listExpanded, this.savedChanges, this.activeSourceId, this.closeToSources, this.sourceBrowserParent));
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
            Identifier sourceId = source.id;
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

    private void loadSource(Identifier sourceId) {
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
        if (Identifier.tryParse(music) == null) return null;
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
            case PLAYLIST -> "music_and_melody:playlists/example";
            case POOL -> "minecraft:music.overworld.forest";
            case SONG -> "music_and_melody:music/overworld/alpha";
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
        if (lower.startsWith("all_of") || lower.startsWith("any_of")) {
            int separator = groupSeparator(part);
            if (separator < 0) return null;
            String type = part.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!type.equals("all_of") && !type.equals("any_of")) return null;
            String body = part.substring(separator + 1).trim();
            if (!body.startsWith("[") || !body.endsWith("]")) return null;
            List<Event.Record.Condition> nested = parseConditionList(body.substring(1, body.length() - 1));
            return nested == null ? null : new Event.Record.Condition(type, Optional.empty(), nested);
        }

        int separator = part.indexOf('=');
        String type = (separator < 0 ? part : part.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
        String conditionValue = separator < 0 ? "" : part.substring(separator + 1).trim();
        Optional<Either<String, Integer>> parsedValue;
        if (type.equals("above_y") || type.equals("below_y")) {
            try {
                parsedValue = Optional.of(Either.right(Integer.parseInt(conditionValue)));
            } catch (NumberFormatException exception) {
                return null;
            }
        } else if (Identifier.tryParse(conditionValue) != null || type.equals("time") || type.equals("weather") || type.equals("game_mode") || type.equals("event")) {
            if (conditionValue.isEmpty()) return null;
            parsedValue = Optional.of(Either.left(conditionValue));
        } else {
            return null;
        }
        return new Event.Record.Condition(type, parsedValue);
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
        return conditions.stream().map(EventMusicScreen::conditionText).collect(Collectors.joining(", "));
    }

    private static String conditionText(Event.Record.Condition condition) {
        if (condition.type().equalsIgnoreCase("all_of") || condition.type().equalsIgnoreCase("any_of")) {
            return condition.type().toLowerCase(Locale.ROOT) + "=[" + conditionsText(condition.conditions()) + "]";
        }
        if (condition.value().isEmpty()) return condition.type();
        Either<String, Integer> value = condition.value().get();
        return condition.type() + "=" + value.map(left -> left, right -> Integer.toString(right));
    }

    private static class EventList extends ObjectSelectionList<EventEntry> {

        private final EventMusicScreen screen;

        EventList(EventMusicScreen screen, Minecraft minecraft, int width, int height, int y) {
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
        protected int scrollBarX() {
            return this.getRowRight() + 6;
        }
    }

    private static class EventEntry extends ObjectSelectionList.Entry<EventEntry> {

        private final EventMusicScreen screen;
        private final Minecraft minecraft;
        private final int index;
        private final Event.ScreenEntry row;

        EventEntry(EventMusicScreen screen, Minecraft minecraft, int index, Event.ScreenEntry row) {
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int color = this.index == this.screen.selectedIndex ? 0xFFFFFF55 : this.row.source().isEnabled() ? 0xFFFFFFFF : 0xFF888888;
            Event.Record.Entry entry = this.row.entry();
            String first = entry.category() + " | " + entry.music() + " | " + entry.priority() + " | sustain=" + entry.sustain() + " | " + entry.weight();
            String second = conditionsText(entry.conditions());
            int maxWidth = this.getContentWidth() - 2;
            graphics.text(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(first, maxWidth), this.getContentX() + 1, this.getContentY() + 5, color);
            graphics.text(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(second, maxWidth), this.getContentX() + 1, this.getContentY() + 17, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.screen.select(this.index);
            return true;
        }
    }

    public static class EventSourceScreen extends Screen {

        private static final Component TITLE = Component.translatable("screen.music_and_melody.events");
        private final EventMusicScreen editor;
        private final Screen parent;
        private SourceList list;
        private Button filterButton;
        private SourceFilter filter = SourceFilter.ALL;

        public EventSourceScreen(Screen parent) {
            this(new EventMusicScreen(parent), parent);
        }

        public EventSourceScreen(EventMusicScreen parent) {
            this(parent, parent);
        }

        public EventSourceScreen(EventMusicScreen editor, Screen parent) {
            super(TITLE);
            this.editor = editor;
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.list = this.addRenderableWidget(new SourceList(this, this.minecraft, this.width, this.height - 64));
            int rowWidth = Math.min(360, this.width - 20);
            int buttonWidth = (rowWidth - 8) / 3;
            int rowX = this.width / 2 - rowWidth / 2;
            int buttonY = this.height - 27;
            this.filterButton = this.addRenderableWidget(Button.builder(filterMessage(), button -> {
                        cycleFilter();
                        button.setMessage(filterMessage());
                        refreshList();
                    })
                    .bounds(rowX, buttonY, buttonWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.new"), button -> newSource())
                    .bounds(rowX + buttonWidth + 4, buttonY, buttonWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                    .bounds(rowX + (buttonWidth + 4) * 2, buttonY, buttonWidth, 20)
                    .build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
            if (this.filterButton != null) this.filterButton.setMessage(filterMessage());
        }

        @Override
        public void onClose() {
            if (this.editor.savedChanges) {
                EventHelper.resetMusicBreak();
            }
            this.minecraft.setScreen(this.parent);
        }

        private void choose(Identifier sourceId) {
            this.editor.loadSource(sourceId);
            this.editor.closeToSources = true;
            this.editor.sourceBrowserParent = this.parent;
            this.minecraft.setScreen(this.editor);
        }

        private void newSource() {
            this.minecraft.setScreen(new NewEventSourceScreen(this));
        }

        private void refreshList() {
            if (this.list != null) this.list.refresh();
        }

        private void cycleFilter() {
            this.filter = switch (this.filter) {
                case ALL -> SourceFilter.CUSTOM;
                case CUSTOM -> SourceFilter.BUILT_IN;
                case BUILT_IN -> SourceFilter.ALL;
            };
        }

        private Component filterMessage() {
            return Component.translatable("button.music_and_melody.event_display." + this.filter.key);
        }

        private boolean visible(Event.Source source) {
            return switch (this.filter) {
                case ALL -> true;
                case CUSTOM -> source.isConfig();
                case BUILT_IN -> !source.isConfig();
            };
        }

        private enum SourceFilter {
            ALL("all"),
            CUSTOM("custom"),
            BUILT_IN("built_in");

            private final String key;

            SourceFilter(String key) {
                this.key = key;
            }
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
        protected int scrollBarX() {
            return this.getRowRight() + 6;
        }
    }

    private static class SourceEntry extends ObjectSelectionList.Entry<SourceEntry> {

        private static final int BUTTON_WIDTH = 64;
        private static final int BUTTON_GAP = 4;
        private final EventSourceScreen screen;
        private final Minecraft minecraft;
        private final Event.Source source;
        private final Button loadButton;
        private final Button toggleButton;
        private final Button deleteButton;

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
            this.deleteButton = source.isConfig() ? Button.builder(Component.translatable("button.music_and_melody.delete"), button -> deleteSource())
                    .size(BUTTON_WIDTH, 20)
                    .build() : null;
        }

        @Override
        public Component getNarration() {
            return this.source.record.name();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX() + 1;
            int buttonsWidth = BUTTON_WIDTH * 3 + BUTTON_GAP * 2;
            int maxWidth = this.getContentWidth() - buttonsWidth - 12;
            int color = this.source.isEnabled() ? 0xFFFFFFFF : 0xFF888888;
            FormattedCharSequence name = this.minecraft.font.split(this.source.record.name(), maxWidth).getFirst();
            graphics.text(this.minecraft.font, name, x, this.getContentYMiddle() - this.minecraft.font.lineHeight - 1, color);
            graphics.text(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(this.source.id.toString(), maxWidth), x, this.getContentYMiddle() + 2, 0xFFAAAAAA);
            renderButtons(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.loadButton.mouseClicked(event, doubleClick)
                    || this.toggleButton.mouseClicked(event, doubleClick)
                    || this.deleteButton != null && this.deleteButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }

        private void renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            int buttonX = this.getContentRight() - (BUTTON_WIDTH * 3 + BUTTON_GAP * 2);
            int buttonY = this.getContentYMiddle() - 10;
            this.loadButton.setX(buttonX);
            this.loadButton.setY(buttonY);
            this.loadButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);

            this.toggleButton.setMessage(toggleMessage());
            this.toggleButton.setX(buttonX + BUTTON_WIDTH + BUTTON_GAP);
            this.toggleButton.setY(buttonY);
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);

            int thirdX = buttonX + (BUTTON_WIDTH + BUTTON_GAP) * 2;
            if (this.deleteButton != null) {
                this.deleteButton.setX(thirdX);
                this.deleteButton.setY(buttonY);
                this.deleteButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            } else if (this.source != null) {
                Component text = Component.translatable("screen.music_and_melody.events.built_in").withStyle(ChatFormatting.GRAY);
                int labelX = thirdX + (BUTTON_WIDTH - this.minecraft.font.width(text)) / 2;
                graphics.text(this.minecraft.font, text, labelX, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, 0xFFAAAAAA);
            }
        }

        private Component loadMessage() {
            if (this.source.isConfig()) return Component.translatable("button.music_and_melody.edit");
            return Component.translatable("button.music_and_melody.view");
        }

        private Component toggleMessage() {
            return Component.translatable(this.source.isEnabled() ? "button.music_and_melody.disable" : "button.music_and_melody.enable");
        }

        private void toggleSource() {
            this.source.setEnabled(!this.source.isEnabled());
            this.screen.editor.markSavedChanges();
            this.screen.editor.reloadEntries();
            this.screen.editor.refreshList();
            this.screen.refreshList();
        }

        private void deleteSource() {
            Identifier deleted = this.source.id;
            if (!this.source.deleteConfig()) return;
            this.screen.editor.markSavedChanges();
            if (deleted.equals(this.screen.editor.activeSourceId)) {
                this.screen.editor.loadSource(null);
            } else {
                this.screen.editor.reloadEntries();
                this.screen.editor.refreshList();
            }
            this.screen.refreshList();
        }
    }

    private static class NewEventSourceScreen extends Screen {

        private static final Component TITLE = Component.translatable("screen.music_and_melody.create_event");
        private final EventSourceScreen parent;
        private EditBox nameField;
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
            this.pathField = this.addRenderableWidget(new EditBox(this.font, fieldX, 104, fieldWidth, 20, Component.translatable("screen.music_and_melody.create_event.path")));
            this.pathField.setMaxLength(256);
            this.pathField.setResponder(value -> refreshCreateState());
            updatePathHint();

            int buttonY = this.height - 27;
            int rowX = this.width / 2 - 154;
            this.createButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.create"), button -> create())
                    .bounds(rowX, buttonY, 152, 20)
                    .build());
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                    .bounds(rowX + 156, buttonY, 152, 20)
                    .build());
            this.setInitialFocus(this.nameField);
            refreshCreateState();
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
            int fieldX = this.nameField.getX();
            graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.name"), fieldX, 50, 0xFFAAAAAA);
            graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.path"), fieldX, 92, 0xFFAAAAAA);
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }

        private void create() {
            Event.Source source = Event.createConfigSource(this.nameField.getValue(), this.pathField.getValue());
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
