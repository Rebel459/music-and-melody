package net.rebel459.music_and_melody.mixin.server;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.rebel459.music_and_melody.config.MaMConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxPlayable.class)
public abstract class JukeboxPlayableMixin {

    @Inject(
            method = "tryInsertIntoJukebox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/resources/Identifier;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void trackDiscUses(Level level, BlockPos pos, ItemStack toInsert, Player player, CallbackInfoReturnable<?> cir) {
        if (MaMConfig.get().server.count_disc_uses) player.awardStat(Stats.ITEM_USED.get(toInsert.getItem()));
    }
}
