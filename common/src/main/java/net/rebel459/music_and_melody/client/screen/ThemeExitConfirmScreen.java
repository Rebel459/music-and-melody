package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.List;
import java.util.function.Consumer;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

final class ThemeExitConfirmScreen extends Screen {

    private static final int DIALOG_HEIGHT = 118;
    private final Screen parent;
    private final Consumer<Boolean> finishAction;
    private final Component warning;
    private int layoutWidth;
    private int layoutHeight;

    ThemeExitConfirmScreen(ThemeEditorScreen parent) {
        this(parent,
                Component.translatable("screen.music_and_melody.theme.unsaved"),
                Component.translatable("screen.music_and_melody.theme.unsaved_warning"),
                parent::finish);
    }

    ThemeExitConfirmScreen(Screen parent, Component title, Component warning, Consumer<Boolean> finishAction) {
        super(title);
        this.parent = parent;
        this.warning = warning;
        this.finishAction = finishAction;
    }

    @Override
    protected void init() {
        calculateLayout();
        this.addRenderableOnly(this::renderPopup);
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY() + DIALOG_HEIGHT - 29;
        int buttonWidth = (width - 8) / 3;
        int buttonX = x + (width - buttonWidth * 3 - 8) / 2;
        this.addRenderableWidget(new WorkspaceButton(buttonX, y, buttonWidth, 20,
                Component.translatable("button.music_and_melody.save"), false, ignored -> {
                    this.finishAction.accept(true);
                }));
        this.addRenderableWidget(new WorkspaceButton(buttonX + buttonWidth + 4, y, buttonWidth, 20,
                Component.translatable("button.music_and_melody.dont_save"), false, ignored -> {
                    this.finishAction.accept(false);
                }));
        this.addRenderableWidget(new WorkspaceButton(buttonX + (buttonWidth + 4) * 2, y, buttonWidth, 20,
                CommonComponents.GUI_CANCEL, false, ignored -> this.onClose()));
    }

    private void calculateLayout() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    @Override
    protected void repositionElements() {
        calculateLayout();
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    }

    private void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY();
        if ((POPUP_OVERLAY >>> 24) != 0) graphics.fill(0, 0, layoutWidth, layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + DIALOG_HEIGHT, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + DIALOG_HEIGHT - 1, x + width, y + DIALOG_HEIGHT, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + DIALOG_HEIGHT, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + DIALOG_HEIGHT, POPUP_OUTLINE);
        ThemeHelper.centeredText(graphics, this.font, this.title, x + width / 2, y + 12, TEXT_TITLE);
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                this.warning, width - 24);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            ThemeHelper.text(graphics, this.font, lines.get(i), x + 12, y + 32 + i * this.font.lineHeight, TEXT_DESCRIPTION);
        }
    }

    private int dialogWidth() {
        return Math.max(1, Math.min(340, layoutWidth - Math.min(32, Math.max(0, layoutWidth - 1))));
    }

    private int dialogX() {
        return (layoutWidth - dialogWidth()) / 2;
    }

    private int dialogY() {
        return (layoutHeight - Math.min(DIALOG_HEIGHT, layoutHeight)) / 2;
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
        if (!inside(toLayoutMouse(event).x(), toLayoutMouse(event).y())) return true;
        super.mouseClicked(toLayoutMouse(event), doubleClick);
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private boolean inside(double x, double y) {
        return x >= dialogX() && x < dialogX() + dialogWidth() && y >= dialogY() && y < dialogY() + DIALOG_HEIGHT;
    }
}
