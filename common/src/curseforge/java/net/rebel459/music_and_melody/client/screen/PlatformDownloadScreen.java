package net.rebel459.music_and_melody.client.screen;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;
import net.rebel459.music_and_melody.client.remote.RemoteContentManager;
import net.rebel459.music_and_melody.client.remote.RemotePack;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.client.util.FileDialogs;
import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.client.util.MusicScreenHelper;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

public final class PlatformDownloadScreen extends Screen {

    private final MusicPlayerScreen parent;
    private final RemotePack pack;
    private WorkspaceButton importButton;
    private boolean choosing;
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
                ignored -> Blaze3D.openUri(URI.create("https://modrinth.com/mod/music-and-melody"))));
        this.addRenderableWidget(new WorkspaceButton(x, y + panelHeight() - 29, buttonWidth, 20,
                Component.translatable("button.music_and_melody.download"), false,
                ignored -> Blaze3D.openUri(RemoteContentManager.externalDownloadUrl(this.pack))));
        this.importButton = this.addRenderableWidget(new WorkspaceButton(x + buttonWidth + 5, y + panelHeight() - 29, buttonWidth, 20,
                Component.translatable("button.music_and_melody.import"), false, ignored -> importLocal()));
        this.addRenderableWidget(new WorkspaceButton(x + (buttonWidth + 5) * 2, y + panelHeight() - 29, buttonWidth, 20,
                CommonComponents.GUI_DONE, false, ignored -> onClose()));
        updateImportButton();
    }

    private void importLocal() {
        // new dialog code
        FileDialogs.openFiles(this.pack.fileName(), "ZIP files", List.of("zip"), false, files -> {
            if (files.isEmpty()) return;
            RemoteContentManager.importLocal(this.pack, files.getFirst());
            updateImportButton();
        });
        // new up-ported code
        if (this.choosing) return;
        this.choosing = true;
        updateImportButton();
        String title = Component.translatable("screen.music_and_melody.remote_redirect.import").getString();
        String defaultName = this.pack.importFileName();
        Thread chooser = new Thread(() -> {
            String path = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.zip"));
                filters.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog(title, defaultName, filters, "ZIP files", false);
            } catch (RuntimeException ignored) {
            }
            String chosen = path;
            Minecraft.getInstance().execute(() -> {
                this.choosing = false;
                if (chosen != null && !chosen.isBlank()) RemoteContentManager.importLocal(this.pack, Path.of(chosen));
                updateImportButton();
            });
        }, "Music & Melody file picker");
        chooser.setDaemon(true);
        chooser.start();
    }

    private void updateImportButton() {
        if (this.importButton == null) return;
        RemoteContentManager.State state = RemoteContentManager.state(this.pack);
        this.importButton.active = !this.choosing
                && state != RemoteContentManager.State.DOWNLOADING
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
        this.parent.extractBackground(graphics, mouseX, mouseY, tickDelta);
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
        if ((POPUP_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.layoutWidth, this.layoutHeight, POPUP_OVERLAY);
        graphics.fill(x, y, x + width, y + height, POPUP_PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, POPUP_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, POPUP_OUTLINE);
        centeredText(graphics, this.font, this.title, x + width / 2, y + 13, TEXT_TITLE);
        centeredText(graphics, this.font, Language.getInstance().getVisualOrder(this.font.substrByWidth(this.pack.name(), Math.max(1, width - 24))),
                x + width / 2, y + 37, TEXT_DESCRIPTION);

        int textY = y + 59;
        for (var line : this.font.split(Component.translatable("screen.music_and_melody.remote_redirect.description"), width - 24)) {
            centeredText(graphics, this.font, line, x + width / 2, textY, TEXT_DESCRIPTION);
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
