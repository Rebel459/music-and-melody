package net.rebel459.legacies_and_legends.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Optional;

public interface PlatformInterface {
    void lal$setLastPlatformPos(Level level, BlockPos pos);
    void lal$eraseLastPlatformPos();
    Optional<GlobalPos> lal$getLastPlatformPos();
    boolean getPlatformSummoned();
    void setPlatformSummoned(boolean summoned);
    HashMap<BlockPos, BlockState> getOldStates();
    void setOldStates(HashMap<BlockPos, BlockState> states);
    HashMap<BlockPos, BlockState> getPlatformStates();
    void setPlatformStates(HashMap<BlockPos, BlockState> states);
}
