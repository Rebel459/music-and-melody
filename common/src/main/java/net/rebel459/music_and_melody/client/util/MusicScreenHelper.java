package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.element.IconButton;
import net.rebel459.music_and_melody.client.remote.RemoteIconManager;

import java.net.URI;

public final class MusicScreenHelper {

    private MusicScreenHelper() {}

    public static final Identifier FALLBACK_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    private static final URI DISCORD = URI.create("https://discord.com/invite/TGbBb47Gr5");
    private static final URI KOFI = URI.create("https://ko-fi.com/rebel459");

    public static Identifier albumIcon(Minecraft minecraft, Identifier icon) {
        if (icon == null) return FALLBACK_ICON;
        icon = CustomAlbums.resolveIcon(minecraft, icon);
        if (CustomAlbums.isDynamicIcon(icon)) return icon;
        if (RemoteIconManager.isDynamic(icon)) return icon;
        if (minecraft == null || minecraft.getResourceManager().getResource(icon).isPresent()) return icon;
        return FALLBACK_ICON;
    }

    public static void addCenteredSocialButtons(Screen screen, int centerX, int y) {
        int width = IconButton.SIZE + 4 + IconButton.SIZE;
        int x = centerX - width / 2;
        screen.addRenderableWidget(new IconButton(x, y, Component.literal("Discord"), IconButton.icon("discord"), button -> Util.getPlatform().openUri(DISCORD)));
        x += IconButton.SIZE + 4;
        screen.addRenderableWidget(new IconButton(x, y, Component.literal("Ko-Fi"), IconButton.icon("kofi"), button -> Util.getPlatform().openUri(KOFI)));
    }

    public static void openKofi() {
        Util.getPlatform().openUri(KOFI);
    }

    public static void openUri(String value) {
        try {
            Util.getPlatform().openUri(URI.create(value));
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static Component trackName(Album album, String song) {
        return trackName(album.trackId(song), fallbackName(song));
    }

    public static Component trackName(SafeIdentifier id) {
        return trackName(id, fallbackName(id.getPath()));
    }

    public static Component trackName(SafeIdentifier id, String fallback) {
        String configName = CustomAlbums.displayName(id);
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

    public static Component playlistName(Minecraft minecraft, SafeIdentifier soundId) {
        return trackName(soundId);
    }
}
