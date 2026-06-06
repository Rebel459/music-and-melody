package net.rebel459.music_and_melody.sound;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MaMSounds {

	public static Holder<SoundEvent> EMPTY = MaMPlatform.SOUND_EVENTS.registerForHolder("music.empty");

	public static void init() {
		MaMPlatform.SOUND_EVENTS.registerVanilla("music_disc.bounce");
	}
}
