package net.rebel459.music_and_melody.client.remote;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;

import net.minecraft.client.Minecraft;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import net.rebel459.music_and_melody.client.screen.PlatformDownloadScreen;

final class PlatformContentManager {

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
}
