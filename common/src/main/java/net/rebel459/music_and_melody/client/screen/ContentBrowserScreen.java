package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

public class ContentBrowserScreen extends Screen {

    static final int MAIN_BUTTON_ROW_WIDTH = 308;
    private static final Component TITLE = Component.translatable("screen.music_and_melody.albums");
    private final Screen parent;
    private final Set<Identifier> pendingPlaylistDeletes = new HashSet<>();
    private final Set<Identifier> pendingRemoteDeletes = new HashSet<>();
    private AlbumList list;
    private MaMDataConfig.BrowserTab tab;
    private boolean catalogRefreshing;
    private boolean reloadPending;
    private String search = "";
    private Button sortButton;

    public ContentBrowserScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.tab = MaMDataConfig.get().albums.browser_tab;
    }

    @Override
    protected void init() {
        RemoteContentManager.refreshIfNeeded();
        this.catalogRefreshing = RemoteContentManager.isRefreshing();

        int rowX = this.width / 2 - MAIN_BUTTON_ROW_WIDTH / 2;
        int topY = 31;
        int tabWidth = (MAIN_BUTTON_ROW_WIDTH - 8) / 3;
        addTabButton(MaMDataConfig.BrowserTab.ALBUMS, rowX, topY, tabWidth);
        addTabButton(MaMDataConfig.BrowserTab.PLAYLISTS, rowX + tabWidth + 4, topY, tabWidth);
        addTabButton(MaMDataConfig.BrowserTab.REMOTE, rowX + (tabWidth + 4) * 2, topY, tabWidth);

        this.list = this.addRenderableWidget(new AlbumList(this, this.minecraft, this.width, this.height - 112, 56));

        int searchY = this.height - 51;
        int buttonY = this.height - 27;
        EditBox searchField = this.addRenderableWidget(new EditBox(this.font, rowX, searchY, MAIN_BUTTON_ROW_WIDTH, 20, Component.translatable("screen.music_and_melody.search")));
        searchField.setValue(this.search);
        searchField.setResponder(value -> {
            this.search = value;
            refreshList();
        });

        this.sortButton = this.addRenderableWidget(Button.builder(sortMessage(), button -> {
                    toggleSortPriority();
                    this.sortButton.setMessage(sortMessage());
                    refreshList();
                })
                .bounds(rowX, buttonY, 152, 20)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + 156, buttonY, 152, 20)
                .build());
        MusicScreenHelper.addSocialButtons(this);
    }

    private void addTabButton(MaMDataConfig.BrowserTab tab, int x, int y, int width) {
        Button button = this.addRenderableWidget(Button.builder(tabMessage(tab), ignored -> {
                    this.setTab(tab);
                    this.rebuildWidgets();
                })
                .bounds(x, y, width, 20)
                .build());
        button.active = this.tab != tab;
    }

    private void setTab(MaMDataConfig.BrowserTab tab) {
        this.tab = tab;
        MaMDataConfig.get().albums.browser_tab = tab;
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private Component sortMessage() {
        MaMDataConfig.Albums albums = MaMDataConfig.get().albums;
        if (this.tab == MaMDataConfig.BrowserTab.REMOTE) {
            return Component.translatable(albums.downloads_first ? "button.music_and_melody.downloads_first" : "button.music_and_melody.downloads_last");
        }
        return Component.translatable(albums.favourites_first ? "button.music_and_melody.favourites_first" : "button.music_and_melody.favourites_last");
    }

    private void toggleSortPriority() {
        MaMDataConfig.Albums albums = MaMDataConfig.get().albums;
        if (this.tab == MaMDataConfig.BrowserTab.REMOTE) {
            albums.downloads_first = !albums.downloads_first;
        } else {
            albums.favourites_first = !albums.favourites_first;
        }
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, TEXT_TITLE);
        if (this.catalogRefreshing && !RemoteContentManager.isRefreshing()) {
            this.catalogRefreshing = false;
            refreshList();
        }
    }

    @Override
    public void onClose() {
        deletePendingPlaylists();
        deletePendingRemotes();
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
            RemoteContentManager.markReloaded();
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

    private boolean isDeletePending(RemotePack pack) {
        return this.pendingRemoteDeletes.contains(pack.id());
    }

    private void toggleDeletePending(RemotePack pack) {
        if (!remoteDeleteAvailable(pack)) return;
        if (!this.pendingRemoteDeletes.remove(pack.id())) {
            this.pendingRemoteDeletes.add(pack.id());
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

    private void deletePendingRemotes() {
        if (this.pendingRemoteDeletes.isEmpty()) return;
        boolean changed = false;
        for (Identifier id : List.copyOf(this.pendingRemoteDeletes)) {
            if (RemoteContentManager.deleteInstalled(id)) {
                changed = true;
            }
        }
        this.pendingRemoteDeletes.clear();
        if (changed) {
            this.reloadPending = true;
            refreshList();
        }
    }

    private static boolean remoteDeleteAvailable(RemotePack pack) {
        RemoteContentManager.State state = RemoteContentManager.state(pack);
        return state == RemoteContentManager.State.INSTALLED
                || state == RemoteContentManager.State.UPDATE_AVAILABLE
                || state == RemoteContentManager.State.NEEDS_RELOAD;
    }

    private static Component tabMessage(MaMDataConfig.BrowserTab tab) {
        return switch (tab) {
            case ALBUMS -> Component.translatable("button.music_and_melody.albums");
            case PLAYLISTS -> Component.translatable("button.music_and_melody.playlists");
            case REMOTE -> Component.translatable("button.music_and_melody.remote");
        };
    }

    private static class AlbumList extends ObjectSelectionList<AlbumEntry> {

        private final ContentBrowserScreen screen;

        AlbumList(ContentBrowserScreen screen, Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 46);
            this.screen = screen;
            this.centerListVertically = false;
            refresh();
        }

        private void refresh() {
            this.clearEntries();
            entries().stream()
                    .filter(this::matchesSearch)
                    .sorted(this.comparator())
                    .map(entry -> new AlbumEntry(this, this.screen, this.minecraft, entry))
                    .forEach(this::addEntry);
        }

        private Comparator<DisplayEntry> comparator() {
            Comparator<DisplayEntry> byName = Comparator.comparing(entry -> entry.name().getString(), String.CASE_INSENSITIVE_ORDER);
            MaMDataConfig.Albums albums = MaMDataConfig.get().albums;
            if (this.screen.tab == MaMDataConfig.BrowserTab.REMOTE) {
                Comparator<DisplayEntry> priority = Comparator.comparingInt(entry -> entry.remotePriority(albums.downloads_first));
                return priority.thenComparing(byName);
            }
            Comparator<DisplayEntry> priority = Comparator.comparing(DisplayEntry::favourite);
            if (albums.favourites_first) priority = priority.reversed();
            return priority.thenComparing(byName);
        }

        private boolean matchesSearch(DisplayEntry entry) {
            String query = this.screen.search.trim().toLowerCase(Locale.ROOT);
            if (query.isEmpty()) return true;
            return entry.name().getString().toLowerCase(Locale.ROOT).contains(query)
                    || entry.id().toString().toLowerCase(Locale.ROOT).contains(query)
                    || entry.details().toLowerCase(Locale.ROOT).contains(query);
        }

        private List<DisplayEntry> entries() {
            List<DisplayEntry> entries = new ArrayList<>();
            switch (this.screen.tab) {
                case ALBUMS -> Album.ALBUMS.stream()
                        .map(album -> new DisplayEntry(album, remoteForInstalledAlbum(album)))
                        .forEach(entries::add);
                case PLAYLISTS -> Playlist.PLAYLISTS.stream()
                        .filter(playlist -> !playlist.hidden)
                        .map(DisplayEntry::new)
                        .forEach(entries::add);
                case REMOTE -> {
                    if (MaMClientConfig.get().remote_downloads) {
                        RemoteContentManager.packs().stream()
                                .map(DisplayEntry::new)
                                .forEach(entries::add);
                    }
                }
            }
            return entries;
        }

        private static RemotePack remoteForInstalledAlbum(Album album) {
            if (!MaMClientConfig.get().remote_downloads) return null;

            return RemoteContentManager.packs().stream()
                    .filter(pack -> pack.id().equals(album.album))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int getRowWidth() {
            return Math.min(500, this.width - 20);
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 6;
        }
    }

    private static class AlbumEntry extends ObjectSelectionList.Entry<AlbumEntry> {

        private static final int ICON_SIZE = 32;
        private static final int DETAILS_BUTTON_WIDTH = 64;
        private static final int BUTTON_GAP = 4;

        private final AlbumList list;
        private final ContentBrowserScreen screen;
        private final Minecraft minecraft;
        private final DisplayEntry entry;
        private final Button detailsButton;
        private final IconButton secondButton;
        private final IconButton thirdButton;

        AlbumEntry(AlbumList list, ContentBrowserScreen screen, Minecraft minecraft, DisplayEntry entry) {
            this.list = list;
            this.screen = screen;
            this.minecraft = minecraft;
            this.entry = entry;

            this.detailsButton = Button.builder(detailsMessage(entry), button -> openDetails())
                    .size(DETAILS_BUTTON_WIDTH, 20)
                    .build();
            this.secondButton = createSecondButton();
            this.thirdButton = createThirdButton();
        }

        private void openDetails() {
            this.minecraft.setScreen(this.entry.remote != null
                    ? new RemoteDetailsScreen(this.screen, this.entry.remote)
                    : this.entry.album != null
                      ? new AlbumDetailsScreen(this.screen, this.entry.album)
                      : new AlbumDetailsScreen(this.screen, this.entry.playlist));
        }

        private IconButton createSecondButton() {
            if (this.entry.isRemote()) {
                IconButton button = new IconButton(remoteActionMessage(this.entry.remote), remoteActionIcon(this.entry.remote), ignored -> remoteAction());
                updateRemoteAction(button);
                return button;
            }

            return new IconButton(favouriteMessage(this.entry), favouriteIcon(this.entry), button -> {
                setFavourite(!this.entry.favourite());
                ((IconButton) button).setIconAndTooltip(favouriteIcon(this.entry), favouriteMessage(this.entry));
                this.list.refresh();
            });
        }

        private IconButton createThirdButton() {
            if (this.entry.isRemote()) {
                IconButton button = new IconButton(remoteDeleteMessage(this.screen, this.entry.remote), remoteDeleteIcon(this.screen, this.entry.remote), ignored -> {
                    this.screen.toggleDeletePending(this.entry.remote);
                });
                updateRemoteDelete(button);
                return button;
            }

            if (this.entry.album != null) {
                return new IconButton(albumEnabledMessage(this.entry.album), albumEnabledIcon(this.entry.album), button -> {
                    toggleAlbum();
                    ((IconButton) button).setIconAndTooltip(albumEnabledIcon(this.entry.album), albumEnabledMessage(this.entry.album));
                    this.list.refresh();
                });
            }

            if (!this.entry.playlist.isCustom()) return null;
            return new IconButton(playlistDeleteMessage(this.entry.playlist), playlistDeleteIcon(this.entry.playlist), button -> {
                this.screen.toggleDeletePending(this.entry.playlist);
                ((IconButton) button).setIconAndTooltip(playlistDeleteIcon(this.entry.playlist), playlistDeleteMessage(this.entry.playlist));
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int iconX = this.getContentX() + 1;
            int iconY = this.getContentYMiddle() - ICON_SIZE / 2;
            int textX = iconX + ICON_SIZE + 7;
            int textY = this.getContentYMiddle() - 15;
            int buttonsWidth = DETAILS_BUTTON_WIDTH + BUTTON_GAP + IconButton.SIZE + BUTTON_GAP + IconButton.SIZE;
            int maxTextWidth = Math.max(1, this.getContentWidth() - ICON_SIZE - buttonsWidth - 26);

            FormattedCharSequence name = this.minecraft.font.split(this.entry.name(), maxTextWidth).getFirst();
            String id = this.minecraft.font.plainSubstrByWidth(this.entry.id().toString(), maxTextWidth);
            String details = this.minecraft.font.plainSubstrByWidth(this.entry.details(), maxTextWidth);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
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

            graphics.text(this.minecraft.font, name, textX, textY, nameColor());
            graphics.text(this.minecraft.font, Component.literal(id), textX, textY + 11, TEXT_DESCRIPTION);
            graphics.text(this.minecraft.font, details, textX, textY + 22, TEXT_DESCRIPTION);

            int detailsX = this.getContentRight() - buttonsWidth;

            this.detailsButton.setX(detailsX);
            this.detailsButton.setY(this.getContentYMiddle() - 10);
            this.detailsButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);

            this.secondButton.setX(detailsX + DETAILS_BUTTON_WIDTH + BUTTON_GAP);
            this.secondButton.setY(this.getContentYMiddle() - 10);
            if (this.entry.isRemote()) updateRemoteAction(this.secondButton);
            this.secondButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);

            int thirdX = detailsX + DETAILS_BUTTON_WIDTH + BUTTON_GAP + IconButton.SIZE + BUTTON_GAP;
            int thirdY = this.getContentYMiddle() - 10;
            if (this.thirdButton != null) {
                this.thirdButton.setX(thirdX);
                this.thirdButton.setY(thirdY);
                if (this.entry.isRemote()) updateRemoteDelete(this.thirdButton);
                this.thirdButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            } else if (this.entry.isBuiltInPlaylist()) {
                IconButton.renderIconWithTooltip(graphics, IconButton.icon("built_in"), thirdX, thirdY, Component.translatable("screen.music_and_melody.events.built_in"), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.detailsButton.mouseClicked(event, doubleClick)
                    || this.secondButton.mouseClicked(event, doubleClick)
                    || this.thirdButton != null && this.thirdButton.mouseClicked(event, doubleClick)
                    || super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation() && this.entry.album != null) {
                this.toggleAlbum();
                if (this.thirdButton != null) {
                    this.thirdButton.setIconAndTooltip(albumEnabledIcon(this.entry.album), albumEnabledMessage(this.entry.album));
                }
                return true;
            }

            return super.keyPressed(event);
        }

        private int nameColor() {
            if (this.entry.playlist != null && this.screen.isDeletePending(this.entry.playlist)) return TEXT_PENDING_DELETION;
            if (this.entry.remote != null && this.screen.isDeletePending(this.entry.remote)) return TEXT_PENDING_DELETION;
            if (this.entry.favourite()) return TEXT_FAVOURITE;
            return TEXT_TITLE;
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

        private void remoteAction() {
            RemotePack pack = this.entry.remote;
            RemoteContentManager.State state = RemoteContentManager.state(pack);

            if (!RemoteContentManager.remoteDownloadsAllowed()) {
                if (state == RemoteContentManager.State.NEEDS_RELOAD) {
                    this.screen.reloadResources();
                } else {
                    this.minecraft.setScreen(new PlatformDownloadScreen(this.screen, pack));
                }
                return;
            }

            if (state == RemoteContentManager.State.REMOTE
                    || state == RemoteContentManager.State.UPDATE_AVAILABLE
                    || state == RemoteContentManager.State.FAILED) {
                RemoteContentManager.download(pack);
            } else if (state == RemoteContentManager.State.NEEDS_RELOAD) {
                this.screen.reloadResources();
            }

            updateRemoteAction(this.secondButton);
            updateRemoteDelete(this.thirdButton);
        }

        private void updateRemoteAction(IconButton button) {
            button.setIconAndTooltip(remoteActionIcon(this.entry.remote), remoteActionMessage(this.entry.remote));
            RemoteContentManager.State state = RemoteContentManager.state(this.entry.remote);
            button.active = state == RemoteContentManager.State.REMOTE
                    || state == RemoteContentManager.State.UPDATE_AVAILABLE
                    || state == RemoteContentManager.State.FAILED
                    || state == RemoteContentManager.State.NEEDS_RELOAD;
        }

        private void updateRemoteDelete(IconButton button) {
            button.setIconAndTooltip(remoteDeleteIcon(this.screen, this.entry.remote), remoteDeleteMessage(this.screen, this.entry.remote));
            button.active = this.screen.isDeletePending(this.entry.remote) || remoteDeleteActive(this.entry.remote);
        }

        private static Component detailsMessage(DisplayEntry entry) {
            return Component.translatable(entry.isRemote() ? "button.music_and_melody.album_about" : "button.music_and_melody.album_details");
        }

        private static Component favouriteMessage(DisplayEntry entry) {
            return Component.translatable(entry.favourite() ? "button.music_and_melody.unfavourite" : "button.music_and_melody.favourite");
        }

        private static Identifier favouriteIcon(DisplayEntry entry) {
            return IconButton.icon(entry.favourite() ? "favourited" : "favourite");
        }

        private static Component albumEnabledMessage(Album album) {
            return Component.translatable(album.isEnabled() ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled");
        }

        private static Identifier albumEnabledIcon(Album album) {
            return IconButton.icon(album.isEnabled() ? "enabled" : "disabled");
        }

        private Component playlistDeleteMessage(Playlist playlist) {
            return Component.translatable(this.screen.isDeletePending(playlist) ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
        }

        private Identifier playlistDeleteIcon(Playlist playlist) {
            return IconButton.icon(this.screen.isDeletePending(playlist) ? "restore" : "delete");
        }

        private static Component remoteActionMessage(RemotePack pack) {
            return switch (RemoteContentManager.state(pack)) {
                case DOWNLOADING -> Component.translatable("button.music_and_melody.downloading");
                case NEEDS_RELOAD -> Component.translatable("button.music_and_melody.reload");
                case UPDATE_AVAILABLE -> Component.translatable("button.music_and_melody.update");
                case FAILED -> Component.translatable("button.music_and_melody.retry");
                case INSTALLED -> Component.translatable("screen.music_and_melody.remote_album.state.installed");
                case REMOTE -> Component.translatable("button.music_and_melody.download");
            };
        }

        private static Identifier remoteActionIcon(RemotePack pack) {
            return switch (RemoteContentManager.state(pack)) {
                case DOWNLOADING -> IconButton.icon("downloading");
                case NEEDS_RELOAD -> IconButton.icon("reload");
                case UPDATE_AVAILABLE -> IconButton.icon("update");
                case FAILED -> IconButton.icon("retry_download");
                case INSTALLED -> IconButton.icon("enabled");
                case REMOTE -> IconButton.icon("download");
            };
        }

        private static Component remoteDeleteMessage(ContentBrowserScreen screen, RemotePack pack) {
            return Component.translatable(screen.isDeletePending(pack) ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
        }

        private static Identifier remoteDeleteIcon(ContentBrowserScreen screen, RemotePack pack) {
            return IconButton.icon(screen.isDeletePending(pack) ? "restore" : "delete");
        }

        private static boolean remoteDeleteActive(RemotePack pack) {
            RemoteContentManager.State state = RemoteContentManager.state(pack);
            return state == RemoteContentManager.State.INSTALLED
                    || state == RemoteContentManager.State.UPDATE_AVAILABLE
                    || state == RemoteContentManager.State.NEEDS_RELOAD;
        }
    }

    private static class DisplayEntry {
        private final Album album;
        private final Playlist playlist;
        private final RemotePack remote;
        private final RemotePack installedRemote;

        DisplayEntry(Album album, RemotePack installedRemote) {
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

        DisplayEntry(RemotePack remote) {
            this.album = null;
            this.playlist = null;
            this.remote = remote;
            this.installedRemote = null;
        }

        boolean isRemote() {
            return this.remote != null;
        }

        boolean isBuiltInPlaylist() {
            return this.playlist != null && !this.playlist.isCustom();
        }

        boolean favourite() {
            return this.album != null ? this.album.isFavourite() : this.playlist != null && this.playlist.isFavourite();
        }

        boolean downloaded() {
            if (this.remote == null) return false;
            RemoteContentManager.State state = RemoteContentManager.state(this.remote);
            return state == RemoteContentManager.State.INSTALLED
                    || state == RemoteContentManager.State.UPDATE_AVAILABLE
                    || state == RemoteContentManager.State.NEEDS_RELOAD;
        }

        int remotePriority(boolean downloadsFirst) {
            if (this.remote == null) return 0;
            RemoteContentManager.State state = RemoteContentManager.state(this.remote);
            if (!downloadsFirst && state == RemoteContentManager.State.REMOTE) return 0;
            int offset = downloadsFirst ? 0 : 1;
            return switch (state) {
                case FAILED -> offset;
                case NEEDS_RELOAD -> offset + 1;
                case UPDATE_AVAILABLE -> offset + 2;
                case DOWNLOADING -> offset + 3;
                case INSTALLED -> offset + 4;
                case REMOTE -> downloadsFirst ? 5 : 0;
            };
        }

        Component name() {
            return this.album != null ? this.album.name : this.playlist != null ? this.playlist.name : this.remote.name();
        }

        Identifier id() {
            return this.album != null ? this.album.album : this.playlist != null ? this.playlist.playlist : this.remote.id();
        }

        Identifier icon() {
            return this.album != null ? this.album.icon : this.playlist != null ? this.playlist.icon : RemoteIconManager.icon(this.remote);
        }

        String details() {
            if (this.remote != null) {
                RemoteContentManager.State state = RemoteContentManager.state(this.remote);
                return this.remote.repository() + " | " + Component.translatable("screen.music_and_melody.remote_album.state." + state.name().toLowerCase(Locale.ROOT)).getString();
            }

            String tracks = count(trackCount(), "track", "tracks");
            String discs = count(discCount(), "disc", "discs");
            if (discCount() == 0) return tracks;
            if (trackCount() == 0) return discs;
            return tracks + " | " + discs;
        }

        int trackCount() {
            return this.album != null ? this.album.tracks.size() : this.playlist != null ? this.playlist.tracks.size() : 0;
        }

        int discCount() {
            return this.album != null ? this.album.discs.size() : this.playlist != null ? this.playlist.discs.size() : 0;
        }

        private static String count(int count, String singular, String plural) {
            return count + " " + (count == 1 ? singular : plural);
        }
    }
}
