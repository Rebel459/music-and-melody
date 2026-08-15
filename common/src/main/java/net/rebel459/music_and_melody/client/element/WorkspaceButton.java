package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.util.ThemeHelper;

import java.util.Optional;

/**
 * The flat action treatment used throughout the compact music workspace. A
 * theme may provide textures for its three states; otherwise the shared colour
 * tokens provide the surface.
 */
public final class WorkspaceButton extends Button {

    private final boolean selected;

    public WorkspaceButton(int x, int y, int width, int height, Component message, boolean selected, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.selected = selected;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        boolean highlighted = this.active && (this.selected || this.isHoveredOrFocused());
        Optional<Identifier> texture = !this.active
                ? ThemeHelper.BUTTON_DISABLED_TEXTURE
                : highlighted
                  ? ThemeHelper.BUTTON_HIGHLIGHTED_TEXTURE
                  : ThemeHelper.BUTTON_TEXTURE;
        if (texture.isPresent()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture.get(), this.getX(), this.getY(),
                    0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        } else {
            int background = highlighted ? ThemeHelper.BUTTON_HIGHLIGHT : ThemeHelper.BUTTON_PASSIVE;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
            if (this.selected) {
                graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.getHeight(), ThemeHelper.PANEL_HIGHLIGHT);
            }
        }
        int textColor = this.active ? ThemeHelper.TEXT_PRIMARY : ThemeHelper.TEXT_DISABLED;
        graphics.centeredText(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2, textColor);
    }
}
