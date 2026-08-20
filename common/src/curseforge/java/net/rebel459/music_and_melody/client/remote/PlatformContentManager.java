package net.rebel459.music_and_melody.client.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.LongConsumer;

import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.client.screen.PlatformDownloadScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;

final class PlatformContentManager {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PlatformContentManager() {}

    static boolean allowRemoteDownloads() {
        return false;
    }

    static boolean openManualDownloadScreen(MusicPlayerScreen parent, RemotePack pack) {
        Minecraft.getInstance().gui.setScreen(new PlatformDownloadScreen(parent, pack));
        return true;
    }

    static Path download(RemotePack pack, LongConsumer progress) throws IOException {
        throw new IOException("Automatic remote downloads are unavailable in CurseForge builds");
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
                        throw new CompletionException(new IOException("Image download failed: " + response.statusCode()));
                    }
                    try (InputStream input = response.body()) {
                        byte[] bytes = input.readNBytes(MAX_IMAGE_BYTES + 1);
                        if (bytes.length > MAX_IMAGE_BYTES) throw new IOException("Remote image exceeds " + MAX_IMAGE_BYTES + " bytes");
                        return bytes;
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                });
    }
}
