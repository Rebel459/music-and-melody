package net.rebel459.legacies_and_legends.registry;

import net.minecraft.world.food.FoodProperties;

public class LaLFoods {
    public static final FoodProperties ENCHANTED_BEETROOT = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(1.2F)
            .alwaysEdible()
            .build();
    public static final FoodProperties ENCHANTED_BEETROOT_SOUP = new FoodProperties.Builder()
            .nutrition(9)
            .saturationModifier(1.2F)
            .alwaysEdible()
            .build();
}