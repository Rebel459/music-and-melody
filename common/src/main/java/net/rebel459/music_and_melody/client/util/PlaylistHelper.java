package net.rebel459.music_and_melody.client.util;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.SampledFloat;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.sound.MaMSounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class PlaylistHelper {
    public static final String LITERAL_TRANSLATION_PREFIX = "music_and_melody.literal:";
    public static final List<Identifier> QUEUED_SONGS = new ArrayList<>();
    public static final HashMap<Identifier, SampledFloat> STORED_VOLUME = new HashMap<>();
    public static boolean loop = false;
    private static boolean loaded = false;
    private static SoundInstance currentSong = null;
    private static Identifier currentSongId = null;
    private static boolean currentSongFromQueue = false;
    private static boolean currentSongFromEvent = false;
    private static boolean queuePaused = true;
    private static int queueIndex = 0;

    public static final Music EMPTY = new Music(MaMSounds.REGISTERED_SOUNDS.get("music.empty"), 0, 0, true);

    private PlaylistHelper() {}

    public static void add(Identifier song) {
        ensureLoaded();
        if (!QUEUED_SONGS.contains(song)) {
            QUEUED_SONGS.add(song);
            save();
        }
    }

    public static void addAll(Collection<Identifier> songs) {
        ensureLoaded();
        boolean changed = false;
        for (Identifier song : songs) {
            if (!QUEUED_SONGS.contains(song)) {
                QUEUED_SONGS.add(song);
                changed = true;
            }
        }
        if (changed) save();
    }

    public static boolean isQueued(Identifier song) {
        ensureLoaded();
        return QUEUED_SONGS.contains(song);
    }

    public static List<Identifier> queuedSongs() {
        ensureLoaded();
        return List.copyOf(QUEUED_SONGS);
    }

    public static void remove(int index) {
        ensureLoaded();
        if (index >= 0 && index < QUEUED_SONGS.size()) {
            Identifier removed = QUEUED_SONGS.get(index);
            QUEUED_SONGS.remove(index);
            if (index < queueIndex) queueIndex--;
            if (currentSongFromQueue && removed.equals(currentSongId)) stop();
            queueIndex = clampQueueIndex(queueIndex);
            save();
        }
    }

    public static void clear() {
        ensureLoaded();
        if (QUEUED_SONGS.isEmpty()) return;
        if (currentSongFromQueue) stop();
        QUEUED_SONGS.clear();
        queueIndex = 0;
        queuePaused = true;
        save();
    }

    public static boolean shuffleQueue() {
        ensureLoaded();
        if (QUEUED_SONGS.size() < 2) return false;
        Collections.shuffle(QUEUED_SONGS);
        queueIndex = 0;
        save();
        return true;
    }

    public static boolean isPlaying(Identifier song) {
        return song.equals(currentSongId) && isPlaying();
    }

    public static boolean isQueuePlaying(Identifier song) {
        return currentSongFromQueue && isPlaying(song);
    }

    public static boolean hasQueuedSongs() {
        ensureLoaded();
        return !QUEUED_SONGS.isEmpty();
    }

    public static boolean isLoopingQueue() {
        ensureLoaded();
        return loop;
    }

    public static void setLoopingQueue(boolean looping) {
        ensureLoaded();
        if (loop != looping) {
            loop = looping;
            save();
        }
    }

    public static boolean isPlaying() {
        return currentSong != null && Minecraft.getInstance().getSoundManager().isActive(currentSong);
    }

    public static boolean isQueuePlaying() {
        return currentSongFromQueue && isPlaying();
    }

    public static boolean isEventPlaying() {
        return currentSongFromEvent && isPlaying();
    }

    public static SoundInstance getCurrentQueueSound() {
        return isQueuePlaying() ? currentSong : null;
    }

    public static void interruptCurrentPlayback(SoundInstance sound) {
        if (!currentSongFromQueue || currentSong == null) return;
        if (sound != null && sound != currentSong) return;
        currentSong = null;
        currentSongId = null;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
    }

    public static String getCurrentMusicTranslationKey() {
        return getMusicTranslationKey(currentSong);
    }

    public static String getMusicTranslationKey(SoundInstance sound) {
        if (sound == null || sound != currentSong || !isPlaying() || currentSongId == null) return null;
        Identifier displayId = currentSongId;
        Sound currentSound = currentSong.getSound();
        if (currentSound != null && currentSound != SoundManager.EMPTY_SOUND && currentSound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            displayId = currentSound.getLocation();
        }
        String configName = ConfigAlbum.displayName(displayId);
        if (configName != null) return LITERAL_TRANSLATION_PREFIX + configName;
        var disc = MusicDiscHelper.matchSound(Minecraft.getInstance(), displayId);
        if (disc.isPresent()) return MusicDiscHelper.translationKey(disc.get().jukeboxSong());
        String pathKey = displayId.getPath().replace('/', '.');
        return displayId.getNamespace().equals("minecraft") ? pathKey : displayId.getNamespace() + "." + pathKey;
    }

    public static boolean playNext() {
        ensureLoaded();
        advanceFinishedQueuedSong();
        if (queuePaused || QUEUED_SONGS.isEmpty() || hasActiveMusic()) return false;
        int playableIndex = nextPlayableIndex(queueIndex, loop);
        if (playableIndex < 0) {
            queuePaused = true;
            return false;
        }
        queueIndex = playableIndex;
        Identifier id = QUEUED_SONGS.get(queueIndex);
        return playSound(id, false, true, false);
    }

    public static boolean playNextNow() {
        ensureLoaded();
        advanceFinishedQueuedSong();
        if (QUEUED_SONGS.isEmpty()) return false;
        queuePaused = false;
        queueIndex = clampQueueIndex(queueIndex);
        int playableIndex = nextPlayableIndex(queueIndex, true);
        if (playableIndex < 0) return false;
        queueIndex = playableIndex;
        Identifier id = QUEUED_SONGS.get(queueIndex);
        stop();
        return playSound(id, false, true, false);
    }

    public static boolean playNow(int index) {
        ensureLoaded();
        if (index < 0 || index >= QUEUED_SONGS.size()) return false;
        if (!MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), QUEUED_SONGS.get(index))) return false;
        queuePaused = false;
        queueIndex = index;
        stop();
        return playSound(QUEUED_SONGS.get(queueIndex), false, true, false);
    }

    private static void advanceFinishedQueuedSong() {
        if (!currentSongFromQueue || currentSongId == null || isPlaying()) return;
        int finishedIndex = QUEUED_SONGS.indexOf(currentSongId);
        if (finishedIndex >= 0) {
            int nextIndex = finishedIndex + 1;
            if (nextIndex >= QUEUED_SONGS.size()) {
                if (loop) {
                    nextIndex = 0;
                } else {
                    nextIndex = 0;
                    queuePaused = true;
                }
            }
            queueIndex = nextIndex;
        }
        currentSong = null;
        currentSongId = null;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
    }

    public static boolean hasActiveMusic() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        if (instances.isEmpty()) return false;
        for (SoundInstance instance : instances) {
            if (isEmptyMusic(instance)) continue;
            if (manager.isActive(instance)) return true;
        }
        return false;
    }

    public static boolean isEmptyMusic(SoundInstance instance) {
        return instance != null && instance.getIdentifier().equals(MaMSounds.REGISTERED_SOUNDS.get("music.empty").value().location());
    }

    private static int clampQueueIndex(int index) {
        if (QUEUED_SONGS.isEmpty()) return 0;
        return Math.max(0, Math.min(index, QUEUED_SONGS.size() - 1));
    }

    private static int nextPlayableIndex(int start, boolean wrap) {
        if (QUEUED_SONGS.isEmpty()) return -1;
        int index = clampQueueIndex(start);
        for (int checked = 0; checked < QUEUED_SONGS.size(); checked++) {
            if (MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), QUEUED_SONGS.get(index))) return index;
            index++;
            if (index >= QUEUED_SONGS.size()) {
                if (!wrap) return -1;
                index = 0;
            }
        }
        return -1;
    }

    private static boolean playSound(Identifier id, boolean loop, boolean fromQueue, boolean fromEvent) {
        id = ConfigAlbum.playableId(id);
        SampledFloat sampledVolume = STORED_VOLUME.get(id);
        float volume = 1.0F;
        RandomSource random = SoundInstance.createUnseededRandom();
        if (sampledVolume != null) volume = sampledVolume.sample(random);
        currentSongId = id;
        currentSongFromQueue = fromQueue;
        currentSongFromEvent = fromEvent;
        currentSong = new DirectSoundInstance(
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
        SoundEngine.PlayResult result = Minecraft.getInstance().getSoundManager().play(currentSong);
        if (result == SoundEngine.PlayResult.NOT_STARTED) {
            currentSong = null;
            currentSongId = null;
            currentSongFromQueue = false;
            currentSongFromEvent = false;
            return false;
        }
        if (result == SoundEngine.PlayResult.STARTED) {
            Minecraft.getInstance().getToastManager().showNowPlayingToast();
        }
        return true;
    }

    public static void stop() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        if (instances != null) {
            for(SoundInstance instance : instances) {
                manager.stop(instance);
            }
        }
        currentSong = null;
        currentSongId = null;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
    }

    public static boolean play(Identifier id, boolean loop) {
        ensureLoaded();
        stop();
        return playSound(id, loop, false, false);
    }

    public static boolean playEvent(Identifier id, boolean loop) {
        ensureLoaded();
        stop();
        return playSound(id, loop, false, true);
    }

    public static void stopEvent() {
        if (currentSongFromEvent) {
            stop();
        }
    }

    public static void pauseQueue() {
        ensureLoaded();
        stop();
        queuePaused = true;
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        QUEUED_SONGS.clear();
        queuePaused = true;
        MaMDataConfig config = MaMDataConfig.get();
        loop = config.playlists.loop;
        for (String song : config.playlists.queued_songs) {
            Identifier id = Identifier.tryParse(song);
            if (id != null && !QUEUED_SONGS.contains(id)) QUEUED_SONGS.add(id);
        }
    }

    private static void save() {
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.loop = loop;
        config.playlists.queued_songs = new ArrayList<>(QUEUED_SONGS.stream().map(Identifier::toString).toList());
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static class DirectSoundInstance extends AbstractSoundInstance {

        private final WeighedSoundEvents soundEvent;

        DirectSoundInstance(
                Identifier location,
                SoundSource source,
                float volume,
                float pitch,
                RandomSource random,
                boolean looping,
                int delay,
                SoundInstance.Attenuation attenuation,
                double x,
                double y,
                double z,
                boolean relative
        ) {
            super(location, source, random);
            this.volume = volume;
            this.pitch = pitch;
            this.x = x;
            this.y = y;
            this.z = z;
            this.looping = looping;
            this.delay = delay;
            this.attenuation = attenuation;
            this.relative = relative;
            this.sound = new Sound(location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
            this.soundEvent = new WeighedSoundEvents(location, null);
            this.soundEvent.addSound(this.sound);
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager soundManager) {
            WeighedSoundEvents registered = soundManager.getSoundEvent(this.identifier);
            if (registered != null) {
                this.sound = registered.getSound(this.random);
                return registered;
            }
            return this.soundEvent;
        }
    }
}
