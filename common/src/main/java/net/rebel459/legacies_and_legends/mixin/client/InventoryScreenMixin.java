package net.rebel459.legacies_and_legends.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.util.AccessorySlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Unique
    private static final RenderPipeline INVENTORY_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation("pipeline/inventory").build()
    );

    @Unique
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");

    @Unique
    private static final Identifier ACCESSORY_SLOT = LaLConstants.id("container/slot/accessory");

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void renderAccessoryBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        InventoryMenu menu = screen.getMenu();

        for (Slot slot : menu.slots) {
            if (slot instanceof AccessorySlot) {
                int screenX = screen.leftPos + slot.x - 1;
                int screenY = screen.topPos + slot.y - 1;

                graphics.blitSprite(INVENTORY_PIPELINE, SLOT, screenX, screenY, 18, 18);
                if (!slot.hasItem()) graphics.blitSprite(INVENTORY_PIPELINE, ACCESSORY_SLOT, screenX, screenY, 18, 18);
                break;
            }
        }
    }
}