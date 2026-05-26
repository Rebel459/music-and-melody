package net.rebel459.music_and_melody.client.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RemoteContentManager {
    public enum State {
        REMOTE,
        DOWNLOADING,
        NEEDS_RELOAD,
        INSTALLED,
        UPDATE_AVAILABLE,
        FAILED
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Path DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "downloads");
    private static final Path MANIFEST_DIRECTORY = DIRECTORY.resolve(".manifests");
    private static final Map<ResourceLocation, State> OVERRIDE_STATES = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> DOWNLOADING = ConcurrentHashMap.newKeySet();
    private static final List<RemotePack> PACKS = new ArrayList<>();
    private static CompletableFuture<Void> refreshTask;
    private static boolean loaded;

    private RemoteContentManager() {}

    public static boolean remoteDownloadsAllowed() {
        return false;
    }

    public static synchronized void refreshIfNeeded() {
        if (!loaded && refreshTask == null) {
            refresh();
        }
    }

    public static synchronized void refresh() {
        loaded = true;
        List<String> repositories = List.copyOf(MaMClientConfig.get().remote_repositories);
        if (!MaMClientConfig.get().remote_downloads || repositories.isEmpty()) {
            PACKS.clear();
            refreshTask = null;
            return;
        }
        refreshTask = CompletableFuture.supplyAsync(() -> loadCatalogs(repositories))
                .thenAccept(packs -> {
                    synchronized (RemoteContentManager.class) {
                        PACKS.clear();
                        PACKS.addAll(packs);
                        refreshTask = null;
                    }
                })
                .exceptionally(throwable -> {
                    synchronized (RemoteContentManager.class) {
                        refreshTask = null;
                    }
                    return null;
                });
    }

    public static synchronized List<RemotePack> packs() {
        refreshIfNeeded();
        return List.copyOf(PACKS);
    }

    public static boolean isRefreshing() {
        return refreshTask != null;
    }

    public static State state(RemotePack pack) {
        if (DOWNLOADING.contains(pack.id())) return State.DOWNLOADING;
        State override = OVERRIDE_STATES.get(pack.id());
        if (override != null) return override;
        MaMDataConfig.DownloadedPack installed = installed(pack.id());
        if (installed == null) return State.REMOTE;
        if (!installed.version.equals(pack.version()) || !installed.sha256.equalsIgnoreCase(pack.sha256())) {
            return State.UPDATE_AVAILABLE;
        }
        return State.INSTALLED;
    }

    public static boolean isDownloadedAlbum(ResourceLocation id) {
        return installed(id) != null;
    }

    public static void download(RemotePack pack) {
    }

    public static void markReloaded() {
        OVERRIDE_STATES.entrySet().removeIf(entry -> entry.getValue() == State.NEEDS_RELOAD);
    }

    public static String externalDownloadUrl(RemotePack pack) {
        return pack.url();
    }

    public static void importLocal(RemotePack pack, Path zip) {
        if (!DOWNLOADING.add(pack.id())) return;
        OVERRIDE_STATES.put(pack.id(), State.DOWNLOADING);
        CompletableFuture.runAsync(() -> {
            try {
                importAndExtract(pack, zip);
                recordInstalled(pack);
                OVERRIDE_STATES.put(pack.id(), State.NEEDS_RELOAD);
            } catch (Exception exception) {
                OVERRIDE_STATES.put(pack.id(), State.FAILED);
            } finally {
                DOWNLOADING.remove(pack.id());
            }
        });
    }

    public record InstalledPack(
            Component name,
            ResourceLocation id,
            String version,
            String sha256
    ) {}

    public static synchronized List<InstalledPack> installedPacks() {
        List<InstalledPack> packs = new ArrayList<>();

        for (MaMDataConfig.DownloadedPack record : MaMDataConfig.get().albums.downloads) {
            ResourceLocation id = ResourceLocation.tryParse(record.id);
            if (id == null) continue;

            packs.add(new InstalledPack(
                    installedName(id),
                    id,
                    record.version,
                    record.sha256
            ));
        }

        packs.sort(Comparator.comparing(pack -> pack.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    public static synchronized boolean deleteInstalled(ResourceLocation id) {
        if (DOWNLOADING.contains(id)) return false;

        MaMDataConfig config = MaMDataConfig.get();
        MaMDataConfig.DownloadedPack record = installed(id);
        if (record == null) return false;

        boolean deletedFiles = deleteFromManifest(id);

        if (!deletedFiles && !hasOtherInstalledPackInNamespace(id)) {
            deleteDirectory(DIRECTORY.resolve(id.getNamespace()));
        }

        String idString = id.toString();
        config.albums.downloads.removeIf(pack -> pack.id.equals(idString));
        config.albums.disabled_albums.remove(idString);
        config.albums.favourites.remove(idString);

        if (!hasOtherInstalledPackInNamespace(id)) {
            String namespacePrefix = id.getNamespace() + ":";
            config.albums.disabled_tracks.removeIf(track -> track.startsWith(namespacePrefix));
        }

        OVERRIDE_STATES.remove(id);

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        DownloadedResources.invalidate();
        return true;
    }

    private static List<RemotePack> loadCatalogs(List<String> repositories) {
        List<RemotePack> packs = new ArrayList<>();
        Set<ResourceLocation> ids = new HashSet<>();
        for (String repository : repositories) {
            try {
                packs.addAll(loadCatalog(repository, ids));
            } catch (Exception ignored) {
            }
        }
        packs.sort(Comparator.comparing(pack -> pack.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    private static List<RemotePack> loadCatalog(String repositoryUrl, Set<ResourceLocation> ids) throws IOException, InterruptedException {
        URI catalogUri = catalogUri(repositoryUrl);
        HttpRequest request = HttpRequest.newBuilder(catalogUri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        String repositoryName = repositoryName(root, repositoryUrl);
        JsonArray array = root.getAsJsonArray("packs");
        if (array == null) return List.of();

        List<RemotePack> packs = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            RemotePack pack = parsePack(element.getAsJsonObject(), repositoryName, catalogUri);
            if (pack == null || !ids.add(pack.id())) continue;
            packs.add(pack);
        }
        return packs;
    }

    private static URI catalogUri(String repositoryUrl) {
        String value = repositoryUrl.trim();
        URI uri = URI.create(value);
        if ("github.com".equalsIgnoreCase(uri.getHost())) {
            String[] parts = uri.getPath().replaceFirst("^/", "").split("/");
            if (parts.length >= 2) {
                String owner = parts[0];
                String repo = parts[1];
                String branch = "main";
                boolean blob = false;
                StringBuilder path = new StringBuilder();
                if (parts.length >= 4 && ("tree".equals(parts[2]) || "blob".equals(parts[2]))) {
                    blob = "blob".equals(parts[2]);
                    branch = parts[3];
                    for (int i = 4; i < parts.length; i++) {
                        if (!path.isEmpty()) path.append('/');
                        path.append(parts[i]);
                    }
                }
                if (!blob && (path.isEmpty() || value.endsWith("/") || !path.toString().endsWith(".json"))) {
                    if (!path.isEmpty()) path.append('/');
                    path.append("catalog.json");
                }
                return URI.create("https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path);
            }
        }
        if (value.endsWith("/")) return uri.resolve("catalog.json");
        return uri;
    }

    private static String repositoryName(JsonObject root, String fallback) {
        JsonObject repository = root.getAsJsonObject("repository");
        if (repository != null && repository.has("name")) return repository.get("name").getAsString();
        return fallback;
    }

    private static RemotePack parsePack(JsonObject object, String repositoryName, URI catalogUri) {
        ResourceLocation id = object.has("id") ? ResourceLocation.tryParse(object.get("id").getAsString()) : null;
        if (id == null || !object.has("name") || !object.has("version") || !object.has("url") || !object.has("sha256") || !object.has("size")) {
            return null;
        }
        ResourceLocation icon = object.has("icon") ? ResourceLocation.tryParse(object.get("icon").getAsString()) : ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
        return new RemotePack(
                id,
                Component.literal(object.get("name").getAsString()),
                Component.literal(object.has("description") ? object.get("description").getAsString() : ""),
                repositoryName,
                object.get("version").getAsString(),
                catalogUri.resolve(object.get("url").getAsString()).toString(),
                object.get("sha256").getAsString().toLowerCase(Locale.ROOT),
                object.get("size").getAsLong(),
                icon == null ? ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png") : icon
        );
    }

    private static void importAndExtract(RemotePack pack, Path zip) throws IOException {
        if (!Files.isRegularFile(zip)) throw new IOException("Import file missing: " + zip);
        if (Files.size(zip) != pack.size()) throw new IOException("Imported size mismatch");
        String hash = sha256(zip);
        if (!hash.equalsIgnoreCase(pack.sha256())) throw new IOException("Imported hash mismatch");
        if (installed(pack.id()) != null) {
            deleteInstalledFiles(pack.id());
        }
        List<Path> extracted = extractZip(zip, DIRECTORY);
        writeManifest(pack.id(), extracted);
    }

    private static void deleteInstalledFiles(ResourceLocation id) {
        boolean deletedFiles = deleteFromManifest(id);

        if (!deletedFiles && !hasOtherInstalledPackInNamespace(id)) {
            deleteDirectory(DIRECTORY.resolve(id.getNamespace()));
        }

        DownloadedResources.invalidate();
    }

    private static List<Path> extractZip(Path zip, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path root = directory.toAbsolutePath().normalize();
        List<Path> extracted = new ArrayList<>();

        try (InputStream input = Files.newInputStream(zip); ZipInputStream stream = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) throw new IOException("Unsafe zip entry: " + entry.getName());

                Files.createDirectories(target.getParent());
                Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                extracted.add(root.relativize(target));
            }
        }

        return extracted;
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder builder = new StringBuilder();
            for (byte value : digest.digest()) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(exception);
        }
    }

    private static MaMDataConfig.DownloadedPack installed(ResourceLocation id) {
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().albums.downloads) {
            if (pack.id.equals(id.toString())) return pack;
        }
        return null;
    }

    private static void recordInstalled(RemotePack pack) {
        MaMDataConfig config = MaMDataConfig.get();
        MaMDataConfig.DownloadedPack record = installed(pack.id());
        if (record == null) {
            record = new MaMDataConfig.DownloadedPack();
            config.albums.downloads.add(record);
        }
        record.id = pack.id().toString();
        record.version = pack.version();
        record.sha256 = pack.sha256();
        record.file = pack.fileName();
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static Component installedName(ResourceLocation id) {
        for (Album album : Album.ALBUMS) {
            if (album.album.equals(id)) {
                return album.name;
            }
        }

        for (RemotePack pack : PACKS) {
            if (pack.id().equals(id)) {
                return pack.name();
            }
        }

        return Component.literal(id.toString());
    }

    private static void writeManifest(ResourceLocation id, List<Path> extracted) throws IOException {
        Files.createDirectories(MANIFEST_DIRECTORY);

        Path manifest = manifestPath(id);
        List<String> lines = extracted.stream()
                .map(path -> path.toString().replace('\\', '/'))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Files.write(manifest, lines);
    }

    private static boolean deleteFromManifest(ResourceLocation id) {
        Path manifest = manifestPath(id);
        if (!Files.isRegularFile(manifest)) return false;

        Path root = DIRECTORY.toAbsolutePath().normalize();
        boolean deletedAny = false;

        try {
            List<Path> paths = Files.readAllLines(manifest).stream()
                    .map(line -> root.resolve(line).normalize())
                    .filter(path -> path.startsWith(root))
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path path : paths) {
                deletedAny |= Files.deleteIfExists(path);
                deleteEmptyParents(path.getParent(), root);
            }

            Files.deleteIfExists(manifest);
            deleteEmptyParents(manifest.getParent(), root);
        } catch (IOException ignored) {
        }

        return deletedAny;
    }

    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) return;

        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static void deleteEmptyParents(Path path, Path root) {
        Path current = path;

        while (current != null && current.startsWith(root) && !current.equals(root)) {
            try {
                if (!Files.isDirectory(current)) return;

                try (var children = Files.list(current)) {
                    if (children.findAny().isPresent()) return;
                }

                Files.deleteIfExists(current);
                current = current.getParent();
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private static boolean hasOtherInstalledPackInNamespace(ResourceLocation id) {
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().albums.downloads) {
            ResourceLocation other = ResourceLocation.tryParse(pack.id);
            if (other == null) continue;
            if (other.equals(id)) continue;
            if (other.getNamespace().equals(id.getNamespace())) return true;
        }

        return false;
    }

    private static Path manifestPath(ResourceLocation id) {
        String path = id.getPath().replace('/', '-');
        return MANIFEST_DIRECTORY.resolve(id.getNamespace() + "-" + path + ".txt");
    }
}
