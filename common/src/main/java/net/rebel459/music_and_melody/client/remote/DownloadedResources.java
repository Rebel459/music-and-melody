package net.rebel459.music_and_melody.client.remote;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.rebel459.music_and_melody.MusicAndMelody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class DownloadedResources {

    private static final Path DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "downloads");
    private static final Map<Identifier, Path> RESOURCES = new HashMap<>();
    private static long lastScan = Long.MIN_VALUE;

    private DownloadedResources() {}

    public static synchronized Set<String> namespaces() {
        reload();
        Set<String> namespaces = new HashSet<>();
        RESOURCES.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        return namespaces;
    }

    public static synchronized Optional<Resource> getResource(Identifier id) {
        reload();
        Path path = RESOURCES.get(id);
        return path == null ? Optional.empty() : Optional.of(resource(path));
    }

    public static synchronized List<Resource> getResourceStack(Identifier id) {
        return getResource(id).map(List::of).orElseGet(List::of);
    }

    public static synchronized Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
        reload();
        String prefix = directory + "/";
        Map<Identifier, Resource> resources = new HashMap<>();
        RESOURCES.forEach((id, path) -> {
            if (id.getPath().startsWith(prefix) && filter.test(id)) {
                resources.put(id, resource(path));
            }
        });
        return resources;
    }

    private static Resource resource(Path path) {
        return new Resource(null, IoSupplier.create(path));
    }

    private static void reload() {
        long modified = modifiedTime(DIRECTORY);
        if (modified == lastScan) return;
        lastScan = modified;
        RESOURCES.clear();
        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException ignored) {
            return;
        }
        if (!Files.isDirectory(DIRECTORY)) return;

        try (var namespaces = Files.list(DIRECTORY)) {
            namespaces
                    .filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .forEach(DownloadedResources::scanNamespace);
        } catch (IOException ignored) {
        }
    }

    private static void scanNamespace(Path namespaceDirectory) {
        String namespace = namespaceDirectory.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!isValidNamespace(namespace)) return;
        try (var files = Files.walk(namespaceDirectory)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> addResource(namespaceDirectory, namespace, file));
        } catch (IOException ignored) {
        }
    }

    private static void addResource(Path namespaceDirectory, String namespace, Path file) {
        String path = namespaceDirectory.relativize(file).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(namespace + ":" + path);
        if (id != null) {
            RESOURCES.put(id, file);
        }
    }

    private static long modifiedTime(Path path) {
        if (!Files.exists(path)) return Long.MIN_VALUE;
        final long[] modified = {0L};
        try (var files = Files.walk(path)) {
            files.forEach(file -> {
                try {
                    modified[0] = Math.max(modified[0], Files.getLastModifiedTime(file).toMillis());
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        return modified[0];
    }

    private static boolean isValidNamespace(String namespace) {
        if (namespace.isBlank()) return false;
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            if (c != '_' && c != '-' && c != '.' && (c < 'a' || c > 'z') && (c < '0' || c > '9')) {
                return false;
            }
        }
        return true;
    }

    public static synchronized void invalidate() {
        lastScan = Long.MIN_VALUE;
        RESOURCES.clear();
    }
}
