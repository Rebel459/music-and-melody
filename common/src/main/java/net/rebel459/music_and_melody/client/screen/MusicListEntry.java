package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

abstract class MusicListEntry<T extends MusicListEntry<T>> extends ObjectSelectionList.Entry<T> {
    private static final int CONTENT_RIGHT_PADDING = 4;

    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    @Override
    public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.setContentBounds(left, top, Math.max(0, width - CONTENT_RIGHT_PADDING), height);
        this.renderContent(graphics, mouseX, mouseY, hovered, tickDelta);
    }

    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
    }

    protected final void setContentBounds(int x, int y, int width, int height) {
        this.contentX = x;
        this.contentY = y;
        this.contentWidth = width;
        this.contentHeight = height;
    }

    protected final int getContentX() {
        return this.contentX;
    }

    protected final int getContentY() {
        return this.contentY;
    }

    protected final int getContentWidth() {
        return this.contentWidth;
    }

    protected final int getContentHeight() {
        return this.contentHeight;
    }

    protected final int getContentRight() {
        return this.contentX + this.contentWidth;
    }

    protected final int getContentYMiddle() {
        return this.contentY + this.contentHeight / 2;
    }
}