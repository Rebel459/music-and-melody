package net.rebel459.music_and_melody.sound;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.UnifiedHelpers;

import java.util.Optional;

public final class MaMStructureMusic {

	public static Music createStructureMusic(Holder<SoundEvent> music) {
		return new Music(music, MaMServerConfig.get().structure_music_min, MaMServerConfig.get().structure_music_max, false);
	}

	public static void init() {
		MaMServerConfig.get().structure_music.forEach(entry -> {
			Identifier id = Identifier.parse(entry.id);
			Identifier pool = Identifier.parse(entry.pool);
			Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.get(pool);
			if (sound.isEmpty()) {
				LogUtils.getLogger().warn("Unregistered sound event specified: {}", pool);
				return;
			}
			UnifiedHelpers.STRUCTURE_MUSIC.add(id, createStructureMusic(sound.get()));
		});
	}
}
