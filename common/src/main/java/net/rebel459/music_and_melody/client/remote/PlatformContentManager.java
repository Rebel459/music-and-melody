package net.rebel459.music_and_melody.client.remote;

import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.LongConsumer;

final class PlatformContentManager {

    private static final int MAX_ICON_BYTES = 5 * 1024 * 1024;
    private static final Path TEMP_DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "downloads", ".tmp");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PlatformContentManager() {}

    static boolean allowRemoteDownloads() {
        return true;
    }

    static boolean openManualDownloadScreen(MusicPlayerScreen parent, RemotePack pack) {
        return false;
    }

    static Path download(RemotePack pack, LongConsumer progress) throws IOException, InterruptedException {
        if (!MaMClientConfig.get().online_functionality) throw new IOException("Online functionality is disabled");
        Files.createDirectories(TEMP_DIRECTORY);
        Path zip = TEMP_DIRECTORY.resolve(pack.fileName());
        HttpRequest request = HttpRequest.newBuilder(URI.create(pack.url()))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("Download failed: " + response.statusCode());
        }
        try (InputStream input = response.body(); var output = Files.newOutputStream(zip)) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = 0L;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) continue;
                output.write(buffer, 0, read);
                downloaded += read;
                progress.accept(downloaded);
            }
        }
        return zip;
    }

    static CompletableFuture<byte[]> loadIcon(URI uri) {
        if (!MaMClientConfig.get().online_functionality) return CompletableFuture.completedFuture(null);
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
}
