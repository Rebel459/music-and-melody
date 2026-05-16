package net.rebel459.legacies_and_legends.mixin.block;

import net.rebel459.legacies_and_legends.registry.LaLMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(method = "entityInside", at = @At(value = "TAIL"))
    private void frozenInPowderSnow(BlockState blockState, Level level, BlockPos blockPos, Entity entity, InsideBlockEffectApplier insideBlockEffectApplier, boolean bl, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.canFreeze()) {
            livingEntity.addEffect(new MobEffectInstance(LaLMobEffects.FREEZING));
        }
    }
}
