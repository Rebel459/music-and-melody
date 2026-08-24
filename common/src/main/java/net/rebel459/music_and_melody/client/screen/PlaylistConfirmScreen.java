package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

import java.util.List;

final class PlaylistConfirmScreen extends Screen {

    private static final int DIALOG_HEIGHT = 110;
    private static final int DIALOG_MAX_WIDTH = 310;
    private static final int DIALOG_SIDE_SPACE = 32;
    private final MusicPlayerScreen parent;
    private final Component message;
    private final Component confirmLabel;
    private final Runnable confirmedAction;
    private int layoutWidth;
    private int layoutHeight;

    PlaylistConfirmScreen(MusicPlayerScreen parent, Component title, Component message, Component confirmLabel, Runnable confirmedAction) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.confirmLabel = confirmLabel;
        this.confirmedAction = confirmedAction;
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderPopup);
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY() + DIALOG_HEIGHT - 29;
        int buttonWidth = (width - 4) / 2;
        int buttonX = x + (width - (buttonWidth * 2 + 4)) / 2;
        this.addRenderableWidget(new WorkspaceButton(buttonX, y, buttonWidth, 20, this.confirmLabel, false, button -> {
                    this.confirmedAction.run();
                    this.minecraft.setScreen(this.parent);
                }));
        this.addRenderableWidget(new WorkspaceButton(buttonX + buttonWidth + 4, y, buttonWidth, 20, CommonComponents.GUI_CANCEL, false,
                button -> this.onClose()));
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

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {}

    private void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY();
        if ((POPUP_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + DIALOG_HEIGHT, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + DIALOG_HEIGHT - 1, x + width, y + DIALOG_HEIGHT, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + DIALOG_HEIGHT, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + DIALOG_HEIGHT, POPUP_OUTLINE);
        ThemeHelper.centeredText(graphics, this.font, this.title, x + width / 2, y + 12, TEXT_TITLE);
        List<FormattedCharSequence> lines = this.font.split(this.message, width - 24);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            ThemeHelper.text(graphics, this.font, lines.get(i), x + 12, y + 31 + i * this.font.lineHeight, TEXT_DESCRIPTION);
        }
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

    private int toLayoutMouse(double mouse) {
        return Math.round((float) (mouse / MaMDataConfig.get().gui_multiplier));
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + DIALOG_HEIGHT;
    }

    private int dialogWidth() {
        int width = Math.min(DIALOG_MAX_WIDTH, Math.max(DIALOG_SIDE_SPACE, this.layoutWidth - DIALOG_SIDE_SPACE));
        if ((width & 1) != (this.layoutWidth & 1) && width > DIALOG_SIDE_SPACE) width--;
        return width;
    }

    private int dialogX() {
        return (this.layoutWidth - dialogWidth()) / 2;
    }

    private int dialogY() {
        return this.layoutHeight / 2 - DIALOG_HEIGHT / 2;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
