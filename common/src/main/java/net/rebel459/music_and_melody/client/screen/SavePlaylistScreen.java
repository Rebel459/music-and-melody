package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

class SavePlaylistScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.save_playlist");
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final MusicPlayerScreen parent;
    private EditBox nameField;
    private EditBox iconField;
    private EditBox pathField;
    private WorkspaceButton saveButton;
    private int layoutWidth;
    private int layoutHeight;

    SavePlaylistScreen(MusicPlayerScreen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderDialog);
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int fieldX = x + 12;
        int fieldWidth = width - 24;

        this.nameField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 50, fieldWidth, 20,
                Component.translatable("screen.music_and_melody.create_theme.name")));
        this.nameField.setMaxLength(80);
        this.nameField.setResponder(value -> {
            updatePathHint();
            refreshSaveState();
        });

        this.iconField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 91, fieldWidth, 20,
                Component.translatable("screen.music_and_melody.create_theme.icon")));
        this.iconField.setMaxLength(256);
        this.iconField.setResponder(value -> refreshSaveState());
        this.iconField.setHint(Component.literal(DEFAULT_ICON.toString()).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));

        this.pathField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 132, fieldWidth, 20,
                Component.translatable("screen.music_and_melody.create_theme.path")));
        this.pathField.setMaxLength(256);
        this.pathField.setResponder(value -> refreshSaveState());
        updatePathHint();

        int buttonY = y + panelHeight() - 29;
        int buttonWidth = (fieldWidth - 5) / 2;
        this.saveButton = this.addRenderableWidget(new WorkspaceButton(fieldX, buttonY, buttonWidth, 20, saveMessage(), false,
                ignored -> save()));
        this.addRenderableWidget(new WorkspaceButton(fieldX + buttonWidth + 5, buttonY, buttonWidth, 20, CommonComponents.GUI_CANCEL, false,
                ignored -> this.onClose()));
        this.setInitialFocus(this.nameField);
        refreshSaveState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, -1, -1, tickDelta);
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

    private void calculateLayoutSize() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    @Override
    protected void repositionElements() {
        calculateLayoutSize();
        this.rebuildWidgets();
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private int toLayoutMouse(int coordinate) {
        return Math.round(coordinate / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {}

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        if ((POPUP_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        ThemeHelper.centeredText(graphics, this.font, this.title, x + width / 2, y + 13, TEXT_TITLE);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.create_theme.name"), x + 12, y + 38, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.create_theme.icon"), x + 12, y + 79, TEXT_DESCRIPTION);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.create_theme.path"), x + 12, y + 120, TEXT_DESCRIPTION);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        event = toLayoutMouse(event);
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        event = toLayoutMouse(event);
        dragX /= MaMDataConfig.get().gui_multiplier;
        dragY /= MaMDataConfig.get().gui_multiplier;
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseDragged(event, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        event = toLayoutMouse(event);
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseReleased(event);
        return true;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        return mouseX >= panelX() && mouseX < panelX() + panelWidth()
                && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void save() {
        if (Playlist.saveCustomPlaylist(this.minecraft, this.nameField.getValue(), this.iconField.getValue(), this.pathField.getValue())) {
            this.onClose();
        }
    }

    private void refreshSaveState() {
        if (this.saveButton == null) return;
        this.saveButton.active = !this.nameField.getValue().trim().isEmpty()
                && iconValid()
                && Playlist.canWriteConfigPlaylist(this.nameField.getValue(), this.pathField.getValue());
        this.saveButton.setMessage(saveMessage());
    }

    private boolean iconValid() {
        String icon = this.iconField.getValue().trim();
        return icon.isEmpty() || Identifier.tryParse(icon) != null;
    }

    private Component saveMessage() {
        return Component.translatable(!this.nameField.getValue().trim().isEmpty()
                && Playlist.configPlaylistExists(this.nameField.getValue(), this.pathField == null ? "" : this.pathField.getValue())
                ? "button.music_and_melody.overwrite"
                : "button.music_and_melody.save");
    }

    private void updatePathHint() {
        if (this.pathField == null) return;
        String preview = Playlist.previewConfigPlaylistPath(this.nameField.getValue());
        this.pathField.setHint(preview.isEmpty() ? Component.empty() : Component.literal(preview).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
    }

    private int panelWidth() {
        return Math.min(360, this.layoutWidth - 24);
    }

    private int panelHeight() {
        return Math.min(193, this.layoutHeight - 28);
    }

    private int panelX() {
        return this.layoutWidth / 2 - panelWidth() / 2;
    }

    private int panelY() {
        return this.layoutHeight / 2 - panelHeight() / 2;
    }
}
