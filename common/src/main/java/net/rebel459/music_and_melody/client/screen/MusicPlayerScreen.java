package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Event;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.config.MaMServerConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static net.rebel459.music_and_melody.client.util.ScreenConstants.*;

/**
 * The compact, persistent music workspace.  It intentionally keeps the
 * left-hand queue and the transport controls alive while only the middle and
 * right panels change page.
 */
public class MusicPlayerScreen extends Screen {

    public enum Page {
        NOW_PLAYING,
        LIBRARY,
        DETAILS,
        EVENTS,
        THEMES,
        ONLINE,
        HOME,
        CONFIG
    }

    private static final Component TITLE = Component.translatable("screen.music_and_melody.music_player");
    private static final int OUTER_MARGIN = 10;
    private static final int PANEL_GAP = 7;
    private static final int PANEL_TOP = OUTER_MARGIN;
    private static final int PANEL_BOTTOM_MARGIN = 10;
    private static final int BOTTOM_PANEL_HEIGHT = 56;
    private static final int REFERENCE_WORKSPACE_WIDTH = 620;
    private static final int MIN_LEFT_WIDTH = 112;
    private static final int MIN_MIDDLE_WIDTH = 180;
    private static final int MIN_RIGHT_WIDTH = 124;
    private static final int TRACK_ROW_HEIGHT = 24;
    private static final int TRACK_NUMBER_OFFSET = 15;
    private static final int TRACK_TEXT_OFFSET = 32;
    private static final int HOME_BUTTON_COUNT = 6;
    private static final int HOME_BUTTON_STEP = 29;
    private static final int HOME_MENU_HEIGHT = 22 + (HOME_BUTTON_COUNT - 1) * HOME_BUTTON_STEP;

    private final Screen parent;
    private Page page;

    private int leftX;
    private int leftWidth;
    private int middleX;
    private int middleWidth;
    private int rightX;
    private int rightWidth;
    private int panelBottom;
    private int contentBottom;
    private int bottomPanelTop;
    private int layoutWidth;
    private int layoutHeight;

    private CurrentSourceCard currentSourceCard;
    private WorkspaceButton customPlaylistButton;
    private QueueList mainQueueList;
    private FavouriteList favouriteList;
    private LibraryList libraryList;
    private ContentTrackList contentTrackList;
    private EventFolderList eventFolderList;
    private EventSourceList eventSourceList;
    private OnlineCatalogList onlineCatalogList;
    private OnlinePackList onlinePackList;
    private TagFilterList<?> tagFilterList;

    private IconButton searchButton;
    private EditBox searchField;
    private IconButton shuffleButton;
    private IconButton previousButton;
    private IconButton playPauseButton;
    private IconButton nextButton;
    private IconButton loopButton;
    private IconButton saveButton;
    private IconButton clearButton;
    private WorkspaceButton vanillaMusicButton;
    private WorkspaceButton eventsButton;
    private WorkspaceButton loadButton;
    private WorkspaceButton queueButton;
    private WorkspaceButton remoteActionButton;
    private WorkspaceButton remoteDeleteButton;
    private WorkspaceButton remoteBackButton;
    private IconButton backButton;

    private boolean searching;
    private boolean focusSearchField;
    private String search = "";
    private boolean draggingVolume;
    private boolean draggingProgress;
    private long seekPreviewMillis;
    private boolean reloadPending;
    private int draggingQueueIndex = -1;
    private QueueList draggingQueueList;

    private final Set<LibraryTag> libraryTags = EnumSet.noneOf(LibraryTag.class);
    private final Set<EventTag> eventTags = EnumSet.noneOf(EventTag.class);
    private final Set<OnlineTag> onlineTags = EnumSet.noneOf(OnlineTag.class);
    private String selectedEventNamespace;
    /** null = choose a catalog, empty = All, any other value = one catalog. */
    private String selectedOnlineCatalog;
    private ContentItem viewedContent;
    private RemotePack viewedRemotePack;
    private final Set<Identifier> pendingRemoteDeletes = new HashSet<>();
    private final List<BreadcrumbHit> breadcrumbHits = new ArrayList<>();

    public MusicPlayerScreen(Screen parent) {
        this(parent, Page.NOW_PLAYING);
    }

    public MusicPlayerScreen(Screen parent, Page page) {
        super(TITLE);
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        // A named source is viewed through its ordinary Album/Playlist page.
        // Only the source-less Custom Playlist owns the editable queue view.
        if (this.page == Page.NOW_PLAYING && !PlaylistHelper.isQueueCustom()) {
            ContentItem source = currentSourceContent();
            if (source != null) {
                this.viewedContent = source;
                this.page = Page.DETAILS;
            }
        }
        calculateLayout();
        MusicDiscHelper.requestStats(this.minecraft);
        RemoteContentManager.refreshIfNeeded();
        this.tagFilterList = null;

        // Rendered first, before every list and widget added below.
        this.addRenderableOnly(this::renderShell);
        buildLeftPanel();
        buildPlaybackStrip();
        buildPage();
    }

    @Override
    protected void rebuildWidgets() {
        double favouriteScroll = this.favouriteList == null ? 0.0D : this.favouriteList.scrollAmount();
        double tagFilterScroll = this.tagFilterList == null ? 0.0D : this.tagFilterList.scrollAmount();
        Page pageBeforeRebuild = this.page;
        double pageScroll = currentPageScrollAmount();

        super.rebuildWidgets();

        if (this.favouriteList != null) this.favouriteList.setScrollAmount(favouriteScroll);
        if (this.tagFilterList != null) this.tagFilterList.setScrollAmount(tagFilterScroll);
        if (this.page == pageBeforeRebuild) restoreCurrentPageScroll(pageScroll);
    }

    @Override
    protected void repositionElements() {
        calculateLayout();
        rebuildWidgets();
    }

    private double currentPageScrollAmount() {
        return switch (this.page) {
            case NOW_PLAYING -> this.mainQueueList == null ? 0.0D : this.mainQueueList.scrollAmount();
            case LIBRARY -> this.libraryList == null ? 0.0D : this.libraryList.scrollAmount();
            case DETAILS -> this.contentTrackList == null ? 0.0D : this.contentTrackList.scrollAmount();
            case EVENTS -> {
                PanelList<?> list = this.selectedEventNamespace == null ? this.eventFolderList : this.eventSourceList;
                yield list == null ? 0.0D : list.scrollAmount();
            }
            case ONLINE -> {
                PanelList<?> list = this.selectedOnlineCatalog == null ? this.onlineCatalogList : this.onlinePackList;
                yield list == null ? 0.0D : list.scrollAmount();
            }
            case HOME, THEMES, CONFIG -> 0.0D;
        };
    }

    private void restoreCurrentPageScroll(double scrollAmount) {
        switch (this.page) {
            case NOW_PLAYING -> {
                if (this.mainQueueList != null) this.mainQueueList.setScrollAmount(scrollAmount);
            }
            case LIBRARY -> {
                if (this.libraryList != null) this.libraryList.setScrollAmount(scrollAmount);
            }
            case DETAILS -> {
                if (this.contentTrackList != null) this.contentTrackList.setScrollAmount(scrollAmount);
            }
            case EVENTS -> {
                PanelList<?> list = this.selectedEventNamespace == null ? this.eventFolderList : this.eventSourceList;
                if (list != null) list.setScrollAmount(scrollAmount);
            }
            case ONLINE -> {
                PanelList<?> list = this.selectedOnlineCatalog == null ? this.onlineCatalogList : this.onlinePackList;
                if (list != null) list.setScrollAmount(scrollAmount);
            }
            case HOME, THEMES, CONFIG -> {
            }
        }
    }

    private void calculateLayout() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
        this.panelBottom = this.layoutHeight - PANEL_BOTTOM_MARGIN;
        this.bottomPanelTop = this.panelBottom - BOTTOM_PANEL_HEIGHT;
        this.contentBottom = this.bottomPanelTop - PANEL_GAP;

        int workspaceWidth = Math.max(3, this.layoutWidth - OUTER_MARGIN * 2);
        int workspaceX = (this.layoutWidth - workspaceWidth) / 2;
        int usableWidth = Math.max(3, workspaceWidth - PANEL_GAP * 2);
        int preferredMinimum = MIN_LEFT_WIDTH + MIN_MIDDLE_WIDTH + MIN_RIGHT_WIDTH;

        if (usableWidth < preferredMinimum) {
            // At large GUI scales, keep all three panels inside the vanilla
            // logical viewport and reduce them proportionally.
            this.leftWidth = Math.max(1, Math.round(usableWidth * (MIN_LEFT_WIDTH / (float) preferredMinimum)));
            this.rightWidth = Math.max(1, Math.round(usableWidth * (MIN_RIGHT_WIDTH / (float) preferredMinimum)));
            this.middleWidth = Math.max(1, usableWidth - this.leftWidth - this.rightWidth);
        } else if (workspaceWidth <= REFERENCE_WORKSPACE_WIDTH) {
            int viewportEquivalentWidth = workspaceWidth + OUTER_MARGIN * 2;
            this.leftWidth = clamp((int) (viewportEquivalentWidth * 0.23F), 132, 210);
            this.rightWidth = clamp((int) (viewportEquivalentWidth * 0.20F), 144, 214);
            this.middleWidth = usableWidth - this.leftWidth - this.rightWidth;
            if (this.middleWidth < MIN_MIDDLE_WIDTH) {
                int shortfall = MIN_MIDDLE_WIDTH - this.middleWidth;
                int fromLeft = Math.min(shortfall / 2, Math.max(0, this.leftWidth - MIN_LEFT_WIDTH));
                int fromRight = Math.min(shortfall - fromLeft, Math.max(0, this.rightWidth - MIN_RIGHT_WIDTH));
                this.leftWidth -= fromLeft;
                this.rightWidth -= fromRight;
                this.middleWidth = usableWidth - this.leftWidth - this.rightWidth;
            }
        } else {
            // Below the scale-7 reference, vanilla exposes a wider logical
            // viewport. Grow the workspace and its columns together instead
            // of leaving a fixed-width island in the middle of the screen.
            this.leftWidth = Math.round(workspaceWidth * (147.0F / REFERENCE_WORKSPACE_WIDTH));
            this.rightWidth = Math.round(workspaceWidth * (144.0F / REFERENCE_WORKSPACE_WIDTH));
            this.middleWidth = usableWidth - this.leftWidth - this.rightWidth;
        }

