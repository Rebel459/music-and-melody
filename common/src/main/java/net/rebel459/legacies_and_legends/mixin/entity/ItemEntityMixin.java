package net.rebel459.legacies_and_legends.mixin.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.registry.LaLDataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Inject(at = @At("HEAD"), method = "playerTouch")
    private void dropWand(CallbackInfo ci) {
        ItemStack stack = this.getItem();
        if (stack.getItem() instanceof WandItem) {
            WandItem.checkComponents(stack);
            WandItem.updateModel(stack, stack.get(LaLDataComponents.WAND_SLOTS.get()), true);
        }
    }
}