package net.rebel459.music_and_melody.sound;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.platform.MaMPlatform;

public class MaMSounds {

	public static Holder<SoundEvent> EMPTY = MaMPlatform.SOUND_EVENTS.registerForHolder("music.empty");

	public static void init() {
		MaMPlatform.SOUND_EVENTS.registerVanilla("music_disc.bounce");
	}
}
