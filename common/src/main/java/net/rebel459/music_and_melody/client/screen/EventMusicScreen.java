package net.rebel459.music_and_melody.client.screen;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.EventMusic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

class EventMusicScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.events");
    private static final EventMusic.CategoryType[] CATEGORIES = EventMusic.CategoryType.values();
    private static final EventMusic.PriorityType[] PRIORITIES = EventMusic.PriorityType.values();

    private final PlaylistScreen parent;
    private final List<EventMusic.Record.Entry> entries = new ArrayList<>();
    private EventList list;
    private EditBox musicField;
    private EditBox weightField;
    private EditBox conditionsField;
    private Button categoryButton;
    private Button priorityButton;
    private Button addButton;
    private Button updateButton;
    private Button removeButton;
    private Button expandButton;
    private int selectedIndex = -1;
    private int categoryIndex = EventMusic.CategoryType.PLAYLIST.ordinal();
    private int priorityIndex = EventMusic.PriorityType.LOW.ordinal();
    private boolean listExpanded;
    private boolean loadingEditor;
    private boolean savedChanges;

    EventMusicScreen(PlaylistScreen parent) {
        super(TITLE);
        this.parent = parent;
        this.entries.addAll(EventMusic.readConfigEntries());
    }

    private EventMusicScreen(PlaylistScreen parent, List<EventMusic.Record.Entry> entries, int selectedIndex, boolean listExpanded, boolean savedChanges) {
        super(TITLE);
        this.parent = parent;
        this.entries.addAll(entries);
        this.selectedIndex = selectedIndex;
        this.listExpanded = listExpanded;
        this.savedChanges = savedChanges;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(420, this.width - 20);
        int fieldX = this.width / 2 - fieldWidth / 2;
        int smallWidth = (fieldWidth - 8) / 3;

        if (!this.listExpanded) {
            this.categoryButton = this.addRenderableWidget(Button.builder(categoryMessage(), button -> {
                this.categoryIndex = (this.categoryIndex + 1) % CATEGORIES.length;
                markDirty();
            }).bounds(fieldX, 42, smallWidth, 20).build());
            this.priorityButton = this.addRenderableWidget(Button.builder(priorityMessage(), button -> {
                this.priorityIndex = (this.priorityIndex + 1) % PRIORITIES.length;
                markDirty();
            }).bounds(fieldX + smallWidth + 4, 42, smallWidth, 20).build());
            this.weightField = this.addRenderableWidget(new EditBox(this.font, fieldX + (smallWidth + 4) * 2, 42, smallWidth, 20, Component.translatable("screen.music_and_melody.events.weight")));
            this.weightField.setMaxLength(8);
            this.weightField.setResponder(value -> markDirty());

            this.musicField = this.addRenderableWidget(new EditBox(this.font, fieldX, 78, fieldWidth, 20, Component.translatable("screen.music_and_melody.events.music")));
            this.musicField.setMaxLength(256);
            this.musicField.setResponder(value -> markDirty());
            this.conditionsField = this.addRenderableWidget(new EditBox(this.font, fieldX, 114, fieldWidth, 20, Component.translatable("screen.music_and_melody.events.conditions")));
            this.conditionsField.setMaxLength(512);
            this.conditionsField.setResponder(value -> markDirty());
            this.conditionsField.setHint(Component.literal("eg. biome=minecraft:forest, time=night, game_mode=survival").withStyle(ChatFormatting.DARK_GRAY));
        }

        int listY = this.listExpanded ? 32 : 146;
        int listHeight = this.listExpanded ? Math.max(34, this.height - 68) : Math.max(34, this.height - 182);
        this.list = this.addRenderableWidget(new EventList(this, this.minecraft, this.width, listHeight, listY));

        int buttonY = this.height - 27;
        int rowWidth = Math.min(520, this.width - 20);
        int buttonWidth = (rowWidth - 16) / 5;
        int rowX = this.width / 2 - rowWidth / 2;
        this.addButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.add"), button -> addEntry())
                .bounds(rowX, buttonY, buttonWidth, 20)
                .build());
        this.updateButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.update"), button -> updateSelected())
                .bounds(rowX + buttonWidth + 4, buttonY, buttonWidth, 20)
                .build());
        this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.remove"), button -> removeSelected())
                .bounds(rowX + (buttonWidth + 4) * 2, buttonY, buttonWidth, 20)
                .build());
        this.expandButton = this.addRenderableWidget(Button.builder(expandMessage(), button -> toggleExpanded())
                .bounds(rowX + (buttonWidth + 4) * 3, buttonY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + (buttonWidth + 4) * 4, buttonY, buttonWidth, 20)
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
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        if (this.listExpanded) {
            refreshEditorState();
            return;
        }
        int fieldX = this.musicField.getX();
        graphics.text(this.font, Component.translatable("screen.music_and_melody.events.type"), fieldX, 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.events.priority"), this.priorityButton.getX(), 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.events.weight"), this.weightField.getX(), 30, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.events.music"), fieldX, 66, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.events.conditions"), fieldX, 102, 0xFFAAAAAA);
        refreshEditorState();
    }

    @Override
    public void onClose() {
        if (this.savedChanges) {
            EventMusic.resetMusicBreak();
        }
        this.minecraft.setScreen(this.parent);
    }

    private void select(int index) {
        if (index < 0 || index >= this.entries.size()) {
            clearEditor();
            return;
        }

        this.selectedIndex = index;
        EventMusic.Record.Entry entry = this.entries.get(index);
        this.loadingEditor = true;
        this.categoryIndex = categoryIndex(entry.category());
        this.priorityIndex = priorityIndex(entry.priority());
        this.musicField.setValue(entry.music());
        this.weightField.setValue(Integer.toString(entry.weight()));
        this.conditionsField.setValue(conditionsText(entry.conditions()));
        this.loadingEditor = false;
        refreshEditorState();
    }

    private void clearEditor() {
        this.selectedIndex = -1;
        this.loadingEditor = true;
        this.categoryIndex = EventMusic.CategoryType.PLAYLIST.ordinal();
        this.priorityIndex = EventMusic.PriorityType.LOW.ordinal();
        if (this.musicField != null) this.musicField.setValue("");
        if (this.weightField != null) this.weightField.setValue("1");
        if (this.conditionsField != null) this.conditionsField.setValue("");
        this.loadingEditor = false;
    }

    private void toggleExpanded() {
        int selected = this.selectedIndex;
        this.minecraft.setScreen(new EventMusicScreen(this.parent, this.entries, selected, !this.listExpanded, this.savedChanges));
    }

    private void addEntry() {
        EventMusic.Record.Entry entry = editorEntry();
        if (entry == null) return;
        this.entries.add(entry);
        if (saveEntries()) {
            refreshList();
            select(this.entries.size() - 1);
        }
    }

    private void updateSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.entries.size()) return;
        EventMusic.Record.Entry entry = editorEntry();
        if (entry == null) return;
        this.entries.set(this.selectedIndex, entry);
        if (saveEntries()) {
            refreshList();
            refreshEditorState();
        }
    }

    private void removeSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.entries.size()) return;
        this.entries.remove(this.selectedIndex);
        int nextIndex = this.selectedIndex;
        if (saveEntries()) {
            refreshList();
            if (this.entries.isEmpty()) clearEditor();
            else select(Math.min(nextIndex, this.entries.size() - 1));
        }
    }

    private EventMusic.Record.Entry editorEntry() {
        String music = this.musicField.getValue().trim();
        if (Identifier.tryParse(music) == null) return null;
        int weight;
        try {
            weight = Math.max(1, Integer.parseInt(this.weightField.getValue().trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
        List<EventMusic.Record.Condition> conditions = parseConditions(this.conditionsField.getValue());
        if (conditions == null) return null;
        return new EventMusic.Record.Entry(
                EventMusic.categoryName(CATEGORIES[this.categoryIndex]),
                music,
                conditions,
                EventMusic.priorityName(PRIORITIES[this.priorityIndex]),
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
        boolean selected = this.selectedIndex >= 0 && this.selectedIndex < this.entries.size();
        if (this.listExpanded) {
            if (this.addButton != null) this.addButton.active = false;
            if (this.updateButton != null) this.updateButton.active = false;
            if (this.removeButton != null) this.removeButton.active = selected;
            if (this.expandButton != null) this.expandButton.setMessage(expandMessage());
            return;
        }
        if (this.categoryButton != null) {
            this.categoryButton.setMessage(categoryMessage());
        }
        if (this.priorityButton != null) {
            this.priorityButton.setMessage(priorityMessage());
        }
        if (this.musicField != null) {
            this.musicField.setHint(Component.literal("eg. " + musicExample()).withStyle(ChatFormatting.DARK_GRAY));
        }
        EventMusic.Record.Entry draft = editorEntry();
        if (this.addButton != null) this.addButton.active = draft != null;
        if (this.updateButton != null) this.updateButton.active = selected && draft != null && !draft.equals(this.entries.get(this.selectedIndex));
        if (this.removeButton != null) this.removeButton.active = selected;
        if (this.expandButton != null) this.expandButton.setMessage(expandMessage());
    }

    private boolean saveEntries() {
        if (!EventMusic.saveConfigEntries(this.entries)) return false;
        this.savedChanges = true;
        return true;
    }

    private Component categoryMessage() {
        return Component.translatable("screen.music_and_melody.events.category." + EventMusic.categoryName(CATEGORIES[this.categoryIndex]));
    }

    private Component priorityMessage() {
        return Component.translatable("screen.music_and_melody.events.priority." + EventMusic.priorityName(PRIORITIES[this.priorityIndex]));
    }

    private Component expandMessage() {
        return Component.translatable(this.listExpanded ? "button.music_and_melody.edit" : "button.music_and_melody.expand");
    }

    private String musicExample() {
        return switch (CATEGORIES[this.categoryIndex]) {
            case ALBUM -> "minecraft:volume_alpha";
            case PLAYLIST -> "music_and_melody:playlists/example";
            case SONG -> "music_and_melody:music/overworld/alpha";
            case DISC -> "minecraft:cat";
        };
    }

    private static int categoryIndex(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (EventMusic.categoryName(CATEGORIES[i]).equals(category.toLowerCase(Locale.ROOT))) return i;
        }
        return EventMusic.CategoryType.PLAYLIST.ordinal();
    }

    private static int priorityIndex(String priority) {
        for (int i = 0; i < PRIORITIES.length; i++) {
            if (EventMusic.priorityName(PRIORITIES[i]).equals(priority.toLowerCase(Locale.ROOT))) return i;
        }
        return EventMusic.PriorityType.LOW.ordinal();
    }

    private static List<EventMusic.Record.Condition> parseConditions(String value) {
        List<EventMusic.Record.Condition> conditions = new ArrayList<>();
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return conditions;
        for (String rawPart : trimmed.split(",")) {
            String part = rawPart.trim();
            if (part.isEmpty()) continue;
            int separator = part.indexOf('=');
            String type = (separator < 0 ? part : part.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
            String conditionValue = separator < 0 ? "" : part.substring(separator + 1).trim();
            Optional<Either<String, Integer>> parsedValue;
            if (type.equals("menu")) {
                parsedValue = Optional.empty();
            } else if (type.equals("above_y") || type.equals("below_y")) {
                try {
                    parsedValue = Optional.of(Either.right(Integer.parseInt(conditionValue)));
                } catch (NumberFormatException exception) {
                    return null;
                }
            } else if (Identifier.tryParse(conditionValue) != null || type.equals("time") || type.equals("weather") || type.equals("game_mode")) {
                if (conditionValue.isEmpty()) return null;
                parsedValue = Optional.of(Either.left(conditionValue));
            } else {
                return null;
            }
            conditions.add(new EventMusic.Record.Condition(type, parsedValue));
        }
        return conditions;
    }

    private static String conditionsText(List<EventMusic.Record.Condition> conditions) {
        return conditions.stream().map(condition -> {
            if (condition.value().isEmpty()) return condition.type();
            Either<String, Integer> value = condition.value().get();
            return condition.type() + "=" + value.map(left -> left, right -> Integer.toString(right));
        }).collect(Collectors.joining(", "));
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
        private final EventMusic.Record.Entry entry;

        EventEntry(EventMusicScreen screen, Minecraft minecraft, int index, EventMusic.Record.Entry entry) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.index = index;
            this.entry = entry;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.entry.music());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int color = this.index == this.screen.selectedIndex ? 0xFFFFFF55 : 0xFFFFFFFF;
            String first = this.entry.category() + " | " + this.entry.music() + " | " + this.entry.priority() + " | " + this.entry.weight();
            String second = conditionsText(this.entry.conditions());
            int maxWidth = this.getContentWidth() - 2;
            graphics.text(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(first, maxWidth), this.getContentX() + 1, this.getContentY() + 5, color);
            graphics.text(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(second.isEmpty() ? "-" : second, maxWidth), this.getContentX() + 1, this.getContentY() + 17, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.screen.select(this.index);
            return true;
        }
    }
}
