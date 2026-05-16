package net.rebel459.legacies_and_legends.mixin.entity;

import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EnderManMixin {

    @Inject(method = "isBeingStaredBy", at = @At(value = "TAIL"), cancellable = true)
    private void necklaceOfIsolation(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (AccessoryHelper.getAccessory(player).is(LaLItems.NECKLACE_OF_ISOLATION.get())) {
            cir.setReturnValue(false);
        }
    }
}
