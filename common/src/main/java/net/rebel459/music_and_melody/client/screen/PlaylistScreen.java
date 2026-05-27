package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.MaMClientConfig;

public class PlaylistScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.playlist");
    private final Screen parent;
    private QueueList list;
    private IconButton playPauseButton;
    private IconButton skipButton;
    private IconButton loopButton;
    private IconButton shuffleButton;
    private IconButton clearButton;
    private IconButton saveIconButton;
    private IconButton searchButton;
    private IconButton directPlayButton;
    private IconButton directRemoveButton;
    private IconButton directLoopButton;
    private EditBox searchField;
    private Button eventsButton;
    private boolean searching;
    private boolean focusSearchAfterClick;
    private String search = "";

    public PlaylistScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        MusicDiscHelper.requestStats(this.minecraft);
        this.searching = false;
        this.focusSearchAfterClick = false;
        this.search = "";
        this.list = this.addRenderableWidget(new QueueList(this, this.minecraft, this.width, this.height - 112));
        int controlY = this.height - 51;
        int navY = this.height - 27;
        int rowWidth = Math.min(ContentBrowserScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
        int navWidth = (rowWidth - 8) / 3;
        int rowX = this.width / 2 - rowWidth / 2;
        int iconGroupWidth = IconButton.SIZE * 4 + 4 * 3;
        int iconGroupX = this.width / 2 - iconGroupWidth / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.browse"), button ->
                this.minecraft.setScreen(new ContentBrowserScreen(this))
        ).bounds(rowX, navY, navWidth, 20).build());
        this.searchButton = this.addRenderableWidget(new IconButton(Component.translatable("screen.music_and_melody.search"), IconButton.icon("search"), button -> toggleSearch()));
        this.searchField = this.addRenderableWidget(new EditBox(this.font, rowX + IconButton.SIZE + 4, controlY, rowWidth - IconButton.SIZE - 4, 20, Component.translatable("screen.music_and_melody.search")));
        this.searchField.setValue(this.search);
        this.searchField.setResponder(value -> {
            this.search = value;
            refreshQueue();
        });
        this.playPauseButton = this.addRenderableWidget(new IconButton(playPauseMessage(), playPauseIcon(), button -> {
            if (PlaylistHelper.isQueuePlaying()) {
                PlaylistHelper.pauseQueue();
            } else {
                PlaylistHelper.playNextNow();
            }
            this.playPauseButton.setIconAndTooltip(playPauseIcon(), playPauseMessage());
        }));
        this.skipButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.skip"), IconButton.icon("next"), button -> {
            PlaylistHelper.skipQueue();
            this.playPauseButton.setIconAndTooltip(playPauseIcon(), playPauseMessage());
        }));
        this.shuffleButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.shuffle"), IconButton.icon("shuffle"), button -> {
            if (PlaylistHelper.shuffleQueue()) {
                this.refreshQueue();
            }
        }));
        this.loopButton = this.addRenderableWidget(new IconButton(loopMessage(), loopIcon(), button -> {
            PlaylistHelper.setLoopingQueue(!PlaylistHelper.isLoopingQueue());
            this.loopButton.setIconAndTooltip(loopIcon(), loopMessage());
        }));
        this.clearButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.clear"), IconButton.icon("clear"), button -> {
            PlaylistHelper.clear();
            this.refreshQueue();
        }));
        this.saveIconButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.save"), IconButton.icon("save"), button ->
                this.minecraft.setScreen(new SavePlaylistScreen(this))
        ));
        this.directPlayButton = new IconButton(directPlayMessage(), directPlayIcon(), button -> {
            if (PlaylistHelper.isDirectPlaying()) {
                PlaylistHelper.stop();
            } else {
                PlaylistHelper.playDirectSong();
            }
            this.directPlayButton.setIconAndTooltip(directPlayIcon(), directPlayMessage());
        });
        this.directRemoveButton = new IconButton(Component.translatable("button.music_and_melody.remove"), IconButton.icon("remove"), button -> PlaylistHelper.removeDirectSong());
        this.directLoopButton = new IconButton(directLoopMessage(), directLoopIcon(), button -> {
            PlaylistHelper.setDirectSongLooping(!PlaylistHelper.isDirectSongLooping());
            this.directLoopButton.setIconAndTooltip(directLoopIcon(), directLoopMessage());
        });
        this.searchButton.setX(rowX);
        this.searchButton.setY(controlY);
        this.playPauseButton.setX(iconGroupX);
        this.playPauseButton.setY(controlY);
        this.skipButton.setX(iconGroupX + (IconButton.SIZE + 4));
        this.skipButton.setY(controlY);
        this.shuffleButton.setX(iconGroupX + (IconButton.SIZE + 4) * 2);
        this.shuffleButton.setY(controlY);
        this.loopButton.setX(iconGroupX + (IconButton.SIZE + 4) * 3);
        this.loopButton.setY(controlY);
        this.clearButton.setX(rowX + rowWidth - IconButton.SIZE);
        this.clearButton.setY(controlY);
        this.saveIconButton.setX(this.clearButton.getX() - IconButton.SIZE - 4);
        this.saveIconButton.setY(controlY);
        this.directPlayButton.visible = false;
        this.directRemoveButton.visible = false;
        this.directLoopButton.visible = false;
        this.eventsButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.playlist_events"), button ->
                this.minecraft.setScreen(new EventScreen(this, true))
        ).bounds(rowX + navWidth + 4, navY, navWidth, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + (navWidth + 4) * 2, navY, navWidth, 20)
                .build());
        MusicScreenHelper.addSocialButtons(this);
        updateSearchRow();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        renderContextRow(graphics, mouseX, mouseY, tickDelta);
        if (this.playPauseButton != null) {
            this.playPauseButton.setIconAndTooltip(playPauseIcon(), playPauseMessage());
            this.playPauseButton.active = PlaylistHelper.isQueuePlaying() || PlaylistHelper.hasQueuedSongs();
        }
        if (this.loopButton != null) this.loopButton.setIconAndTooltip(loopIcon(), loopMessage());
        if (this.skipButton != null) this.skipButton.active = PlaylistHelper.isQueuePlaying() && PlaylistHelper.canSkipQueue();
        if (this.shuffleButton != null) this.shuffleButton.active = PlaylistHelper.queuedSongs().size() > 1;
        if (this.clearButton != null) this.clearButton.active = PlaylistHelper.hasQueuedSongs();
        if (this.saveIconButton != null) this.saveIconButton.active = PlaylistHelper.hasQueuedSongs();
        if (this.eventsButton != null) this.eventsButton.active = MaMClientConfig.get().allow_events;
        updateSearchRow();
        focusSearchAfterClick();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean clicked = this.directPlayButton != null
                && PlaylistHelper.hasDirectSong()
                && this.directPlayButton.mouseClicked(event, doubleClick)
                || this.directRemoveButton != null
                && PlaylistHelper.hasDirectSong()
                && this.directRemoveButton.mouseClicked(event, doubleClick)
                || this.directLoopButton != null
                && PlaylistHelper.hasDirectSong()
                && this.directLoopButton.mouseClicked(event, doubleClick)
                || super.mouseClicked(event, doubleClick);
        focusSearchAfterClick();
        return clicked;
    }

    private void renderContextRow(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        int rowWidth = Math.min(ContentBrowserScreen.MAIN_BUTTON_ROW_WIDTH, this.width - 20);
        int rowX = this.width / 2 - rowWidth / 2;
        int y = 36;
        Component text = null;

        boolean showDirectSong = PlaylistHelper.hasDirectSong()
                && PlaylistHelper.getDirectSongId() != null
                && !PlaylistHelper.isQueuePlaying();
        if (showDirectSong) {
            text = MusicScreenHelper.playlistName(this.minecraft, PlaylistHelper.getDirectSongId());
            int buttonY = y - 5;
            int loopX = rowX + rowWidth - IconButton.SIZE;
            int removeX = loopX - IconButton.SIZE - 4;
            int playX = removeX - IconButton.SIZE - 4;
            this.directPlayButton.visible = true;
            this.directPlayButton.active = true;
            this.directPlayButton.setIconAndTooltip(directPlayIcon(), directPlayMessage());
            this.directPlayButton.setX(playX);
            this.directPlayButton.setY(buttonY);
            this.directPlayButton.render(graphics, mouseX, mouseY, tickDelta);
            this.directRemoveButton.visible = true;
            this.directRemoveButton.active = true;
            this.directRemoveButton.setX(removeX);
            this.directRemoveButton.setY(buttonY);
            this.directRemoveButton.render(graphics, mouseX, mouseY, tickDelta);
            this.directLoopButton.visible = true;
            this.directLoopButton.active = true;
            this.directLoopButton.setIconAndTooltip(directLoopIcon(), directLoopMessage());
            this.directLoopButton.setX(loopX);
            this.directLoopButton.setY(buttonY);
            this.directLoopButton.render(graphics, mouseX, mouseY, tickDelta);
        } else {
            this.directPlayButton.visible = false;
            this.directRemoveButton.visible = false;
            this.directLoopButton.visible = false;
            var source = PlaylistHelper.queueSource();
            if (source.isPresent() && PlaylistHelper.hasQueuedSongs()) {
                text = Component.literal(source.get().name());
            }
        }

        if (text != null) {
            int buttonsWidth = this.directPlayButton.visible ? IconButton.SIZE * 3 + 8 : 0;
            int textWidth = rowWidth - buttonsWidth - (buttonsWidth == 0 ? 0 : 8);
            int textCenter = rowX + textWidth / 2;
            graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(text.getString(), textWidth), textCenter, y, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    void refreshQueue() {
        if (this.list != null) this.list.refresh();
    }

    private void toggleSearch() {
        this.searching = !this.searching;
        if (this.searching) {
            focusSearchField();
        } else {
            this.search = "";
            this.searchField.setValue("");
            refreshQueue();
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
        if (this.playPauseButton != null) this.playPauseButton.visible = controlsVisible;
        if (this.skipButton != null) this.skipButton.visible = controlsVisible;
        if (this.shuffleButton != null) this.shuffleButton.visible = controlsVisible;
        if (this.loopButton != null) this.loopButton.visible = controlsVisible;
        if (this.clearButton != null) this.clearButton.visible = controlsVisible;
        if (this.saveIconButton != null) this.saveIconButton.visible = controlsVisible;
    }

    private static Component loopMessage() {
        if (PlaylistHelper.isLoopingQueue()) return Component.translatable("button.music_and_melody.looping");
        else return Component.translatable("button.music_and_melody.loop");
    }

    private static Identifier loopIcon() {
        return IconButton.icon(PlaylistHelper.isLoopingQueue() ? "looping" : "loop");
    }

    private static Component playPauseMessage() {
        return Component.translatable(PlaylistHelper.isQueuePlaying() ? "button.music_and_melody.stop" : "button.music_and_melody.play");
    }

    private static Identifier playPauseIcon() {
        return IconButton.icon(PlaylistHelper.isQueuePlaying() ? "pause" : "play");
    }

    private static Component directPlayMessage() {
        return Component.translatable(PlaylistHelper.isDirectPlaying() ? "button.music_and_melody.stop" : "button.music_and_melody.play");
    }

    private static Identifier directPlayIcon() {
        return IconButton.icon(PlaylistHelper.isDirectPlaying() ? "pause" : "play");
    }

    private static Component directLoopMessage() {
        return Component.translatable(PlaylistHelper.isDirectSongLooping() ? "button.music_and_melody.looping" : "button.music_and_melody.loop");
    }

    private static Identifier directLoopIcon() {
        return IconButton.icon(PlaylistHelper.isDirectSongLooping() ? "looping" : "loop");
    }

    private static class QueueList extends ObjectSelectionList<QueueEntry> {

        private final PlaylistScreen screen;

        QueueList(PlaylistScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 56, 24);
            this.screen = screen;
            this.centerListVertically = false;
            this.refresh();
        }

        private void refresh() {
            this.clearEntries();
            for (int i = 0; i < PlaylistHelper.queuedSongs().size(); i++) {
                SafeIdentifier id = PlaylistHelper.queuedSongs().get(i);
                if (!matchesSearch(id)) continue;
                this.addEntry(new QueueEntry(this.screen, this.minecraft, i, id));
            }
        }

        private boolean matchesSearch(SafeIdentifier id) {
            String query = this.screen.search.trim().toLowerCase(java.util.Locale.ROOT);
            if (query.isEmpty()) return true;
            return id.toString().toLowerCase(java.util.Locale.ROOT).contains(query)
                    || MusicScreenHelper.playlistName(this.minecraft, id).getString().toLowerCase(java.util.Locale.ROOT).contains(query);
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

    private static class QueueEntry extends ObjectSelectionList.Entry<QueueEntry> {

        private final PlaylistScreen screen;
        private final Minecraft minecraft;
        private final int index;
        private final SafeIdentifier song;
        private final Component text;
        private final int color;
        private final IconButton playButton;
        private final IconButton removeButton;

        QueueEntry(Minecraft minecraft, Component text, int color) {
            this.screen = null;
            this.minecraft = minecraft;
            this.index = -1;
            this.song = null;
            this.text = text;
            this.color = color;
            this.playButton = null;
            this.removeButton = null;
        }

        QueueEntry(PlaylistScreen screen, Minecraft minecraft, int index, SafeIdentifier song) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.index = index;
            this.song = song;
            this.text = MusicScreenHelper.playlistName(minecraft, song);
            this.color = 0xFFFFFFFF;
            this.playButton = new IconButton(playMessage(song), playIcon(song), button -> {
                if (PlaylistHelper.isQueuePlaying(song)) {
                    PlaylistHelper.pauseQueue();
                } else {
                    PlaylistHelper.playNow(index);
                }
                ((IconButton) button).setIconAndTooltip(playIcon(song), playMessage(song));
            });
            this.removeButton = new IconButton(Component.translatable("button.music_and_melody.remove"), IconButton.icon("remove"), button -> {
                PlaylistHelper.remove(index);
                screen.refreshQueue();
            });
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int buttonWidth = this.removeButton == null ? 0 : IconButton.SIZE * 2 + 4 + 8;
            boolean unlocked = this.song == null || MusicDiscHelper.isSoundUnlocked(this.minecraft, this.song);
            Component rowText = this.song == null ? this.text : this.text.copy().withStyle(unlocked ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            if (this.song != null && PlaylistHelper.isQueuePlaying(this.song)) rowText = rowText.copy().withStyle(ChatFormatting.UNDERLINE);
            FormattedCharSequence line = this.minecraft.font.split(rowText, this.getContentWidth() - buttonWidth).getFirst();
            int textColor = this.song == null ? this.color : unlocked ? 0xFFFFFFFF : 0xFF888888;
            graphics.drawString(this.minecraft.font, line, this.getContentX() + 1, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, textColor);
            if (this.removeButton != null) {
                int buttonY = this.getContentYMiddle() - 10;
                int removeX = this.getContentRight() - IconButton.SIZE;
                this.playButton.active = unlocked;
                this.playButton.setIconAndTooltip(playIcon(this.song), playMessage(this.song));
                this.playButton.setX(removeX - IconButton.SIZE - 4);
                this.playButton.setY(buttonY);
                this.removeButton.setX(removeX);
                this.removeButton.setY(buttonY);
                this.playButton.render(graphics, mouseX, mouseY, tickDelta);
                this.removeButton.render(graphics, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.playButton != null && this.playButton.mouseClicked(event, doubleClick)
                    || this.removeButton != null && this.removeButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }

        private static Component playMessage(SafeIdentifier song) {
            return Component.translatable(PlaylistHelper.isQueuePlaying(song) ? "button.music_and_melody.stop" : "button.music_and_melody.play");
        }

        private static Identifier playIcon(SafeIdentifier song) {
            return IconButton.icon(PlaylistHelper.isQueuePlaying(song) ? "pause" : "play");
        }
    }
}
