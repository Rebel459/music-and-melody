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
import net.rebel459.music_and_melody.client.PlaylistHelper;

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
        MusicScreenHelper.requestStats(this.minecraft);
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

    public void onStatsUpdated() {
        if (this.list != null) {
            this.list.refresh(this.album);
        }
    }

    private static class DetailList extends ObjectSelectionList<DetailEntry> {

        private final AlbumScreen screen;

        DetailList(AlbumScreen screen, Minecraft minecraft, int width, int height, Album album) {
            super(minecraft, width, height, 32, 24);
            this.screen = screen;
            this.centerListVertically = false;
            refresh(album);
        }

        private void refresh(Album album) {
            this.clearEntries();
            addTracks(album);
            addDiscs(album);
        }

        private void addTracks(Album album) {
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            if (album.tracks.isEmpty()) {
                this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.empty").withStyle(ChatFormatting.GRAY), 0xFFAAAAAA));
            } else {
                album.tracks.stream()
                        .map(song -> new DetailEntry(this.screen, this.minecraft, album, song, MusicScreenHelper.trackName(album, song).copy().withStyle(ChatFormatting.GRAY), 0xFFAAAAAA))
                        .forEach(this::addEntry);
            }
        }

        private void addDiscs(Album album) {
            if (album.discs.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            album.discs.stream()
                    .map(disc -> {
                        var id = MusicScreenHelper.albumEntryId(album, disc);
                        var name = MusicScreenHelper.discName(id);
                        boolean unlocked = MusicScreenHelper.isDiscUnlocked(this.minecraft, id);
                        return new DetailEntry(this.minecraft, MusicScreenHelper.discSoundId(this.minecraft, id), name.copy().withStyle(ChatFormatting.GRAY), unlocked);
                    })
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

    private static class DetailEntry extends ObjectSelectionList.Entry<DetailEntry> {

        private static final int BUTTON_WIDTH = 54;
        private static final int BUTTON_GAP = 4;
        private final Minecraft minecraft;
        private final AlbumScreen parent;
        private final Album album;
        private final String song;
        private final net.minecraft.resources.Identifier playableSong;
        private final Component text;
        private final int color;
        private final Button playButton;
        private final Button queueButton;
        private final Button toggleButton;
        private final Component statusText;
        private final int statusColor;
        private final boolean playable;

        DetailEntry(Minecraft minecraft, Component text, int color) {
            this(null, minecraft, null, null, null, text, color, true, null, 0xFFFFFFFF);
        }

        DetailEntry(Minecraft minecraft, net.minecraft.resources.Identifier playableSong, Component text, boolean unlocked) {
            this(
                    null,
                    minecraft,
                    null,
                    null,
                    playableSong,
                    text,
                    0xFFAAAAAA,
                    unlocked,
                    Component.translatable(unlocked ? "screen.music_and_melody.album_details.unlocked" : "screen.music_and_melody.album_details.locked"),
                    unlocked ? 0xFFFFFFFF : 0xFF888888
            );
        }

        DetailEntry(AlbumScreen parent, Minecraft minecraft, Album album, String song, Component text, int color) {
            this(parent, minecraft, album, song, album.trackId(song), text, color, true, null, 0xFFFFFFFF);
        }

        DetailEntry(AlbumScreen parent, Minecraft minecraft, Album album, String song, net.minecraft.resources.Identifier playableSong, Component text, int color, boolean playable, Component statusText, int statusColor) {
            this.parent = parent;
            this.minecraft = minecraft;
            this.album = album;
            this.song = song;
            this.playableSong = playableSong;
            this.text = text;
            this.color = color;
            this.playable = playable;
            this.statusText = statusText;
            this.statusColor = statusColor;
            this.playButton = playableSong == null ? null : Button.builder(playMessage(playableSong), button -> {
                playTrack();
                button.setMessage(playMessage(playableSong));
            }).size(BUTTON_WIDTH, 20).build();
            this.queueButton = playableSong == null ? null : Button.builder(Component.translatable("button.music_and_melody.queue"), button -> {
                PlaylistHelper.add(playableSong);
                button.active = false;
            }).size(BUTTON_WIDTH, 20).build();
            this.toggleButton = album == null ? null : Button.builder(toggleMessage(album, song), button -> {
                toggleTrack();
                button.setMessage(toggleMessage(album, song));
            }).size(BUTTON_WIDTH, 20).build();
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int buttonCount = this.playButton == null ? 0 : 3;
            int buttonWidth = buttonCount == 0 ? 0 : BUTTON_WIDTH * buttonCount + BUTTON_GAP * (buttonCount - 1) + 8;
            FormattedCharSequence line = this.minecraft.font.split(this.text, this.getContentWidth() - buttonWidth).getFirst();
            graphics.text(this.minecraft.font, line, this.getContentX(), this.getContentY() + 4, this.color);
            if (this.playButton != null) {
                int buttonY = this.getContentYMiddle() - 10;
                int rightX = this.getContentRight() - BUTTON_WIDTH;
                this.playButton.setMessage(playMessage(this.playableSong));
                this.playButton.active = this.playable;
                this.queueButton.active = this.playable && !PlaylistHelper.isQueued(this.playableSong);
                this.playButton.setX(rightX - BUTTON_WIDTH * 2 - BUTTON_GAP * 2);
                this.playButton.setY(buttonY);
                this.queueButton.setX(rightX - BUTTON_WIDTH - BUTTON_GAP);
                this.queueButton.setY(buttonY);
                if (this.toggleButton != null) {
                    this.toggleButton.setX(rightX);
                    this.toggleButton.setY(buttonY);
                }
                this.playButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                this.queueButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                if (this.toggleButton != null) {
                    this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                } else if (this.statusText != null) {
                    FormattedCharSequence status = this.minecraft.font.split(this.statusText, BUTTON_WIDTH + 12).getFirst();
                    graphics.text(this.minecraft.font, status, rightX + (BUTTON_WIDTH - this.minecraft.font.width(status)) / 2, this.getContentY() + 4, this.statusColor);
                }
            }
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            return this.playButton != null && this.playButton.mouseClicked(event, doubleClick)
                    || this.queueButton != null && this.queueButton.mouseClicked(event, doubleClick)
                    || this.toggleButton != null && this.toggleButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }

        private void playTrack() {
            if (PlaylistHelper.isPlaying(this.playableSong)) {
                PlaylistHelper.stop();
            } else {
                PlaylistHelper.play(this.playableSong, false);
            }
        }

        private void toggleTrack() {
            this.album.setTrackEnabled(this.song, !this.album.isTrackEnabled(this.song));
            this.parent.markReloadPending();
        }

        private static Component playMessage(net.minecraft.resources.Identifier song) {
            return Component.translatable(PlaylistHelper.isPlaying(song) ? "button.music_and_melody.stop" : "button.music_and_melody.play");
        }

        private static Component toggleMessage(Album album, String song) {
            return CommonComponents.optionStatus(album.isTrackEnabled(song));
        }
    }
}
