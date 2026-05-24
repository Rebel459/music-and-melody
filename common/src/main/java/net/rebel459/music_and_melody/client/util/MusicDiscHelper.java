package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.network.ServerHelper;

import java.util.Optional;

public final class MusicDiscHelper {

    private MusicDiscHelper() {}

    public static ResourceLocation albumEntryId(Album album, String path) {
        return path.contains(":") ? ResourceLocation.parse(path) : ResourceLocation.fromNamespaceAndPath(album.album.getNamespace(), path);
    }

    public static Optional<Match> matchSound(Minecraft minecraft, SafeIdentifier soundId) {
        for (Album album : Album.ALBUMS) {
            for (String disc : album.discs) {
                ResourceLocation jukeboxSong = albumEntryId(album, disc);
                if (discSoundId(minecraft, jukeboxSong).equals(soundId.getId())) {
                    return Optional.of(new Match(album, disc, jukeboxSong));
                }
            }
        }
        for (Playlist playlist : Playlist.PLAYLISTS) {
            for (ResourceLocation disc : playlist.discs) {
                if (discSoundId(minecraft, disc).equals(soundId.getId())) {
                    return Optional.of(new Match(null, disc.toString(), disc));
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isSoundUnlocked(Minecraft minecraft, SafeIdentifier soundId) {
        return matchSound(minecraft, soundId)
                .map(match -> match.album() != null && match.album().isDiscForcedUnlocked(match.disc()) || isDiscUnlocked(minecraft, match.jukeboxSong()))
                .orElse(true);
    }

    public static Component discName(ResourceLocation jukeboxSong) {
        return Component.translatableWithFallback(translationKey(jukeboxSong), jukeboxSong.toString());
    }

    public static String translationKey(ResourceLocation jukeboxSong) {
        return "jukebox_song." + jukeboxSong.getNamespace() + "." + jukeboxSong.getPath().replace('/', '.');
    }

    public static ResourceLocation discItemId(ResourceLocation jukeboxSong) {
        return ResourceLocation.fromNamespaceAndPath(jukeboxSong.getNamespace(), "music_disc_" + jukeboxSong.getPath());
    }

    public static boolean isDiscUnlocked(Minecraft minecraft, ResourceLocation jukeboxSong) {
        if (minecraft.player == null || minecraft.player.isCreative()) return true;
        ResourceLocation discItemId = discItemId(jukeboxSong);
        if (!BuiltInRegistries.ITEM.containsKey(discItemId)) return true;
        Item item = BuiltInRegistries.ITEM.get(discItemId);
        if (minecraft.player.getStats().getValue(Stats.ITEM_USED, item) > 0) return true;
        return shouldUseInventoryDiscFallback(minecraft) && minecraft.player.getInventory().contains(stack -> !stack.isEmpty() && stack.getItem() == item);
    }

    public static void requestStats(Minecraft minecraft) {
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    public static ResourceLocation discSoundId(Minecraft minecraft, ResourceLocation jukeboxSongId) {
        Optional<Holder<JukeboxSong>> jukeboxSong = jukeboxSong(minecraft, jukeboxSongId);
        if (jukeboxSong.isEmpty()) return fallbackDiscSoundId(jukeboxSongId);

        SoundEvent event = jukeboxSong.get().value().soundEvent().value();
        ResourceLocation eventId = event.getLocation();
        var soundEvent = minecraft.getSoundManager().getSoundEvent(eventId);
        if (soundEvent != null) {
            Sound sound = soundEvent.getSound(SoundInstance.createUnseededRandom());
            if (sound != SoundManager.EMPTY_SOUND) return sound.getLocation();
        }

        return fallbackDiscSoundId(eventId);
    }

    private static boolean shouldUseInventoryDiscFallback(Minecraft minecraft) {
        return minecraft.getConnection() != null && (ServerHelper.isAbsent() || !ServerHelper.countDiscUses);
    }

    private static Optional<Holder<JukeboxSong>> jukeboxSong(Minecraft minecraft, ResourceLocation id) {
        var access = minecraft.getConnection() != null
                ? minecraft.getConnection().registryAccess()
                : minecraft.level != null ? minecraft.level.registryAccess() : null;
        if (access == null) return Optional.empty();
        return access.lookup(Registries.JUKEBOX_SONG)
                .flatMap(registry -> registry.get(ResourceKey.create(Registries.JUKEBOX_SONG, id)));
    }

    private static ResourceLocation fallbackDiscSoundId(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("music_disc.")) path = path.substring("music_disc.".length());
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "records/" + path);
    }

    public record Match(Album album, String disc, ResourceLocation jukeboxSong) {}
}
