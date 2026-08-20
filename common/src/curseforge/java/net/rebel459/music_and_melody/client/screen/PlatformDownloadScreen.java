package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.net.URI;
import java.nio.file.Path;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

/** CurseForge's manual ZIP-download flow, presented in the workspace modal style. */
public final class PlatformDownloadScreen extends Screen {

    private final MusicPlayerScreen parent;
    private final RemotePack pack;
    private WorkspaceButton importButton;
    private int layoutWidth;
    private int layoutHeight;

    public PlatformDownloadScreen(MusicPlayerScreen parent, RemotePack pack) {
        super(Component.translatable("screen.music_and_melody.remote_redirect"));
        this.parent = parent;
        this.pack = pack;
    }

    @Override
    protected void init() {
        calculateLayoutSize();
        this.addRenderableOnly(this::renderDialog);
        int x = panelX() + 12;
        int y = panelY();
        int width = panelWidth() - 24;
        int buttonWidth = (width - 10) / 3;

        this.addRenderableWidget(new WorkspaceButton(x + buttonWidth + 5, y + 130, buttonWidth, 20,
                Component.translatable("button.music_and_melody.modrinth"), false,
                ignored -> Util.getPlatform().openUri(URI.create("https://modrinth.com/mod/music-and-melody"))));
        this.addRenderableWidget(new WorkspaceButton(x, y + panelHeight() - 29, buttonWidth, 20,
                Component.translatable("button.music_and_melody.download"), false,
                ignored -> Util.getPlatform().openUri(URI.create(RemoteContentManager.externalDownloadUrl(this.pack)))));
        this.importButton = this.addRenderableWidget(new WorkspaceButton(x + buttonWidth + 5, y + panelHeight() - 29, buttonWidth, 20,
                Component.translatable("button.music_and_melody.import"), false, ignored -> importLocal()));
        this.addRenderableWidget(new WorkspaceButton(x + (buttonWidth + 5) * 2, y + panelHeight() - 29, buttonWidth, 20,
                CommonComponents.GUI_DONE, false, ignored -> onClose()));
        updateImportButton();
    }

    private void importLocal() {
        String path = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable("screen.music_and_melody.remote_redirect.import").getString(),
                this.pack.fileName(), null, "ZIP files", false);
        if (path == null || path.isBlank()) return;
        RemoteContentManager.importLocal(this.pack, Path.of(path));
        updateImportButton();
    }

    private void updateImportButton() {
        if (this.importButton == null) return;
        RemoteContentManager.State state = RemoteContentManager.state(this.pack);
        this.importButton.active = state != RemoteContentManager.State.DOWNLOADING
                && state != RemoteContentManager.State.NEEDS_RELOAD
                && state != RemoteContentManager.State.INSTALLED;
    }

    @Override
    public void tick() {
        super.tick();
        updateImportButton();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // The parent workspace supplies the visible background beneath this modal.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, -1, -1, tickDelta);
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

    private void renderDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        if ((DIM_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, DIM_OVERLAY);
        graphics.fill(x, y, x + width, y + height, MODAL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        graphics.centeredText(this.font, this.title, x + width / 2, y + 13, TEXT_TITLE);
        graphics.centeredText(this.font, this.pack.name(), x + width / 2, y + 37, TEXT_DESCRIPTION);

        int textY = y + 59;
        for (var line : this.font.split(Component.translatable("screen.music_and_melody.remote_redirect.description"), width - 24)) {
            graphics.centeredText(this.font, line, x + width / 2, textY, TEXT_DESCRIPTION);
            textY += this.font.lineHeight + 2;
        }
    }

    @Override
    protected void repositionElements() {
        calculateLayoutSize();
        this.rebuildWidgets();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(toLayoutMouse(event), doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(toLayoutMouse(event), dragX / MaMDataConfig.get().gui_multiplier,
                dragY / MaMDataConfig.get().gui_multiplier);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(toLayoutMouse(event));
    }

    private void calculateLayoutSize() {
        this.layoutWidth = Math.max(1, Math.round(this.width / MaMDataConfig.get().gui_multiplier));
        this.layoutHeight = Math.max(1, Math.round(this.height / MaMDataConfig.get().gui_multiplier));
    }

    private MouseButtonEvent toLayoutMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / MaMDataConfig.get().gui_multiplier,
                event.y() / MaMDataConfig.get().gui_multiplier, event.buttonInfo());
    }

    private int toLayoutMouse(int coordinate) {
        return Math.round(coordinate / MaMDataConfig.get().gui_multiplier);
    }

    private int panelWidth() {
        return Math.max(1, Math.min(360, this.layoutWidth - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(190, this.layoutHeight - 24));
    }

    private int panelX() {
        return this.layoutWidth / 2 - panelWidth() / 2;
    }

    private int panelY() {
        return this.layoutHeight / 2 - panelHeight() / 2;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
        this.parent.rebuildWidgets();
    }
}
