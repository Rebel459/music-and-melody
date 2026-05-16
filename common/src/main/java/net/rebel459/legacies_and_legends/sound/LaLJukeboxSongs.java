package net.rebel459.legacies_and_legends.sound;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.item.JukeboxSong;
import org.jetbrains.annotations.NotNull;

public class LaLJukeboxSongs {
	public static final ResourceKey<JukeboxSong> SVALL = create("svall");
	public static final ResourceKey<JukeboxSong> CASTLES = create("castles");
	public static final ResourceKey<JukeboxSong> TASWELL = create("taswell");
	public static final ResourceKey<JukeboxSong> SHULKER = create("shulker");
	public static final ResourceKey<JukeboxSong> TUNDRA = create("tundra");
	public static final ResourceKey<JukeboxSong> FAR_LANDS = create("far_lands");
	public static final ResourceKey<JukeboxSong> INFINITE_SPOOKY_AMETHYST = create("infinite_spooky_amethyst");
	public static final ResourceKey<JukeboxSong> MUSIC_DISC_113 = create("113");
	public static final ResourceKey<JukeboxSong> GRAVEL = create("gravel");

	public static void init() {}

	private static @NotNull ResourceKey<JukeboxSong> create(String path) {
		return ResourceKey.create(Registries.JUKEBOX_SONG, LaLConstants.id(path));
	}

	private static void register(
		@NotNull BootstrapContext<JukeboxSong> context,
		ResourceKey<JukeboxSong> registryKey,
		Holder<SoundEvent> soundEvent,
		int lengthInSeconds,
		int comparatorOutput
	) {
		context.register(
			registryKey,
			new JukeboxSong(soundEvent, Component.translatable(Util.makeDescriptionId("jukebox_song", registryKey.identifier())), (float)lengthInSeconds, comparatorOutput)
		);
	}

	public static void bootstrap(BootstrapContext<JukeboxSong> context) {
		register(context, SVALL, LaLSounds.MUSIC_DISC_SVALL, 244, 6);
		register(context, CASTLES, LaLSounds.MUSIC_DISC_SVALL, 260, 9);
		register(context, TASWELL, LaLSounds.MUSIC_DISC_TASWELL, 600, 7);
		register(context, SHULKER, LaLSounds.MUSIC_DISC_SHULKER, 128, 13);
		register(context, TUNDRA, LaLSounds.MUSIC_DISC_TUNDRA, 118, 5);
		register(context, FAR_LANDS, LaLSounds.MUSIC_DISC_FAR_LANDS, 260, 15);
		register(context, INFINITE_SPOOKY_AMETHYST, LaLSounds.MUSIC_DISC_INFINITE_SPOOKY_AMETHYST, 292, 11);
		register(context, MUSIC_DISC_113, LaLSounds.MUSIC_DISC_113, 197, 8);
		register(context, GRAVEL, LaLSounds.MUSIC_DISC_GRAVEL, 152, 4);
	}
}
