package net.rebel459.legacies_and_legends.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public class FireBlockMixin {

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;", ordinal = 0))
    private Object defaultState(BlockState state, Property property, Comparable comparable, Operation<Object> original) {
        return original.call(state.setValue(WandPlatformBlock.CANCEL_TICK, false), property, comparable);
    }

    @Inject(method = "createBlockStateDefinition", at = @At(value = "HEAD"))
    private void stateDefinition(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(WandPlatformBlock.CANCEL_TICK);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void cancelTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (state.hasProperty(WandPlatformBlock.CANCEL_TICK) && state.getValue(WandPlatformBlock.CANCEL_TICK)) ci.cancel();
    }

    @Inject(method = "canBurn", at = @At(value = "HEAD"), cancellable = true)
    private void burnState(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.hasProperty(WandPlatformBlock.CANCEL_TICK) && state.getValue(WandPlatformBlock.CANCEL_TICK)) cir.setReturnValue(false);
    }

    @Unique
    private BlockState state;

    @Inject(method = "getStateForPlacement(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "HEAD"), cancellable = true)
    private void getState(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        this.state = level.getBlockState(pos);
    }

    @Inject(method = "getStateForPlacement(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "RETURN"), cancellable = true)
    private void returnState(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (this.state.hasProperty(WandPlatformBlock.CANCEL_TICK) && this.state.getValue(WandPlatformBlock.CANCEL_TICK)) cir.setReturnValue(cir.getReturnValue().setValue(WandPlatformBlock.CANCEL_TICK, true));
    }
}
