package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.Theme;
import net.rebel459.music_and_melody.client.ThemeListener;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

/** Creates a config-backed theme without leaving the compact workspace. */
final class CreateThemeScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.create_theme");
    private final MusicPlayerScreen parent;
    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox iconField;
    private EditBox pathField;
    private WorkspaceButton createButton;
    private int layoutWidth;
    private int layoutHeight;

    CreateThemeScreen(MusicPlayerScreen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderDialog);
        int x = panelX();
        int y = panelY();
        int fieldX = x + 12;
        int fieldWidth = panelWidth() - 24;

        this.nameField = field(Component.translatable("screen.music_and_melody.create_theme.name"), fieldX, y + 40, fieldWidth);
        this.nameField.setMaxLength(80);
        this.nameField.setResponder(value -> {
            updatePathHint();
            refreshCreateState();
        });
        this.descriptionField = field(Component.translatable("screen.music_and_melody.theme.description"), fieldX, y + 76, fieldWidth);
        this.iconField = field(Component.translatable("screen.music_and_melody.create_theme.icon"), fieldX, y + 112, fieldWidth);
        this.iconField.setHint(Component.literal(Theme.DEFAULT_ICON.toString()).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        this.iconField.setResponder(value -> refreshCreateState());
        this.pathField = field(Component.translatable("screen.music_and_melody.create_theme.path"), fieldX, y + 148, fieldWidth);
        this.pathField.setResponder(value -> refreshCreateState());
        updatePathHint();

        int buttonY = y + panelHeight() - 29;
        int buttonWidth = (fieldWidth - 5) / 2;
        this.createButton = this.addRenderableWidget(new WorkspaceButton(fieldX, buttonY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.create"), false, ignored -> create()));
        this.addRenderableWidget(new WorkspaceButton(fieldX + buttonWidth + 5, buttonY, buttonWidth, 20,
                CommonComponents.GUI_CANCEL, false, ignored -> this.onClose()));
        this.setInitialFocus(this.nameField);
        refreshCreateState();
    }

    private EditBox field(Component placeholder, int x, int y, int width) {
        EditBox field = this.addRenderableWidget(new EditBox(this.font, x, y, width, 20, placeholder));
        field.setMaxLength(256);
        return field;
    }

    private void create() {
        if (this.createButton == null || !this.createButton.active) return;
        Theme theme = ThemeListener.createConfigTheme(this.nameField.getValue(), this.descriptionField.getValue(),
                this.iconField.getValue(), this.pathField.getValue());
        if (theme == null) {
            this.createButton.active = false;
            return;
        }
        this.parent.themeChanged(theme.theme);
        this.minecraft.gui.setScreen(new ThemeEditorScreen(this.parent, theme));
    }

    private void updatePathHint() {
        if (this.pathField == null || this.pathField.isFocused() || !this.pathField.getValue().isBlank()) return;
        String preview = ThemeListener.previewConfigThemePath(this.nameField == null ? "" : this.nameField.getValue());
        this.pathField.setHint(Component.literal(preview.isBlank() ? "theme.json" : preview + ".json")
                .withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
    }

    private void refreshCreateState() {
        if (this.createButton == null) return;
        String name = this.nameField.getValue().trim();
        String icon = this.iconField.getValue().trim();
        this.createButton.active = !name.isEmpty()
                && (icon.isEmpty() || net.minecraft.resources.Identifier.tryParse(icon) != null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // The parent workspace supplies the visible background beneath this modal.
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
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier,
                event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private int toLayoutMouse(double coordinate) {
        return Math.round((float) (coordinate / MaMDataConfig.get().gui_multiplier));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(toLayoutMouse(event), doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(toLayoutMouse(event), dragX / MaMDataConfig.get().gui_multiplier,
                dragY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(toLayoutMouse(event));
    }

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        if ((DIM_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, DIM_OVERLAY);
        graphics.fill(x, y, x + width, y + height, MODAL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        graphics.centeredText(this.font, this.title, x + width / 2, y + 12, TEXT_TITLE);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.create_theme.name"), x + 12, y + 28, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.theme.description"), x + 12, y + 64, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.create_theme.icon"), x + 12, y + 100, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.create_theme.path"), x + 12, y + 136, TEXT_DESCRIPTION);
    }

    private int panelWidth() {
        return Math.max(1, Math.min(330, this.layoutWidth - Math.min(32, Math.max(0, this.layoutWidth - 1))));
    }

    private int panelHeight() {
        return Math.min(205, Math.max(1, this.layoutHeight - Math.min(24, Math.max(0, this.layoutHeight - 1))));
    }

    private int panelX() {
        return this.layoutWidth / 2 - panelWidth() / 2;
    }

    private int panelY() {
        return this.layoutHeight / 2 - panelHeight() / 2;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
