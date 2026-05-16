package net.rebel459.legacies_and_legends.tag;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class LaLBlockTags {

    public static final TagKey<Block> SAPPHIRE_ORES = bind("sapphire_ores");

    public static final TagKey<Block> MINEABLE_WITH_KNIFE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("farmersdelight", "mineable/knife"));

    @NotNull
    private static TagKey<Block> bind(@NotNull String path) {
        return TagKey.create(Registries.BLOCK, LaLConstants.id(path));
    }
}