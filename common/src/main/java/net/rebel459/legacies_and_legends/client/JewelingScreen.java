package net.rebel459.legacies_and_legends.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.menu.JewelingMenu;
import org.spongepowered.asm.mixin.Unique;

public class JewelingScreen extends AbstractContainerScreen<JewelingMenu> {

    @Unique
    private static final Identifier MENU_TEXTURE = LaLConstants.id("textures/gui/container/jeweling_table.png");

    @Unique
    private static final Identifier WAND_SLOT = LaLConstants.id("container/slot/wand");

    public JewelingScreen(JewelingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelX = 60;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, MENU_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (!this.menu.slots.getFirst().hasItem()) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WAND_SLOT, this.leftPos + 34, this.topPos + 47, 16, 16);
    };
}
