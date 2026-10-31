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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

final class PlatformContentManager {

    private static final Path TEMP_DIRECTORY = Path.of("config", MusicAndMelody.MOD_ID, "downloads", ".tmp");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final long STALL_TIMEOUT_NANOS = Duration.ofSeconds(30).toNanos();

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
        Path zip = TEMP_DIRECTORY.resolve(pack.tempFileName());
        HttpRequest request = HttpRequest.newBuilder(URI.create(pack.url()))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("Download failed: " + response.statusCode());
        }
        AtomicLong lastActivity = new AtomicLong(System.nanoTime());
        AtomicBoolean finished = new AtomicBoolean();
        InputStream body = response.body();
        long limit = pack.downloadLimit();
        long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (declared > limit || declared >= 0L && declared < pack.downloadMinimum()) {
            body.close();
            throw new IOException("Download size doesn't match the size listed in the catalog");
        }
        Thread watchdog = Thread.ofVirtual().unstarted(() -> {
            while (!finished.get()) {
                try {
                    Thread.sleep(2_000L);
                } catch (InterruptedException exception) {
                    return;
                }
                if (System.nanoTime() - lastActivity.get() > STALL_TIMEOUT_NANOS) {
                    try {
                        body.close();
                    } catch (IOException ignored) {
                    }
                    return;
                }
            }
        });
        watchdog.start();
        try (InputStream input = body; var output = Files.newOutputStream(zip)) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = 0L;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) continue;
                lastActivity.set(System.nanoTime());
                downloaded += read;
                if (downloaded > limit) throw new IOException("Download exceeds the size listed in the catalog");
                output.write(buffer, 0, read);
                progress.accept(downloaded);
            }
            if (downloaded < pack.downloadMinimum()) throw new IOException("Download is smaller than the size listed in the catalog");
        } catch (IOException exception) {
            Files.deleteIfExists(zip);
            throw exception;
        } finally {
            finished.set(true);
            watchdog.interrupt();
        }
        return zip;
    }
}
