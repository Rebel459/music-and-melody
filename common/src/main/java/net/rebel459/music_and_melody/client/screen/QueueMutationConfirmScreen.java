package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;

import java.util.List;

/** A focused confirmation before a loaded album or playlist becomes custom. */
final class QueueMutationConfirmScreen extends Screen {

    private static final int DIALOG_HEIGHT = 110;
    private final MusicPlayerScreen parent;
    private final Component message;
    private final Component confirmLabel;
    private final Runnable confirmedAction;

    QueueMutationConfirmScreen(MusicPlayerScreen parent, Component message, Runnable confirmedAction) {
        this(parent, Component.translatable("screen.music_and_melody.queue_mutation"), message,
                Component.translatable("button.music_and_melody.make_custom"), confirmedAction);
    }

    QueueMutationConfirmScreen(MusicPlayerScreen parent, Component title, Component message, Component confirmLabel, Runnable confirmedAction) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.confirmLabel = confirmLabel;
        this.confirmedAction = confirmedAction;
    }

    @Override
    protected void init() {
        this.addRenderableOnly(this::renderPopup);
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY() + DIALOG_HEIGHT - 29;
        int buttonWidth = (width - 4) / 2;
        int buttonX = x + (width - (buttonWidth * 2 + 4)) / 2;
        this.addRenderableWidget(new WorkspaceButton(buttonX, y, buttonWidth, 20, this.confirmLabel, false, button -> {
                    this.confirmedAction.run();
                    this.minecraft.gui.setScreen(this.parent);
                }));
        this.addRenderableWidget(new WorkspaceButton(buttonX + buttonWidth + 4, y, buttonWidth, 20, CommonComponents.GUI_CANCEL, false,
                button -> this.onClose()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // Keep the workspace visible beneath the confirmation instead of
        // replacing it with the normal full-screen screen background.
        this.parent.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // The workspace is rendered manually beneath this modal. Suppressing
        // Screen's own background also avoids requesting a second blur pass
        // when another UI layer is already blurred this frame.
    }

    private void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY();
        graphics.fill(0, 0, this.width, this.height, 0x5C000000);
        graphics.fill(x, y, x + width, y + DIALOG_HEIGHT, 0xFF151C2A);
        graphics.fill(x, y, x + width, y + 1, 0xFF78A6FF);
        graphics.fill(x, y + DIALOG_HEIGHT - 1, x + width, y + DIALOG_HEIGHT, 0xFF3B4963);
        graphics.fill(x, y, x + 1, y + DIALOG_HEIGHT, 0xFF3B4963);
        graphics.fill(x + width - 1, y, x + width, y + DIALOG_HEIGHT, 0xFF3B4963);
        graphics.centeredText(this.font, this.title, x + width / 2, y + 12, 0xFFFFFFFF);
        List<FormattedCharSequence> lines = this.font.split(this.message, width - 24);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            graphics.text(this.font, lines.get(i), x + 12, y + 31 + i * this.font.lineHeight, 0xFFB7C1D5);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseDragged(event, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseReleased(event);
        return true;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        int width = dialogWidth();
        int x = dialogX();
        int y = dialogY();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + DIALOG_HEIGHT;
    }

    private int dialogWidth() {
        return Math.min(310, this.width - 32);
    }

    private int dialogX() {
        return this.width / 2 - dialogWidth() / 2;
    }

    private int dialogY() {
        return this.height / 2 - DIALOG_HEIGHT / 2;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
