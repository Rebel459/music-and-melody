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
import net.rebel459.music_and_melody.client.remote.RemoteAlbumManager;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumPack;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlbumScreen extends Screen {

    static final int MAIN_BUTTON_ROW_WIDTH = 308;
    private static final Component TITLE = Component.translatable("screen.music_and_melody.albums");
    private final Screen parent;
    private AlbumList list;
    private boolean catalogRefreshing;
    private boolean reloadPending;
    private final Set<ResourceLocation> pendingPlaylistDeletes = new HashSet<>();

    public AlbumScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        RemoteAlbumManager.refreshIfNeeded();
        this.catalogRefreshing = RemoteAlbumManager.isRefreshing();
        this.list = this.addRenderableWidget(new AlbumList(this, this.minecraft, this.width, this.height - 64));
        int rowX = this.width / 2 - MAIN_BUTTON_ROW_WIDTH / 2;
        int buttonY = this.height - 27;
        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.filter"), button ->
                        this.minecraft.setScreen(new AlbumFilterScreen(this))
                )
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
        if (this.catalogRefreshing && !RemoteAlbumManager.isRefreshing()) {
            this.catalogRefreshing = false;
            refreshList();
        }
    }

    @Override
    public void onClose() {
        deletePendingPlaylists();
        this.minecraft.setScreen(this.parent);
        if (this.reloadPending) {
            this.minecraft.reloadResourcePacks();
        }
    }

    public void markReloadPending() {
        this.reloadPending = true;
    }

    public void reloadResources() {
        if (this.minecraft == null) return;
        this.minecraft.reloadResourcePacks().thenRun(() -> {
            RemoteAlbumManager.markReloaded();
            this.minecraft.execute(this::refreshList);
        });
        refreshList();
    }

    public void refreshList() {
        if (this.list != null) this.list.refresh();
    }

    private boolean isDeletePending(Playlist playlist) {
        return this.pendingPlaylistDeletes.contains(playlist.playlist);
    }

    private void toggleDeletePending(Playlist playlist) {
        if (!playlist.isCustom()) return;
        if (!this.pendingPlaylistDeletes.remove(playlist.playlist)) {
            this.pendingPlaylistDeletes.add(playlist.playlist);
        }
        refreshList();
    }

    private void deletePendingPlaylists() {
        if (this.pendingPlaylistDeletes.isEmpty()) return;
        boolean changed = false;
        for (Playlist playlist : List.copyOf(Playlist.PLAYLISTS)) {
            if (this.pendingPlaylistDeletes.contains(playlist.playlist) && playlist.deleteCustom()) {
                changed = true;
            }
        }
        this.pendingPlaylistDeletes.clear();
        if (changed) refreshList();
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
            MaMDataConfig.Albums filter = MaMDataConfig.get().albums;
            List<DisplayEntry> entries = new ArrayList<>();

            Album.ALBUMS.stream()
                    .filter(album -> includeAlbum(album, filter))
                    .map(album -> new DisplayEntry(album, remoteForInstalledAlbum(album)))
                    .forEach(entries::add);

            Playlist.PLAYLISTS.stream()
                    .filter(playlist -> includePlaylist(playlist, filter))
                    .map(DisplayEntry::new)
                    .forEach(entries::add);

            RemoteAlbumManager.packs().stream()
                    .filter(pack -> includeRemote(pack, filter))
                    .map(DisplayEntry::new)
                    .forEach(entries::add);

            return entries;
        }

        private static RemoteAlbumPack remoteForInstalledAlbum(Album album) {
            if (!MaMClientConfig.get().remote_downloads) return null;

            return RemoteAlbumManager.packs().stream()
                    .filter(pack -> pack.id().equals(album.album))
                    .findFirst()
                    .orElse(null);
        }

        private static boolean includeAlbum(Album album, MaMDataConfig.Albums filter) {
            if (filter.favourites_only) return filter.show_albums && album.isFavourite();
            else return filter.show_albums;
        }

        private static boolean includePlaylist(Playlist playlist, MaMDataConfig.Albums filter) {
            if (playlist.hidden) return false;
            if (filter.favourites_only) return filter.show_playlists && playlist.isFavourite();
            else return filter.show_playlists;
        }

        private static boolean includeRemote(RemoteAlbumPack pack, MaMDataConfig.Albums filter) {
            if (!MaMClientConfig.get().remote_downloads || isDownloadedRemote(pack) || filter.favourites_only) return false;
            if (isDownloadedRemote(pack)) return false;
            return filter.show_remote && RemoteAlbumManager.state(pack) != RemoteAlbumManager.State.INSTALLED;
        }

        private static boolean isDownloadedRemote(RemoteAlbumPack pack) {
            return Album.ALBUMS.stream().anyMatch(album -> album.album.equals(pack.id()));
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowRight() + 6;
        }
    }

    private static class AlbumEntry extends MusicListEntry<AlbumEntry> {

        private static final int ICON_SIZE = 32;
        private static final int DETAILS_BUTTON_WIDTH = 64;
        private static final int BUTTON_GAP = 4;

        private final AlbumList list;
        private final AlbumScreen screen;
        private final Minecraft minecraft;
        private final DisplayEntry entry;
        private final IconButton favouriteButton;
        private final IconButton actionButton;
        private final Button detailsButton;
        private final IconButton remoteActionButton;

        AlbumEntry(AlbumList list, AlbumScreen screen, Minecraft minecraft, DisplayEntry entry) {
            this.list = list;
            this.screen = screen;
            this.minecraft = minecraft;
            this.entry = entry;

            this.favouriteButton = new IconButton(favouriteMessage(entry), favouriteIcon(entry), button -> {
                if (this.entry.isRemote()) return;

                setFavourite(!isFavourite());
                IconButton iconButton = (IconButton) button;
                iconButton.setIconAndTooltip(favouriteIcon(this.entry), favouriteMessage(this.entry));
                this.list.refresh();
            });
            this.favouriteButton.active = !entry.isRemote();

            this.actionButton = createActionButton(entry);

            this.detailsButton = Button.builder(this.entry.remote != null ? Component.translatable("button.music_and_melody.album_about") : Component.translatable("button.music_and_melody.album_details"), button ->
                    this.minecraft.setScreen(this.entry.remote != null
                            ? new RemoteAlbumDetailsScreen(this.screen, this.entry.remote)
                            : this.entry.album != null
                              ? new AlbumDetailsScreen(this.screen, this.entry.album)
                              : new AlbumDetailsScreen(this.screen, this.entry.playlist))
            ).size(DETAILS_BUTTON_WIDTH, 20).build();

            this.remoteActionButton = entry.remoteActionPack() != null
                    ? new IconButton(remoteActionMessage(entry.remoteActionPack()), remoteActionIcon(entry.remoteActionPack()), button -> remoteAction())
                    : null;
        }

        private IconButton createActionButton(DisplayEntry entry) {
            if (entry.isRemote()) {
                IconButton button = new IconButton(enabledMessage(false), IconButton.icon("disabled"), ignored -> {
                });
                button.active = false;
                return button;
            }

            Component message = actionMessage(entry);
            if (message == null) return null;

            return new IconButton(message, actionIcon(entry), button -> {
                toggleAction();
                IconButton iconButton = (IconButton) button;
                iconButton.setIconAndTooltip(actionIcon(this.entry), actionMessage(this.entry));
            });
        }

        @Override
        public Component getNarration() {
            Component status = this.entry.album == null
                    ? this.entry.playlist == null
                      ? Component.translatable("screen.music_and_melody.album_filter.remote")
                      : Component.translatable("button.music_and_melody.playlist")
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

            int buttonsWidth = buttonsWidth();
            int maxTextWidth = Math.max(1, this.getContentWidth() - ICON_SIZE - buttonsWidth - 26);

            FormattedCharSequence name = this.minecraft.font.split(this.entry.name(), maxTextWidth).getFirst();
            String id = this.minecraft.font.plainSubstrByWidth(this.entry.id().toString(), maxTextWidth);
            String details = this.minecraft.font.plainSubstrByWidth(details(), maxTextWidth);

            graphics.blit(
                    MusicScreenHelper.albumIcon(this.minecraft, this.entry.icon()),
                    iconX,
                    iconY,
                    0.0F,
                    0.0F,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE
            );

            graphics.drawString(this.minecraft.font, name, textX, textY, nameColor());
            graphics.drawString(this.minecraft.font, Component.literal(id).withStyle(ChatFormatting.GRAY), textX, textY + 11, 0xFFAAAAAA);
            graphics.drawString(this.minecraft.font, details, textX, textY + 22, 0xFFAAAAAA);

            int detailsX = detailsButtonX();

            if (this.remoteActionButton != null && this.entry.hasRemoteAction()) {
                updateRemoteAction();
                this.remoteActionButton.setX(detailsX - IconButton.SIZE - BUTTON_GAP);
                this.remoteActionButton.setY(this.getContentYMiddle() - 10);
                this.remoteActionButton.render(graphics, mouseX, mouseY, tickDelta);
            }

            this.detailsButton.setX(detailsX);
            this.detailsButton.setY(this.getContentYMiddle() - 10);
            this.detailsButton.render(graphics, mouseX, mouseY, tickDelta);

            if (this.favouriteButton != null) {
                this.favouriteButton.setIconAndTooltip(favouriteIcon(this.entry), favouriteMessage(this.entry));
                this.favouriteButton.active = !this.entry.isRemote();
                this.favouriteButton.setX(detailsX + DETAILS_BUTTON_WIDTH + BUTTON_GAP);
                this.favouriteButton.setY(this.getContentYMiddle() - 10);
                this.favouriteButton.render(graphics, mouseX, mouseY, tickDelta);
            }

            if (this.actionButton != null) {
                this.actionButton.setIconAndTooltip(actionIcon(this.entry), actionMessage(this.entry));
                this.actionButton.active = !this.entry.isRemote();
                this.actionButton.setX(detailsX + DETAILS_BUTTON_WIDTH + BUTTON_GAP + IconButton.SIZE + BUTTON_GAP);
                this.actionButton.setY(this.getContentYMiddle() - 10);
                this.actionButton.render(graphics, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.remoteActionButton != null && this.entry.hasRemoteAction() && this.remoteActionButton.mouseClicked(mouseX, mouseY, button)
                    || this.detailsButton.mouseClicked(mouseX, mouseY, button)
                    || this.favouriteButton != null && this.favouriteButton.mouseClicked(mouseX, mouseY, button)
                    || this.actionButton != null && this.actionButton.mouseClicked(mouseX, mouseY, button)
                    || super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if ((keyCode == 257 || keyCode == 335 || keyCode == 32) && this.entry.album != null) {
                this.toggleAlbum();
                if (this.actionButton != null) {
                    this.actionButton.setIconAndTooltip(actionIcon(this.entry), actionMessage(this.entry));
                }
                return true;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private int detailsButtonX() {
            int normalButtonsWidth = DETAILS_BUTTON_WIDTH + BUTTON_GAP + IconButton.SIZE + BUTTON_GAP + IconButton.SIZE;
            return this.getContentRight() - normalButtonsWidth;
        }

        private int buttonsWidth() {
            int normalButtonsWidth = DETAILS_BUTTON_WIDTH + BUTTON_GAP + IconButton.SIZE + BUTTON_GAP + IconButton.SIZE;

            if (this.entry.isRemote() || this.entry.hasRemoteAction()) {
                return IconButton.SIZE + BUTTON_GAP + normalButtonsWidth;
            }

            if (this.actionButton == null) {
                return DETAILS_BUTTON_WIDTH + BUTTON_GAP + IconButton.SIZE;
            }

            return normalButtonsWidth;
        }

        private boolean isFavourite() {
            return this.entry.album != null ? this.entry.album.isFavourite() : this.entry.playlist.isFavourite();
        }

        private int nameColor() {
            if (this.entry.isRemote()) return 0xFFFFFFFF;
            if (this.entry.playlist != null && this.screen.isDeletePending(this.entry.playlist)) return 0xFFFF8888;
            if (isFavourite()) return 0xFFD7D272;
            return 0xFFFFFFFF;
        }

        private void setFavourite(boolean favourite) {
            if (this.entry.album != null) {
                this.entry.album.setFavourite(favourite);
            } else {
                this.entry.playlist.setFavourite(favourite);
            }
        }

        private void toggleAlbum() {
            this.entry.album.setEnabled(!this.entry.album.isEnabled());
            this.screen.markReloadPending();
        }

        private void toggleAction() {
            if (this.entry.album != null) {
                toggleAlbum();
            } else {
                this.screen.toggleDeletePending(this.entry.playlist);
            }
        }

        private String details() {
            if (this.entry.remote != null) {
                return this.entry.remote.repository();
            }

            String tracks = count(this.entry.trackCount(), "track", "tracks");
            String discs = count(this.entry.discCount(), "disc", "discs");
            if (this.entry.discCount() == 0) return tracks;
            if (this.entry.trackCount() == 0) return discs;
            return tracks + " | " + discs;
        }

        private static String count(int count, String singular, String plural) {
            return count + " " + (count == 1 ? singular : plural);
        }

        private static Component favouriteMessage(DisplayEntry entry) {
            if (entry.isRemote()) {
                return Component.translatable("button.music_and_melody.favourite");
            }

            boolean favourite = entry.album != null ? entry.album.isFavourite() : entry.playlist.isFavourite();
            return Component.translatable(favourite ? "button.music_and_melody.unfavourite" : "button.music_and_melody.favourite");
        }

        private static ResourceLocation favouriteIcon(DisplayEntry entry) {
            if (entry.isRemote()) {
                return IconButton.icon("favourite");
            }

            boolean favourite = entry.album != null ? entry.album.isFavourite() : entry.playlist.isFavourite();
            return IconButton.icon(favourite ? "favourited" : "favourite");
        }

        private Component actionMessage(DisplayEntry entry) {
            if (entry.isRemote()) {
                return enabledMessage(false);
            }

            if (entry.album != null) return enabledMessage(entry.album.isEnabled());
            if (!entry.playlist.isCustom()) return null;

            return Component.translatable(this.screen.isDeletePending(entry.playlist) ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
        }

        private ResourceLocation actionIcon(DisplayEntry entry) {
            if (entry.isRemote()) {
                return IconButton.icon("disabled");
            }

            if (entry.album != null) return IconButton.icon(entry.album.isEnabled() ? "enabled" : "disabled");
            return IconButton.icon(this.screen.isDeletePending(entry.playlist) ? "restore" : "delete");
        }

        private static Component enabledMessage(boolean enabled) {
            return Component.translatable(enabled ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled");
        }

        private void remoteAction() {
            RemoteAlbumPack pack = this.entry.remoteActionPack();
            if (pack == null) return;

            RemoteAlbumManager.State state = RemoteAlbumManager.state(pack);

            if (state == RemoteAlbumManager.State.REMOTE
                    || state == RemoteAlbumManager.State.UPDATE_AVAILABLE
                    || state == RemoteAlbumManager.State.FAILED) {
                RemoteAlbumManager.download(pack);
            } else if (state == RemoteAlbumManager.State.NEEDS_RELOAD) {
                this.screen.reloadResources();
            }

            updateRemoteAction();
        }

        private void updateRemoteAction() {
            RemoteAlbumPack pack = this.entry.remoteActionPack();
            if (pack == null) return;

            RemoteAlbumManager.State state = RemoteAlbumManager.state(pack);

            this.remoteActionButton.setIconAndTooltip(
                    remoteActionIcon(pack),
                    remoteActionMessage(pack)
            );

            this.remoteActionButton.active = state != RemoteAlbumManager.State.DOWNLOADING
                    && state != RemoteAlbumManager.State.INSTALLED;
        }

        private static Component remoteActionMessage(RemoteAlbumPack pack) {
            return switch (RemoteAlbumManager.state(pack)) {
                case DOWNLOADING -> Component.translatable("button.music_and_melody.downloading");
                case NEEDS_RELOAD -> Component.translatable("button.music_and_melody.reload");
                case UPDATE_AVAILABLE -> Component.translatable("button.music_and_melody.update");
                case FAILED -> Component.translatable("button.music_and_melody.retry");
                default -> Component.translatable("button.music_and_melody.download");
            };
        }

        private static ResourceLocation remoteActionIcon(RemoteAlbumPack pack) {
            return switch (RemoteAlbumManager.state(pack)) {
                case DOWNLOADING -> IconButton.icon("downloading");
                case NEEDS_RELOAD -> IconButton.icon("reload");
                case UPDATE_AVAILABLE -> IconButton.icon("update");
                case FAILED -> IconButton.icon("retry_download");
                default -> IconButton.icon("download");
            };
        }

        private static String remoteStateName(RemoteAlbumManager.State state) {
            return Component.translatable("screen.music_and_melody.remote_album.state." + state.name().toLowerCase()).getString();
        }
    }

    private static class DisplayEntry {
        private final Album album;
        private final Playlist playlist;
        private final RemoteAlbumPack remote;
        private final RemoteAlbumPack installedRemote;

        DisplayEntry(Album album) {
            this(album, null);
        }

        DisplayEntry(Album album, RemoteAlbumPack installedRemote) {
            this.album = album;
            this.playlist = null;
            this.remote = null;
            this.installedRemote = installedRemote;
        }

        DisplayEntry(Playlist playlist) {
            this.album = null;
            this.playlist = playlist;
            this.remote = null;
            this.installedRemote = null;
        }

        DisplayEntry(RemoteAlbumPack remote) {
            this.album = null;
            this.playlist = null;
            this.remote = remote;
            this.installedRemote = null;
        }

        boolean isRemote() {
            return this.remote != null;
        }

        boolean hasRemoteAction() {
            RemoteAlbumPack pack = remoteActionPack();
            if (pack == null) return false;

            RemoteAlbumManager.State state = RemoteAlbumManager.state(pack);

            if (this.remote != null) {
                return state == RemoteAlbumManager.State.REMOTE
                        || state == RemoteAlbumManager.State.DOWNLOADING
                        || state == RemoteAlbumManager.State.NEEDS_RELOAD
                        || state == RemoteAlbumManager.State.FAILED;
            }

            return this.installedRemote != null && (
                    state == RemoteAlbumManager.State.UPDATE_AVAILABLE
                            || state == RemoteAlbumManager.State.DOWNLOADING
                            || state == RemoteAlbumManager.State.NEEDS_RELOAD
                            || state == RemoteAlbumManager.State.FAILED
            );
        }

        RemoteAlbumPack remoteActionPack() {
            if (this.remote != null) return this.remote;
            return this.installedRemote;
        }

        Component name() {
            return this.album != null ? this.album.name : this.playlist != null ? this.playlist.name : this.remote.name();
        }

        ResourceLocation id() {
            return this.album != null ? this.album.album : this.playlist != null ? this.playlist.playlist : this.remote.id();
        }

        ResourceLocation icon() {
            return this.album != null ? this.album.icon : this.playlist != null ? this.playlist.icon : this.remote.icon();
        }

        int trackCount() {
            return this.album != null ? this.album.tracks.size() : this.playlist != null ? this.playlist.tracks.size() : 0;
        }

        int discCount() {
            return this.album != null ? this.album.discs.size() : this.playlist != null ? this.playlist.discs.size() : 0;
        }
    }
}
