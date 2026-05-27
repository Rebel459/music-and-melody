package net.rebel459.music_and_melody.client.util;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.locale.Language;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.SampledFloat;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.sound.MaMSounds;

import java.util.*;

public final class PlaylistHelper {
    public static final String LITERAL_TRANSLATION_PREFIX = "music_and_melody.literal:";
    public static final List<SafeLocation> QUEUED_SONGS = new ArrayList<>();
    public static final HashMap<SafeLocation, SampledFloat> STORED_VOLUME = new HashMap<>();
    public static boolean loop = false;
    private static boolean loaded = false;
    private static SoundInstance currentSong = null;
    private static SafeLocation currentSongId = null;
    private static boolean currentSongLooping = false;
    private static boolean currentSongFromQueue = false;
    private static boolean currentSongFromEvent = false;
    private static SafeLocation directSongId = null;
    private static boolean directSongLooping = false;
    private static boolean stoppingCurrentSong = false;
    private static boolean queuePaused = true;
    private static int queueIndex = 0;

    public static final Music EMPTY = new Music(MaMSounds.REGISTERED_SOUNDS.get("music.empty"), 0, 0, true);

    private PlaylistHelper() {}

    public static void add(SafeLocation song) {
        ensureLoaded();
        if (!QUEUED_SONGS.contains(song)) {
            clearQueueSource();
            QUEUED_SONGS.add(song);
            save();
        }
    }

    public static void addAll(Collection<SafeLocation> songs) {
        ensureLoaded();
        boolean changed = false;
        for (SafeLocation song : songs) {
            if (!QUEUED_SONGS.contains(song)) {
                QUEUED_SONGS.add(song);
                changed = true;
            }
        }
        if (changed) clearQueueSource();
        if (changed) save();
    }

