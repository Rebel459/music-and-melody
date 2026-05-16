package net.rebel459.legacies_and_legends.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {

    @Unique
    private BlockState frictionState;

    @WrapOperation(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    public Block getBlock(BlockState state, Operation<Block> original) {
        this.frictionState = state;
        return original.call(state);
    }
    @WrapOperation(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    public float getFriction(Block block, Operation<Float> original) {
        if (block instanceof WandPlatformBlock) return WandPlatformBlock.getFriction(this.frictionState);
        else return original.call(block);
    }
}
