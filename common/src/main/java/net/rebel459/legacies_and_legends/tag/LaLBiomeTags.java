package net.rebel459.legacies_and_legends.tag;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

public class LaLBiomeTags {
    public static final TagKey<Biome> MUSIC_SNOWY = bind("music/snowy");
    public static final TagKey<Biome> MUSIC_SAVANNA = bind("music/savanna");
    public static final TagKey<Biome> MUSIC_DARK_FOREST = bind("music/dark_forest");

    @NotNull
    private static TagKey<Biome> bind(@NotNull String path) {
        return TagKey.create(Registries.BIOME, LaLConstants.id(path));
    }

}