package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The flat, texture-free action treatment used throughout the compact music
 * workspace. A selected button uses the blue fill; ordinary actions keep the
 * same shape without implying a persistent state.
 */
public final class WorkspaceButton extends Button {

    private static final int ACCENT = 0xFF78A6FF;
    private static final int MUTED = 0xFF9DA9BF;
    private static final int ROW_HOVER = 0xAA344765;

    private final boolean selected;

    public WorkspaceButton(int x, int y, int width, int height, Component message, boolean selected, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.selected = selected;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        boolean hovered = this.active && mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();
        int background = !this.active ? 0x66303A4D : this.selected ? 0xCC365985 : hovered ? ROW_HOVER : 0x66303A4D;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
        if (this.selected) graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.getHeight(), ACCENT);
        int textColor = this.active ? 0xFFE8EDF6 : MUTED;
        graphics.centeredText(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2, textColor);
    }
}
