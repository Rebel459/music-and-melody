package net.rebel459.music_and_melody.client.remote;

import net.rebel459.music_and_melody.MusicAndMelody;

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

    static Path download(RemotePack pack) throws IOException, InterruptedException {
        Files.createDirectories(TEMP_DIRECTORY);
        Path zip = TEMP_DIRECTORY.resolve(pack.fileName());
        HttpRequest request = HttpRequest.newBuilder(URI.create(pack.url()))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        HttpResponse<Path> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(zip));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download failed: " + response.statusCode());
        }
        return zip;
    }

    static CompletableFuture<byte[]> loadIcon(URI uri) {
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
