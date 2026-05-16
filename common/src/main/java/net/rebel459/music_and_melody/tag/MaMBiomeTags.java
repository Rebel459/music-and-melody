package net.rebel459.music_and_melody.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.rebel459.music_and_melody.MusicAndMelody;
import org.jetbrains.annotations.NotNull;

public class MaMBiomeTags {

    public static final TagKey<Biome> HAS_CREATIVE_MUSIC = create("has_creative_music");
    public static final TagKey<Biome> HAS_UNDER_WATER_MUSIC = create("has_under_water_music");

    @NotNull
    private static TagKey<Biome> create(@NotNull String path) {
        return TagKey.create(Registries.BIOME, MusicAndMelody.id(path));
    }

}