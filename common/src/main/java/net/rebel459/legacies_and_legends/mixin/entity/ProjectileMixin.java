package net.rebel459.legacies_and_legends.mixin.entity;

import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.rebel459.legacies_and_legends.tag.LaLEntityTags;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Shadow private @Nullable Entity lastDeflectedBy;

    @Shadow
    public abstract boolean deflect(ProjectileDeflection projectileDeflection, @org.jspecify.annotations.Nullable Entity entity, @org.jspecify.annotations.Nullable EntityReference<Entity> entityReference, boolean bl);

    @Shadow
    @org.jspecify.annotations.Nullable
    protected EntityReference<Entity> owner;

    @Inject(method = "hitTargetOrDeflectSelf", at = @At(value = "HEAD"), cancellable = true)
    public void amuletOfDeflection(HitResult hitResult, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Projectile projectile = Projectile.class.cast(this);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();
        if (entity instanceof Player player && AccessoryHelper.hasAccessory(player, LaLItems.AMULET_OF_DEFLECTION.get())) {
                ProjectileDeflection projectileDeflection = ProjectileDeflection.MOMENTUM_DEFLECT;
                if (entity != this.lastDeflectedBy && this.deflect(projectileDeflection, entity, this.owner, false)) {
                    this.lastDeflectedBy = entity;
                }
                if (!projectile.is(LaLEntityTags.DAMAGELESS_PROJECTILES)) {
                    AccessoryHelper.damageAccessory(player, AccessoryHelper.getFirst(player, LaLItems.AMULET_OF_DEFLECTION.get()), 5);
                    player.playSound(LaLSounds.BOOMERANG_RETURN.get());
                }
                cir.setReturnValue(projectileDeflection);
            }
        }
    }
}
