package net.rebel459.legacies_and_legends.mixin.integration.combat_reborn;

import net.rebel459.combat_reborn.util.QuiverHelper;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QuiverHelper.class)
public abstract class QuiverHelperMixin {

    @Inject(at = @At("TAIL"), method = "getStack", cancellable = true)
    private static void ringOfArchery(Player player, CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue() != null || !AccessoryHelper.isSlotFilled(player)) return;
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (stack.is(LaLItems.RING_OF_ARCHERY.get())) cir.setReturnValue(stack);
    }

    @Inject(at = @At("TAIL"), method = "getAccuracy(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)F", cancellable = true)
    private static void ringOfArcheryAccuracy(ItemStack stack, Player player, CallbackInfoReturnable<Float> cir) {
        if (player == null || !AccessoryHelper.isSlotFilled(player)) return;
        if (AccessoryHelper.getAccessory(player).is(LaLItems.RING_OF_ARCHERY.get())) cir.setReturnValue(cir.getReturnValue() + 4);
    }

    @Inject(at = @At("TAIL"), method = "postProjectileEvent")
    private static void ringOfArcheryDamage(Player player, CallbackInfo ci) {
        if (!AccessoryHelper.isSlotFilled(player)) return;
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (stack.is(LaLItems.RING_OF_ARCHERY.get())) AccessoryHelper.damageAccessory(player, stack);
    }
}
