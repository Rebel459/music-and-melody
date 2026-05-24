package net.rebel459.music_and_melody.client.util;

import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DirectSoundFiles {

	private static final Map<Identifier, Path> FILES = new ConcurrentHashMap<>();
	private static final Map<Identifier, SafeIdentifier> DISPLAY_KEYS = new ConcurrentHashMap<>();

	private DirectSoundFiles() {}

	public static void register(Identifier soundResourceId, Identifier playableId, SafeIdentifier name, Path path) {
		FILES.put(soundResourceId, path.toAbsolutePath().normalize());
		DISPLAY_KEYS.put(playableId, name);
	}

	public static Optional<Path> get(Identifier soundResourceId) {
		return Optional.ofNullable(FILES.get(soundResourceId));
	}

	public static Optional<String> getName(Identifier soundResourceId) {
		SafeIdentifier id = DISPLAY_KEYS.get(soundResourceId);
		if (id == null) return Optional.empty();
		return Optional.of(id.getPath());
	}

	public static boolean contains(Identifier soundResourceId) {
		return FILES.containsKey(soundResourceId);
	}
}