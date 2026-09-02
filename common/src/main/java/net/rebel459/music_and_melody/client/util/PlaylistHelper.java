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
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.sound.MaMSounds;

import java.util.*;

public final class PlaylistHelper {
    public static final String LITERAL_TRANSLATION_PREFIX = "music_and_melody.literal:";
    public static final List<SafeIdentifier> QUEUED_SONGS = new ArrayList<>();
    private static final Map<SafeIdentifier, Identifier> QUEUED_DISCS = new HashMap<>();
    private static final List<MaMDataConfig.Entry> CUSTOM_PLAYLIST = new ArrayList<>();
    public static final HashMap<SafeIdentifier, SampledFloat> STORED_VOLUME = new HashMap<>();
    public static boolean loop = false;
    private static boolean loaded = false;

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
    private static boolean soundEngineReloading = false;
    private static ReloadPlayback reloadPlayback;
    private static long pendingSeekMillis = 0L;

    public static final Music EMPTY = new Music(MaMSounds.EMPTY, 0, 0, true);

    private PlaylistHelper() {}

    public static void add(SafeIdentifier song) {
        ensureLoaded();
        if (!QUEUED_SONGS.contains(song)) {
            clearQueueType();
            QUEUED_SONGS.add(song);
            QUEUED_DISCS.remove(song);
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
        if (changed) clearQueueType();
        if (changed) rebuildShuffleOrderAfterQueueChange();
        if (changed) save();
    }

    public static void setQueueType(MaMDataConfig.NowPlayingType type, String id, String name) {
        ensureLoaded();
        MaMDataConfig config = MaMDataConfig.get();
        config.player.now_playing_type = type;
        config.player.now_playing_id = id;
        config.player.now_playing_name = name;
        touchRecentSource(type, id);
        save();
    }

    public static boolean loadQueueType(Collection<MaMDataConfig.Entry> entries, MaMDataConfig.NowPlayingType type, String id, String name) {
        ensureLoaded();
        if (entries == null) return false;
        replaceRuntimeQueue(entries);

        MaMDataConfig config = MaMDataConfig.get();
        config.player.now_playing_type = type;
        config.player.now_playing_id = id;
        config.player.now_playing_name = name;
        touchRecentSource(type, id);
        save();
        return !QUEUED_SONGS.isEmpty();
    }

    public static boolean loadCustomQueue() {
        ensureLoaded();
        replaceRuntimeQueue(CUSTOM_PLAYLIST);
        clearQueueType();
        save();
        return !QUEUED_SONGS.isEmpty();
    }

    public static Optional<QueueType> queueSource() {
        MaMDataConfig.Player playlist = MaMDataConfig.get().player;
        if (playlist.now_playing_type == MaMDataConfig.NowPlayingType.NONE || playlist.now_playing_name.isBlank()) return Optional.empty();
        return Optional.of(new QueueType(playlist.now_playing_type, playlist.now_playing_id, playlist.now_playing_name));
    }

    public static List<SafeIdentifier> queuedSongs() {
        ensureLoaded();
        return List.copyOf(QUEUED_SONGS);
    }

    public static Optional<Identifier> queuedDisc(SafeIdentifier sound) {
        ensureLoaded();
        return Optional.ofNullable(sound == null ? null : QUEUED_DISCS.get(sound));
    }

    public static List<MaMDataConfig.Entry> customPlaylistEntries() {
        ensureLoaded();
        return CUSTOM_PLAYLIST.stream().map(PlaylistHelper::copyEntry).toList();
    }

    public static List<SafeIdentifier> customPlaylistSongs() {
        ensureLoaded();
        return CUSTOM_PLAYLIST.stream().map(PlaylistHelper::resolveEntry).flatMap(Optional::stream).toList();
    }

    public static boolean hasCustomPlaylistSongs() {
        ensureLoaded();
        return !CUSTOM_PLAYLIST.isEmpty();
    }

    public static boolean isInCustomPlaylist(MaMDataConfig.Entry entry) {
        ensureLoaded();
        return entry != null && CUSTOM_PLAYLIST.stream().anyMatch(existing -> sameEntry(existing, entry));
    }

    public static void addToCustomPlaylist(MaMDataConfig.Entry entry) {
        ensureLoaded();
        if (validPlaylistEntry(entry) && !isInCustomPlaylist(entry)) {
            if (entry.isTrack()) {
                int firstDisc = 0;
                while (firstDisc < CUSTOM_PLAYLIST.size() && CUSTOM_PLAYLIST.get(firstDisc).isTrack()) firstDisc++;
                CUSTOM_PLAYLIST.add(firstDisc, copyEntry(entry));
            } else {
                CUSTOM_PLAYLIST.add(copyEntry(entry));
            }
            syncCustomQueueAfterMutation();
            save();
        }
    }

    public static void addAllToCustomPlaylist(Collection<MaMDataConfig.Entry> entries) {
        ensureLoaded();
        if (entries == null) return;
        boolean changed = false;
        for (MaMDataConfig.Entry entry : entries) {
            if (validPlaylistEntry(entry) && !isInCustomPlaylist(entry)) {
                CUSTOM_PLAYLIST.add(copyEntry(entry));
                changed = true;
            }
        }
        if (changed) {
            normalizeCustomPlaylistOrder();
            syncCustomQueueAfterMutation();
            save();
        }
    }

    public static boolean replaceCustomPlaylist(Collection<MaMDataConfig.Entry> entries) {
        ensureLoaded();
        if (entries == null) return false;
        CUSTOM_PLAYLIST.clear();
        for (MaMDataConfig.Entry entry : entries) {
            if (validPlaylistEntry(entry) && !isInCustomPlaylist(entry)) {
                CUSTOM_PLAYLIST.add(copyEntry(entry));
            }
        }
        normalizeCustomPlaylistOrder();
        syncCustomQueueAfterMutation();
        save();
        return !CUSTOM_PLAYLIST.isEmpty();
    }

    public static boolean moveCustomPlaylistSong(int fromIndex, int toIndex) {
        ensureLoaded();
        if (fromIndex < 0 || fromIndex >= CUSTOM_PLAYLIST.size()
                || toIndex < 0 || toIndex >= CUSTOM_PLAYLIST.size()
                || fromIndex == toIndex) {
            return false;
        }
        if (!Objects.equals(CUSTOM_PLAYLIST.get(fromIndex).type, CUSTOM_PLAYLIST.get(toIndex).type)) return false;
        MaMDataConfig.Entry moved = CUSTOM_PLAYLIST.remove(fromIndex);
        CUSTOM_PLAYLIST.add(toIndex, moved);
        syncCustomQueueAfterMutation();
        save();
        return true;
    }

    public static void removeCustomPlaylistSong(int index) {
        ensureLoaded();
        if (index >= 0 && index < CUSTOM_PLAYLIST.size()) {
            CUSTOM_PLAYLIST.remove(index);
            syncCustomQueueAfterMutation();
            save();
        }
    }

    public static void clearCustomPlaylist() {
        ensureLoaded();
        if (CUSTOM_PLAYLIST.isEmpty()) return;
        CUSTOM_PLAYLIST.clear();
        syncCustomQueueAfterMutation();
        save();
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
        clearQueueType();
        rebuildShuffleOrderAfterQueueChange();
        save();
        return true;
    }

    public static void remove(int index) {
        ensureLoaded();
        if (index >= 0 && index < QUEUED_SONGS.size()) {
            SafeIdentifier removed = QUEUED_SONGS.get(index);
            QUEUED_SONGS.remove(index);
            QUEUED_DISCS.remove(removed);
            if (index < queueIndex) queueIndex--;
            if (index < currentQueueIndex) currentQueueIndex--;
            if (index == pausedQueueIndex) clearPausedQueue();
            else if (index < pausedQueueIndex) pausedQueueIndex--;
            if (currentSongFromQueue && DirectSoundFiles.samePlayable(removed, currentSongId)) stop();
            queueIndex = clampQueueIndex(queueIndex);
            clearQueueType();
            rebuildShuffleOrderAfterQueueChange();
            save();
        }
    }

    public static void clear() {
        ensureLoaded();
        if (QUEUED_SONGS.isEmpty()) return;
        if (currentSongFromQueue) stop();
        QUEUED_SONGS.clear();
        QUEUED_DISCS.clear();
        queueIndex = 0;
        currentQueueIndex = -1;
        queuePaused = true;
        clearPausedQueue();
        currentShuffleIndex = -1;
        SHUFFLE_ORDER.clear();
        shuffleIndex = 0;
        clearQueueType();
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

        SafeIdentifier wanted = CustomAlbums.playableId(song);
        SafeIdentifier current = CustomAlbums.playableId(currentSongId);
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
            if (visibleIndex < 0) return false;
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

    public static boolean seekCurrentSong(long millis) {
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

    public static void beginSoundEngineReload() {
        soundEngineReloading = true;
        reloadPlayback = null;

        if (!currentSongFromQueue || currentSong == null || currentSongId == null || !isPlaying()) return;

        reloadPlayback = new ReloadPlayback(
                currentSongId,
                currentSongLooping,
                currentShuffleIndex,
                shuffleIndex,
                queuePaused,
                currentSongElapsedMillis()
        );
    }

    public static void finishSoundEngineReload() {
        ReloadPlayback playback = reloadPlayback;
        reloadPlayback = null;
        soundEngineReloading = false;

        if (playback == null) {
            interruptCurrentPlayback(null);
            return;
        }

        int visibleIndex = findQueueIndex(playback.song());
        if (visibleIndex < 0) {
            interruptCurrentPlayback(null);
            return;
        }

        queueIndex = visibleIndex;
        currentShuffleIndex = playback.currentShuffleIndex();
        shuffleIndex = playback.shuffleIndex();
        queuePaused = false;
        pendingSeekMillis = playback.elapsedMillis();

        boolean restarted = playSound(playback.song(), playback.looping(), true, false);
        if (restarted && playback.queuePaused()) pauseQueue();
    }

    public static void interruptCurrentPlayback(SoundInstance sound) {
        if (soundEngineReloading) return;
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
        String configName = CustomAlbums.displayName(displayId);
        if (configName != null) return LITERAL_TRANSLATION_PREFIX + configName;
        Optional<Identifier> queuedDisc = currentSongFromQueue ? queuedDisc(currentSongId) : Optional.empty();
        if (queuedDisc.isPresent()) return MusicDiscHelper.translationKey(queuedDisc.get());
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
            if (loop) return !SHUFFLE_ORDER.isEmpty();
            return shuffleIndex < SHUFFLE_ORDER.size();
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
        return findQueueIndex(currentSongId);
    }

    private static int findQueueIndex(SafeIdentifier song) {
        if (song == null) return -1;
        for (int i = 0; i < QUEUED_SONGS.size(); i++) {
            if (DirectSoundFiles.samePlayable(QUEUED_SONGS.get(i), song)) return i;
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
        return QUEUED_SONGS.isEmpty() ? -1 : clampQueueIndex(start);
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
        return QUEUED_SONGS.isEmpty() ? -1 : clampQueueIndex(start);
    }

    private static boolean playSound(SafeIdentifier id, boolean loop, boolean fromQueue, boolean fromEvent) {
        return playSound(id, loop, fromQueue, fromEvent, DirectSoundInstance.Type.ALL);
    }

    private static boolean playSound(SafeIdentifier id, boolean loop, boolean fromQueue, boolean fromEvent, DirectSoundInstance.Type type) {
        id = CustomAlbums.playableId(id);
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
            Minecraft.getInstance().gui.toastManager().showNowPlayingToast();
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
        pausedQueueSong = currentSongId;
        pausedQueueSound = currentSong;
        pausedQueueIndex = currentQueueIndex >= 0 ? currentQueueIndex : findCurrentQueueIndex();
        pausedShuffleIndex = currentShuffleIndex;
        pausedQueueElapsedMillis = currentSongElapsedMillis();
        queuePaused = true;
        thisStopCurrentSongOnly();
    }

    private static boolean resumeQueue() {
        if (pausedQueueSong == null || pausedQueueIndex < 0 || pausedQueueIndex >= QUEUED_SONGS.size()
                || !DirectSoundFiles.samePlayable(QUEUED_SONGS.get(pausedQueueIndex), pausedQueueSong)) {
            clearPausedQueue();
            return false;
        }
        SafeIdentifier song = pausedQueueSong;
        queueIndex = pausedQueueIndex;
        currentShuffleIndex = pausedShuffleIndex;
        if (shuffle && pausedShuffleIndex >= 0) shuffleIndex = pausedShuffleIndex + 1;
        long elapsed = pausedQueueElapsedMillis;
        clearPausedQueue();
        stop();
        pendingSeekMillis = elapsed;
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

    public static long currentSongElapsedMillis() {
        if (queuePaused && currentSongFromQueue) return pausedQueueElapsedMillis;
        if (currentSong == null) return pausedQueueElapsedMillis;
        if (currentSongStartedAtNanos == 0L) return 0L;
        return Math.max(0L, (System.nanoTime() - currentSongStartedAtNanos) / 1_000_000L);
    }

    private record ReloadPlayback(
            SafeIdentifier song,
            boolean looping,
            int currentShuffleIndex,
            int shuffleIndex,
            boolean queuePaused,
            long elapsedMillis
    ) {}

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        QUEUED_SONGS.clear();
        QUEUED_DISCS.clear();
        CUSTOM_PLAYLIST.clear();
        queuePaused = true;
        MaMDataConfig config = MaMDataConfig.get();
        loop = config.player.loop;
        shuffle = config.player.shuffle;
        for (String track : config.player.custom_playlist_tracks) {
            MaMDataConfig.Entry entry = entry(track, "track");
            if (validPlaylistEntry(entry) && CUSTOM_PLAYLIST.stream().noneMatch(existing -> sameEntry(existing, entry))) {
                CUSTOM_PLAYLIST.add(entry);
            }
        }
        for (String disc : config.player.custom_playlist_discs) {
            MaMDataConfig.Entry entry = entry(disc, "disc");
            if (validPlaylistEntry(entry) && CUSTOM_PLAYLIST.stream().noneMatch(existing -> sameEntry(existing, entry))) {
                CUSTOM_PLAYLIST.add(entry);
            }
        }
        restoreConfiguredQueue();
        if (shuffle) rebuildShuffleOrder(null);
    }

    private static void save() {
        MaMDataConfig config = MaMDataConfig.get();
        config.player.loop = loop;
        config.player.shuffle = shuffle;
        config.player.custom_playlist_tracks = new ArrayList<>(CUSTOM_PLAYLIST.stream()
                .filter(MaMDataConfig.Entry::isTrack).map(entry -> entry.id).toList());
        config.player.custom_playlist_discs = new ArrayList<>(CUSTOM_PLAYLIST.stream()
                .filter(MaMDataConfig.Entry::isDisc).map(entry -> entry.id).toList());
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private static void clearQueueType() {
        MaMDataConfig config = MaMDataConfig.get();
        config.player.now_playing_type = MaMDataConfig.NowPlayingType.NONE;
        config.player.now_playing_id = "";
        config.player.now_playing_name = "";
    }

    private static int remapMovedIndex(int index, int fromIndex, int toIndex) {
        if (index < 0) return index;
        if (index == fromIndex) return toIndex;
        if (fromIndex < toIndex && index > fromIndex && index <= toIndex) return index - 1;
        if (toIndex < fromIndex && index >= toIndex && index < fromIndex) return index + 1;
        return index;
    }

    private static void touchRecentSource(MaMDataConfig.NowPlayingType type, String id) {
        if (type == MaMDataConfig.NowPlayingType.NONE || id == null || id.isBlank()) return;
        MaMDataConfig.Player playlist = MaMDataConfig.get().player;
        String entryType = type == MaMDataConfig.NowPlayingType.ALBUM ? "album" : "playlist";
        playlist.recent_favourites.removeIf(entry -> entry != null && id.equals(entry.id) && entryType.equals(entry.type));
        playlist.recent_favourites.addFirst(entry(id, entryType));
    }

    // Returns {@code 0} for the most recently played source, or a large rank when unseen
    public static int recentSourceRank(MaMDataConfig.NowPlayingType type, String id) {
        if (type == MaMDataConfig.NowPlayingType.NONE || id == null) return Integer.MAX_VALUE;
        String entryType = type == MaMDataConfig.NowPlayingType.ALBUM ? "album" : "playlist";
        List<MaMDataConfig.Entry> recent = MaMDataConfig.get().player.recent_favourites;
        for (int index = 0; index < recent.size(); index++) {
            MaMDataConfig.Entry entry = recent.get(index);
            if (entry != null && id.equals(entry.id) && entryType.equals(entry.type)) return index;
        }
        return Integer.MAX_VALUE;
    }

    public static MaMDataConfig.Entry trackEntry(SafeIdentifier track) {
        return entry(track.toString(), "track");
    }

    public static MaMDataConfig.Entry discEntry(Identifier disc) {
        return entry(disc.toString(), "disc");
    }

    public static boolean isFavourite(Identifier id, MaMDataConfig.NowPlayingType type) {
        if (id == null || type == MaMDataConfig.NowPlayingType.NONE) return false;
        String entryType = type == MaMDataConfig.NowPlayingType.ALBUM ? "album" : "playlist";
        return MaMDataConfig.get().player.favourites.stream()
                .anyMatch(entry -> entry != null && id.toString().equals(entry.id) && entryType.equals(entry.type));
    }

    public static void setFavourite(Identifier id, MaMDataConfig.NowPlayingType type, boolean favourite) {
        if (id == null || type == MaMDataConfig.NowPlayingType.NONE) return;
        MaMDataConfig.Player playlist = MaMDataConfig.get().player;
        String entryType = type == MaMDataConfig.NowPlayingType.ALBUM ? "album" : "playlist";
        playlist.favourites.removeIf(entry -> entry != null && id.toString().equals(entry.id) && entryType.equals(entry.type));
        if (favourite) playlist.favourites.add(entry(id.toString(), entryType));
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public static Optional<SafeIdentifier> resolveEntry(MaMDataConfig.Entry entry) {
        if (!validPlaylistEntry(entry)) return Optional.empty();
        if (entry.isTrack()) return Optional.of(SafeIdentifier.parse(entry.id));

        Identifier discId = Identifier.tryParse(entry.id);
        if (discId == null || !MusicDiscHelper.isDiscUnlocked(Minecraft.getInstance(), discId)) return Optional.empty();
        for (Album album : Album.ALBUMS) {
            for (Album.StoredDisc disc : album.discs) {
                if (MusicDiscHelper.albumEntryId(album, disc).equals(discId)) {
                    return MusicDiscHelper.discSoundId(Minecraft.getInstance(), album, disc).map(SafeIdentifier::convert);
                }
            }
        }
        return MusicDiscHelper.discSoundId(Minecraft.getInstance(), discId).map(SafeIdentifier::convert);
    }

    public static Optional<Identifier> entryDisc(MaMDataConfig.Entry entry) {
        return entry != null && entry.isDisc() ? Optional.ofNullable(Identifier.tryParse(entry.id)) : Optional.empty();
    }

    public static void refreshConfiguredQueue() {
        ensureLoaded();
        if (!currentSongFromQueue) restoreConfiguredQueue();
    }

    private static void restoreConfiguredQueue() {
        MaMDataConfig.Player playlist = MaMDataConfig.get().player;
        Collection<MaMDataConfig.Entry> entries = switch (playlist.now_playing_type) {
            case ALBUM -> configuredAlbumEntries(playlist.now_playing_id);
            case PLAYLIST -> configuredPlaylistEntries(playlist.now_playing_id);
            case NONE -> CUSTOM_PLAYLIST;
        };
        replaceRuntimeQueue(entries, false);
    }

    private static List<MaMDataConfig.Entry> configuredAlbumEntries(String id) {
        Identifier albumId = Identifier.tryParse(id);
        if (albumId == null) return List.of();
        for (Album album : Album.ALBUMS) {
            if (!album.album.equals(albumId)) continue;
            List<MaMDataConfig.Entry> entries = new ArrayList<>();
            album.tracks.stream().map(album::trackId).map(PlaylistHelper::trackEntry).forEach(entries::add);
            album.discs.stream().map(disc -> MusicDiscHelper.albumEntryId(album, disc)).map(PlaylistHelper::discEntry).forEach(entries::add);
            return entries;
        }
        return List.of();
    }

    private static List<MaMDataConfig.Entry> configuredPlaylistEntries(String id) {
        Identifier playlistId = Identifier.tryParse(id);
        if (playlistId == null) return List.of();
        for (Playlist playlist : net.rebel459.music_and_melody.client.Playlist.PLAYLISTS) {
            if (!playlist.playlist.equals(playlistId)) continue;
            List<MaMDataConfig.Entry> entries = new ArrayList<>();
            playlist.tracks.stream().map(PlaylistHelper::trackEntry).forEach(entries::add);
            playlist.discs.stream().map(PlaylistHelper::discEntry).forEach(entries::add);
            return entries;
        }
        return List.of();
    }

    private static void replaceRuntimeQueue(Collection<MaMDataConfig.Entry> entries) {
        replaceRuntimeQueue(entries, true);
    }

    private static void replaceRuntimeQueue(Collection<MaMDataConfig.Entry> entries, boolean stopPlayback) {
        if (stopPlayback && currentSongFromQueue) stop();
        else clearPausedQueue();
        materializeQueue(entries);
        queueIndex = 0;
        currentQueueIndex = -1;
        queuePaused = true;
        currentShuffleIndex = -1;
        rebuildShuffleOrder(null);
    }

    private static void syncCustomQueueAfterMutation() {
        if (MaMDataConfig.get().player.now_playing_type != MaMDataConfig.NowPlayingType.NONE) return;
        SafeIdentifier playing = currentSongFromQueue ? currentSongId : null;
        boolean wasPaused = queuePaused;
        materializeQueue(CUSTOM_PLAYLIST);
        if (playing == null) {
            queueIndex = clampQueueIndex(queueIndex);
            rebuildShuffleOrder(null);
            return;
        }

        int current = findQueueIndex(playing);
        if (current < 0) {
            stop();
            queuePaused = QUEUED_SONGS.isEmpty() || wasPaused;
            queueIndex = 0;
            rebuildShuffleOrder(null);
            return;
        }
        currentQueueIndex = current;
        queueIndex = current;
        queuePaused = wasPaused;
        if (pausedQueueSong != null) pausedQueueIndex = current;
        rebuildShuffleOrder(shuffle ? QUEUED_SONGS.get(current) : null);
    }

    private static void materializeQueue(Collection<MaMDataConfig.Entry> entries) {
        QUEUED_SONGS.clear();
        QUEUED_DISCS.clear();
        if (entries != null) {
            for (MaMDataConfig.Entry entry : entries) {
                Optional<SafeIdentifier> resolved = resolveEntry(entry);
                if (resolved.isEmpty() || QUEUED_SONGS.contains(resolved.get())) continue;
                QUEUED_SONGS.add(resolved.get());
                entryDisc(entry).ifPresent(disc -> QUEUED_DISCS.put(resolved.get(), disc));
            }
        }
    }

    private static boolean validPlaylistEntry(MaMDataConfig.Entry entry) {
        return entry != null && entry.id != null && !entry.id.isBlank()
                && entry.type != null && (entry.isTrack() || entry.isDisc());
    }

    private static boolean sameEntry(MaMDataConfig.Entry first, MaMDataConfig.Entry second) {
        return first != null && second != null && Objects.equals(first.id, second.id) && Objects.equals(first.type, second.type);
    }

    private static MaMDataConfig.Entry copyEntry(MaMDataConfig.Entry source) {
        return entry(source.id, source.type);
    }

    private static void normalizeCustomPlaylistOrder() {
        List<MaMDataConfig.Entry> ordered = new ArrayList<>(CUSTOM_PLAYLIST.size());
        CUSTOM_PLAYLIST.stream().filter(MaMDataConfig.Entry::isTrack).forEach(ordered::add);
        CUSTOM_PLAYLIST.stream().filter(MaMDataConfig.Entry::isDisc).forEach(ordered::add);
        CUSTOM_PLAYLIST.clear();
        CUSTOM_PLAYLIST.addAll(ordered);
    }

    private static MaMDataConfig.Entry entry(String id, String type) {
        MaMDataConfig.Entry entry = new MaMDataConfig.Entry();
        entry.id = id;
        entry.type = type;
        return entry;
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
        if (shuffleIndex >= SHUFFLE_ORDER.size()) {
            if (!loop) {
                queuePaused = true;
                return -1;
            }
            rebuildShuffleOrder(null);
        }
        return SHUFFLE_ORDER.isEmpty() ? -1 : shuffleIndex++;
    }

    public record QueueType(MaMDataConfig.NowPlayingType type, String id, String name) {}
}
