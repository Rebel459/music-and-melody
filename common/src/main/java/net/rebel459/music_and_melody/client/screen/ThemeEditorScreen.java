package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.Theme;
import net.rebel459.music_and_melody.client.ThemeListener;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.ExampleHintEditBox;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.EnumMap;
import net.minecraft.util.FormattedCharSequence;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.EnumSet;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

/** Three-panel editor for a custom theme, and a read-only preview for other themes. */
final class ThemeEditorScreen extends Screen {

    private static final int OUTER_MARGIN = 10;
    private static final int PANEL_GAP = 7;
    private static final int PANEL_TOP = 10;
    private static final int PANEL_BOTTOM_MARGIN = 10;
    private static final int BOTTOM_HEIGHT = 56;
    private static final int REFERENCE_WORKSPACE_WIDTH = 620;
    private static final int MIN_LEFT_WIDTH = 112;
    private static final int MIN_MIDDLE_WIDTH = 180;
    private static final int MIN_RIGHT_WIDTH = 124;

    private final MusicPlayerScreen parent;
    private Theme theme;
    private final boolean readOnly;
    private final Map<Role, String> values = new EnumMap<>(Role.class);
    private final Map<Role, String> parentValues = new EnumMap<>(Role.class);
    private final Set<Role> overriddenRoles = EnumSet.noneOf(Role.class);
    private final Set<MainOption> overriddenMain = EnumSet.noneOf(MainOption.class);
    private boolean buttonTextures;
    private boolean parentButtonTextures;
    private boolean overriddenButtonTextures;
    private boolean textShadow;
    private boolean parentTextShadow;
    private boolean overriddenTextShadow;
    private final Map<Category, WorkspaceButton> categoryButtons = new EnumMap<>(Category.class);
    private final Map<Role, RoleButton> roleButtons = new EnumMap<>(Role.class);
    private Category category = Category.MAIN;
    private Role selectedRole = Role.BACKGROUND;
    private Theme inheritedTheme;

    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox iconField;
    private EditBox parentField;
    private EditBox hexField;
    private WorkspaceButton buttonTexturesButton;
    private WorkspaceButton textShadowButton;
    private ColourSlider alphaSlider;
    private ColourSlider redSlider;
    private ColourSlider greenSlider;
    private ColourSlider blueSlider;
    private WorkspaceButton saveButton;
    private IconButton deleteButton;
    private IconButton searchButton;
    private IconButton playPauseButton;
    private boolean loading;
    private boolean dirty;
    private boolean pendingDelete;
    private RemotePack managedRemotePack;
    private int layoutWidth;
    private int layoutHeight;
    private int leftX;
    private int leftWidth;
    private int middleX;
    private int middleWidth;
    private int rightX;
    private int rightWidth;
    private int panelBottom;
    private int bottomPanelTop;
    private int middleViewportTop;
    private int middleViewportBottom;
    private double middleScroll;
    private double middleScrollMax;
    private boolean draggingMiddleScrollbar;
    private double middleScrollbarDragOffset;

    ThemeEditorScreen(MusicPlayerScreen parent, Theme theme) {
        super(Component.translatable("screen.music_and_melody.theme_editor"));
        this.parent = parent;
        this.theme = theme;
        this.readOnly = theme == null || !theme.isCustom();
        this.inheritedTheme = theme == null || Theme.DEFAULT_ID.equals(theme.theme) ? null : ThemeListener.theme(theme.parent);
        this.pendingDelete = theme != null && parent.isThemeDeletePending(theme.theme);
        loadValues(theme);
    }

