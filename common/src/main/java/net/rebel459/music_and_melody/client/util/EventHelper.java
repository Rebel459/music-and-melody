package net.rebel459.music_and_melody.client.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.Event;
import net.rebel459.music_and_melody.client.Playlist;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.network.StructureMusicHandler;
import net.rebel459.music_and_melody.platform.MaMPlatform;

import java.util.*;

public class EventHelper {

    private static int musicBreak;
    private static boolean blockingForEventMusic;
    private static boolean cooldownEmptyMusic;
    private static boolean fading;
    private static boolean finishedFading;
    private static boolean fadingOutCurrentEvent;
    private static QueuedEvent queuedEvent;

    private static Event.PriorityType lastPriority = Event.PriorityType.LOW;
    private static List<Event.Condition> lastConditions = List.of();
    private static boolean shouldSustain = true;
    private static Event.CategoryType lastCategory;
    private static SafeIdentifier lastMusic;

    public static boolean isEndPortalFilled() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> blockSounds = manager.soundEngine.instanceBySource.get(SoundSource.BLOCKS);
        for (SoundInstance instance : new ArrayList<>(blockSounds)) {
            if (SoundEvents.END_PORTAL_SPAWN.location().equals(instance.getIdentifier()) && manager.isActive(instance)) {
                return true;
            }
        }
        return false;
    }

    public static Music processEventMusic() {
        if (!isEnabled()) {
            stopDisabledEventActivity();
            return null;
        }

        WeightedList.Builder<Event> validEvents = new WeightedList.Builder<>();
        processEvents(validEvents, Event.VERY_HIGH_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.HIGH_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.MEDIUM_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.LOW_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.VERY_LOW_PRIORITY);

        WeightedList<Event> events = validEvents.build();
        if (finishedFading) {
            return playQueuedEvent();
        }
        if (fadingOutCurrentEvent) {
            return storedEventMusicOrBlocker();
        }
        if (fading) {
            if (!events.isEmpty()) {
                Event event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
                if (queuedEvent == null || event.priority.ordinal() > queuedEvent.event().priority.ordinal()) {
                    queuedEvent = new QueuedEvent(event, true);
                }
            }
            return storedEventMusicOrBlocker();
        }

        if (!events.isEmpty()) {
            Event event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
            boolean activeMusic = hasActiveNonEmptyMusic();
            boolean storedEventActive = isStoredEventMusicActive();
            boolean storedEventStillApplicable = isCurrentEventMusicStillApplicable(storedEventActive);
            boolean storedEventInactive = lastCategory != null && !storedEventActive;
            if (activeMusic && !storedEventActive) {
                clearStoredEvent();
                storedEventInactive = false;
            }

            int currentPriority = lastCategory != null ? getPriority(lastPriority) : vanillaMusicPriority(activeMusic);
            boolean higherPriority = getPriority(event.priority) > currentPriority;
            if (higherPriority) {
                cooldownEmptyMusic = false;
                if (activeMusic) {
                    queueEventFade(event, true);
                    return null;
                }
                stopEmptyMusic();
                return playEventOrBlockVanilla(Minecraft.getInstance(), event, true);
            }

            if (storedEventStillApplicable) {
                cooldownEmptyMusic = false;
                return storedEventMusicOrBlocker();
            }

            if (musicBreak > 0) {
                musicBreak--;
                blockingForEventMusic = true;
                cooldownEmptyMusic = !activeMusic;
                return activeMusic ? storedEventMusicOrBlocker() : PlaylistHelper.EMPTY;
            }

            if (blockingForEventMusic) {
                if (activeMusic) {
                    cooldownEmptyMusic = false;
                    return storedEventMusicOrBlocker();
                }
                blockingForEventMusic = false;
                cooldownEmptyMusic = false;
                stopEmptyMusic();
                return playEventOrBlockVanilla(Minecraft.getInstance(), event, true);
            }

            if (!blockingForEventMusic && activeMusic && !isCurrentEventMusicStillApplicable(storedEventActive)) {
                musicBreak = eventMusicBreak(event);
                if (musicBreak > 0) {
                    musicBreak--;
                    blockingForEventMusic = true;
                    cooldownEmptyMusic = false;
                    return null;
                }
            }

            if (!blockingForEventMusic && activeMusic) {
                cooldownEmptyMusic = false;
                return storedEventMusicOrBlocker();
            }

            musicBreak = eventMusicBreak(event);
            if (musicBreak > 0) {
                musicBreak--;
                blockingForEventMusic = true;
                cooldownEmptyMusic = true;
                return PlaylistHelper.EMPTY;
            }

            cooldownEmptyMusic = false;
            return playEventOrBlockVanilla(Minecraft.getInstance(), event, blockingForEventMusic);
        }
        musicBreak = 0;
        blockingForEventMusic = false;
        cooldownEmptyMusic = false;
        if (shouldStopStoredEventMusic()) {
            fadingOutCurrentEvent = true;
            return storedEventMusicOrBlocker();
        }
        clearFadeEvent();
        stopEmptyMusic();
        if (isStoredEventMusicActive()) {
            return storedEventMusicOrBlocker();
        }
        clearStoredEvent();
        return null;
    }

    public static void resetMusicBreak() {
        musicBreak = 0;
        blockingForEventMusic = false;
        cooldownEmptyMusic = false;
        clearFadeEvent();
    }

    public static boolean isCooldownEmptyMusic() {
        return isEnabled() && cooldownEmptyMusic;
    }

    public static boolean isFading() {
        return isEnabled() && fading;
    }

    public static boolean shouldContinueEventFade() {
        return isEnabled() && fading && queuedEvent != null && shouldBeActive(queuedEvent.event().conditions, false);
    }

    public static boolean isFadingOutCurrentEvent() {
        return isEnabled() && fadingOutCurrentEvent;
    }

    public static boolean shouldContinueCurrentEventFadeOut() {
        return isEnabled()
                && fadingOutCurrentEvent
                && isStoredEventMusicActive()
                && !shouldSustain
                && !lastConditions.isEmpty()
                && !shouldBeActive(lastConditions, false);
    }

    public static boolean shouldFadeCurrentMusic(SoundInstance currentMusic) {
        return isEnabled()
                && isStoredPoolMusic(currentMusic)
                && !shouldSustain
                && !lastConditions.isEmpty()
                && !shouldBeActive(lastConditions, false)
                && !PlaylistHelper.isPlaying();
    }

    public static void clearStoredEventMusic(SoundInstance currentMusic) {
        if (isStoredPoolMusic(currentMusic)) {
            clearStoredEvent();
        }
    }

    public static void stopOldEventMusic() {
        if (!isEnabled()) {
            stopDisabledEventActivity();
            return;
        }
        if (lastCategory == null || shouldSustain || lastConditions.isEmpty()) {
            return;
        }
        if (shouldStopStoredEventMusic()) {
            fadingOutCurrentEvent = true;
        }
    }

    public static void clearCurrentEventFadeOut() {
        fadingOutCurrentEvent = false;
    }

    public static void finishCurrentEventFadeOut() {
        stopStoredPoolMusic();
        PlaylistHelper.stopEvent();
        clearStoredEvent();
        fadingOutCurrentEvent = false;
    }

    public static Pair<Integer, Integer> getMusicFrequency() {
        MusicManager.MusicFrequency frequency = Minecraft.getInstance().getMusicManager().gameMusicFrequency;
        return switch (frequency) {
            case DEFAULT -> Pair.of(600, 1200);
            case FREQUENT -> Pair.of(300, 600);
            case CONSTANT -> Pair.of(0, 0);
        };
    }

    private static Music playEvent(Minecraft client, Event event, boolean replaceCurrentMusic) {
        boolean playEvent = false;
        if (event.category == Event.CategoryType.ALBUM) {
            Optional<Album> album = Album.ALBUMS.stream().filter(entry -> entry.album.equals(event.music.getId())).findFirst();
            playEvent = album.filter(value -> playRandomEventSong(client, eventAlbumSongs(value, client))).isPresent();
        }
        if (event.category == Event.CategoryType.PLAYLIST) {
            Optional<Playlist> playlist = Playlist.PLAYLISTS.stream().filter(entry -> entry.playlist.equals(event.music.getId())).findFirst();
            if (!playlist.isEmpty()) {
                List<SafeIdentifier> songs = new ArrayList<>(playlist.get().tracks.stream()
                        .filter(EventHelper::isEventTrackEnabled)
                        .toList());
                playlist.get().discs.stream()
                        .map(disc -> MusicDiscHelper.discSoundId(client, disc))
                        .flatMap(Optional::stream)
                        .forEach(id -> songs.add(SafeIdentifier.convert(id)));
                playEvent = playRandomEventSong(client, songs);
            }
        }
        if (event.category == Event.CategoryType.POOL) {
            playEvent = PlaylistHelper.playEvent(event.music, false, DirectSoundInstance.Type.POOLS);
        }
        if (event.category == Event.CategoryType.TRACK) {
            playEvent = PlaylistHelper.playEvent(event.music, false, DirectSoundInstance.Type.TRACKS);
        }
        if (event.category == Event.CategoryType.DISC && MusicDiscHelper.isDiscUnlocked(client, event.music.getId())) {
            playEvent = MusicDiscHelper.discSoundId(client, event.music.getId())
                    .map(sound -> PlaylistHelper.playEvent(SafeIdentifier.convert(sound), false))
                    .orElse(false);
        }

        if (!playEvent) return null;
        storeEvent(event);
        return PlaylistHelper.EMPTY;
    }

    private static void queueEventFade(Event event, boolean replaceCurrentMusic) {
        fading = true;
        finishedFading = false;
        queuedEvent = new QueuedEvent(event, replaceCurrentMusic);
    }

    public static Music playQueuedEvent() {
        QueuedEvent queued = queuedEvent;
        clearFadeEvent();
        if (queued == null) return null;
        Event event = queued.event();
        if (!shouldBeActive(event.conditions, false)) return null;
        return playEventOrBlockVanilla(Minecraft.getInstance(), event, queued.replaceCurrentMusic());
    }

    public static void clearFadeEvent() {
        fading = false;
        finishedFading = false;
        fadingOutCurrentEvent = false;
        queuedEvent = null;
    }

    public static void finishFade() {
        fading = false;
        finishedFading = true;
    }

    private static void stopEmptyMusic() {
        Minecraft client = Minecraft.getInstance();
        client.getMusicManager().stopPlaying(PlaylistHelper.EMPTY);
        SoundManager manager = client.getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        for (SoundInstance instance : new ArrayList<>(instances)) {
            if (PlaylistHelper.isEmptyMusic(instance)) manager.stop(instance);
        }
    }

    private static boolean shouldStopStoredEventMusic() {
        return lastCategory != null
                && isStoredEventMusicActive()
                && !shouldSustain
                && !lastConditions.isEmpty()
                && !shouldBeActive(lastConditions, false);
    }

    private static void stopStoredPoolMusic() {
        if (lastCategory != Event.CategoryType.POOL || lastMusic == null) return;

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        for (SoundInstance instance : new ArrayList<>(instances)) {
            if (isStoredPoolMusic(instance)) manager.stop(instance);
        }
    }

    private static boolean isEnabled() {
        return MaMClientConfig.get().allow_events;
    }

    private static void stopDisabledEventActivity() {
        PlaylistHelper.stopEvent();
        resetMusicBreak();
        clearStoredEvent();
    }

    private static boolean hasActiveNonEmptyMusic() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        for (SoundInstance instance : instances) {
            if (!PlaylistHelper.isEmptyMusic(instance) && manager.isActive(instance)) return true;
        }
        return false;
    }

    private static int getPriority(Event.PriorityType priority) {
        return priority.ordinal() * 2;
    }

    private static int vanillaMusicPriority(boolean activeMusic) {
        if (!activeMusic) return getPriority(Event.PriorityType.LOW);

        Identifier gameMusic = SoundEvents.MUSIC_GAME.value().location();
        Identifier creativeMusic = SoundEvents.MUSIC_CREATIVE.value().location();
        boolean creativeFix = MaMClientConfig.get().creative_fix;
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        for (SoundInstance instance : instances) {
            if (manager.isActive(instance)) {
                if (gameMusic.equals(instance.getIdentifier())) return getPriority(Event.PriorityType.VERY_LOW) - 1;
                if (creativeFix && creativeMusic.equals(instance.getIdentifier())) return getPriority(Event.PriorityType.MEDIUM) - 1;
            }
        }

        return getPriority(Event.PriorityType.LOW) - 1;
    }

    private static Music playEventOrBlockVanilla(Minecraft client, Event event, boolean replaceCurrentMusic) {
        Music music = playEvent(client, event, replaceCurrentMusic);
        if (music != null) {
            return music;
        }

        cooldownEmptyMusic = true;
        return PlaylistHelper.EMPTY;
    }

    private static Music storedEventMusicOrBlocker() {
        if (lastCategory != null) {
            return PlaylistHelper.EMPTY;
        }
        return null;
    }

    private static void storeEvent(Event event) {
        musicBreak = 0;
        blockingForEventMusic = false;
        cooldownEmptyMusic = false;
        EventHelper.lastPriority = event.priority;
        EventHelper.lastConditions = event.conditions;
        EventHelper.shouldSustain = event.sustain;
        EventHelper.lastCategory = event.category;
        EventHelper.lastMusic = event.music;
    }

    private static boolean isCurrentEventMusicStillApplicable(boolean storedEventActive) {
        return storedEventActive && !lastConditions.isEmpty() && shouldBeActive(lastConditions, false);
    }

    private static boolean isStoredEventMusicActive() {
        if (lastCategory == null || lastMusic == null) {
            return false;
        }
        if (lastCategory != Event.CategoryType.POOL) {
            return PlaylistHelper.isEventPlaying();
        }

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        for (SoundInstance instance : instances) {
            if (isStoredPoolMusic(instance) && manager.isActive(instance)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStoredPoolMusic(SoundInstance currentMusic) {
        if (currentMusic == null || lastCategory != Event.CategoryType.POOL || lastMusic == null) return false;
        if (lastMusic.equals(currentMusic.getIdentifier())) return true;

        Sound sound = currentMusic.getSound();
        if (sound == null || sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            return false;
        }

        return isSoundInStoredPool(sound.getLocation()) || isSoundInStoredPool(sound.getPath());
    }

    private static boolean isSoundInStoredPool(Identifier soundId) {
        if (soundId == null || lastMusic == null) return false;

        WeighedSoundEvents event = Minecraft.getInstance().getSoundManager().getSoundEvent(lastMusic.getId());
        return event != null && containsSound(event, soundId);
    }

    private static boolean containsSound(Weighted<Sound> weighted, Identifier soundId) {
        if (weighted instanceof Sound sound) {
            return soundId.equals(sound.getLocation()) || soundId.equals(sound.getPath());
        }

        if (weighted instanceof WeighedSoundEvents event) {
            for (Weighted<Sound> entry : event.list) {
                if (containsSound(entry, soundId)) return true;
            }
        }

        return false;
    }

    public static void clearStoredEvent() {
        lastPriority = Event.PriorityType.LOW;
        lastConditions = List.of();
        shouldSustain = true;
        lastCategory = null;
        lastMusic = null;
        fadingOutCurrentEvent = false;
    }

    public static int randomMusicBreak() {
        Pair<Integer, Integer> frequency = getMusicFrequency();
        int minimumTicks = 0;
        if (Minecraft.getInstance().level == null) minimumTicks = 200;
        return Math.max(minimumTicks, SoundInstance.createUnseededRandom().nextIntBetweenInclusive(frequency.getFirst(), frequency.getSecond()) * 20);
    }

    private static int eventMusicBreak(Event event) {
        int musicBreak = randomMusicBreak();
        if (event.constant) return Math.min(SoundInstance.createUnseededRandom().nextIntBetweenInclusive(10, 20) * 20, musicBreak);
        return musicBreak;
    }

    private static boolean playRandomEventSong(Minecraft client, List<SafeIdentifier> songs) {
        List<SafeIdentifier> playableSongs = songs.stream()
                .filter(song -> MusicDiscHelper.isSoundUnlocked(client, song))
                .toList();
        if (playableSongs.isEmpty()) return false;
        SafeIdentifier song = playableSongs.get(SoundInstance.createUnseededRandom().nextInt(playableSongs.size()));
        return PlaylistHelper.playEvent(song, false);
    }

    private static List<SafeIdentifier> eventAlbumSongs(Album album, Minecraft client) {
        List<SafeIdentifier> songs = new ArrayList<>();
        album.tracks.stream()
                .filter(song -> isEventAlbumTrackEnabled(album, song))
                .map(album::trackId)
                .forEach(songs::add);
        if (album.isEnabled()) {
            album.discs.stream()
                    .map(disc -> MusicDiscHelper.discSoundId(client, album, disc))
                    .flatMap(Optional::stream)
                    .forEach(id -> songs.add(SafeIdentifier.convert(id)));
        }
        return songs;
    }

    private static boolean isEventTrackEnabled(SafeIdentifier track) {
        boolean matchedAlbumTrack = false;
        boolean enabled = false;
        for (Album album : Album.ALBUMS) {
            for (String song : album.tracks) {
                if (!album.trackId(song).equals(track)) continue;
                matchedAlbumTrack = true;
                enabled = enabled || isEventAlbumTrackEnabled(album, song);
            }
        }
        return !matchedAlbumTrack || enabled;
    }

    private static boolean isEventAlbumTrackEnabled(Album album, String song) {
        return album.isTrackForcedEnabled(song) || album.isEnabled() && album.isTrackEnabled(song);
    }

    private static void processEvents(WeightedList.Builder<Event> validEvents, Set<Event> events) {
        for (Event event : events) {
            boolean shouldBeActive = EventHelper.shouldBeActive(event.conditions);
            if (shouldBeActive) validEvents.add(event, event.weight);
        }
    }

    private static boolean shouldBeActive(List<Event.Condition> conditions) {
        return shouldBeActive(conditions, true);
    }

    private static boolean shouldBeActive(List<Event.Condition> conditions, boolean rollRandomChance) {
        boolean shouldBeActive = true;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        Level level = client.level;
        for (Event.Condition condition : conditions) {
            switch(condition.type()) {
                case ALL_OF -> shouldBeActive = shouldBeActive && shouldBeActive(condition.conditions(), rollRandomChance);
                case ANY_OF -> shouldBeActive = shouldBeActive && condition.conditions().stream().anyMatch(nested -> shouldBeActive(List.of(nested), rollRandomChance));
                case NOT -> shouldBeActive = shouldBeActive && !shouldBeActive(condition.conditions(), rollRandomChance);
                case BIOME -> shouldBeActive = shouldBeActive && player != null && level != null && level.getBiome(player.blockPosition()).is(condition.idValue().get());
                case BIOME_TAG -> shouldBeActive = shouldBeActive && player != null && level != null && level.getBiome(player.blockPosition()).is(TagKey.create(Registries.BIOME, condition.idValue().get()));
                case DIMENSION -> shouldBeActive = shouldBeActive && level != null && level.dimension().identifier().equals(condition.idValue().get());
                case STRUCTURE -> shouldBeActive = shouldBeActive && StructureMusicHandler.getClientStructures().structures().contains(condition.idValue().get());
                case STRUCTURE_TAG -> shouldBeActive = shouldBeActive && StructureMusicHandler.getClientStructures().tags().contains(condition.idValue().get());
                case TIME -> {
                    shouldBeActive = shouldBeActive && level != null;
                    if (level != null) {
                        long time = Math.floorMod(level.getDayTime(), 24000L);
                        switch (condition.timeValue().get()) {
                            case DAY -> shouldBeActive = shouldBeActive && time >= 0 && time < 12000;
                            case SUNSET -> shouldBeActive = shouldBeActive && time >= 12000 && time < 13000;
                            case NIGHT -> shouldBeActive = shouldBeActive && time >= 13000 && time < 23000;
                            case SUNRISE -> shouldBeActive = shouldBeActive && time >= 23000 && time < 24000;
                        }
                    }
                }
                case WEATHER -> {
                    shouldBeActive = shouldBeActive && level != null;
                    if (level != null) {
                        switch (condition.weatherValue().get()) {
                            case CLEAR -> shouldBeActive = shouldBeActive && !level.isRaining();
                            case RAIN -> shouldBeActive = shouldBeActive && level.isRaining();
                            case THUNDER -> shouldBeActive = shouldBeActive && level.isThundering();
                        }
                    }
                }
                case GAME_MODE -> {
                    shouldBeActive = shouldBeActive && client.gameMode != null;
                    if (client.gameMode != null) {
                        GameType gameType = client.gameMode.getPlayerMode();
                        switch (condition.gameModeValue().get()) {
                            case SURVIVAL -> shouldBeActive = shouldBeActive && gameType == GameType.SURVIVAL;
                            case CREATIVE -> shouldBeActive = shouldBeActive && gameType == GameType.CREATIVE;
                            case ADVENTURE -> shouldBeActive = shouldBeActive && gameType == GameType.ADVENTURE;
                            case SPECTATOR -> shouldBeActive = shouldBeActive && gameType == GameType.SPECTATOR;
                        };
                    }
                }
                case SPECIAL -> {
                    switch (condition.eventValue().get()) {
                        case MENU -> shouldBeActive = shouldBeActive && client.level == null && !(client.screen instanceof WinScreen);
                        case CREDITS -> shouldBeActive = shouldBeActive && client.screen instanceof WinScreen;
                        case END_PORTAL -> shouldBeActive = shouldBeActive && EventHelper.isEndPortalFilled();
                        case UNDER_WATER -> shouldBeActive = shouldBeActive && player != null && player.isUnderWater();
                    }
                }
                case BELOW_Y -> shouldBeActive = shouldBeActive && player != null && player.blockPosition().getY() < condition.intValue().get();
                case BELOW_VERSION -> shouldBeActive = shouldBeActive && VanillaVersion.parse(condition.stringValue().get()).compareTo(VanillaVersion.getVanillaVersion()) > 0;
                case BOSSBAR -> shouldBeActive = shouldBeActive && Minecraft.getInstance().gui.getBossOverlay().events.values().stream().anyMatch(event -> event.getName().getString().equals(Component.translatable(condition.stringValue().get()).getString()));
                case MOD_LOADED -> shouldBeActive = shouldBeActive && MaMPlatform.PLATFORM.isModLoaded(condition.stringValue().get());
                case ALBUM_LOADED -> shouldBeActive = shouldBeActive && Album.LOADED_ALBUMS.contains(condition.idValue().get());
                case RANDOM_CHANCE -> shouldBeActive = shouldBeActive && (!rollRandomChance || SoundInstance.createUnseededRandom().nextFloat() <= condition.floatValue().get());
            }
        }
        return shouldBeActive;
    }

    private record QueuedEvent(Event event, boolean replaceCurrentMusic) {}
}
