package net.rebel459.legacies_and_legends.mixin.entity;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.rebel459.legacies_and_legends.entity.GlowStickProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {

    @Inject(method = "applyInertia", at = @At("HEAD"), cancellable = true)
    private void glowStickInertia(CallbackInfo ci) {
        ThrowableProjectile projectile = ThrowableProjectile.class.cast(this);
        if (projectile instanceof GlowStickProjectile) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.99D));
        }
        ci.cancel();
    }
}
