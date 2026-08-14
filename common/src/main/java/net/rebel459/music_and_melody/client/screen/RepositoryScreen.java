package net.rebel459.music_and_melody.client.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * An in-context editor for remote catalog URLs. It deliberately renders the
 * player below the dialog so adding a repository never feels like leaving the
 * online browser.
 */
final class RepositoryScreen extends Screen {

    private static final int PANEL_BACKGROUND = 0xFF151C2A;
    private static final int PANEL_BORDER = 0xFF78A6FF;
    private static final int ROW_HOVER = 0xAA344765;
    private static final int MUTED = 0xFF9DA9BF;

    private final MusicPlayerScreen parent;
    private RepositoryList list;
    private EditBox urlField;
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

        this.list = this.addRenderableWidget(new RepositoryList(this, this.minecraft, x + 10, width - 20, y + 34, y + height - 70));
        this.urlField = this.addRenderableWidget(new EditBox(this.font, x + 10, y + height - 31, Math.max(110, width - 82), 20,
                Component.translatable("screen.music_and_melody.repositories.url")));
        this.urlField.setMaxLength(1024);
        this.urlField.setHint(Component.literal("https://example.com/catalog.json"));
        this.urlField.setResponder(ignored -> clearFeedback());
        this.addRenderableWidget(new WorkspaceButton(x + width - 66, y + height - 31, 56, 20,
                Component.translatable("button.music_and_melody.add"), false, ignored -> addRepository()));
        this.addRenderableWidget(new WorkspaceButton(x + width - 66, y + 8, 56, 20, CommonComponents.GUI_DONE, false,
                ignored -> this.onClose()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // See QueueMutationConfirmScreen: this dialog renders its own dimmed
        // backdrop and must not claim another global blur pass.
    }

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        graphics.fill(0, 0, this.width, this.height, 0x52000000);
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        graphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
        graphics.text(this.font, Component.translatable("screen.music_and_melody.repositories.title"), x + 10, y + 14, 0xFFFFFFFF);
        if (!this.feedback.getString().isEmpty()) {
            graphics.text(this.font, this.feedback, x + 10, y + height - 47, this.feedbackError ? 0xFFFF8B8B : 0xFF9ED9A0);
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

        MaMClientConfig config = MaMClientConfig.get();
        if (config.remote_repositories == null) config.remote_repositories = new ArrayList<>();
        if (config.remote_repositories.stream().anyMatch(value::equalsIgnoreCase)) {
            setFeedback(Component.translatable("screen.music_and_melody.repositories.duplicate"), true);
            return;
        }
        config.remote_repositories.add(value);
        saveChanges();
        this.urlField.setValue("");
        setFeedback(Component.translatable("screen.music_and_melody.repositories.added"), false);
        if (this.list != null) this.list.refresh();
    }

    void removeRepository(String value) {
        MaMClientConfig config = MaMClientConfig.get();
        if (config.remote_repositories == null || !config.remote_repositories.remove(value)) return;
        saveChanges();
        setFeedback(Component.translatable("screen.music_and_melody.repositories.removed"), false);
        if (this.list != null) this.list.refresh();
    }

    private void saveChanges() {
        this.changed = true;
        AutoConfig.getConfigHolder(MaMClientConfig.class).save();
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
            List<String> repositories = MaMClientConfig.get().remote_repositories;
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
            this.removeButton = new IconButton(Component.translatable("screen.music_and_melody.repositories.remove"), IconButton.icon("remove"), ignored -> this.screen.removeRepository(this.repository));
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.repository);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), ROW_HOVER);
            int textWidth = Math.max(1, this.getContentWidth() - IconButton.SIZE - 10);
            String shown = tail(this.minecraft, this.repository, textWidth);
            graphics.text(this.minecraft.font, Component.literal(shown), this.getContentX() + 3,
                    this.getContentYMiddle() - this.minecraft.font.lineHeight / 2, 0xFFE6EBF5);
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
