package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.util.ThemeHelper;

/**
 * The action treatment used throughout the compact music workspace. Themes
 * can opt into the normal vanilla button sprites for all ordinary buttons.
 */
public class WorkspaceButton extends Button {

    private static final Identifier VANILLA_BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier VANILLA_BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier VANILLA_BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

    private boolean selected;

    public WorkspaceButton(int x, int y, int width, int height, Component message, boolean selected, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.selected = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        boolean highlighted = this.active && (this.selected || this.isHoveredOrFocused());
        if (ThemeHelper.BUTTON_TEXTURES) {
            Identifier sprite = this.active
                    ? highlighted ? VANILLA_BUTTON_HIGHLIGHTED : VANILLA_BUTTON
                    : VANILLA_BUTTON_DISABLED;
            if (!ThemeHelper.renderThemeButton(graphics, sprite, this.getX(), this.getY(),
                    this.getWidth(), this.getHeight(), 1.0F)) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(),
                        this.getWidth(), this.getHeight());
            }
        } else {
            int background = highlighted ? ThemeHelper.BUTTON_HIGHLIGHT : ThemeHelper.BUTTON_PASSIVE;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
            if (this.selected) {
                graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.getHeight(), ThemeHelper.PANEL_HIGHLIGHT);
            }
        }
        int textColor = textColor();
        graphics.centeredText(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2, textColor);
    }

    protected int textColor() {
        return this.active ? ThemeHelper.TEXT_PRIMARY : ThemeHelper.TEXT_DISABLED;
    }
}