        this.leftX = workspaceX;
        this.middleX = this.leftX + this.leftWidth + PANEL_GAP;
        this.rightX = this.middleX + this.middleWidth + PANEL_GAP;
    }

    private void buildLeftPanel() {
        int sourceTop = PANEL_TOP + 31;
        int favouriteHeaderY = sourceTop + 75;
        int favouriteTop = favouriteHeaderY + 16;

        this.currentSourceCard = this.addRenderableWidget(new CurrentSourceCard(this));
        this.currentSourceCard.setX(this.leftX + 5);
        this.currentSourceCard.setY(sourceTop);
        this.currentSourceCard.setSize(this.leftWidth - 10, 43);
        this.customPlaylistButton = this.addRenderableWidget(new WorkspaceButton(this.leftX + 5, sourceTop + 47,
                this.leftWidth - 10, 20, Component.translatable("screen.music_and_melody.custom_playlist"), false,
                ignored -> loadLastCustomPlaylist()));
        this.favouriteList = this.addRenderableWidget(new FavouriteList(this, this.minecraft, this.leftX, this.leftWidth, favouriteTop, this.panelBottom - 5));
    }

    private void buildPlaybackStrip() {
        int buttonY = this.bottomPanelTop + 29;
        int groupWidth = IconButton.SIZE * 5 + 4 * 4;
        int groupX = this.middleX + this.middleWidth / 2 - groupWidth / 2;

        this.searchButton = this.addRenderableWidget(new IconButton(Component.translatable("screen.music_and_melody.search"), IconButton.icon("search"), button -> toggleSearch()));
        this.searchButton.setX(this.middleX + 8);
        this.searchButton.setY(this.bottomPanelTop + 5);

        // Cover the entire seek hit area while expanded.  Leaving even a
        // couple of pixels exposed makes the bar below the field clickable.
        this.searchField = this.addRenderableWidget(new EditBox(this.font, this.middleX + 30, this.bottomPanelTop + 5, Math.max(32, this.middleWidth - 68), 20, Component.translatable("screen.music_and_melody.search")));
        this.searchField.setValue(this.search);
        this.searchField.setResponder(value -> {
            this.search = value;
            refreshPageList();
        });

        this.shuffleButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.shuffle"), shuffleIcon(), button -> {
            PlaylistHelper.shuffleQueue();
            refreshQueueLists();
        }));
        this.previousButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.previous"), IconButton.icon("previous"), button -> PlaylistHelper.previousQueue()));
        this.playPauseButton = this.addRenderableWidget(new IconButton(playPauseMessage(), playPauseIcon(), button -> {
            if (PlaylistHelper.isQueuePlaying()) {
                PlaylistHelper.pauseQueue();
            } else {
                PlaylistHelper.playNextNow();
            }
        }));
        this.nextButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.next"), IconButton.icon("next"), button -> PlaylistHelper.skipQueue()));
        this.loopButton = this.addRenderableWidget(new IconButton(loopMessage(), loopIcon(), button -> PlaylistHelper.setLoopingQueue(!PlaylistHelper.isLoopingQueue())));

        IconButton[] controls = {this.shuffleButton, this.previousButton, this.playPauseButton, this.nextButton, this.loopButton};
        for (int i = 0; i < controls.length; i++) {
            controls[i].setX(groupX + i * (IconButton.SIZE + 4));
            controls[i].setY(buttonY);
        }
        updateSearchVisibility();
    }

    private void buildPage() {
        switch (this.page) {
            case NOW_PLAYING -> buildNowPlayingPage();
            case LIBRARY -> buildLibraryPage();
            case DETAILS -> buildDetailsPage();
            case EVENTS -> buildEventsPage();
            case THEMES -> buildThemesPage();
            case ONLINE -> buildOnlinePage();
            case HOME -> buildHomePage();
            case CONFIG -> buildConfigPage();
        }
    }

    private void buildNowPlayingPage() {
        addBackButton();
        int listTop = PANEL_TOP + 42;
        this.mainQueueList = this.addRenderableWidget(new QueueList(this, this.minecraft, this.middleX, this.middleWidth, listTop, this.contentBottom - 6, false));

        this.saveButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.save"), IconButton.icon("save"), button ->
                this.minecraft.gui.setScreen(new SavePlaylistScreen(this))));
        int actionGroupWidth = IconButton.SIZE * 2 + 5;
        int actionX = this.rightX + (this.rightWidth - actionGroupWidth) / 2;
        this.saveButton.setX(actionX);
        this.saveButton.setY(playerActionY());

        this.clearButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.clear"), IconButton.icon("clear"), button ->
                requestClearQueue()));
        this.clearButton.setX(actionX + IconButton.SIZE + 5);
        this.clearButton.setY(playerActionY());

        buildMusicToggles();
    }

    private void buildDetailsPage() {
        if (this.viewedContent == null) {
            this.page = Page.LIBRARY;
            this.rebuildWidgets();
            return;
        }
        addBackButton();
        this.contentTrackList = this.addRenderableWidget(new ContentTrackList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 42, this.contentBottom - 6));
        int actionWidth = this.rightWidth - 16;
        int buttonWidth = (actionWidth - 5) / 2;
        int actionX = this.rightX + 8;
        int actionY = playerActionY();
        this.loadButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.load"), false, button -> loadViewedContent()));
        this.queueButton = this.addRenderableWidget(new WorkspaceButton(actionX + buttonWidth + 5, actionY, actionWidth - buttonWidth - 5, 20,
                Component.translatable("button.music_and_melody.queue"), false, button -> queueViewedContent()));
        buildMusicToggles();
    }

    private void buildMusicToggles() {
        MaMClientConfig config = MaMClientConfig.get();
        int togglesY = musicToggleY();
        int toggleWidth = this.rightWidth - 16;
        this.vanillaMusicButton = this.addRenderableWidget(new WorkspaceButton(this.rightX + 8, togglesY, toggleWidth, 20,
                Component.translatable("screen.music_and_melody.vanilla_music"), config.vanilla_music, button -> toggleVanillaMusic()));
        this.eventsButton = this.addRenderableWidget(new WorkspaceButton(this.rightX + 8, togglesY + 24, toggleWidth, 20,
                Component.translatable("screen.music_and_melody.event_music"), config.allow_events, button -> toggleEventMusic()));
    }

    private void buildLibraryPage() {
        addBackButton();
        this.libraryList = this.addRenderableWidget(new LibraryList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6));
        buildTagButtons(LibraryTag.values(), this.libraryTags, this::toggleLibraryTag);
    }

    private void buildEventsPage() {
        addBackButton();
        buildTagButtons(EventTag.values(), this.eventTags, this::toggleEventTag);
        if (this.selectedEventNamespace == null) {
            this.eventFolderList = this.addRenderableWidget(new EventFolderList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6));
        } else {
            this.eventSourceList = this.addRenderableWidget(new EventSourceList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6, this.selectedEventNamespace));
        }
        this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, this.panelBottom - 28, this.rightWidth - 14, 20,
                Component.translatable("button.music_and_melody.new_event"), false,
                button -> this.minecraft.gui.setScreen(new CreateEventScreen(this))));
    }

    private void buildOnlinePage() {
        addBackButton();
        if (this.viewedRemotePack == null) buildTagButtons(OnlineTag.values(), this.onlineTags, this::toggleOnlineTag);
        else buildRemoteDetailsAction();
        if (this.selectedOnlineCatalog == null) {
            this.onlineCatalogList = this.addRenderableWidget(new OnlineCatalogList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6));
        } else {
            this.onlinePackList = this.addRenderableWidget(new OnlinePackList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6, this.selectedOnlineCatalog));
        }
    }

    private void buildHomePage() {
        int listTop = PANEL_TOP + 58;
        int listBottom = this.contentBottom - 6;
        if (listBottom - listTop < HOME_MENU_HEIGHT) {
            this.addRenderableWidget(new HomeMenuList(this, this.minecraft, this.middleX, this.middleWidth, listTop, listBottom));
            return;
        }
        int buttonWidth = mainMenuButtonWidth();
        int x = this.middleX + this.middleWidth / 2 - buttonWidth / 2;
        int y = PANEL_TOP + Math.max(58, (this.contentBottom - PANEL_TOP - HOME_MENU_HEIGHT) / 2);
        addHomeButton(Component.translatable("screen.music_and_melody.albums"), x, y, buttonWidth, () -> setPage(Page.LIBRARY));
        addHomeButton(Component.translatable("button.music_and_melody.events"), x, y + HOME_BUTTON_STEP, buttonWidth, () -> setPage(Page.EVENTS));
        addHomeButton(Component.translatable("screen.music_and_melody.themes"), x, y + HOME_BUTTON_STEP * 2, buttonWidth, () -> setPage(Page.THEMES));
        addHomeButton(Component.translatable("screen.music_and_melody.online_browser"), x, y + HOME_BUTTON_STEP * 3, buttonWidth, () -> setPage(Page.ONLINE));
        addHomeButton(Component.translatable("screen.music_and_melody.config"), x, y + HOME_BUTTON_STEP * 4, buttonWidth, () -> setPage(Page.CONFIG));
        addHomeButton(CommonComponents.GUI_DONE, x, y + HOME_BUTTON_STEP * 5, buttonWidth, this::onClose);
    }

    private void buildThemesPage() {
        addBackButton();
    }

    private void buildConfigPage() {
        addBackButton();
        int buttonWidth = mainMenuButtonWidth();
        int x = this.middleX + this.middleWidth / 2 - buttonWidth / 2;
        int y = PANEL_TOP + 64;
        this.addRenderableWidget(new WorkspaceButton(x, y, buttonWidth, 20,
                Component.translatable("button.music_and_melody.client"), false,
                button -> this.minecraft.gui.setScreen(AutoConfigClient.getConfigScreen(MaMClientConfig.class, this).get())));
        this.addRenderableWidget(new WorkspaceButton(x, y + 28, buttonWidth, 20,
                Component.translatable("button.music_and_melody.server"), false,
                button -> this.minecraft.gui.setScreen(AutoConfigClient.getConfigScreen(MaMServerConfig.class, this).get())));
    }

    private int mainMenuButtonWidth() {
        return Math.max(1, Math.min(230, this.middleWidth - 44));
    }

    private void addHomeButton(Component label, int x, int y, int width, Runnable action) {
        this.addRenderableWidget(new WorkspaceButton(x, y, width, 22, label, false, ignored -> action.run()));
    }

    private void buildRemoteDetailsAction() {
        if (this.viewedRemotePack == null) return;
        RemotePack pack = this.viewedRemotePack;
        int x = this.rightX + 7;
        int width = this.rightWidth - 14;
        int backY = this.panelBottom - 28;
        this.remoteBackButton = this.addRenderableWidget(new WorkspaceButton(x, backY, width, 20,
                Component.translatable("button.music_and_melody.back"), false, button -> closeRemoteDetails()));

        RemoteContentManager.State state = RemoteContentManager.state(pack);
        if (state == RemoteContentManager.State.DOWNLOADING) return;

        boolean canDelete = remoteDeleteAvailable(pack);
        if (canDelete && remoteActionActive(pack)) {
            int actionWidth = (width - 4) / 2;
            this.remoteDeleteButton = this.addRenderableWidget(new WorkspaceButton(x, backY - 24, actionWidth, 20,
                    remoteDeleteMessage(pack), isRemoteDeletePending(pack), button -> toggleRemoteDeletePending(pack)));
            this.remoteActionButton = this.addRenderableWidget(new WorkspaceButton(x + actionWidth + 4, backY - 24, width - actionWidth - 4, 20,
                    remoteActionMessage(pack), false, button -> activateRemotePack(pack)));
            return;
        }

        if (canDelete) {
            this.remoteDeleteButton = this.addRenderableWidget(new WorkspaceButton(x, backY - 24, width, 20,
                    remoteDeleteMessage(pack), isRemoteDeletePending(pack), button -> toggleRemoteDeletePending(pack)));
            return;
        }

        if (remoteActionActive(pack)) {
            this.remoteActionButton = this.addRenderableWidget(new WorkspaceButton(x, backY - 24, width, 20,
                    remoteActionMessage(pack), false, button -> activateRemotePack(pack)));
        }
    }

    private void addBackButton() {
        this.backButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.back"), IconButton.icon("back"), button -> goBack()));
        this.backButton.setX(this.middleX + 8);
        this.backButton.setY(PANEL_TOP + 6);
    }

    private <T extends Enum<T> & Tag> void buildTagButtons(T[] values, Set<T> selected, java.util.function.Consumer<T> onToggle) {
        int top = PANEL_TOP + 38;
        int bottom = tagFilterBottom();
        int availableHeight = Math.max(0, bottom - top);
        if (values.length * 24 > availableHeight) {
            this.tagFilterList = this.addRenderableWidget(new TagFilterList<>(this, this.minecraft, this.rightX, this.rightWidth,
                    top, Math.max(top + 24, bottom), values, selected, onToggle));
            return;
        }
        buildTagButtons(values, selected, onToggle, top);
    }

    private <T extends Enum<T> & Tag> void buildTagButtons(T[] values, Set<T> selected, java.util.function.Consumer<T> onToggle, int y) {
        int width = this.rightWidth - 14;
        for (T tag : values) {
            this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, y, width, 20, tag.label(), selected.contains(tag), ignored -> {
                        onToggle.accept(tag);
                        this.rebuildWidgets();
                    }));
            y += 24;
        }
    }

    private int tagFilterBottom() {
        return switch (this.page) {
            case EVENTS -> this.panelBottom - 34;
            case ONLINE -> this.panelBottom - 8;
            default -> this.panelBottom - 8;
        };
    }

    private void toggleLibraryTag(LibraryTag tag) {
        toggleTag(this.libraryTags, tag);
    }

    private void toggleEventTag(EventTag tag) {
        toggleTag(this.eventTags, tag);
    }

    private void toggleOnlineTag(OnlineTag tag) {
        toggleTag(this.onlineTags, tag);
    }

    private static <T> void toggleTag(Set<T> tags, T tag) {
        if (!tags.add(tag)) tags.remove(tag);
    }

    private void renderShell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, SCREEN_BACKGROUND);
        drawPanel(graphics, this.leftX, PANEL_TOP, this.leftWidth, this.panelBottom - PANEL_TOP);
        drawPanel(graphics, this.middleX, PANEL_TOP, this.middleWidth, this.contentBottom - PANEL_TOP);
        drawPanel(graphics, this.middleX, this.bottomPanelTop, this.middleWidth, this.panelBottom - this.bottomPanelTop);
        drawPanel(graphics, this.rightX, PANEL_TOP, this.rightWidth, this.panelBottom - PANEL_TOP);

        graphics.text(this.font, Component.translatable("screen.music_and_melody.now_playing").withStyle(ChatFormatting.BOLD), this.leftX + 8, PANEL_TOP + 11, TEXT_HEADER);
        int favouriteHeaderY = PANEL_TOP + 31 + 75;
        graphics.text(this.font, Component.translatable("screen.music_and_melody.favourites").withStyle(ChatFormatting.BOLD), this.leftX + 8, favouriteHeaderY + 3, TEXT_HEADER);

        renderMiddleHeader(graphics);
        renderRightPanel(graphics, mouseX, mouseY);
        renderPlaybackStrip(graphics, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, PANEL_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_OUTLINE);
    }

    private void renderMiddleHeader(GuiGraphicsExtractor graphics) {
        this.breadcrumbHits.clear();
        switch (this.page) {
            case NOW_PLAYING -> {
                SourceInfo source = currentSource();
                renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
                graphics.text(this.font, Component.translatable("screen.music_and_melody.content_type_origin", source.typeLabel(), source.originLabel()), this.middleX + 34, PANEL_TOP + 27, TEXT_DESCRIPTION);
            }
            case DETAILS -> {
                if (this.viewedContent != null) {
                    renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
                    drawTrackMarquee(graphics, contentTags(this.viewedContent), this.middleX + 34, PANEL_TOP + 27,
                            this.middleWidth - 42, TEXT_DESCRIPTION);
                }
            }
            case LIBRARY, EVENTS, ONLINE, CONFIG -> renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
            case THEMES -> {
                renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
                graphics.centeredText(this.font, Component.translatable("screen.music_and_melody.themes").withStyle(ChatFormatting.BOLD),
                        this.middleX + this.middleWidth / 2, PANEL_TOP + 42, TEXT_TITLE);
                graphics.centeredText(this.font, Component.translatable("screen.music_and_melody.themes.coming_soon"),
                        this.middleX + this.middleWidth / 2, PANEL_TOP + 58, TEXT_DESCRIPTION);
            }
            case HOME -> {
                renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
                graphics.centeredText(this.font, Component.translatable("screen.music_and_melody.music_player").withStyle(ChatFormatting.BOLD), this.middleX + this.middleWidth / 2, PANEL_TOP + 34, TEXT_TITLE);
                graphics.centeredText(this.font, Component.translatable("screen.music_and_melody.choose_section"), this.middleX + this.middleWidth / 2, PANEL_TOP + 48, TEXT_DESCRIPTION);
            }
        }
    }

    private void renderRightPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.page == Page.NOW_PLAYING || this.page == Page.DETAILS) {
            SourceInfo source = this.page == Page.NOW_PLAYING ? currentSource() : viewedContentSource();
            if (source != null) {
                renderSourceCard(graphics, source);
                renderVolumeSlider(graphics, mouseX, mouseY);
            }
            return;
        }

        if (this.page == Page.ONLINE && this.viewedRemotePack != null) {
            renderRemoteDetails(graphics, this.viewedRemotePack);
            return;
        }

        Component title = switch (this.page) {
            case LIBRARY, EVENTS, ONLINE -> Component.translatable("screen.music_and_melody.filter_by_tags");
            case HOME, CONFIG, NOW_PLAYING, DETAILS, THEMES -> Component.empty();
        };
        if (!title.getString().isEmpty()) graphics.text(this.font, title.copy().withStyle(ChatFormatting.BOLD), this.rightX + 8, PANEL_TOP + 14, TEXT_HEADER);
        if (this.page == Page.ONLINE) renderOnlineDownloadProgress(graphics);
    }

    private SourceInfo viewedContentSource() {
        if (this.viewedContent == null) return null;
        return new SourceInfo(this.viewedContent.name(), this.viewedContent.icon(),
                sourceTypeLabel(this.viewedContent.type()), originFor(this.viewedContent.id(), this.viewedContent.playlist()),
                this.viewedContent.favourite());
    }

    private void renderSourceCard(GuiGraphicsExtractor graphics, SourceInfo source) {
        int cardY = PANEL_TOP + 10;
        int cardSize = sourceCardSize();
        int cardX = this.rightX + (this.rightWidth - cardSize) / 2;
        graphics.fill(cardX, cardY, cardX + cardSize, cardY + cardSize, SOURCE_CARD_BACKGROUND);
        Identifier icon = MusicScreenHelper.albumIcon(this.minecraft, source.icon());
        int iconSize = Math.max(24, cardSize - 24);
        int iconX = cardX + (cardSize - iconSize) / 2;
        int iconY = cardY + 9;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        drawMarquee(graphics, source.name(), cardX + 4, cardY + cardSize - 12, cardSize - 8,
                source.favourite() ? TEXT_FAVOURITE : TEXT_TITLE);
    }

    private void renderVolumeSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int sliderX = volumeSliderX();
        int sliderTop = volumeSliderTop();
        int sliderBottom = volumeSliderBottom();
        float volume = this.minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
        graphics.fill(sliderX, sliderTop, sliderX + 4, sliderBottom, BAR_BACKGROUND);
        int filledTop = sliderBottom - Math.round((sliderBottom - sliderTop) * volume);
        graphics.fill(sliderX, filledTop, sliderX + 4, sliderBottom, PANEL_HIGHLIGHT);
        graphics.fill(sliderX - 4, filledTop - 2, sliderX + 8, filledTop + 3, TEXT_TITLE);
        if (mouseX >= sliderX - 10 && mouseX <= sliderX + 14 && mouseY >= sliderTop && mouseY <= sliderBottom) {
            graphics.setTooltipForNextFrame(Component.translatable("screen.music_and_melody.music_volume", Math.round(volume * 100F)), mouseX, mouseY);
        }
    }

    private void renderOnlineDownloadProgress(GuiGraphicsExtractor graphics) {
        for (RemotePack pack : RemoteContentManager.packs()) {
            OptionalDouble progress = RemoteContentManager.downloadProgress(pack);
            if (progress.isEmpty()) continue;
            int x = this.rightX + 8;
            int right = this.rightX + this.rightWidth - 8;
            int y = Math.max(this.panelBottom - 22, onlineTagBottom() + 8);
            drawTrackMarquee(graphics, pack.name(), x, y - 12, Math.max(1, right - x), TEXT_DESCRIPTION);
            graphics.fill(x, y, right, y + 4, BAR_BACKGROUND);
            graphics.fill(x, y, x + (int) Math.round((right - x) * progress.getAsDouble()), y + 4, PANEL_HIGHLIGHT);
            return;
        }
    }

    private int onlineTagBottom() {
        return PANEL_TOP + 38 + OnlineTag.values().length * 24;
    }

    private void renderRemoteDetails(GuiGraphicsExtractor graphics, RemotePack pack) {
        int x = this.rightX + 8;
        int width = this.rightWidth - 16;
        int headingY = PANEL_TOP + 2;
        graphics.text(this.font, Component.translatable("screen.music_and_melody.details").withStyle(ChatFormatting.BOLD), x, headingY, TEXT_HEADER);

        int iconSize = Math.min(42, width);
        int iconY = PANEL_TOP + 18;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, RemoteIconManager.icon(pack)),
                x, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int textX = x + iconSize + 6;
        int textWidth = Math.max(1, width - iconSize - 6);
        drawTrackMarquee(graphics, pack.name(), textX, iconY + 1, textWidth, TEXT_TITLE);
        // Reserve two lines beside the icon for the content namespace.  Both
        // lines pan independently instead of being cut off in this narrow panel.
        drawTrackMarquee(graphics, Component.literal(pack.id().getNamespace() + ":"), textX, iconY + 13, textWidth, TEXT_DESCRIPTION);
        drawTrackMarquee(graphics, Component.literal(pack.id().getPath()), textX, iconY + 25, textWidth, TEXT_DESCRIPTION);

        int fieldY = iconY + iconSize + 5;
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.repository", Component.literal(pack.repository()), x, fieldY, width);
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.version", Component.literal(pack.version()), x, fieldY + 26, width);
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.state", remoteStateMessage(RemoteContentManager.state(pack)), x, fieldY + 52, width);

        int descriptionY = fieldY + 78;
        graphics.text(this.font, Component.translatable("screen.music_and_melody.remote_details.description").withStyle(ChatFormatting.UNDERLINE),
                x, descriptionY, TEXT_DESCRIPTION);
        descriptionY += 12;
        int descriptionBottom = this.panelBottom - 62;
        for (FormattedCharSequence line : this.font.split(pack.description(), Math.max(1, width))) {
            if (descriptionY + this.font.lineHeight > descriptionBottom) break;
            graphics.text(this.font, line, x, descriptionY, TEXT_PRIMARY);
            descriptionY += this.font.lineHeight + 2;
        }
        renderRemoteDownloadProgress(graphics, pack);
    }

    private void renderRemoteDetailField(GuiGraphicsExtractor graphics, String headingKey, Component value, int x, int y, int width) {
        graphics.text(this.font, Component.translatable(headingKey).withStyle(ChatFormatting.UNDERLINE), x, y, TEXT_DESCRIPTION);
        drawTrackMarquee(graphics, value, x, y + 12, width, TEXT_PRIMARY);
    }

    private void renderRemoteDownloadProgress(GuiGraphicsExtractor graphics, RemotePack pack) {
        OptionalDouble progress = RemoteContentManager.downloadProgress(pack);
        if (progress.isEmpty()) return;
        int x = this.rightX + 8;
        int right = this.rightX + this.rightWidth - 8;
        int y = this.panelBottom - 47;
        graphics.fill(x, y, right, y + 4, BAR_BACKGROUND);
        graphics.fill(x, y, x + (int) Math.round((right - x) * progress.getAsDouble()), y + 4, PANEL_HIGHLIGHT);
    }

    private void renderPlaybackStrip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int progressX = this.middleX + 34;
        int progressRight = this.middleX + this.middleWidth - 42;
        int progressY = this.bottomPanelTop + 9;
        int progressWidth = Math.max(1, progressRight - progressX);
        graphics.fill(progressX, progressY, progressRight, progressY + 3, BAR_BACKGROUND);

        long elapsed = this.draggingProgress ? this.seekPreviewMillis : PlaylistHelper.currentSongElapsedMillis();
        Optional<Long> duration = MusicDurationHelper.currentDurationMillis(this.minecraft, PlaylistHelper.getCurrentSong());
        if (duration.isPresent() && duration.get() > 0L) {
            float progress = Math.min(1.0F, elapsed / (float) duration.get());
            int handleX = progressX + Math.round(progressWidth * progress);
            graphics.fill(progressX, progressY, handleX, progressY + 3, PANEL_HIGHLIGHT);
            graphics.fill(handleX - 1, progressY - 2, handleX + 2, progressY + 5, TEXT_TITLE);
            graphics.text(this.font, Component.literal(formatDuration(Math.max(0L, duration.get() - elapsed))), progressRight + 6, this.bottomPanelTop + 7, TEXT_DESCRIPTION);
        } else {
            graphics.text(this.font, Component.literal("--:--"), progressRight + 6, this.bottomPanelTop + 7, TEXT_DESCRIPTION);
        }
        if (!this.searching && PlaylistHelper.getCurrentSongId() != null) {
            Component track = MusicScreenHelper.playlistName(this.minecraft, PlaylistHelper.getCurrentSongId());
            // Keep the moving title inside the same horizontal lane as the
            // seek bar; it must never sweep under the search control.
            drawMarquee(graphics, track, progressX, this.bottomPanelTop + 17, progressWidth, TEXT_DESCRIPTION);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        updateDynamicControls();
        graphics.pose().pushMatrix();
        graphics.pose().scale(MaMDataConfig.get().gui_multiplier);
        super.extractRenderState(graphics, toLayoutMouse(mouseX), toLayoutMouse(mouseY), tickDelta);
        graphics.pose().popMatrix();
    }

    private int toLayoutMouse(double mouse) {
        return Math.round((float) (mouse / MaMDataConfig.get().gui_multiplier));
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier, event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private void updateDynamicControls() {
        if (this.shuffleButton != null) {
            this.shuffleButton.setIconAndTooltip(shuffleIcon(), Component.translatable(PlaylistHelper.isShuffleQueue()
                    ? "screen.music_and_melody.shuffle_on"
                    : "screen.music_and_melody.shuffle_off"));
            this.shuffleButton.setSelected(PlaylistHelper.isShuffleQueue());
            this.shuffleButton.active = PlaylistHelper.queuedSongs().size() > 1;
        }
        if (this.previousButton != null) this.previousButton.active = PlaylistHelper.hasPreviousQueue();
        if (this.playPauseButton != null) {
            this.playPauseButton.setIconAndTooltip(playPauseIcon(), playPauseMessage());
            this.playPauseButton.active = PlaylistHelper.isQueuePlaying() || PlaylistHelper.hasQueuedSongs();
        }
        if (this.nextButton != null) this.nextButton.active = PlaylistHelper.canSkipQueue();
        if (this.loopButton != null) this.loopButton.setIconAndTooltip(loopIcon(), loopMessage());
        if (this.saveButton != null) this.saveButton.active = PlaylistHelper.hasQueuedSongs() && PlaylistHelper.isQueueCustom();
        if (this.clearButton != null) this.clearButton.active = PlaylistHelper.hasQueuedSongs();
        if (this.loadButton != null || this.queueButton != null) {
            boolean hasTracks = this.viewedContent != null && !this.viewedContent.queueSongs(this.minecraft).isEmpty();
            if (this.loadButton != null) this.loadButton.active = hasTracks;
            if (this.queueButton != null) this.queueButton.active = hasTracks;
        }
        if (this.remoteActionButton != null && this.viewedRemotePack != null) {
            this.remoteActionButton.setMessage(remoteActionMessage(this.viewedRemotePack));
            this.remoteActionButton.active = remoteActionActive(this.viewedRemotePack);
        }
        updateSearchVisibility();
        if (this.focusSearchField && this.searching && this.searchField != null) {
            this.setFocused(this.searchField);
            this.searchField.setFocused(true);
            this.focusSearchField = false;
        }
    }

    private void toggleVanillaMusic() {
        MaMClientConfig config = MaMClientConfig.get();
        config.vanilla_music = !config.vanilla_music;
        AutoConfig.getConfigHolder(MaMClientConfig.class).save();
        if (!config.vanilla_music) this.minecraft.getMusicManager().stopPlaying();
        this.rebuildWidgets();
    }

    private void toggleEventMusic() {
        MaMClientConfig config = MaMClientConfig.get();
        config.allow_events = !config.allow_events;
        AutoConfig.getConfigHolder(MaMClientConfig.class).save();
        if (!config.allow_events) EventHelper.stopDisabledEventMusic();
        this.rebuildWidgets();
    }

    private void toggleSearch() {
        this.searching = !this.searching;
        if (!this.searching) {
            this.search = "";
            this.searchField.setValue("");
            this.searchField.setFocused(false);
            this.setFocused(null);
            this.focusSearchField = false;
            refreshPageList();
        } else {
            // Screen's click dispatch focuses the icon after this callback
            // returns.  Defer the field focus to the next render pass so it
            // wins that race and is immediately ready for typing.
            updateSearchVisibility();
            this.focusSearchField = true;
        }
        updateSearchVisibility();
    }

    private void updateSearchVisibility() {
        if (this.searchField != null) {
            this.searchField.visible = this.searching;
            this.searchField.active = this.searching;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        event = toLayoutMouse(event);
        // The expanded search field intentionally covers the seek strip. It
        // must receive the click first, otherwise the concealed seek target
        // changes playback position beneath an active text field.
        if (this.searching && this.searchField != null && this.searchField.isMouseOver(event.x(), event.y())) {
            return this.searchField.mouseClicked(event, doubleClick);
        }
        for (BreadcrumbHit hit : this.breadcrumbHits) {
            if (hit.contains(event.x(), event.y())) {
                AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
                hit.action.run();
                return true;
            }
        }
        if (isInProgressBar(event.x(), event.y()) && currentTrackDuration().isPresent()) {
            this.draggingProgress = true;
            setSeekPreviewFromX(event.x());
            return true;
        }
        if ((this.page == Page.NOW_PLAYING || this.page == Page.DETAILS) && isInVolumeSlider(event.x(), event.y())) {
            this.draggingVolume = true;
            setVolumeFromY(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        event = toLayoutMouse(event);
        dragX /= MaMDataConfig.get().gui_multiplier;
        dragY /= MaMDataConfig.get().gui_multiplier;
        if (this.draggingProgress) {
            setSeekPreviewFromX(event.x());
            return true;
        }
        if (this.draggingVolume) {
            setVolumeFromY(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        event = toLayoutMouse(event);
        if (this.draggingProgress) {
            this.draggingProgress = false;
            PlaylistHelper.seekCurrentSong(this.seekPreviewMillis);
            return true;
        }
        if (this.draggingVolume) {
            this.draggingVolume = false;
            return true;
        }
        if (this.draggingQueueIndex >= 0 && this.draggingQueueList != null) {
            int from = this.draggingQueueIndex;
            int to = this.draggingQueueList.indexAt(event.x(), event.y());
            this.draggingQueueIndex = -1;
            this.draggingQueueList = null;
            if (to >= 0 && to != from) {
                requestQueueMutation(Component.translatable("screen.music_and_melody.queue_mutation.warning"), () -> PlaylistHelper.move(from, to));
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / MaMDataConfig.get().gui_multiplier, mouseY / MaMDataConfig.get().gui_multiplier, scrollX, scrollY);
    }

    private boolean isInVolumeSlider(double mouseX, double mouseY) {
        int x = volumeSliderX();
        int top = volumeSliderTop();
        int bottom = volumeSliderBottom();
        return mouseX >= x - 10 && mouseX <= x + 14 && mouseY >= top && mouseY <= bottom;
    }

    private boolean isInProgressBar(double mouseX, double mouseY) {
        int left = this.middleX + 30;
        int right = this.middleX + this.middleWidth - 38;
        int top = this.bottomPanelTop + 4;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= top + 12;
    }

    private Optional<Long> currentTrackDuration() {
        return MusicDurationHelper.currentDurationMillis(this.minecraft, PlaylistHelper.getCurrentSong())
                .filter(value -> value > 0L);
    }

    private void setSeekPreviewFromX(double mouseX) {
        Optional<Long> duration = currentTrackDuration();
        if (duration.isEmpty()) return;
        int progressX = this.middleX + 34;
        int progressRight = this.middleX + this.middleWidth - 42;
        float fraction = (float) ((mouseX - progressX) / Math.max(1, progressRight - progressX));
        this.seekPreviewMillis = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * duration.get());
    }

    private void setVolumeFromY(double mouseY) {
        int top = volumeSliderTop();
        int bottom = volumeSliderBottom();
        float value = (float) ((bottom - mouseY) / (bottom - top));
        value = Math.max(0.0F, Math.min(1.0F, value));
        this.minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set((double) value);
        this.minecraft.options.save();
    }

    private int playerActionY() {
        return this.panelBottom - IconButton.SIZE - 8;
    }

    private int musicToggleY() {
        return playerActionY() - 52;
    }

    private int volumeSliderX() {
        return this.rightX + this.rightWidth / 2 - 2;
    }

    private int volumeSliderTop() {
        return PANEL_TOP + 10 + sourceCardSize() + 22;
    }

    private int volumeSliderBottom() {
        return Math.max(volumeSliderTop() + 1, musicToggleY() - 14);
    }

    private int sourceCardSize() {
        int widthLimit = this.rightWidth - 16;
        int heightLimit = musicToggleY() - PANEL_TOP - 86;
        return Math.max(24, Math.min(112, Math.min(widthLimit, heightLimit)));
    }

    void startQueueDrag(QueueList list, int index) {
        if (list.compact || index < 0) return;
        this.draggingQueueList = list;
        this.draggingQueueIndex = index;
    }

    void playClick() {
        AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
    }

    boolean isDraggingQueue(int index) {
        return this.draggingQueueIndex == index;
    }

    void playQueueTrack(int index) {
        PlaylistHelper.playNow(index);
        refreshQueueLists();
    }

    private void requestQueueMutation(Component message, Runnable mutation) {
        if (PlaylistHelper.isQueueCustom()) {
            mutation.run();
            refreshAfterQueueMutation();
            return;
        }
        this.minecraft.gui.setScreen(new QueueMutationConfirmScreen(this, message, () -> {
            mutation.run();
            refreshAfterQueueMutation();
        }));
    }

    private void requestClearQueue() {
        if (!PlaylistHelper.hasQueuedSongs()) return;
        if (hasUnsavedCustomPlaylist()) {
            requestDiscardCustomPlaylist(Component.translatable("button.music_and_melody.clear"), this::clearQueueAndRefresh);
        } else {
            // Clearing a named source still turns it into a Custom Playlist,
            // so retain the normal make-custom acknowledgement first.
            requestQueueMutation(Component.translatable("screen.music_and_melody.queue_mutation.warning"), PlaylistHelper::clear);
        }
    }

    private void clearQueueAndRefresh() {
        PlaylistHelper.clear();
        refreshAfterQueueMutation();
    }

    private boolean hasUnsavedCustomPlaylist() {
        return PlaylistHelper.isQueueCustom() && PlaylistHelper.hasQueuedSongs();
    }

    /**
     * A source-less queue is the only unsaved playlist.  Do not silently
     * replace or empty it, regardless of which control initiated the action.
     */
    private void requestDiscardCustomPlaylist(Component confirmLabel, Runnable action) {
        if (!hasUnsavedCustomPlaylist()) {
            action.run();
            return;
        }
        this.minecraft.gui.setScreen(new QueueMutationConfirmScreen(
                this,
                Component.translatable("screen.music_and_melody.discard_custom_playlist"),
                Component.translatable("screen.music_and_melody.discard_custom_playlist.warning"),
                confirmLabel,
                action
        ));
    }

    void requestRemoveQueueTrack(int index) {
        requestQueueMutation(Component.translatable("screen.music_and_melody.queue_mutation.warning"), () -> PlaylistHelper.remove(index));
    }

    private void refreshAfterQueueMutation() {
        if (this.page == Page.DETAILS && PlaylistHelper.isQueueCustom()) {
            this.viewedContent = null;
            this.page = Page.NOW_PLAYING;
        }
        refreshQueueLists();
        refreshFavouriteList();
        if (this.page == Page.NOW_PLAYING) this.rebuildWidgets();
    }

    void refreshQueueLists() {
        if (this.mainQueueList != null) this.mainQueueList.refresh();
    }

    void refreshFavouriteList() {
        if (this.favouriteList != null) this.favouriteList.refresh();
    }

    void toggleContentTrack(Album album, String track) {
        if (album == null || track == null || album.isTrackForcedEnabled(track)) return;
        album.setTrackEnabled(track, !album.isTrackEnabled(track));
        this.reloadPending = true;
        if (this.contentTrackList != null) this.contentTrackList.refresh();
    }

    void toggleAlbumEnabled(Album album) {
        if (album == null) return;
        album.setEnabled(!album.isEnabled());
        this.reloadPending = true;
        if (this.libraryList != null) this.libraryList.refresh();
    }

    private void refreshPageList() {
        if (this.libraryList != null) this.libraryList.refresh();
        if (this.contentTrackList != null) this.contentTrackList.refresh();
        if (this.eventFolderList != null) this.eventFolderList.refresh();
        if (this.eventSourceList != null) this.eventSourceList.refresh();
        if (this.onlineCatalogList != null) this.onlineCatalogList.refresh();
        if (this.onlinePackList != null) this.onlinePackList.refresh();
    }

    void openContent(ContentItem item) {
        this.viewedContent = item;
        this.page = Page.DETAILS;
        this.searching = false;
        this.search = "";
        this.rebuildWidgets();
    }

    private void loadViewedContent() {
        if (this.viewedContent == null) return;
        List<SafeIdentifier> songs = this.viewedContent.queueSongs(this.minecraft);
        if (songs.isEmpty()) return;

        if (hasUnsavedCustomPlaylist()) {
            requestDiscardCustomPlaylist(Component.translatable("button.music_and_melody.load"),
                    () -> loadViewedContentNow(songs));
        } else if (!PlaylistHelper.isQueueCustom() && PlaylistHelper.hasQueuedSongs()) {
            requestQueueMutation(Component.translatable("screen.music_and_melody.queue_mutation.warning"),
                    () -> loadViewedContentNow(songs));
        } else {
            loadViewedContentNow(songs);
        }
    }

    private void loadViewedContentNow(List<SafeIdentifier> songs) {
        if (this.viewedContent == null || songs.isEmpty()) return;
        if (!PlaylistHelper.loadCustomQueue(songs)) return;
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        this.searching = false;
        this.search = "";
        this.rebuildWidgets();
    }

    private void loadLastCustomPlaylist() {
        Playlist playlist = Playlist.PLAYLISTS.stream()
                .filter(Playlist::isCustom)
                .min(Comparator.<Playlist>comparingInt(value -> PlaylistHelper.recentSourceRank(MaMDataConfig.QueueSourceType.PLAYLIST, value.playlist.toString()))
                        .thenComparing(value -> value.name.getString(), String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
        if (playlist == null) {
            requestEmptyCustomPlaylist();
            return;
        }
        List<SafeIdentifier> songs = new ContentItem(null, playlist).queueSongs(this.minecraft);
        // Preserve the recency ordering of saved custom playlists while the
        // active queue itself remains the editable, source-less Custom Playlist.
        PlaylistHelper.setQueueSource(MaMDataConfig.QueueSourceType.PLAYLIST, playlist.playlist.toString(), playlist.name.getString());
        PlaylistHelper.loadCustomQueue(songs);
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        this.searching = false;
        this.search = "";
        this.rebuildWidgets();
    }

    private void requestEmptyCustomPlaylist() {
        PlaylistHelper.loadCustomQueue(List.of());
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        this.searching = false;
        this.search = "";
        this.rebuildWidgets();
    }

    private void queueViewedContent() {
        if (this.viewedContent == null) return;
        List<SafeIdentifier> songs = this.viewedContent.queueSongs(this.minecraft);
        if (songs.isEmpty()) return;
        if (!PlaylistHelper.hasQueuedSongs() && !PlaylistHelper.isQueueCustom()) {
            loadViewedContent();
            return;
        }
        if (songs.stream().noneMatch(song -> !PlaylistHelper.isQueued(song))) return;
        requestQueueMutation(Component.translatable("screen.music_and_melody.queue_mutation.warning"), () -> PlaylistHelper.addAll(songs));
    }

    void playContentTrack(int index) {
        if (this.viewedContent == null) return;
        List<SafeIdentifier> songs = this.viewedContent.queueSongs(this.minecraft);
        if (index < 0 || index >= songs.size() || !MusicDiscHelper.isSoundUnlocked(this.minecraft, songs.get(index))) return;

        if (!isViewedContentQueueSource()) {
            loadAndPlayViewedContentTrack(songs, index);
            return;
        }
        finishPlayingViewedContentTrack(index);
    }

    private void loadAndPlayViewedContentTrack(List<SafeIdentifier> songs, int index) {
        if (this.viewedContent == null || !PlaylistHelper.loadQueueSource(songs, this.viewedContent.type(), this.viewedContent.id().toString(), this.viewedContent.name().getString())) return;
        finishPlayingViewedContentTrack(index);
    }

    private void finishPlayingViewedContentTrack(int index) {
        if (!PlaylistHelper.playNow(index)) return;
        this.page = Page.DETAILS;
        this.searching = false;
        this.search = "";
        this.rebuildWidgets();
    }

    private boolean isViewedContentQueueSource() {
        if (this.viewedContent == null) return false;
        return PlaylistHelper.queueSource()
                .filter(source -> source.type() == this.viewedContent.type())
                .map(source -> source.id().equals(this.viewedContent.id().toString()))
                .orElse(false);
    }

    void toggleFavourite(ContentItem item) {
        if (item.album() != null) item.album().setFavourite(!item.album().isFavourite());
        else if (item.playlist() != null) item.playlist().setFavourite(!item.playlist().isFavourite());
        refreshFavouriteList();
        if (this.libraryList != null) this.libraryList.refresh();
    }

    void chooseEventNamespace(String namespace) {
        this.selectedEventNamespace = namespace;
        this.rebuildWidgets();
    }

    void openEvent(Event.Source source) {
        this.minecraft.gui.setScreen(new EventScreen(this, source.id));
    }

    void chooseOnlineCatalog(String catalog) {
        this.selectedOnlineCatalog = catalog;
        this.viewedRemotePack = null;
        this.rebuildWidgets();
    }

    void viewRemotePack(RemotePack pack) {
        this.viewedRemotePack = pack;
        this.rebuildWidgets();
    }

    void openRepositoryEditor() {
        this.minecraft.gui.setScreen(new RepositoryScreen(this));
    }

    void repositoriesChanged() {
        this.selectedOnlineCatalog = null;
        this.viewedRemotePack = null;
        RemoteContentManager.refresh();
        this.rebuildWidgets();
    }

    void activateRemotePack(RemotePack pack) {
        RemoteContentManager.State state = RemoteContentManager.state(pack);
        if (state == RemoteContentManager.State.REMOTE
                || state == RemoteContentManager.State.UPDATE_AVAILABLE
                || state == RemoteContentManager.State.FAILED) {
            RemoteContentManager.download(pack);
        } else if (state == RemoteContentManager.State.NEEDS_RELOAD) {
            this.minecraft.reloadResourcePacks().thenRun(RemoteContentManager::markReloaded);
        }
        if (this.onlinePackList != null) this.onlinePackList.refresh();
        if (this.viewedRemotePack != null && this.viewedRemotePack.id().equals(pack.id())) this.rebuildWidgets();
    }

    private static boolean remoteDeleteAvailable(RemotePack pack) {
        RemoteContentManager.State state = RemoteContentManager.state(pack);
        return state == RemoteContentManager.State.INSTALLED
                || state == RemoteContentManager.State.UPDATE_AVAILABLE
                || state == RemoteContentManager.State.NEEDS_RELOAD;
    }

    boolean isRemoteDeletePending(RemotePack pack) {
        return this.pendingRemoteDeletes.contains(pack.id());
    }

    void toggleRemoteDeletePending(RemotePack pack) {
        if (!remoteDeleteAvailable(pack)) return;
        if (!this.pendingRemoteDeletes.remove(pack.id())) this.pendingRemoteDeletes.add(pack.id());
        if (this.onlinePackList != null) this.onlinePackList.refresh();
        this.rebuildWidgets();
    }

    private Component remoteDeleteMessage(RemotePack pack) {
        return Component.translatable(isRemoteDeletePending(pack) ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
    }

    private void closeRemoteDetails() {
        this.viewedRemotePack = null;
        this.rebuildWidgets();
    }

    private void applyPendingRemoteDeletes() {
        if (this.pendingRemoteDeletes.isEmpty()) return;
        boolean changed = false;
        for (Identifier id : List.copyOf(this.pendingRemoteDeletes)) {
            changed |= RemoteContentManager.deleteInstalled(id);
        }
        this.pendingRemoteDeletes.clear();
        if (!changed) return;
        this.reloadPending = true;
        RemoteContentManager.refresh();
    }

    private void setPage(Page page) {
        if (this.page == Page.ONLINE && page != Page.ONLINE) applyPendingRemoteDeletes();
        this.page = page;
        this.searching = false;
        this.search = "";
        this.selectedEventNamespace = null;
        this.selectedOnlineCatalog = null;
        this.viewedRemotePack = null;
        this.rebuildWidgets();
    }

    private void goBack() {
        switch (this.page) {
            case NOW_PLAYING -> setPage(Page.LIBRARY);
            case DETAILS -> {
                this.viewedContent = null;
                setPage(Page.LIBRARY);
            }
            case EVENTS -> {
                if (this.selectedEventNamespace != null) {
                    this.selectedEventNamespace = null;
                    this.rebuildWidgets();
                } else setPage(Page.HOME);
            }
            case ONLINE -> {
                if (this.selectedOnlineCatalog != null) {
                    this.selectedOnlineCatalog = null;
                    this.viewedRemotePack = null;
                    this.rebuildWidgets();
                } else setPage(Page.HOME);
            }
            case LIBRARY, CONFIG, THEMES -> setPage(Page.HOME);
            case HOME -> this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void onClose() {
        if (this.page == Page.HOME) {
            this.minecraft.gui.setScreen(this.parent);
            if (this.reloadPending) this.minecraft.reloadResourcePacks();
        } else goBack();
    }

    /** Opens the actual source page; Now Playing itself is not a second list type. */
    private void openCurrentPlayback() {
        ContentItem source = currentSourceContent();
        if (source != null) {
            openContent(source);
            return;
        }
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        this.searching = false;
        this.search = "";
        this.rebuildWidgets();
    }

    private ContentItem currentSourceContent() {
        Optional<PlaylistHelper.QueueSource> queuedSource = PlaylistHelper.queueSource();
        if (queuedSource.isEmpty()) return null;
        PlaylistHelper.QueueSource source = queuedSource.get();
        Identifier id = Identifier.tryParse(source.id());
        if (id == null) return null;
        if (source.type() == MaMDataConfig.QueueSourceType.ALBUM) {
            for (Album album : Album.ALBUMS) {
                if (album.album.equals(id)) return new ContentItem(album, null);
            }
        } else if (source.type() == MaMDataConfig.QueueSourceType.PLAYLIST) {
            for (Playlist playlist : Playlist.PLAYLISTS) {
                if (playlist.playlist.equals(id)) return new ContentItem(null, playlist);
            }
        }
        return null;
    }

    private SourceInfo currentSource() {
        Optional<PlaylistHelper.QueueSource> queuedSource = PlaylistHelper.queueSource();
        if (queuedSource.isEmpty()) {
            return new SourceInfo(Component.translatable("screen.music_and_melody.custom_playlist"), MusicScreenHelper.FALLBACK_ALBUM_ICON,
                    sourceTypeLabel(MaMDataConfig.QueueSourceType.PLAYLIST), Component.translatable("screen.music_and_melody.content_origin.custom"), false);
        }
        PlaylistHelper.QueueSource source = queuedSource.get();
        Identifier id = Identifier.tryParse(source.id());
        if (id != null && source.type() == MaMDataConfig.QueueSourceType.ALBUM) {
            for (Album album : Album.ALBUMS) {
                if (album.album.equals(id)) return new SourceInfo(album.name, album.icon, sourceTypeLabel(MaMDataConfig.QueueSourceType.ALBUM), originFor(id, null), album.isFavourite());
            }
        }
        if (id != null && source.type() == MaMDataConfig.QueueSourceType.PLAYLIST) {
            for (Playlist playlist : Playlist.PLAYLISTS) {
                if (playlist.playlist.equals(id)) return new SourceInfo(playlist.name, playlist.icon, sourceTypeLabel(MaMDataConfig.QueueSourceType.PLAYLIST), originFor(id, playlist), playlist.isFavourite());
            }
        }
        return new SourceInfo(Component.literal(source.name()), MusicScreenHelper.FALLBACK_ALBUM_ICON,
                sourceTypeLabel(source.type()), Component.translatable("screen.music_and_melody.content_origin.built_in"), false);
    }

    private static Component sourceTypeLabel(MaMDataConfig.QueueSourceType type) {
        return Component.translatable(type == MaMDataConfig.QueueSourceType.ALBUM
                ? "screen.music_and_melody.tag.album"
                : "screen.music_and_melody.tag.playlist");
    }

    private static Component originFor(Identifier id, Playlist playlist) {
        return Component.translatable("screen.music_and_melody.content_origin." + originKeyFor(id, playlist));
    }

    private static String originKeyFor(Identifier id, Playlist playlist) {
        if (playlist != null && playlist.isCustom()) return "custom";
        return RemoteContentManager.isDownloadedAlbum(id) ? "downloaded" : "built_in";
    }

    /** Returns every tag that applies to an opened Album or Playlist. */
    private static Component contentTags(ContentItem item) {
        MutableComponent combined = Component.empty();
        boolean first = true;
        for (LibraryTag tag : LibraryTag.values()) {
            if (!tag.matches(item)) continue;
            if (!first) combined.append(Component.literal(" \u00b7 "));
            combined.append(tag.label());
            first = false;
        }
        return combined;
    }

    List<ContentItem> libraryItems() {
        List<ContentItem> items = new ArrayList<>();
        for (Album album : Album.ALBUMS) {
            ContentItem item = new ContentItem(album, null);
            if (matchesLibrary(item)) items.add(item);
        }
        for (Playlist playlist : Playlist.PLAYLISTS) {
            if (playlist.hidden) continue;
            ContentItem item = new ContentItem(null, playlist);
            if (matchesLibrary(item)) items.add(item);
        }
        items.sort(Comparator.comparing((ContentItem item) -> !item.favourite())
                .thenComparing(item -> item.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    private boolean matchesLibrary(ContentItem item) {
        for (LibraryTag tag : this.libraryTags) {
            if (!tag.matches(item)) return false;
        }
        return matchesSearch(item.name().getString(), item.id().toString(), item.details().getString());
    }

    List<ContentItem> favouriteItems() {
        List<ContentItem> items = new ArrayList<>();
        for (Album album : Album.ALBUMS) if (album.isFavourite()) items.add(new ContentItem(album, null));
        for (Playlist playlist : Playlist.PLAYLISTS) if (!playlist.hidden && playlist.isFavourite()) items.add(new ContentItem(null, playlist));
        items.sort(Comparator.<ContentItem>comparingInt(item -> PlaylistHelper.recentSourceRank(item.type(), item.id().toString()))
                .thenComparing(item -> item.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    List<Event.Source> visibleEventSources() {
        return Event.sources().stream()
                .filter(this::matchesEventTags)
                .filter(source -> matchesSearch(source.record.name().getString(), source.id.toString(), source.record.description().getString()))
                .sorted(Comparator.comparing(source -> source.record.name().getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean matchesEventTags(Event.Source source) {
        for (EventTag tag : this.eventTags) {
            if (!tag.matches(source)) return false;
        }
        return true;
    }

    List<String> eventNamespaces() {
        Map<String, List<Event.Source>> byNamespace = new LinkedHashMap<>();
        for (Event.Source source : visibleEventSources()) {
            byNamespace.computeIfAbsent(source.id.getNamespace(), ignored -> new ArrayList<>()).add(source);
        }
        return byNamespace.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    List<Event.Source> eventsInNamespace(String namespace) {
        return visibleEventSources().stream().filter(source -> source.id.getNamespace().equals(namespace)).toList();
    }

    List<String> onlineCatalogs() {
        return RemoteContentManager.packs().stream()
                .map(RemotePack::repository)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    List<RemotePack> onlinePacks(String catalog) {
        return RemoteContentManager.packs().stream()
                .filter(pack -> catalog == null || catalog.isEmpty() || pack.repository().equals(catalog))
                .filter(this::matchesOnlineTags)
                .filter(pack -> matchesSearch(pack.name().getString(), pack.id().toString(), pack.description().getString()))
                .sorted(Comparator.comparing(pack -> pack.name().getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean matchesOnlineTags(RemotePack pack) {
        for (OnlineTag tag : this.onlineTags) {
            if (!tag.matches(pack)) return false;
        }
        return true;
    }

    private boolean matchesSearch(String... values) {
        String query = this.search.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return true;
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private static Component playPauseMessage() {
        return Component.translatable(PlaylistHelper.isQueuePlaying() ? "button.music_and_melody.pause" : "button.music_and_melody.play");
    }

    private static Identifier shuffleIcon() {
        return IconButton.icon(PlaylistHelper.isShuffleQueue() ? "shuffle_on" : "shuffle_off");
    }

    private static Identifier playPauseIcon() {
        return IconButton.icon(PlaylistHelper.isQueuePlaying() ? "pause" : "play");
    }

    private static Component loopMessage() {
        return Component.translatable(PlaylistHelper.isLoopingQueue() ? "button.music_and_melody.looping" : "button.music_and_melody.loop");
    }

    private static Identifier loopIcon() {
        return IconButton.icon(PlaylistHelper.isLoopingQueue() ? "looping" : "loop");
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

    private static boolean remoteActionActive(RemotePack pack) {
        RemoteContentManager.State state = RemoteContentManager.state(pack);
        return state != RemoteContentManager.State.INSTALLED && state != RemoteContentManager.State.DOWNLOADING;
    }

    private static Component remoteStateMessage(RemoteContentManager.State state) {
        return Component.translatable("screen.music_and_melody.remote_album.state." + state.name().toLowerCase(Locale.ROOT));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000L;
        return seconds / 60L + ":" + String.format(Locale.ROOT, "%02d", seconds % 60L);
    }

    private void drawTruncated(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        FormattedCharSequence line = this.font.split(text, Math.max(1, width)).getFirst();
        graphics.text(this.font, line, x, y, color);
    }

    /**
     * Draws a title within a bounded area. Long titles pause briefly at both
     * ends instead of jumping back to the beginning.
     */
    private void drawMarquee(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        int textWidth = this.font.width(text);
        if (textWidth <= width) {
            graphics.text(this.font, text, x + (width - textWidth) / 2, y, color);
            return;
        }

        int travel = textWidth - width;
        long pause = 850L;
        long move = Math.max(1L, travel * 42L);
        long cycle = pause * 2L + move * 2L;
        long elapsed = Util.getMillis() % cycle;
        float fraction;
        if (elapsed < pause) fraction = 0.0F;
        else if (elapsed < pause + move) fraction = (elapsed - pause) / (float) move;
        else if (elapsed < pause + move + pause) fraction = 1.0F;
        else fraction = 1.0F - (elapsed - pause - move - pause) / (float) move;
        int offset = Math.round(travel * Math.max(0.0F, Math.min(1.0F, fraction)));

        graphics.enableScissor(x, y, x + width, y + this.font.lineHeight);
        graphics.text(this.font, text, x - offset, y, color);
        graphics.disableScissor();
    }

    /** A left-aligned marquee for compact track rows. */
    private void drawTrackMarquee(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        int textWidth = this.font.width(text);
        if (textWidth <= width) {
            graphics.text(this.font, text, x, y, color);
            return;
        }

        int travel = textWidth - width;
        long pause = 850L;
        long move = Math.max(1L, travel * 42L);
        long cycle = pause * 2L + move * 2L;
        long elapsed = Math.floorMod(Util.getMillis() + Math.abs(text.getString().hashCode()), cycle);
        float fraction;
        if (elapsed < pause) fraction = 0.0F;
        else if (elapsed < pause + move) fraction = (elapsed - pause) / (float) move;
        else if (elapsed < pause + move + pause) fraction = 1.0F;
        else fraction = 1.0F - (elapsed - pause - move - pause) / (float) move;
        int offset = Math.round(travel * Math.max(0.0F, Math.min(1.0F, fraction)));

        graphics.enableScissor(x, y, x + width, y + this.font.lineHeight);
        graphics.text(this.font, text, x - offset, y, color);
        graphics.disableScissor();
    }

    private List<Breadcrumb> breadcrumbsForCurrentPage() {
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        switch (this.page) {
            case HOME -> breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), null));
            case NOW_PLAYING -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.albums"), () -> setPage(Page.LIBRARY)));
                breadcrumbs.add(new Breadcrumb(currentSource().name(), null));
            }
            case LIBRARY -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.albums"), null));
            }
            case DETAILS -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.albums"), () -> {
                    this.viewedContent = null;
                    setPage(Page.LIBRARY);
                }));
                if (this.viewedContent != null) breadcrumbs.add(new Breadcrumb(this.viewedContent.name(), null));
            }
            case EVENTS -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("button.music_and_melody.events"), this.selectedEventNamespace == null ? null : () -> {
                    this.selectedEventNamespace = null;
                    this.rebuildWidgets();
                }));
                if (this.selectedEventNamespace != null) breadcrumbs.add(new Breadcrumb(eventFolderLabel(this.selectedEventNamespace), null));
            }
            case ONLINE -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.online_browser"), this.selectedOnlineCatalog == null ? null : () -> {
                    this.selectedOnlineCatalog = null;
                    this.viewedRemotePack = null;
                    this.rebuildWidgets();
                }));
                if (this.selectedOnlineCatalog != null) breadcrumbs.add(new Breadcrumb(this.selectedOnlineCatalog.isEmpty()
                        ? Component.translatable("screen.music_and_melody.all")
                        : Component.literal(this.selectedOnlineCatalog), null));
            }
            case THEMES -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.albums"), null));
            }
            case CONFIG -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.config"), null));
            }
        }
        return breadcrumbs;
    }

    /**
     * A file-explorer-like path that intentionally drops left-most entries
     * first.  This keeps the active folder or content name readable at narrow
     * resolutions.
     */
    private void renderBreadcrumbs(GuiGraphicsExtractor graphics, List<Breadcrumb> breadcrumbs) {
        if (breadcrumbs.isEmpty()) return;
        int left = this.middleX + 34;
        int right = this.middleX + this.middleWidth - 8;
        int available = Math.max(1, right - left);
        Component separator = Component.literal(" / ");
        int separatorWidth = this.font.width(separator);
        List<Breadcrumb> visible = new ArrayList<>();
        int used = 0;
        boolean omitted = false;

        for (int index = breadcrumbs.size() - 1; index >= 0; index--) {
            Breadcrumb breadcrumb = breadcrumbs.get(index);
            int labelWidth = this.font.width(breadcrumb.label());
            int required = labelWidth + (visible.isEmpty() ? 0 : separatorWidth);
            if (visible.isEmpty() && required > available) {
                visible.add(new Breadcrumb(tailToWidth(breadcrumb.label(), available), breadcrumb.action()));
                break;
            }
            if (used + required > available) {
                omitted = true;
                break;
            }
            visible.addFirst(breadcrumb);
            used += required;
        }

        Component omission = Component.literal("\u2026 / ");
        if (omitted) {
            int omissionWidth = this.font.width(omission);
            while (visible.size() > 1 && used + omissionWidth > available) {
                Breadcrumb removed = visible.removeFirst();
                used -= this.font.width(removed.label()) + separatorWidth;
            }
            graphics.text(this.font, omission, left, PANEL_TOP + 11, TEXT_DESCRIPTION);
            left += omissionWidth;
        }

        for (int index = 0; index < visible.size(); index++) {
            Breadcrumb breadcrumb = visible.get(index);
            int labelWidth = this.font.width(breadcrumb.label());
            int color = index == visible.size() - 1 ? TEXT_TITLE : TEXT_HEADER;
            graphics.text(this.font, breadcrumb.label(), left, PANEL_TOP + 11, color);
            if (breadcrumb.action() != null) {
                this.breadcrumbHits.add(new BreadcrumbHit(left, PANEL_TOP + 8, labelWidth, this.font.lineHeight + 5, breadcrumb.action()));
            }
            left += labelWidth;
            if (index < visible.size() - 1) {
                graphics.text(this.font, separator, left, PANEL_TOP + 11, TEXT_DESCRIPTION);
                left += separatorWidth;
            }
        }
    }

    private Component tailToWidth(Component text, int width) {
        if (this.font.width(text) <= width) return text;
        String raw = text.getString();
        int suffixStart = raw.length();
        while (suffixStart > 0 && this.font.width(Component.literal("\u2026" + raw.substring(suffixStart - 1))) <= width) suffixStart--;
        return Component.literal("\u2026" + raw.substring(suffixStart));
    }

    private interface Tag {
        Component label();
    }

    private record Breadcrumb(Component label, Runnable action) {}

    private record BreadcrumbHit(int x, int y, int width, int height, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private enum LibraryTag implements Tag {
        ALBUM("screen.music_and_melody.tag.album"),
        PLAYLIST("screen.music_and_melody.tag.playlist"),
        BUILT_IN("screen.music_and_melody.tag.built_in"),
        CUSTOM("screen.music_and_melody.tag.custom"),
        FAVOURITED("screen.music_and_melody.tag.favourited"),
        DOWNLOADED("screen.music_and_melody.tag.downloaded"),
        ENABLED("screen.music_and_melody.tag.enabled"),
        DISABLED("screen.music_and_melody.tag.disabled");

        private final String translationKey;

        LibraryTag(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public Component label() {
            return Component.translatable(this.translationKey);
        }

        boolean matches(ContentItem item) {
            return switch (this) {
                case ALBUM -> item.album() != null;
                case PLAYLIST -> item.playlist() != null;
                case BUILT_IN -> "built_in".equals(originKeyFor(item.id(), item.playlist()));
                case CUSTOM -> "custom".equals(originKeyFor(item.id(), item.playlist()));
                case FAVOURITED -> item.favourite();
                case DOWNLOADED -> "downloaded".equals(originKeyFor(item.id(), item.playlist()));
                case ENABLED -> item.album() == null || item.album().isEnabled();
                case DISABLED -> item.album() != null && !item.album().isEnabled();
            };
        }
    }

    private enum EventTag implements Tag {
        BUILT_IN("screen.music_and_melody.tag.built_in"),
        CUSTOM("screen.music_and_melody.tag.custom"),
        DOWNLOADED("screen.music_and_melody.tag.downloaded"),
        ENABLED("screen.music_and_melody.tag.enabled"),
        DISABLED("screen.music_and_melody.tag.disabled");

        private final String translationKey;

        EventTag(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public Component label() {
            return Component.translatable(this.translationKey);
        }

        boolean matches(Event.Source source) {
            return switch (this) {
                case BUILT_IN -> !source.isConfig();
                case CUSTOM -> source.isConfig();
                // Downloaded event origins are not distinguished until remote catalogs expose categories.
                case DOWNLOADED -> false;
                case ENABLED -> source.isEnabled();
                case DISABLED -> !source.isEnabled();
            };
        }
    }

    private enum OnlineTag implements Tag {
        ALBUM("screen.music_and_melody.tag.album"),
        PLAYLIST("screen.music_and_melody.tag.playlist"),
        EVENT("screen.music_and_melody.tag.event"),
        THEME("screen.music_and_melody.tag.album"),
        DOWNLOADED("screen.music_and_melody.tag.downloaded"),
        REMOTE("screen.music_and_melody.tag.remote"),
        NEEDS_UPDATE("screen.music_and_melody.tag.needs_update");

        private final String translationKey;

        OnlineTag(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public Component label() {
            return Component.translatable(this.translationKey);
        }

        boolean matches(RemotePack pack) {
            RemoteContentManager.State state = RemoteContentManager.state(pack);
            return switch (this) {
                case ALBUM -> true;
                case PLAYLIST, EVENT, THEME -> false;
                case DOWNLOADED -> state == RemoteContentManager.State.INSTALLED
                        || state == RemoteContentManager.State.NEEDS_RELOAD
                        || state == RemoteContentManager.State.UPDATE_AVAILABLE;
                case REMOTE -> state == RemoteContentManager.State.REMOTE || state == RemoteContentManager.State.FAILED;
                case NEEDS_UPDATE -> state == RemoteContentManager.State.UPDATE_AVAILABLE;
            };
        }
    }

    record ContentItem(Album album, Playlist playlist) {
        Component name() {
            return this.album != null ? this.album.name : this.playlist.name;
        }

        Identifier id() {
            return this.album != null ? this.album.album : this.playlist.playlist;
        }

        Identifier icon() {
            return this.album != null ? this.album.icon : this.playlist.icon;
        }

        MaMDataConfig.QueueSourceType type() {
            return this.album != null ? MaMDataConfig.QueueSourceType.ALBUM : MaMDataConfig.QueueSourceType.PLAYLIST;
        }

        boolean favourite() {
            return this.album != null ? this.album.isFavourite() : this.playlist.isFavourite();
        }

        Component details() {
            int tracks = this.album != null ? this.album.tracks.size() : this.playlist.tracks.size();
            int discs = this.album != null ? this.album.discs.size() : this.playlist.discs.size();
            Component tracksText = Component.translatable(tracks == 1
                    ? "screen.music_and_melody.track_count.single"
                    : "screen.music_and_melody.track_count.multiple", tracks);
            if (discs == 0) return tracksText;
            Component discsText = Component.translatable(discs == 1
                    ? "screen.music_and_melody.disc_count.single"
                    : "screen.music_and_melody.disc_count.multiple", discs);
            return Component.translatable("screen.music_and_melody.content_details", tracksText, discsText);
        }

        List<SafeIdentifier> queueSongs(Minecraft minecraft) {
            if (this.album != null) return AlbumDetailsScreen.queueSongs(this.album, minecraft);
            List<SafeIdentifier> songs = new ArrayList<>(this.playlist.tracks);
            this.playlist.discs.stream()
                    .map(disc -> MusicDiscHelper.discSoundId(minecraft, disc))
                    .flatMap(Optional::stream)
                    .map(SafeIdentifier::convert)
                    .filter(song -> !songs.contains(song))
                    .forEach(songs::add);
            return songs;
        }
    }

    private record SourceInfo(Component name, Identifier icon, Component typeLabel, Component originLabel, boolean favourite) {}

    /** The top-left card is a shortcut to the active queue, not a second track list. */
    private static final class CurrentSourceCard extends Button {
        private final MusicPlayerScreen screen;

        CurrentSourceCard(MusicPlayerScreen screen) {
            super(0, 0, 1, 1, Component.translatable("screen.music_and_melody.open_current_playlist"), ignored -> screen.openCurrentPlayback(), DEFAULT_NARRATION);
            this.screen = screen;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            boolean hovered = this.active && mouseX >= this.getX() && mouseY >= this.getY()
                    && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();
            int background = hovered ? BUTTON_HIGHLIGHT : SOURCE_CARD_BACKGROUND;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, hovered ? PANEL_HIGHLIGHT : PANEL_OUTLINE);

            SourceInfo source = this.screen.currentSource();
            int iconSize = Math.min(32, this.getHeight() - 8);
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.screen.minecraft, source.icon()),
                    this.getX() + 5, this.getY() + (this.getHeight() - iconSize) / 2,
                    0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            int textX = this.getX() + iconSize + 10;
            int textWidth = Math.max(1, this.getWidth() - iconSize - 15);
            this.screen.drawTruncated(graphics, source.name(), textX, this.getY() + 7, textWidth,
                    source.favourite() ? TEXT_FAVOURITE : TEXT_TITLE);
            this.screen.drawTruncated(graphics, Component.translatable("screen.music_and_melody.content_type_origin", source.typeLabel(), source.originLabel()),
                    textX, this.getY() + 21, textWidth, TEXT_DESCRIPTION);
        }
    }

    private static final class ContentTrackList extends PanelList<ContentTrackEntry> {
        ContentTrackList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, TRACK_ROW_HEIGHT);
            refresh();
        }

        void refresh() {
            this.clearEntries();
            if (this.screen.viewedContent == null) return;
            ContentItem content = this.screen.viewedContent;
            List<SafeIdentifier> queue = content.queueSongs(this.minecraft);
            if (content.playlist() != null && content.playlist().isCustom()) {
                for (int index = 0; index < queue.size(); index++) addSong(index, queue.get(index), null);
                return;
            }

            if (content.album() != null) {
                addAlbum(content.album(), queue);
            } else if (content.playlist() != null) {
                addPlaylist(content.playlist(), queue);
            }
        }

        private void addAlbum(Album album, List<SafeIdentifier> queue) {
            boolean tracksHeader = false;
            for (String track : album.tracks) {
                SafeIdentifier song = album.trackId(track);
                TrackStatus status = TrackStatus.forAlbumTrack(album, track);
                if (!matches(song, MusicScreenHelper.trackName(album, track))) continue;
                if (!tracksHeader) {
                    this.addEntry(ContentTrackEntry.header(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks")));
                    tracksHeader = true;
                }
                addSong(queue.indexOf(song), song, status);
            }

            boolean discsHeader = false;
            for (Album.StoredDisc disc : album.discs) {
                Identifier discId = MusicDiscHelper.albumEntryId(album, disc);
                Component name = MusicDiscHelper.discName(discId, disc);
                TrackStatus status = TrackStatus.forDisc(this.minecraft, discId);
                if (!matches(SafeIdentifier.convert(discId), name)) continue;
                Optional<Identifier> sound = MusicDiscHelper.discSoundId(this.minecraft, album, disc);
                if (sound.isEmpty()) continue;
                if (!discsHeader) {
                    this.addEntry(ContentTrackEntry.header(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs")));
                    discsHeader = true;
                }
                SafeIdentifier song = SafeIdentifier.convert(sound.get());
                addSong(queue.indexOf(song), song, status);
            }
        }

        private void addPlaylist(Playlist playlist, List<SafeIdentifier> queue) {
            boolean tracksHeader = false;
            for (SafeIdentifier song : playlist.tracks) {
                Component name = MusicScreenHelper.playlistName(this.minecraft, song);
                TrackStatus status = TrackStatus.forPlaylistTrack(song);
                if (!matches(song, name)) continue;
                if (!tracksHeader) {
                    this.addEntry(ContentTrackEntry.header(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks")));
                    tracksHeader = true;
                }
                addSong(queue.indexOf(song), song, status);
            }

            boolean discsHeader = false;
            for (Identifier disc : playlist.discs) {
                Component name = MusicDiscHelper.discName(disc);
                TrackStatus status = TrackStatus.forDisc(this.minecraft, disc);
                if (!matches(SafeIdentifier.convert(disc), name)) continue;
                Optional<Identifier> sound = MusicDiscHelper.discSoundId(this.minecraft, disc);
                if (sound.isEmpty()) continue;
                if (!discsHeader) {
                    this.addEntry(ContentTrackEntry.header(this.minecraft, Component.translatable("screen.music_and_melody.album_details.discs")));
                    discsHeader = true;
                }
                SafeIdentifier song = SafeIdentifier.convert(sound.get());
                addSong(queue.indexOf(song), song, status);
            }
        }

        private void addSong(int queueIndex, SafeIdentifier song, TrackStatus status) {
            if (queueIndex < 0) return;
            Component title = MusicScreenHelper.playlistName(this.minecraft, song);
            if (!matches(song, title)) return;
            this.addEntry(new ContentTrackEntry(this.screen, this.minecraft, queueIndex, song, status));
        }

        private boolean matches(SafeIdentifier song, Component title) {
            return this.screen.matchesSearch(title.getString(), song.toString());
        }
    }

    private static final class ContentTrackEntry extends ObjectSelectionList.Entry<ContentTrackEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final int queueIndex;
        private final SafeIdentifier song;
        private final Component heading;
        private final TrackStatus status;
        private final IconButton addButton;
        private final IconButton toggleButton;

        private ContentTrackEntry(Minecraft minecraft, Component heading) {
            this.screen = null;
            this.minecraft = minecraft;
            this.queueIndex = -1;
            this.song = null;
            this.heading = heading;
            this.status = null;
            this.addButton = null;
            this.toggleButton = null;
        }

        static ContentTrackEntry header(Minecraft minecraft, Component heading) {
            return new ContentTrackEntry(minecraft, heading);
        }

        ContentTrackEntry(MusicPlayerScreen screen, Minecraft minecraft, int queueIndex, SafeIdentifier song, TrackStatus status) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.queueIndex = queueIndex;
            this.song = song;
            this.heading = null;
            this.status = status;
            this.addButton = new IconButton(Component.translatable("button.music_and_melody.queue"), IconButton.icon("queue"), ignored ->
                    this.screen.requestQueueMutation(Component.translatable("screen.music_and_melody.queue_mutation.warning"), () -> PlaylistHelper.add(this.song)));
            this.toggleButton = status != null && status.toggleable()
                    ? new IconButton(status.message(), status.icon(), ignored -> this.screen.toggleContentTrack(status.album(), status.track()))
                    : null;
        }

        @Override
        public Component getNarration() {
            return this.heading != null ? this.heading : MusicScreenHelper.playlistName(this.minecraft, this.song);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (this.heading != null) {
                graphics.text(this.minecraft.font, this.heading.copy().withStyle(ChatFormatting.BOLD), this.getContentX() + 4,
                        this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, PANEL_HIGHLIGHT);
                return;
            }
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            boolean unlocked = MusicDiscHelper.isSoundUnlocked(this.minecraft, this.song);
            int numberX = this.getContentX() + TRACK_NUMBER_OFFSET;
            int textX = this.getContentX() + TRACK_TEXT_OFFSET;
            int buttons = IconButton.SIZE + (this.status == null ? 0 : IconButton.SIZE + 4);
            int textWidth = Math.max(1, this.getContentWidth() - TRACK_TEXT_OFFSET - buttons - 8);
            Component title = MusicScreenHelper.playlistName(this.minecraft, this.song);
            boolean enabled = this.status == null || this.status.enabled();
            int color = PlaylistHelper.isQueuePlaying(this.song) ? TEXT_SELECTED : enabled && unlocked ? TEXT_PRIMARY : TEXT_DISABLED;
            graphics.text(this.minecraft.font, Component.literal((this.queueIndex + 1) + "."), numberX,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, TEXT_DESCRIPTION);
            this.screen.drawTrackMarquee(graphics, title, textX,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, textWidth, color);
            int addX = this.getContentRight() - IconButton.SIZE - 3;
            this.addButton.setX(addX);
            this.addButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.addButton.active = !PlaylistHelper.queuedSongs().contains(this.song);
            this.addButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            if (this.status != null) {
                int statusX = addX - IconButton.SIZE - 4;
                if (this.toggleButton != null) {
                    this.toggleButton.setIconAndTooltip(this.status.icon(), this.status.message());
                    this.toggleButton.setX(statusX);
                    this.toggleButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
                    this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                } else {
                    IconButton.renderIconWithTooltip(graphics, this.status.icon(), statusX,
                            this.getContentYMiddle() - IconButton.SIZE / 2, this.status.message(), mouseX, mouseY);
                }
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.heading != null) return true;
            if (this.addButton.mouseClicked(event, doubleClick)) return true;
            if (this.toggleButton != null && this.toggleButton.mouseClicked(event, doubleClick)) return true;
            this.screen.playClick();
            this.screen.playContentTrack(this.queueIndex);
            return true;
        }
    }

    private record TrackStatus(Album album, String track, Identifier icon, Component message, boolean toggleable, boolean enabled) {
        static TrackStatus forAlbumTrack(Album album, String track) {
            if (album.isTrackForcedEnabled(track)) {
                return new TrackStatus(album, track, IconButton.icon("always_enabled"),
                        Component.translatable("screen.music_and_melody.album_details.enabled"), false, true);
            }
            boolean enabled = album.isTrackEnabled(track);
            return new TrackStatus(album, track, IconButton.icon(enabled ? "enabled" : "disabled"),
                    Component.translatable(enabled ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled"), true, enabled);
        }

        static TrackStatus forPlaylistTrack(SafeIdentifier song) {
            for (Album album : Album.ALBUMS) {
                for (String track : album.tracks) {
                    if (album.trackId(track).equals(song)) return forAlbumTrack(album, track);
                }
            }
            return null;
        }

        static TrackStatus forDisc(Minecraft minecraft, Identifier disc) {
            boolean unlocked = MusicDiscHelper.isDiscUnlocked(minecraft, disc);
            return new TrackStatus(null, null, IconButton.icon(unlocked ? "unlocked" : "locked"),
                    Component.translatable(unlocked ? "screen.music_and_melody.album_details.unlocked" : "screen.music_and_melody.album_details.locked"), false, unlocked);
        }
    }

    private abstract static class PanelList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
        final MusicPlayerScreen screen;
        final int panelX;
        final int panelWidth;

        PanelList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom, int rowHeight) {
            // Since 26.1 the third constructor argument is a height, not a
            // bottom coordinate.  Supplying the latter makes the list extend
            // from `top` by almost a whole screen and lets it cover the player
            // strip and neighbouring panels.
            super(minecraft, panelWidth, Math.max(1, bottom - top), top, rowHeight);
            this.screen = screen;
            this.panelX = panelX;
            this.panelWidth = panelWidth;
            this.setX(panelX);
            this.centerListVertically = false;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
            // The enclosing workspace panel supplies the background.  The
            // stock list background is full-screen styled and does not belong
            // inside a compact sub-panel.
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
            // Likewise, do not draw the stock header/footer separator across
            // a panel-local list.
        }

        @Override
        public int getRowLeft() {
            return this.panelX + 5;
        }

        @Override
        public int getRowWidth() {
            return Math.max(24, this.panelWidth - 14);
        }

        @Override
        protected int scrollBarX() {
            return this.panelX + this.panelWidth - 8;
        }

        @Override
        protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            if (!this.scrollable()) return;
            int x = scrollBarX();
            int top = this.getY() + 2;
            int bottom = this.getY() + this.getHeight() - 2;
            graphics.fill(x, top, x + 4, bottom, BUTTON_PASSIVE);
            int thumbTop = Math.max(top, this.scrollBarY());
            int thumbBottom = Math.min(bottom, thumbTop + this.scrollerHeight());
            int color = mouseX >= x - 2 && mouseX <= x + 6 && mouseY >= thumbTop && mouseY <= thumbBottom ? PANEL_HIGHLIGHT : SCROLLBAR_THUMB;
            graphics.fill(x, thumbTop, x + 4, thumbBottom, color);
        }
    }

    private static final class HomeMenuList extends PanelList<HomeMenuEntry> {
        HomeMenuList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, Math.max(top + 29, bottom), 29);
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.albums"), () -> screen.setPage(Page.LIBRARY)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("button.music_and_melody.events"), () -> screen.setPage(Page.EVENTS)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.themes"), () -> screen.setPage(Page.THEMES)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.online_browser"), () -> screen.setPage(Page.ONLINE)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.config"), () -> screen.setPage(Page.CONFIG)));
            this.addEntry(new HomeMenuEntry(screen, CommonComponents.GUI_DONE, screen::onClose));
        }
    }

    private static final class HomeMenuEntry extends ObjectSelectionList.Entry<HomeMenuEntry> {
        private final MusicPlayerScreen screen;
        private final WorkspaceButton button;

        HomeMenuEntry(MusicPlayerScreen screen, Component label, Runnable action) {
            this.screen = screen;
            this.button = new WorkspaceButton(0, 0, 1, 22, label, false, ignored -> action.run());
        }

        @Override
        public Component getNarration() {
            return this.button.getMessage();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int width = this.screen.mainMenuButtonWidth();
            this.button.setX(this.screen.middleX + (this.screen.middleWidth - width) / 2);
            this.button.setY(this.getContentY() + 3);
            this.button.setWidth(width);
            this.button.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.button.mouseClicked(event, doubleClick);
        }
    }

    private static final class TagFilterList<T extends Enum<T> & Tag> extends PanelList<TagFilterEntry<T>> {
        TagFilterList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom,
                      T[] values, Set<T> selected, java.util.function.Consumer<T> onToggle) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 24);
            for (T tag : values) {
                this.addEntry(new TagFilterEntry<>(screen, tag, selected, onToggle));
            }
        }
    }

    private static final class TagFilterEntry<T extends Enum<T> & Tag> extends ObjectSelectionList.Entry<TagFilterEntry<T>> {
        private final WorkspaceButton button;

        TagFilterEntry(MusicPlayerScreen screen, T tag, Set<T> selected, java.util.function.Consumer<T> onToggle) {
            this.button = new WorkspaceButton(0, 0, 1, 20, tag.label(), selected.contains(tag), ignored -> {
                onToggle.accept(tag);
                screen.rebuildWidgets();
            });
        }

        @Override
        public Component getNarration() {
            return this.button.getMessage();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.button.setX(this.getContentX());
            this.button.setY(this.getContentY());
            this.button.setWidth(this.getContentWidth());
            this.button.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.button.mouseClicked(event, doubleClick);
        }
    }

    static final class QueueList extends PanelList<QueueEntry> {
        final boolean compact;

        QueueList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom, boolean compact) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, compact ? 21 : TRACK_ROW_HEIGHT);
            this.compact = compact;
            refresh();
        }

        void refresh() {
            this.clearEntries();
            List<SafeIdentifier> songs = PlaylistHelper.queuedSongs();
            for (int index = 0; index < songs.size(); index++) {
                this.addEntry(new QueueEntry(this.screen, this, this.minecraft, index, songs.get(index), this.compact));
            }
        }

        int indexAt(double mouseX, double mouseY) {
            QueueEntry entry = this.getEntryAtPosition(mouseX, mouseY);
            return entry == null ? -1 : entry.index;
        }
    }

    private static final class QueueEntry extends ObjectSelectionList.Entry<QueueEntry> {
        private final MusicPlayerScreen screen;
        private final QueueList queueList;
        private final Minecraft minecraft;
        private final int index;
        private final SafeIdentifier song;
        private final boolean compact;
        private final IconButton removeButton;

        QueueEntry(MusicPlayerScreen screen, QueueList queueList, Minecraft minecraft, int index, SafeIdentifier song, boolean compact) {
            this.screen = screen;
            this.queueList = queueList;
            this.minecraft = minecraft;
            this.index = index;
            this.song = song;
            this.compact = compact;
            this.removeButton = new IconButton(Component.translatable("button.music_and_melody.remove"), IconButton.icon("remove"), ignored ->
                    this.screen.requestRemoveQueueTrack(this.index));
        }

        @Override
        public Component getNarration() {
            return MusicScreenHelper.playlistName(this.minecraft, this.song);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean dragging = this.screen.isDraggingQueue(this.index);
            int rowColor = dragging || hovered ? BUTTON_HIGHLIGHT : 0;
            if (rowColor != 0) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), rowColor);
            if (dragging) {
                int left = this.getContentX();
                int top = this.getContentY();
                int right = this.getContentRight();
                int bottom = this.getContentBottom();
                graphics.fill(left, top, right, top + 1, DRAG_OUTLINE);
                graphics.fill(left, bottom - 1, right, bottom, DRAG_OUTLINE);
                graphics.fill(left, top, left + 1, bottom, DRAG_OUTLINE);
                graphics.fill(right - 1, top, right, bottom, DRAG_OUTLINE);
            }
            int numberX = this.getContentX() + TRACK_NUMBER_OFFSET;
            int textX = this.getContentX() + TRACK_TEXT_OFFSET;
            int maxWidth = this.getContentWidth() - TRACK_TEXT_OFFSET - (this.compact ? 5 : IconButton.SIZE + 8);
            Component name = MusicScreenHelper.playlistName(this.minecraft, this.song);
            int color = PlaylistHelper.isQueuePlaying(this.song) ? TEXT_SELECTED : TEXT_PRIMARY;
            graphics.text(this.minecraft.font, Component.literal((this.index + 1) + "."), numberX, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, TEXT_DESCRIPTION);
            this.screen.drawTrackMarquee(graphics, name, textX,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, Math.max(1, maxWidth), color);
            if (!this.compact) {
                this.removeButton.setX(this.getContentRight() - IconButton.SIZE - 3);
                this.removeButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
                this.removeButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.compact && this.removeButton.mouseClicked(event, doubleClick)) return true;
            if (!this.compact && event.x() < this.getContentX() + TRACK_TEXT_OFFSET) {
                this.screen.playClick();
                this.screen.startQueueDrag(this.queueList, this.index);
                return true;
            }
            this.screen.playClick();
            this.screen.playQueueTrack(this.index);
            return true;
        }
    }

    private static final class FavouriteList extends PanelList<FavouriteEntry> {
        FavouriteList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 31);
            refresh();
        }

        void refresh() {
            this.clearEntries();
            this.screen.favouriteItems().stream()
                    .map(item -> new FavouriteEntry(this.screen, this.minecraft, item))
                    .forEach(this::addEntry);
        }
    }

    private static final class FavouriteEntry extends ObjectSelectionList.Entry<FavouriteEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final ContentItem item;

        FavouriteEntry(MusicPlayerScreen screen, Minecraft minecraft, ContentItem item) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.item = item;
        }

        @Override
        public Component getNarration() {
            return this.item.name();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            int iconSize = 22;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, this.item.icon()), this.getContentX() + 2, this.getContentYMiddle() - iconSize / 2, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            this.screen.drawTrackMarquee(graphics, this.item.name(), this.getContentX() + iconSize + 6,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2,
                    Math.max(1, this.getContentWidth() - iconSize - 8), TEXT_FAVOURITE);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.screen.playClick();
            this.screen.openContent(this.item);
            return true;
        }
    }

    private static final class LibraryList extends PanelList<LibraryEntry> {
        LibraryList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 39);
            refresh();
        }

        void refresh() {
            this.clearEntries();
            this.screen.libraryItems().stream()
                    .map(item -> new LibraryEntry(this.screen, this.minecraft, item))
                    .forEach(this::addEntry);
        }
    }

    private static final class LibraryEntry extends ObjectSelectionList.Entry<LibraryEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final ContentItem item;
        private final IconButton favouriteButton;
        private final IconButton albumEnabledButton;
        private final Identifier playlistOriginIcon;

        LibraryEntry(MusicPlayerScreen screen, Minecraft minecraft, ContentItem item) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.item = item;
            this.favouriteButton = new IconButton(Component.translatable("button.music_and_melody.favourite"), IconButton.icon(item.favourite() ? "favourited" : "favourite"), ignored -> this.screen.toggleFavourite(this.item));
            this.albumEnabledButton = item.album() == null ? null : new IconButton(
                    Component.translatable(item.album().isEnabled() ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled"),
                    IconButton.icon(item.album().isEnabled() ? "enabled" : "disabled"),
                    ignored -> this.screen.toggleAlbumEnabled(this.item.album()));
            this.playlistOriginIcon = item.playlist() == null ? null
                    : IconButton.icon(item.playlist().isCustom() ? "config" : "built_in");
        }

        @Override
        public Component getNarration() {
            return this.item.name();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            int iconSize = 30;
            int iconY = this.getContentYMiddle() - iconSize / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, this.item.icon()), this.getContentX() + 3, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            int textX = this.getContentX() + iconSize + 9;
            int actionWidth = IconButton.SIZE + (this.albumEnabledButton == null && this.playlistOriginIcon == null ? 0 : IconButton.SIZE + 4);
            int textWidth = this.getContentWidth() - iconSize - actionWidth - 16;
            this.screen.drawTrackMarquee(graphics, this.item.name(), textX, this.getContentYMiddle() - 10,
                    Math.max(1, textWidth), this.item.favourite() ? TEXT_FAVOURITE : TEXT_TITLE);
            this.screen.drawTrackMarquee(graphics, this.item.details(), textX, this.getContentYMiddle() + 2, Math.max(1, textWidth), TEXT_DESCRIPTION);
            this.favouriteButton.setIconAndTooltip(IconButton.icon(this.item.favourite() ? "favourited" : "favourite"), Component.translatable(this.item.favourite()
                    ? "button.music_and_melody.unfavourite"
                    : "button.music_and_melody.favourite"));
            int favouriteX = this.getContentRight() - IconButton.SIZE - 3;
            this.favouriteButton.setX(favouriteX);
            this.favouriteButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.favouriteButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            if (this.albumEnabledButton != null) {
                Album album = this.item.album();
                this.albumEnabledButton.setIconAndTooltip(IconButton.icon(album.isEnabled() ? "enabled" : "disabled"),
                        Component.translatable(album.isEnabled() ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled"));
                this.albumEnabledButton.setX(favouriteX - IconButton.SIZE - 4);
                this.albumEnabledButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
                this.albumEnabledButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            } else if (this.playlistOriginIcon != null) {
                Playlist playlist = this.item.playlist();
                IconButton.renderIconWithTooltip(graphics, this.playlistOriginIcon, favouriteX - IconButton.SIZE - 4,
                        this.getContentYMiddle() - IconButton.SIZE / 2,
                        Component.translatable(playlist.isCustom()
                                ? "screen.music_and_melody.content_origin.custom"
                                : "screen.music_and_melody.content_origin.built_in"), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.favouriteButton.mouseClicked(event, doubleClick)) return true;
            if (this.albumEnabledButton != null && this.albumEnabledButton.mouseClicked(event, doubleClick)) return true;
            this.screen.playClick();
            this.screen.openContent(this.item);
            return true;
        }
    }

    private static final class EventFolderList extends PanelList<EventFolderEntry> {
        EventFolderList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 30);
            refresh();
        }

        void refresh() {
            this.clearEntries();
            this.screen.eventNamespaces().stream()
                    .map(namespace -> new EventFolderEntry(this.screen, namespace, this.screen.eventsInNamespace(namespace).size()))
                    .forEach(this::addEntry);
        }
    }

    private static Component eventFolderLabel(String namespace) {
        String key = "event." + namespace + ".category";
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(namespace);
    }

    private static final class EventFolderEntry extends ObjectSelectionList.Entry<EventFolderEntry> {
        private final MusicPlayerScreen screen;
        private final String namespace;
        private final int count;

        EventFolderEntry(MusicPlayerScreen screen, String namespace, int count) {
            this.screen = screen;
            this.namespace = namespace;
            this.count = count;
        }

        @Override
        public Component getNarration() {
            return eventFolderLabel(this.namespace);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            graphics.text(this.screen.font, Component.literal("\u25B8 ").append(eventFolderLabel(this.namespace)), this.getContentX() + 4, this.getContentYMiddle() - this.screen.font.lineHeight / 2, TEXT_TITLE);
            Component suffix = Component.translatable(this.count == 1
                    ? "screen.music_and_melody.event_count.single"
                    : "screen.music_and_melody.event_count.multiple", this.count);
            int x = this.getContentRight() - this.screen.font.width(suffix) - 3;
            graphics.text(this.screen.font, suffix, x, this.getContentYMiddle() - this.screen.font.lineHeight / 2, TEXT_DESCRIPTION);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.screen.playClick();
            this.screen.chooseEventNamespace(this.namespace);
            return true;
        }
    }

    private static final class EventSourceList extends PanelList<EventSourceEntry> {
        private final String namespace;

        EventSourceList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom, String namespace) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 38);
            this.namespace = namespace;
            refresh();
        }

        void refresh() {
            this.clearEntries();
            this.screen.eventsInNamespace(this.namespace).stream()
                    .map(source -> new EventSourceEntry(this.screen, this.minecraft, source))
                    .forEach(this::addEntry);
        }
    }

    private static final class EventSourceEntry extends ObjectSelectionList.Entry<EventSourceEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final Event.Source source;
        private final IconButton toggleButton;

        EventSourceEntry(MusicPlayerScreen screen, Minecraft minecraft, Event.Source source) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.source = source;
            this.toggleButton = new IconButton(Component.translatable(source.isEnabled() ? "button.music_and_melody.disable" : "button.music_and_melody.enable"), IconButton.icon(source.isEnabled() ? "enabled" : "disabled"), ignored -> {
                this.source.setEnabled(!this.source.isEnabled());
                if (this.screen.eventSourceList != null) this.screen.eventSourceList.refresh();
            });
        }

        @Override
        public Component getNarration() {
            return this.source.record.name();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            int iconSize = 28;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, this.source.icon()), this.getContentX() + 3, this.getContentYMiddle() - iconSize / 2, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            int textX = this.getContentX() + iconSize + 8;
            int textWidth = this.getContentWidth() - iconSize - IconButton.SIZE - 17;
            this.screen.drawTrackMarquee(graphics, this.source.record.name(), textX, this.getContentYMiddle() - 10,
                    Math.max(1, textWidth), this.source.isEnabled() ? TEXT_TITLE : TEXT_DESCRIPTION);
            this.screen.drawTrackMarquee(graphics, this.source.record.description(), textX, this.getContentYMiddle() + 2, Math.max(1, textWidth), TEXT_DESCRIPTION);
            this.toggleButton.setIconAndTooltip(IconButton.icon(this.source.isEnabled() ? "enabled" : "disabled"), Component.translatable(this.source.isEnabled()
                    ? "button.music_and_melody.disable"
                    : "button.music_and_melody.enable"));
            this.toggleButton.setX(this.getContentRight() - IconButton.SIZE - 3);
            this.toggleButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.toggleButton.mouseClicked(event, doubleClick)) return true;
            this.screen.playClick();
            this.screen.openEvent(this.source);
            return true;
        }
    }

    private static final class OnlineCatalogList extends PanelList<OnlineCatalogEntry> {
        OnlineCatalogList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 30);
            refresh();
        }

        void refresh() {
            this.clearEntries();
            this.addEntry(new OnlineCatalogEntry(this.screen, Component.translatable("screen.music_and_melody.all"), ""));
            this.screen.onlineCatalogs().stream()
                    .map(catalog -> new OnlineCatalogEntry(this.screen, Component.literal(catalog), catalog))
                    .forEach(this::addEntry);
            this.addEntry(OnlineCatalogEntry.addRepository(this.screen));
        }
    }

    private static final class OnlineCatalogEntry extends ObjectSelectionList.Entry<OnlineCatalogEntry> {
        private final MusicPlayerScreen screen;
        private final Component label;
        private final String catalog;
        private final boolean addRepository;

        OnlineCatalogEntry(MusicPlayerScreen screen, Component label, String catalog) {
            this(screen, label, catalog, false);
        }

        private OnlineCatalogEntry(MusicPlayerScreen screen, Component label, String catalog, boolean addRepository) {
            this.screen = screen;
            this.label = label;
            this.catalog = catalog;
            this.addRepository = addRepository;
        }

        static OnlineCatalogEntry addRepository(MusicPlayerScreen screen) {
            return new OnlineCatalogEntry(screen, Component.translatable("button.music_and_melody.add_repository"), "", true);
        }

        @Override
        public Component getNarration() {
            return this.label;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            int color = this.addRepository ? PANEL_HIGHLIGHT : TEXT_TITLE;
            Component text = this.addRepository ? this.label : Component.literal("\u25B8 ").append(this.label);
            graphics.text(this.screen.font, text, this.getContentX() + 4, this.getContentYMiddle() - this.screen.font.lineHeight / 2, color);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.screen.playClick();
            if (this.addRepository) this.screen.openRepositoryEditor();
            else this.screen.chooseOnlineCatalog(this.catalog);
            return true;
        }
    }

    private static final class OnlinePackList extends PanelList<OnlinePackEntry> {
        private final String catalog;

        OnlinePackList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom, String catalog) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, 39);
            this.catalog = catalog;
            refresh();
        }

        void refresh() {
            this.clearEntries();
            this.screen.onlinePacks(this.catalog).stream()
                    .map(pack -> new OnlinePackEntry(this.screen, this.minecraft, pack))
                    .forEach(this::addEntry);
        }
    }

    private static final class OnlinePackEntry extends ObjectSelectionList.Entry<OnlinePackEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final RemotePack pack;
        private final IconButton actionButton;

        OnlinePackEntry(MusicPlayerScreen screen, Minecraft minecraft, RemotePack pack) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.pack = pack;
            this.actionButton = new IconButton(Component.translatable("button.music_and_melody.download"), IconButton.icon("download"), ignored -> {
                if (RemoteContentManager.state(this.pack) == RemoteContentManager.State.INSTALLED) {
                    this.screen.toggleRemoteDeletePending(this.pack);
                } else {
                    this.screen.activateRemotePack(this.pack);
                }
            });
        }

        @Override
        public Component getNarration() {
            return this.pack.name();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            int iconSize = 30;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, RemoteIconManager.icon(this.pack)), this.getContentX() + 3, this.getContentYMiddle() - iconSize / 2, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            int textX = this.getContentX() + iconSize + 9;
            int textWidth = this.getContentWidth() - iconSize - IconButton.SIZE - 16;
            int titleColor = this.screen.isRemoteDeletePending(this.pack) ? TEXT_PENDING_DELETION : TEXT_TITLE;
            this.screen.drawTrackMarquee(graphics, this.pack.name(), textX, this.getContentYMiddle() - 10, Math.max(1, textWidth), titleColor);
            this.screen.drawTrackMarquee(graphics, remoteStateMessage(RemoteContentManager.state(this.pack)), textX,
                    this.getContentYMiddle() + 2, Math.max(1, textWidth), TEXT_DESCRIPTION);
            updateAction();
            this.actionButton.setX(this.getContentRight() - IconButton.SIZE - 3);
            this.actionButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.actionButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        private void updateAction() {
            RemoteContentManager.State state = RemoteContentManager.state(this.pack);
            if (state == RemoteContentManager.State.INSTALLED) {
                this.actionButton.setIconAndTooltip(IconButton.icon(this.screen.isRemoteDeletePending(this.pack) ? "restore" : "delete"),
                        this.screen.remoteDeleteMessage(this.pack));
                this.actionButton.active = remoteDeleteAvailable(this.pack);
                return;
            }
            Identifier icon = switch (state) {
                case DOWNLOADING -> IconButton.icon("downloading");
                case NEEDS_RELOAD -> IconButton.icon("reload");
                case UPDATE_AVAILABLE -> IconButton.icon("update");
                case FAILED -> IconButton.icon("retry_download");
                case INSTALLED -> throw new IllegalStateException("Handled above");
                case REMOTE -> IconButton.icon("download");
            };
            this.actionButton.setIconAndTooltip(icon, remoteActionMessage(this.pack));
            this.actionButton.active = state != RemoteContentManager.State.INSTALLED && state != RemoteContentManager.State.DOWNLOADING;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.actionButton.mouseClicked(event, doubleClick)) return true;
            this.screen.playClick();
            this.screen.viewRemotePack(this.pack);
            return true;
        }
    }
}
