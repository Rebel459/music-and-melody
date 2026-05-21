package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.MusicDiscHelper;
import net.rebel459.music_and_melody.client.PlaylistHelper;

public class PlaylistScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.playlist");
    private final Screen parent;
    private QueueList list;
    private Button playPauseButton;
    private Button loopButton;
    private Button shuffleButton;
    private Button clearButton;
    private Button saveButton;

    public PlaylistScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        MusicDiscHelper.requestStats(this.minecraft);
        this.list = this.addRenderableWidget(new QueueList(this, this.minecraft, this.width, this.height - 88));
        int controlY = this.height - 51;
        int navY = this.height - 27;
        int rowWidth = Math.min(360, this.width - 20);
        int controlWidth = (rowWidth - 12) / 4;
        int navWidth = (rowWidth - 12) / 4;
        int rowX = this.width / 2 - rowWidth / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.albums"), button ->
                this.minecraft.setScreen(new AlbumScreen(this))
        ).bounds(rowX, navY, navWidth, 20).build());
        this.playPauseButton = this.addRenderableWidget(Button.builder(playPauseMessage(), button -> {
            if (PlaylistHelper.isPlaying()) {
                PlaylistHelper.pauseQueue();
            } else {
                PlaylistHelper.playNextNow();
            }
            button.setMessage(playPauseMessage());
        }).bounds(rowX, controlY, controlWidth, 20).build());
        this.loopButton = this.addRenderableWidget(Button.builder(loopMessage(), button -> {
            PlaylistHelper.setLoopingQueue(!PlaylistHelper.isLoopingQueue());
            button.setMessage(loopMessage());
        }).bounds(rowX + (controlWidth + 4), controlY, controlWidth, 20).build());
        this.shuffleButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.shuffle"), button -> {
            if (PlaylistHelper.shuffleQueue()) {
                this.refreshQueue();
            }
        }).bounds(rowX + (controlWidth + 4) * 2, controlY, controlWidth, 20).build());
        this.clearButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.clear_all"), button -> {
            PlaylistHelper.clear();
            this.refreshQueue();
        }).bounds(rowX + (controlWidth + 4) * 3, controlY, controlWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.events"), button ->
                this.minecraft.setScreen(new EventScreen(this, true))
        ).bounds(rowX + navWidth + 4, navY, navWidth, 20).build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.save"), button ->
                this.minecraft.setScreen(new SavePlaylistScreen(this))
        ).bounds(rowX + (navWidth + 4) * 2, navY, navWidth, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + (navWidth + 4) * 3, navY, navWidth, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        if (this.playPauseButton != null) {
            this.playPauseButton.setMessage(playPauseMessage());
            this.playPauseButton.active = PlaylistHelper.isPlaying() || PlaylistHelper.hasQueuedSongs();
        }
        if (this.loopButton != null) this.loopButton.setMessage(loopMessage());
        if (this.shuffleButton != null) this.shuffleButton.active = PlaylistHelper.queuedSongs().size() > 1;
        if (this.clearButton != null) this.clearButton.active = PlaylistHelper.hasQueuedSongs();
        if (this.saveButton != null) this.saveButton.active = PlaylistHelper.hasQueuedSongs();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    void refreshQueue() {
        if (this.list != null) this.list.refresh();
    }

    private static Component loopMessage() {
        if (PlaylistHelper.isLoopingQueue()) return Component.translatable("button.music_and_melody.looping");
        else return Component.translatable("button.music_and_melody.loop");
    }

    private static Component playPauseMessage() {
        return Component.translatable(PlaylistHelper.isPlaying() ? "button.music_and_melody.stop" : "button.music_and_melody.play");
    }

    private static class QueueList extends ObjectSelectionList<QueueEntry> {

        private final PlaylistScreen screen;

        QueueList(PlaylistScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 32, 24);
            this.screen = screen;
            this.centerListVertically = false;
            this.refresh();
        }

        private void refresh() {
            this.clearEntries();
            for (int i = 0; i < PlaylistHelper.queuedSongs().size(); i++) {
                Identifier id = PlaylistHelper.queuedSongs().get(i);
                this.addEntry(new QueueEntry(this.screen, this.minecraft, i, id));
            }
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
        private final Identifier song;
        private final Component text;
        private final int color;
        private final Button playButton;
        private final Button removeButton;

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

        QueueEntry(PlaylistScreen screen, Minecraft minecraft, int index, Identifier song) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.index = index;
            this.song = song;
            this.text = MusicScreenHelper.playlistName(minecraft, song);
            this.color = 0xFFFFFFFF;
            this.playButton = Button.builder(Component.translatable("button.music_and_melody.play"), button -> PlaylistHelper.playNow(index)).size(64, 20).build();
            this.removeButton = Button.builder(Component.translatable("button.music_and_melody.remove"), button -> {
                PlaylistHelper.remove(index);
                screen.refreshQueue();
            }).size(64, 20).build();
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int buttonWidth = this.removeButton == null ? 0 : 122;
            boolean unlocked = this.song == null || MusicDiscHelper.isSoundUnlocked(this.minecraft, this.song);
            Component rowText = this.song == null ? this.text : this.text.copy().withStyle(unlocked ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            if (this.song != null && PlaylistHelper.isPlaying(this.song)) rowText = rowText.copy().withStyle(ChatFormatting.UNDERLINE);
            FormattedCharSequence line = this.minecraft.font.split(rowText, this.getContentWidth() - buttonWidth).getFirst();
            int textColor = this.song == null ? this.color : unlocked ? 0xFFFFFFFF : 0xFF888888;
            graphics.text(this.minecraft.font, line, this.getContentX() + 1, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, textColor);
            if (this.removeButton != null) {
                int buttonY = this.getContentYMiddle() - 10;
                int removeX = this.getContentRight() - 64;
                this.playButton.active = unlocked;
                this.playButton.setX(removeX - 68);
                this.playButton.setY(buttonY);
                this.removeButton.setX(removeX);
                this.removeButton.setY(buttonY);
                this.playButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                this.removeButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.playButton != null && this.playButton.mouseClicked(event, doubleClick)
                    || this.removeButton != null && this.removeButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }
    }
}
