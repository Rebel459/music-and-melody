package net.rebel459.legacies_and_legends.registry;

import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;

public class LaLConsumables {
    public static final Consumable ENCHANTED_BEETROOT = defaultFood()
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                                    new MobEffectInstance(MobEffects.REGENERATION, 100, 2)
                            )
                    )
            )
            .build();
    public static final Consumable ENCHANTED_BEETROOT_SOUP = defaultFood()
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                                    new MobEffectInstance(MobEffects.REGENERATION, 300, 4)
                            )
                    )
            )
            .build();

    public static final Consumable TABLET_OF_RECALL = Consumable.builder()
            .consumeSeconds(12.8F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                            )
                    )
            )
            .build();
    public static final Consumable TABLET_OF_HASTE = Consumable.builder()
            .consumeSeconds(1.6F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                                    new MobEffectInstance(MobEffects.HASTE, 3600, 0)
                            )
                    )
            )
            .build();
    public static final Consumable TABLET_OF_INSTABILITY = Consumable.builder()
            .consumeSeconds(1.6F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                                    new MobEffectInstance(LaLMobEffects.INSTABILITY, 3600)
                            )
                    )
            )
            .build();
    public static final Consumable TABLET_OF_WARPING = Consumable.builder()
            .consumeSeconds(1.6F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                                    new MobEffectInstance(LaLMobEffects.WARPING, 3600)
                            )
                    )
            )
            .build();
    public static final Consumable TABLET_OF_CHANNELING = Consumable.builder()
            .consumeSeconds(6.4F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                            )
                    )
            )
            .build();
    public static final Consumable TABLET_OF_DEAFENING = Consumable.builder()
            .consumeSeconds(6.4F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                            )
                    )
            )
            .build();
    public static final Consumable TABLET_OF_REVEALING = Consumable.builder()
            .consumeSeconds(3.2F)
            .animation(ItemUseAnimation.BLOCK)
            .sound(LaLSounds.TABLET_USE)
            .soundAfterConsume(LaLSounds.TABLET_BREAK)
            .hasConsumeParticles(false)
            .onConsume(
                    new ApplyStatusEffectsConsumeEffect(
                            List.of(
                            )
                    )
            )
            .build();

    public static Consumable.Builder defaultFood() {
        return Consumable.builder().consumeSeconds(1.6F).animation(ItemUseAnimation.EAT).sound(SoundEvents.GENERIC_EAT).hasConsumeParticles(true);
    }

    public static Consumable.Builder defaultDrink() {
        return Consumable.builder().consumeSeconds(1.6F).animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false);
    }
}
