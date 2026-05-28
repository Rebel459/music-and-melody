package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.network.ServerHelper;

import java.util.Optional;

public final class MusicDiscHelper {

    private MusicDiscHelper() {}

    public static Identifier albumEntryId(Album album, String path) {
        return path.contains(":") ? Identifier.parse(path) : Identifier.fromNamespaceAndPath(album.album.getNamespace(), path);
    }

    public static Identifier albumEntryId(Album album, Album.StoredDisc disc) {
        return albumEntryId(album, disc.path());
    }

    public static Optional<Match> matchSound(Minecraft minecraft, SafeIdentifier soundId) {
        for (Album album : Album.ALBUMS) {
            for (Album.StoredDisc disc : album.discs) {
                Identifier jukeboxSong = albumEntryId(album, disc);
                Optional<Identifier> sound = discSoundId(minecraft, album, disc);
                if (sound.isPresent() && sound.get().equals(soundId.getId())) {
                    return Optional.of(new Match(album, disc.path(), jukeboxSong));
                }
            }
        }
        for (Playlist playlist : Playlist.PLAYLISTS) {
            for (Identifier disc : playlist.discs) {
                Optional<Identifier> sound = discSoundId(minecraft, disc);
                if (sound.isPresent() && sound.get().equals(soundId.getId())) {
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

    public static Component discName(Identifier jukeboxSong) {
        return Component.translatableWithFallback(translationKey(jukeboxSong), jukeboxSong.toString());
    }

    public static Component discName(Identifier jukeboxSong, Album.StoredDisc disc) {
        return disc.description().orElseGet(() -> discName(jukeboxSong));
    }

    public static String translationKey(Identifier jukeboxSong) {
        return "jukebox_song." + jukeboxSong.getNamespace() + "." + jukeboxSong.getPath().replace('/', '.');
    }

    public static Identifier discItemId(Identifier jukeboxSong) {
        return Identifier.fromNamespaceAndPath(jukeboxSong.getNamespace(), "music_disc_" + jukeboxSong.getPath());
    }

    public static boolean isDiscUnlocked(Minecraft minecraft, Identifier jukeboxSong) {
        if (minecraft.player == null || minecraft.player.isCreative()) return true;
        Identifier discItemId = discItemId(jukeboxSong);
        if (!BuiltInRegistries.ITEM.containsKey(discItemId)) return true;
        Item item = BuiltInRegistries.ITEM.getValue(discItemId);
        if (minecraft.player.getStats().getValue(Stats.ITEM_USED, item) > 0) return true;
        return shouldUseInventoryDiscFallback(minecraft) && minecraft.player.getInventory().contains(stack -> !stack.isEmpty() && stack.getItem() == item);
    }

    public static void requestStats(Minecraft minecraft) {
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    public static Optional<Identifier> discSoundId(Minecraft minecraft, Album album, Album.StoredDisc disc) {
        Optional<Identifier> explicitEvent = disc.soundEvent().map(Identifier::tryParse);
        if (explicitEvent.isPresent()) return soundFromEvent(minecraft, explicitEvent.get());
        return discSoundId(minecraft, albumEntryId(album, disc));
    }

    private static boolean shouldUseInventoryDiscFallback(Minecraft minecraft) {
        return minecraft.getConnection() != null && (ServerHelper.isAbsent() || !ServerHelper.countDiscUses);
    }

    public static Optional<Identifier> discSoundId(Minecraft minecraft, Identifier id) {
        for (Identifier eventId : fallbackSoundEvents(id)) {
            Optional<Identifier> sound = soundFromEvent(minecraft, eventId);
            if (sound.isPresent()) return sound;
        }

        return Optional.empty();
    }

    private static Optional<Identifier> soundFromEvent(Minecraft minecraft, Identifier eventId) {
        var soundEvent = minecraft.getSoundManager().getSoundEvent(eventId);
        if (soundEvent == null) return Optional.empty();
        Sound sound = soundEvent.getSound(SoundInstance.createUnseededRandom());
        if (sound == SoundManager.EMPTY_SOUND) return Optional.empty();
        return Optional.of(sound.getLocation());
    }

    private static Identifier[] fallbackSoundEvents(Identifier jukeboxSongId) {
        String path = jukeboxSongId.getPath();
        return new Identifier[] {
                Identifier.fromNamespaceAndPath(jukeboxSongId.getNamespace(), "music_disc_" + path),
                Identifier.fromNamespaceAndPath(jukeboxSongId.getNamespace(), "music_disc." + path)
        };
    }

    public record Match(Album album, String disc, Identifier jukeboxSong) {}
}
