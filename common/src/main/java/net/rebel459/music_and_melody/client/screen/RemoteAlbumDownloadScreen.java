package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumManager;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumPack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public class RemoteAlbumDownloadScreen extends Screen {

    private static final int WIDTH = 308;
    private final AlbumScreen parent;
    private final RemoteAlbumPack pack;
    private Button importButton;

    public RemoteAlbumDownloadScreen(AlbumScreen parent, RemoteAlbumPack pack) {
        super(Component.translatable("screen.music_and_melody.remote_redirect"));
        this.parent = parent;
        this.pack = pack;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - WIDTH / 2;
        int buttonY = this.height - 27;
        int buttonWidth = (WIDTH - 8) / 3;
        int modrinthY = descriptionBottomY() + 10;

        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.modrinth"), button -> {
                    Util.getPlatform().openUri(URI.create("https://modrinth.com/mod/music-and-melody"));
                })
                .bounds(x + buttonWidth + 4, modrinthY, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.download"), button -> {
            Util.getPlatform().openUri(URI.create(RemoteAlbumManager.externalDownloadUrl(this.pack)));
        }).bounds(x, buttonY, buttonWidth, 20).build());

        this.importButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.import"), button -> importLocal())
                .bounds(x + buttonWidth + 4, buttonY, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(x + (buttonWidth + 4) * 2, buttonY, buttonWidth, 20)
                .build());
        MusicScreenHelper.addSocialButtons(this);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);

        int y = 48;
        graphics.drawCenteredString(this.font, this.pack.name(), this.width / 2, y, 0xFFFFFFFF);

        y += 28;
        for (var line : descriptionLines()) {
            graphics.drawCenteredString(this.font, line, this.width / 2, y, 0xFFCCCCCC);
            y += this.font.lineHeight + 2;
        }

        updateImportButton();
    }

    @Override
    public void onClose() {
        this.parent.refreshList();
        this.minecraft.setScreen(this.parent);
    }

    private void importLocal() {
        String path = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable("screen.music_and_melody.remote_redirect.import").getString(),
                this.pack.fileName(),
                null,
                "ZIP files",
                false
        );
        if (path == null || path.isBlank()) return;

        RemoteAlbumManager.importLocal(this.pack, Path.of(path));
        updateImportButton();
    }

    private void updateImportButton() {
        if (this.importButton != null) {
            RemoteAlbumManager.State state = RemoteAlbumManager.state(this.pack);
            this.importButton.active = state != RemoteAlbumManager.State.DOWNLOADING
                    && state != RemoteAlbumManager.State.NEEDS_RELOAD
                    && state != RemoteAlbumManager.State.INSTALLED;
        }
    }

    private List<FormattedCharSequence> descriptionLines() {
        return this.font.split(Component.translatable("screen.music_and_melody.remote_redirect.description"), WIDTH);
    }

    private int descriptionBottomY() {
        int y = 48 + 28;
        for (int i = 0; i < descriptionLines().size(); i++) {
            y += this.font.lineHeight + 2;
        }
        return y;
    }
}
