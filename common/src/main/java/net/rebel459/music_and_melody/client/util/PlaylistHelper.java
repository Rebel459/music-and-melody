package net.rebel459.music_and_melody.client.util;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
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
    public static final List<SafeIdentifier> QUEUED_SONGS = new ArrayList<>();
    public static final HashMap<SafeIdentifier, SampledFloat> STORED_VOLUME = new HashMap<>();
    public static boolean loop = false;
    private static boolean loaded = false;

    /**
     * The visible queue is deliberately never shuffled.  This list is the
     * independent traversal order used while shuffle mode is enabled.
     */
    private static final List<SafeIdentifier> SHUFFLE_ORDER = new ArrayList<>();
    private static boolean shuffle = false;
    private static int shuffleIndex = 0;
    private static int currentShuffleIndex = -1;

    private static SoundInstance currentSong = null;
    private static SafeIdentifier currentSongId = null;
    private static boolean currentSongLooping = false;
    private static boolean currentSongFromQueue = false;
    private static boolean currentSongFromEvent = false;
    private static DirectSoundInstance.Type currentSongType = DirectSoundInstance.Type.ALL;
    private static SafeIdentifier directSongId = null;
    private static boolean directSongLooping = false;
    private static boolean stoppingCurrentSong = false;
    private static boolean queuePaused = true;
    private static int queueIndex = 0;
    private static int currentQueueIndex = -1;
    private static long currentSongStartedAtNanos = 0L;
    private static SafeIdentifier pausedQueueSong;
    private static SoundInstance pausedQueueSound;
    private static int pausedQueueIndex = -1;
    private static int pausedShuffleIndex = -1;
    private static long pausedQueueElapsedMillis;
    /** Offset requested for the next decoded stream, consumed by SoundBufferLibraryMixin. */
    private static long pendingSeekMillis = 0L;

    public static final Music EMPTY = new Music(MaMSounds.EMPTY, 0, 0, true);

    private PlaylistHelper() {}

    public static void add(SafeIdentifier song) {
        ensureLoaded();
        if (!QUEUED_SONGS.contains(song)) {
            clearQueueSource();
            QUEUED_SONGS.add(song);
            rebuildShuffleOrderAfterQueueChange();
            save();
        }
    }

    public static void addAll(Collection<SafeIdentifier> songs) {
        ensureLoaded();
        boolean changed = false;
        for (SafeIdentifier song : songs) {
            if (!QUEUED_SONGS.contains(song)) {
                QUEUED_SONGS.add(song);
                changed = true;
            }
        }
        if (changed) clearQueueSource();
        if (changed) rebuildShuffleOrderAfterQueueChange();
        if (changed) save();
    }

    public static void setQueueSource(MaMDataConfig.QueueSourceType type, String id, String name) {
        ensureLoaded();
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.queue_source_type = type;
        config.playlists.queue_source_id = id;
        config.playlists.queue_source_name = name;
        touchRecentSource(type, id);
        save();
    }

    /** Replaces the queue without turning the loaded album or playlist into a custom list. */
    public static boolean loadQueueSource(Collection<SafeIdentifier> songs, MaMDataConfig.QueueSourceType type, String id, String name) {
        ensureLoaded();
        if (songs == null) return false;

        if (currentSongFromQueue) stop();
        else clearPausedQueue();
        QUEUED_SONGS.clear();
        for (SafeIdentifier song : songs) {
            if (song != null && !QUEUED_SONGS.contains(song)) {
                QUEUED_SONGS.add(song);
            }
        }
        queueIndex = 0;
        currentQueueIndex = -1;
        queuePaused = true;
        currentShuffleIndex = -1;
        rebuildShuffleOrder(null);

        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.queue_source_type = type;
        config.playlists.queue_source_id = id;
        config.playlists.queue_source_name = name;
        touchRecentSource(type, id);
        save();
        return !QUEUED_SONGS.isEmpty();
    }

    /**
     * Opens the editable Custom Playlist from an explicit song list.  Unlike
     * {@link #loadQueueSource(Collection, MaMDataConfig.QueueSourceType, String, String)},
     * this intentionally has no album or playlist source identity.
     */
    public static boolean loadCustomQueue(Collection<SafeIdentifier> songs) {
        ensureLoaded();
        if (songs == null) return false;

        if (currentSongFromQueue) stop();
        else clearPausedQueue();
        QUEUED_SONGS.clear();
        for (SafeIdentifier song : songs) {
            if (song != null && !QUEUED_SONGS.contains(song)) {
                QUEUED_SONGS.add(song);
            }
        }
        queueIndex = 0;
        currentQueueIndex = -1;
        queuePaused = true;
        currentShuffleIndex = -1;
        clearQueueSource();
        rebuildShuffleOrder(null);
        save();
        return !QUEUED_SONGS.isEmpty();
    }

    public static Optional<QueueSource> queueSource() {
        MaMDataConfig.Playlists playlists = MaMDataConfig.get().playlists;
        if (playlists.queue_source_type == MaMDataConfig.QueueSourceType.NONE || playlists.queue_source_name.isBlank()) return Optional.empty();
        return Optional.of(new QueueSource(playlists.queue_source_type, playlists.queue_source_id, playlists.queue_source_name));
    }

    public static boolean isQueued(SafeIdentifier song) {
        ensureLoaded();
        return QUEUED_SONGS.contains(song);
    }

    public static List<SafeIdentifier> queuedSongs() {
        ensureLoaded();
        return List.copyOf(QUEUED_SONGS);
    }

    public static boolean isQueueCustom() {
        return queueSource().isEmpty();
    }

    public static boolean move(int fromIndex, int toIndex) {
        ensureLoaded();
        if (fromIndex < 0 || fromIndex >= QUEUED_SONGS.size()
                || toIndex < 0 || toIndex >= QUEUED_SONGS.size()
                || fromIndex == toIndex) {
            return false;
        }

        SafeIdentifier moved = QUEUED_SONGS.remove(fromIndex);
        QUEUED_SONGS.add(toIndex, moved);
        queueIndex = remapMovedIndex(queueIndex, fromIndex, toIndex);
        currentQueueIndex = remapMovedIndex(currentQueueIndex, fromIndex, toIndex);
        pausedQueueIndex = remapMovedIndex(pausedQueueIndex, fromIndex, toIndex);
        clearQueueSource();
        rebuildShuffleOrderAfterQueueChange();
        save();
        return true;
    }

    public static void remove(int index) {
        ensureLoaded();
        if (index >= 0 && index < QUEUED_SONGS.size()) {
            SafeIdentifier removed = QUEUED_SONGS.get(index);
            QUEUED_SONGS.remove(index);
            if (index < queueIndex) queueIndex--;
            if (index < currentQueueIndex) currentQueueIndex--;
            if (index == pausedQueueIndex) clearPausedQueue();
            else if (index < pausedQueueIndex) pausedQueueIndex--;
            if (currentSongFromQueue && DirectSoundFiles.samePlayable(removed, currentSongId)) stop();
            queueIndex = clampQueueIndex(queueIndex);
            clearQueueSource();
            rebuildShuffleOrderAfterQueueChange();
            save();
        }
    }

    public static void clear() {
        ensureLoaded();
        if (QUEUED_SONGS.isEmpty()) return;
        if (currentSongFromQueue) stop();
        QUEUED_SONGS.clear();
        queueIndex = 0;
        currentQueueIndex = -1;
        queuePaused = true;
        clearPausedQueue();
        currentShuffleIndex = -1;
        SHUFFLE_ORDER.clear();
        shuffleIndex = 0;
        clearQueueSource();
        save();
    }

    public static boolean shuffleQueue() {
        ensureLoaded();
        if (QUEUED_SONGS.size() < 2) return false;
        setShuffleQueue(!shuffle);
        return true;
    }

    public static boolean isShuffleQueue() {
        ensureLoaded();
        return shuffle;
    }

    public static void setShuffleQueue(boolean enabled) {
        ensureLoaded();
        if (shuffle == enabled) return;
        shuffle = enabled;
        if (shuffle) {
            SafeIdentifier current = currentSongFromQueue && currentQueueIndex >= 0
                    ? QUEUED_SONGS.get(currentQueueIndex)
                    : null;
            rebuildShuffleOrder(current);
        } else {
            SHUFFLE_ORDER.clear();
            shuffleIndex = 0;
            currentShuffleIndex = -1;
        }
        save();
    }

    public static boolean isPlaying(SafeIdentifier song) {
        if (song == null || currentSong == null || currentSongId == null || !isPlaying()) {
            return false;
        }

        SafeIdentifier wanted = ConfigAlbum.playableId(song);
        SafeIdentifier current = ConfigAlbum.playableId(currentSongId);
        SafeIdentifier soundInstanceId = SafeIdentifier.convert(currentSong.getIdentifier());

        if (wanted.equals(current)) return true;
        if (wanted.equals(soundInstanceId)) return true;

        return DirectSoundFiles.samePlayable(wanted, current)
                || DirectSoundFiles.samePlayable(wanted, soundInstanceId);
    }

    public static boolean isQueuePlaying(SafeIdentifier song) {
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

    public static boolean hasPreviousQueue() {
        ensureLoaded();
        if (QUEUED_SONGS.size() < 2) return false;
        if (shuffle) {
            return currentShuffleIndex > 0 || (!isQueuePlaying() && shuffleIndex > 0);
        }
        int index = currentSongFromQueue && currentQueueIndex >= 0 ? currentQueueIndex : queueIndex;
        return loop || index > 0;
    }

    public static boolean previousQueue() {
        ensureLoaded();
        if (!hasPreviousQueue()) return false;
        queuePaused = false;

        if (shuffle) {
            ensureShuffleOrder();
            int previous = currentShuffleIndex >= 0 ? currentShuffleIndex - 1 : shuffleIndex - 1;
            if (previous < 0) {
                if (!loop || SHUFFLE_ORDER.isEmpty()) return false;
                previous = SHUFFLE_ORDER.size() - 1;
            }
            SafeIdentifier id = SHUFFLE_ORDER.get(previous);
            int visibleIndex = QUEUED_SONGS.indexOf(id);
            if (visibleIndex < 0 || !MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), id)) return false;
            shuffleIndex = previous + 1;
            currentShuffleIndex = previous;
            queueIndex = visibleIndex;
            stop();
            return playSound(id, false, true, false);
        }

        int index = currentSongFromQueue && currentQueueIndex >= 0 ? currentQueueIndex : queueIndex;
        index--;
        if (index < 0) index = QUEUED_SONGS.size() - 1;
        int playableIndex = previousPlayableIndex(index, loop);
        if (playableIndex < 0) return false;
        queueIndex = playableIndex;
        stop();
        return playSound(QUEUED_SONGS.get(queueIndex), false, true, false);
    }

    public static boolean isPlaying() {
        return currentSong != null && Minecraft.getInstance().getSoundManager().isActive(currentSong);
    }

    public static boolean isQueuePlaying() {
        return currentSongFromQueue && !queuePaused && isPlaying();
    }

    public static boolean isDirectPlaying() {
        return currentSong != null && !currentSongFromQueue && !currentSongFromEvent && isPlaying();
    }

    public static boolean hasDirectSong() {
        return directSongId != null;
    }

    public static SafeIdentifier getDirectSongId() {
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

    public static SafeIdentifier getCurrentSongId() {
        return currentSongId != null ? currentSongId : pausedQueueSong;
    }

    public static boolean isEventPlaying() {
        return currentSongFromEvent && isPlaying();
    }

    public static boolean isPlaylistOrAlbumPlaying() {
        return currentSong != null && !currentSongFromEvent && isPlaying();
    }

    public static SoundInstance getCurrentSong() {
        return currentSong != null ? currentSong : pausedQueueSound;
    }

    /**
     * Reopens the current stream and discards decoded samples until {@code millis}.
     * This is deliberately restart-at-offset seeking: it is dependable for both
     * resource-pack and direct OGG files without a new audio dependency.
     */
    public static boolean seekCurrentSong(long millis) {
        // SoundEngine can report a streaming source as inactive for the few
        // ticks between creating its channel and attaching the stream. The
        // player still has a valid song at that point, so do not reject a UI
        // seek solely because of that transient engine state.
        if (currentSong == null || currentSongId == null || currentSongStartedAtNanos == 0L) return false;

        SafeIdentifier id = currentSongId;
        boolean looping = currentSongLooping;
        boolean fromQueue = currentSongFromQueue;
        boolean fromEvent = currentSongFromEvent;
        DirectSoundInstance.Type type = currentSongType;
        long offset = Math.max(0L, millis);

        thisStopCurrentSongOnly();
        pendingSeekMillis = offset;
        return playSound(id, looping, fromQueue, fromEvent, type);
    }

    /** Called by the stream factory exactly once for the matching next sound. */
    public static long consumePendingSeekMillis(Identifier streamLocation) {
        if (pendingSeekMillis <= 0L || streamLocation == null || currentSong == null) return 0L;
        Sound sound = currentSong.getSound();
        if (sound == null || sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND) return 0L;

        boolean matches = DirectSoundFiles.sameStreamResource(streamLocation, sound.getPath())
                || DirectSoundFiles.sameStreamResource(streamLocation, sound.getLocation())
                || DirectSoundFiles.sameStreamResource(streamLocation, currentSong.getIdentifier());
        if (!matches) return 0L;

        long offset = pendingSeekMillis;
        pendingSeekMillis = 0L;
        return offset;
    }

    public static void interruptCurrentPlayback(SoundInstance sound) {
        if ((!currentSongFromQueue && !currentSongFromEvent) || currentSong == null) return;
        if (sound != null && sound != currentSong) return;
        if (sound != null && currentSongFromQueue) {
            if (!stoppingCurrentSong) {
                advanceFinishedQueuedSong(false);
            }
            return;
        }
        currentSong = null;
        currentSongId = null;
        currentSongLooping = false;
        currentSongType = DirectSoundInstance.Type.ALL;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
        currentQueueIndex = -1;
        currentSongStartedAtNanos = 0L;
    }

    public static String getCurrentMusicTranslationKey() {
        return getMusicTranslationKey(currentSong);
    }

    public static String getMusicTranslationKey(SoundInstance sound) {
        if (sound == null || sound != currentSong || !isPlaying() || currentSongId == null) return null;
        SafeIdentifier displayId = currentSongId;
        Sound currentSound = currentSong.getSound();
        if (currentSound != null && currentSound != SoundManager.EMPTY_SOUND && currentSound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            Optional<String> directName = DirectSoundFiles.getName(currentSound.getLocation());
            if (directName.isPresent()) return directName.get();
            displayId = SafeIdentifier.convert(currentSound.getLocation());
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
        if (shuffle) return playNextShuffled();
        int playableIndex = nextPlayableIndex(queueIndex, loop);
        if (playableIndex < 0) {
            queuePaused = true;
            return false;
        }
        queueIndex = playableIndex;
        SafeIdentifier id = QUEUED_SONGS.get(queueIndex);
        return playSound(id, false, true, false);
    }

    public static boolean playNextNow() {
        ensureLoaded();
        if (QUEUED_SONGS.isEmpty()) return false;
        if (resumeQueue()) return true;
        queuePaused = false;
        if (currentSongFromQueue && currentSongId != null && isPlaying()) {
            if (shuffle) {
                return skipQueue();
            }
            queueIndex = nextQueueIndex(currentQueueIndex >= 0 ? currentQueueIndex : queueIndex);
        } else {
            advanceFinishedQueuedSong(true);
        }
        if (shuffle) {
            if (currentSong != null && isPlaying()) stop();
            return playNextShuffled();
        }
        queueIndex = clampQueueIndex(queueIndex);
        int playableIndex = nextPlayableIndex(queueIndex, true);
        if (playableIndex < 0) return false;
        queueIndex = playableIndex;
        SafeIdentifier id = QUEUED_SONGS.get(queueIndex);
        stop();
        return playSound(id, false, true, false);
    }

    public static boolean canSkipQueue() {
        ensureLoaded();
        if (QUEUED_SONGS.size() < 2) return false;
        if (shuffle) {
            ensureShuffleOrder();
            if (loop) return SHUFFLE_ORDER.stream().anyMatch(id -> MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), id));
            return hasUnlockedShuffleSongFrom(shuffleIndex);
        }
        int index = currentSongFromQueue && currentQueueIndex >= 0 ? currentQueueIndex : queueIndex;
        return loop || index < QUEUED_SONGS.size() - 1;
    }

    public static boolean skipQueue() {
        ensureLoaded();
        if (!canSkipQueue()) return false;
        queuePaused = false;
        if (shuffle) {
            ensureShuffleOrder();
            int shuffledIndex = nextShuffledPlayableIndex();
            if (shuffledIndex < 0) return false;
            SafeIdentifier id = SHUFFLE_ORDER.get(shuffledIndex);
            int visibleIndex = QUEUED_SONGS.indexOf(id);
            if (visibleIndex < 0) return false;
            currentShuffleIndex = shuffledIndex;
            queueIndex = visibleIndex;
            stop();
            return playSound(id, false, true, false);
        }
        int index = currentSongFromQueue && currentQueueIndex >= 0 ? currentQueueIndex : queueIndex;
        queueIndex = index + 1 >= QUEUED_SONGS.size() ? 0 : index + 1;
        int playableIndex = nextPlayableIndex(queueIndex, loop);
        if (playableIndex < 0) return false;
        queueIndex = playableIndex;
        SafeIdentifier id = QUEUED_SONGS.get(queueIndex);
        stop();
        return playSound(id, false, true, false);
    }

    public static boolean playNow(int index) {
        ensureLoaded();
        if (index < 0 || index >= QUEUED_SONGS.size()) return false;
        if (!MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), QUEUED_SONGS.get(index))) return false;
        queuePaused = false;
        queueIndex = index;
        if (shuffle) {
            rebuildShuffleOrder(QUEUED_SONGS.get(index));
        }
        stop();
        return playSound(QUEUED_SONGS.get(queueIndex), false, true, false);
    }

    private static void advanceFinishedQueuedSong(boolean requireInactive) {
        if (!currentSongFromQueue || currentSongId == null || (requireInactive && isPlaying())) return;
        int finishedIndex = currentQueueIndex >= 0 && currentQueueIndex < QUEUED_SONGS.size()
                ? currentQueueIndex
                : findCurrentQueueIndex();
        if (finishedIndex >= 0) {
            if (!shuffle) queueIndex = nextQueueIndex(finishedIndex);
        }
        currentSong = null;
        currentSongId = null;
        currentSongLooping = false;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
        currentQueueIndex = -1;
        currentSongStartedAtNanos = 0L;
    }

    private static int findCurrentQueueIndex() {
        for (int i = 0; i < QUEUED_SONGS.size(); i++) {
            if (DirectSoundFiles.samePlayable(QUEUED_SONGS.get(i), currentSongId)) return i;
        }
        return -1;
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

    private static int previousPlayableIndex(int start, boolean wrap) {
        if (QUEUED_SONGS.isEmpty()) return -1;
        int index = clampQueueIndex(start);
        for (int checked = 0; checked < QUEUED_SONGS.size(); checked++) {
            if (MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), QUEUED_SONGS.get(index))) return index;
            index--;
            if (index < 0) {
                if (!wrap) return -1;
                index = QUEUED_SONGS.size() - 1;
            }
        }
        return -1;
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
        return instance != null && instance.getIdentifier().toString().equals(MusicAndMelody.MOD_ID + ":music.empty");
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

    private static boolean playSound(SafeIdentifier id, boolean loop, boolean fromQueue, boolean fromEvent) {
        return playSound(id, loop, fromQueue, fromEvent, DirectSoundInstance.Type.ALL);
    }

    private static boolean playSound(SafeIdentifier id, boolean loop, boolean fromQueue, boolean fromEvent, DirectSoundInstance.Type type) {
        id = ConfigAlbum.playableId(id);
        SampledFloat sampledVolume = STORED_VOLUME.get(id);
        float volume = 1.0F;
        RandomSource random = SoundInstance.createUnseededRandom();
        if (sampledVolume != null) volume = sampledVolume.sample(random);
        currentSongId = id;
        currentSongLooping = loop;
        currentSongType = type;
        currentSongFromQueue = fromQueue;
        currentSongFromEvent = fromEvent;
        currentQueueIndex = fromQueue ? queueIndex : -1;
        currentSongStartedAtNanos = System.nanoTime() - pendingSeekMillis * 1_000_000L;
        if (!fromQueue && !fromEvent) {
            directSongId = id;
            directSongLooping = loop;
        } else {
            directSongId = null;
            directSongLooping = false;
        }
        if (type == DirectSoundInstance.Type.TRACKS) currentSong = DirectSoundInstance.createTracksOnly(id, volume, loop);
        else if (type == DirectSoundInstance.Type.POOLS) currentSong = DirectSoundInstance.createPoolsOnly(id, volume, loop);
        else currentSong = new DirectSoundInstance(
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
            currentSongLooping = false;
            currentSongType = DirectSoundInstance.Type.ALL;
            currentSongFromQueue = false;
            currentSongFromEvent = false;
            currentQueueIndex = -1;
            currentShuffleIndex = -1;
            currentSongStartedAtNanos = 0L;
            pendingSeekMillis = 0L;
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
        currentSongType = DirectSoundInstance.Type.ALL;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
        currentQueueIndex = -1;
        currentSongStartedAtNanos = 0L;
        pendingSeekMillis = 0L;
        clearPausedQueue();
    }

    private static void thisStopCurrentSongOnly() {
        SoundInstance song = currentSong;
        if (song != null) {
            stoppingCurrentSong = true;
            try {
                Minecraft.getInstance().getSoundManager().stop(song);
            } finally {
                stoppingCurrentSong = false;
            }
        }
        currentSong = null;
        currentSongId = null;
        currentSongLooping = false;
        currentSongType = DirectSoundInstance.Type.ALL;
        currentSongFromQueue = false;
        currentSongFromEvent = false;
        currentQueueIndex = -1;
        currentSongStartedAtNanos = 0L;
    }

    public static boolean play(SafeIdentifier id, boolean loop) {
        ensureLoaded();
        stop();
        return playSound(id, loop, false, false);
    }

    public static boolean playEvent(SafeIdentifier id, boolean loop) {
        return playEvent(id, loop, DirectSoundInstance.Type.ALL);
    }

    public static boolean playEvent(SafeIdentifier id, boolean loop, DirectSoundInstance.Type type) {
        ensureLoaded();
        stop();
        return playSound(id, loop, false, true, type);
    }

    public static void stopEvent() {
        if (currentSongFromEvent) {
            stop();
        }
    }

    public static void pauseQueue() {
        ensureLoaded();
        if (!currentSongFromQueue || currentSongId == null || !isPlaying()) return;
        long elapsed = currentSongElapsedMillis();
        SoundEngineStopper engine = (SoundEngineStopper) Minecraft.getInstance().getSoundManager().soundEngine;
        if (!engine.pausePlaylist(currentSong)) return;
        pausedQueueElapsedMillis = elapsed;
        queuePaused = true;
    }

    private static boolean resumeQueue() {
        if (queuePaused && currentSongFromQueue && currentSong != null) {
            SoundEngineStopper engine = (SoundEngineStopper) Minecraft.getInstance().getSoundManager().soundEngine;
            if (!engine.resumePlaylist(currentSong)) return false;
            currentSongStartedAtNanos = System.nanoTime() - pausedQueueElapsedMillis * 1_000_000L;
            pausedQueueElapsedMillis = 0L;
            queuePaused = false;
            return true;
        }
        if (pausedQueueSong == null || pausedQueueIndex < 0 || pausedQueueIndex >= QUEUED_SONGS.size()
                || !DirectSoundFiles.samePlayable(QUEUED_SONGS.get(pausedQueueIndex), pausedQueueSong)) {
            clearPausedQueue();
            return false;
        }
        SafeIdentifier song = pausedQueueSong;
        queueIndex = pausedQueueIndex;
        currentShuffleIndex = pausedShuffleIndex;
        if (shuffle && pausedShuffleIndex >= 0) shuffleIndex = pausedShuffleIndex + 1;
        pendingSeekMillis = pausedQueueElapsedMillis;
        clearPausedQueue(false);
        queuePaused = false;
        return playSound(song, false, true, false);
    }

    private static void clearPausedQueue() {
        clearPausedQueue(true);
    }

    private static void clearPausedQueue(boolean clearSeek) {
        pausedQueueSong = null;
        pausedQueueSound = null;
        pausedQueueIndex = -1;
        pausedShuffleIndex = -1;
        pausedQueueElapsedMillis = 0L;
        if (clearSeek) pendingSeekMillis = 0L;
    }

    /** Returns elapsed playback time for the shared player progress display. */
    public static long currentSongElapsedMillis() {
        if (queuePaused && currentSongFromQueue) return pausedQueueElapsedMillis;
        if (currentSong == null) return pausedQueueElapsedMillis;
        if (currentSongStartedAtNanos == 0L) return 0L;
        return Math.max(0L, (System.nanoTime() - currentSongStartedAtNanos) / 1_000_000L);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        QUEUED_SONGS.clear();
        queuePaused = true;
        MaMDataConfig config = MaMDataConfig.get();
        loop = config.playlists.loop;
        shuffle = config.playlists.shuffle;
        for (String song : config.playlists.queued_songs) {
            SafeIdentifier id = SafeIdentifier.parse(song);
            if (id != null && !QUEUED_SONGS.contains(id)) QUEUED_SONGS.add(id);
        }
        if (shuffle) rebuildShuffleOrder(null);
    }

    private static void save() {
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.loop = loop;
        config.playlists.shuffle = shuffle;
        config.playlists.queued_songs = new ArrayList<>(QUEUED_SONGS.stream().map(SafeIdentifier::toString).toList());
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static void clearQueueSource() {
        MaMDataConfig config = MaMDataConfig.get();
        config.playlists.queue_source_type = MaMDataConfig.QueueSourceType.NONE;
        config.playlists.queue_source_id = "";
        config.playlists.queue_source_name = "";
    }

    private static int remapMovedIndex(int index, int fromIndex, int toIndex) {
        if (index < 0) return index;
        if (index == fromIndex) return toIndex;
        if (fromIndex < toIndex && index > fromIndex && index <= toIndex) return index - 1;
        if (toIndex < fromIndex && index >= toIndex && index < fromIndex) return index + 1;
        return index;
    }

    private static void touchRecentSource(MaMDataConfig.QueueSourceType type, String id) {
        if (type == MaMDataConfig.QueueSourceType.NONE || id == null || id.isBlank()) return;
        MaMDataConfig.Playlists playlists = MaMDataConfig.get().playlists;
        String key = type.name() + "|" + id;
        playlists.recent_sources.remove(key);
        playlists.recent_sources.addFirst(key);
    }

    /** Returns {@code 0} for the most recently played source, or a large rank when unseen. */
    public static int recentSourceRank(MaMDataConfig.QueueSourceType type, String id) {
        if (type == MaMDataConfig.QueueSourceType.NONE || id == null) return Integer.MAX_VALUE;
        int index = MaMDataConfig.get().playlists.recent_sources.indexOf(type.name() + "|" + id);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static void rebuildShuffleOrderAfterQueueChange() {
        if (!shuffle) return;
        SafeIdentifier current = currentSongFromQueue && currentQueueIndex >= 0 && currentQueueIndex < QUEUED_SONGS.size()
                ? QUEUED_SONGS.get(currentQueueIndex)
                : null;
        rebuildShuffleOrder(current);
    }

    private static void ensureShuffleOrder() {
        if (!shuffle) return;
        if (SHUFFLE_ORDER.size() != QUEUED_SONGS.size() || !SHUFFLE_ORDER.containsAll(QUEUED_SONGS)) {
            SafeIdentifier current = currentSongFromQueue && currentQueueIndex >= 0 && currentQueueIndex < QUEUED_SONGS.size()
                    ? QUEUED_SONGS.get(currentQueueIndex)
                    : null;
            rebuildShuffleOrder(current);
        }
    }

    /** Starts a new shuffle cycle while preserving the current song at the front when necessary. */
    private static void rebuildShuffleOrder(SafeIdentifier current) {
        SHUFFLE_ORDER.clear();
        List<SafeIdentifier> remaining = new ArrayList<>(QUEUED_SONGS);
        if (current != null && remaining.remove(current)) {
            SHUFFLE_ORDER.add(current);
            currentShuffleIndex = 0;
            shuffleIndex = 1;
        } else {
            currentShuffleIndex = -1;
            shuffleIndex = 0;
        }
        Collections.shuffle(remaining);
        SHUFFLE_ORDER.addAll(remaining);
    }

    private static boolean playNextShuffled() {
        ensureShuffleOrder();
        int shuffledIndex = nextShuffledPlayableIndex();
        if (shuffledIndex < 0) return false;
        SafeIdentifier id = SHUFFLE_ORDER.get(shuffledIndex);
        int visibleIndex = QUEUED_SONGS.indexOf(id);
        if (visibleIndex < 0) return false;
        currentShuffleIndex = shuffledIndex;
        queueIndex = visibleIndex;
        return playSound(id, false, true, false);
    }

    private static int nextShuffledPlayableIndex() {
        int checked = 0;
        while (checked < Math.max(1, QUEUED_SONGS.size())) {
            if (shuffleIndex >= SHUFFLE_ORDER.size()) {
                if (!loop) {
                    queuePaused = true;
                    return -1;
                }
                rebuildShuffleOrder(null);
            }
            if (SHUFFLE_ORDER.isEmpty()) return -1;
            int candidate = shuffleIndex++;
            SafeIdentifier id = SHUFFLE_ORDER.get(candidate);
            if (MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), id)) return candidate;
            checked++;
        }
        return -1;
    }

    private static boolean hasUnlockedShuffleSongFrom(int start) {
        ensureShuffleOrder();
        for (int i = Math.max(0, start); i < SHUFFLE_ORDER.size(); i++) {
            if (MusicDiscHelper.isSoundUnlocked(Minecraft.getInstance(), SHUFFLE_ORDER.get(i))) return true;
        }
        return false;
    }

    public record QueueSource(MaMDataConfig.QueueSourceType type, String id, String name) {}
}
