package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
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
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private static final int EDITOR_BOTTOM_HEIGHT = 56;
    private static final int SETTINGS_STEP = 42;

    private final Screen parent;
    private final List<Event.ScreenEntry> entries = new ArrayList<>();
    private EventList list;
    private EventDescriptionList descriptionList;
    private EditBox musicField;
    private EditBox weightField;
    private MultiLineEditBox conditionsField;
    private Button categoryButton;
    private Button priorityButton;
    private Button replaceButton;
    private Button sustainButton;
    private Button constantButton;
    private Button addButton;
    private Button saveButton;
    private Button removeButton;
    private IconButton deleteButton;
    private IconButton playPauseButton;
    private int selectedIndex = -1;
    private int categoryIndex = Event.CategoryType.POOL.ordinal();
    private int priorityIndex = Event.PriorityType.LOW.ordinal();
    private boolean replace = false;
    private boolean sustain = true;
    private boolean constant = false;
    private boolean loadingEditor;
    private boolean savedChanges;
    private boolean deletePending;
    private EditorSnapshot soundPoolsSnapshot;
    private Identifier activeSourceId;
    private int layoutWidth;
    private int layoutHeight;
    private double settingsScroll;
    private double settingsScrollMax;

    public EventScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        reloadEntries();
    }

    public EventScreen(Screen parent, Identifier sourceId) {
        this(parent);
        this.activeSourceId = sourceId;
        if (parent instanceof MusicPlayerScreen musicPlayer) this.deletePending = musicPlayer.isEventDeletePending(sourceId);
        reloadEntries();
    }

    @Override
    protected void init() {
        if (!MaMClientConfig.get().allow_events) {
            this.minecraft.gui.setScreen(this.parent);
            return;
        }
        calculateLayoutSize();
        EditorLayout layout = editorLayout();
        updateSettingsScroll(layout);
        this.addRenderableOnly(this::renderEditorShell);

        int settingsX = layout.leftX + 8;
        int settingsWidth = layout.leftWidth - 16;
        int controlY = settingsControlY();
        int controlStep = SETTINGS_STEP;
        this.addRenderableOnly((graphics, mouseX, mouseY, tickDelta) ->
                graphics.enableScissor(layout.leftX + 1, settingsViewportTop(),
                        layout.leftX + layout.leftWidth - 1, settingsViewportBottom(layout)));
        this.categoryButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY, settingsWidth, 20, categoryMessage(), false, button -> {
                this.categoryIndex = (this.categoryIndex + 1) % CATEGORIES.length;
                markDirty();
        }));
        this.priorityButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep, settingsWidth, 20, priorityMessage(), false, button -> {
                this.priorityIndex = (this.priorityIndex + 1) % PRIORITIES.length;
                markDirty();
        }));
        this.replaceButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep * 2, settingsWidth, 20, replaceMessage(), false, button -> {
                this.replace = !this.replace;
                markDirty();
        }));
        this.sustainButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep * 3, settingsWidth, 20, sustainMessage(), false, button -> {
                this.sustain = !this.sustain;
                markDirty();
        }));
        this.constantButton = this.addRenderableWidget(new WorkspaceButton(settingsX, controlY + controlStep * 4, settingsWidth, 20, constantMessage(), false, button -> {
                this.constant = !this.constant;
                markDirty();
        }));
        this.weightField = this.addRenderableWidget(new EditBox(this.font, settingsX, controlY + controlStep * 5, settingsWidth, 20, Component.translatable("screen.music_and_melody.event_editor.weight")));
        this.weightField.setMaxLength(8);
        this.weightField.setResponder(value -> markDirty());
        this.addRenderableOnly((graphics, mouseX, mouseY, tickDelta) -> graphics.disableScissor());

        int fieldX = layout.middleX + 8;
        int fieldWidth = Math.max(48, layout.middleWidth - 16);
        int musicY = PANEL_TOP + 38;
        int conditionsY = PANEL_TOP + 76;
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

        int listBottom = layout.bottomPanelTop - 6;
        int listTop = conditionsY + CONDITIONS_TWO_LINE_HEIGHT + 8;
        this.list = this.addRenderableWidget(new EventList(this, this.minecraft, layout.middleX, layout.middleWidth, listTop, listBottom));

        int actionX = layout.rightX + 7;
        int actionWidth = layout.rightWidth - 14;
        int actionY = PANEL_TOP + 132;

        this.addButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY, actionWidth, 20,
                Component.translatable("button.music_and_melody.add"), false, button -> addEntry()));
        this.removeButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY + 25, actionWidth, 20,
                Component.translatable("button.music_and_melody.remove"), false, button -> removeSelected()));
        this.saveButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY + 50, actionWidth, 20,
                Component.translatable("button.music_and_melody.save"), false, button -> saveEntry()));
        this.deleteButton = this.addRenderableWidget(new IconButton(deleteMessage(), deleteIcon(), button -> toggleDelete()));
        this.deleteButton.setX(actionX + (actionWidth - IconButton.SIZE) / 2);
        this.deleteButton.setY(layout.panelBottom - 52);
        this.addRenderableWidget(new WorkspaceButton(actionX, layout.panelBottom - 28, actionWidth, 20,
                CommonComponents.GUI_DONE, false, button -> this.onClose()));
        this.addRenderableWidget(new WorkspaceButton(settingsX, layout.panelBottom - 28, settingsWidth, 20,
                Component.translatable("screen.music_and_melody.sound_pools"), false,
                button -> openSoundPools()));
        buildPlaybackControls(layout);
        positionSettingsWidgets(layout);

        if (this.soundPoolsSnapshot != null) {
            restoreEditor(this.soundPoolsSnapshot);
            this.soundPoolsSnapshot = null;
        } else if (this.selectedIndex >= 0 && this.selectedIndex < this.entries.size()) {
            select(this.selectedIndex);
        } else {
            clearEditor();
        }
        refreshEditorState();
    }

    private void openSoundPools() {
        if (this.musicField == null || this.weightField == null || this.conditionsField == null) {
            return;
        }
        this.soundPoolsSnapshot = new EditorSnapshot(
                this.selectedIndex,
                this.categoryIndex,
                this.priorityIndex,
                this.replace,
                this.sustain,
                this.constant,
                this.musicField.getValue(),
                this.weightField.getValue(),
                this.conditionsField.getValue());
        this.minecraft.gui.setScreen(new SoundPoolsScreen(this));
    }

    private void restoreEditor(EditorSnapshot snapshot) {
        this.loadingEditor = true;
        this.selectedIndex = snapshot.selectedIndex;
        this.categoryIndex = snapshot.categoryIndex;
        this.priorityIndex = snapshot.priorityIndex;
        this.replace = snapshot.replace;
        this.sustain = snapshot.sustain;
        this.constant = snapshot.constant;
        this.musicField.setValue(snapshot.music);
        this.weightField.setValue(snapshot.weight);
        this.conditionsField.setValue(snapshot.conditions);
        this.loadingEditor = false;
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
        MouseButtonEvent layoutEvent = toLayoutMouse(event);
        EditorLayout layout = editorLayout();
        if (this.parent instanceof MusicPlayerScreen musicPlayer
                && musicPlayer.handlePlaybackClick(layoutEvent.x(), layoutEvent.y(), layout.middleX, layout.middleWidth, layout.bottomPanelTop)) {
            return true;
        }
        return super.mouseClicked(layoutEvent, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent layoutEvent = toLayoutMouse(event);
        EditorLayout layout = editorLayout();
        if (this.parent instanceof MusicPlayerScreen musicPlayer
                && musicPlayer.handlePlaybackDrag(layoutEvent.x(), layoutEvent.y(), layout.middleX, layout.middleWidth)) return true;
        return super.mouseDragged(layoutEvent, dragX / MaMDataConfig.get().gui_multiplier, dragY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.parent instanceof MusicPlayerScreen musicPlayer && musicPlayer.handlePlaybackRelease()) return true;
        return super.mouseReleased(toLayoutMouse(event));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = toLayoutMouse(mouseX);
        int y = toLayoutMouse(mouseY);
        EditorLayout layout = editorLayout();
        if (x >= layout.leftX && x < layout.leftX + layout.leftWidth
                && y >= settingsViewportTop() && y < settingsViewportBottom(layout)
                && this.settingsScrollMax > 0) {
            this.settingsScroll = Math.max(0,
                    Math.min(this.settingsScrollMax, this.settingsScroll - scrollY * SETTINGS_STEP));
            positionSettingsWidgets(layout);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
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

    private void buildPlaybackControls(EditorLayout layout) {
        int groupWidth = IconButton.SIZE * 5 + 16;
        int x = layout.middleX + (layout.middleWidth - groupWidth) / 2;
        int y = layout.bottomPanelTop + 29;
        IconButton search = this.addRenderableWidget(new IconButton(
                Component.translatable("screen.music_and_melody.search"), IconButton.icon("search"), ignored -> {}));
        search.setX(layout.middleX + 8);
        search.setY(layout.bottomPanelTop + 5);
        search.active = false;
        IconButton shuffle = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.shuffle"),
                IconButton.icon(PlaylistHelper.isShuffleQueue() ? "shuffle_on" : "shuffle_off"), ignored -> {
            PlaylistHelper.shuffleQueue();
            this.rebuildWidgets();
        }));
        IconButton previous = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.previous"),
                IconButton.icon("previous"), ignored -> PlaylistHelper.previousQueue()));
        this.playPauseButton = this.addRenderableWidget(new IconButton(
                Component.translatable(PlaylistHelper.isQueuePlaying() ? "button.music_and_melody.pause" : "button.music_and_melody.play"),
                IconButton.icon(PlaylistHelper.isQueuePlaying() ? "pause" : "play"), ignored -> {
            if (PlaylistHelper.isQueuePlaying()) PlaylistHelper.pauseQueue();
            else PlaylistHelper.playNextNow();
        }));
        IconButton next = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.next"),
                IconButton.icon("next"), ignored -> PlaylistHelper.skipQueue()));
        IconButton loop = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.loop"),
                IconButton.icon(PlaylistHelper.isLoopingQueue() ? "looping" : "loop"), ignored -> {
            PlaylistHelper.setLoopingQueue(!PlaylistHelper.isLoopingQueue());
            this.rebuildWidgets();
        }));
        IconButton[] controls = {shuffle, previous, this.playPauseButton, next, loop};
        for (int i = 0; i < controls.length; i++) {
            controls[i].setX(x + i * (IconButton.SIZE + 4));
            controls[i].setY(y);
        }
    }

    private void renderEditorShell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        EditorLayout layout = editorLayout();
        graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, BACKGROUND);
        drawPanel(graphics, layout.leftX, PANEL_TOP, layout.leftWidth, layout.panelBottom - PANEL_TOP);
        drawPanel(graphics, layout.middleX, PANEL_TOP, layout.middleWidth, layout.bottomPanelTop - PANEL_GAP - PANEL_TOP);
        drawPanel(graphics, layout.middleX, layout.bottomPanelTop, layout.middleWidth, layout.panelBottom - layout.bottomPanelTop);
        drawPanel(graphics, layout.rightX, PANEL_TOP, layout.rightWidth, layout.panelBottom - PANEL_TOP);

        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.settings").withStyle(ChatFormatting.BOLD), layout.leftX + 8, PANEL_TOP + 11, TEXT_HEADER);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.entries").withStyle(ChatFormatting.BOLD), layout.middleX + 8, PANEL_TOP + 11, TEXT_HEADER);

        Event.Source source = activeSource();
        int titleX = layout.rightX + 8;
        if (source != null) {
            int iconSize = 18;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, source.icon()), titleX, PANEL_TOP + 16,
                    0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            titleX += iconSize + 6;
        }
        int titleWidth = Math.max(1, layout.rightWidth - (titleX - layout.rightX) - 8);
        if (source == null) {
            drawMarquee(graphics, title(), titleX, PANEL_TOP + 18, titleWidth, TEXT_TITLE);
        } else {
            Component sourceId = Component.literal(source.id.toString());
            drawMarquee(graphics, source.record.name(), titleX, PANEL_TOP + 15, titleWidth, TEXT_TITLE);
            drawMarquee(graphics, sourceId, titleX, PANEL_TOP + 27, titleWidth, TEXT_DESCRIPTION);
            int descriptionY = PANEL_TOP + 50;
            for (FormattedCharSequence line : this.font.split(source.record.description(), Math.max(1, layout.rightWidth - 16))) {
                if (descriptionY + this.font.lineHeight > PANEL_TOP + 124) break;
                ThemeHelper.text(graphics, this.font, line, layout.rightX + 8, descriptionY, TEXT_PRIMARY);
                descriptionY += this.font.lineHeight + 2;
            }
        }

        int controlLabelY = PANEL_TOP + 30 - (int) Math.round(this.settingsScroll);
        int settingsX = layout.leftX + 8;
        graphics.enableScissor(layout.leftX + 1, settingsViewportTop(),
                layout.leftX + layout.leftWidth - 1, settingsViewportBottom(layout));
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.type"), settingsX, controlLabelY, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.priority"), settingsX, controlLabelY + SETTINGS_STEP, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.replace"), settingsX, controlLabelY + SETTINGS_STEP * 2, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.sustain"), settingsX, controlLabelY + SETTINGS_STEP * 3, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.constant"), settingsX, controlLabelY + SETTINGS_STEP * 4, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.weight"), settingsX, controlLabelY + SETTINGS_STEP * 5, TEXT_DESCRIPTION);
        graphics.disableScissor();
        renderSettingsScrollbar(graphics, layout, mouseX, mouseY);

        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.music"), layout.middleX + 8, PANEL_TOP + 27, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.event_editor.conditions"), layout.middleX + 8, PANEL_TOP + 65, TEXT_DESCRIPTION);
        if (this.parent instanceof MusicPlayerScreen musicPlayer) {
            musicPlayer.renderPlaybackStrip(graphics, layout.middleX, layout.middleWidth, layout.bottomPanelTop);
        }
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
            ThemeHelper.text(graphics, this.font, text, x, y, color);
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
        graphics.enableScissor(x, y - 1, x + width, y + this.font.lineHeight + 2);
        ThemeHelper.text(graphics, this.font, text, x - offset, y, color);
        graphics.disableScissor();
    }

    private int settingsControlY() {
        return PANEL_TOP + 42 - (int) Math.round(this.settingsScroll);
    }

    private int settingsViewportTop() {
        return PANEL_TOP + 27;
    }

    private int settingsViewportBottom(EditorLayout layout) {
        return layout.panelBottom - 35;
    }

    private void updateSettingsScroll(EditorLayout layout) {
        int contentBottom = PANEL_TOP + 42 + SETTINGS_STEP * 5 + 20;
        this.settingsScrollMax = Math.max(0, contentBottom - settingsViewportBottom(layout));
        this.settingsScroll = Math.max(0, Math.min(this.settingsScroll, this.settingsScrollMax));
    }

    private void positionSettingsWidgets(EditorLayout layout) {
        AbstractWidget[] widgets = {
                this.categoryButton,
                this.priorityButton,
                this.replaceButton,
                this.sustainButton,
                this.constantButton,
                this.weightField
        };
        int y = settingsControlY();
        int top = settingsViewportTop();
        int bottom = settingsViewportBottom(layout);
        for (int index = 0; index < widgets.length; index++) {
            AbstractWidget widget = widgets[index];
            widget.setY(y + SETTINGS_STEP * index);
            widget.visible = widget.getY() >= top && widget.getY() + widget.getHeight() <= bottom;
        }
    }

    private void renderSettingsScrollbar(GuiGraphicsExtractor graphics, EditorLayout layout,
                                          int mouseX, int mouseY) {
        if (this.settingsScrollMax <= 0) {
            return;
        }
        int top = settingsViewportTop();
        int bottom = settingsViewportBottom(layout);
        int viewport = bottom - top;
        int thumbHeight = Math.max(16,
                (int) Math.round(viewport * viewport / (viewport + this.settingsScrollMax)));
        int thumbY = top + (int) Math.round(
                (viewport - thumbHeight) * this.settingsScroll / this.settingsScrollMax);
        int x = layout.leftX + layout.leftWidth - 5;
        graphics.fill(x, top, x + 2, bottom, BAR_BACKGROUND);
        int color = mouseX >= x - 2 && mouseX <= x + 4
                && mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                ? PANEL_HIGHLIGHTED
                : SCROLLBAR_THUMB;
        graphics.fill(x - 1, thumbY, x + 3, thumbY + thumbHeight, color);
    }

    private record EditorLayout(int leftX, int leftWidth, int middleX, int middleWidth, int rightX, int rightWidth, int panelBottom, int bottomPanelTop) { }

    private record EditorSnapshot(
            int selectedIndex,
            int categoryIndex,
            int priorityIndex,
            boolean replace,
            boolean sustain,
            boolean constant,
            String music,
            String weight,
            String conditions
    ) {
    }

    @Override
    public void onClose() {
        if (this.savedChanges) {
            EventHelper.resetMusicBreak();
        }
        if (this.parent instanceof MusicPlayerScreen musicPlayer) musicPlayer.rebuildWidgets();
        this.minecraft.gui.setScreen(this.parent);
    }

    private void toggleDelete() {
        Event.Source source = activeSource();
        if (source != null && this.parent instanceof MusicPlayerScreen musicPlayer
                && RemoteContentManager.owner(source.id, RemotePack.Tag.EVENT).isPresent()) {
            musicPlayer.manageRemoteContent(source.id, RemotePack.Tag.EVENT);
            return;
        }
        if (source == null || !source.isConfig()) return;
        if (this.parent instanceof MusicPlayerScreen musicPlayer) {
            this.deletePending = musicPlayer.toggleEventDeletePending(source.id);
        } else {
            this.deletePending = !this.deletePending;
        }
        refreshEditorState();
    }

    private Component deleteMessage() {
        Event.Source source = activeSource();
        if (source != null && RemoteContentManager.owner(source.id, RemotePack.Tag.EVENT).isPresent()) {
            return Component.translatable("button.music_and_melody.manage");
        }
        return Component.translatable(this.deletePending ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
    }

    private Identifier deleteIcon() {
        Event.Source source = activeSource();
        if (source != null && RemoteContentManager.owner(source.id, RemotePack.Tag.EVENT).isPresent()) {
            return IconButton.icon("manage");
        }
        return IconButton.icon(this.deletePending ? "restore" : "delete");
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
        this.replace = entry.replace();
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
        this.categoryIndex = Event.CategoryType.POOL.ordinal();
        this.priorityIndex = Event.PriorityType.LOW.ordinal();
        this.replace = false;
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
                this.replace,
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

    private void refreshEditorState() {
        if (this.playPauseButton != null) {
            boolean playing = PlaylistHelper.isQueuePlaying();
            this.playPauseButton.setIconAndTooltip(IconButton.icon(playing ? "pause" : "play"),
                    Component.translatable(playing ? "button.music_and_melody.pause" : "button.music_and_melody.play"));
        }
        Event.ScreenEntry selected = selectedEntry();
        boolean configSelected = selected != null && selected.source().isConfig();
        if (this.categoryButton != null) this.categoryButton.setMessage(categoryMessage());
        if (this.priorityButton != null) this.priorityButton.setMessage(priorityMessage());
        if (this.replaceButton != null) this.replaceButton.setMessage(replaceMessage());
        if (this.sustainButton != null) this.sustainButton.setMessage(sustainMessage());
        if (this.constantButton != null) this.constantButton.setMessage(constantMessage());
        if (this.musicField != null) {
            String example = this.font.plainSubstrByWidth("eg. " + musicExample(),
                    Math.max(0, this.musicField.getWidth() - 8));
            this.musicField.setHint(Component.literal(example)
                    .withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        }

        Event.Record.Entry draft = editorEntry();
        Event.Source source = selectedSource();
        boolean editable = source != null && source.isConfig();
        if (this.categoryButton != null) this.categoryButton.active = editable;
        if (this.priorityButton != null) this.priorityButton.active = editable;
        if (this.replaceButton != null) this.replaceButton.active = editable;
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
        if (this.deleteButton != null) {
            this.deleteButton.setIconAndTooltip(deleteIcon(), deleteMessage());
            Event.Source managedSource = activeSource();
            this.deleteButton.active = editable || managedSource != null
                    && RemoteContentManager.owner(managedSource.id, RemotePack.Tag.EVENT).isPresent();
        }
    }

    private boolean saveSourceEntries(Event.Source source, List<Event.Record.Entry> entries) {
        if (!Event.saveSourceEntries(source, entries)) return false;
        this.savedChanges = true;
        return true;
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
        String category = Event.categoryName(CATEGORIES[this.categoryIndex]);
        String key = switch (category) {
            case "album" -> "screen.music_and_melody.tag.album";
            case "playlist" -> "screen.music_and_melody.tag.playlist";
            default -> "screen.music_and_melody.event_editor.category." + category;
        };
        return Component.translatable(key);
    }

    private Component priorityMessage() {
        return Component.translatable("screen.music_and_melody.event_editor.priority." + Event.priorityName(PRIORITIES[this.priorityIndex]));
    }

    private Component replaceMessage() {
        return Component.translatable("screen.music_and_melody.event_editor.replace")
                .append(": ")
                .append(CommonComponents.optionStatus(this.replace));
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
            case PLAYLIST -> "config:example";
            case POOL -> "minecraft:music.overworld.forest";
            case TRACK -> "music_and_melody:music/overworld/alpha";
            case DISC -> "minecraft:cat";
        };
    }

    private static int categoryIndex(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (Event.categoryName(CATEGORIES[i]).equals(category.toLowerCase(Locale.ROOT))) return i;
        }
        return Event.CategoryType.POOL.ordinal();
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

        if (type.equals("below_y") || type.equals("pve_score") || type.equals("pvp_score")) {
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
        return type.equals("time") || type.equals("weather") || type.equals("game_mode") || type.equals("special") || type.equals("mod_loaded") || type.equals("bossbar") || type.equals("below_version") || type.equals("player");
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
            graphics.fill(x, top, x + 3, bottom, BAR_BACKGROUND);
            int thumbTop = Math.max(top, this.scrollBarY());
            int thumbBottom = Math.min(bottom, thumbTop + this.scrollerHeight());
            graphics.fill(x, thumbTop, x + 3, thumbBottom,
                    mouseX >= x - 2 && mouseX <= x + 5 && mouseY >= thumbTop && mouseY <= thumbBottom ? PANEL_HIGHLIGHTED : SCROLLBAR_THUMB);
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
            ThemeHelper.text(graphics, Minecraft.getInstance().font, this.line, this.getContentX(),
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
            graphics.fill(x, top, x + 4, bottom, BAR_BACKGROUND);
            int thumbTop = Math.max(top, this.scrollBarY());
            int thumbBottom = Math.min(bottom, thumbTop + this.scrollerHeight());
            int color = mouseX >= x - 2 && mouseX <= x + 6 && mouseY >= thumbTop && mouseY <= thumbBottom ? PANEL_HIGHLIGHTED : SCROLLBAR_THUMB;
            graphics.fill(x, thumbTop, x + 4, thumbBottom, color);
        }

        @Override
        public int getRowLeft() {
            return this.panelX + 5;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {}

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {}
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
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHTED);
            } else if (hovered) {
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHTED);
            }
            int color = this.index == this.screen.selectedIndex ? TEXT_SELECTED : this.row.source().isEnabled() ? TEXT_PRIMARY : TEXT_DISABLED;
            Event.Record.Entry entry = this.row.entry();
            String first = entry.music() + " (" + entry.category() + ")";
            String second = "priority=" + entry.priority() + " | replace=" + entry.replace() + " | sustain=" + entry.sustain() + " | constant=" + entry.constant() + " | weight=" + entry.weight();
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

}
