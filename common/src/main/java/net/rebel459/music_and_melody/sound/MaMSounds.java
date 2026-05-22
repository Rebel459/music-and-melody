package net.rebel459.music_and_melody.sound;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MaMSounds {

	public static HashMap<String, Holder<SoundEvent>> REGISTERED_SOUNDS = new HashMap<>();

	public static final Holder<SoundEvent> MUSIC_DISC_BOUNCE = MaMPlatform.SOUND_EVENTS.registerVanilla("music_disc.bounce");

	public static void init() {
		Set<String> sounds = new HashSet<>(MaMServerConfig.get().sound_events);
		Set<String> builtInSounds = Set.of(
				"music.empty",
				"music.common",
				"music.overworld.snowy",
				"music.overworld.dark_forest",
				"music.overworld.savanna",
				"music.end.main_island",
				"music.structure.stronghold",
				"music.structure.ancient_city",
				"music.wither",
				"music.threshold"
		);
		sounds.addAll(builtInSounds);
		for (String pool : sounds) {
			Holder<SoundEvent> sound = MaMPlatform.SOUND_EVENTS.registerForHolder(pool);
			REGISTERED_SOUNDS.put(pool, sound);
		}
	}
}
