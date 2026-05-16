package net.rebel459.legacies_and_legends.mixin.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.util.FallOnInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "playerDestroy", at = @At(value = "TAIL"))
    private void excavationRing(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (AccessoryHelper.getAccessory(player).is(LaLItems.RING_OF_EXCAVATION.get())) {
            AccessoryHelper.damageAccessory(player, stack);
        }
    }

    @Inject(method = "fallOn", at = @At(value = "HEAD"), cancellable = true)
    private void fallBounciness(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (entity instanceof FallOnInterface fallOn && fallOn.getBounciness() > 0) {
            fallOn.setBounciness(Math.max(fallOn.getBounciness() - 1, 0));
            ci.cancel();
        }
    }

    @Inject(method = "updateEntityMovementAfterFallOn", at = @At(value = "HEAD"), cancellable = true)
    private void updateBounciness(BlockGetter level, Entity entity, CallbackInfo ci) {
        if (entity instanceof FallOnInterface fallOn && fallOn.getBounciness() >= 1) {
            WandPlatformBlock.handleSlimeBounciness(entity, fallOn);
            ci.cancel();
        }
    }
}
