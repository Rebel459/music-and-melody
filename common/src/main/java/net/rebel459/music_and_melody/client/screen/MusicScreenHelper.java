package net.rebel459.music_and_melody.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Item;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.network.ServerHelper;

import java.util.Optional;

final class MusicScreenHelper {

    private MusicScreenHelper() {}

    static Identifier albumEntryId(Album album, String path) {
        return path.contains(":") ? Identifier.parse(path) : Identifier.fromNamespaceAndPath(album.album.getNamespace(), path);
    }

    static Component trackName(Album album, String song) {
        return trackName(album.trackId(song), song);
    }

    static Component trackName(Identifier id) {
        return trackName(id, id.toString());
    }

    static Component trackName(Identifier id, String fallback) {
        String pathKey = id.getPath().replace('/', '.');
        String key = id.getNamespace().equals("minecraft") ? pathKey : id.getNamespace() + "." + pathKey;
        return Component.translatableWithFallback(key, fallback);
    }

    static Component discName(Identifier jukeboxSong) {
        String key = "jukebox_song." + jukeboxSong.getNamespace() + "." + jukeboxSong.getPath().replace('/', '.');
        return Component.translatableWithFallback(key, jukeboxSong.toString());
    }

    static Identifier discItemId(Identifier jukeboxSong) {
        return Identifier.fromNamespaceAndPath(jukeboxSong.getNamespace(), "music_disc_" + jukeboxSong.getPath());
    }

    static boolean isDiscUnlocked(Minecraft minecraft, Identifier jukeboxSong) {
        if (minecraft.player == null || minecraft.player.isCreative()) return true;
        Item item = BuiltInRegistries.ITEM.getValue(discItemId(jukeboxSong));
        if (minecraft.player.getStats().getValue(Stats.ITEM_USED, item) > 0) {
            return true;
        }

        return shouldUseInventoryDiscFallback(minecraft) && minecraft.player.getInventory().contains(stack -> !stack.isEmpty() && stack.getItem() == item);
    }

    private static boolean shouldUseInventoryDiscFallback(Minecraft minecraft) {
        return minecraft.getConnection() != null && (ServerHelper.isAbsent() || !ServerHelper.countDiscUses);
    }

    static void requestStats(Minecraft minecraft) {
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    static Identifier discSoundId(Minecraft minecraft, Identifier jukeboxSongId) {
        Optional<JukeboxSong> jukeboxSong = jukeboxSong(minecraft, jukeboxSongId);
        if (jukeboxSong.isEmpty()) return fallbackDiscSoundId(jukeboxSongId);

        SoundEvent event = jukeboxSong.get().soundEvent().value();
        Identifier eventId = event.location();
        var soundEvent = minecraft.getSoundManager().getSoundEvent(eventId);
        if (soundEvent != null) {
            Sound sound = soundEvent.getSound(SoundInstance.createUnseededRandom());
            if (sound != SoundManager.EMPTY_SOUND) return sound.getLocation();
        }

        return fallbackDiscSoundId(eventId);
    }

    private static Optional<JukeboxSong> jukeboxSong(Minecraft minecraft, Identifier id) {
        var access = minecraft.getConnection() != null
                ? minecraft.getConnection().registryAccess()
                : minecraft.level != null ? minecraft.level.registryAccess() : null;
        if (access == null) return Optional.empty();
        return access.lookup(Registries.JUKEBOX_SONG)
                .flatMap(registry -> registry.getOptional(ResourceKey.create(Registries.JUKEBOX_SONG, id)));
    }

    private static Identifier fallbackDiscSoundId(Identifier id) {
        String path = id.getPath();
        if (path.startsWith("music_disc.")) path = path.substring("music_disc.".length());
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "records/" + path);
    }
}
