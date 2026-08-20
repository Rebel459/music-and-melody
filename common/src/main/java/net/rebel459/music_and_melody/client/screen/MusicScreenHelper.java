package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMClientConfig;

import java.net.URI;

final class MusicScreenHelper {

    private MusicScreenHelper() {}

    static final Identifier FALLBACK_ALBUM_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    /** Relative to Minecraft's current GUI scale; 7 displays approximately like the previous 6. */

    private static final URI DISCORD = URI.create("https://discord.com/invite/TGbBb47Gr5");
    private static final URI KOFI = URI.create("https://ko-fi.com/rebel459");

    static Identifier albumIcon(Minecraft minecraft, Identifier icon) {
        if (icon == null) return FALLBACK_ALBUM_ICON;
        if (RemoteIconManager.isDynamic(icon)) return icon;
        if (minecraft == null || minecraft.getResourceManager().getResource(icon).isPresent()) return icon;
        return FALLBACK_ALBUM_ICON;
    }

    static void addSocialButtons(Screen screen) {
        int y = screen.height - 27;
        if (MaMClientConfig.get().discord_button) {
            screen.addRenderableWidget(new IconButton(8, y, Component.literal("Discord"), IconButton.icon("discord"), button ->
                    Util.getPlatform().openUri(DISCORD)
            ));
        }
        if (MaMClientConfig.get().kofi_button) {
            screen.addRenderableWidget(new IconButton(screen.width - IconButton.SIZE - 8, y, Component.literal("Ko-Fi"), IconButton.icon("kofi"), button ->
                    Util.getPlatform().openUri(KOFI)
            ));
        }
    }

    static void addSocialButtons(Screen screen, int x, int width, int y) {
        if (MaMClientConfig.get().discord_button) {
            screen.addRenderableWidget(new IconButton(x, y, Component.literal("Discord"), IconButton.icon("discord"), button ->
                    Util.getPlatform().openUri(DISCORD)
            ));
        }
        if (MaMClientConfig.get().kofi_button) {
            screen.addRenderableWidget(new IconButton(x + width - IconButton.SIZE, y, Component.literal("Ko-Fi"), IconButton.icon("kofi"), button ->
                    Util.getPlatform().openUri(KOFI)
            ));
        }
    }

    static void addCenteredSocialButtons(Screen screen, int centerX, int y) {
        boolean discord = MaMClientConfig.get().discord_button;
        boolean kofi = MaMClientConfig.get().kofi_button;
        int width = (discord ? IconButton.SIZE : 0) + (discord && kofi ? 4 : 0) + (kofi ? IconButton.SIZE : 0);
        int x = centerX - width / 2;
        if (discord) {
            screen.addRenderableWidget(new IconButton(x, y, Component.literal("Discord"), IconButton.icon("discord"), button -> Util.getPlatform().openUri(DISCORD)));
            x += IconButton.SIZE + 4;
        }
        if (kofi) screen.addRenderableWidget(new IconButton(x, y, Component.literal("Ko-Fi"), IconButton.icon("kofi"), button -> Util.getPlatform().openUri(KOFI)));
    }

    static void openKofi() {
        Util.getPlatform().openUri(KOFI);
    }

    static void openUri(String value) {
        try {
            Util.getPlatform().openUri(URI.create(value));
        } catch (IllegalArgumentException ignored) {
        }
    }

    static Component trackName(Album album, String song) {
        return trackName(album.trackId(song), fallbackName(song));
    }

    static Component trackName(SafeIdentifier id) {
        return trackName(id, fallbackName(id.getPath()));
    }

    static Component trackName(SafeIdentifier id, String fallback) {
        String configName = ConfigAlbum.displayName(id);
        if (configName != null) return Component.literal(configName);
        String pathKey = id.getPath().replace('/', '.');
        String key = id.getNamespace().equals("minecraft") ? pathKey : id.getNamespace() + "." + pathKey;
        return Component.translatableWithFallback(key, fallback);
    }

    private static String fallbackName(String path) {
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.endsWith(".ogg") ? name.substring(0, name.length() - ".ogg".length()) : name;
    }

    static Component playlistName(Minecraft minecraft, SafeIdentifier soundId) {
        return MusicDiscHelper.matchSound(minecraft, soundId)
                .map(match -> MusicDiscHelper.discName(match.jukeboxSong()))
                .orElseGet(() -> trackName(soundId));
    }
}
