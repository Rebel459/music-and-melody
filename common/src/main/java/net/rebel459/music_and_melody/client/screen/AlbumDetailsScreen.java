package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.ConfigAlbum;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailsScreen extends Screen {

    private final AlbumScreen parent;
    private final Album album;
    private final Playlist playlist;
    private DetailList list;
    private Button loadButton;

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
        int rowWidth = Math.min(AlbumScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
        int buttonWidth = (rowWidth - 4) / 2;
        int rowX = this.width / 2 - rowWidth / 2;

        this.loadButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.load"), button -> {
                        PlaylistHelper.clear();
                        PlaylistHelper.addAll(queueSongs(this.minecraft));
                    })
                .bounds(rowX, buttonY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + buttonWidth + 4, buttonY, buttonWidth, 20)
                .build());
        MusicScreenHelper.addSocialButtons(this);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);
        int maxTextWidth = Math.max(1, this.width - 60);
        FormattedCharSequence title = this.font.split(this.title, maxTextWidth).getFirst();
        String id = this.font.plainSubstrByWidth(id().toString(), maxTextWidth);
        int textWidth = Math.max(this.font.width(title), this.font.width(id));
        int titleX = this.width / 2 - (24 + 6 + textWidth) / 2;
        graphics.blit(MusicScreenHelper.albumIcon(this.minecraft, icon()), titleX, 3, 0.0F, 0.0F, 24, 24, 24, 24);
        graphics.drawString(this.font, title, titleX + 30, 5, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal(id).withStyle(ChatFormatting.GRAY), titleX + 30, 16, 0xFFAAAAAA);
        if (this.loadButton != null) this.loadButton.active = !queueSongs(this.minecraft).isEmpty();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    public void onStatsUpdated() {
        if (this.list != null) {
            this.list.refresh(this.album, this.playlist);
        }
    }

    private ResourceLocation id() {
        return this.album != null ? this.album.album : this.playlist.playlist;
    }

    private ResourceLocation icon() {
        return this.album != null ? this.album.icon : this.playlist.icon;
    }

    public static List<SafeIdentifier> queueSongs(Album album, Minecraft minecraft) {
        List<SafeIdentifier> songs = new ArrayList<>();
        album.tracks.stream()
                .map(album::trackId)
                .forEach(songs::add);
        album.discs.stream()
                .map(disc -> MusicDiscHelper.albumEntryId(album, disc))
                .map(disc -> MusicDiscHelper.discSoundId(minecraft, disc))
                .forEach(id -> songs.add(SafeIdentifier.convert(id)));
        return songs;
    }

    private List<SafeIdentifier> queueSongs(Minecraft minecraft) {
        List<SafeIdentifier> songs = new ArrayList<>();
        if (album != null) {
            return queueSongs(this.album, minecraft);
        }

        songs.addAll(this.playlist.tracks);
        this.playlist.discs.stream()
                .map(disc -> MusicDiscHelper.discSoundId(minecraft, disc))
                .forEach(id -> songs.add(SafeIdentifier.convert(id)));
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
            if (album.album.equals(ConfigAlbum.ALBUM_ID)) return Component.translatable("screen.music_and_melody.album_details.config");
            if (album.isTrackForcedEnabled(song)) return Component.translatable("screen.music_and_melody.album_details.enabled");
            return null;
        }

        private void addAlbumDiscs(Album album) {
            if (album.discs.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs").withStyle(ChatFormatting.BOLD), 0xFFFFFFFF));
            album.discs.stream()
                    .map(disc -> {
                        ResourceLocation id = MusicDiscHelper.albumEntryId(album, disc);
                        Component name = MusicDiscHelper.discName(id);
                        boolean unlocked = album.isDiscForcedUnlocked(disc) || MusicDiscHelper.isDiscUnlocked(this.minecraft, id);
                        return new DetailEntry(this.minecraft, SafeIdentifier.convert(MusicDiscHelper.discSoundId(this.minecraft, id)), name.copy().withStyle(ChatFormatting.GRAY), unlocked);
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

        private static AlbumTrack findAlbumTrack(SafeIdentifier track) {
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
                        return new DetailEntry(this.minecraft, SafeIdentifier.convert(MusicDiscHelper.discSoundId(this.minecraft, disc)), name.copy().withStyle(ChatFormatting.GRAY), unlocked);
                    })
                    .forEach(this::addEntry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowRight() + 6;
        }

        private record AlbumTrack(Album album, String song) {}
    }

    private static class DetailEntry extends MusicListEntry<DetailEntry> {

        private static final int BUTTON_WIDTH = IconButton.SIZE;
        private static final int BUTTON_GAP = 4;
        private static final int STATUS_WIDTH = 54;
        private final Minecraft minecraft;
        private final AlbumScreen parent;
        private final Album album;
        private final String song;
        private final SafeIdentifier playableSong;
        private final Component text;
        private final int color;
        private final IconButton playButton;
        private final IconButton queueButton;
        private final IconButton toggleButton;
        private final Component statusText;
        private final ResourceLocation statusIcon;
        private final int statusColor;
        private final boolean playable;

        DetailEntry(Minecraft minecraft, Component text, int color) {
            this(null, minecraft, null, null, null, text, color, true, null, null, 0xFFFFFFFF);
        }

        DetailEntry(Minecraft minecraft, SafeIdentifier playableSong, Component text, boolean unlocked) {
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
                    IconButton.icon(unlocked ? "unlocked" : "locked"),
                    unlocked ? 0xFFFFFFFF : 0xFF888888
            );
        }

        DetailEntry(Minecraft minecraft, SafeIdentifier playableSong, Component text) {
            this(null, minecraft, null, null, playableSong, text, 0xFFAAAAAA, true, null, null, 0xFFFFFFFF);
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
                    forcedStatusIcon(album, song, forcedStatus),
                    0xFFFFFFFF
            );
        }

        DetailEntry(AlbumScreen parent, Minecraft minecraft, Album album, String song, SafeIdentifier playableSong, Component text, int color, boolean playable, Component statusText, ResourceLocation statusIcon, int statusColor) {
            this.parent = parent;
            this.minecraft = minecraft;
            this.album = album;
            this.song = song;
            this.playableSong = playableSong;
            this.text = text;
            this.color = color;
            this.playable = playable;
            this.statusText = statusText;
            this.statusIcon = statusIcon;
            this.statusColor = statusColor;
            this.playButton = playableSong == null ? null : new IconButton(playMessage(playableSong), playIcon(playableSong), button -> {
                playTrack();
                ((IconButton) button).setIconAndTooltip(playIcon(playableSong), playMessage(playableSong));
            });
            this.queueButton = playableSong == null ? null : new IconButton(Component.translatable("button.music_and_melody.queue"), IconButton.icon("queue"), button -> {
                PlaylistHelper.add(playableSong);
                button.active = false;
            });
            this.toggleButton = album == null ? null : new IconButton(toggleMessage(album, song), toggleIcon(album, song), button -> {
                toggleTrack();
                ((IconButton) button).setIconAndTooltip(toggleIcon(album, song), toggleMessage(album, song));
            });
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.setContentBounds(left, top, width, height);
            int controlsWidth = controlsWidth();
            int contentRight = this.getContentRight() + 4;
            int contentYMiddle = top + height / 2;
            int rightPadding = controlsWidth == 0 ? 0 : 8;

            FormattedCharSequence line = this.minecraft.font.split(
                    this.text,
                    this.getContentWidth() - (controlsWidth == 0 ? 0 : controlsWidth + rightPadding)
            ).getFirst();

            graphics.drawString(
                    this.minecraft.font,
                    line,
                    this.getContentX() + 1,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2,
                    this.color
            );

            if (this.playButton != null) {
                int buttonY = contentYMiddle - 10;
                int controlX = contentRight - controlsWidth - rightPadding;
                boolean missing = hasMissingStatusIcon();
                this.playButton.setIconAndTooltip(playIcon(this.playableSong), playMessage(this.playableSong));
                this.playButton.active = this.playable && !missing;
                this.queueButton.active = this.playable && !missing && !PlaylistHelper.isQueued(this.playableSong);
                this.playButton.setX(controlX);
                this.playButton.setY(buttonY);
                this.queueButton.setX(this.playButton.getX() + BUTTON_WIDTH + BUTTON_GAP);
                this.queueButton.setY(buttonY);
                if (this.toggleButton != null) {
                    this.toggleButton.setIconAndTooltip(toggleIcon(this.album, this.song), toggleMessage(this.album, this.song));
                    this.toggleButton.setX(this.queueButton.getX() + BUTTON_WIDTH + BUTTON_GAP);
                    this.toggleButton.setY(buttonY);
                } else if (this.statusIcon != null) {
                    int statusX = this.queueButton.getX() + BUTTON_WIDTH + BUTTON_GAP;
                    IconButton.renderIconWithTooltip(graphics, this.statusIcon, statusX, buttonY, this.statusText, mouseX, mouseY);
                } else if (hasMissingStatusIcon()) {
                    int statusX = this.queueButton.getX() + BUTTON_WIDTH + BUTTON_GAP;
                    IconButton.renderIconWithTooltip(graphics, IconButton.icon("unknown"), statusX, buttonY, Component.translatable("button.music_and_melody.unknown"), mouseX, mouseY);
                }
                this.playButton.render(graphics, mouseX, mouseY, tickDelta);
                this.queueButton.render(graphics, mouseX, mouseY, tickDelta);
                if (this.toggleButton != null) {
                    this.toggleButton.render(graphics, mouseX, mouseY, tickDelta);
                } else if (this.statusText != null && this.statusIcon == null) {
                    int statusX = this.queueButton.getX() + BUTTON_WIDTH + BUTTON_GAP;
                    FormattedCharSequence status = this.minecraft.font.split(this.statusText, STATUS_WIDTH).getFirst();
                    graphics.drawString(this.minecraft.font, status, statusX + (STATUS_WIDTH - this.minecraft.font.width(status)) / 2, contentYMiddle - this.minecraft.font.lineHeight / 2, this.statusColor);
                }
            }
        }

        private int controlsWidth() {
            if (this.playButton == null) return 0;
            int width = BUTTON_WIDTH * 2 + BUTTON_GAP;
            if (this.toggleButton != null) return width + BUTTON_GAP + BUTTON_WIDTH;
            if (this.statusIcon != null) return width + BUTTON_GAP + BUTTON_WIDTH;
            if (hasMissingStatusIcon()) return width + BUTTON_GAP + BUTTON_WIDTH;
            if (this.statusText != null) return width + BUTTON_GAP + STATUS_WIDTH;
            return width;
        }

        private boolean hasMissingStatusIcon() {
            return this.playButton != null && this.toggleButton == null && this.statusIcon == null && this.statusText == null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.playButton != null && this.playButton.mouseClicked(mouseX, mouseY, button)
                    || this.queueButton != null && this.queueButton.mouseClicked(mouseX, mouseY, button)
                    || this.toggleButton != null && this.toggleButton.mouseClicked(mouseX, mouseY, button)
                    || super.mouseClicked(mouseX, mouseY, button);
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

        private static Component playMessage(SafeIdentifier song) {
            return Component.translatable(PlaylistHelper.isPlaying(song) ? "button.music_and_melody.stop" : "button.music_and_melody.play");
        }

        private static ResourceLocation playIcon(SafeIdentifier song) {
            return IconButton.icon(PlaylistHelper.isPlaying(song) ? "pause" : "play");
        }

        private static Component toggleMessage(Album album, String song) {
            return Component.translatable(album.isTrackEnabled(song) ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled");
        }

        private static ResourceLocation toggleIcon(Album album, String song) {
            return IconButton.icon(album.isTrackEnabled(song) ? "enabled" : "disabled");
        }

        private static ResourceLocation forcedStatusIcon(Album album, String song, Component forcedStatus) {
            if (forcedStatus == null) return null;
            if (album.album.equals(ConfigAlbum.ALBUM_ID)) return IconButton.icon("config");
            return album.isTrackForcedEnabled(song) ? IconButton.icon("always_enabled") : null;
        }
    }
}
