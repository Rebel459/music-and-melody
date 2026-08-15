package net.rebel459.music_and_melody.client.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.unified.api.util.VanillaVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RemoteContentManager {

    public static Set<Integer> SUPPORTED_REMOTE_SCHEMAS = new HashSet<>(Set.of(1));
    public static String OFFICIAL_PROVIDER = "https://github.com/Rebel459/music-and-melody-remote/official-catalogs.json";
    public static String COMMUNITY_PROVIDER = "https://github.com/Rebel459/music-and-melody-remote/community-catalogs.json";

    public enum State {
        REMOTE,
        DOWNLOADING,
        NEEDS_RELOAD,
        INSTALLED,
        UPDATE_AVAILABLE,
        FAILED
    }

    private static final Path DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "downloads");
    private static final Path PACK_DIRECTORY = DIRECTORY.resolve(".packs");
    private static final Path MANIFEST_DIRECTORY = DIRECTORY.resolve(".manifests");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<RemotePack.Key, State> OVERRIDE_STATES = new ConcurrentHashMap<>();
    private static final Set<RemotePack.Key> DOWNLOADING = ConcurrentHashMap.newKeySet();
    private static final Map<RemotePack.Key, Double> DOWNLOAD_PROGRESS = new ConcurrentHashMap<>();
    private static final List<RemotePack> PACKS = new ArrayList<>();
    private static CompletableFuture<Void> refreshTask;
    private static boolean loaded;

    private RemoteContentManager() {}

    public static boolean remoteDownloadsAllowed() {
        return PlatformContentManager.allowRemoteDownloads();
    }

    public static synchronized void refreshIfNeeded() {
        if (!loaded && refreshTask == null) {
            refresh();
        }
    }

    public static synchronized void refresh() {
        loaded = true;
        MaMDataConfig.Remote remote = MaMDataConfig.get().remote;
        List<String> repositories = remote == null || remote.added_repositories == null
                ? List.of()
                : remote.added_repositories.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
        List<ProviderSource> providers = new ArrayList<>();
        if (remote != null && remote.official_provider) {
            providers.add(new ProviderSource(OFFICIAL_PROVIDER, RemotePack.Provenance.OFFICIAL));
        }
        if (remote != null && remote.community_provider) {
            providers.add(new ProviderSource(COMMUNITY_PROVIDER, RemotePack.Provenance.VERIFIED));
        }
        if (!MaMClientConfig.get().remote_downloads || providers.isEmpty() && repositories.isEmpty()) {
            PACKS.clear();
            refreshTask = null;
            return;
        }
        refreshTask = CompletableFuture.supplyAsync(() -> loadCatalogs(providers, repositories))
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
        RemotePack.Key key = pack.key();
        if (DOWNLOADING.contains(key)) return State.DOWNLOADING;
        State override = OVERRIDE_STATES.get(key);
        if (override != null) return override;
        MaMDataConfig.DownloadedPack installed = installed(pack);
        if (installed == null) return State.REMOTE;
        if (!installed.version.equals(pack.version()) || !installed.sha256.equalsIgnoreCase(pack.sha256())) {
            return State.UPDATE_AVAILABLE;
        }
        return State.INSTALLED;
    }

    public static OptionalDouble downloadProgress(RemotePack pack) {
        Double value = DOWNLOAD_PROGRESS.get(pack.key());
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public static boolean isDownloadedAlbum(Identifier id) {
        return installed(id, RemotePack.Tag.ALBUM) != null;
    }

    public static boolean isDownloaded(Identifier id, RemotePack.Tag tag) {
        return installed(id, tag) != null;
    }

    public static void download(RemotePack pack) {
        if (!remoteDownloadsAllowed()) return;
        RemotePack.Key key = pack.key();
        if (!DOWNLOADING.add(key)) return;
        DOWNLOAD_PROGRESS.put(key, 0.0D);
        OVERRIDE_STATES.put(key, State.DOWNLOADING);
        CompletableFuture.runAsync(() -> {
            Path zip = null;
            try {
                zip = PlatformContentManager.download(pack, downloaded -> {
                    if (pack.size() > 0L) DOWNLOAD_PROGRESS.put(key, Math.min(1.0D, downloaded / (double) pack.size()));
                });
                importAndExtract(pack, zip);
                recordInstalled(pack);
                OVERRIDE_STATES.put(key, State.NEEDS_RELOAD);
            } catch (Exception exception) {
                OVERRIDE_STATES.put(key, State.FAILED);
            } finally {
                if (zip != null) {
                    try {
                        Files.deleteIfExists(zip);
                    } catch (IOException ignored) {
                    }
                }
                DOWNLOADING.remove(key);
                DOWNLOAD_PROGRESS.remove(key);
            }
        });
    }

    public static void importLocal(RemotePack pack, Path zip) {
        RemotePack.Key key = pack.key();
        if (!DOWNLOADING.add(key)) return;
        OVERRIDE_STATES.put(key, State.DOWNLOADING);
        CompletableFuture.runAsync(() -> {
            try {
                importAndExtract(pack, zip);
                recordInstalled(pack);
                OVERRIDE_STATES.put(key, State.NEEDS_RELOAD);
            } catch (Exception exception) {
                OVERRIDE_STATES.put(key, State.FAILED);
            } finally {
                DOWNLOADING.remove(key);
            }
        });
    }

    public static String externalDownloadUrl(RemotePack pack) {
        return pack.url();
    }

    public static void markReloaded() {
        OVERRIDE_STATES.entrySet().removeIf(entry -> entry.getValue() == State.NEEDS_RELOAD);
    }

    private static List<RemotePack> loadCatalogs(List<ProviderSource> providers, List<String> repositories) {
        List<RemotePack> packs = new ArrayList<>();
        Set<RemotePack.Key> ids = new HashSet<>();
        Set<String> loadedCatalogs = new HashSet<>();
        for (ProviderSource provider : providers) {
            for (String catalog : loadProvider(provider.url())) {
                if (!loadedCatalogs.add(catalog)) continue;
                try {
                    packs.addAll(loadCatalog(catalog, ids, provider.provenance()));
                } catch (Exception ignored) {
                }
            }
        }
        for (String repository : repositories) {
            if (!loadedCatalogs.add(repository)) continue;
            try {
                packs.addAll(loadCatalog(repository, ids, RemotePack.Provenance.UNVERIFIED));
            } catch (Exception ignored) {
            }
        }
        packs.sort(Comparator.comparing(pack -> pack.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    private record ProviderSource(String url, RemotePack.Provenance provenance) {}

    private static List<String> loadProvider(String providerUrl) {
        try {
            URI providerUri = catalogUri(providerUrl);
            HttpRequest request = HttpRequest.newBuilder(providerUri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();

            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonArray()) return List.of();
            List<String> catalogs = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
                String value = element.getAsString().trim();
                if (!value.isEmpty()) catalogs.add(providerUri.resolve(value).toString());
            }
            return catalogs;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<RemotePack> loadCatalog(String repositoryUrl, Set<RemotePack.Key> ids, RemotePack.Provenance provenance) throws IOException, InterruptedException {
        URI catalogUri = catalogUri(repositoryUrl);
        HttpRequest request = HttpRequest.newBuilder(catalogUri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!isSupportedCatalogSchema(root)) return List.of();
        String repositoryName = repositoryName(root, repositoryUrl);
        JsonArray array = root.getAsJsonArray("packs");
        if (array == null) return List.of();

        List<RemotePack> packs = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            RemotePack pack;
            try {
                pack = parsePack(element.getAsJsonObject(), repositoryName, catalogUri, provenance);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (pack == null || !supportsCurrentVersion(pack) || !ids.add(pack.key())) continue;
            packs.add(pack);
        }
        return packs;
    }

    private static boolean isSupportedCatalogSchema(JsonObject root) {
        JsonElement schema = root.get("schema");
        if (schema == null || !schema.isJsonPrimitive() || !schema.getAsJsonPrimitive().isNumber()) {
            return false;
        }

        try {
            return SUPPORTED_REMOTE_SCHEMAS.contains(schema.getAsBigDecimal().intValueExact());
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private static boolean supportsCurrentVersion(RemotePack pack) {
        VanillaVersion version = VanillaVersion.getVanillaVersion();
        boolean withinMaxExclusive = pack.belowVersion().isEmpty()
                || VanillaVersion.parse(pack.belowVersion()).compareTo(version) > 0;
        boolean withinMinInclusive = pack.atLeastVersion().isEmpty()
                || VanillaVersion.parse(pack.atLeastVersion()).compareTo(version) <= 0;
        return withinMaxExclusive && withinMinInclusive;
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
                StringBuilder path = new StringBuilder();
                if (parts.length >= 4 && ("tree".equals(parts[2]) || "blob".equals(parts[2]))) {
                    branch = parts[3];
                    for (int i = 4; i < parts.length; i++) {
                        if (!path.isEmpty()) path.append('/');
                        path.append(parts[i]);
                    }
                    if ("blob".equals(parts[2]) || isJsonCatalogUrl(uri)) {
                        return URI.create("https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path);
                    }
                }
                if (path.isEmpty() && isJsonCatalogUrl(uri)) {
                    for (int i = 2; i < parts.length; i++) {
                        if (!path.isEmpty()) path.append('/');
                        path.append(parts[i]);
                    }
                    return URI.create("https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path);
                }
                if (!path.isEmpty()) path.append('/');
                path.append("catalog.json");
                return URI.create("https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path);
            }
        }
        if (isJsonCatalogUrl(uri)) return uri;

        String path = uri.getPath();
        if (path == null || path.isEmpty() || path.endsWith("/")) return uri.resolve("catalog.json");
        return uri.resolve(path.substring(path.lastIndexOf('/') + 1) + "/catalog.json");
    }

    private static boolean isJsonCatalogUrl(URI uri) {
        String path = uri.getPath();
        return path != null && path.toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static String repositoryName(JsonObject root, String fallback) {
        JsonObject repository = root.getAsJsonObject("repository");
        if (repository != null && repository.has("name")) return repository.get("name").getAsString();
        return fallback;
    }

    private static RemotePack parsePack(JsonObject object, String repositoryName, URI catalogUri, RemotePack.Provenance provenance) {
        Identifier id = object.has("id") ? Identifier.tryParse(object.get("id").getAsString()) : null;
        if (id == null || !object.has("name") || !object.has("version") || !object.has("url") || !object.has("sha256") || !object.has("size") || !object.has("tag")) {
            return null;
        }
        RemotePack.Tag tag = RemotePack.Tag.fromSerialized(object.get("tag").getAsString());
        if (tag == null) return null;
        String icon = object.has("icon")
                ? object.get("icon").getAsString()
                : Identifier.withDefaultNamespace("textures/misc/unknown_pack.png").toString();
        String atLeastVersion = object.has("at_least_version") ? object.get("at_least_version").getAsString() : "";
        String belowVersion = object.has("below_version") ? object.get("below_version").getAsString() : "";
        return new RemotePack(
                id,
                Component.literal(object.get("name").getAsString()),
                Component.literal(object.has("description") ? object.get("description").getAsString() : ""),
                repositoryName,
                object.get("version").getAsString(),
                catalogUri.resolve(object.get("url").getAsString()).toString(),
                object.get("sha256").getAsString().toLowerCase(Locale.ROOT),
                object.get("size").getAsLong(),
                icon,
                atLeastVersion,
                belowVersion,
                tag,
                provenance
        );
    }

    private static void importAndExtract(RemotePack pack, Path zip) throws IOException {
        if (!Files.isRegularFile(zip)) throw new IOException("Import file missing: " + zip);
        if (Files.size(zip) != pack.size()) throw new IOException("Imported size mismatch");
        String hash = sha256(zip);
        if (!hash.equalsIgnoreCase(pack.sha256())) throw new IOException("Imported hash mismatch");
        if (installed(pack) != null) {
            deleteInstalledFiles(pack.key());
        }
        List<Path> extracted = extractZip(zip, packDirectory(pack.key()));
        writeManifest(pack.key(), extracted);
    }

    private static void deleteInstalledFiles(RemotePack.Key key) {
        boolean deletedFiles = deleteFromManifest(key);

        if (!deletedFiles) {
            deleteDirectory(packDirectory(key));
            if (!hasOtherInstalledPackInNamespace(key.id(), key)) {
                deleteDirectory(DIRECTORY.resolve(key.id().getNamespace()));
            }
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
                extracted.add(target);
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

    private static MaMDataConfig.DownloadedPack installed(RemotePack pack) {
        return installed(pack.id(), pack.tag());
    }

    private static MaMDataConfig.DownloadedPack installed(Identifier id, RemotePack.Tag tag) {
        MaMDataConfig.DownloadedPack legacy = null;
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().remote.downloads) {
            if (pack == null || !pack.id.equals(id.toString())) continue;
            RemotePack.Tag storedTag = RemotePack.Tag.fromSerialized(pack.tag);
            if (storedTag == tag) return pack;
            if (storedTag == null) legacy = pack;
        }
        return legacy;
    }

    private static MaMDataConfig.DownloadedPack installedAny(Identifier id) {
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().remote.downloads) {
            if (pack != null && pack.id.equals(id.toString())) return pack;
        }
        return null;
    }

    private static void recordInstalled(RemotePack pack) {
        MaMDataConfig config = MaMDataConfig.get();
        MaMDataConfig.DownloadedPack record = installed(pack);
        if (record == null) {
            record = new MaMDataConfig.DownloadedPack();
            config.remote.downloads.add(record);
        }
        record.id = pack.id().toString();
        record.tag = pack.tag() == null ? "" : pack.tag().name().toLowerCase(Locale.ROOT);
        record.version = pack.version();
        record.sha256 = pack.sha256();
        record.file = pack.fileName();
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public record InstalledPack(
            Component name,
            Identifier id,
            RemotePack.Tag tag,
            String version,
            String sha256
    ) {}

    public static synchronized List<InstalledPack> installedPacks() {
        List<InstalledPack> packs = new ArrayList<>();

        for (MaMDataConfig.DownloadedPack record : MaMDataConfig.get().remote.downloads) {
            Identifier id = Identifier.tryParse(record.id);
            if (id == null) continue;
            RemotePack.Tag tag = RemotePack.Tag.fromSerialized(record.tag);

            packs.add(new InstalledPack(
                    installedName(id, tag),
                    id,
                    tag,
                    record.version,
                    record.sha256
            ));
        }

        packs.sort(Comparator.comparing(pack -> pack.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    private static Component installedName(Identifier id, RemotePack.Tag tag) {
        for (Album album : Album.ALBUMS) {
            if (album.album.equals(id)) {
                return album.name;
            }
        }

        for (RemotePack pack : PACKS) {
            if (pack.id().equals(id) && (tag == null || pack.tag() == tag)) {
                return pack.name();
            }
        }

        return Component.literal(id.toString());
    }

    public static synchronized boolean deleteInstalled(RemotePack pack) {
        return deleteInstalled(pack.key());
    }

    public static synchronized boolean deleteInstalled(RemotePack.Key key) {
        if (DOWNLOADING.contains(key)) return false;

        MaMDataConfig config = MaMDataConfig.get();
        MaMDataConfig.DownloadedPack record = installed(key.id(), key.tag());
        if (record == null) return false;

        boolean deletedFiles = deleteFromManifest(key);

        if (!deletedFiles) {
            deleteDirectory(packDirectory(key));
            if (!hasOtherInstalledPackInNamespace(key.id(), key)) {
                deleteDirectory(DIRECTORY.resolve(key.id().getNamespace()));
            }
        }

        config.remote.downloads.removeIf(pack -> pack == record);
        String idString = key.id().toString();
        config.albums.disabled_albums.remove(idString);
        config.albums.favourites.remove(idString);

        if (!hasOtherInstalledPackInNamespace(key.id(), key)) {
            String namespacePrefix = key.id().getNamespace() + ":";
            config.albums.disabled_tracks.removeIf(track -> track.startsWith(namespacePrefix));
        }

        OVERRIDE_STATES.remove(key);

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        DownloadedResources.invalidate();
        return true;
    }

    /** Compatibility overload for callers that only have an identifier. */
    public static synchronized boolean deleteInstalled(Identifier id) {
        MaMDataConfig.DownloadedPack record = installedAny(id);
        if (record == null) return false;
        RemotePack.Tag tag = RemotePack.Tag.fromSerialized(record.tag);
        return deleteInstalled(new RemotePack.Key(id, tag));
    }

    private static void writeManifest(RemotePack.Key key, List<Path> extracted) throws IOException {
        Files.createDirectories(MANIFEST_DIRECTORY);

        Path manifest = manifestPath(key);
        Path root = DIRECTORY.toAbsolutePath().normalize();
        List<String> lines = extracted.stream()
                .map(path -> root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Files.write(manifest, lines);
    }

    private static boolean deleteFromManifest(RemotePack.Key key) {
        Path manifest = manifestPath(key);
        if (!Files.isRegularFile(manifest)) return false;

        Path root = DIRECTORY.toAbsolutePath().normalize();
        Set<Path> otherPackFiles = filesOwnedByOtherManifests(manifest, root);

        try {
            List<Path> paths = Files.readAllLines(manifest).stream()
                    .map(line -> root.resolve(line).normalize())
                    .filter(path -> path.startsWith(root))
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path path : paths) {
                if (otherPackFiles.contains(path)) continue;
                Files.deleteIfExists(path);
                deleteEmptyParents(path.getParent(), root);
            }

            Files.deleteIfExists(manifest);
            deleteEmptyParents(manifest.getParent(), root);
        } catch (IOException ignored) {
        }

        return true;
    }

    private static Set<Path> filesOwnedByOtherManifests(Path excludedManifest, Path root) {
        Set<Path> paths = new HashSet<>();
        if (!Files.isDirectory(MANIFEST_DIRECTORY)) return paths;

        try (var manifests = Files.list(MANIFEST_DIRECTORY)) {
            for (Path manifest : manifests.filter(Files::isRegularFile).toList()) {
                if (manifest.equals(excludedManifest)) continue;
                try {
                    Files.readAllLines(manifest).stream()
                            .map(line -> root.resolve(line).normalize())
                            .filter(path -> path.startsWith(root))
                            .forEach(paths::add);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }

        return paths;
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

    private static boolean hasOtherInstalledPackInNamespace(Identifier id, RemotePack.Key excluded) {
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().remote.downloads) {
            Identifier other = Identifier.tryParse(pack.id);
            if (other == null) continue;
            RemotePack.Tag otherTag = RemotePack.Tag.fromSerialized(pack.tag);
            if (other.equals(excluded.id()) && otherTag == excluded.tag()) continue;
            if (other.getNamespace().equals(id.getNamespace())) return true;
        }

        return false;
    }

    private static Path manifestPath(RemotePack.Key key) {
        String path = key.id().getPath().replace('/', '-');
        if (key.tag() == null) return MANIFEST_DIRECTORY.resolve(key.id().getNamespace() + "-" + path + ".txt");
        String tag = key.tag() == null ? "unknown" : key.tag().name().toLowerCase(Locale.ROOT);
        return MANIFEST_DIRECTORY.resolve(key.id().getNamespace() + "-" + path + "-" + tag + ".txt");
    }

    static Path packDirectory(RemotePack.Key key) {
        if (key.tag() == null) {
            return PACK_DIRECTORY.resolve(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(key.id().toString().getBytes(StandardCharsets.UTF_8)));
        }
        String tag = key.tag() == null ? "unknown" : key.tag().name().toLowerCase(Locale.ROOT);
        String name = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((key.id() + "|" + tag).getBytes(StandardCharsets.UTF_8));
        return PACK_DIRECTORY.resolve(name);
    }
}
