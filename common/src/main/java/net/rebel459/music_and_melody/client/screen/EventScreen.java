package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.rebel459.music_and_melody.client.Event;
import net.rebel459.music_and_melody.client.element.ExampleHintEditBox;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

public class EventScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.event_editor");
    private static final Event.CategoryType[] CATEGORIES = Event.CategoryType.values();
    private static final Event.PriorityType[] PRIORITIES = Event.PriorityType.values();
    private static final int CONDITIONS_Y = 114;
    private static final int CONDITIONS_ONE_LINE_HEIGHT = 18;
    private static final int CONDITIONS_TWO_LINE_HEIGHT = 28;
    private static final int CONDITIONS_THREE_LINE_HEIGHT = 38;
    private static final int CONDITIONS_LIST_GAP = 12;
    private static final int BOTTOM_BUTTON_AREA_HEIGHT = 60;
    private static final int OUTER_MARGIN = 10;
    private static final int PANEL_GAP = 7;
    private static final int PANEL_TOP = OUTER_MARGIN;
    private static final int PANEL_BOTTOM_MARGIN = 10;
    private static final int REFERENCE_WORKSPACE_WIDTH = 620;
    private static final int MIN_LEFT_WIDTH = 112;
    private static final int MIN_MIDDLE_WIDTH = 180;
    private static final int MIN_RIGHT_WIDTH = 124;
    /** Matches the shared workspace footer baseline while fitting one single-line and one two-line field. */
    private static final int EDITOR_BOTTOM_HEIGHT = 56;








    private final Screen parent;
    private final List<Event.ScreenEntry> entries = new ArrayList<>();
    private EventList list;
    private EventDescriptionList descriptionList;
    private EditBox musicField;
    private EditBox weightField;
    private MultiLineEditBox conditionsField;
    private Button categoryButton;
    private Button priorityButton;
    private Button sustainButton;
    private Button constantButton;
    private Button addButton;
    private Button saveButton;
    private Button removeButton;
    private int selectedIndex = -1;
    private int categoryIndex = Event.CategoryType.PLAYLIST.ordinal();
    private int priorityIndex = Event.PriorityType.LOW.ordinal();
    private boolean sustain = true;
    private boolean constant = false;
    private boolean loadingEditor;
    private boolean savedChanges;
    private boolean openSourcesOnInit;
    private boolean closeToSources;
    private Screen sourceBrowserParent;
    private Identifier activeSourceId;
    private int layoutWidth;
    private int layoutHeight;

    public EventScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        reloadEntries();
    }

    public EventScreen(Screen parent, boolean openSourcesOnInit) {
        this(parent);
        this.openSourcesOnInit = openSourcesOnInit;
    }

    /** Opens the existing editor directly on one event source. */
    public EventScreen(Screen parent, Identifier sourceId) {
        this(parent);
        this.activeSourceId = sourceId;
        reloadEntries();
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        if (this.openSourcesOnInit) {
            this.openSourcesOnInit = false;
            this.minecraft.gui.setScreen(new EventBrowserScreen(this, this.parent));
            return;
        }

        EditorLayout layout = editorLayout();
        this.addRenderableOnly(this::renderEditorShell);

        int settingsX = layout.leftX + 8;
        int settingsWidth = layout.leftWidth - 16;
        int controlY = PANEL_TOP + 42;
        int controlStep = settingsStep(layout);
        this.categoryButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY, settingsWidth, 20, categoryMessage(), false, button -> {
                this.categoryIndex = (this.categoryIndex + 1) % CATEGORIES.length;
                markDirty();
        }));
        this.priorityButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep, settingsWidth, 20, priorityMessage(), false, button -> {
                this.priorityIndex = (this.priorityIndex + 1) % PRIORITIES.length;
                markDirty();
        }));
        this.sustainButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep * 2, settingsWidth, 20, sustainMessage(), false, button -> {
                this.sustain = !this.sustain;
                markDirty();
        }));
        this.constantButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep * 3, settingsWidth, 20, constantMessage(), false, button -> {
                this.constant = !this.constant;
                markDirty();
        }));
        this.weightField = this.addRenderableWidget(new EditBox(this.font, settingsX, controlY + controlStep * 4, settingsWidth, 20, Component.translatable("screen.music_and_melody.event_editor.weight")));
        this.weightField.setMaxLength(8);
        this.weightField.setResponder(value -> markDirty());

        int bottomX = layout.leftX + 8;
        int labelWidth = this.font.width(Component.translatable("screen.music_and_melody.event_editor.conditions")) + 8;
        int fieldX = bottomX + labelWidth;
        int fieldWidth = Math.max(48, layout.bottomRight() - fieldX - 8);
        int musicY = layout.bottomPanelTop + 2;
        int conditionsY = layout.bottomPanelTop + 26;
        this.musicField = this.addRenderableWidget(new ExampleHintEditBox(this.font, fieldX, musicY, fieldWidth, 20,
                Component.translatable("screen.music_and_melody.event_editor.music")));
        this.musicField.setMaxLength(256);
        this.musicField.setResponder(value -> markDirty());
        this.conditionsField = this.addRenderableWidget(MultiLineEditBox.builder()
                .setX(fieldX)
                .setY(conditionsY)
                .setPlaceholder(Component.literal("eg. biome=minecraft:forest, time=night, event=menu").withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))))
                .build(this.font, fieldWidth, CONDITIONS_TWO_LINE_HEIGHT, Component.translatable("screen.music_and_melody.event_editor.conditions")));
        this.conditionsField.setValueListener(value -> markDirty());

        int descriptionTop = PANEL_TOP + 53;
        int listBottom = layout.bottomPanelTop - 6;
        // Two compact description lines are enough context; entries remain
        // the dominant, immediately reachable part of the middle panel.
        int descriptionBottom = Math.max(descriptionTop + 1,
                Math.min(descriptionTop + (this.font.lineHeight + 2) * 2, listBottom - 46 - 6));
        this.descriptionList = this.addRenderableWidget(new EventDescriptionList(this, this.minecraft, layout.middleX, layout.middleWidth,
                descriptionTop, descriptionBottom));
        int listTop = descriptionBottom + 6;
        this.list = this.addRenderableWidget(new EventList(this, this.minecraft, layout.middleX, layout.middleWidth, listTop, listBottom));

        int actionX = layout.rightX + 7;
        int actionWidth = layout.rightWidth - 14;
        // Match the Event Browser's Filter by Tags section exactly: heading
        // at +14 and its first control at +38.
        int actionY = PANEL_TOP + 38;

        this.addButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY, actionWidth, 20,
                Component.translatable("button.music_and_melody.add"), false, button -> addEntry()));
        this.removeButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY + 25, actionWidth, 20,
                Component.translatable("button.music_and_melody.remove"), false, button -> removeSelected()));
        this.saveButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY + 50, actionWidth, 20,
                Component.translatable("button.music_and_melody.save"), false, button -> saveEntry()));
        this.addRenderableWidget(new WorkspaceButton(actionX, layout.panelBottom - 28, actionWidth, 20,
                CommonComponents.GUI_DONE, false, button -> this.onClose()));

        if (this.selectedIndex >= 0 && this.selectedIndex < this.entries.size()) {
            select(this.selectedIndex);
        } else {
            clearEditor();
        }
        refreshEditorState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        IconButton.setTooltipScale(MaMDataConfig.get().gui_multiplier);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(MaMDataConfig.get().gui_multiplier);
            super.extractRenderState(graphics, toLayoutMouse(mouseX), toLayoutMouse(mouseY), tickDelta);
        } finally {
            graphics.pose().popMatrix();
            IconButton.resetTooltipScale();
        }
        refreshEditorState();
    }

    private void calculateLayoutSize() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    @Override
    protected void repositionElements() {
        calculateLayoutSize();
        this.rebuildWidgets();
    }

    private int toLayoutMouse(double mouse) {
        return Math.round((float) (mouse / MaMDataConfig.get().gui_multiplier));
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(toLayoutMouse(event), doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(toLayoutMouse(event), dragX / MaMDataConfig.get().gui_multiplier, dragY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(toLayoutMouse(event));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier, scrollX, scrollY);
    }

    private EditorLayout editorLayout() {
        int panelBottom = this.layoutHeight - PANEL_BOTTOM_MARGIN;
        int bottomPanelTop = panelBottom - EDITOR_BOTTOM_HEIGHT;
        int workspaceWidth = Math.max(3, this.layoutWidth - OUTER_MARGIN * 2);
        int workspaceX = (this.layoutWidth - workspaceWidth) / 2;
        int usableWidth = Math.max(3, workspaceWidth - PANEL_GAP * 2);
        int preferredMinimum = MIN_LEFT_WIDTH + MIN_MIDDLE_WIDTH + MIN_RIGHT_WIDTH;
        int leftWidth;
        int middleWidth;
        int rightWidth;
        if (usableWidth < preferredMinimum) {
            leftWidth = Math.max(1, Math.round(usableWidth * (MIN_LEFT_WIDTH / (float) preferredMinimum)));
            rightWidth = Math.max(1, Math.round(usableWidth * (MIN_RIGHT_WIDTH / (float) preferredMinimum)));
            middleWidth = Math.max(1, usableWidth - leftWidth - rightWidth);
        } else if (workspaceWidth <= REFERENCE_WORKSPACE_WIDTH) {
            int viewportEquivalentWidth = workspaceWidth + OUTER_MARGIN * 2;
            leftWidth = Math.max(132, Math.min(210, (int) (viewportEquivalentWidth * 0.23F)));
            rightWidth = Math.max(144, Math.min(214, (int) (viewportEquivalentWidth * 0.20F)));
            middleWidth = usableWidth - leftWidth - rightWidth;
            if (middleWidth < MIN_MIDDLE_WIDTH) {
                int shortfall = MIN_MIDDLE_WIDTH - middleWidth;
                int fromLeft = Math.min(shortfall / 2, Math.max(0, leftWidth - MIN_LEFT_WIDTH));
                int fromRight = Math.min(shortfall - fromLeft, Math.max(0, rightWidth - MIN_RIGHT_WIDTH));
                leftWidth -= fromLeft;
                rightWidth -= fromRight;
                middleWidth = usableWidth - leftWidth - rightWidth;
            }
        } else {
            leftWidth = Math.round(workspaceWidth * (147.0F / REFERENCE_WORKSPACE_WIDTH));
            rightWidth = Math.round(workspaceWidth * (144.0F / REFERENCE_WORKSPACE_WIDTH));
            middleWidth = usableWidth - leftWidth - rightWidth;
        }
        int leftX = workspaceX;
        int middleX = leftX + leftWidth + PANEL_GAP;
        int rightX = middleX + middleWidth + PANEL_GAP;
        return new EditorLayout(leftX, leftWidth, middleX, middleWidth, rightX, rightWidth, panelBottom, bottomPanelTop);
    }

    private void renderEditorShell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        EditorLayout layout = editorLayout();
        graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, SCREEN_BACKGROUND);
        drawPanel(graphics, layout.leftX, PANEL_TOP, layout.leftWidth, layout.bottomPanelTop - PANEL_GAP - PANEL_TOP);
        drawPanel(graphics, layout.middleX, PANEL_TOP, layout.middleWidth, layout.bottomPanelTop - PANEL_GAP - PANEL_TOP);
        drawPanel(graphics, layout.leftX, layout.bottomPanelTop, layout.bottomRight() - layout.leftX, layout.panelBottom - layout.bottomPanelTop);
        // The footer belongs to Settings + Entries only.  Actions stays a
        // full-height right panel so its Done control remains inside it.
        drawPanel(graphics, layout.rightX, PANEL_TOP, layout.rightWidth, layout.panelBottom - PANEL_TOP);

        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.settings").withStyle(ChatFormatting.BOLD), layout.leftX + 8, PANEL_TOP + 11, TEXT_HEADER);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.entries").withStyle(ChatFormatting.BOLD), layout.middleX + 8, PANEL_TOP + 11, TEXT_HEADER);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.actions").withStyle(ChatFormatting.BOLD), layout.rightX + 8, PANEL_TOP + 14, TEXT_HEADER);

        Event.Source source = activeSource();
        int titleX = layout.middleX + 8;
        if (source != null) {
            int iconSize = 18;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, source.icon()), titleX, PANEL_TOP + 31,
                    0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            titleX += iconSize + 6;
        }
        int titleWidth = Math.max(1, layout.middleWidth - (titleX - layout.middleX) - 8);
        drawMarquee(graphics, title(), titleX, PANEL_TOP + 35, titleWidth, TEXT_TITLE);

        int controlLabelY = PANEL_TOP + 30;
        int controlStep = settingsStep(layout);
        int settingsX = layout.leftX + 8;
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.type"), settingsX, controlLabelY, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.priority"), settingsX, controlLabelY + controlStep, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.sustain"), settingsX, controlLabelY + controlStep * 2, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.constant"), settingsX, controlLabelY + controlStep * 3, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.weight"), settingsX, controlLabelY + controlStep * 4, TEXT_DESCRIPTION);

        int bottomX = layout.leftX + 8;
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.music"), bottomX, layout.bottomPanelTop + 7, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.conditions"), bottomX, layout.bottomPanelTop + 31, TEXT_DESCRIPTION);
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, PANEL_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_OUTLINE);
    }

    private void drawMarquee(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        int textWidth = this.font.width(text);
        if (textWidth <= width) {
            graphics.text(this.font, text, x, y, color);
            return;
        }
        int travel = textWidth - width;
        long pause = 850L;
        long move = Math.max(1L, travel * 42L);
        long cycle = pause * 2L + move * 2L;
        long elapsed = Math.floorMod(Util.getMillis() + Math.abs(text.getString().hashCode()), cycle);
        float fraction;
        if (elapsed < pause) fraction = 0.0F;
        else if (elapsed < pause + move) fraction = (elapsed - pause) / (float) move;
        else if (elapsed < pause + move + pause) fraction = 1.0F;
        else fraction = 1.0F - (elapsed - pause - move - pause) / (float) move;
        int offset = Math.round(travel * Math.max(0.0F, Math.min(1.0F, fraction)));
        graphics.enableScissor(x, y, x + width, y + this.font.lineHeight);
        graphics.text(this.font, text, x - offset, y, color);
        graphics.disableScissor();
    }

    private static int settingsStep(EditorLayout layout) {
        int available = layout.bottomPanelTop - PANEL_GAP - (PANEL_TOP + 42) - 20;
        return Math.max(24, Math.min(42, available / 4));
    }

    private record EditorLayout(int leftX, int leftWidth, int middleX, int middleWidth, int rightX, int rightWidth, int panelBottom, int bottomPanelTop) {
        int bottomRight() {
            return this.middleX + this.middleWidth;
        }
    }

    @Override
    public void onClose() {
        if (this.savedChanges) {
            EventHelper.resetMusicBreak();
        }
        if (this.closeToSources) this.minecraft.gui.setScreen(new EventBrowserScreen(this, this.sourceBrowserParent == null ? this.parent : this.sourceBrowserParent));
        else this.minecraft.gui.setScreen(this.parent);
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
        this.constant = entry.constant();
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
        this.constant = false;
        if (this.musicField != null) this.musicField.setValue("");
        if (this.weightField != null) this.weightField.setValue("1");
        if (this.conditionsField != null) this.conditionsField.setValue("");
        this.loadingEditor = false;
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
        this.minecraft.gui.setScreen(new EventBrowserScreen(this, this.parent));
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
        refreshDescription();
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
                this.constant,
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

    private void refreshDescription() {
        if (this.descriptionList != null) this.descriptionList.refresh();
    }

    private void refreshEditorState() {
        Event.ScreenEntry selected = selectedEntry();
        boolean configSelected = selected != null && selected.source().isConfig();
        if (this.categoryButton != null) this.categoryButton.setMessage(categoryMessage());
        if (this.priorityButton != null) this.priorityButton.setMessage(priorityMessage());
        if (this.sustainButton != null) this.sustainButton.setMessage(sustainMessage());
        if (this.constantButton != null) this.constantButton.setMessage(constantMessage());
        if (this.musicField != null) this.musicField.setHint(Component.literal("eg. " + musicExample()).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));

        Event.Record.Entry draft = editorEntry();
        Event.Source source = selectedSource();
        boolean editable = source != null && source.isConfig();
        if (this.categoryButton != null) this.categoryButton.active = editable;
        if (this.priorityButton != null) this.priorityButton.active = editable;
        if (this.weightField != null) this.weightField.active = editable;
        if (this.sustainButton != null) this.sustainButton.active = editable;
        if (this.constantButton != null) this.constantButton.active = editable;
        if (this.musicField != null) this.musicField.active = editable;
        if (this.conditionsField != null) this.conditionsField.active = editable;
        if (this.addButton != null) this.addButton.active = editable;
        if (this.saveButton != null) {
            boolean changed = selected == null || draft != null && !draft.equals(selected.entry());
            this.saveButton.active = editable && draft != null && changed;
        }
        if (this.removeButton != null) this.removeButton.active = configSelected;
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

    private Component constantMessage() {
        return Component.translatable("screen.music_and_melody.event_editor.constant")
                .append(": ")
                .append(CommonComponents.optionStatus(this.constant));
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

        if (type.equals("below_y")) {
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
        } else if (isStringCondition(type) || Identifier.tryParse(conditionValue) != null) {
            if (conditionValue.isEmpty()) return null;
            parsedValue = new Event.Record.Condition.Value.String(conditionValue);
        } else {
            return null;
        }

        return new Event.Record.Condition(type, parsedValue);
    }

    private static boolean isStringCondition(String type) {
        return type.equals("time") || type.equals("weather") || type.equals("game_mode") || type.equals("special") || type.equals("mod_loaded") || type.equals("bossbar") || type.equals("below_version");
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

    /** A compact, independently scrollable source description above the entries. */
    private static class EventDescriptionList extends ObjectSelectionList<DescriptionEntry> {
        private final EventScreen screen;
        private final int panelX;
        private final int panelWidth;

        EventDescriptionList(EventScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(minecraft, panelWidth, Math.max(1, bottom - top), top, minecraft.font.lineHeight + 2);
            this.screen = screen;
            this.panelX = panelX;
            this.panelWidth = panelWidth;
            this.setX(panelX);
            this.centerListVertically = false;
            refresh();
        }

        void refresh() {
            this.clearEntries();
            Event.Source source = this.screen.activeSource();
            if (source == null || source.record.description().getString().isBlank()) return;
            int width = Math.max(1, this.panelWidth - 18);
            for (FormattedCharSequence line : this.screen.font.split(source.record.description(), width)) {
                this.addEntry(new DescriptionEntry(line));
            }
        }

        @Override
        public int getRowLeft() {
            return this.panelX + 7;
        }

        @Override
        public int getRowWidth() {
            return Math.max(24, this.panelWidth - 18);
        }

        @Override
        protected int scrollBarX() {
            return this.panelX + this.panelWidth - 6;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {}

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

        @Override
        protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            if (!this.scrollable()) return;
            int x = scrollBarX();
            int top = this.getY() + 1;
            int bottom = this.getY() + this.getHeight() - 1;
            graphics.fill(x, top, x + 3, bottom, BUTTON_PASSIVE);
            int thumbTop = Math.max(top, this.scrollBarY());
            int thumbBottom = Math.min(bottom, thumbTop + this.scrollerHeight());
            graphics.fill(x, thumbTop, x + 3, thumbBottom,
                    mouseX >= x - 2 && mouseX <= x + 5 && mouseY >= thumbTop && mouseY <= thumbBottom ? PANEL_HIGHLIGHT : SCROLLBAR_THUMB);
        }
    }

    private static class DescriptionEntry extends ObjectSelectionList.Entry<DescriptionEntry> {
        private final FormattedCharSequence line;

        DescriptionEntry(FormattedCharSequence line) {
            this.line = line;
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            graphics.text(Minecraft.getInstance().font, this.line, this.getContentX(),
                    this.getContentYMiddle() - Minecraft.getInstance().font.lineHeight / 2, TEXT_DESCRIPTION);
        }
    }

    private static class EventList extends ObjectSelectionList<EventEntry> {

        private final EventScreen screen;
        private final int panelX;
        private final int panelWidth;

        EventList(EventScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(minecraft, panelWidth, Math.max(1, bottom - top), top, 46);
            this.screen = screen;
            this.panelX = panelX;
            this.panelWidth = panelWidth;
            this.setX(panelX);
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
            return Math.max(24, this.panelWidth - 14);
        }

        @Override
        protected int scrollBarX() {
            return this.panelX + this.panelWidth - 8;
        }

        @Override
        protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            if (!this.scrollable()) return;
            int x = scrollBarX();
            int top = this.getY() + 2;
            int bottom = this.getY() + this.getHeight() - 2;
            graphics.fill(x, top, x + 4, bottom, BUTTON_PASSIVE);
            int thumbTop = Math.max(top, this.scrollBarY());
            int thumbBottom = Math.min(bottom, thumbTop + this.scrollerHeight());
            int color = mouseX >= x - 2 && mouseX <= x + 6 && mouseY >= thumbTop && mouseY <= thumbBottom ? PANEL_HIGHLIGHT : SCROLLBAR_THUMB;
            graphics.fill(x, thumbTop, x + 4, thumbBottom, color);
        }

        @Override
        public int getRowLeft() {
            return this.panelX + 5;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
            // The editor shell already supplies a panel-local background.
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
            // Do not draw the stock full-width list separators inside a panel.
        }
    }

    private static class EventEntry extends ObjectSelectionList.Entry<EventEntry> {

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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (this.index == this.screen.selectedIndex) {
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            } else if (hovered) {
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            }
            int color = this.index == this.screen.selectedIndex ? TEXT_SELECTED : this.row.source().isEnabled() ? TEXT_PRIMARY : TEXT_DISABLED;
            Event.Record.Entry entry = this.row.entry();
            String first = entry.music() + " (" + entry.category() + ")";
            String second = "priority=" + entry.priority() + " | sustain=" + entry.sustain() + " | constant=" + entry.constant() + " | weight=" + entry.weight();
            String third = conditionsText(entry.conditions());
            int maxWidth = this.getContentWidth() - 2;
            this.screen.drawMarquee(graphics, Component.literal(first), this.getContentX() + 1, this.getContentY() + 5, maxWidth, color);
            this.screen.drawMarquee(graphics, Component.literal(second), this.getContentX() + 1, this.getContentY() + 17, maxWidth, TEXT_DESCRIPTION);
            this.screen.drawMarquee(graphics, Component.literal(third), this.getContentX() + 1, this.getContentY() + 29, maxWidth, TEXT_DESCRIPTION);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            AbstractWidget.playButtonClickSound(this.screen.minecraft.getSoundManager());
            this.screen.select(this.index);
            return true;
        }
    }

    public static class EventBrowserScreen extends Screen {

        private static final Component TITLE = Component.translatable("screen.music_and_melody.events");
        private final EventScreen editor;
        private final Screen parent;
        private final Set<Identifier> deletePendingSources = new HashSet<>();
        private SourceList list;
        private String search = "";
        private Button sortButton;

        public EventBrowserScreen(Screen parent) {
            this(new EventScreen(parent), parent);
        }

        public EventBrowserScreen(EventScreen parent) {
            this(parent, parent.parent);
        }

        public EventBrowserScreen(EventScreen editor, Screen parent) {
            super(TITLE);
            this.editor = editor;
            this.parent = parent;
        }

        @Override
        protected void init() {
            int rowWidth = Math.min(ContentBrowserScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
            int rowX = this.width / 2 - rowWidth / 2;
            int topY = 31;
            int halfWidth = (rowWidth - 4) / 2;
            MaMDataConfig.Events events = MaMDataConfig.get().events;

            addCheckbox("screen.music_and_melody.event.custom", rowX, topY, halfWidth, () -> events.show_custom, value -> events.show_custom = value);
            addCheckbox("screen.music_and_melody.event.built_in", rowX + halfWidth + 4, topY, halfWidth, () -> events.show_built_in, value -> events.show_built_in = value);

            this.list = this.addRenderableWidget(new SourceList(this, this.minecraft, this.width, this.height - 112));

            int searchY = this.height - 51;
            int buttonY = this.height - 27;
            EditBox searchField = this.addRenderableWidget(new EditBox(this.font, rowX, searchY, rowWidth, 20, Component.translatable("screen.music_and_melody.search")));
            searchField.setValue(this.search);
            searchField.setResponder(value -> {
                this.search = value;
                refreshList();
            });

            int buttonWidth = (rowWidth - 8) / 3;
            this.sortButton = this.addRenderableWidget(Button.builder(sortMessage(), button -> {
                        events.enabled_first = !events.enabled_first;
                        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
                        this.sortButton.setMessage(sortMessage());
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
            MusicScreenHelper.addSocialButtons(this);
        }

        private Component sortMessage() {
            return Component.translatable(MaMDataConfig.get().events.enabled_first ? "button.music_and_melody.enabled_first" : "button.music_and_melody.enabled_last");
        }

        private void addCheckbox(String key, int x, int y, int width, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
            Checkbox checkbox = Checkbox.builder(Component.translatable(key), this.font)
                    .pos(x, y)
                    .selected(getter.get())
                    .maxWidth(width)
                    .onValueChange((changedCheckbox, selected) -> {
                        setter.accept(selected);
                        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
                        refreshList();
                    })
                    .build();
            checkbox.setX(x + (width - checkbox.getWidth()) / 2);
            this.addRenderableWidget(checkbox);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            graphics.centeredText(this.font, this.title, this.width / 2, 15, TEXT_TITLE);
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
            this.minecraft.gui.setScreen(this.parent);
        }

        private void choose(Identifier sourceId) {
            this.editor.loadSource(sourceId);
            this.editor.closeToSources = true;
            this.editor.sourceBrowserParent = this.parent;
            this.minecraft.gui.setScreen(this.editor);
        }

        private void newSource() {
            this.minecraft.gui.setScreen(new NewEventScreen(this));
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
            if (!shouldShow) return false;
            String query = this.search.trim().toLowerCase(Locale.ROOT);
            return query.isEmpty()
                    || source.record.name().getString().toLowerCase(Locale.ROOT).contains(query)
                    || source.id.toString().toLowerCase(Locale.ROOT).contains(query)
                    || source.record.description().getString().toLowerCase(Locale.ROOT).contains(query);
        }
    }

    private static class SourceList extends ObjectSelectionList<SourceEntry> {

        private final EventBrowserScreen screen;

        SourceList(EventBrowserScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 56, 38);
            this.screen = screen;
            this.centerListVertically = false;
            refresh();
        }

        private void refresh() {
            this.clearEntries();
            Comparator<Event.Source> comparator = Comparator
                    .comparing(Event.Source::isEnabled);
            if (MaMDataConfig.get().events.enabled_first) comparator = comparator.reversed();
            comparator = comparator.thenComparing(source -> source.record.name().getString(), String.CASE_INSENSITIVE_ORDER);

            Event.sources().stream()
                    .filter(this.screen::visible)
                    .sorted(comparator)
                    .map(source -> new SourceEntry(this.screen, this.minecraft, source))
                    .forEach(this::addEntry);
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
        private final EventBrowserScreen screen;
        private final Minecraft minecraft;
        private final Event.Source source;
        private final Button loadButton;
        private final Button toggleButton;
        private final IconButton deleteButton;

        SourceEntry(EventBrowserScreen screen, Minecraft minecraft, Event.Source source) {
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentX() + 1;
            int buttonsWidth = BUTTON_WIDTH * 2 + IconButton.SIZE + BUTTON_GAP * 2;
            int maxWidth = this.getContentWidth() - buttonsWidth - 12;
            int color = this.screen.isDeletePending(this.source) ? TEXT_PENDING_DELETION : this.source.isEnabled() ? TEXT_TITLE : TEXT_DISABLED;
            FormattedCharSequence name = this.minecraft.font.split(this.source.record.name(), maxWidth).getFirst();
            graphics.text(this.minecraft.font, name, x, this.getContentYMiddle() - this.minecraft.font.lineHeight - 1, color);
            graphics.text(this.minecraft.font, this.minecraft.font.plainSubstrByWidth(this.source.id.toString(), maxWidth), x, this.getContentYMiddle() + 2, TEXT_DESCRIPTION);
            if (hovered && hasDescription() && mouseX < this.getContentRight() - buttonsWidth) {
                graphics.setTooltipForNextFrame(this.minecraft.font, this.minecraft.font.split(this.source.record.description(), 240), mouseX, mouseY);
            }
            renderButtons(graphics, mouseX, mouseY, tickDelta);
        }

        private boolean hasDescription() {
            return !this.source.record.description().getString().isBlank();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.loadButton.mouseClicked(event, doubleClick)
                    || this.toggleButton.mouseClicked(event, doubleClick)
                    || this.deleteButton != null && this.deleteButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }

        private void renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            int buttonX = this.getContentRight() - (BUTTON_WIDTH * 2 + IconButton.SIZE + BUTTON_GAP * 2);
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
                this.deleteButton.setIconAndTooltip(deleteIcon(), deleteMessage());
                this.deleteButton.setX(thirdX);
                this.deleteButton.setY(buttonY);
                this.deleteButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
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

        private Identifier deleteIcon() {
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

    private static class NewEventScreen extends Screen {

        private static final Component TITLE = Component.translatable("screen.music_and_melody.create_event");
        private final EventBrowserScreen parent;
        private EditBox nameField;
        private EditBox descriptionField;
        private EditBox iconField;
        private EditBox pathField;
        private Button createButton;

        NewEventScreen(EventBrowserScreen parent) {
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
            this.iconField = this.addRenderableWidget(new EditBox(this.font, fieldX, 146, fieldWidth, 20, Component.translatable("screen.music_and_melody.event_editor.icon")));
            this.iconField.setMaxLength(256);
            this.iconField.setHint(Component.literal(Event.DEFAULT_ICON.toString()).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
            this.iconField.setResponder(value -> refreshCreateState());
            this.pathField = this.addRenderableWidget(new EditBox(this.font, fieldX, 188, fieldWidth, 20, Component.translatable("screen.music_and_melody.create_event.path")));
            this.pathField.setMaxLength(256);
            this.pathField.setResponder(value -> refreshCreateState());
            updatePathHint();

            int buttonY = this.height - 27;
            int rowWidth = Math.min(ContentBrowserScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
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
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            graphics.centeredText(this.font, this.title, this.width / 2, 15, TEXT_TITLE);
            int fieldX = this.nameField.getX();
            graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.name"), fieldX, 50, TEXT_DESCRIPTION);
            graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.description"), fieldX, 92, TEXT_DESCRIPTION);
            graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.icon"), fieldX, 134, TEXT_DESCRIPTION);
            graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.path"), fieldX, 176, TEXT_DESCRIPTION);
        }

        @Override
        public void onClose() {
            this.minecraft.gui.setScreen(this.parent);
        }

        private void create() {
            Event.Source source = Event.createConfigSource(this.nameField.getValue(), this.descriptionField.getValue(), this.iconField.getValue(), this.pathField.getValue());
            if (source == null) return;
            this.parent.editor.markSavedChanges();
            this.parent.editor.loadSource(source.id);
            this.parent.editor.closeToSources = true;
            this.parent.editor.sourceBrowserParent = this.parent.parent;
            this.minecraft.gui.setScreen(this.parent.editor);
        }

        private void refreshCreateState() {
            if (this.createButton == null) return;
            String icon = this.iconField == null ? "" : this.iconField.getValue().trim();
            this.createButton.active = Event.canCreateConfigSource(this.nameField.getValue(), this.pathField.getValue())
                    && (icon.isEmpty() || Identifier.tryParse(icon) != null);
        }

        private void updatePathHint() {
            if (this.pathField == null) return;
            String preview = Event.previewConfigSourcePath(this.nameField.getValue());
            this.pathField.setHint(preview.isEmpty() ? Component.empty() : Component.literal(preview).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        }
    }
}
