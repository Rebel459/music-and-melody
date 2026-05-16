package net.rebel459.legacies_and_legends.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Weapon;
import net.rebel459.legacies_and_legends.registry.LaLToolMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AxeItem.class)
public class AxeItemMixin {

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/Item$Properties;axe(Lnet/minecraft/world/item/ToolMaterial;FF)Lnet/minecraft/world/item/Item$Properties;"
            )
    )
    private static Item.Properties modifyBlockingSecondsIfCleavingBattleAxe(
            Item.Properties original,
            @Local(argsOnly = true) ToolMaterial material
    ) {
        if (material == LaLToolMaterial.CLEAVING) original = original.component(DataComponents.WEAPON, new Weapon(1, 10F));
        return original;
    }
}
