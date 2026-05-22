package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlbumScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.music_and_melody.albums");
    private final Screen parent;
    private AlbumList list;
    private Button displayButton;
    private boolean reloadPending;

    public AlbumScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = this.addRenderableWidget(new AlbumList(this, this.minecraft, this.width, this.height - 64));
        int rowX = this.width / 2 - 154;
        int buttonY = this.height - 27;
        this.displayButton = this.addRenderableWidget(Button.builder(displayMessage(), button -> {
                    cycleDisplay();
                    button.setMessage(displayMessage());
                    refreshList();
                })
                .bounds(rowX, buttonY, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + 156, buttonY, 152, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        if (this.displayButton != null) this.displayButton.setMessage(displayMessage());
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

    public void refreshList() {
        if (this.list != null) this.list.refresh();
    }

    private static void cycleDisplay() {
        MaMDataConfig config = MaMDataConfig.get();
        config.albums.display = switch (config.albums.display) {
            case ALL -> MaMDataConfig.AlbumDisplay.ALBUMS;
            case ALBUMS -> MaMDataConfig.AlbumDisplay.PLAYLISTS;
            case PLAYLISTS -> MaMDataConfig.AlbumDisplay.FAVOURITES;
            case FAVOURITES -> MaMDataConfig.AlbumDisplay.ALL;
        };
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static Component displayMessage() {
        return Component.translatable("button.music_and_melody.album_display." + MaMDataConfig.get().albums.display.name().toLowerCase());
    }

    private static class AlbumList extends ObjectSelectionList<AlbumEntry> {

        private final AlbumScreen screen;

        AlbumList(AlbumScreen screen, Minecraft minecraft, int width, int height) {
            super(minecraft, width, height, 32, 46);
            this.screen = screen;
            this.centerListVertically = false;
            refresh();
        }

        private void refresh() {
            this.clearEntries();
            entries().stream()
                    .sorted(Comparator.comparing(entry -> entry.name().getString(), String.CASE_INSENSITIVE_ORDER))
                    .map(entry -> new AlbumEntry(this, this.screen, this.minecraft, entry))
                    .forEach(this::addEntry);
        }

        private static List<DisplayEntry> entries() {
            MaMDataConfig.AlbumDisplay display = MaMDataConfig.get().albums.display;
            List<DisplayEntry> entries = new ArrayList<>();
            if (display == MaMDataConfig.AlbumDisplay.ALBUMS || display == MaMDataConfig.AlbumDisplay.ALL || display == MaMDataConfig.AlbumDisplay.FAVOURITES) {
                Album.ALBUMS.stream()
                        .filter(album -> display != MaMDataConfig.AlbumDisplay.FAVOURITES || album.isFavourite())
                        .map(DisplayEntry::new)
                        .forEach(entries::add);
            }
            if (display == MaMDataConfig.AlbumDisplay.PLAYLISTS || display == MaMDataConfig.AlbumDisplay.ALL || display == MaMDataConfig.AlbumDisplay.FAVOURITES) {
                Playlist.PLAYLISTS.stream()
                        .filter(playlist -> !playlist.hidden)
                        .filter(playlist -> display != MaMDataConfig.AlbumDisplay.FAVOURITES || playlist.isFavourite())
                        .map(DisplayEntry::new)
                        .forEach(entries::add);
            }
            return entries;
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
        private final DisplayEntry entry;
        private final Button toggleButton;
        private final Button detailsButton;

        AlbumEntry(AlbumList list, AlbumScreen screen, Minecraft minecraft, DisplayEntry entry) {
            this.list = list;
            this.screen = screen;
            this.minecraft = minecraft;
            this.entry = entry;
            this.toggleButton = entry.album == null ? null : Button.builder(toggleMessage(entry.album), button -> {
                toggleAlbum();
                button.setMessage(toggleMessage(entry.album));
            }).size(BUTTON_WIDTH, 20).build();
            this.detailsButton = Button.builder(Component.translatable("button.music_and_melody.album_details"), button ->
                    this.minecraft.setScreen(this.entry.album != null
                            ? new AlbumDetailsScreen(this.screen, this.entry.album)
                            : new AlbumDetailsScreen(this.screen, this.entry.playlist))
            ).size(BUTTON_WIDTH, 20).build();
        }

        @Override
        public Component getNarration() {
            Component status = this.entry.album == null
                    ? Component.translatable("button.music_and_melody.playlist")
                    : CommonComponents.optionStatus(this.entry.album.isEnabled());
            return Component.empty()
                    .append(this.entry.name())
                    .append(CommonComponents.NARRATION_SEPARATOR)
                    .append(status);
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int iconX = this.getContentX() + 1;
            int iconY = this.getContentYMiddle() - ICON_SIZE / 2;
            int textX = iconX + ICON_SIZE + 7;
            int textY = this.getContentYMiddle() - 15;
            int buttonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
            int maxTextWidth = this.getContentWidth() - ICON_SIZE - buttonsWidth - 26;

            FormattedCharSequence name = this.minecraft.font.split(this.entry.name(), maxTextWidth).getFirst();
            String id = this.minecraft.font.plainSubstrByWidth(this.entry.id().toString(), maxTextWidth);
            String details = this.minecraft.font.plainSubstrByWidth(details(), maxTextWidth);

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.entry.icon(), iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            graphics.drawString(this.minecraft.font, name, textX, textY, 0xFFFFFFFF);
            graphics.drawString(this.minecraft.font, Component.literal(id).withStyle(ChatFormatting.GRAY), textX, textY + 11, 0xFFAAAAAA);
            graphics.drawString(this.minecraft.font, details, textX, textY + 22, 0xFFAAAAAA);

            int buttonX = this.getContentRight() - buttonsWidth;
            this.detailsButton.setX(buttonX);
            this.detailsButton.setY(this.getContentYMiddle() - 10);
            this.detailsButton.render(graphics, mouseX, mouseY, tickDelta);
            if (this.toggleButton != null) {
                this.toggleButton.setX(buttonX + BUTTON_WIDTH + BUTTON_GAP);
                this.toggleButton.setY(this.getContentYMiddle() - 10);
                this.toggleButton.render(graphics, mouseX, mouseY, tickDelta);
            } else {
                Component type = Component.translatable("button.music_and_melody.playlist").withStyle(ChatFormatting.GRAY);
                int labelX = buttonX + BUTTON_WIDTH + BUTTON_GAP + (BUTTON_WIDTH - this.minecraft.font.width(type)) / 2;
                graphics.drawString(this.minecraft.font, type, labelX, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, 0xFFAAAAAA);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.detailsButton.mouseClicked(event, doubleClick)
                    || this.toggleButton != null && this.toggleButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation() && this.entry.album != null) {
                this.toggleAlbum();
                this.toggleButton.setMessage(toggleMessage(this.entry.album));
                return true;
            }
            return super.keyPressed(event);
        }

        private void toggleAlbum() {
            this.entry.album.setEnabled(!this.entry.album.isEnabled());
            this.screen.markReloadPending();
        }

        private String details() {
            String tracks = count(this.entry.trackCount(), "track", "tracks");
            String discs = count(this.entry.discCount(), "disc", "discs");
            return tracks + " | " + discs;
        }

        private static String count(int count, String singular, String plural) {
            return count + " " + (count == 1 ? singular : plural);
        }

        private static Component toggleMessage(Album album) {
            return CommonComponents.optionStatus(album.isEnabled());
        }
    }

    private static class DisplayEntry {
        private final Album album;
        private final Playlist playlist;

        DisplayEntry(Album album) {
            this.album = album;
            this.playlist = null;
        }

        DisplayEntry(Playlist playlist) {
            this.album = null;
            this.playlist = playlist;
        }

        Component name() {
            return this.album != null ? this.album.name : this.playlist.name;
        }

        Identifier id() {
            return this.album != null ? this.album.album : this.playlist.playlist;
        }

        Identifier icon() {
            return this.album != null ? this.album.icon : this.playlist.icon;
        }

        int trackCount() {
            return this.album != null ? this.album.tracks.size() : this.playlist.tracks.size();
        }

        int discCount() {
            return this.album != null ? this.album.discs.size() : this.playlist.discs.size();
        }
    }
}
