package net.rebel459.music_and_melody.sound;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.rebel459.music_and_melody.config.MaMConfig;
import net.rebel459.unified.platform.UnifiedHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MaMBiomeMusic {

	public static void init() {
		List<MaMConfig.BiomeMusic> tagEntries = new ArrayList<>();
		List<MaMConfig.BiomeMusic> biomeEntries = new ArrayList<>();
		MaMConfig.get().server.biome_music.forEach(entry -> {
			if (entry.key.contains("#")) tagEntries.add(entry);
			else biomeEntries.add(entry);
		});
		tagEntries.forEach(entry -> {
			Identifier id = Identifier.parse(entry.key.substring(1));
			Identifier pool = Identifier.parse(entry.pool);
			UnifiedHelpers.BIOME_MODIFICATIONS.register(TagKey.create(Registries.BIOME, id), context -> {
				Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.get(pool);
				if (sound.isEmpty()) {
                    LogUtils.getLogger().warn("Unregistered sound event specified: {}", pool);
					return;
				}
				context.getEnvironmentAttributes().set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(sound.get()));
			});
		});
		biomeEntries.forEach(entry -> {
			Identifier id = Identifier.parse(entry.key);
			Identifier pool = Identifier.parse(entry.pool);
			UnifiedHelpers.BIOME_MODIFICATIONS.register(ResourceKey.create(Registries.BIOME, id), context -> {
				Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.get(pool);
				if (sound.isEmpty()) {
					LogUtils.getLogger().warn("Unregistered sound event specified: {}", pool);
					return;
				}
				context.getEnvironmentAttributes().set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(sound.get()));
			});
		});
	}
}
