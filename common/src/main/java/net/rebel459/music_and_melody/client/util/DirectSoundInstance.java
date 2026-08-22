package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.rebel459.music_and_melody.MusicAndMelody;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class DirectSoundInstance extends AbstractSoundInstance {

	private final WeighedSoundEvents soundEvent;
	private final Type type;

	public static DirectSoundInstance createTracksOnly(SafeIdentifier id, float volume, boolean loop) {
		return create(id, volume, loop, Type.TRACKS);
	}

	public static DirectSoundInstance createPoolsOnly(SafeIdentifier id, float volume, boolean loop) {
		return create(id, volume, loop, Type.POOLS);
	}

	private static DirectSoundInstance create(SafeIdentifier id, float volume, boolean loop, Type type) {
		Identifier actualId = Identifier.tryParse(id.toString());
		if (actualId == null) throw new IllegalArgumentException("Could not resolve sound '" + id + "'.");
		return new DirectSoundInstance(new ResolvedSound(actualId), SoundSource.MUSIC, volume, 1.0F, SoundInstance.createUnseededRandom(), loop, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true, type);
	}

	public DirectSoundInstance(
			SafeIdentifier location,
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
	public WeighedSoundEvents resolve(@NonNull SoundManager soundManager) {
		if (this.type == Type.TRACKS) return this.soundEvent;

		WeighedSoundEvents registered = soundManager.getSoundEvent(this.identifier);
		if (registered == null) {
			if (this.type == Type.ALL) return this.soundEvent;
			throw new IllegalArgumentException("Sound event " + this.identifier + " does not exist");
		}

		this.sound = registered.getSound(this.random);
		return registered;
	}

	public void setLooping(boolean looping) {
		this.looping = looping;
	}

	private static ResolvedSound resolveSound(SafeIdentifier location) {
		return CustomAlbums.file(location)
				.or(() -> SafeMusicHelper.resolve(location))
				.map(path -> createDirectFileSound(location, path))
				.orElseGet(() -> {
					Identifier id = Identifier.tryParse(location.toString());

					if (id != null) {
						return new ResolvedSound(id);
					}

					throw new IllegalStateException("Could not resolve sound '" + location + "'.");
				});
	}

	private static ResolvedSound createDirectFileSound(SafeIdentifier originalLocation, Path source) {
		String safePath = stripAudioExtension(SafeMusicHelper.sanitize(originalLocation.getPath()));

		if (safePath.isBlank()) {
			safePath = "sound";
		}

		String playablePath = "type/" + shortHash(source.toAbsolutePath().normalize().toString()) + "/" + safePath;

		Identifier playableId = Identifier.fromNamespaceAndPath(MusicAndMelody.MOD_ID, playablePath);

		Identifier soundResourceId = Identifier.fromNamespaceAndPath(
				MusicAndMelody.MOD_ID,
				// Minecraft's Sound#getPath always requests FILE sounds through
				// a synthetic .ogg resource. Keep that logical alias regardless of
				// the physical file type; DirectSoundFiles routes it to the real file
				// and SoundBufferLibraryMixin chooses the appropriate decoder.
				"sounds/" + playablePath + ".ogg"
		);

		String configName = CustomAlbums.displayName(originalLocation);
		SafeIdentifier trackName = SafeIdentifier.fromNamespaceAndPath(
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

	private static String stripAudioExtension(String value) {
		for (String extension : new String[] {".ogg", ".mp3", ".flac", ".wav"}) {
			if (value.endsWith(extension)) return value.substring(0, value.length() - extension.length());
		}
		return value;
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

	private record ResolvedSound(Identifier playableId) {}

	public enum Type {
		ALL,
		POOLS,
		TRACKS
	}
}
