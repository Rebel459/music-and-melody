package net.rebel459.music_and_melody.client.util;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DirectSoundFiles {

	private static final Map<ResourceLocation, Path> FILES = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, SafeIdentifier> DISPLAY_KEYS = new ConcurrentHashMap<>();

	private DirectSoundFiles() {}

	public static void register(ResourceLocation soundResourceId, ResourceLocation playableId, SafeIdentifier name, Path path) {
		FILES.put(soundResourceId, path.toAbsolutePath().normalize());
		DISPLAY_KEYS.put(playableId, name);
	}

	public static Optional<Path> get(ResourceLocation soundResourceId) {
		return Optional.ofNullable(FILES.get(soundResourceId));
	}

	public static Optional<String> getName(ResourceLocation soundResourceId) {
		SafeIdentifier id = DISPLAY_KEYS.get(soundResourceId);
		if (id == null) return Optional.empty();
		return Optional.of(id.getPath());
	}

	public static boolean contains(ResourceLocation soundResourceId) {
		return FILES.containsKey(soundResourceId);
	}
}