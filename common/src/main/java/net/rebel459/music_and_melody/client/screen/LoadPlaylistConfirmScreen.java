package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.element.WorkspaceButton;

import java.util.List;

import static net.rebel459.music_and_melody.client.util.ThemeHelper.*;

/** Confirmation shown before Load overwrites the Custom Playlist. */
final class LoadPlaylistConfirmScreen extends Screen {

    private static final int WIDTH = 310;
    private static final int HEIGHT = 110;
    private final Screen parent;
    private final Runnable action;

    LoadPlaylistConfirmScreen(Screen parent, Runnable action) {
        super(Component.translatable("screen.music_and_melody.discard_custom_playlist"));
        this.parent = parent;
        this.action = action;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - WIDTH / 2;
        int y = this.height / 2 - HEIGHT / 2 + HEIGHT - 29;
        int buttonWidth = (WIDTH - 8) / 2;
        this.addRenderableWidget(new WorkspaceButton(x + 2, y, buttonWidth, 20,
                Component.translatable("button.music_and_melody.load"), false, ignored -> {
                    this.action.run();
                    this.minecraft.gui.setScreen(this.parent);
                }));
        this.addRenderableWidget(new WorkspaceButton(x + buttonWidth + 6, y, buttonWidth, 20,
                CommonComponents.GUI_CANCEL, false, ignored -> this.onClose()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        // The parent workspace supplies the visible background beneath this modal.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        int x = this.width / 2 - WIDTH / 2;
        int y = this.height / 2 - HEIGHT / 2;
        if ((DIM_OVERLAY >>> 24) != 0) graphics.fill(0, 0, this.width, this.height, DIM_OVERLAY);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, MODAL_BACKGROUND);
        graphics.fill(x, y, x + WIDTH, y + 1, POPUP_OUTLINE);
        graphics.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, POPUP_OUTLINE);
        graphics.fill(x, y, x + 1, y + HEIGHT, POPUP_OUTLINE);
        graphics.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, POPUP_OUTLINE);
        graphics.centeredText(this.font, this.title, x + WIDTH / 2, y + 12, TEXT_TITLE);
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                Component.translatable("screen.music_and_melody.discard_custom_playlist.warning"), WIDTH - 24);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.text(this.font, lines.get(i), x + 12, y + 31 + i * this.font.lineHeight, TEXT_DESCRIPTION);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = this.width / 2 - WIDTH / 2;
        int y = this.height / 2 - HEIGHT / 2;
        if (event.x() < x || event.x() >= x + WIDTH || event.y() < y || event.y() >= y + HEIGHT) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
