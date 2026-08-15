package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.Event;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

/** Creates a config-backed event without leaving the compact workspace. */
final class CreateEventScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.create_event");

    private final MusicPlayerScreen parent;
    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox iconField;
    private EditBox pathField;
    private WorkspaceButton createButton;
    private int layoutWidth;
    private int layoutHeight;

    CreateEventScreen(MusicPlayerScreen parent) {
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

        this.nameField = field(Component.translatable("screen.music_and_melody.create_event.name"), fieldX, y + 40, fieldWidth);
        this.nameField.setMaxLength(80);
        this.nameField.setResponder(value -> {
            updatePathHint();
            refreshCreateState();
        });

        this.descriptionField = field(Component.translatable("screen.music_and_melody.create_event.description"), fieldX, y + 76, fieldWidth);
        this.iconField = field(Component.translatable("screen.music_and_melody.event_editor.icon"), fieldX, y + 112, fieldWidth);
        this.iconField.setHint(Component.literal(Event.DEFAULT_ICON.toString()).withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        this.iconField.setResponder(value -> refreshCreateState());

        this.pathField = field(Component.translatable("screen.music_and_melody.create_event.path"), fieldX, y + 148, fieldWidth);
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, mouseX, mouseY, tickDelta);
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // The parent workspace supplies the visible background beneath this modal.
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
        graphics.centeredText(this.font, this.title, x + width / 2, y + 13, TEXT_TITLE);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.name"), x + 12, y + 28, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.description"), x + 12, y + 64, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.event_editor.icon"), x + 12, y + 100, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.create_event.path"), x + 12, y + 136, TEXT_DESCRIPTION);
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

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void create() {
        Event.Source source = Event.createConfigSource(this.nameField.getValue(), this.descriptionField.getValue(),
                this.iconField.getValue(), this.pathField.getValue());
        if (source != null) this.minecraft.gui.setScreen(new EventScreen(this.parent, source.id));
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

    private int panelWidth() {
        return Math.min(360, this.layoutWidth - 24);
    }

    private int panelHeight() {
        return Math.min(224, this.layoutHeight - 28);
    }

    private int panelX() {
        return this.layoutWidth / 2 - panelWidth() / 2;
    }

    private int panelY() {
        return this.layoutHeight / 2 - panelHeight() / 2;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        return mouseX >= panelX() && mouseX < panelX() + panelWidth()
                && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }
}
