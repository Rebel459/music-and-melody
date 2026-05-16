package net.rebel459.legacies_and_legends.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Leashable.class)
public interface LeashableMixin {

    @Inject(method = "angularFriction", at = @At(value = "TAIL"))
    private static void getFriction(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (!entity.onGround()) return;
        BlockState state = entity.level().getBlockState(entity.getBlockPosBelowThatAffectsMyMovement());
        if (state.getBlock() instanceof WandPlatformBlock) cir.setReturnValue(WandPlatformBlock.getFriction(state) * 0.91F);
    }
}