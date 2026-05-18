package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;

public class AlbumDetailsScreen extends Screen {

    private final AlbumScreen parent;
    private final Album album;
    private DetailList list;

    public AlbumDetailsScreen(AlbumScreen parent, Album album) {
        super(album.name);
        this.parent = parent;
        this.album = album;
    }

    @Override
    protected void init() {
        this.list = this.addRenderableWidget(new DetailList(this.parent, this.minecraft, this.width, this.height - 64, this.album));
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        int titleX = this.width / 2 - 100;
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.album.icon, titleX, 4, 0.0F, 0.0F, 24, 24, 24, 24);
        graphics.text(this.font, this.album.name, titleX + 30, 6, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal(this.album.album.toString()).withStyle(ChatFormatting.GRAY), titleX + 30, 17, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static class DetailList extends ObjectSelectionList<DetailEntry> {

        private final AlbumScreen screen;

        DetailList(AlbumScreen screen, Minecraft minecraft, int width, int height, Album album) {
            super(minecraft, width, height, 32, 22);
            this.screen = screen;
            this.centerListVertically = false;
            addTracks(album);
        }

        private void addTracks(Album album) {
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            if (album.songs.isEmpty()) {
                this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.empty").withStyle(ChatFormatting.GRAY), 0xFFAAAAAA));
                return;
            }
            album.songs.stream()
                    .map(song -> new DetailEntry(this.screen, this.minecraft, album, song, trackName(album, song).copy().withStyle(ChatFormatting.GRAY), 0xFFAAAAAA))
                    .forEach(this::addEntry);
        }

        private Component trackName(Album album, String song) {
            String pathKey = song.replace('/', '.');
            String key = album.album.getNamespace().equals("minecraft") ? pathKey : album.album.getNamespace() + "." + pathKey;
            return Component.translatableWithFallback(key, song);
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

    private static class DetailEntry extends ObjectSelectionList.Entry<DetailEntry> {

        private final Minecraft minecraft;
        private final AlbumScreen parent;
        private final Album album;
        private final String song;
        private final Component text;
        private final int color;
        private final Button toggleButton;

        DetailEntry(Minecraft minecraft, Component text, int color) {
            this(null, minecraft, null, null, text, color);
        }

        DetailEntry(AlbumScreen parent, Minecraft minecraft, Album album, String song, Component text, int color) {
            this.parent = parent;
            this.minecraft = minecraft;
            this.album = album;
            this.song = song;
            this.text = text;
            this.color = color;
            this.toggleButton = album == null ? null : Button.builder(toggleMessage(album, song), button -> {
                toggleTrack();
                button.setMessage(toggleMessage(album, song));
            }).size(64, 20).build();
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int buttonWidth = this.toggleButton == null ? 0 : 72;
            FormattedCharSequence line = this.minecraft.font.split(this.text, this.getContentWidth() - buttonWidth).getFirst();
            graphics.text(this.minecraft.font, line, this.getContentX(), this.getContentY() + 4, this.color);
            if (this.toggleButton != null) {
                this.toggleButton.setX(this.getContentRight() - 64);
                this.toggleButton.setY(this.getContentYMiddle() - 10);
                this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            return this.toggleButton != null && this.toggleButton.mouseClicked(event, doubleClick) || super.mouseClicked(event, doubleClick);
        }

        private void toggleTrack() {
            this.album.setTrackEnabled(this.song, !this.album.isTrackEnabled(this.song));
            this.parent.markReloadPending();
        }

        private static Component toggleMessage(Album album, String song) {
            return CommonComponents.optionStatus(album.isTrackEnabled(song));
        }
    }
}
