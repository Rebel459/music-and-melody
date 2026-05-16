package net.rebel459.legacies_and_legends.mixin.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @WrapOperation(method = "fill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V", ordinal = 1))
    private void handleAccessoryQuickMove(Container instance, int i, ItemStack stack, Operation<Void> original) {
        if (stack.is(LaLItemTags.ACCESSORIES)) AccessoryHelper.setupRandomComponents(stack, RandomSource.create());
        original.call(instance, i, stack);
    }
}
