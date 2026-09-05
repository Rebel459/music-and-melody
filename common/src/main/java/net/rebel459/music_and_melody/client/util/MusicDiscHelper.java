package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.locale.Language;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.network.ServerPresenceHandler;

import java.util.Optional;

public final class MusicDiscHelper {

    private MusicDiscHelper() {}

    public static Identifier albumEntryId(Album album, String path) {
        if (album.album.equals(CustomAlbums.MOD_DISCS)) return Identifier.parse(path);
        return Identifier.fromNamespaceAndPath(album.album.getNamespace(), path);
    }

    public static Identifier albumEntryId(Album album, Album.StoredDisc disc) {
        return albumEntryId(album, disc.path());
    }

    public static Component discName(Identifier jukeboxSong) {
        return Component.translatableWithFallback(translationKey(jukeboxSong), jukeboxSong.toString());
    }

    public static Component discName(Identifier jukeboxSong, Album.StoredDisc disc) {
        return disc.description().orElseGet(() -> discName(jukeboxSong));
    }

    public static String translationKey(Identifier jukeboxSong) {
        String path = jukeboxSong.getPath().replace('/', '.');
        String jukeboxKey = "jukebox_song." + jukeboxSong.getNamespace() + "." + path;
        if (Language.getInstance().has(jukeboxKey)) return jukeboxKey;
        String itemKey = "item." + jukeboxSong.getNamespace() + ".music_disc_" + path + ".desc";
        return Language.getInstance().has(itemKey) ? itemKey : jukeboxKey;
    }

    public static Identifier discItemId(Identifier jukeboxSong) {
        return Identifier.fromNamespaceAndPath(jukeboxSong.getNamespace(), "music_disc_" + jukeboxSong.getPath());
    }

    public static boolean isDiscUnlocked(Minecraft minecraft, Identifier jukeboxSong) {
        if (ServerPresenceHandler.discUnlocking) {
            if (minecraft.player == null || minecraft.player.isCreative()) return true;
            Identifier discItemId = discItemId(jukeboxSong);
            if (!BuiltInRegistries.ITEM.containsKey(discItemId)) return true;
            return minecraft.player.getStats().getValue(Stats.ITEM_USED, BuiltInRegistries.ITEM.getValue(discItemId)) > 0;
        }
        return true;
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
}
