package net.rebel459.legacies_and_legends.mixin.effect;

import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectUtil.class)
public abstract class MobEffectUtilMixin {

    @Inject(at = @At("TAIL"), method = "hasDigSpeed", cancellable = true)
    private static void hasTurtleHelmet(LivingEntity livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!LaLConfig.get().misc.improved_turtle_shell) return;
        cir.setReturnValue(cir.getReturnValue() || (livingEntity instanceof Player player && player.isUnderWater() && player.isEquipped(Items.TURTLE_HELMET)));
    }

    @Inject(at = @At("TAIL"), method = "getDigSpeedAmplification", cancellable = true)
    private static void turtleHelmetDigSpeed(LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
        if (!LaLConfig.get().misc.improved_turtle_shell) return;
        int speed = 0;
        if (livingEntity instanceof Player player && player.isUnderWater() && player.isEquipped(Items.TURTLE_HELMET)) speed = 1;
        cir.setReturnValue(Math.max(cir.getReturnValue(), speed));
    }
}