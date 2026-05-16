package net.rebel459.legacies_and_legends.item;

import net.rebel459.legacies_and_legends.registry.LaLToolMaterial;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CleavingBattleAxeItem extends AxeItem {

    public CleavingBattleAxeItem(Properties properties) {
        super(LaLToolMaterial.CLEAVING, 5F, -3F, properties);
    }

    @Override
    public void postHurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), attacker);
    }
}