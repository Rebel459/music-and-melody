package net.rebel459.legacies_and_legends.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.rebel459.legacies_and_legends.util.PlatformInterface;
import net.rebel459.unified.platform.UnifiedEvents;

public class PlayerEvents {

    public static void init(){
        UnifiedEvents.Players.onJoin(player -> {
            if (!(player instanceof PlatformInterface platform)) return;
            if (!platform.getPlatformSummoned()) return;
            ServerLevel serverLevel = player.level();

            var lastPlatformPos = platform.lal$getLastPlatformPos();
            if (lastPlatformPos.isEmpty() || !lastPlatformPos.get().dimension().equals(player.level().dimension())) return;

            for (BlockPos pos : platform.getPlatformStates().keySet()) {
                serverLevel.getBlockTicks().clearArea(new BoundingBox(pos));
            }

            for (var entry : platform.getPlatformStates().entrySet()) {
                if (!player.level().getBlockState(entry.getKey()).equals(entry.getValue())) {
                    player.level().setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
                }
            }
        });
        UnifiedEvents.Players.onRespawn((oldPlayer, newPlayer) -> {
            var level = newPlayer.level;
            var serverLevel = level.getServer().getLevel(level.dimension());
            if (serverLevel == null) return;
            if (serverLevel.getGameRules().get(GameRules.KEEP_INVENTORY)) AccessoryHelper.setAccessory(newPlayer, AccessoryHelper.getActualAccessory(oldPlayer));
        });
    }
}
