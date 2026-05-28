package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.network.ServerHelper;

import java.util.Optional;

public final class MusicDiscHelper {

    private MusicDiscHelper() {}

    public static ResourceLocation albumEntryId(Album album, String path) {
        return path.contains(":") ? ResourceLocation.parse(path) : ResourceLocation.fromNamespaceAndPath(album.album.getNamespace(), path);
    }

    public static ResourceLocation albumEntryId(Album album, Album.StoredDisc disc) {
        return albumEntryId(album, disc.path());
    }

    public static Optional<Match> matchSound(Minecraft minecraft, SafeLocation soundId) {
        for (Album album : Album.ALBUMS) {
            for (Album.StoredDisc disc : album.discs) {
                ResourceLocation jukeboxSong = albumEntryId(album, disc);
                Optional<ResourceLocation> sound = discSoundId(minecraft, album, disc);
                if (sound.isPresent() && sound.get().equals(soundId.getId())) {
                    return Optional.of(new Match(album, disc.path(), jukeboxSong));
                }
            }
        }
        for (Playlist playlist : Playlist.PLAYLISTS) {
            for (ResourceLocation disc : playlist.discs) {
                Optional<ResourceLocation> sound = discSoundId(minecraft, disc);
                if (sound.isPresent() && sound.get().equals(soundId.getId())) {
                    return Optional.of(new Match(null, disc.toString(), disc));
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isSoundUnlocked(Minecraft minecraft, SafeLocation soundId) {
        return matchSound(minecraft, soundId)
                .map(match -> match.album() != null && match.album().isDiscForcedUnlocked(match.disc()) || isDiscUnlocked(minecraft, match.jukeboxSong()))
                .orElse(true);
    }

    public static Component discName(ResourceLocation jukeboxSong) {
        return Component.translatableWithFallback(translationKey(jukeboxSong), jukeboxSong.toString());
    }

    public static Component discName(ResourceLocation jukeboxSong, Album.StoredDisc disc) {
        return disc.description().orElseGet(() -> discName(jukeboxSong));
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

    public static Optional<ResourceLocation> discSoundId(Minecraft minecraft, Album album, Album.StoredDisc disc) {
        Optional<ResourceLocation> explicitEvent = disc.soundEvent().map(ResourceLocation::tryParse);
        if (explicitEvent.isPresent()) return soundFromEvent(minecraft, explicitEvent.get());
        return discSoundId(minecraft, albumEntryId(album, disc));
    }

    private static boolean shouldUseInventoryDiscFallback(Minecraft minecraft) {
        return minecraft.getConnection() != null && (ServerHelper.isAbsent() || !ServerHelper.countDiscUses);
    }

    public static Optional<ResourceLocation> discSoundId(Minecraft minecraft, ResourceLocation id) {
        for (ResourceLocation eventId : fallbackSoundEvents(id)) {
            Optional<ResourceLocation> sound = soundFromEvent(minecraft, eventId);
            if (sound.isPresent()) return sound;
        }

        return Optional.empty();
    }

    private static Optional<ResourceLocation> soundFromEvent(Minecraft minecraft, ResourceLocation eventId) {
        var soundEvent = minecraft.getSoundManager().getSoundEvent(eventId);
        if (soundEvent == null) return Optional.empty();
        Sound sound = soundEvent.getSound(SoundInstance.createUnseededRandom());
        if (sound == SoundManager.EMPTY_SOUND) return Optional.empty();
        return Optional.of(sound.getLocation());
    }

    private static ResourceLocation[] fallbackSoundEvents(ResourceLocation jukeboxSongId) {
        String path = jukeboxSongId.getPath();
        return new ResourceLocation[] {
                ResourceLocation.fromNamespaceAndPath(jukeboxSongId.getNamespace(), "music_disc_" + path),
                ResourceLocation.fromNamespaceAndPath(jukeboxSongId.getNamespace(), "music_disc." + path)
        };
    }

    public record Match(Album album, String disc, ResourceLocation jukeboxSong) {}
}
