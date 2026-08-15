package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.List;

import static net.rebel459.music_and_melody.client.util.ScreenConstants.*;

public class AlbumDetailsScreen extends Screen {

    private final ContentBrowserScreen parent;
    private final Album album;
    private final Playlist playlist;
    private DetailList list;
    private Button loadButton;
    private Button queueAllButton;
    private IconButton searchButton;
    private EditBox searchField;
    private boolean searching;
    private boolean focusSearchAfterClick;
    private String search = "";

    public AlbumDetailsScreen(ContentBrowserScreen parent, Album album) {
        super(album.name);
        this.parent = parent;
        this.album = album;
        this.playlist = null;
    }

    public AlbumDetailsScreen(ContentBrowserScreen parent, Playlist playlist) {
        super(playlist.name);
        this.parent = parent;
        this.album = null;
        this.playlist = playlist;
    }

    @Override
    protected void init() {
        MusicDiscHelper.requestStats(this.minecraft);
        int rowWidth = Math.min(ContentBrowserScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
        int rowX = this.width / 2 - rowWidth / 2;
        this.list = this.addRenderableWidget(new DetailList(this.parent, this.minecraft, this.width, this.height - 112, 56, this.album, this.playlist, this.search));
        int actionY = this.height - 51;
        int leftWidth = (rowWidth - 4) / 2;
        int rightX = rowX + leftWidth + 4;
        this.searchButton = this.addRenderableWidget(new IconButton(Component.translatable("screen.music_and_melody.search"), IconButton.icon("search"), button -> toggleSearch()));
        this.searchButton.setX(rowX);
        this.searchButton.setY(actionY);
        this.searchField = this.addRenderableWidget(new EditBox(this.font, rowX + IconButton.SIZE + 4, actionY, rowWidth - IconButton.SIZE - 4, 20, Component.translatable("screen.music_and_melody.search")));
        this.searchField.setValue(this.search);
        this.searchField.setResponder(value -> {
            this.search = value;
            refreshList();
        });
        int buttonY = this.height - 27;
        int doneWidth = Math.min(152, rowWidth);

        this.loadButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.load"), button -> loadAll())
                .bounds(rowX + IconButton.SIZE + 4, actionY, leftWidth - IconButton.SIZE - 4, 20)
                .build());
        this.queueAllButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.queue_all"), button -> queueAll())
                .bounds(rightX, actionY, leftWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - doneWidth / 2, buttonY, doneWidth, 20)
                .build());
        MusicScreenHelper.addSocialButtons(this);
        updateSearchRow();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        int iconSize = 36;
        int gap = 7;
        int maxTextWidth = Math.max(1, this.width - iconSize - gap - 40);
        FormattedCharSequence title = this.font.split(this.title, maxTextWidth).getFirst();
        String id = this.font.plainSubstrByWidth(id().toString(), maxTextWidth);
        String counts = countText();
        int textWidth = Math.max(Math.max(this.font.width(title), this.font.width(id)), this.font.width(counts));
        int titleX = this.width / 2 - (iconSize + gap + textWidth) / 2;
        int titleY = 12;
        int textX = titleX + iconSize + gap;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, icon()), titleX, titleY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int titleColor = this.album != null && this.album.isFavourite() || this.playlist != null && this.playlist.isFavourite()
                ? TEXT_FAVOURITE
                : TEXT_TITLE;
        graphics.text(this.font, title, textX, titleY + 2, titleColor);
        graphics.text(this.font, Component.literal(id), textX, titleY + 14, TEXT_DESCRIPTION);
        graphics.text(this.font, Component.literal(counts), textX, titleY + 26, TEXT_DESCRIPTION);
        List<SafeIdentifier> songs = queueSongs(this.minecraft);
        boolean hasSongs = !songs.isEmpty();
        if (this.loadButton != null) this.loadButton.active = hasSongs && !songs.equals(PlaylistHelper.queuedSongs());
        if (this.queueAllButton != null) this.queueAllButton.active = hasQueueableSongs(songs);
        updateSearchRow();
        focusSearchAfterClick();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean clicked = super.mouseClicked(event, doubleClick);
        focusSearchAfterClick();
        return clicked;
    }

    public void onStatsUpdated() {
        if (this.list != null) {
            this.list.refresh(this.album, this.playlist, this.search);
        }
    }

    private void refreshList() {
        if (this.list != null) {
            this.list.refresh(this.album, this.playlist, this.search);
        }
    }

    private void loadAll() {
        PlaylistHelper.clear();
        PlaylistHelper.addAll(queueSongs(this.minecraft));
        PlaylistHelper.setQueueSource(this.album != null ? MaMDataConfig.QueueSourceType.ALBUM : MaMDataConfig.QueueSourceType.PLAYLIST, id().toString(), this.title.getString());
    }

    private void queueAll() {
        PlaylistHelper.addAll(queueSongs(this.minecraft));
    }

    private boolean hasQueueableSongs(List<SafeIdentifier> songs) {
        for (SafeIdentifier song : songs) {
            if (!PlaylistHelper.isQueued(song)) return true;
        }
        return false;
    }

    private void toggleSearch() {
        this.searching = !this.searching;
        if (this.searching) {
            focusSearchField();
        } else {
            this.search = "";
            this.searchField.setValue("");
            refreshList();
            updateSearchRow();
        }
    }

    private void focusSearchField() {
        updateSearchRow();
        this.focusSearchAfterClick = true;
    }

    private void focusSearchAfterClick() {
        if (!this.focusSearchAfterClick || this.searchField == null || !this.searching) return;
        this.setFocused(this.searchField);
        this.setInitialFocus(this.searchField);
        this.searchField.setFocused(true);
        this.focusSearchAfterClick = false;
    }

    private void updateSearchRow() {
        boolean controlsVisible = !this.searching;
        if (this.searchField != null) {
            this.searchField.visible = this.searching;
            this.searchField.active = this.searching;
        }
        if (this.loadButton != null) this.loadButton.visible = controlsVisible;
        if (this.queueAllButton != null) this.queueAllButton.visible = controlsVisible;
    }

    private Identifier id() {
        return this.album != null ? this.album.album : this.playlist.playlist;
    }

    private Identifier icon() {
        return this.album != null ? this.album.icon : this.playlist.icon;
    }

    private String countText() {
        int tracks = this.album != null ? this.album.tracks.size() : this.playlist.tracks.size();
        int discs = this.album != null ? this.album.discs.size() : this.playlist.discs.size();
        String trackText = tracks + " " + (tracks == 1 ? "track" : "tracks");
        String discText = discs + " " + (discs == 1 ? "disc" : "discs");
        return discs == 0 ? trackText : tracks == 0 ? discText : trackText + " | " + discText;
    }

    public static List<SafeIdentifier> queueSongs(Album album, Minecraft minecraft) {
        List<SafeIdentifier> songs = new ArrayList<>();
        album.tracks.stream()
                .map(album::trackId)
                .forEach(songs::add);
        album.discs.stream()
                .map(disc -> MusicDiscHelper.discSoundId(minecraft, album, disc))
                .flatMap(java.util.Optional::stream)
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
                .flatMap(java.util.Optional::stream)
                .forEach(id -> songs.add(SafeIdentifier.convert(id)));
        return songs;
    }

    private static class DetailList extends ObjectSelectionList<DetailEntry> {

        private final ContentBrowserScreen screen;

        DetailList(ContentBrowserScreen screen, Minecraft minecraft, int width, int height, int y, Album album, Playlist playlist, String search) {
            super(minecraft, width, height, y, 24);
            this.screen = screen;
            this.centerListVertically = false;
            refresh(album, playlist, search);
        }

        private void refresh(Album album, Playlist playlist, String search) {
            this.clearEntries();
            if (album != null) {
                addAlbumTracks(album, search);
                addAlbumDiscs(album, search);
            } else {
                addPlaylistTracks(playlist, search);
                addPlaylistDiscs(playlist, search);
            }
        }

        private void addAlbumTracks(Album album, String search) {
            if (album.tracks.isEmpty()) return;
            List<DetailEntry> entries = album.tracks.stream()
                    .filter(song -> matches(search, MusicScreenHelper.trackName(album, song), album.trackId(song).toString()))
                    .map(song -> new DetailEntry(this.screen, this.minecraft, album, song, MusicScreenHelper.trackName(album, song).copy(),
                            album.isTrackEnabled(song) ? TEXT_PRIMARY : TEXT_DISABLED, trackStatus(album, song)))
                    .toList();
            if (entries.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks").withStyle(ChatFormatting.BOLD), TEXT_HEADER));
            entries.forEach(this::addEntry);
        }

        private static Component trackStatus(Album album, String song) {
            if (album.album.equals(ConfigAlbum.ALBUM_ID)) return Component.translatable("screen.music_and_melody.album_details.config");
            if (album.isTrackForcedEnabled(song)) return Component.translatable("screen.music_and_melody.album_details.enabled");
            return null;
        }

        private void addAlbumDiscs(Album album, String search) {
            if (album.discs.isEmpty()) return;
            List<DetailEntry> entries = album.discs.stream()
                    .filter(disc -> {
                        Identifier id = MusicDiscHelper.albumEntryId(album, disc);
                        return matches(search, MusicDiscHelper.discName(id, disc), id.toString());
                    })
                    .map(disc -> {
                        Identifier id = MusicDiscHelper.albumEntryId(album, disc);
                        Component name = MusicDiscHelper.discName(id, disc);
                        boolean unlocked = MusicDiscHelper.isDiscUnlocked(this.minecraft, id);
                        return MusicDiscHelper.discSoundId(this.minecraft, album, disc)
                                .map(sound -> new DetailEntry(this.minecraft, SafeIdentifier.convert(sound), name.copy(), unlocked))
                                .orElseGet(() -> new DetailEntry(this.minecraft, name.copy(), Component.translatable("button.music_and_melody.unknown"), IconButton.icon("unknown")));
                    })
                    .toList();
            if (entries.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs").withStyle(ChatFormatting.BOLD), TEXT_HEADER));
            entries.forEach(this::addEntry);
        }

        private void addPlaylistTracks(Playlist playlist, String search) {
            if (playlist.tracks.isEmpty()) return;
            List<DetailEntry> entries = playlist.tracks.stream()
                    .filter(track -> matches(search, MusicScreenHelper.trackName(track), track.toString()))
                    .map(track -> {
                        Component name = MusicScreenHelper.trackName(track).copy();
                        AlbumTrack albumTrack = findAlbumTrack(track);
                        if (albumTrack != null) {
                            int color = albumTrack.album().isTrackEnabled(albumTrack.song()) ? TEXT_PRIMARY : TEXT_DISABLED;
                            return new DetailEntry(this.screen, this.minecraft, albumTrack.album(), albumTrack.song(), name, color, trackStatus(albumTrack.album(), albumTrack.song()));
                        }
                        return new DetailEntry(this.minecraft, track, name);
                    })
                    .toList();
            if (entries.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks").withStyle(ChatFormatting.BOLD), TEXT_HEADER));
            entries.forEach(this::addEntry);
        }

        private static AlbumTrack findAlbumTrack(SafeIdentifier track) {
            for (Album album : Album.ALBUMS) {
                for (String song : album.tracks) {
                    if (album.trackId(song).equals(track)) return new AlbumTrack(album, song);
                }
            }
            return null;
        }

        private void addPlaylistDiscs(Playlist playlist, String search) {
            if (playlist.discs.isEmpty()) return;
            List<DetailEntry> entries = playlist.discs.stream()
                    .filter(disc -> matches(search, MusicDiscHelper.discName(disc), disc.toString()))
                    .map(disc -> {
                        Component name = MusicDiscHelper.discName(disc);
                        boolean unlocked = MusicDiscHelper.isDiscUnlocked(this.minecraft, disc);
                        return MusicDiscHelper.discSoundId(this.minecraft, disc)
                                .map(sound -> new DetailEntry(this.minecraft, SafeIdentifier.convert(sound), name.copy(), unlocked))
                                .orElseGet(() -> new DetailEntry(this.minecraft, name.copy(), Component.translatable("button.music_and_melody.unknown"), IconButton.icon("unknown")));
                    })
                    .toList();
            if (entries.isEmpty()) return;
            this.addEntry(new DetailEntry(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs").withStyle(ChatFormatting.BOLD), TEXT_HEADER));
            entries.forEach(this::addEntry);
        }

        private static boolean matches(String search, Component name, String id) {
            String query = search.trim().toLowerCase(java.util.Locale.ROOT);
            return query.isEmpty()
                    || name.getString().toLowerCase(java.util.Locale.ROOT).contains(query)
                    || id.toLowerCase(java.util.Locale.ROOT).contains(query);
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

        private static final int BUTTON_WIDTH = IconButton.SIZE;
        private static final int BUTTON_GAP = 4;
        private static final int STATUS_WIDTH = 54;
        private final Minecraft minecraft;
        private final ContentBrowserScreen parent;
        private final Album album;
        private final String song;
        private final SafeIdentifier playableSong;
        private final Component text;
        private final int color;
        private final IconButton playButton;
        private final IconButton queueButton;
        private final IconButton toggleButton;
        private final Component statusText;
        private final Identifier statusIcon;
        private final int statusColor;
        private final boolean playable;

        DetailEntry(Minecraft minecraft, Component text, int color) {
            this(null, minecraft, null, null, null, text, color, true, null, null, TEXT_TITLE);
        }

        DetailEntry(Minecraft minecraft, Component text, Component statusText, Identifier statusIcon) {
            this(null, minecraft, null, null, null, text, TEXT_PRIMARY, false, statusText, statusIcon, TEXT_DISABLED);
        }

        DetailEntry(Minecraft minecraft, SafeIdentifier playableSong, Component text, boolean unlocked) {
            this(
                    null,
                    minecraft,
                    null,
                    null,
                    playableSong,
                    text,
                    unlocked ? TEXT_PRIMARY : TEXT_DISABLED,
                    unlocked,
                    Component.translatable(unlocked ? "screen.music_and_melody.album_details.unlocked" : "screen.music_and_melody.album_details.locked"),
                    IconButton.icon(unlocked ? "unlocked" : "locked"),
                    unlocked ? TEXT_TITLE : TEXT_DISABLED
            );
        }

        DetailEntry(Minecraft minecraft, SafeIdentifier playableSong, Component text) {
            this(null, minecraft, null, null, playableSong, text, TEXT_PRIMARY, true, null, null, TEXT_TITLE);
        }

        DetailEntry(ContentBrowserScreen parent, Minecraft minecraft, Album album, String song, Component text, int color, Component forcedStatus) {
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
                    TEXT_TITLE
            );
        }

        DetailEntry(ContentBrowserScreen parent, Minecraft minecraft, Album album, String song, SafeIdentifier playableSong, Component text, int color, boolean playable, Component statusText, Identifier statusIcon, int statusColor) {
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int controlsWidth = controlsWidth();
            FormattedCharSequence line = this.minecraft.font.split(this.text, this.getContentWidth() - (controlsWidth == 0 ? 0 : controlsWidth + 8)).getFirst();
            graphics.text(this.minecraft.font, line, this.getContentX() + 1, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, this.color);
            if (this.playButton == null) {
                renderStatus(graphics, controlsWidth, mouseX, mouseY);
                return;
            }
            if (this.playButton != null) {
                int buttonY = this.getContentYMiddle() - 10;
                int controlX = this.getContentRight() - controlsWidth;
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
                this.playButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                this.queueButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                if (this.toggleButton != null) {
                    this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                } else if (this.statusText != null && this.statusIcon == null) {
                    renderStatusText(graphics, this.queueButton.getX() + BUTTON_WIDTH + BUTTON_GAP);
                }
            }
        }

        private int controlsWidth() {
            if (this.playButton == null) {
                if (this.statusIcon != null) return BUTTON_WIDTH;
                if (this.statusText != null) return STATUS_WIDTH;
                return 0;
            }
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

        private void renderStatus(GuiGraphicsExtractor graphics, int controlsWidth, int mouseX, int mouseY) {
            if (controlsWidth == 0) return;
            int statusX = this.getContentRight() - controlsWidth;
            int statusY = this.getContentYMiddle() - 10;
            if (this.statusIcon != null) {
                IconButton.renderIconWithTooltip(graphics, this.statusIcon, statusX, statusY, this.statusText, mouseX, mouseY);
            } else if (this.statusText != null) {
                renderStatusText(graphics, statusX);
            }
        }

        private void renderStatusText(GuiGraphicsExtractor graphics, int statusX) {
            FormattedCharSequence status = this.minecraft.font.split(this.statusText, STATUS_WIDTH).getFirst();
            graphics.text(this.minecraft.font, status, statusX + (STATUS_WIDTH - this.minecraft.font.width(status)) / 2, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, this.statusColor);
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

        private static Component playMessage(SafeIdentifier song) {
            return Component.translatable(PlaylistHelper.isPlaying(song) ? "button.music_and_melody.stop" : "button.music_and_melody.play");
        }

        private static Identifier playIcon(SafeIdentifier song) {
            return IconButton.icon(PlaylistHelper.isPlaying(song) ? "pause" : "play");
        }

        private static Component toggleMessage(Album album, String song) {
            return Component.translatable(album.isTrackEnabled(song) ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled");
        }

        private static Identifier toggleIcon(Album album, String song) {
            return IconButton.icon(album.isTrackEnabled(song) ? "enabled" : "disabled");
        }

        private static Identifier forcedStatusIcon(Album album, String song, Component forcedStatus) {
            if (forcedStatus == null) return null;
            if (album.album.equals(ConfigAlbum.ALBUM_ID)) return IconButton.icon("config");
            return album.isTrackForcedEnabled(song) ? IconButton.icon("always_enabled") : null;
        }
    }
}
