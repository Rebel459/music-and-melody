package net.rebel459.music_and_melody.sound;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.unified.api.core.UnifiedPlatform;
import net.rebel459.unified.api.core.UnifiedRegistries;

public class MaMSounds {

	private static final UnifiedRegistries.SoundEvents SOUNDS = UnifiedRegistries.SoundEvents.create(MusicAndMelody.MOD_ID);

	public static Holder<SoundEvent> EMPTY = SOUNDS.register("empty").holder();

	public static void init() {
		if (!UnifiedPlatform.isModLoaded("drops_backport")) {
			UnifiedRegistries.SoundEvents.create("minecraft").register("music_disc.bounce");
		}
	}
}
