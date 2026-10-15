package net.rebel459.music_and_melody.client.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
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
    private Tooltip tooltip;
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
            renderTooltipForArea(graphics, tooltip, x, y, SIZE, SIZE, mouseX, mouseY);
        }
    }

    public static void renderTooltipForArea(GuiGraphicsExtractor graphics, Component tooltip,
                                            int x, int y, int width, int height, int mouseX, int mouseY) {
        scheduleTooltip(graphics, Tooltip.create(tooltip), x, y, width, height, mouseX, mouseY, false, false);
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
        this.tooltip = Tooltip.create(message);
        this.setMessage(message);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.onPress(this);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        super.updateWidgetNarration(output);
        if (this.tooltip != null) this.tooltip.updateNarration(output);
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
        boolean hovered = mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth()
                && mouseY < this.getY() + this.getHeight();
        boolean keyboardFocused = this.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        if (this.tooltip != null && (hovered || keyboardFocused)) {
            scheduleTooltip(graphics, this.tooltip, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                    mouseX, mouseY, this.isFocused(), keyboardFocused && !hovered);
        }
    }

    private static void scheduleTooltip(GuiGraphicsExtractor graphics, Tooltip tooltip, int x, int y, int width, int height, int mouseX, int mouseY, boolean replaceExisting, boolean keyboardFocused) {
        int left = scaleTooltipCoordinate(x);
        int top = scaleTooltipCoordinate(y);
        int right = scaleTooltipCoordinate(x + width);
        int bottom = scaleTooltipCoordinate(y + height);
        ScreenRectangle bounds = new ScreenRectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
        ClientTooltipPositioner positioner = keyboardFocused
                ? new BelowOrAboveWidgetTooltipPositioner(bounds)
                : new MenuTooltipPositioner(bounds);
        Minecraft minecraft = Minecraft.getInstance();
        graphics.setTooltipForNextFrame(minecraft.font, tooltip.toCharSequence(minecraft), tooltip.component(),
                positioner, scaleTooltipCoordinate(mouseX), scaleTooltipCoordinate(mouseY),
                replaceExisting, tooltip.style());
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
