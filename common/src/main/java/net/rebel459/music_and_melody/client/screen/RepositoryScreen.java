package net.rebel459.music_and_melody.client.screen;

import net.rebel459.music_and_melody.client.util.ThemeHelper;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

final class RepositoryScreen extends Screen {

    private final MusicPlayerScreen parent;
    private RepositoryList list;
    private EditBox urlField;
    private WorkspaceButton officialButton;
    private WorkspaceButton communityButton;
    private Component feedback = Component.empty();
    private boolean feedbackError;
    private boolean changed;

    RepositoryScreen(MusicPlayerScreen parent) {
        super(Component.translatable("screen.music_and_melody.repositories"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableOnly(this::renderDialog);
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();

        int entriesX = x + 10;
        int entriesWidth = width - 20;
        int providerTop = y + 34;
        int providerWidth = (entriesWidth - 4) / 2;
        MaMDataConfig.Remote remote = MaMDataConfig.get().remote;
        this.officialButton = this.addRenderableWidget(new WorkspaceButton(entriesX, providerTop, providerWidth, 20,
                Component.translatable("button.music_and_melody.official_catalogs"), remote.official_provider,
                ignored -> toggleProvider(true)));
        this.officialButton.setTooltip(Tooltip.create(Component.translatable("button.music_and_melody.official_catalogs.tooltip")));
        this.communityButton = this.addRenderableWidget(new WorkspaceButton(entriesX + providerWidth + 4, providerTop,
                entriesWidth - providerWidth - 4, 20,
                Component.translatable("button.music_and_melody.community_catalogs"), remote.community_provider,
                ignored -> toggleProvider(false)));
        this.communityButton.setTooltip(Tooltip.create(Component.translatable("button.music_and_melody.community_catalogs.tooltip")));
        this.list = this.addRenderableWidget(new RepositoryList(this, this.minecraft, entriesX, entriesWidth, providerTop + 24, y + height - 70));
        this.urlField = this.addRenderableWidget(new EditBox(this.font, x + 10, y + height - 31, Math.max(110, width - 82), 20,
                Component.translatable("screen.music_and_melody.repositories.url")));
        this.urlField.setMaxLength(1024);
        this.urlField.setHint(Component.literal("https://example.com/catalog.json")
                .withStyle(style -> style.withColor(rgb(TEXT_EXAMPLE))));
        this.urlField.setResponder(ignored -> clearFeedback());
        this.addRenderableWidget(new WorkspaceButton(x + width - 66, y + height - 31, 56, 20,
                Component.translatable("button.music_and_melody.add"), false, ignored -> addRepository()));
        this.addRenderableWidget(new WorkspaceButton(x + width - 66, y + 8, 56, 20, CommonComponents.GUI_DONE, false,
                ignored -> this.onClose()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, -1, -1, tickDelta);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {}

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        if ((POPUP_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.width, this.height, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        ThemeHelper.text(graphics, this.font, Component.translatable("button.music_and_melody.add_repository"), x + 10, y + 14, TEXT_HEADER_SECONDARY);
        if (!this.feedback.getString().isEmpty()) {
            ThemeHelper.text(graphics, this.font, this.feedback, x + 10, y + height - 47, this.feedbackError ? TEXT_PENDING_DELETION : TEXT_SELECTED);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseDragged(event, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!insideDialog(event.x(), event.y())) return true;
        super.mouseReleased(event);
        return true;
    }

    private boolean insideDialog(double mouseX, double mouseY) {
        return mouseX >= panelX() && mouseX < panelX() + panelWidth()
                && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    private void addRepository() {
        if (this.urlField == null) return;
        String value = this.urlField.getValue().trim();
        if (!validRepository(value)) {
            setFeedback(Component.translatable("screen.music_and_melody.repositories.invalid"), true);
            return;
        }

        MaMDataConfig config = MaMDataConfig.get();
        if (config.remote.catalogs == null) config.remote.catalogs = new ArrayList<>();
        if (config.remote.catalogs.stream().anyMatch(value::equalsIgnoreCase)) {
            setFeedback(Component.translatable("screen.music_and_melody.repositories.duplicate"), true);
            return;
        }
        config.remote.catalogs.add(value);
        saveChanges();
        this.urlField.setValue("");
        setFeedback(Component.translatable("screen.music_and_melody.repositories.added"), false);
        if (this.list != null) this.list.refresh();
    }

    private void toggleProvider(boolean official) {
        MaMDataConfig.Remote remote = MaMDataConfig.get().remote;
        if (official) remote.official_provider = !remote.official_provider;
        else remote.community_provider = !remote.community_provider;
        this.changed = true;
        saveChanges();
        RemoteContentManager.refresh();
        this.rebuildWidgets();
    }

    void removeRepository(String value) {
        MaMDataConfig config = MaMDataConfig.get();
        if (config.remote.catalogs == null || !config.remote.catalogs.remove(value)) return;
        saveChanges();
        setFeedback(Component.translatable("screen.music_and_melody.repositories.removed"), false);
        if (this.list != null) this.list.refresh();
    }

    private void saveChanges() {
        this.changed = true;
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private void clearFeedback() {
        this.feedback = Component.empty();
    }

    private void setFeedback(Component message, boolean error) {
        this.feedback = message;
        this.feedbackError = error;
    }

    private static boolean validRepository(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
        if (this.changed) this.parent.repositoriesChanged();
    }

    private int panelWidth() {
        return Math.min(460, this.width - 24);
    }

    private int panelHeight() {
        return Math.min(250, this.height - 28);
    }

    private int panelX() {
        return this.width / 2 - panelWidth() / 2;
    }

    private int panelY() {
        return this.height / 2 - panelHeight() / 2;
    }

    private static final class RepositoryList extends ObjectSelectionList<RepositoryEntry> {
        private final RepositoryScreen screen;
        private final int x;
        private final int width;

        RepositoryList(RepositoryScreen screen, Minecraft minecraft, int x, int width, int top, int bottom) {
            super(minecraft, width, Math.max(1, bottom - top), top, 25);
            this.screen = screen;
            this.x = x;
            this.width = width;
            this.setX(x);
            this.centerListVertically = false;
            refresh();
        }

        void refresh() {
            this.clearEntries();
            List<String> repositories = MaMDataConfig.get().remote.catalogs;
            if (repositories == null) return;
            for (String repository : repositories) {
                if (repository != null && !repository.isBlank()) this.addEntry(new RepositoryEntry(this.screen, this.minecraft, repository));
            }
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {}

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

        @Override
        public int getRowLeft() {
            return this.x + 3;
        }

        @Override
        public int getRowWidth() {
            return Math.max(32, this.width - 10);
        }

        @Override
        protected int scrollBarX() {
            return this.x + this.width - 7;
        }
    }

    private static final class RepositoryEntry extends ObjectSelectionList.Entry<RepositoryEntry> {
        private final RepositoryScreen screen;
        private final Minecraft minecraft;
        private final String repository;
        private final IconButton removeButton;

        RepositoryEntry(RepositoryScreen screen, Minecraft minecraft, String repository) {
            this.screen = screen;
            this.minecraft = minecraft;
            this.repository = repository;
            this.removeButton = IconButton.createListIcon(Component.translatable("screen.music_and_melody.repositories.remove"), IconButton.icon("remove"), ignored -> this.screen.removeRepository(this.repository));
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.repository);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BUTTON_HIGHLIGHTED);
            int textWidth = Math.max(1, this.getContentWidth() - IconButton.SIZE - 10);
            String shown = tail(this.minecraft, this.repository, textWidth);
            ThemeHelper.text(graphics, this.minecraft.font, Component.literal(shown), this.getContentX() + 3,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, TEXT_PRIMARY);
            this.removeButton.setX(this.getContentRight() - IconButton.SIZE - 3);
            this.removeButton.setY(this.getContentYMiddle() - IconButton.SIZE / 2);
            this.removeButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.removeButton.mouseClicked(event, doubleClick);
        }

        private static String tail(Minecraft minecraft, String value, int width) {
            if (minecraft.font.width(value) <= width) return value;
            String ellipsis = "\u2026";
            int start = value.length();
            while (start > 0 && minecraft.font.width(ellipsis + value.substring(start - 1)) <= width) start--;
            return ellipsis + value.substring(start);
        }
    }
}
