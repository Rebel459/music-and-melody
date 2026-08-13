package net.rebel459.music_and_melody.client.remote;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

final class PlatformContentManager {

    private PlatformContentManager() {}

    static boolean allowRemoteDownloads() {
        return false;
    }

    static Path download(RemotePack pack) throws IOException {
        throw new IOException("Automatic remote downloads are unavailable in CurseForge builds");
    }

    static CompletableFuture<byte[]> loadIcon(URI uri) {
        return CompletableFuture.completedFuture(null);
    }
}
