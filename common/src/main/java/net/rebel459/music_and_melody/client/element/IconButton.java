package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.util.ThemeHelper;

public class IconButton extends Button {

    public static final int SIZE = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = (SIZE - ICON_SIZE) / 2;

    private Identifier icon;
    private Component tooltipMessage;
    private boolean selected;
    private static float tooltipScale = 1.0F;

    public IconButton(Component message, Identifier icon, OnPress onPress) {
        this(0, 0, message, icon, onPress);
    }

    public IconButton(int x, int y, Component message, Identifier icon, OnPress onPress) {
        super(x, y, SIZE, SIZE, message, onPress, DEFAULT_NARRATION);
        setIconAndTooltip(icon, message);
    }

    public static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "textures/gui/" + name + ".png");
    }

    public static void renderIcon(GuiGraphicsExtractor graphics, Identifier icon, int x, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                x + ICON_PADDING,
                y + ICON_PADDING,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE
        );
    }

    public static void renderIconWithTooltip(GuiGraphicsExtractor graphics, Identifier icon, int x, int y, Component tooltip, int mouseX, int mouseY) {
        renderIcon(graphics, icon, x, y);
        if (mouseX >= x && mouseY >= y && mouseX < x + SIZE && mouseY < y + SIZE) {
            graphics.setTooltipForNextFrame(tooltip, scaleTooltipCoordinate(mouseX), scaleTooltipCoordinate(mouseY));
        }
    }

    /**
     * The compact workspace renders in its own logical coordinate space. The
     * widgets still receive logical mouse coordinates for hit testing, while
     * tooltip positions must be expressed in the actual screen coordinate
     * space. Screens using that renderer set this for the duration of their
     * extraction pass.
     */
    public static void setTooltipScale(float scale) {
        tooltipScale = Math.max(0.01F, scale);
    }

    public static void resetTooltipScale() {
        tooltipScale = 1.0F;
    }

    public static int scaleTooltipCoordinate(double coordinate) {
        return Math.round((float) (coordinate * tooltipScale));
    }

    public void setIconAndTooltip(Identifier icon, Component message) {
        this.icon = icon;
        this.tooltipMessage = message;
        this.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltipForNextRenderPass(graphics, scaleTooltipCoordinate(mouseX), scaleTooltipCoordinate(mouseY));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.onPress(this);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        boolean highlighted = this.active && (this.selected || this.isHoveredOrFocused());
        Identifier texture = !this.active
                ? ThemeHelper.BUTTON_DISABLED_TEXTURE.orElse(null)
                : highlighted
                  ? ThemeHelper.BUTTON_HIGHLIGHTED_TEXTURE.orElse(null)
                  : ThemeHelper.BUTTON_TEXTURE.orElse(null);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.getX(), this.getY(),
                    0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
        } else if (highlighted) {
            graphics.fill(this.getX(), this.getY(), this.getX() + SIZE, this.getY() + SIZE,
                    ThemeHelper.BUTTON_HIGHLIGHT);
        }
        renderIcon(graphics, this.icon, this.getX(), this.getY());
        if (this.tooltipMessage != null
                && mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth()
                && mouseY < this.getY() + this.getHeight()) {
            graphics.setTooltipForNextFrame(this.tooltipMessage,
                    scaleTooltipCoordinate(mouseX), scaleTooltipCoordinate(mouseY));
        }
    }
}
