package net.rebel459.legacies_and_legends.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IBlockExtension.class)
public interface IBlockExtensionMixin {

    @Inject(method = "getFriction", at = @At(value = "HEAD", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"), cancellable = true)
    default void getFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity, CallbackInfoReturnable<Float> cir) {
        if (state.getBlock() instanceof WandPlatformBlock) cir.setReturnValue(WandPlatformBlock.getFriction(state));
    }
}