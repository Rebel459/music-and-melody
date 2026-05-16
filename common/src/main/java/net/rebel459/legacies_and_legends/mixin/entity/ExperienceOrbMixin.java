package net.rebel459.legacies_and_legends.mixin.entity;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Inject(method = "playerTouch", at = @At(value = "HEAD"))
    private void ringOfRestoration(Player player, CallbackInfo ci) {
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (stack.is(LaLItems.RING_OF_RESTORATION.get()) && player.getHealth() < player.getMaxHealth()) {
            player.heal(1);
            AccessoryHelper.damageAccessory(player, stack);
        }
    }
}
