package net.rebel459.legacies_and_legends.block;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.util.Gem;

public class WandPlatformTintSource implements BlockTintSource {

    @Override
    public int color(BlockState blockState) {
        if (!(blockState.getBlock() instanceof WandPlatformBlock)) return 0xFFFFFFFF;
        if (blockState.getValue(WandPlatformBlock.PRIMARY_MATERIAL) == Gem.EMPTY) return blockState.getValue(WandPlatformBlock.SECONDARY_MATERIAL).tint();
        return blockState.getValue(WandPlatformBlock.PRIMARY_MATERIAL).tint();
    }
}
