package net.rebel459.music_and_melody.client.remote;

import java.io.IOException;
import java.nio.file.Path;

final class PlatformContentManager {

    private PlatformContentManager() {}

    static boolean allowRemoteDownloads() {
        return false;
    }

    static Path download(RemotePack pack) throws IOException {
        throw new IOException("Automatic remote downloads are unavailable in CurseForge builds");
    }
}
