package net.rebel459.music_and_melody.client.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.unified.api.core.UnifiedPlatform;
import net.rebel459.unified.api.util.VanillaVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RemoteContentManager {

    public static Set<Integer> SUPPORTED_SCHEMAS = new HashSet<>(Set.of(1));
    public static String OFFICIAL_PROVIDER = "https://github.com/Rebel459/music-and-melody-remote/official-catalogs.json";
    public static String SUPPORTER_PROVIDER = "https://github.com/Rebel459/music-and-melody-remote/supporter-catalogs.json";
    public static String COMMUNITY_PROVIDER = "https://github.com/Rebel459/music-and-melody-remote/community-catalogs.json";
    public static String SUPPORTERS = "https://github.com/Rebel459/music-and-melody-remote/supporters.json";
    public static String COMPOSERS = "https://github.com/Rebel459/music-and-melody-remote/composers.json";
    public static String SPLASHES = "https://raw.githubusercontent.com/Rebel459/music-and-melody-remote/main/splashes.txt";

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
    private static final int MAX_ICON_BYTES = 5 * 1024 * 1024;
    private static final int MAX_TEXT_BYTES = 4 * 1024 * 1024;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<RemotePack.Key, State> OVERRIDE_STATES = new ConcurrentHashMap<>();
    private static final Set<RemotePack.Key> DOWNLOADING = ConcurrentHashMap.newKeySet();
    private static final Map<RemotePack.Key, Long> MEASURED_SIZES = new ConcurrentHashMap<>();
    private static final Set<RemotePack.Key> MEASURING = ConcurrentHashMap.newKeySet();
    private static final Map<RemotePack.Key, Double> DOWNLOAD_PROGRESS = new ConcurrentHashMap<>();
    private static final List<RemotePack> PACKS = new ArrayList<>();
    private static volatile List<String> supporters = List.of();
    private static volatile List<String> composers = List.of();
    private static volatile List<String> splashes = List.of();
    private static CompletableFuture<Void> refreshTask;
    private static int refreshGeneration;
    private static boolean creditsRequested;
    private static boolean creditsRunning;
    private static boolean loaded;

    private RemoteContentManager() {}

    public static boolean remoteDownloadsAllowed() {
        return onlineFunctionalityEnabled() && PlatformContentManager.allowRemoteDownloads();
    }

    public static boolean onlineFunctionalityEnabled() {
        return MaMClientConfig.get().online_functionality;
    }

    static CompletableFuture<byte[]> loadIcon(URI uri) {
        if (!onlineFunctionalityEnabled()) return CompletableFuture.completedFuture(null);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException("Icon download failed: " + response.statusCode()));
                    }
                    try (InputStream input = response.body()) {
                        byte[] bytes = input.readNBytes(MAX_ICON_BYTES + 1);
                        if (bytes.length > MAX_ICON_BYTES) {
                            throw new IOException("Remote icon exceeds " + MAX_ICON_BYTES + " bytes");
                        }
                        return bytes;
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                });
    }

    public static boolean openManualDownloadScreen(MusicPlayerScreen parent, RemotePack pack) {
        return onlineFunctionalityEnabled() && PlatformContentManager.openManualDownloadScreen(parent, pack);
    }

    public static synchronized void refreshIfNeeded() {
        if (!loaded && refreshTask == null) {
            refresh();
        }
    }

    public static synchronized void refresh() {
        int generation = ++refreshGeneration;
        MaMDataConfig.Remote remote = MaMDataConfig.get().remote;
        List<String> repositories = remote == null || remote.catalogs == null
                ? List.of()
                : remote.catalogs.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
        List<ProviderSource> providers = new ArrayList<>();
        if (remote != null && remote.official_provider) {
            providers.add(new ProviderSource(OFFICIAL_PROVIDER, RemotePack.Provenance.OFFICIAL));
        }
        if (remote != null && remote.community_provider) {
            providers.add(new ProviderSource(COMMUNITY_PROVIDER, RemotePack.Provenance.VERIFIED));
        }
        if (!onlineFunctionalityEnabled() || providers.isEmpty() && repositories.isEmpty()) {
            if (onlineFunctionalityEnabled()) loaded = true;
            PACKS.clear();
            refreshTask = null;
            return;
        }
        loaded = true;
        SupporterIdentity identity = supporterIdentity();
        boolean officialEnabled = remote != null && remote.official_provider;
        refreshTask = CompletableFuture.supplyAsync(() -> loadCatalogs(providers, repositories, officialEnabled, identity))
                .orTimeout(3, TimeUnit.MINUTES)
                .thenAccept(packs -> {
                    synchronized (RemoteContentManager.class) {
                        if (generation != refreshGeneration) return;
                        PACKS.clear();
                        PACKS.addAll(packs);
                        refreshTask = null;
                    }
                })
                .exceptionally(throwable -> {
                    synchronized (RemoteContentManager.class) {
                        if (generation == refreshGeneration) {
                            refreshTask = null;
                            loaded = false;
                        }
                    }
                    return null;
                });
    }

    public static synchronized List<RemotePack> packs() {
        if (!onlineFunctionalityEnabled()) return List.of();
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
        if (!installed.version.equals(pack.version())) {
            return State.UPDATE_AVAILABLE;
        }
        return State.INSTALLED;
    }

    public static OptionalLong installedSize(RemotePack pack) {
        MaMDataConfig.DownloadedPack record = installed(pack);
        if (record == null) return OptionalLong.empty();
        if (record.size > 0L) return OptionalLong.of(record.size);
        RemotePack.Key key = pack.key();
        Long measured = MEASURED_SIZES.get(key);
        if (measured != null) return measured > 0L ? OptionalLong.of(measured) : OptionalLong.empty();
        if (MEASURING.add(key)) {
            CompletableFuture.runAsync(() -> {
                MEASURED_SIZES.put(key, directorySize(packDirectory(key)));
                MEASURING.remove(key);
            });
        }
        return OptionalLong.empty();
    }

    private static long directorySize(Path directory) {
        if (!Files.isDirectory(directory)) return 0L;
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (IOException exception) {
                    return 0L;
                }
            }).sum();
        } catch (IOException exception) {
            return 0L;
        }
    }

    public static OptionalDouble downloadProgress(RemotePack pack) {
        Double value = DOWNLOAD_PROGRESS.get(pack.key());
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public static boolean isDownloadedAlbum(Identifier id) {
        return isDownloaded(id, RemotePack.Tag.ALBUM);
    }

    public static boolean isDownloaded(Identifier id, RemotePack.Tag tag) {
        return DownloadedResources.owner(id, tag).map(key -> installed(key.id()) != null).orElse(false);
    }

    public static synchronized void refreshCredits() {
        if (!onlineFunctionalityEnabled()) {
            supporters = List.of();
            composers = List.of();
            return;
        }
        if (creditsRequested) return;
        creditsRequested = true;
        creditsRunning = true;
        CompletableFuture.runAsync(() -> {
            supporters = loadStringArray(SUPPORTERS);
            composers = loadStringArray(COMPOSERS);
            splashes = loadLines(SPLASHES);
            cacheNextWelcomeValues();
        }).whenComplete((ignored, throwable) -> {
            synchronized (RemoteContentManager.class) {
                creditsRunning = false;
                if (throwable != null) creditsRequested = false;
            }
        });
    }

    public static synchronized boolean creditsLoading() {
        return creditsRunning;
    }

    public static List<String> supporters() {
        return supporters;
    }

    public static List<String> displaySupporters() {
        return supporters.stream().filter(entry -> entry.indexOf('=') >= 0 || !isUuidOnly(entry)).toList();
    }

    public static List<String> composers() {
        return composers;
    }

    public static List<String> splashes() {
        return splashes;
    }

    public static Optional<RemotePack> owner(Identifier contentId, RemotePack.Tag tag) {
        return DownloadedResources.owner(contentId, tag)
                .flatMap(key -> packs().stream().filter(pack -> pack.key().equals(key)).findFirst());
    }

    public static List<String> missingDependencies(RemotePack pack) {
        return pack.dependencies().stream().filter(dependency -> !UnifiedPlatform.isModLoaded(dependency)).toList();
    }

    public static boolean isDownloadable(RemotePack pack) {
        State state = state(pack);
        return (state == State.REMOTE || state == State.FAILED) && missingDependencies(pack).isEmpty();
    }

    public static void download(RemotePack pack) {
        if (!onlineFunctionalityEnabled() || !remoteDownloadsAllowed() || !missingDependencies(pack).isEmpty()) return;
        RemotePack.Key key = pack.key();
        if (!DOWNLOADING.add(key)) return;
        DOWNLOAD_PROGRESS.put(key, 0.0D);
        OVERRIDE_STATES.put(key, State.DOWNLOADING);
        CompletableFuture.runAsync(() -> {
            Path zip = null;
            Path staging = null;
            try {
                zip = PlatformContentManager.download(pack, downloaded -> {
                    if (pack.downloadSize() > 0L) DOWNLOAD_PROGRESS.put(key, Math.min(1.0D, downloaded / (double) pack.downloadSize()));
                });
                staging = stage(pack, zip, false);
                commitOnMainThread(pack, staging);
            } catch (Exception exception) {
                if (staging != null) deleteDirectory(staging);
                failInstall(key);
            } finally {
                if (zip != null) {
                    try {
                        Files.deleteIfExists(zip);
                    } catch (IOException ignored) {
                    }
                }
            }
        });
    }

    public static void importLocal(RemotePack pack, Path zip) {
        RemotePack.Key key = pack.key();
        if (!DOWNLOADING.add(key)) return;
        OVERRIDE_STATES.put(key, State.DOWNLOADING);
        CompletableFuture.runAsync(() -> {
            Path staging = null;
            try {
                staging = stage(pack, zip, true);
                commitOnMainThread(pack, staging);
            } catch (Exception exception) {
                if (staging != null) deleteDirectory(staging);
                failInstall(key);
            }
        });
    }

    private static void failInstall(RemotePack.Key key) {
        OVERRIDE_STATES.put(key, State.FAILED);
        DOWNLOADING.remove(key);
        DOWNLOAD_PROGRESS.remove(key);
    }

    private static void commitOnMainThread(RemotePack pack, Path staging) {
        RemotePack.Key key = pack.key();
        Minecraft.getInstance().execute(() -> {
            try {
                commit(pack, staging);
                OVERRIDE_STATES.put(key, State.NEEDS_RELOAD);
                DOWNLOADING.remove(key);
                DOWNLOAD_PROGRESS.remove(key);
            } catch (Exception exception) {
                deleteDirectory(staging);
                failInstall(key);
            }
        });
    }

    public static URI externalDownloadUrl(RemotePack pack) {
        return URI.create(pack.url());
    }

    public static void markReloaded() {
        OVERRIDE_STATES.entrySet().removeIf(entry -> entry.getValue() == State.NEEDS_RELOAD);
    }

    private static List<RemotePack> loadCatalogs(List<ProviderSource> providers, List<String> repositories, boolean officialEnabled, SupporterIdentity identity) {
        List<ProviderSource> sources = new ArrayList<>(providers);
        boolean supporter = (officialEnabled || !repositories.isEmpty()) && isSupporter(identity);
        if (officialEnabled && supporter) sources.add(new ProviderSource(SUPPORTER_PROVIDER, RemotePack.Provenance.OFFICIAL));
        Set<String> restrictedCatalogs = supporter || repositories.isEmpty() ? Set.of()
                : loadProvider(SUPPORTER_PROVIDER).stream()
                .map(RemoteContentManager::catalogKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<RemotePack> packs = new ArrayList<>();
        Set<RemotePack.Key> ids = new HashSet<>();
        Set<String> loadedCatalogs = new HashSet<>();
        for (ProviderSource provider : sources) {
            for (String catalog : loadProvider(provider.url())) {
                if (!loadedCatalogs.add(catalogKey(catalog))) continue;
                try {
                    packs.addAll(loadCatalog(catalog, ids, provider.provenance()));
                } catch (Exception ignored) {
                }
            }
        }
        for (String repository : repositories) {
            String catalog = catalogKey(repository);
            if (restrictedCatalogs.contains(catalog) || !loadedCatalogs.add(catalog)) continue;
            try {
                packs.addAll(loadCatalog(repository, ids, RemotePack.Provenance.UNVERIFIED));
            } catch (Exception ignored) {
            }
        }
        packs.sort(Comparator.comparing(pack -> pack.name().getString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    private record ProviderSource(String url, RemotePack.Provenance provenance) {}

    private static String fetchText(HttpRequest request) throws IOException, InterruptedException {
        try {
            return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(response -> {
                        try (InputStream input = response.body()) {
                            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
                            byte[] bytes = input.readNBytes(MAX_TEXT_BYTES + 1);
                            if (bytes.length > MAX_TEXT_BYTES) throw new IOException("Response exceeds " + MAX_TEXT_BYTES + " bytes");
                            return new String(bytes, StandardCharsets.UTF_8);
                        } catch (IOException exception) {
                            throw new CompletionException(exception);
                        }
                    })
                    .orTimeout(30, TimeUnit.SECONDS)
                    .get();
        } catch (ExecutionException exception) {
            throw new IOException(exception.getCause());
        }
    }

    private static List<String> loadProvider(String providerUrl) {
        URI providerUri = catalogUri(providerUrl);
        return loadStringArray(providerUrl).stream().map(value -> providerUri.resolve(value).toString()).toList();
    }

    private static List<String> loadStringArray(String providerUrl) {
        try {
            URI providerUri = catalogUri(providerUrl);
            HttpRequest request = HttpRequest.newBuilder(providerUri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            String body = fetchText(request);
            if (body == null) return List.of();

            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonArray()) return List.of();
            List<String> catalogs = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
                String value = element.getAsString().trim();
                if (!value.isEmpty()) catalogs.add(value);
            }
            return catalogs;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<String> loadLines(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            String body = fetchText(request);
            if (body == null) return List.of();
            return body.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static void cacheNextWelcomeValues() {
        List<String> displaySupporters = displaySupporters();
        String supporter = randomEntry(displaySupporters);
        String composer = randomEntry(composers);
        String splash = randomEntry(splashes);
        if (supporter == null && composer == null && splash == null) return;
        Minecraft.getInstance().execute(() -> {
            MaMDataConfig.Cache cache = MaMDataConfig.get().cache;
            boolean changed = false;
            if (supporter != null) {
                cache.supporter = supporter;
                changed = true;
            }
            if (composer != null) {
                cache.composer = composer;
                changed = true;
            }
            if (splash != null) {
                cache.splash = splash;
                changed = true;
            }
            if (changed) AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        });
    }

    private static String randomEntry(List<String> entries) {
        return entries.isEmpty() ? null : entries.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(entries.size()));
    }

    private static boolean isSupporter(SupporterIdentity identity) {
        List<String> entries = loadStringArray(SUPPORTERS);
        if (!entries.isEmpty()) supporters = entries;
        for (String entry : entries) {
            int separator = entry.indexOf('=');
            if (separator < 0) {
                if (isUuidOnly(entry) && normalizedUuid(entry).equals(normalizedUuid(identity.uuid()))) return true;
                continue;
            }
            String listedIdentity = entry.substring(separator + 1).trim();
            String listedUuid = normalizedUuid(listedIdentity);
            if (!listedIdentity.isBlank() && listedIdentity.equalsIgnoreCase(identity.name())) return true;
            if (!listedUuid.isEmpty() && listedUuid.equals(normalizedUuid(identity.uuid()))) return true;
        }
        return false;
    }

    private static SupporterIdentity supporterIdentity() {
        Minecraft minecraft = Minecraft.getInstance();
        return new SupporterIdentity(minecraft.getUser().getName(), minecraft.getUser().getProfileId().toString());
    }

    private record SupporterIdentity(String name, String uuid) {}

    private static String normalizedUuid(String value) {
        return value == null ? "" : value.replace("[", "").replace("]", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isUuidOnly(String value) {
        return normalizedUuid(value).matches("[0-9a-f]{32}");
    }

    private static List<RemotePack> loadCatalog(String repositoryUrl, Set<RemotePack.Key> ids, RemotePack.Provenance provenance) throws IOException, InterruptedException {
        URI catalogUri = catalogUri(repositoryUrl);
        HttpRequest request = HttpRequest.newBuilder(catalogUri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        String body = fetchText(request);
        if (body == null) return List.of();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
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
            return SUPPORTED_SCHEMAS.contains(schema.getAsBigDecimal().intValueExact());
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private static boolean supportsCurrentVersion(RemotePack pack) {
        VanillaVersion version = VanillaVersion.getVanillaVersion();
        boolean withinMaxExclusive = pack.showBelowVersion().isEmpty()
                || VanillaVersion.parse(pack.showBelowVersion()).compareTo(version) > 0;
        boolean withinMinInclusive = pack.showFromVersion().isEmpty()
                || VanillaVersion.parse(pack.showFromVersion()).compareTo(version) <= 0;
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

    private static String catalogKey(String catalog) {
        try {
            return catalogUri(catalog).normalize().toString();
        } catch (RuntimeException ignored) {
            return catalog == null ? "" : catalog.trim();
        }
    }

    private static String repositoryName(JsonObject root, String fallback) {
        JsonObject repository = root.getAsJsonObject("repository");
        if (repository != null && repository.has("name")) return repository.get("name").getAsString();
        return fallback;
    }

    static URI rawGithubBlobUri(URI uri) {
        if (!"github.com".equalsIgnoreCase(uri.getHost())) return uri;
        String[] parts = uri.getPath().replaceFirst("^/", "").split("/");
        if (parts.length < 5 || !"blob".equals(parts[2])) return uri;
        StringBuilder path = new StringBuilder();
        for (int index = 4; index < parts.length; index++) {
            if (!path.isEmpty()) path.append('/');
            path.append(parts[index]);
        }
        return URI.create("https://raw.githubusercontent.com/" + parts[0] + "/" + parts[1]
                + "/" + parts[3] + "/" + path);
    }

    private static RemotePack parsePack(JsonObject object, String repositoryName, URI catalogUri, RemotePack.Provenance provenance) {
        Identifier id = object.has("id") ? Identifier.tryParse(object.get("id").getAsString()) : null;
        if (id != null && id.getNamespace().startsWith(".")) return null;
        if (id == null || !object.has("name") || !object.has("version") || !object.has("url") || !object.has("tags")
                || !object.has("download_size") && !object.has("size")) {
            return null;
        }
        long downloadSize = object.has("download_size") ? megabytes(object.get("download_size")) : object.get("size").getAsLong();
        long installSize = object.has("install_size") ? megabytes(object.get("install_size")) : 0L;
        if (downloadSize < 0L) return null;
        List<RemotePack.Tag> tags = stringArray(object, "tags").stream().map(RemotePack.Tag::fromSerialized).toList();
        if (tags.isEmpty() || tags.contains(null)) return null;
        List<String> dependencies = stringArray(object, "dependencies");
        String icon = object.has("icon")
                ? resolveIcon(catalogUri, object.get("icon").getAsString())
                : Identifier.withDefaultNamespace("textures/misc/unknown_pack.png").toString();
        String showFromVersion = object.has("show_from_version") ? object.get("show_from_version").getAsString() : "";
        String showBelowVersion = object.has("show_below_version") ? object.get("show_below_version").getAsString() : "";
        return new RemotePack(
                id,
                Component.literal(object.get("name").getAsString()),
                Component.literal(object.has("description") ? object.get("description").getAsString() : ""),
                repositoryName,
                object.get("version").getAsString(),
                rawGithubBlobUri(catalogUri.resolve(object.get("url").getAsString())).toString(),
                downloadSize,
                installSize,
                icon,
                showFromVersion,
                showBelowVersion,
                tags,
                dependencies,
                provenance
        );
    }

    private static long megabytes(JsonElement element) {
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value < 0.0D) throw new IllegalArgumentException("Invalid size: " + value);
        return Math.round(value * 1024.0D * 1024.0D);
    }

    private static String resolveIcon(URI catalogUri, String icon) {
        if (icon.indexOf(':') > 0 && Identifier.tryParse(icon) != null) return icon;
        return catalogUri.resolve(icon).toString();
    }

    private static List<String> stringArray(JsonObject object, String name) {
        JsonArray array = object.getAsJsonArray(name);
        if (array == null) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
            String value = element.getAsString().trim();
            if (!value.isEmpty()) values.add(value);
        }
        return List.copyOf(values);
    }

    private static Path stage(RemotePack pack, Path zip, boolean imported) throws IOException {
        if (!Files.isRegularFile(zip)) throw new IOException("Import file missing: " + zip);
        String expectedName = pack.expectedFileName();
        if (imported && !expectedName.isEmpty() && !zip.getFileName().toString().equalsIgnoreCase(expectedName)) {
            throw new IOException("Imported file name mismatch");
        }
        long fileSize = Files.size(zip);
        if (fileSize > pack.downloadLimit() || fileSize < pack.downloadMinimum()) {
            throw new IOException("File size doesn't match the download size listed in the catalog");
        }

        Path target = packDirectory(pack.key());
        Path staging = target.resolveSibling(target.getFileName() + ".new");
        deleteDirectory(staging);
        try {
            extractZip(zip, staging, pack.installMinimum(), pack.installLimit());
        } catch (IOException | RuntimeException exception) {
            deleteDirectory(staging);
            throw exception;
        }
        return staging;
    }

    private static synchronized void commit(RemotePack pack, Path staging) throws IOException {
        Path target = packDirectory(pack.key());
        if (installed(pack) != null) {
            deleteInstalledFiles(pack.key());
        } else {
            deleteDirectory(target);
            if (!hasOtherInstalledPackInNamespace(pack.id(), pack.key())) {
                deleteNamespaceDirectory(pack.id());
            }
        }
        if (Files.exists(target)) throw new IOException("Could not replace existing pack files (in use?)");
        Files.createDirectories(target.getParent());
        Files.move(staging, target);
        List<Path> installedFiles;
        try (var files = Files.walk(target)) {
            installedFiles = files.filter(Files::isRegularFile).toList();
        }
        writeManifest(pack.key(), installedFiles);
        long installedBytes = 0L;
        for (Path file : installedFiles) installedBytes += Files.size(file);
        recordInstalled(pack, installedBytes);
    }

    private static void deleteInstalledFiles(RemotePack.Key key) {
        deleteFromManifest(key);
        deleteDirectory(packDirectory(key));
        if (!hasOtherInstalledPackInNamespace(key.id(), key)) {
            deleteNamespaceDirectory(key.id());
        }

        DownloadedResources.invalidate();
    }

    private static List<Path> extractZip(Path zip, Path directory, long minBytes, long maxBytes) throws IOException {
        Files.createDirectories(directory);
        Path root = directory.toAbsolutePath().normalize();
        List<Path> extracted = new ArrayList<>();
        long total = 0L;
        byte[] buffer = new byte[64 * 1024];

        try (InputStream input = Files.newInputStream(zip); ZipInputStream stream = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) throw new IOException("Unsafe zip entry: " + entry.getName());

                Files.createDirectories(target.getParent());
                try (var output = Files.newOutputStream(target)) {
                    int read;
                    while ((read = stream.read(buffer)) >= 0) {
                        total += read;
                        if (total > maxBytes) throw new IOException("Pack is larger than the install size listed in the catalog");
                        output.write(buffer, 0, read);
                    }
                }
                extracted.add(target);
            }
        }
        if (total < minBytes) throw new IOException("Pack is smaller than the install size listed in the catalog");

        return extracted;
    }

    private static MaMDataConfig.DownloadedPack installed(RemotePack pack) {
        return installed(pack.id());
    }

    private static MaMDataConfig.DownloadedPack installed(Identifier id) {
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().remote.downloads) {
            if (pack == null || !pack.id.equals(id.toString())) continue;
            return pack;
        }
        return null;
    }

    private static MaMDataConfig.DownloadedPack installedAny(Identifier id) {
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().remote.downloads) {
            if (pack != null && pack.id.equals(id.toString())) return pack;
        }
        return null;
    }

    private static void recordInstalled(RemotePack pack, long installedBytes) {
        MaMDataConfig config = MaMDataConfig.get();
        MaMDataConfig.DownloadedPack record = installed(pack);
        if (record == null) {
            record = new MaMDataConfig.DownloadedPack();
            config.remote.downloads.add(record);
        }
        record.id = pack.id().toString();
        record.tags = pack.tags().stream().map(tag -> tag.name().toLowerCase(Locale.ROOT)).toList();
        record.version = pack.version();
        record.size = installedBytes;
        record.file = pack.fileName();
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        DownloadedResources.invalidate();
    }

    public record InstalledPack(
            Component name,
            Identifier id,
            RemotePack.Tag tag,
            String version
    ) {}

    public static synchronized List<InstalledPack> installedPacks() {
        List<InstalledPack> packs = new ArrayList<>();

        for (MaMDataConfig.DownloadedPack record : MaMDataConfig.get().remote.downloads) {
            Identifier id = Identifier.tryParse(record.id);
            if (id == null) continue;
            RemotePack.Tag tag = record.tags == null || record.tags.isEmpty() ? null : RemotePack.Tag.fromSerialized(record.tags.getFirst());

            packs.add(new InstalledPack(
                    installedName(id, tag),
                    id,
                    tag,
                    record.version
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
            if (pack.id().equals(id) && (tag == null || pack.tags().contains(tag))) {
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
        MaMDataConfig.DownloadedPack record = installed(key.id());
        if (record == null) return false;

        deleteFromManifest(key);
        boolean directoryGone = deleteDirectory(packDirectory(key));
        boolean otherInstalledInNamespace = hasOtherInstalledPackInNamespace(key.id(), key);
        boolean namespaceGone = otherInstalledInNamespace || deleteNamespaceDirectory(key.id());
        if (!directoryGone || !namespaceGone) {
            DownloadedResources.invalidate();
            return false;
        }

        config.remote.downloads.removeIf(pack -> pack == record);
        String idString = key.id().toString();
        config.albums.disabled.remove(idString);
        config.player.favourites.removeIf(entry -> entry != null && idString.equals(entry.id));

        if (!otherInstalledInNamespace) {
            String namespacePrefix = key.id().getNamespace() + ":";
            config.albums.disabled_tracks.removeIf(track -> track.startsWith(namespacePrefix));
        }

        OVERRIDE_STATES.remove(key);
        MEASURED_SIZES.remove(key);

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
        DownloadedResources.invalidate();
        return true;
    }

    public static synchronized boolean deleteInstalled(Identifier id) {
        MaMDataConfig.DownloadedPack record = installedAny(id);
        if (record == null) return false;
        return deleteInstalled(new RemotePack.Key(id));
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
        if (!Files.isRegularFile(manifest)) manifest = legacyManifestPath(key);
        if (!Files.isRegularFile(manifest)) return false;

        Path root = DIRECTORY.toAbsolutePath().normalize();
        Set<Path> otherPackFiles = filesOwnedByOtherManifests(manifest, root);
        List<Path> paths;

        try {
            paths = Files.readAllLines(manifest).stream()
                    .map(line -> root.resolve(line).normalize())
                    .filter(path -> path.startsWith(root))
                    .sorted(Comparator.reverseOrder())
                    .toList();
        } catch (IOException ignored) {
            return false;
        }

        boolean complete = true;
        for (Path path : paths) {
            if (otherPackFiles.contains(path)) continue;
            try {
                Files.deleteIfExists(path);
                deleteEmptyParents(path.getParent(), root);
            } catch (IOException ignored) {
                complete = false;
            }
        }

        try {
            Files.deleteIfExists(manifest);
            deleteEmptyParents(manifest.getParent(), root);
        } catch (IOException ignored) {
            complete = false;
        }

        return complete;
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

    private static boolean deleteDirectory(Path directory) {
        if (!Files.exists(directory)) return true;

        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        return !Files.exists(directory);
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
        MaMDataConfig.DownloadedPack excludedRecord = installed(id);
        for (MaMDataConfig.DownloadedPack pack : MaMDataConfig.get().remote.downloads) {
            if (pack == null) continue;
            if (pack == excludedRecord) continue;
            Identifier other = Identifier.tryParse(pack.id);
            if (other == null) continue;
            if (other.getNamespace().equals(id.getNamespace())) return true;
        }

        return false;
    }

    private static boolean deleteNamespaceDirectory(Identifier id) {
        String namespace = id.getNamespace();
        if (namespace.startsWith(".")) return true;
        Path root = DIRECTORY.toAbsolutePath().normalize();
        Path target = root.resolve(namespace).normalize();
        if (!target.getParent().equals(root)) return true;
        return deleteDirectory(target);
    }

    private static Path manifestPath(RemotePack.Key key) {
        String name = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.id().toString().getBytes(StandardCharsets.UTF_8));
        return MANIFEST_DIRECTORY.resolve(name + ".txt");
    }

    private static Path legacyManifestPath(RemotePack.Key key) {
        String path = key.id().getPath().replace('/', '-');
        return MANIFEST_DIRECTORY.resolve(key.id().getNamespace() + "-" + path + ".txt");
    }

    static Path packDirectory(RemotePack.Key key) {
        String name = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.id().toString().getBytes(StandardCharsets.UTF_8));
        return PACK_DIRECTORY.resolve(name);
    }
}
