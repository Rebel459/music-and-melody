package net.rebel459.music_and_melody.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumManager;
import net.rebel459.music_and_melody.client.remote.RemoteAlbumPack;

import java.util.List;

public class RemoteAlbumDetailsScreen extends Screen {

    private final AlbumScreen parent;
    private final RemoteAlbumPack pack;

    public RemoteAlbumDetailsScreen(AlbumScreen parent, RemoteAlbumPack pack) {
        super(pack.name());
        this.parent = parent;
        this.pack = pack;
    }

    @Override
    protected void init() {
        int rowX = this.width / 2 - AlbumScreen.MAIN_BUTTON_ROW_WIDTH / 2;
        int buttonY = this.height - 27;
        int buttonWidth = (AlbumScreen.MAIN_BUTTON_ROW_WIDTH - 4) / 2;

        Button loadButton = this.addRenderableWidget(Button.builder(Component.translatable("button.music_and_melody.load"), button -> {})
                .bounds(rowX, buttonY, buttonWidth, 20)
                .build());
        loadButton.active = false;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(rowX + (buttonWidth + 4), buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        super.render(graphics, mouseX, mouseY, tickDelta);
        int iconSize = 32;
        int maxTextWidth = Math.max(1, this.width - 80);
        FormattedCharSequence title = this.font.split(this.title, maxTextWidth).getFirst();
        String id = this.font.plainSubstrByWidth(this.pack.id().toString(), maxTextWidth);
        int textWidth = Math.max(this.font.width(title), this.font.width(id));
        int titleX = this.width / 2 - (iconSize + 6 + textWidth) / 2;
        int titleY = 24;

        graphics.blit(RenderPipelines.GUI_TEXTURED, MusicScreenHelper.albumIcon(this.minecraft, this.pack.icon()), titleX, titleY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        graphics.drawString(this.font, title, titleX + iconSize + 6, titleY + 4, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal(id).withStyle(ChatFormatting.GRAY), titleX + iconSize + 6, titleY + 17, 0xFFAAAAAA);

        int x = this.width / 2 - AlbumScreen.MAIN_BUTTON_ROW_WIDTH / 2;
        int y = 76;
        line(graphics, Component.translatable("screen.music_and_melody.remote_album.repository", this.pack.repository()), x, y);
        line(graphics, Component.translatable("screen.music_and_melody.remote_album.version", this.pack.version()), x, y += 14);

        List<FormattedCharSequence> description = this.font.split(this.pack.description(), AlbumScreen.MAIN_BUTTON_ROW_WIDTH);
        y += 24;
        for (FormattedCharSequence line : description) {
            graphics.drawString(this.font, line, x, y, 0xFFCCCCCC);
            y += this.font.lineHeight + 2;
        }
    }

    @Override
    public void onClose() {
        this.parent.refreshList();
        this.minecraft.setScreen(this.parent);
    }

    private void line(GuiGraphics graphics, Component component, int x, int y) {
        graphics.drawString(this.font, component, x, y, 0xFFAAAAAA);
    }

    private Component remoteActionMessage() {
        return switch (RemoteAlbumManager.state(this.pack)) {
            case DOWNLOADING -> Component.translatable("button.music_and_melody.downloading");
            case NEEDS_RELOAD -> Component.translatable("button.music_and_melody.reload");
            case UPDATE_AVAILABLE -> Component.translatable("button.music_and_melody.update");
            case FAILED -> Component.translatable("button.music_and_melody.retry");
            default -> Component.translatable("button.music_and_melody.download");
        };
    }

    private static Component stateName(RemoteAlbumManager.State state) {
        return Component.translatable("screen.music_and_melody.remote_album.state." + state.name().toLowerCase());
    }
}
