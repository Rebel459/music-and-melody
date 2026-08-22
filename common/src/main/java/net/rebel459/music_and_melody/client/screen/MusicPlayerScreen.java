package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

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
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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
import net.rebel459.music_and_melody.client.Theme;
import net.rebel459.music_and_melody.client.ThemeListener;
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
import net.rebel459.music_and_melody.client.util.CustomAlbums;
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

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

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
    private static final int HOME_MENU_TOP = 64;
    private static final int HOME_MENU_HEIGHT = 22 + (HOME_BUTTON_COUNT - 1) * HOME_BUTTON_STEP;

    private final Screen parent;
    private Page page;
    private static Page lastOpenedPage;

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
    private ThemeList themeList;
    private TagFilterList<?> tagFilterList;

    private IconButton searchButton;
    private EditBox searchField;
    private IconButton shuffleButton;
    private IconButton previousButton;
    private IconButton playPauseButton;
    private IconButton nextButton;
    private IconButton loopButton;
    private IconButton saveButton;
    private IconButton replaceButton;
    private IconButton clearButton;
    private WorkspaceButton vanillaMusicButton;
    private WorkspaceButton eventsButton;
    private WorkspaceButton loadButton;
    private WorkspaceButton queueButton;
    private IconButton contentDeleteButton;
    private IconButton contentManageButton;
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
    private final Set<ThemeTag> themeTags = EnumSet.noneOf(ThemeTag.class);
    private final Set<OnlineTag> onlineTags = EnumSet.noneOf(OnlineTag.class);
    private String selectedEventNamespace;
    private String selectedOnlineCatalog;
    private ContentItem viewedContent;
    private RemotePack viewedRemotePack;
    private Theme viewedTheme;
    private Identifier selectedThemeId;
    private boolean previewingTheme;
    private final Set<RemotePack.Key> pendingRemoteDeletes = new HashSet<>();
    private final Set<Identifier> pendingPlaylistDeletes = new HashSet<>();
    private final Set<Identifier> pendingConfigAlbumDeletes = new HashSet<>();
    private final Set<Identifier> pendingEventDeletes = new HashSet<>();
    private final Set<Identifier> pendingThemeDeletes = new HashSet<>();
    private final List<BreadcrumbHit> breadcrumbHits = new ArrayList<>();
    private final List<BreadcrumbHit> welcomeLinks = new ArrayList<>();
    private String featuredSupporter;
    private String featuredComposer;
    private String featuredComposerUrl;
    private String featuredSplash;
    private int welcomeSocialY;
    private double welcomeScroll;
    private double welcomeScrollMax;
    private int welcomeViewportTop;
    private int welcomeViewportBottom;
    private boolean renderingWelcomeScrollableContent;
    private boolean draggingWelcomeScrollbar;
    private double welcomeScrollbarDragOffset;

    public MusicPlayerScreen(Screen parent) {
        this(parent, Page.NOW_PLAYING);
    }

    public MusicPlayerScreen(Screen parent, Page page) {
        super(TITLE);
        this.parent = parent;
        this.page = page;
    }

    public static MusicPlayerScreen openLast(Screen parent) {
        Page page = lastOpenedPage == null ? Page.HOME
                : lastOpenedPage == Page.DETAILS ? Page.LIBRARY : lastOpenedPage;
        return new MusicPlayerScreen(parent, page);
    }

    @Override
    protected void init() {
        lastOpenedPage = this.page;
        calculateLayout();
        loadCachedWelcomeValues();
        MusicDiscHelper.requestStats(this.minecraft);
        RemoteContentManager.refreshIfNeeded();
        RemoteContentManager.refreshCredits();
        this.tagFilterList = null;

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

    public void onStatsUpdated() {
        if (this.page == Page.DETAILS) rebuildWidgets();
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
            case THEMES -> this.themeList == null ? 0.0D : this.themeList.scrollAmount();
            case HOME, CONFIG -> 0.0D;
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
            case THEMES -> {
                if (this.themeList != null) this.themeList.setScrollAmount(scrollAmount);
            }
            case HOME, CONFIG -> {
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
                ignored -> openCustomPlaylist()));
        this.favouriteList = this.addRenderableWidget(new FavouriteList(this, this.minecraft, this.leftX, this.leftWidth,
                favouriteTop, this.panelBottom - 34));
        this.addRenderableWidget(new WorkspaceButton(this.leftX + 5, this.panelBottom - 28, this.leftWidth - 10, 20,
                Component.translatable("button.music_and_melody.exit"), false, ignored -> exitToParent()));
    }

    private void buildPlaybackStrip() {
        int buttonY = this.bottomPanelTop + 29;
        int groupWidth = IconButton.SIZE * 5 + 4 * 4;
        int groupX = this.middleX + this.middleWidth / 2 - groupWidth / 2;

        this.searchButton = this.addRenderableWidget(new IconButton(Component.translatable("screen.music_and_melody.search"), IconButton.icon("search"), button -> toggleSearch()));
        this.searchButton.setX(this.middleX + 8);
        this.searchButton.setY(this.bottomPanelTop + 5);

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
                openSavePlaylistScreen()));
        int actionGroupWidth = IconButton.SIZE * 3 + 5 * 2;
        int actionX = this.rightX + (this.rightWidth - actionGroupWidth) / 2;
        this.saveButton.setX(actionX);
        this.saveButton.setY(playerActionY());

        this.replaceButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.replace"), IconButton.icon("replace"), button ->
                openReplacePlaylistScreen()));
        this.replaceButton.setX(actionX + IconButton.SIZE + 5);
        this.replaceButton.setY(playerActionY());

        this.clearButton = this.addRenderableWidget(new IconButton(Component.translatable("button.music_and_melody.clear"), IconButton.icon("clear"), button ->
                requestClearQueue()));
        this.clearButton.setX(actionX + (IconButton.SIZE + 5) * 2);
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
        if (this.viewedRemotePack != null) {
            buildRemoteDetailsAction();
            return;
        }
        int actionWidth = this.rightWidth - 14;
        int buttonWidth = Math.max(1, (actionWidth - IconButton.SIZE - 8) / 2);
        int actionX = this.rightX + 7;
        int actionY = playerActionY();
        this.loadButton = this.addRenderableWidget(new WorkspaceButton(actionX, actionY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.load"), false, button -> loadViewedContent()));
        int deleteX = actionX + buttonWidth + 4;
        RemotePack remotePack = viewedContentRemotePack();
        if (remotePack != null) {
            this.contentManageButton = this.addRenderableWidget(new IconButton(
                    Component.translatable("button.music_and_melody.manage"), IconButton.icon("manage"),
                    button -> viewRemotePack(remotePack)));
            this.contentManageButton.setX(deleteX);
            this.contentManageButton.setY(actionY);
        } else if (viewedContentDeleteable()) {
            this.contentDeleteButton = this.addRenderableWidget(new IconButton(contentDeleteMessage(), contentDeleteIcon(),
                    button -> toggleViewedContentDelete()));
            this.contentDeleteButton.setX(deleteX);
            this.contentDeleteButton.setY(actionY);
        }
        int queueX = deleteX + IconButton.SIZE + 4;
        this.queueButton = this.addRenderableWidget(new WorkspaceButton(queueX, actionY, actionX + actionWidth - queueX, 20,
                Component.translatable("button.music_and_melody.queue"), false, button -> queueViewedContent()));
        buildMusicToggles();
    }

    private void buildMusicToggles() {
        int togglesY = musicToggleY();
        int toggleWidth = this.rightWidth - 14;
        this.vanillaMusicButton = this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, togglesY, toggleWidth, 20,
                Component.translatable("screen.music_and_melody.vanilla_music"), MaMDataConfig.get().vanilla_music, button -> toggleVanillaMusic()));
        this.eventsButton = this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, togglesY + 24, toggleWidth, 20,
                Component.translatable("screen.music_and_melody.event_music"), MaMDataConfig.get().event_music, button -> toggleEventMusic()));
    }

    private void buildLibraryPage() {
        addBackButton();
        this.libraryList = this.addRenderableWidget(new LibraryList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6));
        buildTagButtons(LibraryTag.values(), this.libraryTags, this::toggleLibraryTag);
        this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, this.panelBottom - 28, this.rightWidth - 14, 20,
                Component.translatable("button.music_and_melody.new_album"), false,
                button -> openConfigAlbumEditor(null)));
    }

    private void buildEventsPage() {
        if (!MaMClientConfig.get().allow_events) {
            setPage(Page.HOME);
            return;
        }
        addBackButton();
        if (this.viewedRemotePack == null) buildTagButtons(EventTag.values(), this.eventTags, this::toggleEventTag);
        else buildRemoteDetailsAction();
        if (this.selectedEventNamespace == null) {
            this.eventFolderList = this.addRenderableWidget(new EventFolderList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6));
        } else {
            this.eventSourceList = this.addRenderableWidget(new EventSourceList(this, this.minecraft, this.middleX, this.middleWidth, PANEL_TOP + 38, this.contentBottom - 6, this.selectedEventNamespace));
        }
        if (this.viewedRemotePack == null) {
            this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, this.panelBottom - 28, this.rightWidth - 14, 20,
                    Component.translatable("button.music_and_melody.new_event"), false,
                    button -> openCreateEventScreen()));
        }
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
        buildWelcomePanel();
        int listTop = PANEL_TOP + HOME_MENU_TOP;
        int listBottom = this.contentBottom - 6;
        if (listBottom - listTop < HOME_MENU_HEIGHT) {
            this.addRenderableWidget(new HomeMenuList(this, this.minecraft, this.middleX, this.middleWidth, listTop, listBottom));
            return;
        }
        int buttonWidth = mainMenuButtonWidth();
        int x = this.middleX + this.middleWidth / 2 - buttonWidth / 2;
        int y = PANEL_TOP + Math.max(HOME_MENU_TOP, (this.contentBottom - PANEL_TOP - HOME_MENU_HEIGHT) / 2);
        addHomeButton(Component.translatable("screen.music_and_melody.albums"), x, y, buttonWidth, () -> setPage(Page.LIBRARY));
        WorkspaceButton events = this.addRenderableWidget(new WorkspaceButton(x, y + HOME_BUTTON_STEP, buttonWidth, 22,
                Component.translatable("button.music_and_melody.events"), false, button -> setPage(Page.EVENTS)));
        events.active = MaMClientConfig.get().allow_events;
        addHomeButton(Component.translatable("screen.music_and_melody.themes"), x, y + HOME_BUTTON_STEP * 2, buttonWidth, () -> setPage(Page.THEMES));
        WorkspaceButton onlineBrowser = this.addRenderableWidget(new WorkspaceButton(x, y + HOME_BUTTON_STEP * 3, buttonWidth, 22,
                Component.translatable("screen.music_and_melody.online_browser"), false, button -> setPage(Page.ONLINE)));
        onlineBrowser.active = RemoteContentManager.onlineFunctionalityEnabled();
        addHomeButton(Component.translatable("screen.music_and_melody.config"), x, y + HOME_BUTTON_STEP * 4, buttonWidth, () -> setPage(Page.CONFIG));
        addHomeButton(CommonComponents.GUI_DONE, x, y + HOME_BUTTON_STEP * 5, buttonWidth, this::onClose);
    }

    private void buildThemesPage() {
        addBackButton();
        this.themeList = this.addRenderableWidget(new ThemeList(this, this.minecraft, this.middleX, this.middleWidth,
                PANEL_TOP + 38, this.contentBottom - 6));
        if (this.viewedRemotePack != null) {
            buildRemoteDetailsAction();
        } else if (this.viewedTheme == null) {
            buildTagButtons(ThemeTag.values(), this.themeTags, this::toggleThemeTag);
            this.addRenderableWidget(new WorkspaceButton(this.rightX + 7, this.panelBottom - 28, this.rightWidth - 14, 20,
                    Component.translatable("button.music_and_melody.new_theme"), false,
                    button -> openCreateThemeScreen()));
        } else {
            buildThemeDetailsActions();
        }
    }

    private void buildConfigPage() {
        buildWelcomePanel();
        addBackButton();
        int buttonWidth = mainMenuButtonWidth();
        int x = this.middleX + this.middleWidth / 2 - buttonWidth / 2;
        int y = PANEL_TOP + 64;
        this.addRenderableWidget(new WorkspaceButton(x, y, buttonWidth, 22,
                Component.translatable("button.music_and_melody.client"), false,
                button -> this.minecraft.gui.setScreen(AutoConfigClient.getConfigScreen(MaMClientConfig.class, this).get())));
        this.addRenderableWidget(new WorkspaceButton(x, y + HOME_BUTTON_STEP, buttonWidth, 22,
                Component.translatable("button.music_and_melody.server"), false,
                button -> this.minecraft.gui.setScreen(AutoConfigClient.getConfigScreen(MaMServerConfig.class, this).get())));
        this.addRenderableWidget(new GuiMultiplierSlider(this, x, y + HOME_BUTTON_STEP * 2, buttonWidth, 20));
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
                Component.translatable("gui.done"), false, button -> closeRemoteDetails()));

        RemoteContentManager.State state = RemoteContentManager.state(pack);
        if (state == RemoteContentManager.State.DOWNLOADING) return;

        boolean canDelete = remoteDeleteAvailable(pack);
        if (remoteActionActive(pack) && !isRemoteDeletePending(pack)) {
            this.remoteActionButton = this.addRenderableWidget(new WorkspaceButton(x, backY - 24, width, 20,
                    remoteActionMessage(pack), false, button -> activateRemotePack(pack)));
            return;
        }

        if (canDelete) {
            this.remoteDeleteButton = this.addRenderableWidget(new WorkspaceButton(x, backY - 24, width, 20,
                    remoteDeleteMessage(pack), isRemoteDeletePending(pack), button -> toggleRemoteDeletePending(pack)));
            return;
        }

    }

    private void buildThemeDetailsActions() {
        if (this.viewedTheme == null) return;
        int x = this.rightX + 7;
        int width = this.rightWidth - 14;
        int doneY = this.panelBottom - 28;
        int openY = doneY - 24;
        int applyY = openY - 24;
        int previewY = themePreviewY();
        WorkspaceButton preview = this.addRenderableWidget(new WorkspaceButton(x, previewY, width, 20,
                Component.translatable("button.music_and_melody.preview"), this.previewingTheme,
                button -> previewTheme(this.viewedTheme)));
        WorkspaceButton apply = this.addRenderableWidget(new WorkspaceButton(x, applyY, width, 20,
                Component.translatable("button.music_and_melody.apply"), false,
                button -> applyTheme(this.viewedTheme)));
        WorkspaceButton open = this.addRenderableWidget(new WorkspaceButton(x, openY, width, 20,
                Component.translatable("button.music_and_melody.open_theme"), false,
                button -> openThemeEditor(this.viewedTheme)));
        this.addRenderableWidget(new WorkspaceButton(x, doneY, width, 20, CommonComponents.GUI_DONE, false,
                button -> finishThemePreview()));
        boolean valid = this.viewedTheme.valid;
        preview.active = valid;
        apply.active = valid && !isActiveTheme(this.viewedTheme);
        open.active = true;
    }

    private static boolean isActiveTheme(Theme theme) {
        Theme active = ThemeListener.activeTheme();
        return theme != null && active != null && active.theme.equals(theme.theme);
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

    private void toggleThemeTag(ThemeTag tag) {
        toggleTag(this.themeTags, tag);
    }

    private static <T> void toggleTag(Set<T> tags, T tag) {
        if (!tags.add(tag)) tags.remove(tag);
    }

    private void renderShell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, BACKGROUND);
        drawPanel(graphics, this.leftX, PANEL_TOP, this.leftWidth, this.panelBottom - PANEL_TOP);
        drawPanel(graphics, this.middleX, PANEL_TOP, this.middleWidth, this.contentBottom - PANEL_TOP);
        drawPanel(graphics, this.middleX, this.bottomPanelTop, this.middleWidth, this.panelBottom - this.bottomPanelTop);
        drawPanel(graphics, this.rightX, PANEL_TOP, this.rightWidth, this.panelBottom - PANEL_TOP);

        MutableComponent playbackHeading = Component.translatable(PlaylistHelper.isPlaying()
                ? "screen.music_and_melody.now_playing"
                : "screen.music_and_melody.last_played");
        ThemeHelper.text(graphics, this.font, playbackHeading.withStyle(ChatFormatting.BOLD), this.leftX + 8, PANEL_TOP + 11, TEXT_HEADER);
        int favouriteHeaderY = PANEL_TOP + 31 + 75;
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.favourites").withStyle(ChatFormatting.BOLD), this.leftX + 8, favouriteHeaderY + 3, TEXT_HEADER);

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
                SourceInfo source = customPlaylistSource();
                renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
                ThemeHelper.text(graphics, this.font, fittedContentDetails(source.typeLabel(), source.originLabel(), this.middleWidth - 42),
                        this.middleX + 34, PANEL_TOP + 27, TEXT_DESCRIPTION);
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
            }
            case HOME -> {
                renderBreadcrumbs(graphics, breadcrumbsForCurrentPage());
                ThemeHelper.centeredText(graphics, this.font, Component.translatable("screen.music_and_melody.music_player").withStyle(ChatFormatting.BOLD), this.middleX + this.middleWidth / 2, PANEL_TOP + 34, TEXT_TITLE);
                if (RemoteContentManager.onlineFunctionalityEnabled()) {
                    updateFeaturedCredits();
                    Component splash = this.featuredSplash == null && RemoteContentManager.creditsLoading()
                            ? Component.translatable("screen.music_and_melody.loading")
                            : this.featuredSplash == null ? Component.translatable("screen.music_and_melody.choose_section")
                            : Component.literal(this.featuredSplash);
                    ThemeHelper.centeredText(graphics, this.font, splash, this.middleX + this.middleWidth / 2, PANEL_TOP + 48, TEXT_DESCRIPTION);
                }
            }
        }
    }

    private void renderRightPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.viewedRemotePack != null) {
            renderRemoteDetails(graphics, this.viewedRemotePack);
            return;
        }

        if (this.page == Page.NOW_PLAYING || this.page == Page.DETAILS) {
            SourceInfo source = this.page == Page.NOW_PLAYING ? customPlaylistSource() : viewedContentSource();
            if (source != null) {
                renderSourceCard(graphics, source);
                renderVolumeSlider(graphics, mouseX, mouseY);
            }
            if (this.page == Page.DETAILS && !viewedContentDeleteable() && viewedContentRemotePack() == null) {
                int actionWidth = this.rightWidth - 14;
                int buttonWidth = Math.max(1, (actionWidth - IconButton.SIZE - 8) / 2);
                int iconX = this.rightX + 7 + buttonWidth + 4;
                IconButton.renderIconWithTooltip(graphics, IconButton.icon("built_in"), iconX, playerActionY(),
                        Component.translatable("screen.music_and_melody.content_origin.built_in"), mouseX, mouseY);
            }
            return;
        }

        if (this.page == Page.THEMES && this.viewedTheme != null) {
            renderThemeDetails(graphics, this.viewedTheme);
            return;
        }

        Component title = switch (this.page) {
            case LIBRARY, EVENTS, ONLINE, THEMES -> Component.translatable("screen.music_and_melody.filter_by_tags");
            case HOME, CONFIG, NOW_PLAYING, DETAILS -> Component.empty();
        };
        if (!title.getString().isEmpty()) ThemeHelper.text(graphics, this.font, title.copy().withStyle(ChatFormatting.BOLD), this.rightX + 8, PANEL_TOP + 14, TEXT_HEADER);
        if (this.page == Page.HOME || this.page == Page.CONFIG) renderWelcomePanel(graphics, mouseX, mouseY);
        if (this.page == Page.ONLINE) renderOnlineDownloadProgress(graphics);
    }

    private SourceInfo viewedContentSource() {
        if (this.viewedContent == null) return null;
        return new SourceInfo(this.viewedContent.name(), this.viewedContent.icon(),
                Component.literal(this.viewedContent.id().toString()),
                sourceTypeLabel(this.viewedContent.type()), originFor(this.viewedContent.id(), this.viewedContent.playlist()),
                this.viewedContent.favourite());
    }

    private void renderSourceCard(GuiGraphicsExtractor graphics, SourceInfo source) {
        int cardY = PANEL_TOP + 10;
        int cardSize = sourceCardSize();
        int cardX = this.rightX + (this.rightWidth - cardSize) / 2;
        Identifier icon = MusicScreenHelper.albumIcon(this.minecraft, source.icon());
        int iconSize = Math.max(20, cardSize - 42);
        int iconX = cardX + (cardSize - iconSize) / 2;
        int iconY = cardY + 9;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int titleColor = this.page == Page.DETAILS && isContentDeletePending(this.viewedContent)
                ? TEXT_PENDING_DELETION : source.favourite() ? TEXT_FAVOURITE : TEXT_TITLE;
        drawMarquee(graphics, source.name(), cardX + 4, cardY + cardSize - 25, cardSize - 8, titleColor);
        drawMarquee(graphics, source.id(), cardX + 4, cardY + cardSize - 12, cardSize - 8, TEXT_DESCRIPTION);
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
            graphics.setTooltipForNextFrame(Component.translatable("screen.music_and_melody.music_volume", Math.round(volume * 100F)),
                    IconButton.scaleTooltipCoordinate(mouseX), IconButton.scaleTooltipCoordinate(mouseY));
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
        int headingY = PANEL_TOP + 14;
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.details").withStyle(ChatFormatting.BOLD), x, headingY, TEXT_HEADER);

        int iconSize = Math.min(42, width);
        int iconY = PANEL_TOP + 30;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, RemoteIconManager.icon(pack)),
                x, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int textX = x + iconSize + 6;
        int textWidth = Math.max(1, width - iconSize - 6);
        int titleColor = isRemoteDeletePending(pack) ? TEXT_PENDING_DELETION : TEXT_TITLE;
        drawTrackMarquee(graphics, pack.name(), textX, iconY + 1, textWidth, titleColor);
        drawTrackMarquee(graphics, Component.literal(pack.id().getNamespace() + ":"), textX, iconY + 13, textWidth, TEXT_DESCRIPTION);
        drawTrackMarquee(graphics, Component.literal(pack.id().getPath()), textX, iconY + 25, textWidth, TEXT_DESCRIPTION);

        int fieldY = iconY + iconSize + 5;
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.repository", Component.literal(pack.repository()), x, fieldY, width);
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.version", Component.literal(pack.version()), x, fieldY + 26, width);
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.state", remoteStateMessage(RemoteContentManager.state(pack)), x, fieldY + 52, width);
        List<String> missing = RemoteContentManager.missingDependencies(pack);
        Component dependencies = pack.dependencies().isEmpty() ? Component.literal("-")
                : Component.literal(String.join(", ", pack.dependencies()));
        if (!missing.isEmpty()) dependencies = dependencies.copy().append(Component.literal(" Ã‚Â· ")).append(Component.translatable(
                "screen.music_and_melody.remote_details.missing_dependencies", String.join(", ", missing)));
        renderRemoteDetailField(graphics, "screen.music_and_melody.remote_details.dependencies", dependencies, x, fieldY + 78, width);

        int descriptionY = fieldY + 104;
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme.description").withStyle(ChatFormatting.UNDERLINE),
                x, descriptionY, TEXT_DESCRIPTION);
        descriptionY += 12;
        int descriptionBottom = this.panelBottom - 62;
        for (FormattedCharSequence line : this.font.split(pack.description(), Math.max(1, width))) {
            if (descriptionY + this.font.lineHeight > descriptionBottom) break;
            ThemeHelper.text(graphics, this.font, line, x, descriptionY, TEXT_PRIMARY);
            descriptionY += this.font.lineHeight + 2;
        }
        renderRemoteDownloadProgress(graphics, pack);
    }

    private RemotePack viewedContentRemotePack() {
        if (this.viewedContent == null) return null;
        RemotePack.Tag tag = this.viewedContent.playlist() == null ? RemotePack.Tag.ALBUM : RemotePack.Tag.PLAYLIST;
        return RemoteContentManager.owner(this.viewedContent.id(), tag).orElse(null);
    }

    private boolean viewedContentDeleteable() {
        if (this.viewedContent == null) return false;
        return this.viewedContent.playlist() != null && this.viewedContent.playlist().isCustom()
                || this.viewedContent.album() != null && CustomAlbums.isConfigAlbum(this.viewedContent.album());
    }

    private boolean viewedContentDeletePending() {
        if (this.viewedContent == null) return false;
        if (this.viewedContent.playlist() != null && this.viewedContent.playlist().isCustom()) {
            return this.pendingPlaylistDeletes.contains(this.viewedContent.id());
        }
        if (this.viewedContent.album() != null && CustomAlbums.isConfigAlbum(this.viewedContent.album())) {
            return this.pendingConfigAlbumDeletes.contains(this.viewedContent.id());
        }
        RemotePack pack = viewedContentRemotePack();
        return pack != null && this.pendingRemoteDeletes.contains(pack.key());
    }

    private Component contentDeleteMessage() {
        return Component.translatable(viewedContentDeletePending()
                ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
    }

    private Identifier contentDeleteIcon() {
        return IconButton.icon(viewedContentDeletePending() ? "restore" : "delete");
    }

    private void toggleViewedContentDelete() {
        if (!viewedContentDeleteable()) return;
        if (this.viewedContent.playlist() != null && this.viewedContent.playlist().isCustom()) {
            if (!this.pendingPlaylistDeletes.remove(this.viewedContent.id())) this.pendingPlaylistDeletes.add(this.viewedContent.id());
        } else if (this.viewedContent.album() != null && CustomAlbums.isConfigAlbum(this.viewedContent.album())) {
            if (!this.pendingConfigAlbumDeletes.remove(this.viewedContent.id())) this.pendingConfigAlbumDeletes.add(this.viewedContent.id());
        } else {
            RemotePack pack = viewedContentRemotePack();
            if (pack == null) return;
            if (!this.pendingRemoteDeletes.remove(pack.key())) this.pendingRemoteDeletes.add(pack.key());
        }
        this.rebuildWidgets();
    }

    private void renderThemeDetails(GuiGraphicsExtractor graphics, Theme theme) {
        int x = this.rightX + 8;
        int width = this.rightWidth - 16;
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.details").withStyle(ChatFormatting.BOLD),
                x, PANEL_TOP + 14, TEXT_HEADER);
        int iconSize = Math.min(42, width);
        int iconY = PANEL_TOP + 30;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, theme.icon), x, iconY,
                0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        int textX = x + iconSize + 6;
        int textWidth = Math.max(1, width - iconSize - 6);
        int titleColor = isThemeDeletePending(theme.theme) ? TEXT_PENDING_DELETION
                : isActiveTheme(theme) ? TEXT_SELECTED : TEXT_TITLE;
        drawTrackMarquee(graphics, theme.name, textX, iconY + 1, textWidth, titleColor);
        drawTrackMarquee(graphics, Component.literal(theme.theme.getNamespace() + ":"), textX, iconY + 13, textWidth, TEXT_DESCRIPTION);
        drawTrackMarquee(graphics, Component.literal(theme.theme.getPath()), textX, iconY + 25, textWidth, TEXT_DESCRIPTION);
        int descriptionY = iconY + iconSize + 6;
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme.description").withStyle(ChatFormatting.UNDERLINE),
                x, descriptionY, TEXT_DESCRIPTION);
        descriptionY += 12;
        List<FormattedCharSequence> lines = this.font.split(theme.description, Math.max(1, width));
        int bottom = themePreviewY() - 6;
        for (FormattedCharSequence line : lines) {
            if (descriptionY + this.font.lineHeight > bottom) break;
            ThemeHelper.text(graphics, this.font, line, x, descriptionY, TEXT_PRIMARY);
            descriptionY += this.font.lineHeight + 2;
        }
        if (!theme.valid) {
            if (descriptionY + this.font.lineHeight <= bottom) {
                ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.theme.invalid").withStyle(ChatFormatting.BOLD),
                        x, descriptionY, TEXT_PENDING_DELETION);
            }
        }
    }

    private int themePreviewY() {
        return this.panelBottom - 100;
    }

    private void renderRemoteDetailField(GuiGraphicsExtractor graphics, String headingKey, Component value, int x, int y, int width) {
        ThemeHelper.text(graphics, this.font, Component.translatable(headingKey).withStyle(ChatFormatting.UNDERLINE), x, y, TEXT_DESCRIPTION);
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

    void renderPlaybackStrip(GuiGraphicsExtractor graphics, int middleX, int middleWidth, int bottomPanelTop) {
        int progressX = middleX + 34;
        int progressRight = middleX + middleWidth - 42;
        int progressY = bottomPanelTop + 9;
        int progressWidth = Math.max(1, progressRight - progressX);
        graphics.fill(progressX, progressY, progressRight, progressY + 3, BAR_BACKGROUND);

        long elapsed = this.draggingProgress ? this.seekPreviewMillis : PlaylistHelper.currentSongElapsedMillis();
        Optional<Long> duration = MusicDurationHelper.currentDurationMillis(this.minecraft, PlaylistHelper.getCurrentSong());
        if (duration.isPresent() && duration.get() > 0L) {
            float progress = Math.min(1.0F, elapsed / (float) duration.get());
            int handleX = progressX + Math.round(progressWidth * progress);
            graphics.fill(progressX, progressY, handleX, progressY + 3, PANEL_HIGHLIGHT);
            graphics.fill(handleX - 1, progressY - 2, handleX + 2, progressY + 5, TEXT_TITLE);
            ThemeHelper.text(graphics, this.font, Component.literal(formatDuration(Math.max(0L, duration.get() - elapsed))), progressRight + 6, bottomPanelTop + 7, TEXT_DESCRIPTION);
        } else {
            ThemeHelper.text(graphics, this.font, Component.literal("--:--"), progressRight + 6, bottomPanelTop + 7, TEXT_DESCRIPTION);
        }
        if (!this.searching && PlaylistHelper.getCurrentSongId() != null) {
            Component track = MusicScreenHelper.playlistName(this.minecraft, PlaylistHelper.getCurrentSongId());
            drawMarquee(graphics, track, progressX, bottomPanelTop + 17, progressWidth, TEXT_DESCRIPTION);
        }
    }

    private void renderPlaybackStrip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderPlaybackStrip(graphics, this.middleX, this.middleWidth, this.bottomPanelTop);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        updateDynamicControls();
        IconButton.setTooltipScale(MaMDataConfig.get().gui_multiplier);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(MaMDataConfig.get().gui_multiplier);
            super.extractRenderState(graphics, toLayoutMouse(mouseX), toLayoutMouse(mouseY), tickDelta);
        } finally {
            graphics.pose().popMatrix();
            IconButton.resetTooltipScale();
        }
    }

    private int toLayoutMouse(double mouse) {
        // fixes mouse positioning detection with gui multiplier
        return (int) Math.floor(mouse / MaMDataConfig.get().gui_multiplier);
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
        if (this.saveButton != null) this.saveButton.active = PlaylistHelper.hasCustomPlaylistSongs();
        if (this.replaceButton != null) this.replaceButton.active = PlaylistHelper.hasCustomPlaylistSongs()
                && Playlist.PLAYLISTS.stream().anyMatch(Playlist::isCustom);
        if (this.clearButton != null) this.clearButton.active = PlaylistHelper.hasCustomPlaylistSongs();
        if (this.loadButton != null || this.queueButton != null) {
            List<SafeIdentifier> viewedSongs = this.viewedContent == null ? List.of() : this.viewedContent.queueSongs(this.minecraft);
            boolean hasTracks = !viewedSongs.isEmpty();
            if (this.loadButton != null) this.loadButton.active = hasTracks;
            if (this.queueButton != null) {
                this.queueButton.active = hasTracks && viewedSongs.stream().anyMatch(song -> !PlaylistHelper.isInCustomPlaylist(song));
            }
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
        MaMDataConfig config = MaMDataConfig.get();
        config.vanilla_music = !config.vanilla_music;
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        if (!config.vanilla_music) this.minecraft.getMusicManager().stopPlaying();
        this.rebuildWidgets();
    }

    private void toggleEventMusic() {
        MaMDataConfig config = MaMDataConfig.get();
        config.event_music = !config.event_music;
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        if (!config.event_music) EventHelper.stopDisabledEventMusic();
        this.rebuildWidgets();
    }

    private void openSavePlaylistScreen() {
        closeSearch();
        this.minecraft.gui.setScreen(new SavePlaylistScreen(this));
    }

    private void openReplacePlaylistScreen() {
        closeSearch();
        this.minecraft.gui.setScreen(new ReplacePlaylistScreen(this));
    }

    private void openConfigAlbumEditor(Album album) {
        closeSearch();
        this.minecraft.gui.setScreen(album == null ? new AlbumEditorScreen(this) : new AlbumEditorScreen(this, album.album));
    }

    void configAlbumsChanged() {
        closeSearch();
        this.viewedContent = null;
        this.page = Page.LIBRARY;
        this.reloadPending = true;
        this.minecraft.reloadResourcePacks().thenRun(() -> this.minecraft.execute(this::rebuildWidgets));
    }

    private void openCreateEventScreen() {
        if (!MaMClientConfig.get().allow_events) return;
        closeSearch();
        this.minecraft.gui.setScreen(new CreateEventScreen(this));
    }

    private void openCreateThemeScreen() {
        closeSearch();
        this.minecraft.gui.setScreen(new CreateThemeScreen(this));
    }

    private void toggleSearch() {
        if (!supportsSearch()) {
            closeSearch();
            return;
        }
        if (this.searching) {
            closeSearch();
        } else {
            this.searching = true;
            // allows immediate typing on clicking the search icon
            this.focusSearchField = true;
        }
        updateSearchVisibility();
    }

    private void updateSearchVisibility() {
        boolean supported = supportsSearch();
        if (!supported) {
            this.searching = false;
            this.search = "";
            this.focusSearchField = false;
        }
        if (this.searchButton != null) this.searchButton.active = supported;
        if (this.searchField != null) {
            this.searchField.visible = supported && this.searching;
            this.searchField.active = supported && this.searching;
        }
    }

    private boolean supportsSearch() {
        return switch (this.page) {
            case LIBRARY, DETAILS, EVENTS, THEMES, ONLINE -> true;
            case NOW_PLAYING, HOME, CONFIG -> false;
        };
    }

    private void closeSearch() {
        boolean wasSearching = this.searching;
        boolean hadText = !this.search.isEmpty();
        this.searching = false;
        this.search = "";
        this.focusSearchField = false;
        this.setFocused(null);
        if (this.searchField != null) {
            this.searchField.setFocused(false);
            if (!this.searchField.getValue().isEmpty()) this.searchField.setValue("");
        }
        if (wasSearching && !hadText && this.searchField != null && this.searchField.getValue().isEmpty()) {
            refreshPageList();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        event = toLayoutMouse(event);
        if (isWelcomePage() && this.welcomeScrollMax > 0
                && event.x() >= this.rightX + this.rightWidth - 8 && event.x() <= this.rightX + this.rightWidth) {
            int thumbY = welcomeScrollbarY();
            int thumbHeight = welcomeScrollbarHeight();
            if (event.y() >= this.welcomeViewportTop && event.y() < this.welcomeViewportBottom) {
                this.draggingWelcomeScrollbar = true;
                this.welcomeScrollbarDragOffset = event.y() >= thumbY && event.y() < thumbY + thumbHeight
                        ? event.y() - thumbY : thumbHeight / 2.0D;
                setWelcomeScrollFromThumb(event.y() - this.welcomeScrollbarDragOffset);
                return true;
            }
        }
        // Prioritise search bar over the music bar beneath it
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
        if (isWelcomePage()) for (BreadcrumbHit hit : this.welcomeLinks) {
            if (hit.contains(event.x(), event.y())) {
                AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
                hit.action.run();
                return true;
            }
        }
        if (handlePlaybackClick(event.x(), event.y(), this.middleX, this.middleWidth, this.bottomPanelTop)) return true;
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
        if (this.draggingWelcomeScrollbar) {
            setWelcomeScrollFromThumb(event.y() - this.welcomeScrollbarDragOffset);
            return true;
        }
        if (handlePlaybackDrag(event.x(), event.y(), this.middleX, this.middleWidth)) return true;
        if (this.draggingVolume) {
            setVolumeFromY(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        event = toLayoutMouse(event);
        if (this.draggingWelcomeScrollbar) {
            this.draggingWelcomeScrollbar = false;
            return true;
        }
        if (handlePlaybackRelease()) return true;
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
                PlaylistHelper.moveCustomPlaylistSong(from, to);
                refreshQueueLists();
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
        double x = mouseX / MaMDataConfig.get().gui_multiplier;
        double y = mouseY / MaMDataConfig.get().gui_multiplier;
        if (isWelcomePage() && this.welcomeScrollMax > 0
                && x >= this.rightX && x < this.rightX + this.rightWidth
                && y >= this.welcomeViewportTop && y < this.welcomeViewportBottom) {
            double next = Math.max(0, Math.min(this.welcomeScrollMax, this.welcomeScroll - scrollY * 24));
            if (next != this.welcomeScroll) {
                this.welcomeScroll = next;
                this.rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean isInVolumeSlider(double mouseX, double mouseY) {
        int x = volumeSliderX();
        int top = volumeSliderTop();
        int bottom = volumeSliderBottom();
        return mouseX >= x - 10 && mouseX <= x + 14 && mouseY >= top && mouseY <= bottom;
    }

    boolean handlePlaybackClick(double mouseX, double mouseY, int stripX, int stripWidth, int stripTop) {
        if (!isInProgressBar(mouseX, mouseY, stripX, stripWidth, stripTop)
                || currentTrackDuration().isEmpty()) return false;
        this.draggingProgress = true;
        setSeekPreviewFromX(mouseX, stripX, stripWidth);
        return true;
    }

    boolean handlePlaybackDrag(double mouseX, double mouseY, int stripX, int stripWidth) {
        if (!this.draggingProgress) return false;
        setSeekPreviewFromX(mouseX, stripX, stripWidth);
        return true;
    }

    boolean handlePlaybackRelease() {
        if (!this.draggingProgress) return false;
        this.draggingProgress = false;
        PlaylistHelper.seekCurrentSong(this.seekPreviewMillis);
        return true;
    }

    private boolean isInProgressBar(double mouseX, double mouseY, int stripX, int stripWidth, int stripTop) {
        int left = stripX + 30;
        int right = stripX + stripWidth - 38;
        int top = stripTop + 4;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= top + 12;
    }

    private Optional<Long> currentTrackDuration() {
        return MusicDurationHelper.currentDurationMillis(this.minecraft, PlaylistHelper.getCurrentSong())
                .filter(value -> value > 0L);
    }

    private void setSeekPreviewFromX(double mouseX, int stripX, int stripWidth) {
        Optional<Long> duration = currentTrackDuration();
        if (duration.isEmpty()) return;
        int progressX = stripX + 34;
        int progressRight = stripX + stripWidth - 42;
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
        return Math.max(48, Math.min(112, Math.min(widthLimit, heightLimit)));
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
        List<SafeIdentifier> songs = PlaylistHelper.customPlaylistSongs();
        if (index < 0 || index >= songs.size() || !PlaylistHelper.loadCustomQueue(songs)) return;
        PlaylistHelper.playNow(index);
        refreshQueueLists();
    }

    private void requestClearQueue() {
        if (!PlaylistHelper.hasCustomPlaylistSongs()) return;
        requestDiscardCustomPlaylist(Component.translatable("button.music_and_melody.clear"), () -> {
            PlaylistHelper.clearCustomPlaylist();
            refreshAfterQueueMutation();
        });
    }

    private boolean hasUnsavedCustomPlaylist() {
        return PlaylistHelper.hasCustomPlaylistSongs();
    }

    private void requestDiscardCustomPlaylist(Component confirmLabel, Runnable action) {
        if (!hasUnsavedCustomPlaylist()) {
            action.run();
            return;
        }
        this.minecraft.gui.setScreen(new PlaylistConfirmScreen(
                this,
                Component.translatable("screen.music_and_melody.discard_custom_playlist"),
                Component.translatable("screen.music_and_melody.discard_custom_playlist.warning"),
                confirmLabel,
                action
        ));
    }

    void requestRemoveQueueTrack(int index) {
        PlaylistHelper.removeCustomPlaylistSong(index);
        refreshAfterQueueMutation();
    }

    void addTrackToCustomPlaylist(SafeIdentifier song) {
        PlaylistHelper.addToCustomPlaylist(song);
        refreshAfterQueueMutation();
    }

    private void refreshAfterQueueMutation() {
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
        if (album == null || CustomAlbums.isConfigAlbum(album)) return;
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
        if (this.themeList != null) this.themeList.refresh();
    }

    private void previewTheme(Theme theme) {
        if (theme == null || !theme.valid) return;
        if (this.previewingTheme && this.viewedTheme != null && this.viewedTheme.theme.equals(theme.theme)) {
            ThemeListener.restoreActive();
            this.previewingTheme = false;
            this.rebuildWidgets();
            return;
        }
        ThemeListener.preview(theme.theme);
        this.selectedThemeId = theme.theme;
        this.previewingTheme = true;
        this.rebuildWidgets();
    }

    private void applyTheme(Theme theme) {
        if (theme == null || !theme.valid || !ThemeListener.apply(theme.theme)) return;
        this.selectedThemeId = theme.theme;
        this.previewingTheme = false;
        this.rebuildWidgets();
    }

    private void openThemeEditor(Theme theme) {
        if (theme == null) return;
        closeSearch();
        this.minecraft.gui.setScreen(new ThemeEditorScreen(this, theme));
    }

    void themeChanged(Identifier id) {
        this.selectedThemeId = id;
        this.viewedTheme = ThemeListener.theme(id);
        this.previewingTheme = false;
        ThemeListener.restoreActive();
        this.rebuildWidgets();
    }

    private void finishThemePreview() {
        if (this.previewingTheme) ThemeListener.restoreActive();
        this.previewingTheme = false;
        this.viewedTheme = null;
        this.rebuildWidgets();
    }

    void openContent(ContentItem item) {
        this.viewedContent = item;
        this.page = Page.DETAILS;
        closeSearch();
        this.rebuildWidgets();
    }

    private void loadViewedContent() {
        if (this.viewedContent == null) return;
        List<SafeIdentifier> songs = this.viewedContent.queueSongs(this.minecraft);
        if (songs.isEmpty()) return;

        if (hasUnsavedCustomPlaylist()) {
            requestDiscardCustomPlaylist(Component.translatable("button.music_and_melody.load"),
                    () -> loadViewedContentNow(songs));
        } else {
            loadViewedContentNow(songs);
        }
    }

    private void loadViewedContentNow(List<SafeIdentifier> songs) {
        if (this.viewedContent == null || songs.isEmpty()) return;
        if (!PlaylistHelper.replaceCustomPlaylist(songs)) return;
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        closeSearch();
        this.rebuildWidgets();
    }

    private void openCustomPlaylist() {
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        closeSearch();
        this.rebuildWidgets();
    }

    private void queueViewedContent() {
        if (this.viewedContent == null) return;
        List<SafeIdentifier> songs = this.viewedContent.queueSongs(this.minecraft);
        if (songs.isEmpty()) return;
        if (songs.stream().noneMatch(song -> !PlaylistHelper.isInCustomPlaylist(song))) return;
        PlaylistHelper.addAllToCustomPlaylist(songs);
        refreshAfterQueueMutation();
    }

    void playContentTrack(int index) {
        if (this.viewedContent == null) return;
        List<SafeIdentifier> songs = this.viewedContent.queueSongs(this.minecraft);
        if (index < 0 || index >= songs.size() || !MusicDiscHelper.isSoundUnlocked(this.minecraft, songs.get(index))) return;

        if (!isViewedContentQueueType()) {
            loadAndPlayViewedContentTrack(songs, index);
            return;
        }
        finishPlayingViewedContentTrack(index);
    }

    private void loadAndPlayViewedContentTrack(List<SafeIdentifier> songs, int index) {
        if (this.viewedContent == null || !PlaylistHelper.loadQueueType(songs, this.viewedContent.type(), this.viewedContent.id().toString(), this.viewedContent.name().getString())) return;
        finishPlayingViewedContentTrack(index);
    }

    private void finishPlayingViewedContentTrack(int index) {
        if (!PlaylistHelper.playNow(index)) return;
        this.page = Page.DETAILS;
        closeSearch();
        this.rebuildWidgets();
    }

    private boolean isViewedContentQueueType() {
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
        closeSearch();
        this.selectedEventNamespace = namespace;
        this.rebuildWidgets();
    }

    void openEvent(Event.Source source) {
        if (!MaMClientConfig.get().allow_events) return;
        closeSearch();
        this.minecraft.gui.setScreen(new EventScreen(this, source.id));
    }

    void chooseOnlineCatalog(String catalog) {
        closeSearch();
        this.selectedOnlineCatalog = catalog;
        this.viewedRemotePack = null;
        this.rebuildWidgets();
    }

    void viewRemotePack(RemotePack pack) {
        closeSearch();
        this.viewedRemotePack = pack;
        this.rebuildWidgets();
    }

    private void renderWelcomePanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        updateFeaturedCredits();
        this.welcomeLinks.clear();
        int x = this.rightX + 8;
        int width = Math.max(1, this.rightWidth - 16);
        ThemeHelper.text(graphics, this.font, Component.translatable("screen.music_and_melody.welcome").withStyle(ChatFormatting.BOLD), x, PANEL_TOP + 14, TEXT_HEADER);

        int y = this.welcomeViewportTop - (int) Math.round(this.welcomeScroll);
        graphics.enableScissor(this.rightX + 2, this.welcomeViewportTop, this.rightX + this.rightWidth - 2, this.welcomeViewportBottom);
        this.renderingWelcomeScrollableContent = true;
        for (FormattedCharSequence line : this.font.split(Component.translatable("screen.music_and_melody.message"), width)) {
            ThemeHelper.centeredText(graphics, this.font, line, this.rightX + this.rightWidth / 2, y, TEXT_DESCRIPTION);
            y += this.font.lineHeight + 2;
        }
        y += 5;
        y += IconButton.SIZE + 4;
        renderWelcomeLink(graphics, Component.translatable("button.music_and_melody.report_issues").getString(), x, y, width,
                mouseX, mouseY, () -> MusicScreenHelper.openUri("https://github.com/Rebel459/music-and-melody/issues"));
        if (RemoteContentManager.onlineFunctionalityEnabled()) {
            y += 28;
            ThemeHelper.centeredText(graphics, this.font, Component.translatable("screen.music_and_melody.supporter_thanks"), this.rightX + this.rightWidth / 2, y, TEXT_DESCRIPTION);
            String supporter = this.featuredSupporter == null && RemoteContentManager.creditsLoading()
                    ? Component.translatable("screen.music_and_melody.loading").getString() : this.featuredSupporter;
            renderWelcomeLink(graphics, supporter, x, y + 14, width, mouseX, mouseY,
                    this.featuredSupporter == null ? null : MusicScreenHelper::openKofi);
            y += 42;
            ThemeHelper.centeredText(graphics, this.font, Component.translatable("screen.music_and_melody.composer_credits"), this.rightX + this.rightWidth / 2, y, TEXT_DESCRIPTION);
            String composer = this.featuredComposer == null && RemoteContentManager.creditsLoading()
                    ? Component.translatable("screen.music_and_melody.loading").getString() : this.featuredComposer;
            renderWelcomeLink(graphics, composer, x, y + 14, width, mouseX, mouseY,
                    this.featuredComposerUrl == null ? null : () -> MusicScreenHelper.openUri(this.featuredComposerUrl));
        }
        this.renderingWelcomeScrollableContent = false;
        graphics.disableScissor();

        renderWelcomeLink(graphics, Component.translatable("screen.music_and_melody.mod_credits").getString(), x,
                this.panelBottom - 18, width, mouseX, mouseY,
                () -> MusicScreenHelper.openUri("https://modrinth.com/user/Rebel459"));
        renderWelcomeScrollbar(graphics, mouseX, mouseY);
    }

    private void renderWelcomeLink(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int mouseX, int mouseY, Runnable action) {
        if (text == null || text.isBlank()) return;
        boolean insideViewport = !this.renderingWelcomeScrollableContent
                || y >= this.welcomeViewportTop && y + this.font.lineHeight <= this.welcomeViewportBottom;
        boolean hovered = action != null && insideViewport
                && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + this.font.lineHeight;
        Component label = Component.literal(text);
        if (hovered) label = label.copy().withStyle(ChatFormatting.UNDERLINE);
        drawCenteredTrackMarquee(graphics, label, x, y, width, TEXT_PRIMARY);
        if (action != null && insideViewport) this.welcomeLinks.add(new BreadcrumbHit(x, y, width, this.font.lineHeight, action));
    }

    private void drawCenteredTrackMarquee(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        if (this.font.width(text) <= width) {
            ThemeHelper.centeredText(graphics, this.font, text, x + width / 2, y, color);
        } else {
            drawTrackMarquee(graphics, text, x, y, width, color);
        }
    }

    private void updateFeaturedCredits() {
        if (this.featuredSplash == null && !RemoteContentManager.splashes().isEmpty()) {
            this.featuredSplash = RemoteContentManager.splashes().get((int) (Util.getMillis() % RemoteContentManager.splashes().size()));
        }
        if (this.featuredSupporter == null && !RemoteContentManager.displaySupporters().isEmpty()) {
            List<String> displaySupporters = RemoteContentManager.displaySupporters();
            String entry = displaySupporters.get((int) (Util.getMillis() % displaySupporters.size()));
            int separator = entry.indexOf('=');
            this.featuredSupporter = (separator < 0 ? entry : entry.substring(0, separator)).trim();
        }
        if (this.featuredComposer == null && !RemoteContentManager.composers().isEmpty()) {
            String entry = RemoteContentManager.composers().get((int) (Util.getMillis() % RemoteContentManager.composers().size()));
            int separator = entry.indexOf('=');
            this.featuredComposer = (separator < 0 ? entry : entry.substring(0, separator)).trim();
            this.featuredComposerUrl = separator < 0 ? null : entry.substring(separator + 1).trim();
            if (this.featuredComposerUrl != null && this.featuredComposerUrl.isBlank()) this.featuredComposerUrl = null;
        }
    }

    private void loadCachedWelcomeValues() {
        MaMDataConfig.Cache cache = MaMDataConfig.get().cache;
        if (this.featuredSplash == null && cache.splash != null && !cache.splash.isBlank()) this.featuredSplash = cache.splash;
        if (this.featuredSupporter == null && cache.supporter != null && !cache.supporter.isBlank()) {
            this.featuredSupporter = displayName(cache.supporter);
        }
        if (this.featuredComposer == null && cache.composer != null && !cache.composer.isBlank()) {
            int separator = cache.composer.indexOf('=');
            this.featuredComposer = displayName(cache.composer);
            this.featuredComposerUrl = separator < 0 ? null : cache.composer.substring(separator + 1).trim();
            if (this.featuredComposerUrl != null && this.featuredComposerUrl.isBlank()) this.featuredComposerUrl = null;
        }
    }

    private static String displayName(String entry) {
        int separator = entry.indexOf('=');
        return (separator < 0 ? entry : entry.substring(0, separator)).trim();
    }

    private void buildWelcomePanel() {
        int width = Math.max(1, this.rightWidth - 16);
        int messageLines = this.font.split(Component.translatable("screen.music_and_melody.message"), width).size();
        boolean online = RemoteContentManager.onlineFunctionalityEnabled();
        int buttonX = this.rightX + 7;
        int buttonWidth = this.rightWidth - 14;
        int newsY = PANEL_TOP + 30;
        this.welcomeViewportTop = newsY + 26;
        this.welcomeViewportBottom = this.panelBottom - 30;
        this.welcomeScrollMax = Math.max(0, welcomeContentHeight(messageLines, online) - Math.max(1, this.welcomeViewportBottom - this.welcomeViewportTop));
        this.welcomeScroll = Math.max(0, Math.min(this.welcomeScroll, this.welcomeScrollMax));
        WorkspaceButton news = this.addRenderableWidget(new WorkspaceButton(buttonX, newsY, buttonWidth, 20,
                Component.translatable("button.music_and_melody.news"), false,
                ignored -> this.minecraft.gui.setScreen(new NewsScreen(this))));
        news.active = online;
        this.welcomeSocialY = this.welcomeViewportTop - (int) Math.round(this.welcomeScroll)
                + messageLines * (this.font.lineHeight + 2) + 5;
        if (this.welcomeSocialY >= this.welcomeViewportTop
                && this.welcomeSocialY + IconButton.SIZE <= this.welcomeViewportBottom) {
            MusicScreenHelper.addCenteredSocialButtons(this, this.rightX + this.rightWidth / 2, this.welcomeSocialY);
        }
    }

    private int welcomeContentHeight(int messageLines, boolean online) {
        return messageLines * (this.font.lineHeight + 2) + 5
                + IconButton.SIZE + 4 + 28 + (online ? 42 + 42 : 0);
    }

    private boolean isWelcomePage() {
        return this.page == Page.HOME || this.page == Page.CONFIG;
    }

    private void renderWelcomeScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.welcomeScrollMax <= 0) return;
        int y = welcomeScrollbarY();
        int thumb = welcomeScrollbarHeight();
        graphics.fill(this.rightX + this.rightWidth - 5, y, this.rightX + this.rightWidth - 2, y + thumb,
                mouseX >= this.rightX + this.rightWidth - 7 ? PANEL_HIGHLIGHT : POPUP_OUTLINE);
    }

    private int welcomeScrollbarHeight() {
        int viewport = Math.max(1, this.welcomeViewportBottom - this.welcomeViewportTop);
        return Math.max(16, (int) Math.round(viewport * viewport / (viewport + this.welcomeScrollMax)));
    }

    private int welcomeScrollbarY() {
        int viewport = Math.max(1, this.welcomeViewportBottom - this.welcomeViewportTop);
        int travel = Math.max(0, viewport - welcomeScrollbarHeight());
        return this.welcomeViewportTop + (int) Math.round(travel * this.welcomeScroll / Math.max(1.0D, this.welcomeScrollMax));
    }

    private void setWelcomeScrollFromThumb(double thumbTop) {
        int travel = Math.max(1, this.welcomeViewportBottom - this.welcomeViewportTop - welcomeScrollbarHeight());
        double fraction = (thumbTop - this.welcomeViewportTop) / travel;
        this.welcomeScroll = Math.max(0, Math.min(this.welcomeScrollMax, fraction * this.welcomeScrollMax));
        this.rebuildWidgets();
    }

    void manageRemoteContent(Identifier contentId, RemotePack.Tag tag) {
        RemoteContentManager.owner(contentId, tag).ifPresent(pack -> {
            this.viewedRemotePack = pack;
            this.minecraft.gui.setScreen(this);
            this.rebuildWidgets();
        });
    }

    void openRepositoryEditor() {
        closeSearch();
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
            if (!RemoteContentManager.openManualDownloadScreen(this, pack)) {
                RemoteContentManager.download(pack);
            }
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
        return this.pendingRemoteDeletes.contains(pack.key());
    }

    void toggleRemoteDeletePending(RemotePack pack) {
        if (!remoteDeleteAvailable(pack)) return;
        if (!this.pendingRemoteDeletes.remove(pack.key())) this.pendingRemoteDeletes.add(pack.key());
        if (this.onlinePackList != null) this.onlinePackList.refresh();
        this.rebuildWidgets();
    }

    private Component remoteDeleteMessage(RemotePack pack) {
        return Component.translatable(isRemoteDeletePending(pack) ? "button.music_and_melody.restore" : "button.music_and_melody.delete");
    }

    private void closeRemoteDetails() {
        closeSearch();
        this.viewedRemotePack = null;
        this.rebuildWidgets();
    }

    private void applyPendingRemoteDeletes() {
        if (this.pendingRemoteDeletes.isEmpty()) return;
        boolean changed = false;
        for (RemotePack.Key key : List.copyOf(this.pendingRemoteDeletes)) {
            changed |= RemoteContentManager.deleteInstalled(key);
        }
        this.pendingRemoteDeletes.clear();
        if (!changed) return;
        this.reloadPending = true;
        RemoteContentManager.refresh();
    }

    private boolean isContentDeletePending(ContentItem item) {
        if (item == null) return false;
        if (item.playlist() != null && item.playlist().isCustom()) return this.pendingPlaylistDeletes.contains(item.id());
        if (item.album() != null && CustomAlbums.isConfigAlbum(item.album())) return this.pendingConfigAlbumDeletes.contains(item.id());
        RemotePack.Tag tag = item.playlist() == null ? RemotePack.Tag.ALBUM : RemotePack.Tag.PLAYLIST;
        RemotePack pack = RemoteContentManager.owner(item.id(), tag).orElse(null);
        return pack != null && this.pendingRemoteDeletes.contains(pack.key());
    }

    private void applyPendingPlaylistDeletes() {
        if (this.pendingPlaylistDeletes.isEmpty()) return;
        for (Playlist playlist : List.copyOf(Playlist.PLAYLISTS)) {
            if (this.pendingPlaylistDeletes.contains(playlist.playlist)) playlist.deleteCustom();
        }
        this.pendingPlaylistDeletes.clear();
    }

    private void applyPendingConfigAlbumDeletes() {
        if (this.pendingConfigAlbumDeletes.isEmpty()) return;
        for (Identifier id : List.copyOf(this.pendingConfigAlbumDeletes)) CustomAlbums.delete(id);
        this.pendingConfigAlbumDeletes.clear();
        this.reloadPending = true;
    }

    boolean isEventDeletePending(Identifier id) {
        if (id == null) return false;
        if (this.pendingEventDeletes.contains(id)) return true;
        return RemoteContentManager.owner(id, RemotePack.Tag.EVENT)
                .map(pack -> this.pendingRemoteDeletes.contains(pack.key())).orElse(false);
    }

    boolean toggleEventDeletePending(Identifier id) {
        if (id == null) return false;
        if (!this.pendingEventDeletes.remove(id)) this.pendingEventDeletes.add(id);
        refreshPageList();
        return this.pendingEventDeletes.contains(id);
    }

    boolean isThemeDeletePending(Identifier id) {
        if (id == null) return false;
        if (this.pendingThemeDeletes.contains(id)) return true;
        return RemoteContentManager.owner(id, RemotePack.Tag.THEME)
                .map(pack -> this.pendingRemoteDeletes.contains(pack.key())).orElse(false);
    }

    boolean toggleThemeDeletePending(Identifier id) {
        if (id == null) return false;
        if (!this.pendingThemeDeletes.remove(id)) this.pendingThemeDeletes.add(id);
        refreshPageList();
        return this.pendingThemeDeletes.contains(id);
    }

    private void applyPendingEventDeletes() {
        if (this.pendingEventDeletes.isEmpty()) return;
        for (Event.Source source : List.copyOf(Event.sources())) {
            if (this.pendingEventDeletes.contains(source.id)) source.deleteConfig();
        }
        this.pendingEventDeletes.clear();
        this.reloadPending = true;
    }

    private void applyPendingThemeDeletes() {
        if (this.pendingThemeDeletes.isEmpty()) return;
        for (Identifier id : List.copyOf(this.pendingThemeDeletes)) ThemeListener.deleteConfigTheme(id);
        this.pendingThemeDeletes.clear();
        this.reloadPending = true;
    }

    private void applyAllPendingDeletes() {
        applyPendingPlaylistDeletes();
        applyPendingConfigAlbumDeletes();
        applyPendingRemoteDeletes();
        applyPendingEventDeletes();
        applyPendingThemeDeletes();
    }

    private void setPage(Page page) {
        if (page == Page.EVENTS && !MaMClientConfig.get().allow_events) page = Page.HOME;
        if (this.page == Page.THEMES && page != Page.THEMES && this.previewingTheme) {
            ThemeListener.restoreActive();
            this.previewingTheme = false;
        }
        closeSearch();
        this.page = page;
        if (page != Page.DETAILS) lastOpenedPage = page;
        this.selectedEventNamespace = null;
        this.selectedOnlineCatalog = null;
        this.viewedRemotePack = null;
        this.viewedTheme = null;
        this.rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.page == Page.EVENTS && !MaMClientConfig.get().allow_events) setPage(Page.HOME);
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
                    closeSearch();
                    this.selectedEventNamespace = null;
                    this.rebuildWidgets();
                } else setPage(Page.HOME);
            }
            case ONLINE -> {
                if (this.selectedOnlineCatalog != null) {
                    closeSearch();
                    this.selectedOnlineCatalog = null;
                    this.viewedRemotePack = null;
                    this.rebuildWidgets();
                } else setPage(Page.HOME);
            }
            case THEMES -> {
                if (this.viewedTheme != null) finishThemePreview();
                else setPage(Page.HOME);
            }
            case LIBRARY, CONFIG -> setPage(Page.HOME);
            case HOME -> this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void onClose() {
        if (this.page == Page.HOME) {
            applyAllPendingDeletes();
            this.minecraft.gui.setScreen(this.parent);
            if (this.reloadPending) this.minecraft.reloadResourcePacks();
        } else goBack();
    }

    private void exitToParent() {
        if (this.page == Page.THEMES && this.previewingTheme) ThemeListener.restoreActive();
        this.previewingTheme = false;
        applyAllPendingDeletes();
        this.minecraft.gui.setScreen(this.parent);
        if (this.reloadPending) this.minecraft.reloadResourcePacks();
    }

    private void openCurrentPlayback() {
        ContentItem source = currentSourceContent();
        if (source != null) {
            openContent(source);
            return;
        }
        this.viewedContent = null;
        this.page = Page.NOW_PLAYING;
        closeSearch();
        this.rebuildWidgets();
    }

    private ContentItem currentSourceContent() {
        Optional<PlaylistHelper.QueueType> queuedSource = PlaylistHelper.queueSource();
        if (queuedSource.isEmpty()) return null;
        PlaylistHelper.QueueType source = queuedSource.get();
        Identifier id = Identifier.tryParse(source.id());
        if (id == null) return null;
        if (source.type() == MaMDataConfig.QueueType.ALBUM) {
            for (Album album : Album.ALBUMS) {
                if (album.album.equals(id)) return new ContentItem(album, null);
            }
        } else if (source.type() == MaMDataConfig.QueueType.PLAYLIST) {
            for (Playlist playlist : Playlist.PLAYLISTS) {
                if (playlist.playlist.equals(id)) return new ContentItem(null, playlist);
            }
        }
        return null;
    }

    private SourceInfo currentSource() {
        Optional<PlaylistHelper.QueueType> queuedSource = PlaylistHelper.queueSource();
        if (queuedSource.isEmpty()) {
            return new SourceInfo(Component.translatable("screen.music_and_melody.custom_playlist"), MusicScreenHelper.FALLBACK_ICON,
                    Component.literal("custom"),
                    sourceTypeLabel(MaMDataConfig.QueueType.PLAYLIST), Component.translatable("screen.music_and_melody.content_origin.custom"), false);
        }
        PlaylistHelper.QueueType source = queuedSource.get();
        Identifier id = Identifier.tryParse(source.id());
        if (id != null && source.type() == MaMDataConfig.QueueType.ALBUM) {
            for (Album album : Album.ALBUMS) {
                if (album.album.equals(id)) return new SourceInfo(album.name, album.icon, Component.literal(album.album.toString()),
                        sourceTypeLabel(MaMDataConfig.QueueType.ALBUM), originFor(id, null), album.isFavourite());
            }
        }
        if (id != null && source.type() == MaMDataConfig.QueueType.PLAYLIST) {
            for (Playlist playlist : Playlist.PLAYLISTS) {
                if (playlist.playlist.equals(id)) return new SourceInfo(playlist.name, playlist.icon, Component.literal(playlist.playlist.toString()),
                        sourceTypeLabel(MaMDataConfig.QueueType.PLAYLIST), originFor(id, playlist), playlist.isFavourite());
            }
        }
        return new SourceInfo(Component.literal(source.name()), MusicScreenHelper.FALLBACK_ICON,
                Component.literal(source.id()),
                sourceTypeLabel(source.type()), Component.translatable("screen.music_and_melody.content_origin.built_in"), false);
    }

    private static Component sourceTypeLabel(MaMDataConfig.QueueType type) {
        return Component.translatable(type == MaMDataConfig.QueueType.ALBUM ? "screen.music_and_melody.tag.album" : "screen.music_and_melody.tag.playlist");
    }

    private static Component originFor(Identifier id, Playlist playlist) {
        return Component.translatable("screen.music_and_melody.content_origin." + originKeyFor(id, playlist));
    }

    private static String originKeyFor(Identifier id, Playlist playlist) {
        if (playlist != null && playlist.isCustom()) return "custom";
        if (playlist == null && CustomAlbums.isConfigAlbum(id)) return "custom";
        RemotePack.Tag tag = playlist == null ? RemotePack.Tag.ALBUM : RemotePack.Tag.PLAYLIST;
        return RemoteContentManager.isDownloaded(id, tag) ? "downloaded" : "built_in";
    }

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
        items.sort(Comparator.comparing(item -> item.name().getString(), String.CASE_INSENSITIVE_ORDER));
        items.sort(Comparator.comparingInt(item -> item.album() != null && item.album().album.equals(CustomAlbums.MOD_DISCS_ID) ? 1 : 0));
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

    List<Theme> visibleThemes() {
        return ThemeListener.themes().stream()
                .filter(this::matchesThemeTags)
                .filter(theme -> matchesSearch(theme.name.getString(), theme.theme.toString(), theme.description.getString()))
                .sorted(Comparator.comparing(theme -> theme.name.getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean matchesThemeTags(Theme theme) {
        for (ThemeTag tag : this.themeTags) {
            if (!tag.matches(theme)) return false;
        }
        return true;
    }

    List<OnlineCatalog> onlineCatalogs() {
        Map<String, RemotePack.Provenance> provenanceByCatalog = new LinkedHashMap<>();
        for (RemotePack pack : RemoteContentManager.packs()) {
            String repository = pack.repository();
            if (repository == null || repository.isBlank()) continue;
            RemotePack.Provenance provenance = pack.provenance() == null
                    ? RemotePack.Provenance.UNVERIFIED
                    : pack.provenance();
            provenanceByCatalog.merge(repository, provenance, MusicPlayerScreen::preferredProvenance);
        }
        return provenanceByCatalog.entrySet().stream()
                .map(entry -> new OnlineCatalog(entry.getKey(), entry.getKey(), entry.getValue()))
                .filter(catalog -> matchesSearch(catalog.name(), catalog.provenance().label().getString()))
                .sorted(Comparator.comparingInt((OnlineCatalog catalog) -> catalog.provenance().ordinal())
                        .thenComparing(OnlineCatalog::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static SourceInfo customPlaylistSource() {
        return new SourceInfo(Component.translatable("screen.music_and_melody.custom_playlist"), MusicScreenHelper.FALLBACK_ICON,
                Component.literal("custom"),
                sourceTypeLabel(MaMDataConfig.QueueType.PLAYLIST), Component.translatable("screen.music_and_melody.content_origin.custom"), false);
    }

    private static RemotePack.Provenance preferredProvenance(RemotePack.Provenance first, RemotePack.Provenance second) {
        return first.ordinal() <= second.ordinal() ? first : second;
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
            case INSTALLED -> Component.translatable("screen.music_and_melody.content_origin.downloaded");
            case REMOTE -> Component.translatable("button.music_and_melody.download");
        };
    }

    private static boolean remoteActionActive(RemotePack pack) {
        RemoteContentManager.State state = RemoteContentManager.state(pack);
        return RemoteContentManager.missingDependencies(pack).isEmpty()
                && state != RemoteContentManager.State.INSTALLED && state != RemoteContentManager.State.DOWNLOADING;
    }

    static String remoteStateTranslationKey(RemoteContentManager.State state) {
        return switch (state) {
            case INSTALLED -> "screen.music_and_melody.content_origin.downloaded";
            case REMOTE -> "screen.music_and_melody.tag.remote";
            default -> "screen.music_and_melody.remote_album.state." + state.name().toLowerCase(Locale.ROOT);
        };
    }

    private static Component remoteStateMessage(RemoteContentManager.State state) {
        return Component.translatable(remoteStateTranslationKey(state));
    }

    private static Component remoteTypeMessage(RemotePack pack) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < pack.tags().size(); i++) {
            if (i > 0) result.append(Component.literal(" Ã‚Â· "));
            result.append(Component.translatable(switch (pack.tags().get(i)) {
                case ALBUM -> "screen.music_and_melody.tag.album";
                case PLAYLIST -> "screen.music_and_melody.tag.playlist";
                case EVENT -> "screen.music_and_melody.tag.event";
                case THEME -> "screen.music_and_melody.tag.theme";
            }));
        }
        return result;
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
        ThemeHelper.text(graphics, this.font, line, x, y, color);
    }

    private Component fittedContentDetails(Component first, Component second, int width) {
        if (second == null || second.getString().isBlank()) return first;
        int separatorWidth = this.font.width(" \u00b7 ");
        if (this.font.width(first) + separatorWidth + this.font.width(second) <= Math.max(1, width)) {
            return Component.translatable("screen.music_and_melody.content_details", first, second);
        }
        return first;
    }

    private void drawMarquee(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        int textWidth = this.font.width(text);
        if (textWidth <= width) {
            ThemeHelper.text(graphics, this.font, text, x + (width - textWidth) / 2, y, color);
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

        graphics.enableScissor(x, y, x + width, y + this.font.lineHeight + 2);
        ThemeHelper.text(graphics, this.font, text, x - offset, y, color);
        graphics.disableScissor();
    }

    private void drawTrackMarquee(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        int textWidth = this.font.width(text);
        if (textWidth <= width) {
            ThemeHelper.text(graphics, this.font, text, x, y, color);
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

        graphics.enableScissor(x, y, x + width, y + this.font.lineHeight + 2);
        ThemeHelper.text(graphics, this.font, text, x - offset, y, color);
        graphics.disableScissor();
    }

    private List<Breadcrumb> breadcrumbsForCurrentPage() {
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        switch (this.page) {
            case HOME -> breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), null));
            case NOW_PLAYING -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.albums"), () -> setPage(Page.LIBRARY)));
                breadcrumbs.add(new Breadcrumb(customPlaylistSource().name(), null));
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
                    closeSearch();
                    this.selectedEventNamespace = null;
                    this.rebuildWidgets();
                }));
                if (this.selectedEventNamespace != null) breadcrumbs.add(new Breadcrumb(eventFolderLabel(this.selectedEventNamespace), null));
            }
            case ONLINE -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.online_browser"), this.selectedOnlineCatalog == null ? null : () -> {
                    closeSearch();
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
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.themes"), null));
            }
            case CONFIG -> {
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.home"), () -> setPage(Page.HOME)));
                breadcrumbs.add(new Breadcrumb(Component.translatable("screen.music_and_melody.config"), null));
            }
        }
        return breadcrumbs;
    }

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
            ThemeHelper.text(graphics, this.font, omission, left, PANEL_TOP + 11, TEXT_DESCRIPTION);
            left += omissionWidth;
        }

        for (int index = 0; index < visible.size(); index++) {
            Breadcrumb breadcrumb = visible.get(index);
            int labelWidth = this.font.width(breadcrumb.label());
            int color = index == visible.size() - 1 ? TEXT_TITLE : TEXT_HEADER;
            ThemeHelper.text(graphics, this.font, breadcrumb.label(), left, PANEL_TOP + 11, color);
            if (breadcrumb.action() != null) {
                this.breadcrumbHits.add(new BreadcrumbHit(left, PANEL_TOP + 8, labelWidth, this.font.lineHeight + 5, breadcrumb.action()));
            }
            left += labelWidth;
            if (index < visible.size() - 1) {
                ThemeHelper.text(graphics, this.font, separator, left, PANEL_TOP + 11, TEXT_DESCRIPTION);
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
        BUILT_IN("screen.music_and_melody.content_origin.built_in"),
        CUSTOM("screen.music_and_melody.content_origin.custom"),
        FAVOURITED("screen.music_and_melody.favourites"),
        DOWNLOADED("screen.music_and_melody.content_origin.downloaded"),
        ENABLED("screen.music_and_melody.album_details.enabled"),
        DISABLED("screen.music_and_melody.album_details.disabled");

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
                case ENABLED -> item.album() != null && !CustomAlbums.isConfigAlbum(item.album()) && item.album().isEnabled();
                case DISABLED -> item.album() != null && !CustomAlbums.isConfigAlbum(item.album()) && !item.album().isEnabled();
            };
        }
    }

    private enum EventTag implements Tag {
        BUILT_IN("screen.music_and_melody.content_origin.built_in"),
        CUSTOM("screen.music_and_melody.content_origin.custom"),
        DOWNLOADED("screen.music_and_melody.content_origin.downloaded"),
        ENABLED("screen.music_and_melody.album_details.enabled"),
        DISABLED("screen.music_and_melody.album_details.disabled");

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
                // downloaded event origins are not distinguished until remote catalogs expose categories
                case DOWNLOADED -> false;
                case ENABLED -> source.isEnabled();
                case DISABLED -> !source.isEnabled();
            };
        }
    }

    private enum ThemeTag implements Tag {
        BUILT_IN("screen.music_and_melody.content_origin.built_in"),
        CUSTOM("screen.music_and_melody.content_origin.custom"),
        DOWNLOADED("screen.music_and_melody.content_origin.downloaded");

        private final String translationKey;

        ThemeTag(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public Component label() {
            return Component.translatable(this.translationKey);
        }

        boolean matches(Theme theme) {
            boolean custom = theme.isCustom();
            boolean downloaded = !custom && ThemeListener.isDownloaded(theme.theme);
            return switch (this) {
                case BUILT_IN -> !custom && !downloaded;
                case CUSTOM -> custom;
                case DOWNLOADED -> downloaded;
            };
        }
    }

    private enum OnlineTag implements Tag {
        ALBUM("screen.music_and_melody.tag.album"),
        PLAYLIST("screen.music_and_melody.tag.playlist"),
        EVENT("screen.music_and_melody.tag.event"),
        THEME("screen.music_and_melody.tag.theme"),
        DOWNLOADED("screen.music_and_melody.content_origin.downloaded"),
        DOWNLOADABLE("screen.music_and_melody.tag.downloadable"),
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
                case ALBUM -> pack.tags().contains(RemotePack.Tag.ALBUM);
                case PLAYLIST -> pack.tags().contains(RemotePack.Tag.PLAYLIST);
                case EVENT -> pack.tags().contains(RemotePack.Tag.EVENT);
                case THEME -> pack.tags().contains(RemotePack.Tag.THEME);
                case DOWNLOADED -> state == RemoteContentManager.State.INSTALLED
                        || state == RemoteContentManager.State.NEEDS_RELOAD
                        || state == RemoteContentManager.State.UPDATE_AVAILABLE;
                case DOWNLOADABLE -> RemoteContentManager.isDownloadable(pack);
                case NEEDS_UPDATE -> state == RemoteContentManager.State.UPDATE_AVAILABLE;
            };
        }
    }

    private static final class GuiMultiplierSlider extends AbstractSliderButton {
        private static final int STEP_COUNT = 10;
        private final MusicPlayerScreen screen;

        GuiMultiplierSlider(MusicPlayerScreen screen, int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), positionFor(MaMDataConfig.get().gui_multiplier));
            this.screen = screen;
            updateMessage();
        }

        @Override
        protected void setValue(double value) {
            super.setValue(steppedPosition(value));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("screen.music_and_melody.gui_multiplier", formattedMultiplier(multiplier(this.value))));
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            commit();
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            boolean handled = super.keyPressed(event);
            if (handled) commit();
            return handled;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            boolean highlighted = this.active && (this.isMouseOver(mouseX, mouseY) || this.isFocused());
            int background = !this.active ? BUTTON_DISABLED : highlighted ? BUTTON_HIGHLIGHT : BUTTON_PASSIVE;
            graphics.fill(x, y, x + width, y + height, background);
            int filledWidth = Math.round((width - 4) * (float) this.value);
            graphics.fill(x, y, x + filledWidth + 2, y + height, PANEL_HIGHLIGHT);
            int handleX = x + Math.round((width - 4) * (float) this.value);
            graphics.fill(handleX, y - 1, handleX + 4, y + height + 1, TEXT_TITLE);
            int textColor = this.active ? TEXT_PRIMARY : TEXT_DISABLED;
            var font = Minecraft.getInstance().font;
            ThemeHelper.text(graphics, font, this.getMessage(), x + (width - font.width(this.getMessage())) / 2, y + (height - 8) / 2, textColor);
        }

        private void commit() {
            float multiplier = multiplier(this.value);
            MaMDataConfig config = MaMDataConfig.get();
            if (Float.compare(config.gui_multiplier, multiplier) == 0) return;
            config.gui_multiplier = multiplier;
            AutoConfig.getConfigHolder(MaMDataConfig.class).save();
            this.screen.repositionElements();
        }

        private static double positionFor(float multiplier) {
            float clamped = Math.max(0.5F, Math.min(1.0F, multiplier));
            return (clamped - 0.5F) / 0.5F;
        }

        private static double steppedPosition(double value) {
            return Math.round(Math.max(0.0D, Math.min(1.0D, value)) * STEP_COUNT) / (double) STEP_COUNT;
        }

        private static float multiplier(double position) {
            int step = (int) Math.round(Math.max(0.0D, Math.min(1.0D, position)) * STEP_COUNT);
            return 0.5F + step * 0.05F;
        }

        private static String formattedMultiplier(float multiplier) {
            return String.format(Locale.ROOT, "%.0f%%", multiplier * 100.0F);
        }
    }

    private record OnlineCatalog(String name, String catalog, RemotePack.Provenance provenance) {}

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

        MaMDataConfig.QueueType type() {
            return this.album != null ? MaMDataConfig.QueueType.ALBUM : MaMDataConfig.QueueType.PLAYLIST;
        }

        boolean favourite() {
            return this.album != null ? this.album.isFavourite() : this.playlist.isFavourite();
        }

        Component details() {
            int tracks = this.album != null ? this.album.tracks.size() : this.playlist.tracks.size();
            int discs = this.album != null ? this.album.discs.size() : this.playlist.discs.size();
            Component tracksText = Component.translatable(tracks == 1 ? "screen.music_and_melody.track_count.single" : "screen.music_and_melody.track_count.multiple", tracks);
            if (discs == 0) return tracksText;
            Component discsText = Component.translatable(discs == 1 ? "screen.music_and_melody.disc_count.single" : "screen.music_and_melody.disc_count.multiple", discs);
            return Component.translatable("screen.music_and_melody.content_details", tracksText, discsText);
        }

        List<SafeIdentifier> queueSongs(Minecraft minecraft) {
            if (this.album != null) {
                List<SafeIdentifier> songs = new ArrayList<>();
                this.album.tracks.stream()
                        .map(this.album::trackId)
                        .forEach(songs::add);
                this.album.discs.stream()
                        .map(disc -> MusicDiscHelper.discSoundId(minecraft, this.album, disc))
                        .flatMap(Optional::stream)
                        .map(SafeIdentifier::convert)
                        .forEach(songs::add);
                return songs;
            }
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

    private record SourceInfo(Component name, Identifier icon, Component id, Component typeLabel, Component originLabel, boolean favourite) {}

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
            if (!this.active || hovered) {
                int background = !this.active ? BUTTON_DISABLED : BUTTON_HIGHLIGHT;
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
            }
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
            this.screen.drawTruncated(graphics, this.screen.fittedContentDetails(source.typeLabel(), source.originLabel(), textWidth),
                    textX, this.getY() + 21, textWidth, TEXT_DESCRIPTION);
        }
    }

    private static final class ContentTrackList extends PanelList<ContentTrackEntry> {
        ContentTrackList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, bottom, TRACK_ROW_HEIGHT);
            refresh();
        }

        void refresh() {
            double scroll = this.scrollAmount();
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
            this.setScrollAmount(scroll);
        }

        private void addAlbum(Album album, List<SafeIdentifier> queue) {
            if (CustomAlbums.isConfigAlbum(album)) {
                this.addEntry(ContentTrackEntry.manage(this.screen, this.minecraft, () -> this.screen.openConfigAlbumEditor(album)));
            }
            boolean tracksHeader = false;
            for (String track : album.tracks) {
                SafeIdentifier song = album.trackId(track);
                TrackStatus status = CustomAlbums.isConfigAlbum(album) ? null : TrackStatus.forAlbumTrack(album, track);
                if (!matches(song, MusicScreenHelper.trackName(album, track))) continue;
                if (!tracksHeader) {
                    this.addEntry(ContentTrackEntry.header(this.minecraft, Component.translatable("screen.music_and_melody.album_details.tracks")));
                    tracksHeader = true;
                }
                addSong(queue.indexOf(song), song, status, CustomAlbums.isConfigAlbum(album));
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
            addSong(queueIndex, song, status, false);
        }

        private void addSong(int queueIndex, SafeIdentifier song, TrackStatus status, boolean configTrack) {
            if (queueIndex < 0) return;
            Component title = MusicScreenHelper.playlistName(this.minecraft, song);
            if (!matches(song, title)) return;
            this.addEntry(new ContentTrackEntry(this.screen, this.minecraft, queueIndex, song, status, configTrack));
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
        private final WorkspaceButton manageButton;
        private final boolean configTrack;

        private ContentTrackEntry(Minecraft minecraft, Component heading) {
            this.screen = null;
            this.minecraft = minecraft;
            this.queueIndex = -1;
            this.song = null;
            this.heading = heading;
            this.status = null;
            this.addButton = null;
            this.toggleButton = null;
            this.manageButton = null;
            this.configTrack = false;
        }

        static ContentTrackEntry header(Minecraft minecraft, Component heading) {
            return new ContentTrackEntry(minecraft, heading);
        }

        static ContentTrackEntry manage(MusicPlayerScreen screen, Minecraft minecraft, Runnable action) {
            return new ContentTrackEntry(screen, minecraft, action);
        }

        private ContentTrackEntry(MusicPlayerScreen screen, Minecraft minecraft, Runnable action) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.queueIndex = -1;
            this.song = null;
            this.heading = null;
            this.status = null;
            this.addButton = null;
            this.toggleButton = null;
            this.manageButton = new WorkspaceButton(0, 0, 1, 20, Component.translatable("screen.music_and_melody.edit_album"), false,
                    ignored -> action.run());
            this.configTrack = false;
        }

        ContentTrackEntry(MusicPlayerScreen screen, Minecraft minecraft, int queueIndex, SafeIdentifier song, TrackStatus status) {
            this(screen, minecraft, queueIndex, song, status, false);
        }

        ContentTrackEntry(MusicPlayerScreen screen, Minecraft minecraft, int queueIndex, SafeIdentifier song, TrackStatus status, boolean configTrack) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.queueIndex = queueIndex;
            this.song = song;
            this.heading = null;
            this.status = status;
            this.addButton = IconButton.createListIcon(Component.translatable("button.music_and_melody.queue"), IconButton.icon("queue"), ignored ->
                    this.screen.addTrackToCustomPlaylist(this.song));
            this.toggleButton = status != null && status.toggleable()
                    ? IconButton.createListIcon(status.message(), status.icon(), ignored -> this.screen.toggleContentTrack(status.album(), status.track()))
                    : null;
            this.manageButton = null;
            this.configTrack = configTrack;
        }

        @Override
        public Component getNarration() {
            if (this.manageButton != null) return this.manageButton.getMessage();
            return this.heading != null ? this.heading : MusicScreenHelper.playlistName(this.minecraft, this.song);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (this.manageButton != null) {
                this.manageButton.setX(this.getContentX());
                this.manageButton.setY(this.getContentY());
                this.manageButton.setWidth(this.getContentWidth());
                this.manageButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                return;
            }
            if (this.heading != null) {
                ThemeHelper.text(graphics, this.minecraft.font, this.heading.copy().withStyle(ChatFormatting.BOLD), this.getContentX() + 4,
                        this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, TEXT_HEADER_SECONDARY);
                return;
            }
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            boolean unlocked = MusicDiscHelper.isSoundUnlocked(this.minecraft, this.song);
            int numberX = this.getContentX() + TRACK_NUMBER_OFFSET;
            int textX = this.getContentX() + TRACK_TEXT_OFFSET;
            int buttons = IconButton.SIZE + (this.status == null ? 0 : IconButton.SIZE + 4) + (this.configTrack ? IconButton.SIZE + 4 : 0);
            int textWidth = Math.max(1, this.getContentWidth() - TRACK_TEXT_OFFSET - buttons - 8);
            Component title = MusicScreenHelper.playlistName(this.minecraft, this.song);
            boolean enabled = this.status == null || this.status.enabled();
            int color = PlaylistHelper.isQueuePlaying(this.song) ? TEXT_SELECTED : enabled && unlocked ? TEXT_PRIMARY : TEXT_DISABLED;
            ThemeHelper.text(graphics, this.minecraft.font, Component.literal((this.queueIndex + 1) + "."), numberX,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, TEXT_DESCRIPTION);
            this.screen.drawTrackMarquee(graphics, title, textX,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, textWidth, color);
            int addX = this.getContentRight() - IconButton.SIZE - 3;
            boolean alreadyAdded = PlaylistHelper.isInCustomPlaylist(this.song);
            this.addButton.setX(addX);
            this.addButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.addButton.active = !alreadyAdded;
            if (!alreadyAdded) this.addButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
            int actionX = addX - IconButton.SIZE - 4;
            if (this.configTrack) {
                IconButton.renderIconWithTooltip(graphics, IconButton.icon("config"), actionX,
                        this.getContentYMiddle() - IconButton.SIZE / 2,
                        Component.translatable("screen.music_and_melody.content_origin.custom"), mouseX, mouseY);
                actionX -= IconButton.SIZE + 4;
            }
            if (this.status != null) {
                int statusX = actionX;
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
            if (this.manageButton != null) return this.manageButton.mouseClicked(event, doubleClick);
            if (this.heading != null) return true;
            this.addButton.active = !PlaylistHelper.isInCustomPlaylist(this.song);
            if (contains(this.addButton, event)) {
                if (this.addButton.active) {
                    this.addButton.mouseClicked(event, doubleClick);
                    this.addButton.active = false;
                }
                return true;
            }
            if (this.toggleButton != null && contains(this.toggleButton, event)) {
                if (this.toggleButton.active) this.toggleButton.mouseClicked(event, doubleClick);
                return true;
            }
            this.screen.playClick();
            this.screen.playContentTrack(this.queueIndex);
            return true;
        }

        private static boolean contains(IconButton button, MouseButtonEvent event) {
            return event.x() >= button.getX() && event.y() >= button.getY()
                    && event.x() < button.getX() + button.getWidth()
                    && event.y() < button.getY() + button.getHeight();
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
            super(minecraft, panelWidth, Math.max(1, bottom - top), top, rowHeight);
            this.screen = screen;
            this.panelX = panelX;
            this.panelWidth = panelWidth;
            this.setX(panelX);
            this.centerListVertically = false;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {}

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

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
            graphics.fill(x, top, x + 4, bottom, BAR_BACKGROUND);
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
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("button.music_and_melody.events"),
                    MaMClientConfig.get().allow_events, () -> screen.setPage(Page.EVENTS)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.themes"), () -> screen.setPage(Page.THEMES)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.online_browser"),
                    RemoteContentManager.onlineFunctionalityEnabled(), () -> screen.setPage(Page.ONLINE)));
            this.addEntry(new HomeMenuEntry(screen, Component.translatable("screen.music_and_melody.config"), () -> screen.setPage(Page.CONFIG)));
            this.addEntry(new HomeMenuEntry(screen, CommonComponents.GUI_DONE, screen::onClose));
        }
    }

    private static final class HomeMenuEntry extends ObjectSelectionList.Entry<HomeMenuEntry> {
        private final MusicPlayerScreen screen;
        private final WorkspaceButton button;

        HomeMenuEntry(MusicPlayerScreen screen, Component label, Runnable action) {
            this(screen, label, true, action);
        }

        HomeMenuEntry(MusicPlayerScreen screen, Component label, boolean active, Runnable action) {
            this.screen = screen;
            this.button = new WorkspaceButton(0, 0, 1, 22, label, false, ignored -> action.run());
            this.button.active = active;
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
            List<SafeIdentifier> songs = PlaylistHelper.customPlaylistSongs();
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
            this.removeButton = IconButton.createListIcon(Component.translatable("button.music_and_melody.remove"), IconButton.icon("remove"), ignored ->
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
            ThemeHelper.text(graphics, this.minecraft.font, Component.literal((this.index + 1) + "."), numberX, this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, TEXT_DESCRIPTION);
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
            double scroll = this.scrollAmount();
            this.clearEntries();
            this.screen.favouriteItems().stream()
                    .map(item -> new FavouriteEntry(this.screen, this.minecraft, item))
                    .forEach(this::addEntry);
            this.setScrollAmount(scroll);
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
                    Math.max(1, this.getContentWidth() - iconSize - 8),
                    this.screen.isContentDeletePending(this.item) ? TEXT_PENDING_DELETION : TEXT_FAVOURITE);
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
            double scroll = this.scrollAmount();
            this.clearEntries();
            this.screen.libraryItems().stream()
                    .map(item -> new LibraryEntry(this.screen, this.minecraft, item))
                    .forEach(this::addEntry);
            this.setScrollAmount(scroll);
        }
    }

    private static final class LibraryEntry extends ObjectSelectionList.Entry<LibraryEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final ContentItem item;
        private final IconButton favouriteButton;
        private final IconButton albumEnabledButton;
        private final Identifier contentOriginIcon;

        LibraryEntry(MusicPlayerScreen screen, Minecraft minecraft, ContentItem item) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.item = item;
            this.favouriteButton = IconButton.createListIcon(Component.translatable("button.music_and_melody.favourite"), IconButton.icon(item.favourite() ? "favourited" : "favourite"), ignored -> this.screen.toggleFavourite(this.item));
            this.albumEnabledButton = item.album() == null || CustomAlbums.isConfigAlbum(item.album()) ? null : IconButton.createListIcon(
                    Component.translatable(item.album().isEnabled() ? "screen.music_and_melody.album_details.enabled" : "screen.music_and_melody.album_details.disabled"),
                    IconButton.icon(item.album().isEnabled() ? "enabled" : "disabled"),
                    ignored -> this.screen.toggleAlbumEnabled(this.item.album()));
            boolean custom = item.playlist() != null && item.playlist().isCustom()
                    || item.album() != null && CustomAlbums.isConfigAlbum(item.album());
            this.contentOriginIcon = item.playlist() != null || custom ? IconButton.icon(custom ? "config" : "built_in") : null;
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
            int actionWidth = IconButton.SIZE + (this.albumEnabledButton == null && this.contentOriginIcon == null ? 0 : IconButton.SIZE + 4);
            int textWidth = this.getContentWidth() - iconSize - actionWidth - 16;
            int titleColor = this.screen.isContentDeletePending(this.item) ? TEXT_PENDING_DELETION
                    : this.item.favourite() ? TEXT_FAVOURITE : TEXT_TITLE;
            this.screen.drawTrackMarquee(graphics, this.item.name(), textX, this.getContentYMiddle() - 10,
                    Math.max(1, textWidth), titleColor);
            this.screen.drawTrackMarquee(graphics, this.item.details(), textX, this.getContentYMiddle() + 2, Math.max(1, textWidth), TEXT_DESCRIPTION);
            this.favouriteButton.setIconAndTooltip(IconButton.icon(this.item.favourite() ? "favourited" : "favourite"), Component.translatable(this.item.favourite() ? "button.music_and_melody.unfavourite" : "button.music_and_melody.favourite"));
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
            } else if (this.contentOriginIcon != null) {
                boolean custom = this.item.playlist() != null && this.item.playlist().isCustom()
                        || this.item.album() != null && CustomAlbums.isConfigAlbum(this.item.album());
                IconButton.renderIconWithTooltip(graphics, this.contentOriginIcon, favouriteX - IconButton.SIZE - 4,
                        this.getContentYMiddle() - IconButton.SIZE / 2,
                        Component.translatable(custom ? "screen.music_and_melody.content_origin.custom" : "screen.music_and_melody.content_origin.built_in"), mouseX, mouseY);
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
            ThemeHelper.text(graphics, this.screen.font, Component.literal("\u25B8 ").append(eventFolderLabel(this.namespace)), this.getContentX() + 4, this.getContentYMiddle() - this.screen.font.lineHeight / 2, TEXT_TITLE);
            Component suffix = Component.translatable(this.count == 1 ? "screen.music_and_melody.event_count.single" : "screen.music_and_melody.event_count.multiple", this.count);
            int x = this.getContentRight() - this.screen.font.width(suffix) - 3;
            ThemeHelper.text(graphics, this.screen.font, suffix, x, this.getContentYMiddle() - this.screen.font.lineHeight / 2, TEXT_DESCRIPTION);
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
            this.toggleButton = IconButton.createListIcon(Component.translatable(source.isEnabled() ? "button.music_and_melody.disable" : "button.music_and_melody.enable"), IconButton.icon(source.isEnabled() ? "enabled" : "disabled"), ignored -> {
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
            int titleColor = this.screen.isEventDeletePending(this.source.id) ? TEXT_PENDING_DELETION : this.source.isEnabled() ? TEXT_TITLE : TEXT_DESCRIPTION;
            this.screen.drawTrackMarquee(graphics, this.source.record.name(), textX, this.getContentYMiddle() - 10,
                    Math.max(1, textWidth), titleColor);
            this.screen.drawTrackMarquee(graphics, this.source.record.description(), textX, this.getContentYMiddle() + 2, Math.max(1, textWidth), TEXT_DESCRIPTION);
            this.toggleButton.setIconAndTooltip(IconButton.icon(this.source.isEnabled() ? "enabled" : "disabled"), Component.translatable(this.source.isEnabled() ? "button.music_and_melody.disable" : "button.music_and_melody.enable"));
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

    private static final class ThemeList extends PanelList<ThemeEntry> {
        ThemeList(MusicPlayerScreen screen, Minecraft minecraft, int panelX, int panelWidth, int top, int bottom) {
            super(screen, minecraft, panelX, panelWidth, top, completeRowsBottom(top, bottom, 42), 42);
            refresh();
        }

        private static int completeRowsBottom(int top, int bottom, int rowHeight) {
            // AbstractSelectionList starts its first entry two pixels below the list top. Include that inset when aligning the viewport, or the final row would still lose its bottom two pixels
            int available = Math.max(0, bottom - top - 2);
            int rows = Math.max(1, available / rowHeight);
            return Math.min(bottom, top + 2 + rows * rowHeight + 2);
        }

        void refresh() {
            double scroll = this.scrollAmount();
            this.clearEntries();
            this.screen.visibleThemes().stream()
                    .map(theme -> new ThemeEntry(this.screen, this.minecraft, theme))
                    .forEach(this::addEntry);
            this.setScrollAmount(scroll);
        }
    }

    private static final class ThemeEntry extends ObjectSelectionList.Entry<ThemeEntry> {
        private final MusicPlayerScreen screen;
        private final Minecraft minecraft;
        private final Theme theme;

        ThemeEntry(MusicPlayerScreen screen, Minecraft minecraft, Theme theme) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.theme = theme;
        }

        @Override
        public Component getNarration() {
            return this.theme.name;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean selected = this.theme.theme.equals(this.screen.selectedThemeId);
            if (hovered || selected) {
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            }
            int iconSize = 32;
            int iconY = this.getContentYMiddle() - iconSize / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, this.theme.icon),
                    this.getContentX() + 3, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            int textX = this.getContentX() + iconSize + 9;
            int textWidth = Math.max(1, this.getContentWidth() - iconSize - 18);
            int titleColor = this.screen.isThemeDeletePending(this.theme.theme) || !this.theme.valid
                    ? TEXT_PENDING_DELETION : isActiveTheme(this.theme) ? TEXT_SELECTED : TEXT_TITLE;
            this.screen.drawTrackMarquee(graphics, this.theme.name, textX, this.getContentYMiddle() - 15, textWidth,
                    titleColor);
            this.screen.drawTrackMarquee(graphics, this.theme.description, textX,
                    this.getContentYMiddle() - 4, textWidth, TEXT_DESCRIPTION);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.screen.playClick();
            if (this.screen.previewingTheme) ThemeListener.restoreActive();
            this.screen.selectedThemeId = this.theme.theme;
            this.screen.viewedTheme = this.theme;
            this.screen.previewingTheme = false;
            this.screen.rebuildWidgets();
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
                    .map(catalog -> new OnlineCatalogEntry(this.screen, catalog))
                    .forEach(this::addEntry);
            this.addEntry(OnlineCatalogEntry.addRepository(this.screen));
        }
    }

    private static final class OnlineCatalogEntry extends ObjectSelectionList.Entry<OnlineCatalogEntry> {
        private final MusicPlayerScreen screen;
        private final Component label;
        private final String catalog;
        private final RemotePack.Provenance provenance;
        private final boolean addRepository;

        OnlineCatalogEntry(MusicPlayerScreen screen, Component label, String catalog) {
            this(screen, label, catalog, null, false);
        }

        OnlineCatalogEntry(MusicPlayerScreen screen, OnlineCatalog catalog) {
            this(screen, Component.literal(catalog.name()), catalog.catalog(), catalog.provenance(), false);
        }

        private OnlineCatalogEntry(MusicPlayerScreen screen, Component label, String catalog,
                                   RemotePack.Provenance provenance, boolean addRepository) {
            this.screen = screen;
            this.label = label;
            this.catalog = catalog;
            this.provenance = provenance;
            this.addRepository = addRepository;
        }

        static OnlineCatalogEntry addRepository(MusicPlayerScreen screen) {
            return new OnlineCatalogEntry(screen, Component.translatable("button.music_and_melody.add_repository"), "", null, true);
        }

        @Override
        public Component getNarration() {
            return this.label;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHT);
            int color = this.addRepository ? TEXT_HEADER_SECONDARY : TEXT_TITLE;
            Component text = this.addRepository ? this.label : Component.literal("\u25B8 ").append(this.label);
            int textY = this.getContentYMiddle() - this.screen.font.lineHeight / 2;
            if (this.provenance == null) {
                ThemeHelper.text(graphics, this.screen.font, text, this.getContentX() + 4, textY, color);
                return;
            }
            Component status = this.provenance.label();
            int statusX = this.getContentRight() - this.screen.font.width(status) - 4;
            int textWidth = Math.max(1, statusX - this.getContentX() - 8);
            this.screen.drawTrackMarquee(graphics, text, this.getContentX() + 4, textY, textWidth, color);
            ThemeHelper.text(graphics, this.screen.font, status, statusX, textY, TEXT_DESCRIPTION);
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
            this.actionButton = IconButton.createListIcon(Component.translatable("button.music_and_melody.download"), IconButton.icon("download"), ignored -> {
                if (showRemoteDeleteAction(this.screen, this.pack)) {
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
            Component details = Component.translatable("screen.music_and_melody.content_details",
                    remoteTypeMessage(this.pack), remoteStateMessage(RemoteContentManager.state(this.pack)));
            this.screen.drawTrackMarquee(graphics, details, textX, this.getContentYMiddle() + 2, Math.max(1, textWidth), TEXT_DESCRIPTION);
            updateAction();
            this.actionButton.setX(this.getContentRight() - IconButton.SIZE - 3);
            this.actionButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.actionButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        private void updateAction() {
            RemoteContentManager.State state = RemoteContentManager.state(this.pack);
            if (showRemoteDeleteAction(this.screen, this.pack)) {
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
            this.actionButton.active = remoteActionActive(this.pack);
        }

        private static boolean showRemoteDeleteAction(MusicPlayerScreen screen, RemotePack pack) {
            return remoteDeleteAvailable(pack)
                    && (screen.isRemoteDeletePending(pack) || !remoteActionActive(pack));
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
