package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public final class SafeMusicHelper {

	private SafeMusicHelper() {
	}

	public static Optional<Path> resolve(SafeIdentifier id) {
		String wantedPath = normalize(id.getPath());
		String wantedFile = fileName(wantedPath);
		String wantedStem = stripOgg(wantedFile);

		return findFlatAlbumFile(wantedStem)
				.or(() -> findDirectDownloadFile(wantedPath))
				.or(() -> findRecursiveDownloadFile(wantedPath, wantedStem));
	}

	public static List<String> tracksInFolder(SafeIdentifier folder) {
		String folderPath = normalize(folder.getPath());

		try (
				Stream<String> albumTracks = flatAlbumTracks(folder.getNamespace(), folderPath);
				Stream<String> downloadTracks = recursiveDownloadTracks(folder.getNamespace(), folderPath)
		) {
			return Stream.concat(albumTracks, downloadTracks)
					.distinct()
					.sorted(Comparator.naturalOrder())
					.toList();
		}
	}

	private static Optional<Path> findFlatAlbumFile(String wantedStem) {
		Path root = albumRoot();

		if (!Files.isDirectory(root)) {
			return Optional.empty();
		}

		try (Stream<Path> stream = Files.list(root)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(SafeMusicHelper::isOgg)
					.filter(path -> sameStem(path, wantedStem))
					.findFirst();
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to scan album folder '" + root + "'.", exception);
		}
	}

	private static Optional<Path> findDirectDownloadFile(String wantedPath) {
		Path root = downloadsRoot();

		if (!Files.isDirectory(root)) {
			return Optional.empty();
		}

		Path exact = root.resolve(wantedPath).normalize();

		if (Files.isRegularFile(exact)) {
			return Optional.of(exact);
		}

		Path withOgg = root.resolve(ensureOgg(wantedPath)).normalize();

		if (Files.isRegularFile(withOgg)) {
			return Optional.of(withOgg);
		}

		return Optional.empty();
	}

	private static Optional<Path> findRecursiveDownloadFile(String wantedPath, String wantedStem) {
		Path root = downloadsRoot();

		if (!Files.isDirectory(root)) {
			return Optional.empty();
		}

		String wantedWithOgg = ensureOgg(wantedPath);

		try (Stream<Path> stream = Files.walk(root)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(SafeMusicHelper::isOgg)
					.filter(path -> {
						String relative = normalize(root.relativize(path).toString());

						return relative.equals(wantedWithOgg)
								|| relative.endsWith("/" + wantedWithOgg)
								|| sameStem(path, wantedStem);
					})
					.findFirst();
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to scan downloads folder '" + root + "'.", exception);
		}
	}

	private static Stream<String> flatAlbumTracks(String namespace, String folderPath) {
		Path root = albumRoot();

		if (!Files.isDirectory(root)) {
			return Stream.empty();
		}

		try {
			return Files.list(root)
					.filter(Files::isRegularFile)
					.filter(SafeMusicHelper::isOgg)
					.map(path -> trackId(namespace, folderPath, path));
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to scan album folder '" + root + "'.", exception);
		}
	}

	private static Stream<String> recursiveDownloadTracks(String namespace, String folderPath) {
		Path root = downloadsRoot();

		if (!Files.isDirectory(root)) {
			return Stream.empty();
		}

		try {
			return Files.walk(root)
					.filter(Files::isRegularFile)
					.filter(SafeMusicHelper::isOgg)
					.filter(path -> isInsideRequestedFolder(root, path, folderPath))
					.map(path -> trackId(namespace, folderPath, path));
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to scan downloads folder '" + root + "'.", exception);
		}
	}

	private static boolean isInsideRequestedFolder(Path root, Path file, String folderPath) {
		if (folderPath.isBlank()) {
			return true;
		}

		String relative = normalize(root.relativize(file).toString());

		return relative.startsWith(folderPath + "/")
				|| relative.contains("/" + folderPath + "/")
				|| relative.contains("/sounds/" + folderPath + "/");
	}

	private static String trackId(String namespace, String folderPath, Path file) {
		String stem = stripOgg(file.getFileName().toString());

		if (folderPath.isBlank()) {
			return namespace + ":" + stem;
		}

		return namespace + ":" + folderPath + "/" + stem;
	}

	private static boolean sameStem(Path path, String wantedStem) {
		String actualStem = stripOgg(path.getFileName().toString());

		return actualStem.equals(wantedStem)
				|| actualStem.equalsIgnoreCase(wantedStem)
				|| sanitize(actualStem).equals(sanitize(wantedStem));
	}

	private static boolean isOgg(Path path) {
		return path.getFileName()
				.toString()
				.toLowerCase(Locale.ROOT)
				.endsWith(".ogg");
	}

	public static String sanitize(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		StringBuilder builder = new StringBuilder(lower.length());

		for (int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);

			if ((c >= 'a' && c <= 'z') ||
					(c >= '0' && c <= '9') ||
					c == '_' ||
					c == '-' ||
					c == '.' ||
					c == '/') {
				builder.append(c);
			} else {
				builder.append('_');
			}
		}

		String result = builder.toString();

		while (result.contains("__")) {
			result = result.replace("__", "_");
		}

		while (result.startsWith("_")) {
			result = result.substring(1);
		}

		while (result.endsWith("_")) {
			result = result.substring(0, result.length() - 1);
		}

		return result;
	}

	public static String normalize(String value) {
		String result = stripNamespace(value).replace('\\', '/');

		while (result.startsWith("/")) {
			result = result.substring(1);
		}

		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}

		if (result.startsWith("sounds/")) {
			result = result.substring("sounds/".length());
		}

		while (result.contains("//")) {
			result = result.replace("//", "/");
		}

		return result;
	}

	private static String fileName(String path) {
		int slash = path.lastIndexOf('/');
		return slash >= 0 ? path.substring(slash + 1) : path;
	}

	private static String ensureOgg(String value) {
		return value.toLowerCase(Locale.ROOT).endsWith(".ogg") ? value : value + ".ogg";
	}

	private static String stripOgg(String value) {
		return value.toLowerCase(Locale.ROOT).endsWith(".ogg")
				? value.substring(0, value.length() - ".ogg".length())
				: value;
	}

	private static Path configRoot() {
		return Minecraft.getInstance()
				.gameDirectory
				.toPath()
				.resolve("config")
				.resolve(MusicAndMelody.MOD_ID);
	}

	private static Path albumRoot() {
		return configRoot().resolve("album");
	}

	private static Path downloadsRoot() {
		return configRoot().resolve("downloads");
	}

	private static String stripNamespace(String value) {
		int colon = value.indexOf(':');

		if (colon >= 0) {
			return value.substring(colon + 1);
		}

		return value;
	}
}