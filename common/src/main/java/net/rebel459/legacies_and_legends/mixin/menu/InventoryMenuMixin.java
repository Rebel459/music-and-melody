package net.rebel459.legacies_and_legends.mixin.menu;

import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.AccessorySlotInterface;
import net.rebel459.legacies_and_legends.util.AccessorySlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu implements AccessorySlotInterface {

    @Unique
    private final SimpleContainer accessoryContainer = new SimpleContainer(1);

    @Unique
    public Slot accessorySlot;

    protected InventoryMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @Override
    public ItemStack getAccessory() {
        return this.accessoryContainer.getItem(0);
    }

    @Override
    public void setAccessory(ItemStack stack) {
        this.accessoryContainer.setItem(0, stack.copy());
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL")
    )
    private void addAccessorySlot(Inventory playerInventory, boolean active, Player player, CallbackInfo ci) {
        InventoryMenu menu = InventoryMenu.class.cast(this);

        int x = 77;
        int y = 44;

        this.accessorySlot = new AccessorySlot(this.accessoryContainer, player, 0, x, y);
        menu.addSlot(this.accessorySlot);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void handleAccessoryQuickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        InventoryMenu menu = InventoryMenu.class.cast(this);
        Slot slot = menu.slots.get(index);
        if (slot == this.accessorySlot) {
            if (slot.hasItem()) {}
        } else if (slot.getItem().is(LaLItemTags.ACCESSORIES) && !this.accessorySlot.hasItem()) {
            ItemStack stack = slot.remove(slot.getItem().getCount());
            this.accessorySlot.set(stack);
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
