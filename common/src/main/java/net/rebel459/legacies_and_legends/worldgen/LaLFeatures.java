package net.rebel459.legacies_and_legends.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.unified.platform.UnifiedHelpers;

import static net.minecraft.core.registries.Registries.PLACED_FEATURE;

public class LaLFeatures {

    public static final ResourceKey<PlacedFeature> SAPPHIRE_ORE = ResourceKey.create(PLACED_FEATURE, LaLConstants.id("ore_sapphire"));
    public static final ResourceKey<PlacedFeature> SAPPHIRE_ORE_DEEP = ResourceKey.create(PLACED_FEATURE, LaLConstants.id("ore_sapphire_deep"));

    public static void init() {
        if (LaLConfig.get().magic.sapphire) {
            UnifiedHelpers.BIOME_MODIFICATIONS.register(BiomeTags.IS_OVERWORLD, context -> {
                context.getFeatures().addFeature(SAPPHIRE_ORE, GenerationStep.Decoration.UNDERGROUND_ORES);
                context.getFeatures().addFeature(SAPPHIRE_ORE_DEEP, GenerationStep.Decoration.UNDERGROUND_ORES);
            });
        }
    }
}