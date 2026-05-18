package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;

import java.util.Comparator;

public class AlbumScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.albums");
    private final Screen parent;
    private AlbumList list;
    private boolean reloadPending;

    public AlbumScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = this.addRenderableWidget(new AlbumList(this, this.minecraft, this.width, this.height - 64));
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
        if (this.reloadPending) {
            this.minecraft.reloadResourcePacks();
        }
    }

    public void markReloadPending() {
        this.reloadPending = true;
    }

    private static class AlbumList extends ObjectSelectionList<AlbumEntry> {

        private final AlbumScreen screen;

        AlbumList(AlbumScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 32, 46);
            this.screen = screen;
            this.centerListVertically = false;

            Album.ALBUMS.stream()
                    .sorted(Comparator.comparing(album -> album.name.getString()))
                    .map(album -> new AlbumEntry(this, this.screen, this.minecraft, album))
                    .forEach(this::addEntry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, this.width - 20);
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 6;
        }
    }

    private static class AlbumEntry extends ObjectSelectionList.Entry<AlbumEntry> {

        private static final int ICON_SIZE = 32;
        private static final int BUTTON_WIDTH = 64;
        private static final int BUTTON_GAP = 4;
        private final AlbumList list;
        private final AlbumScreen screen;
        private final Minecraft minecraft;
        private final Album album;
        private final Button toggleButton;
        private final Button detailsButton;

        AlbumEntry(AlbumList list, AlbumScreen screen, Minecraft minecraft, Album album) {
            this.list = list;
            this.screen = screen;
            this.minecraft = minecraft;
            this.album = album;
            this.toggleButton = Button.builder(toggleMessage(album), button -> {
                toggleAlbum();
                button.setMessage(toggleMessage(album));
            }).size(BUTTON_WIDTH, 20).build();
            this.detailsButton = Button.builder(Component.translatable("button.music_and_melody.album_details"), button ->
                    this.minecraft.setScreen(new AlbumDetailsScreen(this.screen, this.album))
            ).size(BUTTON_WIDTH, 20).build();
        }

        @Override
        public Component getNarration() {
            return Component.empty()
                    .append(this.album.name)
                    .append(CommonComponents.NARRATION_SEPARATOR)
                    .append(CommonComponents.optionStatus(this.album.isEnabled()));
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int iconX = this.getContentX();
            int iconY = this.getContentY() + 3;
            int textX = iconX + ICON_SIZE + 6;
            int textY = this.getContentY() + 2;
            int buttonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
            int maxTextWidth = this.getContentWidth() - ICON_SIZE - buttonsWidth - 26;

            FormattedCharSequence name = this.minecraft.font.split(this.album.name, maxTextWidth).getFirst();
            String id = this.minecraft.font.plainSubstrByWidth(this.album.album.toString(), maxTextWidth);
            String details = this.minecraft.font.plainSubstrByWidth(details(), maxTextWidth);

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.album.icon, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            graphics.text(this.minecraft.font, name, textX, textY, 0xFFFFFFFF);
            graphics.text(this.minecraft.font, Component.literal(id).withStyle(ChatFormatting.GRAY), textX, textY + 11, 0xFFAAAAAA);
            graphics.text(this.minecraft.font, details, textX, textY + 22, 0xFFAAAAAA);

            int buttonX = this.getContentRight() - buttonsWidth;
            this.detailsButton.setX(buttonX);
            this.detailsButton.setY(this.getContentYMiddle() - 10);
            this.toggleButton.setX(buttonX + BUTTON_WIDTH + BUTTON_GAP);
            this.toggleButton.setY(this.getContentYMiddle() - 10);
            this.detailsButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.detailsButton.mouseClicked(event, doubleClick) || this.toggleButton.mouseClicked(event, doubleClick) || super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation()) {
                this.toggleAlbum();
                this.toggleButton.setMessage(toggleMessage(this.album));
                return true;
            }
            return super.keyPressed(event);
        }

        private void toggleAlbum() {
            this.album.setEnabled(!this.album.isEnabled());
            this.screen.markReloadPending();
        }

        private String details() {
            String composers = count(this.album.composers, "composer", "composers");
            String tracks = count(this.album.tracks.size(), "track", "tracks");
            String discs = count(this.album.discs.size(), "disc", "discs");
            String music = this.album.discs.isEmpty() ? tracks : tracks + " | " + discs;
            if (this.album.composers == 0) return music;
            return composers + " | " + music;
        }

        private static String count(int count, String singular, String plural) {
            return count + " " + (count == 1 ? singular : plural);
        }

        private static Component toggleMessage(Album album) {
            return CommonComponents.optionStatus(album.isEnabled());
        }
    }
}
