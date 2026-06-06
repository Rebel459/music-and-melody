package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class DirectSoundInstance extends AbstractSoundInstance {

	private final WeighedSoundEvents soundEvent;
	private final Type type;

	public static DirectSoundInstance createTracksOnly(SafeLocation id, float volume, boolean loop) {
		return create(id, volume, loop, Type.TRACKS);
	}

	public static DirectSoundInstance createPoolsOnly(SafeLocation id, float volume, boolean loop) {
		return create(id, volume, loop, Type.POOLS);
	}

	private static DirectSoundInstance create(SafeLocation id, float volume, boolean loop, Type type) {
		ResourceLocation actualId = ResourceLocation.tryParse(id.toString());
		if (actualId == null) throw new IllegalArgumentException("Could not resolve sound '" + id + "'.");
		return new DirectSoundInstance(new ResolvedSound(actualId), SoundSource.MUSIC, volume, 1.0F, SoundInstance.createUnseededRandom(), loop, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true, type);
	}

	public DirectSoundInstance(
			SafeLocation location,
			SoundSource source,
			float volume,
			float pitch,
			RandomSource random,
			boolean looping,
			int delay,
			SoundInstance.Attenuation attenuation,
			double x,
			double y,
			double z,
			boolean relative
	) {
		this(
				resolveSound(location),
				source,
				volume,
				pitch,
				random,
				looping,
				delay,
				attenuation,
				x,
				y,
				z,
				relative,
				Type.ALL
		);
	}

	private DirectSoundInstance(
			ResolvedSound resolved,
			SoundSource source,
			float volume,
			float pitch,
			RandomSource random,
			boolean looping,
			int delay,
			SoundInstance.Attenuation attenuation,
			double x,
			double y,
			double z,
			boolean relative,
			Type type
	) {
		super(resolved.playableId(), source, random);

		this.volume = volume;
		this.pitch = pitch;
		this.x = x;
		this.y = y;
		this.z = z;
		this.looping = looping;
		this.delay = delay;
		this.attenuation = attenuation;
		this.relative = relative;
		this.type = type;

		if (type == Type.POOLS) {
			this.soundEvent = null;
		}
		else {
			this.sound = new Sound(
					resolved.playableId(),
					ConstantFloat.of(1.0F),
					ConstantFloat.of(1.0F),
					1,
					Sound.Type.FILE,
					true,
					false,
					16
			);
			this.soundEvent = new WeighedSoundEvents(resolved.playableId(), null);
			this.soundEvent.addSound(this.sound);
		}
	}

	@Override
	public WeighedSoundEvents resolve(@NotNull SoundManager soundManager) {
		if (this.type == Type.TRACKS) return this.soundEvent;

		WeighedSoundEvents registered = soundManager.getSoundEvent(this.location);
		if (registered == null) {
			if (this.type == Type.ALL) return this.soundEvent;
			throw new IllegalArgumentException("Sound event " + this.location + " does not exist");
		}

		this.sound = registered.getSound(this.random);
		return registered;
	}

	public void setLooping(boolean looping) {
		this.looping = looping;
	}

	private static ResolvedSound resolveSound(SafeLocation location) {
		return ConfigAlbum.file(location)
				.or(() -> SafeMusicHelper.resolve(location))
				.map(path -> createDirectFileSound(location, path))
				.orElseGet(() -> {
					ResourceLocation id = ResourceLocation.tryParse(location.toString());

					if (id != null) {
						return new ResolvedSound(id);
					}

					throw new IllegalStateException("Could not resolve sound '" + location + "'.");
				});
	}

	private static ResolvedSound createDirectFileSound(SafeLocation originalLocation, Path source) {
		String safePath = SafeMusicHelper.sanitize(originalLocation.getPath());

		if (safePath.isBlank()) {
			safePath = "sound";
		}

		String playablePath = "type/" + shortHash(source.toAbsolutePath().normalize().toString()) + "/" + safePath;

		ResourceLocation playableId = ResourceLocation.fromNamespaceAndPath(MusicAndMelody.MOD_ID, playablePath);

		ResourceLocation soundResourceId = ResourceLocation.fromNamespaceAndPath(
				MusicAndMelody.MOD_ID,
				"sounds/" + playablePath + ".ogg"
		);

		String configName = ConfigAlbum.displayName(originalLocation);
		SafeLocation trackName = SafeLocation.fromNamespaceAndPath(
				originalLocation.getNamespace(),
				configName != null ? configName : fileNameOnly(originalLocation.getPath())
		);
		DirectSoundFiles.register(soundResourceId, playableId, originalLocation, trackName, source);

		return new ResolvedSound(playableId);
	}

	private static String fileNameOnly(String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return slash == -1 ? path : path.substring(slash + 1);
	}

	private static String shortHash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < 6; i++) {
				builder.append(String.format(Locale.ROOT, "%02x", hash[i]));
			}

			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-1 is unavailable.", exception);
		}
	}

	private record ResolvedSound(ResourceLocation playableId) {}

	public enum Type {
		ALL,
		POOLS,
		TRACKS
	}
}
