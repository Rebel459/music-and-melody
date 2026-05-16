package net.rebel459.legacies_and_legends.sound;

import net.minecraft.sounds.Music;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.rebel459.legacies_and_legends.LegaciesAndLegends;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.legacies_and_legends.tag.LaLBiomeTags;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.minecraft.sounds.Musics;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biomes;

public final class LaLMusic {

	public static Music SNOWY = Musics.createGameMusic(LaLSounds.SNOWY_MUSIC);
	public static Music SAVANNA = Musics.createGameMusic(LaLSounds.SAVANNA_MUSIC);
	public static Music DARK_FOREST = Musics.createGameMusic(LaLSounds.DARK_FOREST_MUSIC);
	public static Music MAIN_END_ISLAND = Musics.createGameMusic(LaLSounds.MAIN_END_ISLAND_MUSIC);

	public static Music STRONGHOLD = new Music(LaLSounds.STRONGHOLD_MUSIC, LaLConfig.get().music.structure_music_min, LaLConfig.get().music.structure_music_max, false);
	public static Music ANCIENT_CITY = new Music(LaLSounds.ANCIENT_CITY_MUSIC, LaLConfig.get().music.structure_music_min, LaLConfig.get().music.structure_music_max, false);

	public static void init() {

		if (LaLConfig.get().music.stronghold_music) UnifiedHelpers.STRUCTURE_MUSIC.add(BuiltinStructures.STRONGHOLD.identifier(), STRONGHOLD);
		if (LaLConfig.get().music.ancient_city_music) UnifiedHelpers.STRUCTURE_MUSIC.add(BuiltinStructures.ANCIENT_CITY.identifier(), ANCIENT_CITY);

		UnifiedHelpers.BIOME_MODIFICATIONS.register(LaLBiomeTags.MUSIC_SNOWY, context -> {
			if (LaLConfig.get().music.snowy_music && (!LegaciesAndLegends.isWilderWildLoaded || !LaLConfig.get().integrations.wilder_wild)) {
				context.getEnvironmentAttributes().set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SNOWY));
			}
		});

		UnifiedHelpers.BIOME_MODIFICATIONS.register(LaLBiomeTags.MUSIC_SAVANNA, context -> {
			if (LaLConfig.get().music.savanna_music) {
				context.getEnvironmentAttributes().set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SAVANNA));
			}
		});

		UnifiedHelpers.BIOME_MODIFICATIONS.register(LaLBiomeTags.MUSIC_DARK_FOREST, context -> {
			if (LaLConfig.get().music.dark_forest_music) {
				context.getEnvironmentAttributes().set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(DARK_FOREST));
			}
		});

		UnifiedHelpers.BIOME_MODIFICATIONS.register(Biomes.THE_END, context -> {
			if (LaLConfig.get().music.main_end_island_music) {
				context.getEnvironmentAttributes().set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(MAIN_END_ISLAND));
			}
		});
	}
}
