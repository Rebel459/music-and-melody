package net.rebel459.legacies_and_legends.util;

import net.minecraft.world.level.block.state.BlockState;

public interface FallOnInterface {
    BlockState getFallOnState();
    void setFallOnState(BlockState state);
    int getBounciness();
    void setBounciness(int bounciness);
}