    public static void setQueueSource(MaMDataConfig.QueueSourceType type, String id, String name) {
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.queue_source_type = type;
        config.playlists.queue_source_id = id;
        config.playlists.queue_source_name = name;
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public static Optional<QueueSource> queueSource() {
        MaMDataConfig.Playlists playlists = MaMDataConfig.get().playlists;
        if (playlists.queue_source_type == MaMDataConfig.QueueSourceType.NONE || playlists.queue_source_name.isBlank()) return Optional.empty();
        return Optional.of(new QueueSource(playlists.queue_source_type, playlists.queue_source_id, playlists.queue_source_name));
    }

    public static boolean isQueued(SafeLocation song) {
        ensureLoaded();
        return QUEUED_SONGS.contains(song);
    }

    public static List<SafeLocation> queuedSongs() {
        ensureLoaded();
        return List.copyOf(QUEUED_SONGS);
    }

    public static void remove(int index) {
        ensureLoaded();
        if (index >= 0 && index < QUEUED_SONGS.size()) {
            SafeLocation removed = QUEUED_SONGS.get(index);
            QUEUED_SONGS.remove(index);
            if (index < queueIndex) queueIndex--;
            if (currentSongFromQueue && DirectSoundFiles.samePlayable(removed, currentSongId)) stop();
            queueIndex = clampQueueIndex(queueIndex);
            clearQueueSource();
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
        clearQueueSource();
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

    public static boolean isPlaying(SafeLocation song) {
        if (song == null || currentSong == null || currentSongId == null || !isPlaying()) {
            return false;
        }

        SafeLocation wanted = ConfigAlbum.playableId(song);
        SafeLocation current = ConfigAlbum.playableId(currentSongId);
        SafeLocation soundInstanceId = SafeLocation.convert(currentSong.getLocation());

        if (wanted.equals(current)) return true;
        if (wanted.equals(soundInstanceId)) return true;

        return DirectSoundFiles.samePlayable(wanted, current)
                || DirectSoundFiles.samePlayable(wanted, soundInstanceId);
    }

    public static boolean isQueuePlaying(SafeLocation song) {
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

    public static boolean isDirectPlaying() {
        return currentSong != null && !currentSongFromQueue && !currentSongFromEvent && isPlaying();
    }

    public static boolean hasDirectSong() {
        return directSongId != null;
    }

    public static SafeLocation getDirectSongId() {
        return directSongId;
    }

    public static boolean isDirectSongLooping() {
        return directSongLooping;
    }

    public static void setDirectSongLooping(boolean looping) {
        directSongLooping = looping;
        if (isDirectPlaying() && currentSongId != null) {
            currentSongLooping = looping;
            if (currentSong instanceof DirectSoundInstance directSound) {
                directSound.setLooping(looping);
            }
        }
    }

    public static boolean playDirectSong() {
        if (directSongId == null) return false;
        return play(directSongId, directSongLooping);
    }

    public static void removeDirectSong() {
        if (isDirectPlaying()) {
            stop();
        }
        directSongId = null;
        directSongLooping = false;
    }

    public static SafeLocation getCurrentSongId() {
        return currentSongId;
    }

    public static boolean isEventPlaying() {
        return currentSongFromEvent && isPlaying();
    }

    public static boolean isPlaylistOrAlbumPlaying() {
        return currentSong != null && !currentSongFromEvent && isPlaying();
    }

    public static SoundInstance getCurrentSong() {
        return currentSong;
    }

    public static void interruptCurrentPlayback(SoundInstance sound) {
        if ((!currentSongFromQueue && !currentSongFromEvent) || currentSong == null) return;
        if (sound != null && sound != currentSong) return;
        if (sound != null && currentSongFromQueue && !stoppingCurrentSong) {
            advanceFinishedQueuedSong(false);
            return;
        }
        currentSong = null;
        currentSongId = null;
        currentSongLooping = false;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
    }

    public static String getCurrentMusicTranslationKey() {
        return getMusicTranslationKey(currentSong);
    }

    public static String getMusicTranslationKey(SoundInstance sound) {
        if (sound == null || sound != currentSong || !isPlaying() || currentSongId == null) return null;
        SafeLocation displayId = currentSongId;
        Sound currentSound = currentSong.getSound();
        if (currentSound != null && currentSound != SoundManager.EMPTY_SOUND && currentSound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            Optional<String> directName = DirectSoundFiles.getName(currentSound.getLocation());
            if (directName.isPresent()) return directName.get();
            displayId = SafeLocation.convert(currentSound.getLocation());
        }
        String configName = ConfigAlbum.displayName(displayId);
        if (configName != null) return LITERAL_TRANSLATION_PREFIX + configName;
        var disc = MusicDiscHelper.matchSound(Minecraft.getInstance(), displayId);
        if (disc.isPresent()) return MusicDiscHelper.translationKey(disc.get().jukeboxSong());
        String pathKey = displayId.getPath().replace('/', '.');
        String key = displayId.getNamespace().equals("minecraft") ? pathKey : displayId.getNamespace() + "." + pathKey;
        return Language.getInstance().has(key) ? key : LITERAL_TRANSLATION_PREFIX + fallbackName(displayId.getPath());
    }

    public static boolean playNext() {
        ensureLoaded();
        advanceFinishedQueuedSong(true);
        if (queuePaused || QUEUED_SONGS.isEmpty() || hasActiveMusic()) return false;
        int playableIndex = nextPlayableIndex(queueIndex, loop);
        if (playableIndex < 0) {
            queuePaused = true;
            return false;
        }
        queueIndex = playableIndex;
        SafeLocation id = QUEUED_SONGS.get(queueIndex);
        return playSound(id, false, true, false);
    }

    public static boolean playNextNow() {
        ensureLoaded();
        if (QUEUED_SONGS.isEmpty()) return false;
        queuePaused = false;
        if (currentSongFromQueue && currentSongId != null && isPlaying()) {
            queueIndex = nextQueueIndex(queueIndex);
        } else {
            advanceFinishedQueuedSong(true);
        }
        queueIndex = clampQueueIndex(queueIndex);
        int playableIndex = nextPlayableIndex(queueIndex, true);
        if (playableIndex < 0) return false;
        queueIndex = playableIndex;
        SafeLocation id = QUEUED_SONGS.get(queueIndex);
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

    private static void advanceFinishedQueuedSong(boolean requireInactive) {
        if (!currentSongFromQueue || currentSongId == null || (requireInactive && isPlaying())) return;
        int finishedIndex = -1;
        for (int i = 0; i < QUEUED_SONGS.size(); i++) {
            if (DirectSoundFiles.samePlayable(QUEUED_SONGS.get(i), currentSongId)) {
                finishedIndex = i;
                break;
            }
        }
        if (finishedIndex >= 0) {
            queueIndex = nextQueueIndex(finishedIndex);
        }
        currentSong = null;
        currentSongId = null;
        currentSongLooping = false;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
    }

    private static int nextQueueIndex(int index) {
        int nextIndex = index + 1;
        if (nextIndex >= QUEUED_SONGS.size()) {
            if (loop) {
                nextIndex = 0;
            } else {
                nextIndex = 0;
                queuePaused = true;
            }
        }
        return nextIndex;
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
        return instance != null && instance.getLocation().toString().equals(MusicAndMelody.MOD_ID + ":music.empty");
    }

    private static int clampQueueIndex(int index) {
        if (QUEUED_SONGS.isEmpty()) return 0;
        return Math.max(0, Math.min(index, QUEUED_SONGS.size() - 1));
    }

    private static String fallbackName(String path) {
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.endsWith(".ogg") ? name.substring(0, name.length() - ".ogg".length()) : name;
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

    private static boolean playSound(SafeLocation id, boolean loop, boolean fromQueue, boolean fromEvent) {
        id = ConfigAlbum.playableId(id);
        SampledFloat sampledVolume = STORED_VOLUME.get(id);
        float volume = 1.0F;
        RandomSource random = SoundInstance.createUnseededRandom();
        if (sampledVolume != null) volume = sampledVolume.sample(random);
        currentSongId = id;
        currentSongLooping = loop;
        currentSongFromQueue = fromQueue;
        currentSongFromEvent = fromEvent;
        if (!fromQueue && !fromEvent) {
            directSongId = id;
            directSongLooping = loop;
        } else {
            directSongId = null;
            directSongLooping = false;
        }
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
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        WeighedSoundEvents resolved = currentSong.resolve(soundManager);
        Sound resolvedSound = currentSong.getSound();
        if (resolved == null || resolvedSound == SoundManager.EMPTY_SOUND || resolvedSound == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            currentSong = null;
            currentSongId = null;
            currentSongFromQueue = false;
            currentSongFromEvent = false;
            return false;
        }
        soundManager.play(currentSong);
        return true;
    }

    public static void stop() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        stoppingCurrentSong = true;
        try {
            if (instances != null) {
                for(SoundInstance instance : instances) {
                    manager.stop(instance);
                }
            }
        } finally {
            stoppingCurrentSong = false;
        }
        currentSong = null;
        currentSongId = null;
        currentSongLooping = false;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
    }

    public static boolean play(SafeLocation id, boolean loop) {
        ensureLoaded();
        stop();
        return playSound(id, loop, false, false);
    }

    public static boolean playEvent(SafeLocation id, boolean loop) {
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
            SafeLocation id = SafeLocation.parse(song);
            if (id != null && !QUEUED_SONGS.contains(id)) QUEUED_SONGS.add(id);
        }
    }

    private static void save() {
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.loop = loop;
        config.playlists.queued_songs = new ArrayList<>(QUEUED_SONGS.stream().map(SafeLocation::toString).toList());
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static void clearQueueSource() {
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.queue_source_type = MaMDataConfig.QueueSourceType.NONE;
        config.playlists.queue_source_id = "";
        config.playlists.queue_source_name = "";
    }

    public record QueueSource(MaMDataConfig.QueueSourceType type, String id, String name) {}
}