    @Override
    protected void init() {
        calculateLayout();
        this.addRenderableOnly(this::renderShell);

        for (Category value : Category.values()) {
            int y = PANEL_TOP + 38 + value.ordinal() * 25;
            WorkspaceButton button = this.addRenderableWidget(new WorkspaceButton(leftX + 8, y, leftWidth - 16, 20,
                    value.label(), value == this.category, ignored -> selectCategory(value)));
            this.categoryButtons.put(value, button);
        }

        Map<Category, Integer> categoryRows = new EnumMap<>(Category.class);
        for (Role role : Role.values()) {
            int row = categoryRows.getOrDefault(role.category, 0);
            int roleY = PANEL_TOP + 38 + row * 24;
            categoryRows.put(role.category, row + 1);
            RoleButton button = this.addRenderableWidget(new RoleButton(middleX + 8, roleY, middleWidth - 16, 20,
                    role, role.label(), role == this.selectedRole, ignored -> selectRole(role)));
            this.roleButtons.put(role, button);
        }

        int middleFieldX = middleX + 8;
        int middleFieldWidth = Math.max(40, middleWidth - 16);
        this.nameField = field(Component.translatable("screen.music_and_melody.create_theme.name"), middleFieldX, PANEL_TOP + 50, middleFieldWidth);
        this.descriptionField = field(Component.translatable("screen.music_and_melody.theme.description"), middleFieldX, PANEL_TOP + 88, middleFieldWidth);
        this.iconField = field(Component.translatable("screen.music_and_melody.create_theme.icon"), middleFieldX, PANEL_TOP + 126, middleFieldWidth);
        this.parentField = field(Component.translatable("screen.music_and_melody.theme_editor.parent"), middleFieldX, PANEL_TOP + 164, middleFieldWidth);
        this.buttonTexturesButton = this.addRenderableWidget(new WorkspaceButton(middleFieldX, buttonTexturesBaseY(),
                middleFieldWidth, 20, buttonTexturesMessage(), this.buttonTextures,
                ignored -> toggleButtonTextures()));
        this.textShadowButton = this.addRenderableWidget(new WorkspaceButton(middleFieldX, textShadowBaseY(),
                middleFieldWidth, 20, textShadowMessage(), this.textShadow,
                ignored -> toggleTextShadow()));
        this.loading = true;
        this.nameField.setValue(theme.name.getString());
        this.descriptionField.setValue(theme.description.getString());
        this.iconField.setValue(theme.icon.toString());
        this.parentField.setValue(rawParent(theme));
        this.parentField.setHint(Component.literal("music_and_melody:default").withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        this.loading = false;
        this.nameField.setResponder(value -> textChanged(MainOption.NAME, value));
        this.descriptionField.setResponder(value -> textChanged(MainOption.DESCRIPTION, value));
        this.iconField.setResponder(value -> textChanged(MainOption.ICON, value));
        this.parentField.setResponder(ignored -> parentChanged());

        int fieldX = rightX + 8;
        int fieldWidth = Math.max(40, rightWidth - 16);
        this.hexField = field(Component.translatable("screen.music_and_melody.theme_editor.hex"), fieldX, PANEL_TOP + 52, fieldWidth);
        this.hexField.setResponder(this::hexChanged);

        int sliderWidth = fieldWidth;
        this.alphaSlider = this.addRenderableWidget(new ColourSlider(fieldX, PANEL_TOP + 82, sliderWidth, 20, Channel.ALPHA));
        this.redSlider = this.addRenderableWidget(new ColourSlider(fieldX, PANEL_TOP + 106, sliderWidth, 20, Channel.RED));
        this.greenSlider = this.addRenderableWidget(new ColourSlider(fieldX, PANEL_TOP + 130, sliderWidth, 20, Channel.GREEN));
        this.blueSlider = this.addRenderableWidget(new ColourSlider(fieldX, PANEL_TOP + 154, sliderWidth, 20, Channel.BLUE));

        int actionX = rightX + 7;
        int actionWidth = Math.max(40, rightWidth - 14);
        this.deleteButton = this.addRenderableWidget(new IconButton(deleteMessage(), deleteIcon(), ignored -> toggleDelete()));
        this.deleteButton.setX(actionX + (actionWidth - IconButton.SIZE) / 2);
        this.deleteButton.setY(panelBottom - 76);
        this.saveButton = this.addRenderableWidget(new WorkspaceButton(actionX, panelBottom - 52, actionWidth, 20,
                Component.translatable("button.music_and_melody.save"), false, ignored -> saveChanges()));
        this.addRenderableWidget(new WorkspaceButton(actionX, panelBottom - 28, actionWidth, 20,
                CommonComponents.GUI_DONE, false, ignored -> done()));
        buildPlaybackControls();
        updateWidgets();
    }

    private void buildPlaybackControls() {
        int groupWidth = IconButton.SIZE * 5 + 16;
        int x = middleX + (middleWidth - groupWidth) / 2;
        int y = bottomPanelTop + 29;
        this.searchButton = this.addRenderableWidget(new IconButton(
                Component.translatable("screen.music_and_melody.search"), IconButton.icon("search"), ignored -> {}));
        this.searchButton.setX(this.middleX + 8);
        this.searchButton.setY(this.bottomPanelTop + 5);
        this.searchButton.active = false;
        IconButton shuffle = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.shuffle"),
                IconButton.icon(PlaylistHelper.isShuffleQueue() ? "shuffle_on" : "shuffle_off"), ignored -> {
            PlaylistHelper.shuffleQueue();
            this.rebuildWidgets();
        }));
        shuffle.setX(x);
        shuffle.setY(y);
        IconButton previous = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.previous"),
                IconButton.icon("previous"), ignored -> PlaylistHelper.previousQueue()));
        previous.setX(x + IconButton.SIZE + 4);
        previous.setY(y);
        this.playPauseButton = this.addRenderableWidget(new IconButton(playPauseMessage(), playPauseIcon(), ignored -> {
            if (PlaylistHelper.isQueuePlaying()) PlaylistHelper.pauseQueue();
            else PlaylistHelper.playNextNow();
        }));
        this.playPauseButton.setX(x + (IconButton.SIZE + 4) * 2);
        this.playPauseButton.setY(y);
        IconButton next = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.next"),
                IconButton.icon("next"), ignored -> PlaylistHelper.skipQueue()));
        next.setX(x + (IconButton.SIZE + 4) * 3);
        next.setY(y);
        IconButton loop = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.loop"),
                IconButton.icon(PlaylistHelper.isLoopingQueue() ? "looping" : "loop"), ignored -> {
            PlaylistHelper.setLoopingQueue(!PlaylistHelper.isLoopingQueue());
            this.rebuildWidgets();
        }));
        loop.setX(x + (IconButton.SIZE + 4) * 4);
        loop.setY(y);
    }

    private static Component playPauseMessage() {
        return Component.translatable(PlaylistHelper.isQueuePlaying()
                ? "button.music_and_melody.pause" : "button.music_and_melody.play");
    }

    private static Identifier playPauseIcon() {
        return IconButton.icon(PlaylistHelper.isQueuePlaying() ? "pause" : "play");
    }

    private EditBox field(Component message, int x, int y, int width) {
        // IDs, metadata, and texture paths are ordinary text values. Keep
        // them visually consistent with the event editor's text fields;
        // colour values alone use the colour editor below.
        EditBox field = this.addRenderableWidget(new ExampleHintEditBox(this.font, x, y, width, 20, message));
        field.setMaxLength(512);
        return field;
    }

    private void loadValues(Theme theme) {
        for (Role role : Role.values()) {
            values.put(role, role.resolved(theme));
            parentValues.put(role, role.resolved(this.inheritedTheme));
        }
        this.buttonTextures = theme != null && theme.elements.buttonTextures();
        this.parentButtonTextures = this.inheritedTheme != null && this.inheritedTheme.elements.buttonTextures();
        this.overriddenButtonTextures = this.buttonTextures != this.parentButtonTextures;
        this.textShadow = theme == null || theme.text.shadow();
        this.parentTextShadow = this.inheritedTheme == null || this.inheritedTheme.text.shadow();
        this.overriddenTextShadow = this.textShadow != this.parentTextShadow;
        if (theme == null) return;
        Theme.Record record = theme.record;
        if (record == null) return;
        for (Role role : Role.values()) {
            role.raw(record).ifPresent(value -> {
                values.put(role, value);
                if (!sameColour(value, parentValues.get(role))) overriddenRoles.add(role);
            });
        }
        if (record.name().isPresent()) overriddenMain.add(MainOption.NAME);
        if (record.description().isPresent()) overriddenMain.add(MainOption.DESCRIPTION);
        if (record.icon().isPresent()) overriddenMain.add(MainOption.ICON);
        record.elements().flatMap(Theme.RawElements::buttonTextures).ifPresent(value -> {
            this.buttonTextures = value;
            this.overriddenButtonTextures = value != this.parentButtonTextures;
        });
        record.text().flatMap(Theme.RawText::shadow).ifPresent(value -> {
            this.textShadow = value;
            this.overriddenTextShadow = value != this.parentTextShadow;
        });
    }

    private String rawParent(Theme theme) {
        return theme.record != null ? theme.record.parent().orElse("") : "";
    }

    private void selectCategory(Category category) {
        this.category = category;
        for (Map.Entry<Category, WorkspaceButton> entry : this.categoryButtons.entrySet()) entry.getValue().setSelected(entry.getKey() == category);
        if (category == Category.PANELS) selectRole(Role.BACKGROUND);
        else if (category == Category.ELEMENTS) selectRole(Role.BUTTON_BACKGROUND);
        else if (category == Category.TEXT) selectRole(Role.SELECTED);
        updateWidgets();
    }

    private void selectRole(Role role) {
        this.selectedRole = role;
        this.category = role.category;
        for (Map.Entry<Category, WorkspaceButton> entry : this.categoryButtons.entrySet()) entry.getValue().setSelected(entry.getKey() == this.category);
        for (Map.Entry<Role, RoleButton> entry : this.roleButtons.entrySet()) entry.getValue().setSelected(entry.getKey() == role);
        refreshColourControls();
        updateWidgets();
    }

    private void updateWidgets() {
        boolean main = this.category == Category.MAIN;
        for (Map.Entry<Role, RoleButton> entry : this.roleButtons.entrySet()) {
            boolean visible = !main && entry.getKey().category == this.category;
            entry.getValue().visible = visible;
            entry.getValue().active = visible;
        }
        boolean elements = this.category == Category.ELEMENTS;
        boolean text = this.category == Category.TEXT;
        this.nameField.visible = main;
        this.descriptionField.visible = main;
        this.iconField.visible = main;
        this.parentField.visible = main;
        this.buttonTexturesButton.visible = elements;
        this.textShadowButton.visible = text;
        boolean colour = !main && this.managedRemotePack == null;
        this.hexField.visible = colour;
        this.alphaSlider.visible = colour;
        this.redSlider.visible = colour;
        this.greenSlider.visible = colour;
        this.blueSlider.visible = colour;
        this.nameField.active = !readOnly && this.nameField.visible;
        this.descriptionField.active = !readOnly && this.descriptionField.visible;
        this.iconField.active = !readOnly && this.iconField.visible;
        this.parentField.active = !readOnly && this.parentField.visible;
        this.buttonTexturesButton.active = !readOnly && this.buttonTexturesButton.visible;
        this.textShadowButton.active = !readOnly && this.textShadowButton.visible;
        this.buttonTexturesButton.setSelected(this.buttonTextures);
        this.buttonTexturesButton.setMessage(buttonTexturesMessage());
        this.textShadowButton.setSelected(this.textShadow);
        this.textShadowButton.setMessage(textShadowMessage());
        this.hexField.active = !readOnly && colour;
        this.alphaSlider.active = !readOnly && colour;
        this.redSlider.active = !readOnly && colour;
        this.greenSlider.active = !readOnly && colour;
        this.blueSlider.active = !readOnly && colour;
        updateMiddleScroll();
        this.saveButton.active = !readOnly && dirty && isValidDraft() && !pendingDelete;
        boolean remote = theme != null && RemoteContentManager.owner(theme.theme, RemotePack.Tag.THEME).isPresent();
        this.deleteButton.visible = !readOnly || remote;
        this.deleteButton.active = !readOnly || remote;
        this.deleteButton.setIconAndTooltip(deleteIcon(), deleteMessage());
        updateTextColours();
        refreshColourControls();
    }

    private void updateMiddleScroll() {
        this.middleViewportTop = PANEL_TOP + 32;
        // Keep the viewport (and its scrollbar) inside the upper middle panel.
        this.middleViewportBottom = Math.max(this.middleViewportTop + 20,
                this.bottomPanelTop - PANEL_GAP - 2);
        int contentBottom = switch (this.category) {
            case MAIN -> PANEL_TOP + 204;
            case ELEMENTS -> buttonTexturesBaseY() + 24;
            case PANELS -> PANEL_TOP + 38 + roleCount(this.category) * 24;
            case TEXT -> textShadowBaseY() + 24;
        };
        this.middleScrollMax = Math.max(0.0D, contentBottom - this.middleViewportBottom);
        this.middleScroll = Math.max(0.0D, Math.min(this.middleScroll, this.middleScrollMax));

        for (Role role : Role.values()) {
            RoleButton button = this.roleButtons.get(role);
            if (button == null) continue;
            int y = middleY(roleBaseY(role));
            button.setY(y);
            button.visible = role.category == this.category && middleVisible(y, button.getHeight());
        }

        setMiddleFieldPosition(this.nameField, PANEL_TOP + 50);
        setMiddleFieldPosition(this.descriptionField, PANEL_TOP + 88);
        setMiddleFieldPosition(this.iconField, PANEL_TOP + 126);
        setMiddleFieldPosition(this.parentField, PANEL_TOP + 164);
        this.buttonTexturesButton.setY(middleY(buttonTexturesBaseY()));
        this.buttonTexturesButton.visible = this.category == Category.ELEMENTS
                && middleVisible(this.buttonTexturesButton.getY(), this.buttonTexturesButton.getHeight());
        this.textShadowButton.setY(middleY(textShadowBaseY()));
        this.textShadowButton.visible = this.category == Category.TEXT
                && middleVisible(this.textShadowButton.getY(), this.textShadowButton.getHeight());
    }

    private int buttonTexturesBaseY() {
        return PANEL_TOP + 38 + roleCount(Category.ELEMENTS) * 24 + 4;
    }

    private int textShadowBaseY() {
        return PANEL_TOP + 38 + roleCount(Category.TEXT) * 24 + 4;
    }

    private void setMiddleFieldPosition(EditBox field, int baseY) {
        setMiddleFieldPosition(field, baseY, field == this.nameField || field == this.descriptionField
                || field == this.iconField || field == this.parentField ? this.category == Category.MAIN
                : this.category == Category.ELEMENTS);
    }

    private void setMiddleFieldPosition(EditBox field, int baseY, boolean categoryVisible) {
        if (field == null) return;
        field.setY(middleY(baseY));
        field.visible = categoryVisible && middleVisible(field.getY(), field.getHeight());
    }

    private int roleCount(Category category) {
        int count = 0;
        for (Role role : Role.values()) if (role.category == category) count++;
        return count;
    }

    private int roleBaseY(Role target) {
        int row = 0;
        for (Role role : Role.values()) {
            if (role.category != target.category) continue;
            if (role == target) return PANEL_TOP + 38 + row * 24;
            row++;
        }
        return PANEL_TOP + 38;
    }

    private int middleY(int baseY) {
        return baseY - (int) Math.round(this.middleScroll);
    }

    private boolean middleVisible(int y, int height) {
        return y >= this.middleViewportTop && y + height <= this.middleViewportBottom;
    }

    private boolean inMiddleViewport(double x, double y) {
        return x >= this.middleX + 2 && x < this.middleX + this.middleWidth - 2
                && y >= this.middleViewportTop && y < this.middleViewportBottom;
    }

    private int middleScrollbarX() {
        return this.middleX + this.middleWidth - 6;
    }

    private int middleScrollbarHeight() {
        int viewportHeight = Math.max(1, this.middleViewportBottom - this.middleViewportTop);
        int contentHeight = Math.max(viewportHeight, (int) Math.round(viewportHeight + this.middleScrollMax));
        return Math.max(12, Math.round(viewportHeight * (viewportHeight / (float) contentHeight)));
    }

    private int middleScrollbarY() {
        int viewportHeight = this.middleViewportBottom - this.middleViewportTop;
        int thumbHeight = middleScrollbarHeight();
        int travel = Math.max(0, viewportHeight - thumbHeight);
        return this.middleViewportTop + (this.middleScrollMax <= 0.0D
                ? 0 : (int) Math.round(travel * this.middleScroll / this.middleScrollMax));
    }

    private void setMiddleScroll(double value) {
        this.middleScroll = Math.max(0.0D, Math.min(this.middleScrollMax, value));
        updateMiddleScroll();
    }

    private void refreshColourControls() {
        if (this.hexField == null || this.selectedRole == null) return;
        this.loading = true;
        String value = values.getOrDefault(this.selectedRole, "");
        this.hexField.setValue(value);
        Integer parsedValue = ThemeListener.parseColor(value);
        int parsed = parsedValue == null ? 0 : parsedValue;
        this.alphaSlider.setSlider((parsed >>> 24) & 0xFF);
        this.redSlider.setSlider((parsed >>> 16) & 0xFF);
        this.greenSlider.setSlider((parsed >>> 8) & 0xFF);
        this.blueSlider.setSlider(parsed & 0xFF);
        this.loading = false;
    }

    private void hexChanged(String value) {
        if (loading || readOnly || selectedRole == null) return;
        dirty = true;
        Integer parsed = ThemeListener.parseColor(value);
        if (parsed == null) {
            overriddenRoles.add(selectedRole);
            updateWidgets();
            return;
        }
        values.put(selectedRole, ThemeListener.formatColor(parsed));
        updateRoleOverride(selectedRole);
        refreshColourControls();
        updateWidgets();
    }

    private void setChannel(Channel channel, int value) {
        if (loading || readOnly || selectedRole == null) return;
        Integer parsed = ThemeListener.parseColor(values.get(selectedRole));
        if (parsed == null) parsed = ThemeListener.parseColor(parentValues.get(selectedRole));
        if (parsed == null) return;
        int result = switch (channel) {
            case ALPHA -> (parsed & 0x00FFFFFF) | (value << 24);
            case RED -> (parsed & 0xFF00FFFF) | (value << 16);
            case GREEN -> (parsed & 0xFFFF00FF) | (value << 8);
            case BLUE -> (parsed & 0xFFFFFF00) | value;
        };
        values.put(selectedRole, ThemeListener.formatColor(result));
        updateRoleOverride(selectedRole);
        dirty = true;
        refreshColourControls();
        updateWidgets();
    }

    private void textChanged(MainOption option, String value) {
        if (loading || readOnly) return;
        overriddenMain.add(option);
        dirty = true;
        updateWidgets();
    }

    private void toggleButtonTextures() {
        if (loading || readOnly) return;
        this.buttonTextures = !this.buttonTextures;
        this.overriddenButtonTextures = this.buttonTextures != this.parentButtonTextures;
        dirty = true;
        updateWidgets();
    }

    private void toggleTextShadow() {
        if (loading || readOnly) return;
        this.textShadow = !this.textShadow;
        this.overriddenTextShadow = this.textShadow != this.parentTextShadow;
        dirty = true;
        updateWidgets();
    }

    private void parentChanged() {
        if (loading || readOnly) return;
        Identifier id = Identifier.tryParse(parentField.getValue().trim());
        Theme resolvedParent = id == null ? ThemeListener.theme(Theme.DEFAULT_ID) : ThemeListener.theme(id);
        this.inheritedTheme = resolvedParent == null ? ThemeListener.theme(Theme.DEFAULT_ID) : resolvedParent;
        this.loading = true;
        for (Role role : Role.values()) {
            String previousParent = parentValues.get(role);
            String nextParent = role.resolved(this.inheritedTheme);
            parentValues.put(role, nextParent);
            if (!overriddenRoles.contains(role) || sameColour(values.get(role), previousParent)) values.put(role, nextParent);
            updateRoleOverride(role);
        }
        boolean previousParentButtonTextures = this.parentButtonTextures;
        this.parentButtonTextures = this.inheritedTheme != null && this.inheritedTheme.elements.buttonTextures();
        if (!this.overriddenButtonTextures || this.buttonTextures == previousParentButtonTextures) {
            this.buttonTextures = this.parentButtonTextures;
        }
        this.overriddenButtonTextures = this.buttonTextures != this.parentButtonTextures;
        boolean previousParentTextShadow = this.parentTextShadow;
        this.parentTextShadow = this.inheritedTheme == null || this.inheritedTheme.text.shadow();
        if (!this.overriddenTextShadow || this.textShadow == previousParentTextShadow) {
            this.textShadow = this.parentTextShadow;
        }
        this.overriddenTextShadow = this.textShadow != this.parentTextShadow;
        this.loading = false;
        dirty = true;
        updateWidgets();
    }

    private void updateRoleOverride(Role role) {
        if (sameColour(values.get(role), parentValues.get(role))) overriddenRoles.remove(role);
        else overriddenRoles.add(role);
    }

    private static boolean sameColour(String first, String second) {
        Integer a = ThemeListener.parseColor(first);
        Integer b = ThemeListener.parseColor(second);
        return a != null && a.equals(b);
    }

    private void updateTextColours() {
        setFieldColour(nameField, true);
        setFieldColour(descriptionField, true);
        setFieldColour(iconField, true);
        setFieldColour(parentField, true);
        setFieldColour(hexField, overriddenRoles.contains(selectedRole));
    }

    private void setFieldColour(EditBox field, boolean overridden) {
        if (field instanceof ExampleHintEditBox example) example.setNormalTextColor(overridden ? TEXT_PRIMARY : TEXT_DISABLED);
        else field.setTextColor(overridden ? TEXT_PRIMARY : TEXT_DISABLED);
    }

    private boolean isValidDraft() {
        if (nameField == null || nameField.getValue().trim().isEmpty()) return false;
        if (Identifier.tryParse(iconField.getValue().trim()) == null) return false;
        String parent = parentField.getValue().trim();
        if (!parent.isEmpty() && Identifier.tryParse(parent) == null) return false;
        for (Role role : overriddenRoles) if (ThemeListener.parseColor(values.get(role)) == null) return false;
        return true;
    }

    private Theme.Record draftRecord() {
        Optional<String> parent = parentField.getValue().trim().isEmpty()
                ? Optional.empty() : Optional.of(parentField.getValue().trim());
        Theme.RawPanels panels = new Theme.RawPanels(
                roleValue(Role.BACKGROUND), roleValue(Role.PANEL_BACKGROUND),
                roleValue(Role.PANEL_OUTLINE), roleValue(Role.PANEL_HIGHLIGHT),
                roleValue(Role.POPUP_PANEL_BACKGROUND), roleValue(Role.POPUP_OUTLINE),
                roleValue(Role.POPUP_OVERLAY));
        Theme.RawElements elements = new Theme.RawElements(
                roleValue(Role.BUTTON_BACKGROUND), roleValue(Role.BUTTON_HIGHLIGHT),
                roleValue(Role.BUTTON_DISABLED), roleValue(Role.OUTLINE), roleValue(Role.BAR_BACKGROUND),
                roleValue(Role.BAR_THUMB), buttonTexturesValue());
        Theme.RawText text = new Theme.RawText(
                roleValue(Role.SELECTED), roleValue(Role.TITLE), roleValue(Role.PRIMARY), roleValue(Role.PRIMARY_HIGHLIGHT),
                roleValue(Role.DESCRIPTION), roleValue(Role.HEADER), roleValue(Role.HEADER_SECONDARY), roleValue(Role.FAVOURITE),
                roleValue(Role.EXAMPLE), roleValue(Role.DISABLED), roleValue(Role.WARNING), textShadowValue());
        return new Theme.Record(theme.theme, componentValue(MainOption.NAME, nameField),
                componentValue(MainOption.DESCRIPTION, descriptionField), stringValue(MainOption.ICON, iconField), parent,
                Optional.of(panels), Optional.of(elements), Optional.of(text));
    }

    private Optional<String> roleValue(Role role) {
        return overriddenRoles.contains(role) ? Optional.of(values.get(role)) : Optional.empty();
    }

    private Optional<Component> componentValue(MainOption option, EditBox field) {
        return overriddenMain.contains(option) ? Optional.of(Component.literal(field.getValue().trim())) : Optional.empty();
    }

    private Optional<String> stringValue(MainOption option, EditBox field) {
        return overriddenMain.contains(option) ? Optional.of(field.getValue().trim()) : Optional.empty();
    }

    private Optional<Boolean> buttonTexturesValue() {
        return this.overriddenButtonTextures ? Optional.of(this.buttonTextures) : Optional.empty();
    }

    private Component buttonTexturesMessage() {
        return Component.translatable("screen.music_and_melody.theme_editor.button_textures",
                this.buttonTextures ? Component.translatable("options.on") : Component.translatable("options.off"));
    }

    private Optional<Boolean> textShadowValue() {
        return this.overriddenTextShadow ? Optional.of(this.textShadow) : Optional.empty();
    }

    private Component textShadowMessage() {
        return Component.translatable("screen.music_and_melody.theme_editor.text_shadow",
                this.textShadow ? Component.translatable("options.on") : Component.translatable("options.off"));
    }

    private boolean saveChanges() {
        if (readOnly || pendingDelete || !isValidDraft()) return false;
        if (!ThemeListener.saveConfigTheme(draftRecord())) return false;
        Theme updated = ThemeListener.theme(theme.theme);
        if (updated != null) theme = updated;
        dirty = false;
        parent.themeChanged(theme.theme);
        updateWidgets();
        return true;
    }

    private void toggleDelete() {
        if (this.managedRemotePack != null) {
            this.parent.toggleRemoteDeletePending(this.managedRemotePack);
            updateWidgets();
            return;
        }
        if (theme != null && RemoteContentManager.owner(theme.theme, RemotePack.Tag.THEME).isPresent()) {
            this.managedRemotePack = RemoteContentManager.owner(theme.theme, RemotePack.Tag.THEME).orElse(null);
            updateWidgets();
            return;
        }
        if (readOnly) return;
        pendingDelete = parent.toggleThemeDeletePending(theme.theme);
        updateWidgets();
    }

    private Component deleteMessage() {
        if (this.managedRemotePack != null) {
            return Component.translatable(parent.isRemoteDeletePending(this.managedRemotePack)
                    ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
        }
        if (theme != null && RemoteContentManager.owner(theme.theme, RemotePack.Tag.THEME).isPresent()) {
            return Component.translatable("button.music_and_melody.manage");
        }
        return Component.translatable(pendingDelete ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
    }

    private Identifier deleteIcon() {
        if (this.managedRemotePack != null) {
            return IconButton.icon(parent.isRemoteDeletePending(this.managedRemotePack) ? "restore" : "delete");
        }
        if (theme != null && RemoteContentManager.owner(theme.theme, RemotePack.Tag.THEME).isPresent()) {
            return IconButton.icon("manage");
        }
        return IconButton.icon(pendingDelete ? "restore" : "delete");
    }

    private void done() {
        if (!readOnly && dirty) {
            this.minecraft.gui.setScreen(new ThemeExitConfirmScreen(this));
            return;
        }
        finish(false);
    }

    void finish(boolean save) {
        if (save && dirty) {
            pendingDelete = false;
            if (!saveChanges()) {
                pendingDelete = parent.isThemeDeletePending(theme.theme);
                this.minecraft.gui.setScreen(this);
                return;
            }
            pendingDelete = parent.isThemeDeletePending(theme.theme);
        }
        dirty = false;
        ThemeListener.restoreActive();
        parent.themeChanged(theme.theme);
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        if (this.playPauseButton != null) this.playPauseButton.setIconAndTooltip(playPauseIcon(), playPauseMessage());
        IconButton.setTooltipScale(MaMDataConfig.get().gui_multiplier);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(MaMDataConfig.get().gui_multiplier);
            super.extractRenderState(graphics, toLayoutMouse(mouseX), toLayoutMouse(mouseY), tickDelta);
        } finally {
            graphics.pose().popMatrix();
            IconButton.resetTooltipScale();
        }
    }

    @Override
    protected void repositionElements() {
        calculateLayout();
        this.rebuildWidgets();
    }

    private void calculateLayout() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
        this.panelBottom = layoutHeight - PANEL_BOTTOM_MARGIN;
        this.bottomPanelTop = panelBottom - BOTTOM_HEIGHT;
        int workspaceWidth = Math.max(3, layoutWidth - OUTER_MARGIN * 2);
        int usable = Math.max(3, workspaceWidth - PANEL_GAP * 2);
        int preferredMinimum = MIN_LEFT_WIDTH + MIN_MIDDLE_WIDTH + MIN_RIGHT_WIDTH;
        if (usable < preferredMinimum) {
            this.leftWidth = Math.max(1, Math.round(usable * (MIN_LEFT_WIDTH / (float) preferredMinimum)));
            this.rightWidth = Math.max(1, Math.round(usable * (MIN_RIGHT_WIDTH / (float) preferredMinimum)));
            this.middleWidth = Math.max(1, usable - leftWidth - rightWidth);
        } else if (workspaceWidth <= REFERENCE_WORKSPACE_WIDTH) {
            int viewportWidth = workspaceWidth + OUTER_MARGIN * 2;
            this.leftWidth = Math.max(132, Math.min(210, (int) (viewportWidth * 0.23F)));
            this.rightWidth = Math.max(144, Math.min(214, (int) (viewportWidth * 0.20F)));
            this.middleWidth = usable - leftWidth - rightWidth;
            if (this.middleWidth < MIN_MIDDLE_WIDTH) {
                int shortfall = MIN_MIDDLE_WIDTH - this.middleWidth;
                int fromLeft = Math.min(shortfall / 2, Math.max(0, this.leftWidth - MIN_LEFT_WIDTH));
                int fromRight = Math.min(shortfall - fromLeft, Math.max(0, this.rightWidth - MIN_RIGHT_WIDTH));
                this.leftWidth -= fromLeft;
                this.rightWidth -= fromRight;
                this.middleWidth = usable - leftWidth - rightWidth;
            }
        } else {
            this.leftWidth = Math.round(workspaceWidth * (147.0F / REFERENCE_WORKSPACE_WIDTH));
            this.rightWidth = Math.round(workspaceWidth * (144.0F / REFERENCE_WORKSPACE_WIDTH));
            this.middleWidth = usable - leftWidth - rightWidth;
        }
        this.leftX = OUTER_MARGIN;
        this.middleX = leftX + leftWidth + PANEL_GAP;
        this.rightX = middleX + middleWidth + PANEL_GAP;
    }

    private int toLayoutMouse(double coordinate) {
        return Math.round((float) (coordinate / MaMDataConfig.get().gui_multiplier));
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier,
                event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        MouseButtonEvent layoutEvent = toLayoutMouse(event);
        double x = layoutEvent.x();
        double y = layoutEvent.y();
        if (this.parent.handlePlaybackClick(x, y, this.middleX, this.middleWidth, this.bottomPanelTop)) return true;
        int scrollbarX = middleScrollbarX();
        int thumbY = middleScrollbarY();
        int thumbBottom = thumbY + middleScrollbarHeight();
        if (this.middleScrollMax > 0.0D && x >= scrollbarX - 4 && x <= scrollbarX + 5
                && y >= this.middleViewportTop && y < this.middleViewportBottom) {
            if (y >= thumbY && y < thumbBottom) {
                this.draggingMiddleScrollbar = true;
                this.middleScrollbarDragOffset = y - thumbY;
            } else {
                setMiddleScroll((y - this.middleViewportTop) / (double)
                        Math.max(1, this.middleViewportBottom - this.middleViewportTop) * this.middleScrollMax);
                this.draggingMiddleScrollbar = true;
                this.middleScrollbarDragOffset = middleScrollbarHeight() / 2.0D;
            }
            return true;
        }
        return super.mouseClicked(layoutEvent, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent layoutEvent = toLayoutMouse(event);
        if (this.parent.handlePlaybackDrag(layoutEvent.x(), layoutEvent.y(), this.middleX, this.middleWidth)) return true;
        if (this.draggingMiddleScrollbar) {
            int thumbHeight = middleScrollbarHeight();
            int travel = Math.max(1, this.middleViewportBottom - this.middleViewportTop - thumbHeight);
            double thumbTop = layoutEvent.y() - this.middleScrollbarDragOffset;
            double fraction = (thumbTop - this.middleViewportTop) / travel;
            setMiddleScroll(fraction * this.middleScrollMax);
            return true;
        }
        return super.mouseDragged(layoutEvent, dragX / MaMDataConfig.get().gui_multiplier,
                dragY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingMiddleScrollbar = false;
        if (this.parent.handlePlaybackRelease()) return true;
        return super.mouseReleased(toLayoutMouse(event));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double x = mouseX / MaMDataConfig.get().gui_multiplier;
        double y = mouseY / MaMDataConfig.get().gui_multiplier;
        if (this.middleScrollMax > 0.0D && inMiddleViewport(x, y)) {
            setMiddleScroll(this.middleScroll - scrollY * 24.0D);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    private void renderShell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, layoutWidth, layoutHeight, SCREEN_BACKGROUND);
        drawPanel(graphics, leftX, PANEL_TOP, leftWidth, panelBottom - PANEL_TOP);
        drawPanel(graphics, middleX, PANEL_TOP, middleWidth, bottomPanelTop - PANEL_GAP - PANEL_TOP);
        drawPanel(graphics, middleX, bottomPanelTop, middleWidth, panelBottom - bottomPanelTop);
        drawPanel(graphics, rightX, PANEL_TOP, rightWidth, panelBottom - PANEL_TOP);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme_editor.categories"), leftX + 8, PANEL_TOP + 14, TEXT_HEADER);
        Component middleHeading = category == Category.MAIN
                ? Component.translatable("screen.music_and_melody.theme_editor.metadata")
                : category == Category.ELEMENTS ? category.label()
                : Component.translatable("screen.music_and_melody.theme_editor.colours");
        ThemeHelper.text(graphics, this.font, middleHeading, middleX + 8, PANEL_TOP + 14, TEXT_HEADER);
        if (this.managedRemotePack != null) {
            renderManagedRemoteDetails(graphics, this.managedRemotePack);
        } else if (category != Category.MAIN) {
            ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme_editor.value"), rightX + 8, PANEL_TOP + 14, TEXT_HEADER);
        }
        if (this.managedRemotePack == null && category != Category.MAIN && selectedRole != null) {
            int colour = ThemeListener.parseColor(values.get(selectedRole)) == null ? TEXT_PENDING_DELETION
                    : ThemeListener.parseColor(values.get(selectedRole));
            ThemeHelper.text(graphics, this.font, selectedRole.label(), rightX + 8, PANEL_TOP + 32, TEXT_TITLE);
            graphics.fill(rightX + rightWidth - 25, PANEL_TOP + 29, rightX + rightWidth - 8, PANEL_TOP + 46, colour);
            if (ThemeListener.parseColor(values.get(selectedRole)) == null) {
                ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme.invalid_colour"), rightX + 8, PANEL_TOP + 179, TEXT_PENDING_DELETION);
            }
        }
        graphics.enableScissor(this.middleX + 2, this.middleViewportTop,
                this.middleX + this.middleWidth - 2, this.middleViewportBottom);
        if (category == Category.MAIN) {
            drawMiddleFieldLabel(graphics, MainOption.NAME.label(), PANEL_TOP + 38, overriddenMain.contains(MainOption.NAME));
            drawMiddleFieldLabel(graphics, MainOption.DESCRIPTION.label(), PANEL_TOP + 76, overriddenMain.contains(MainOption.DESCRIPTION));
            drawMiddleFieldLabel(graphics, MainOption.ICON.label(), PANEL_TOP + 114, overriddenMain.contains(MainOption.ICON));
            drawMiddleFieldLabel(graphics, Component.translatable("screen.music_and_melody.theme_editor.parent"), PANEL_TOP + 152, true);
        }
        graphics.disableScissor();
        renderMiddleScrollbar(graphics, mouseX, mouseY);
        if (!theme.valid) {
            ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme.invalid"), leftX + 8, panelBottom - 44, TEXT_PENDING_DELETION);
        }
        if (readOnly) ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme.read_only"), leftX + 8, panelBottom - 28, TEXT_DESCRIPTION);
        renderPlaybackStrip(graphics);
    }

    private void drawMiddleFieldLabel(GuiGraphicsExtractor graphics, Component label, int y, boolean overridden) {
        ThemeHelper.text(graphics, this.font, label, middleX + 8, middleY(y), overridden ? TEXT_PRIMARY : TEXT_DISABLED);
    }

    private void renderManagedRemoteDetails(GuiGraphicsExtractor graphics, RemotePack pack) {
        int x = rightX + 8;
        int width = Math.max(1, rightWidth - 16);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.details"), x, PANEL_TOP + 14, TEXT_HEADER);
        int iconSize = Math.min(42, width);
        int iconY = PANEL_TOP + 30;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, RemoteIconManager.icon(pack)),
                x, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int textX = x + iconSize + 6;
        int textWidth = Math.max(1, width - iconSize - 6);
        int titleColour = parent.isRemoteDeletePending(pack) ? TEXT_PENDING_DELETION : TEXT_TITLE;
        drawRemoteDetailValue(graphics, pack.name(), textX, iconY + 1, textWidth, titleColour);
        drawRemoteDetailValue(graphics, Component.literal(pack.id().toString()), textX, iconY + 13, textWidth, TEXT_DESCRIPTION);

        int fieldY = iconY + iconSize + 6;
        drawRemoteDetailField(graphics, "screen.music_and_melody.remote_details.repository", Component.literal(pack.repository()), x, fieldY, width);
        drawRemoteDetailField(graphics, "screen.music_and_melody.remote_details.version", Component.literal(pack.version()), x, fieldY + 26, width);
        drawRemoteDetailField(graphics, "screen.music_and_melody.remote_details.state",
                Component.translatable(MusicPlayerScreen.remoteStateTranslationKey(RemoteContentManager.state(pack))), x, fieldY + 52, width);
    }

    private void drawRemoteDetailField(GuiGraphicsExtractor graphics, String headingKey, Component value, int x, int y, int width) {
        ThemeHelper.text(graphics, this.font, Component.translatable(headingKey), x, y, TEXT_DESCRIPTION);
        drawRemoteDetailValue(graphics, value, x, y + 12, width, TEXT_PRIMARY);
    }

    private void drawRemoteDetailValue(GuiGraphicsExtractor graphics, Component value, int x, int y, int width, int colour) {
        List<FormattedCharSequence> lines = this.font.split(value, Math.max(1, width));
        if (!lines.isEmpty()) ThemeHelper.text(graphics, this.font, lines.getFirst(), x, y, colour);
    }

    private void renderMiddleScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.middleScrollMax <= 0.0D) return;
        int x = middleScrollbarX();
        graphics.fill(x, this.middleViewportTop, x + 2, this.middleViewportBottom, BAR_BACKGROUND);
        int y = middleScrollbarY();
        int colour = mouseX >= x - 4 && mouseX <= x + 5 && mouseY >= y && mouseY < y + middleScrollbarHeight()
                ? PANEL_HIGHLIGHT : SCROLLBAR_THUMB;
        graphics.fill(x - 1, y, x + 3, y + middleScrollbarHeight(), colour);
    }

    private void renderPlaybackStrip(GuiGraphicsExtractor graphics) {
        this.parent.renderPlaybackStrip(graphics, this.middleX, this.middleWidth, this.bottomPanelTop);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, PANEL_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_OUTLINE);
    }

    @Override
    public void onClose() {
        done();
    }

    private enum Category {
        MAIN("screen.music_and_melody.theme_editor.main"),
        PANELS("screen.music_and_melody.theme_editor.panels"),
        ELEMENTS("screen.music_and_melody.theme_editor.elements"),
        TEXT("screen.music_and_melody.theme_editor.text");

        private final String key;

        Category(String key) {
            this.key = key;
        }

        Component label() {
            return Component.translatable(this.key);
        }
    }

    private enum MainOption {
        NAME("screen.music_and_melody.create_theme.name"),
        DESCRIPTION("screen.music_and_melody.theme.description"),
        ICON("screen.music_and_melody.create_theme.icon");

        private final String key;
        MainOption(String key) { this.key = key; }
        Component label() { return Component.translatable(this.key); }
    }

    private enum Role {
        BACKGROUND(Category.PANELS, "screen.music_and_melody.theme.background"),
        PANEL_BACKGROUND(Category.PANELS, "screen.music_and_melody.theme.panel_background"),
        PANEL_OUTLINE(Category.PANELS, "screen.music_and_melody.theme.panel_outline"),
        PANEL_HIGHLIGHT(Category.PANELS, "screen.music_and_melody.theme.panel_highlight"),
        POPUP_PANEL_BACKGROUND(Category.PANELS, "screen.music_and_melody.theme.popup_panel_background"),
        POPUP_OUTLINE(Category.PANELS, "screen.music_and_melody.theme.popup_outline"),
        POPUP_OVERLAY(Category.PANELS, "screen.music_and_melody.theme.popup_overlay"),
        BUTTON_BACKGROUND(Category.ELEMENTS, "screen.music_and_melody.theme.button_background"),
        BUTTON_HIGHLIGHT(Category.ELEMENTS, "screen.music_and_melody.theme.button_highlight"),
        BUTTON_DISABLED(Category.ELEMENTS, "screen.music_and_melody.theme.button_disabled"),
        OUTLINE(Category.ELEMENTS, "screen.music_and_melody.theme.outline"),
        BAR_BACKGROUND(Category.ELEMENTS, "screen.music_and_melody.theme.bar_background"),
        BAR_THUMB(Category.ELEMENTS, "screen.music_and_melody.theme.bar_thumb"),
        SELECTED(Category.TEXT, "screen.music_and_melody.theme.selected"),
        TITLE(Category.TEXT, "screen.music_and_melody.theme.title"),
        PRIMARY(Category.TEXT, "screen.music_and_melody.theme.primary"),
        PRIMARY_HIGHLIGHT(Category.TEXT, "screen.music_and_melody.theme.primary_highlight"),
        DESCRIPTION(Category.TEXT, "screen.music_and_melody.theme.description_text"),
        HEADER(Category.TEXT, "screen.music_and_melody.theme.header"),
        HEADER_SECONDARY(Category.TEXT, "screen.music_and_melody.theme.header_secondary"),
        FAVOURITE(Category.TEXT, "screen.music_and_melody.theme.favourite"),
        EXAMPLE(Category.TEXT, "screen.music_and_melody.theme.example"),
        DISABLED(Category.TEXT, "screen.music_and_melody.theme.disabled"),
        WARNING(Category.TEXT, "screen.music_and_melody.theme.warning");

        private final Category category;
        private final String key;
        Role(Category category, String key) {
            this.category = category;
            this.key = key;
        }

        Component label() {
            return Component.translatable(this.key);
        }

        String resolved(Theme theme) {
            if (theme == null) return "";
            return switch (this) {
                case BACKGROUND -> theme.panels.background();
                case PANEL_BACKGROUND -> theme.panels.panelBackground();
                case PANEL_OUTLINE -> theme.panels.panelOutline();
                case PANEL_HIGHLIGHT -> theme.panels.panelHighlight();
                case POPUP_PANEL_BACKGROUND -> theme.panels.popupPanelBackground();
                case POPUP_OUTLINE -> theme.panels.popupOutline();
                case POPUP_OVERLAY -> theme.panels.popupOverlay();
                case BUTTON_BACKGROUND -> theme.elements.buttonBackground();
                case BUTTON_HIGHLIGHT -> theme.elements.buttonHighlight();
                case BUTTON_DISABLED -> theme.elements.buttonDisabled();
                case OUTLINE -> theme.elements.outline();
                case BAR_BACKGROUND -> theme.elements.barBackground();
                case BAR_THUMB -> theme.elements.barThumb();
                case SELECTED -> theme.text.selected();
                case TITLE -> theme.text.title();
                case PRIMARY -> theme.text.primary();
                case PRIMARY_HIGHLIGHT -> theme.text.primaryHighlight();
                case DESCRIPTION -> theme.text.description();
                case HEADER -> theme.text.header();
                case HEADER_SECONDARY -> theme.text.headerSecondary();
                case FAVOURITE -> theme.text.favourite();
                case EXAMPLE -> theme.text.example();
                case DISABLED -> theme.text.disabled();
                case WARNING -> theme.text.warning();
            };
        }

        Optional<String> raw(Theme.Record record) {
            if (record == null) return Optional.empty();
            return switch (this) {
                case BACKGROUND -> record.panels().flatMap(Theme.RawPanels::background);
                case PANEL_BACKGROUND -> record.panels().flatMap(Theme.RawPanels::panelBackground);
                case PANEL_OUTLINE -> record.panels().flatMap(Theme.RawPanels::panelOutline);
                case PANEL_HIGHLIGHT -> record.panels().flatMap(Theme.RawPanels::panelHighlight);
                case POPUP_PANEL_BACKGROUND -> record.panels().flatMap(Theme.RawPanels::popupPanelBackground);
                case POPUP_OUTLINE -> record.panels().flatMap(Theme.RawPanels::popupOutline);
                case POPUP_OVERLAY -> record.panels().flatMap(Theme.RawPanels::popupOverlay);
                case BUTTON_BACKGROUND -> record.elements().flatMap(Theme.RawElements::buttonBackground);
                case BUTTON_HIGHLIGHT -> record.elements().flatMap(Theme.RawElements::buttonHighlight);
                case BUTTON_DISABLED -> record.elements().flatMap(Theme.RawElements::buttonDisabled);
                case OUTLINE -> record.elements().flatMap(Theme.RawElements::outline);
                case BAR_BACKGROUND -> record.elements().flatMap(Theme.RawElements::barBackground);
                case BAR_THUMB -> record.elements().flatMap(Theme.RawElements::barThumb);
                case SELECTED -> record.text().flatMap(Theme.RawText::selected);
                case TITLE -> record.text().flatMap(Theme.RawText::title);
                case PRIMARY -> record.text().flatMap(Theme.RawText::primary);
                case PRIMARY_HIGHLIGHT -> record.text().flatMap(Theme.RawText::primaryHighlight);
                case DESCRIPTION -> record.text().flatMap(Theme.RawText::description);
                case HEADER -> record.text().flatMap(Theme.RawText::header);
                case HEADER_SECONDARY -> record.text().flatMap(Theme.RawText::headerSecondary);
                case FAVOURITE -> record.text().flatMap(Theme.RawText::favourite);
                case EXAMPLE -> record.text().flatMap(Theme.RawText::example);
                case DISABLED -> record.text().flatMap(Theme.RawText::disabled);
                case WARNING -> record.text().flatMap(Theme.RawText::warning);
            };
        }
    }

    private enum Channel {
        ALPHA("screen.music_and_melody.theme.alpha"),
        RED("screen.music_and_melody.theme.red"),
        GREEN("screen.music_and_melody.theme.green"),
        BLUE("screen.music_and_melody.theme.blue");

        private final String key;

        Channel(String key) {
            this.key = key;
        }

        Component label(int value) {
            return Component.translatable(this.key, value);
        }
    }

    private final class ColourSlider extends AbstractSliderButton {
        private final Channel channel;
        private int channelValue;

        ColourSlider(int x, int y, int width, int height, Channel channel) {
            super(x, y, width, height, Component.empty(), 1.0D);
            this.channel = channel;
            setSlider(255);
        }

        void setSlider(int value) {
            this.channelValue = Math.max(0, Math.min(255, value));
            this.value = this.channelValue / 255.0D;
            updateMessage();
        }

        @Override
        protected void setValue(double value) {
            this.value = Math.max(0.0D, Math.min(1.0D, value));
            this.channelValue = (int) Math.round(this.value * 255.0D);
            updateMessage();
            ThemeEditorScreen.this.setChannel(this.channel, this.channelValue);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.channel.label(this.channelValue));
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            boolean highlighted = this.active && (this.isMouseOver(mouseX, mouseY) || this.isFocused());
            int background = !this.active ? BUTTON_DISABLED : highlighted ? BUTTON_HIGHLIGHT : BUTTON_PASSIVE;
            graphics.fill(x, y, x + width, y + height, background);
            int filled = Math.round((width - 4) * (float) this.value);
            graphics.fill(x, y, x + filled + 2, y + height, PANEL_HIGHLIGHT);
            int handleX = x + filled;
            graphics.fill(handleX, y - 1, handleX + 4, y + height + 1, TEXT_TITLE);
            var font = Minecraft.getInstance().font;
            ThemeHelper.text(graphics, font, this.getMessage(), x + (width - font.width(this.getMessage())) / 2,
                    y + (height - 8) / 2, overriddenRoles.contains(selectedRole) ? TEXT_PRIMARY : TEXT_DISABLED, TEXT_SHADOW);
        }
    }

    private final class RoleButton extends WorkspaceButton {
        private final Role role;

        RoleButton(int x, int y, int width, int height, Role role, Component message, boolean selected, OnPress onPress) {
            super(x, y, width, height, message, selected, onPress);
            this.role = role;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            super.extractContents(graphics, mouseX, mouseY, tickDelta);
            Integer colour = ThemeListener.parseColor(values.get(this.role));
            if (colour != null) graphics.fill(this.getX() + 4, this.getY() + 4, this.getX() + 16, this.getY() + 16, colour);
        }

        @Override
        protected int textColor(boolean highlighted) {
            if (!overriddenRoles.contains(this.role)) return TEXT_DISABLED;
            return highlighted ? TEXT_PRIMARY_HIGHLIGHT : TEXT_PRIMARY;
        }
    }
}
