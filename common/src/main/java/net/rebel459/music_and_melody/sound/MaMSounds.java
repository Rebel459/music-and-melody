package net.rebel459.music_and_melody.sound;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.MaMConfig;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.util.registry.Supplied;

import java.util.ArrayList;
import java.util.List;

public class MaMSounds {
	
	public static UnifiedRegistries.SoundEvents SOUNDS = UnifiedRegistries.SoundEvents.create(MusicAndMelody.MOD_ID);

	public static final Holder<SoundEvent> MUSIC_EMPTY = SOUNDS.registerForHolder("music.empty");
	public static final Supplied<SoundEvent> MUSIC_COMMON = SOUNDS.register("music.common");
	public static final Holder<SoundEvent> MUSIC_WITHER = SOUNDS.registerForHolder("music.wither");
	public static final Holder<SoundEvent> MUSIC_THRESHOLD = SOUNDS.registerForHolder("music.threshold");

	public static void init() {
        List<String> pools = new ArrayList<>();
		for (MaMConfig.BiomeMusic entry : MaMConfig.get().server.biome_music) {
            pools.add(entry.pool);
		}
		for (MaMConfig.StructureMusic entry : MaMConfig.get().server.structure_music) {
			pools.add(entry.pool);
		}
		for (String pool : pools) {
			Identifier id = Identifier.parse(pool);
			if (!id.getNamespace().equals(MusicAndMelody.MOD_ID)) continue;
			SOUNDS.registerForHolder(id.getPath());
		}
	}
}
