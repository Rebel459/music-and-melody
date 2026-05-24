package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rebel459.music_and_melody.MusicAndMelody;

class IconButton extends Button {

    static final int SIZE = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = (SIZE - ICON_SIZE) / 2;

    private ResourceLocation icon;

    IconButton(Component message, ResourceLocation icon, OnPress onPress) {
        this(0, 0, message, icon, onPress);
    }

    IconButton(int x, int y, Component message, ResourceLocation icon, OnPress onPress) {
        super(x, y, SIZE, SIZE, message, onPress, DEFAULT_NARRATION);
        setIconAndTooltip(icon, message);
    }

    static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(MusicAndMelody.MOD_ID, "textures/gui/" + name + ".png");
    }

    static void renderIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y) {
        graphics.blit(
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

    static void renderIconWithTooltip(GuiGraphics graphics, ResourceLocation icon, int x, int y, Component tooltip, int mouseX, int mouseY) {
        renderIcon(graphics, icon, x, y);
        if (mouseX >= x && mouseY >= y && mouseX < x + SIZE && mouseY < y + SIZE) {
            graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }

    void setIconAndTooltip(ResourceLocation icon, Component message) {
        this.icon = icon;
        this.setMessage(message);
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        Component message = this.getMessage();
        this.setMessage(Component.empty());
        super.renderWidget(graphics, mouseX, mouseY, tickDelta);
        this.setMessage(message);
        renderIcon(graphics, this.icon, this.getX(), this.getY());
    }
}
