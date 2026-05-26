package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.client.util.SafeLocation;
import net.rebel459.music_and_melody.config.ConfigAlbum;

import java.net.URI;

final class MusicScreenHelper {

    private MusicScreenHelper() {}

    static final ResourceLocation FALLBACK_ALBUM_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

    static final URI DISCORD = URI.create("https://discord.com/invite/TGbBb47Gr5");
    static final URI KOFI = URI.create("https://ko-fi.com/rebel459");

    static ResourceLocation albumIcon(Minecraft minecraft, ResourceLocation icon) {
        if (icon == null) return FALLBACK_ALBUM_ICON;
        if (minecraft == null || minecraft.getResourceManager().getResource(icon).isPresent()) return icon;
        return FALLBACK_ALBUM_ICON;
    }

    static Component trackName(Album album, String song) {
        return trackName(album.trackId(song), fallbackName(song));
    }

    static Component trackName(SafeLocation id) {
        return trackName(id, fallbackName(id.getPath()));
    }

    static Component trackName(SafeLocation id, String fallback) {
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

    static Component playlistName(Minecraft minecraft, SafeLocation soundId) {
        return MusicDiscHelper.matchSound(minecraft, soundId)
                .map(match -> MusicDiscHelper.discName(match.jukeboxSong()))
                .orElseGet(() -> trackName(soundId));
    }
}
