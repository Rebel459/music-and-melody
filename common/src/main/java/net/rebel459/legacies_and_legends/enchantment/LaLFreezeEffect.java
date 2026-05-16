package net.rebel459.legacies_and_legends.enchantment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record LaLFreezeEffect(LevelBasedValue duration) implements EnchantmentEntityEffect {

    public static final MapCodec<LaLFreezeEffect> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance
                    .group(LevelBasedValue.CODEC.fieldOf("duration").forGetter(effect -> effect.duration))
                    .apply(instance, LaLFreezeEffect::new)
    );

    @Override
    public void apply(ServerLevel world, int level, EnchantedItemInUse context, Entity user, Vec3 pos) {
        user.setTicksFrozen((int) Math.ceil(duration.calculate(level)));

        world.sendParticles(ParticleTypes.SNOWFLAKE, user.getX(), user.getRandomY(), user.getZ(), 10, 0, -1, 0, 0.5);
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}