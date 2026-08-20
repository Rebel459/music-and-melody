package net.rebel459.music_and_melody.client.remote;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private static final Map<Identifier, List<Path>> RESOURCES = new HashMap<>();
    private static final Map<RemotePack.Tag, Map<Identifier, RemotePack.Key>> OWNERS = new HashMap<>();
    private static boolean dirty = true;

    private DownloadedResources() {}

    public static synchronized Set<String> namespaces() {
        reload();
        Set<String> namespaces = new HashSet<>();
        RESOURCES.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        return namespaces;
    }

    public static synchronized Optional<Resource> getResource(Identifier id) {
        reload();
        List<Path> paths = RESOURCES.get(id);
        return paths == null || paths.isEmpty()
                ? Optional.empty()
                : Optional.of(resource(paths.getLast()));
    }

    public static synchronized List<Resource> getResourceStack(Identifier id) {
        reload();
        List<Path> paths = RESOURCES.get(id);
        return paths == null ? List.of() : paths.stream().map(DownloadedResources::resource).toList();
    }

    public static synchronized Optional<RemotePack.Key> owner(Identifier contentId, RemotePack.Tag tag) {
        reload();
        return Optional.ofNullable(OWNERS.getOrDefault(tag, Map.of()).get(contentId));
    }

    public static synchronized Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
        reload();
        String prefix = directory + "/";
        Map<Identifier, Resource> resources = new HashMap<>();
        RESOURCES.forEach((id, paths) -> {
            if (id.getPath().startsWith(prefix) && filter.test(id)) {
                resources.put(id, resource(paths.getLast()));
            }
        });
        return resources;
    }

    private static Resource resource(Path path) {
        return new Resource(null, IoSupplier.create(path));
    }

    private static void reload() {
        if (!dirty) return;
        dirty = false;
        RESOURCES.clear();
        OWNERS.clear();
        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException ignored) {
            return;
        }
        if (!Files.isDirectory(DIRECTORY)) return;

        Set<String> installedNamespaces = installedNamespaces();
        try (var namespaces = Files.list(DIRECTORY)) {
            namespaces
                    .filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .filter(path -> installedNamespaces.contains(path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(DownloadedResources::scanNamespace);
        } catch (IOException ignored) {
        }

        scanPackDirectories();
    }

    private static void scanPackDirectories() {
        Set<Path> scanned = new HashSet<>();
        MaMDataConfig.Remote remote = MaMDataConfig.get().remote;
        if (remote == null || remote.downloads == null) return;
        for (MaMDataConfig.DownloadedPack pack : remote.downloads) {
            if (pack == null) continue;
            Identifier id = Identifier.tryParse(pack.id);
            if (id == null) continue;
            RemotePack.Key key = new RemotePack.Key(id);
            Path directory = RemoteContentManager.packDirectory(key);
            if (scanned.add(directory)) scanPack(directory, key);
        }
    }

    private static Set<String> installedNamespaces() {
        Set<String> namespaces = new HashSet<>();
        MaMDataConfig.Remote remote = MaMDataConfig.get().remote;
        if (remote == null || remote.downloads == null) return namespaces;
        for (MaMDataConfig.DownloadedPack pack : remote.downloads) {
            if (pack == null) continue;
            Identifier id = Identifier.tryParse(pack.id);
            if (id != null) namespaces.add(id.getNamespace().toLowerCase(Locale.ROOT));
        }
        return namespaces;
    }

    private static void scanPack(Path packDirectory, RemotePack.Key key) {
        if (!Files.isDirectory(packDirectory)) return;
        try (var namespaces = Files.list(packDirectory)) {
            namespaces.filter(Files::isDirectory)
                    .forEach(namespace -> scanNamespace(namespace, key));
        } catch (IOException ignored) {
        }
    }

    private static void scanNamespace(Path namespaceDirectory) {
        scanNamespace(namespaceDirectory, null);
    }

    private static void scanNamespace(Path namespaceDirectory, RemotePack.Key owner) {
        String namespace = namespaceDirectory.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!isValidNamespace(namespace)) return;
        try (var files = Files.walk(namespaceDirectory)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> addResource(namespaceDirectory, namespace, file, owner));
        } catch (IOException ignored) {
        }
    }

    private static void addResource(Path namespaceDirectory, String namespace, Path file, RemotePack.Key owner) {
        String path = namespaceDirectory.relativize(file).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(namespace + ":" + path);
        if (id != null) {
            RESOURCES.computeIfAbsent(id, ignored -> new ArrayList<>()).add(file);
            if (owner != null) recordOwner(namespace, path, owner);
        }
    }

    private static void recordOwner(String namespace, String resourcePath, RemotePack.Key owner) {
        for (RemotePack.Tag tag : RemotePack.Tag.values()) {
            String directory = tag.name().toLowerCase(Locale.ROOT) + "s/";
            if (!resourcePath.startsWith(directory) || !resourcePath.endsWith(".json")) continue;
            String path = resourcePath.substring(directory.length(), resourcePath.length() - 5);
            Identifier contentId = Identifier.tryParse(namespace + ":" + path);
            if (contentId != null) OWNERS.computeIfAbsent(tag, ignored -> new HashMap<>()).put(contentId, owner);
        }
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
        dirty = true;
        RESOURCES.clear();
        OWNERS.clear();
    }
}
