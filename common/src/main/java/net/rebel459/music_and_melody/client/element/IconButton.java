package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.util.ThemeHelper;

public class IconButton extends Button {

    public static final int SIZE = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = (SIZE - ICON_SIZE) / 2;
    private static final Identifier VANILLA_BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier VANILLA_BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier VANILLA_BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

    private Identifier icon;
    private Component tooltipMessage;
    private boolean selected;
    private boolean listIcon;
    private boolean forceVanillaTextures;
    private static float tooltipScale = 1.0F;

    public IconButton(Component message, Identifier icon, OnPress onPress) {
        this(0, 0, message, icon, onPress);
    }

    public IconButton(int x, int y, Component message, Identifier icon, OnPress onPress) {
        super(x, y, SIZE, SIZE, message, onPress, DEFAULT_NARRATION);
        setIconAndTooltip(icon, message);
    }

    public static IconButton createMusicPlayer(int x, int y, OnPress onPress) {
        IconButton button = new IconButton(x, y, Component.translatable("button.music_and_melody.music_player"),
                MusicAndMelody.id("textures/gui/icon.png"), onPress);
        button.forceVanillaTextures = true;
        return button;
    }

    public static IconButton createListIcon(Component message, Identifier icon, OnPress onPress) {
        IconButton button = new IconButton(message, icon, onPress);
        button.listIcon = true;
        return button;
    }

    public static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "textures/gui/" + name + ".png");
    }

    public static void renderIcon(GuiGraphicsExtractor graphics, Identifier icon, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ThemeHelper.getThemeTexture(icon), x + ICON_PADDING, y + ICON_PADDING, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    public static void renderIconWithTooltip(GuiGraphicsExtractor graphics, Identifier icon, int x, int y, Component tooltip, int mouseX, int mouseY) {
        renderIcon(graphics, icon, x, y);
        if (mouseX >= x && mouseY >= y && mouseX < x + SIZE && mouseY < y + SIZE) {
            graphics.setTooltipForNextFrame(tooltip, scaleTooltipCoordinate(mouseX), scaleTooltipCoordinate(mouseY));
        }
    }

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
        int width = Math.max(1, this.getWidth());
        int height = Math.max(1, this.getHeight());
        boolean highlighted = this.active && this.isMouseOver(mouseX, mouseY);
        if ((ThemeHelper.BUTTON_TEXTURES || this.forceVanillaTextures) && !this.listIcon) {
            Identifier sprite = this.active
                    ? highlighted ? VANILLA_BUTTON_HIGHLIGHTED : VANILLA_BUTTON
                    : VANILLA_BUTTON_DISABLED;
            if (forceVanillaTextures || !ThemeHelper.renderThemeButton(graphics, sprite, this.getX(), this.getY(),
                    width, height, this.getAlpha())) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), width, height,
                        this.getAlpha());
            }
        } else if (highlighted) {
            graphics.fill(this.getX(), this.getY(), this.getX() + width, this.getY() + height,
                    ARGB.multiplyAlpha(ThemeHelper.BUTTON_HIGHLIGHTED, this.getAlpha()));
        }
        renderButtonIcon(graphics, this.icon, this.getX(), this.getY(), width, height, this.getAlpha());
        if (this.tooltipMessage != null
                && mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth()
                && mouseY < this.getY() + this.getHeight()) {
            graphics.setTooltipForNextFrame(this.tooltipMessage,
                    scaleTooltipCoordinate(mouseX), scaleTooltipCoordinate(mouseY));
        }
    }

    private static void renderButtonIcon(GuiGraphicsExtractor graphics, Identifier icon, int x, int y, int width, int height, float alpha) {
        icon = ThemeHelper.getThemeTexture(icon);
        int iconSize = Math.min(ICON_SIZE, Math.max(1, Math.min(width, height) - 4));
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon,
                x + (width - iconSize) / 2,
                y + (height - iconSize) / 2,
                0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize, ARGB.white(alpha));
    }
}
