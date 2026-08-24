package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.Comparator;
import java.util.List;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

final class ReplacePlaylistScreen extends Screen {

    private final MusicPlayerScreen parent;
    private int layoutWidth;
    private int layoutHeight;

    ReplacePlaylistScreen(MusicPlayerScreen parent) {
        super(Component.translatable("screen.music_and_melody.replace_playlist"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderDialog);
        int x = panelX();
        int y = panelY();
        this.addRenderableWidget(new PlaylistList(this.minecraft, x + 12, panelWidth() - 24, y + 32, y + panelHeight() - 34));
        this.addRenderableWidget(new WorkspaceButton(x + 12, y + panelHeight() - 28, panelWidth() - 24, 20,
                CommonComponents.GUI_CANCEL, false, ignored -> this.onClose()));
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
    protected void repositionElements() {
        calculateLayoutSize();
        this.rebuildWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {}

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
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseDragged(event, dragX / MaMDataConfig.get().gui_multiplier, dragY / MaMDataConfig.get().gui_multiplier);
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
        this.minecraft.setScreen(this.parent);
    }

    private void choose(Playlist playlist) {
        this.minecraft.setScreen(new SavePlaylistScreen(this.parent, playlist));
    }

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        centeredText(graphics, this.font, this.title, x + width / 2, y + 13, TEXT_TITLE);
    }

    private void calculateLayoutSize() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    private int toLayoutMouse(int coordinate) {
        return Math.round(coordinate / MaMDataConfig.get().gui_multiplier);
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private int panelWidth() { return Math.min(360, this.layoutWidth - 24); }
    private int panelHeight() { return Math.min(280, this.layoutHeight - 28); }
    private int panelX() { return this.layoutWidth / 2 - panelWidth() / 2; }
    private int panelY() { return this.layoutHeight / 2 - panelHeight() / 2; }

    private boolean insideDialog(double mouseX, double mouseY) {
        return mouseX >= panelX() && mouseX < panelX() + panelWidth()
                && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    private final class PlaylistList extends ObjectSelectionList<PlaylistEntry> {
        private final int panelX;
        private final int panelWidth;

        PlaylistList(Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(minecraft, panelWidth, Math.max(1, bottom - top), top, 24);
            this.panelX = panelX;
            this.panelWidth = panelWidth;
            this.setX(panelX);
            this.centerListVertically = false;
            Playlist.PLAYLISTS.stream().filter(Playlist::isCustom)
                    .sorted(Comparator.comparing(playlist -> playlist.name.getString(), String.CASE_INSENSITIVE_ORDER))
                    .map(ReplacePlaylistScreen.PlaylistEntry::new).forEach(this::addEntry);
        }

        @Override protected void extractListBackground(GuiGraphicsExtractor graphics) {}
        @Override protected void extractListSeparators(GuiGraphicsExtractor graphics) {}
        @Override public int getRowLeft() { return this.panelX; }
        @Override public int getRowWidth() { return this.panelWidth - 7; }
        @Override protected int scrollBarX() { return this.panelX + this.panelWidth - 4; }
    }

    private final class PlaylistEntry extends ObjectSelectionList.Entry<PlaylistEntry> {
        private final Playlist playlist;

        PlaylistEntry(Playlist playlist) {
            this.playlist = playlist;
        }

        @Override public Component getNarration() { return this.playlist.name; }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(getContentX(), getContentY(), getContentRight(), getContentBottom(), BUTTON_HIGHLIGHT);
            text(graphics, font, this.playlist.name, getContentX() + 4, getContentYMiddle() - font.lineHeight / 2, TEXT_TITLE);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            choose(this.playlist);
            return true;
        }
    }
}
