package net.rebel459.music_and_melody.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(JukeboxPlayable.class)
public abstract class JukeboxPlayableMixin {

    @WrapOperation(
            method = "tryInsertIntoJukebox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;consumeAndReturn(ILnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack trackDiscUses(ItemStack stack, int amount, LivingEntity owner, Operation<ItemStack> original) {
        if (MaMServerConfig.get().count_disc_uses && owner instanceof Player player) player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        return original.call(stack, amount, owner);
    }
}
