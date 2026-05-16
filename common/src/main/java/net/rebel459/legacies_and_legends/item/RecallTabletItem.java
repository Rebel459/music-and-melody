package net.rebel459.legacies_and_legends.item;

import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jetbrains.annotations.NotNull;

public class RecallTabletItem extends Item {

    public RecallTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        player.teleport(player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
        level.playSound(null, player.blockPosition(), LaLSounds.TABLET_TELEPORT.get(), SoundSource.PLAYERS, 0.6F, 1F);

        if (player.gameMode.getGameModeForPlayer().isCreative()) return stack;
        return ItemStack.EMPTY;
    }
    
}