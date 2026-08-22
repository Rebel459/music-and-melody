package net.rebel459.music_and_melody.client.util;

import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DirectSoundFiles {

	private static final Map<Identifier, Path> FILES = new ConcurrentHashMap<>();

	// playable id / sound resource id -> display safe id
	private static final Map<Identifier, SafeIdentifier> DISPLAY_KEYS = new ConcurrentHashMap<>();

	// safe/menu/display aliases -> playable id
	private static final Map<SafeIdentifier, Identifier> PLAYABLE_IDS = new ConcurrentHashMap<>();

	// identifier aliases -> playable id
	private static final Map<Identifier, Identifier> IDENTIFIER_PLAYABLE_IDS = new ConcurrentHashMap<>();

	private DirectSoundFiles() {}

	public static void register(
			Identifier soundResourceId,
			Identifier playableId,
			SafeIdentifier originalLocation,
			SafeIdentifier displayName,
			Path path
	) {
		FILES.put(soundResourceId, path.toAbsolutePath().normalize());
		FILES.put(playableId, path.toAbsolutePath().normalize());

		DISPLAY_KEYS.put(playableId, displayName);
		DISPLAY_KEYS.put(soundResourceId, displayName);

		IDENTIFIER_PLAYABLE_IDS.put(playableId, playableId);
		IDENTIFIER_PLAYABLE_IDS.put(soundResourceId, playableId);

		registerAlias(originalLocation, playableId);
		registerAlias(displayName, playableId);
		registerAlias(SafeIdentifier.convert(playableId), playableId);
		registerAlias(SafeIdentifier.convert(soundResourceId), playableId);
	}

	private static void registerAlias(SafeIdentifier alias, Identifier playableId) {
		if (alias != null) {
			PLAYABLE_IDS.put(alias, playableId);
		}
	}

	public static Optional<Path> get(Identifier soundResourceId) {
		return Optional.ofNullable(FILES.get(soundResourceId));
	}

	public static Optional<String> getName(Identifier id) {
		SafeIdentifier display = DISPLAY_KEYS.get(id);
		return display == null ? Optional.empty() : Optional.of(display.getPath());
	}

	public static Optional<Identifier> getPlayableId(SafeIdentifier id) {
		Identifier direct = PLAYABLE_IDS.get(id);
		if (direct != null) return Optional.of(direct);

		Identifier parsed = Identifier.tryParse(id.toString());
		if (parsed == null) return Optional.empty();

		return Optional.ofNullable(IDENTIFIER_PLAYABLE_IDS.get(parsed));
	}

	public static Identifier playableIdOrSelf(SafeIdentifier id) {
		Identifier playable = PLAYABLE_IDS.get(id);
		if (playable != null) return playable;

		Identifier parsed = Identifier.tryParse(id.toString());
		if (parsed != null) {
			Identifier fromIdentifier = IDENTIFIER_PLAYABLE_IDS.get(parsed);
			if (fromIdentifier != null) return fromIdentifier;
			return parsed;
		}

		return id.getId();
	}

	public static boolean samePlayable(SafeIdentifier a, SafeIdentifier b) {
		if (a == null || b == null) return false;
		return playableIdOrSelf(a).equals(playableIdOrSelf(b));
	}

	public static boolean sameStreamResource(Identifier a, Identifier b) {
		if (a == null || b == null) return false;
		if (a.equals(b)) return true;

		Identifier aPlayable = IDENTIFIER_PLAYABLE_IDS.getOrDefault(a, a);
		Identifier bPlayable = IDENTIFIER_PLAYABLE_IDS.getOrDefault(b, b);
		if (aPlayable.equals(bPlayable)) return true;

		if (!a.getNamespace().equals(b.getNamespace())) return false;
		return normalizedStreamPath(a.getPath()).equals(normalizedStreamPath(b.getPath()));
	}

	private static String normalizedStreamPath(String path) {
		String normalized = path.startsWith("sounds/") ? path.substring("sounds/".length()) : path;
		for (String extension : new String[] {".ogg", ".mp3", ".flac", ".wav"}) {
			if (normalized.endsWith(extension)) return normalized.substring(0, normalized.length() - extension.length());
		}
		return normalized;
	}

	public static boolean contains(Identifier soundResourceId) {
		return FILES.containsKey(soundResourceId);
	}
}
