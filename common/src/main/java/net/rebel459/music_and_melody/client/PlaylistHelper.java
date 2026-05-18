package net.rebel459.music_and_melody.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.SampledFloat;
import net.rebel459.music_and_melody.sound.MaMSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class PlaylistHelper {
    private static final List<Identifier> QUEUED_SONGS = new ArrayList<>();
    public static final HashMap<Identifier, SampledFloat> STORED_VOLUME = new HashMap<>();
    public static boolean loop = false;
    private static SoundInstance currentSong = null;

    public static final Music EMPTY = new Music(MaMSounds.MUSIC_EMPTY, 0, 0, true);

    private PlaylistHelper() {}

    public static void add(Identifier song) {
        if (!QUEUED_SONGS.contains(song)) QUEUED_SONGS.add(song);
    }

    public static boolean isPlaying() {
        return currentSong != null && Minecraft.getInstance().getSoundManager().isActive(currentSong);
    }

    public static boolean playNext() {
        if (QUEUED_SONGS.isEmpty()) return false;
        Identifier id = QUEUED_SONGS.getFirst();
        if (!loop) QUEUED_SONGS.removeFirst();
        playSound(id, false);
        return true;
    }

    private static void playSound(Identifier id, boolean loop) {
        SampledFloat sampledVolume = STORED_VOLUME.get(id);
        float volume = 1.0F;
        RandomSource random = SoundInstance.createUnseededRandom();
        if (sampledVolume != null) volume = sampledVolume.sample(random);
        currentSong = new SimpleSoundInstance(
                id,
                SoundSource.MUSIC,
                volume,
                1.0F,
                SoundInstance.createUnseededRandom(),
                loop,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true
        );
        Minecraft.getInstance().getSoundManager().play(currentSong);
    }

    public static void stop() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for(SoundInstance instance : manager.soundEngine.instanceBySource.get(SoundSource.MUSIC)) {
            manager.stop(instance);
        }
        currentSong = null;
    }

    public static void play(Identifier id, boolean loop) {
        stop();
        playSound(id, loop);
    }
}