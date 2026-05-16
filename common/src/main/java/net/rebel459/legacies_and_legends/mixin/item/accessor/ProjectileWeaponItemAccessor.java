package net.rebel459.legacies_and_legends.mixin.item.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ProjectileWeaponItem.class)
public interface ProjectileWeaponItemAccessor {

    @Invoker("createProjectile")
    Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit);

    @Invoker("getDurabilityUse")
    int getDurabilityUse(ItemStack stack);
}
