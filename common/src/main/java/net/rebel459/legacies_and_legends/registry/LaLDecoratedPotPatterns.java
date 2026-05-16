package net.rebel459.legacies_and_legends.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.rebel459.legacies_and_legends.LaLConstants;
import org.jetbrains.annotations.Nullable;

public final class LaLDecoratedPotPatterns {

    public static final ResourceKey<DecoratedPotPattern> DUSK = create("dusk");
    public static final ResourceKey<DecoratedPotPattern> HARVEST = create("harvest");
    public static final ResourceKey<DecoratedPotPattern> VERDANT = create("verdant");
    public static final ResourceKey<DecoratedPotPattern> FORAGER = create("forager");

    private LaLDecoratedPotPatterns() {}

    public static void bootstrap(Registry<DecoratedPotPattern> registry) {
        register(registry, DUSK, "dusk");
        register(registry, HARVEST, "harvest");
        register(registry, VERDANT, "verdant");
        register(registry, FORAGER, "forager");
    }

    public static @Nullable ResourceKey<DecoratedPotPattern> fromItem(Item item) {
        if (item == LaLItems.DUSK_POTTERY_SHERD.get()) {
            return DUSK;
        }
        if (item == LaLItems.HARVEST_POTTERY_SHERD.get()) {
            return HARVEST;
        }
        if (item == LaLItems.VERDANT_POTTERY_SHERD.get()) {
            return VERDANT;
        }
        if (item == LaLItems.FORAGER_POTTERY_SHERD.get()) {
            return FORAGER;
        }
        return null;
    }

    private static ResourceKey<DecoratedPotPattern> create(String name) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERN, LaLConstants.id(name + "_pottery_pattern"));
    }

    private static void register(Registry<DecoratedPotPattern> registry, ResourceKey<DecoratedPotPattern> key, String name) {
        Registry.register(registry, key, new DecoratedPotPattern(LaLConstants.id(name + "_pottery_pattern")));
    }
}
