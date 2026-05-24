package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;

class IconButton extends Button {

    static final int SIZE = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = (SIZE - ICON_SIZE) / 2;

    private Identifier icon;

    IconButton(Component message, Identifier icon, OnPress onPress) {
        this(0, 0, message, icon, onPress);
    }

    IconButton(int x, int y, Component message, Identifier icon, OnPress onPress) {
        super(x, y, SIZE, SIZE, message, onPress, DEFAULT_NARRATION);
        setIconAndTooltip(icon, message);
    }

    static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "textures/gui/" + name + ".png");
    }

    static void renderIcon(GuiGraphics graphics, Identifier icon, int x, int y) {
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

    static void renderIconWithTooltip(GuiGraphics graphics, Identifier icon, int x, int y, Component tooltip, int mouseX, int mouseY) {
        renderIcon(graphics, icon, x, y);
        if (mouseX >= x && mouseY >= y && mouseX < x + SIZE && mouseY < y + SIZE) {
            graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
        }
    }

    void setIconAndTooltip(Identifier icon, Component message) {
        this.icon = icon;
        this.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.onPress(this);
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        this.renderDefaultSprite(graphics);
        renderIcon(graphics, this.icon, this.getX(), this.getY());
    }
}
