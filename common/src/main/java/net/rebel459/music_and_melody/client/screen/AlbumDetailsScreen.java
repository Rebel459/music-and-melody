package net.rebel459.music_and_melody.client.screen;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.MusicDiscHelper;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.client.PlaylistHelper;
import net.rebel459.music_and_melody.config.ConfigAlbum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlbumDetailsScreen extends Screen {

    private final AlbumScreen parent;
    private final Album album;
    private final Playlist playlist;
    private DetailList list;
    private Button actionButton;
    private Button loadButton;
    private boolean deletePending;

    public AlbumDetailsScreen(AlbumScreen parent, Album album) {
        super(album.name);
        this.parent = parent;
        this.album = album;
        this.playlist = null;
    }

    public AlbumDetailsScreen(AlbumScreen parent, Playlist playlist) {
        super(playlist.name);
        this.parent = parent;
        this.album = null;
        this.playlist = playlist;
    }

    @Override
    protected void init() {
        MusicDiscHelper.requestStats(this.minecraft);
        this.list = this.addRenderableWidget(new DetailList(this.parent, this.minecraft, this.width, this.height - 64, this.album, this.playlist));
        int buttonY = this.height - 27;
        int buttonCount = 3;
        int buttonWidth = 100;
        int rowWidth = buttonWidth * buttonCount + 4 * (buttonCount - 1);
        int rowX = this.width / 2 - rowWidth / 2;

        this.actionButton = this.addRenderableWidget(Button.builder(actionMessage(), button -> {
                    if (this.playlist != null && this.playlist.isCustom()) {
                        this.deletePending = !this.deletePending;
                        button.setMessage(actionMessage());
                    } else {
                        setFavourite(!isFavourite());
                        button.setMessage(actionMessage());
                        this.parent.refreshList();
                    }
                })
                .bounds(rowX, buttonY, buttonWidth, 20)
                .build());
        this.loadButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.load"), button -> {
                        PlaylistHelper.clear();
                        PlaylistHelper.addAll(queueSongs(this.minecraft));
                    })
                .bounds(rowX + buttonWidth + 4, buttonY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + (buttonWidth + 4) * 2, buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        int titleX = this.width / 2 - 100;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon(), titleX, 3, 0.0F, 0.0F, 24, 24, 24, 24);
        graphics.text(this.font, this.title, titleX + 30, 5, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal(id().toString()).withStyle(ChatFormatting.GRAY), titleX + 30, 16, 0xFFAAAAAA);
        if (this.actionButton != null) this.actionButton.setMessage(actionMessage());
        if (this.loadButton != null) this.loadButton.active = !queueSongs(this.minecraft).isEmpty();
    }

    @Override
    public void onClose() {
        if (this.deletePending && this.playlist != null && this.playlist.deleteCustom()) {
            this.parent.refreshList();
        }
        this.minecraft.setScreen(this.parent);
    }

    public void onStatsUpdated() {
        if (this.list != null) {
            this.list.refresh(this.album, this.playlist);
        }
    }

    private Identifier id() {
        return this.album != null ? this.album.album : this.playlist.playlist;
    }

    private Identifier icon() {
        return this.album != null ? this.album.icon : this.playlist.icon;
    }

    private boolean isFavourite() {
        return this.album != null ? this.album.isFavourite() : this.playlist.isFavourite();
    }

    private void setFavourite(boolean favourite) {
        if (this.album != null) {
            this.album.setFavourite(favourite);
        } else {
            this.playlist.setFavourite(favourite);
        }
    }

    private Component favouriteMessage() {
        return Component.translatable(isFavourite() ? "button.music_and_melody.unfavourite" : "button.music_and_melody.favourite");
    }

    private Component actionMessage() {
        if (this.playlist != null && this.playlist.isCustom()) {
            return Component.translatable(this.deletePending ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
        }
        return favouriteMessage();
    }

    public static List<Identifier> queueSongs(Album album, Minecraft minecraft) {
        List<Identifier> songs = new ArrayList<>();
        album.tracks.stream()
                .map(album::trackId)
                .forEach(songs::add);
        album.discs.stream()
                .map(disc -> MusicDiscHelper.albumEntryId(album, disc))
                .map(disc -> MusicDiscHelper.discSoundId(minecraft, disc))
                .forEach(songs::add);
        return songs;
    }

    private List<Identifier> queueSongs(Minecraft minecraft) {
        List<Identifier> songs = new ArrayList<>();
        if (album != null) {
            return queueSongs(this.album, minecraft);
        }

        songs.addAll(this.playlist.tracks);
        this.playlist.discs.stream()
                .map(disc -> MusicDiscHelper.discSoundId(minecraft, disc))
                .forEach(songs::add);
        return songs;
    }

    private static class DetailList extends ObjectSelectionList<DetailEntry> {

        private final AlbumScreen screen;

        DetailList(AlbumScreen screen, Minecraft minecraft, int width, int height, Album album, Playlist playlist) {
            super(minecraft, width, height, 32, 24);
            this.screen = screen;
            this.centerListVertically = false;
            refresh(album, playlist);
        }

        private void refresh(Album album, Playlist playlist) {
            this.clearEntries();
            if (album != null) {
                addAlbumTracks(album);
                addAlbumDiscs(album);
            } else {
                addPlaylistTracks(playlist);
                addPlaylistDiscs(playlist);
            }
        }

        private void addAlbumTracks(Album album) {
            if (album.tracks.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            album.tracks.stream()
                    .map(song -> new DetailEntry(this.screen, this.minecraft, album, song, MusicScreenHelper.trackName(album, song).copy().withStyle(ChatFormatting.GRAY), 0xFFAAAAAA, trackStatus(album, song)))
                    .forEach(this::addEntry);
        }

        private static Component trackStatus(Album album, String song) {
            if (album.album.equals(ConfigAlbum.ALBUM_ID)) return Component.translatable("screen.music_and_melody.album_details.custom");
            if (album.isTrackForcedEnabled(song)) return Component.translatable("screen.music_and_melody.album_details.enabled");
            return null;
        }

        private void addAlbumDiscs(Album album) {
            if (album.discs.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            album.discs.stream()
                    .map(disc -> {
                        Identifier id = MusicDiscHelper.albumEntryId(album, disc);
                        Component name = MusicDiscHelper.discName(id);
                        boolean unlocked = album.isDiscForcedUnlocked(disc) || MusicDiscHelper.isDiscUnlocked(this.minecraft, id);
                        return new DetailEntry(this.minecraft, MusicDiscHelper.discSoundId(this.minecraft, id), name.copy().withStyle(ChatFormatting.GRAY), unlocked);
                    })
                    .forEach(this::addEntry);
        }

        private void addPlaylistTracks(Playlist playlist) {
            if (playlist.tracks.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            playlist.tracks.stream()
                    .map(track -> {
                        Component name = MusicScreenHelper.trackName(track).copy().withStyle(ChatFormatting.GRAY);
                        AlbumTrack albumTrack = findAlbumTrack(track);
                        if (albumTrack != null) {
                            return new DetailEntry(this.screen, this.minecraft, albumTrack.album(), albumTrack.song(), name, 0xFFAAAAAA, trackStatus(albumTrack.album(), albumTrack.song()));
                        }
                        return new DetailEntry(this.minecraft, track, name);
                    })
                    .forEach(this::addEntry);
        }

        private static AlbumTrack findAlbumTrack(Identifier track) {
            for (Album album : Album.ALBUMS) {
                for (String song : album.tracks) {
                    if (album.trackId(song).equals(track)) return new AlbumTrack(album, song);
                }
            }
            return null;
        }

        private void addPlaylistDiscs(Playlist playlist) {
            if (playlist.discs.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            playlist.discs.stream()
                    .map(disc -> {
                        Component name = MusicDiscHelper.discName(disc);
                        boolean unlocked = MusicDiscHelper.isDiscUnlocked(this.minecraft, disc);
                        return new DetailEntry(this.minecraft, MusicDiscHelper.discSoundId(this.minecraft, disc), name.copy().withStyle(ChatFormatting.GRAY), unlocked);
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

        private record AlbumTrack(Album album, String song) {}
    }

    private static class DetailEntry extends ObjectSelectionList.Entry<DetailEntry> {

        private static final int BUTTON_WIDTH = 54;
        private static final int BUTTON_GAP = 4;
        private final Minecraft minecraft;
        private final AlbumScreen parent;
        private final Album album;
        private final String song;
        private final Identifier playableSong;
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

        DetailEntry(Minecraft minecraft, Identifier playableSong, Component text, boolean unlocked) {
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

        DetailEntry(Minecraft minecraft, Identifier playableSong, Component text) {
            this(null, minecraft, null, null, playableSong, text, 0xFFAAAAAA, true, null, 0xFFFFFFFF);
        }

        DetailEntry(AlbumScreen parent, Minecraft minecraft, Album album, String song, Component text, int color, Component forcedStatus) {
            this(
                    parent,
                    minecraft,
                    forcedStatus != null ? null : album,
                    song,
                    album.trackId(song),
                    text,
                    color,
                    true,
                    forcedStatus,
                    0xFFFFFFFF
            );
        }

        DetailEntry(AlbumScreen parent, Minecraft minecraft, Album album, String song, Identifier playableSong, Component text, int color, boolean playable, Component statusText, int statusColor) {
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
            int buttonCount = this.playButton == null ? 0 : this.toggleButton == null && this.statusText == null ? 2 : 3;
            int buttonWidth = buttonCount == 0 ? 0 : BUTTON_WIDTH * buttonCount + BUTTON_GAP * (buttonCount - 1) + 8;
            FormattedCharSequence line = this.minecraft.font.split(this.text, this.getContentWidth() - buttonWidth).getFirst();
            graphics.text(this.minecraft.font, line, this.getContentX() + 1, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, this.color);
            if (this.playButton != null) {
                int buttonY = this.getContentYMiddle() - 10;
                int rightX = this.getContentRight() - BUTTON_WIDTH;
                this.playButton.setMessage(playMessage(this.playableSong));
                this.playButton.active = this.playable;
                this.queueButton.active = this.playable && !PlaylistHelper.isQueued(this.playableSong);
                this.playButton.setX(rightX - BUTTON_WIDTH * (buttonCount - 1) - BUTTON_GAP * (buttonCount - 1));
                this.playButton.setY(buttonY);
                this.queueButton.setX(this.playButton.getX() + BUTTON_WIDTH + BUTTON_GAP);
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
                    graphics.text(this.minecraft.font, status, rightX + (BUTTON_WIDTH - this.minecraft.font.width(status)) / 2, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, this.statusColor);
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

        private static Component playMessage(Identifier song) {
            return Component.translatable(PlaylistHelper.isPlaying(song) ? "button.music_and_melody.stop" : "button.music_and_melody.play");
        }

        private static Component toggleMessage(Album album, String song) {
            return CommonComponents.optionStatus(album.isTrackEnabled(song));
        }
    }
}
