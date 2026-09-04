package net.rebel459.music_and_melody.sound;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.UnifiedPlatform;
import net.rebel459.unified.platform.UnifiedRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MaMSounds {

	private static final UnifiedRegistries.SoundEvents SOUNDS = UnifiedRegistries.SoundEvents.create(MusicAndMelody.MOD_ID);

	public static Holder<SoundEvent> EMPTY = SOUNDS.registerForHolder("empty");

	public static void init() {
		if (!UnifiedPlatform.isModLoaded("drops_backport")) {
			UnifiedRegistries.SoundEvents.create("minecraft").registerForHolder("music_disc.bounce");
		}
	}
}
