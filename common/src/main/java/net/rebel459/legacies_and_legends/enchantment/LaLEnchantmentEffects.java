package net.rebel459.legacies_and_legends.enchantment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.unified.platform.UnifiedRegistries;

public class LaLEnchantmentEffects {

    public static UnifiedRegistries.DeferredRegistry<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_EFFECTS = UnifiedRegistries.DeferredRegistry.create(LaLConstants.MOD_ID, BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE);
    public static UnifiedRegistries.DeferredRegistry<MapCodec<? extends EnchantmentLocationBasedEffect>> LOCATION_EFFECTS = UnifiedRegistries.DeferredRegistry.create(LaLConstants.MOD_ID, BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE);

    private static void registerEntityAndLocationBasedEffect(final String path, final MapCodec<? extends EnchantmentEntityEffect> codec) {
        ENTITY_EFFECTS.register(path, () -> codec);
        LOCATION_EFFECTS.register(path, () -> codec);
    }

    public static void init() {
        registerEntityAndLocationBasedEffect("freeze", LaLFreezeEffect.CODEC);
    }

    private LaLEnchantmentEffects() {}
}