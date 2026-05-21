package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.util.MusicDiscHelper;
import net.rebel459.music_and_melody.config.ConfigAlbum;

final class MusicScreenHelper {

    private MusicScreenHelper() {}

    static Component trackName(Album album, String song) {
        return trackName(album.trackId(song), song);
    }

    static Component trackName(Identifier id) {
        return trackName(id, id.toString());
    }

    static Component trackName(Identifier id, String fallback) {
        String configName = ConfigAlbum.displayName(id);
        if (configName != null) return Component.literal(configName);
        String pathKey = id.getPath().replace('/', '.');
        String key = id.getNamespace().equals("minecraft") ? pathKey : id.getNamespace() + "." + pathKey;
        return Component.translatableWithFallback(key, fallback);
    }

    static Component playlistName(Minecraft minecraft, Identifier soundId) {
        return MusicDiscHelper.matchSound(minecraft, soundId)
                .map(match -> MusicDiscHelper.discName(match.jukeboxSong()))
                .orElseGet(() -> trackName(soundId));
    }
}
