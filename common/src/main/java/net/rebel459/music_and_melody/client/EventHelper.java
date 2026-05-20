package net.rebel459.music_and_melody.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.rebel459.music_and_melody.client.screen.AlbumDetailsScreen;
import net.rebel459.music_and_melody.network.StructureMusicHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EventHelper {

    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static int structureRequestCooldown;
    private static int musicBreak;
    private static boolean blockingForEventMusic;
    private static boolean cooldownEmptyMusic;
    private static boolean fading;
    private static boolean finishedFading;
    private static QueuedEvent queuedEvent;

    private static Event.PriorityType lastPriority = Event.PriorityType.LOW;
    private static List<Event.Condition> lastConditions = List.of();
    private static boolean shouldSustain = true;
    private static Event.CategoryType lastCategory;
    private static Identifier lastMusic;

    public static boolean hasWitherBossBar() {
        return Minecraft.getInstance().gui.getBossOverlay().events.values().stream().anyMatch(event -> event.getName().getString().equals(Component.translatable("entity.minecraft.wither").getString()));
    }

    public static boolean isEndPortalFilled() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        return manager.soundEngine.instanceBySource.values().stream().filter(instance -> SoundEvents.END_PORTAL_SPAWN.location().equals(instance.getIdentifier())).anyMatch(manager::isActive);
    }

    public static Music processEventMusic() {
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
        if (fading) {
            if (!events.isEmpty()) {
                Event event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
                if (queuedEvent == null || event.priority.ordinal() >= queuedEvent.event().priority.ordinal()) {
                    queuedEvent = new QueuedEvent(event, true);
                }
            }
            return null;
        }

        if (!events.isEmpty()) {
            Event event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
            boolean activeMusic = hasActiveNonEmptyMusic();
            boolean storedEventActive = isStoredEventMusicActive();
            if (activeMusic && !storedEventActive) {
                clearStoredEvent();
            }

            Event.PriorityType currentPriority = storedEventActive ? lastPriority : Event.PriorityType.LOW;
            boolean higherPriority = event.priority.ordinal() > currentPriority.ordinal();
            if (higherPriority) {
                cooldownEmptyMusic = false;
                if (activeMusic) {
                    queueEventFade(event, true);
                    return null;
                }
                stopEmptyMusic();
                return playEventOrBlockVanilla(Minecraft.getInstance(), event, true);
            }

            if (musicBreak > 0) {
                musicBreak--;
                blockingForEventMusic = true;
                cooldownEmptyMusic = !activeMusic;
                return activeMusic ? null : PlaylistHelper.EMPTY;
            }

            if (blockingForEventMusic) {
                if (activeMusic) {
                    cooldownEmptyMusic = false;
                    return null;
                }
                blockingForEventMusic = false;
                cooldownEmptyMusic = false;
                stopEmptyMusic();
                return playEventOrBlockVanilla(Minecraft.getInstance(), event, true);
            }

            if (!blockingForEventMusic && activeMusic && !isCurrentEventMusicStillApplicable(storedEventActive)) {
                musicBreak = randomMusicBreak();
                if (musicBreak > 0) {
                    musicBreak--;
                    blockingForEventMusic = true;
                    cooldownEmptyMusic = false;
                    return null;
                }
            }

            if (!blockingForEventMusic && activeMusic) {
                cooldownEmptyMusic = false;
                return null;
            }

            musicBreak = randomMusicBreak();
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
        clearFadeEvent();
        stopEmptyMusic();
        if (!isStoredEventMusicActive()) {
            clearStoredEvent();
        }
        return null;
    }

    public static void resetMusicBreak() {
        musicBreak = 0;
        blockingForEventMusic = false;
        cooldownEmptyMusic = false;
        clearFadeEvent();
    }

    public static boolean isCooldownEmptyMusic() {
        return cooldownEmptyMusic;
    }

    public static boolean isFading() {
        return fading;
    }

    public static boolean shouldContinueEventFade() {
        return fading && queuedEvent != null && shouldBeActive(queuedEvent.event().conditions, StructureMode.IGNORE);
    }

    public static boolean shouldFadeCurrentMusic(SoundInstance currentMusic) {
        return isStoredPoolMusic(currentMusic)
                && !shouldSustain
                && !lastConditions.isEmpty()
                && !storedConditionsStillActive()
                && !PlaylistHelper.isPlaying();
    }

    public static void clearStoredEventMusic(SoundInstance currentMusic) {
        if (isStoredPoolMusic(currentMusic)) {
            clearStoredEvent();
        }
    }

    public static void stopOldEventMusic() {
        if (lastCategory == null || lastCategory == Event.CategoryType.POOL || shouldSustain || lastConditions.isEmpty()) {
            return;
        }
        if (PlaylistHelper.isEventPlaying() && !shouldBeActive(lastConditions)) {
            PlaylistHelper.stopEvent();
            clearStoredEvent();
        }
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
            Optional<Album> album = Album.ALBUMS.stream().filter(entry -> entry.album.equals(event.music)).findFirst();
            playEvent = album.filter(value -> playRandomEventSong(client, AlbumDetailsScreen.queueSongs(value, client))).isPresent();
        }
        if (event.category == Event.CategoryType.PLAYLIST) {
            Optional<Playlist> playlist = Playlist.PLAYLISTS.stream().filter(entry -> entry.playlist.equals(event.music)).findFirst();
            if (!playlist.isEmpty()) {
                List<Identifier> songs = new ArrayList<>(playlist.get().tracks);
                playlist.get().discs.stream()
                        .map(disc -> MusicDiscHelper.discSoundId(client, disc))
                        .forEach(songs::add);
                playEvent = playRandomEventSong(client, songs);
            }
        }
        if (event.category == Event.CategoryType.POOL) {
            Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.get(event.music);
            if (sound.isEmpty()) return null;
            storeEvent(event);
            return new Music(sound.get(), 0, 0, replaceCurrentMusic);
        }
        if (event.category == Event.CategoryType.SONG) {
            playEvent = PlaylistHelper.playEvent(event.music, false);
        }
        if (event.category == Event.CategoryType.DISC && MusicDiscHelper.isDiscUnlocked(client, event.music)) {
            playEvent = PlaylistHelper.playEvent(MusicDiscHelper.discSoundId(client, event.music), false);
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
        if (!shouldBeActive(event.conditions, StructureMode.IGNORE)) return null;
        return playEventOrBlockVanilla(Minecraft.getInstance(), event, queued.replaceCurrentMusic());
    }

    public static void clearFadeEvent() {
        fading = false;
        finishedFading = false;
        queuedEvent = null;
    }

    public static void finishFade() {
        fading = false;
        finishedFading = true;
    }

    public static void resetStoredEvent() {
        clearStoredEvent();
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

    private static boolean hasActiveNonEmptyMusic() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Collection<SoundInstance> instances = manager.soundEngine.instanceBySource.get(SoundSource.MUSIC);
        for (SoundInstance instance : instances) {
            if (!PlaylistHelper.isEmptyMusic(instance) && manager.isActive(instance)) return true;
        }
        return false;
    }

    private static Music playEventOrBlockVanilla(Minecraft client, Event event, boolean replaceCurrentMusic) {
        Music music = playEvent(client, event, replaceCurrentMusic);
        if (music != null) {
            return music;
        }

        cooldownEmptyMusic = true;
        return PlaylistHelper.EMPTY;
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
        return storedEventActive && !lastConditions.isEmpty() && storedConditionsStillActive();
    }

    private static boolean storedConditionsStillActive() {
        return shouldBeActive(lastConditions, StructureMode.NORMAL);
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
            if (lastMusic.equals(instance.getIdentifier()) && manager.isActive(instance)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStoredPoolMusic(SoundInstance currentMusic) {
        return currentMusic != null && lastCategory == Event.CategoryType.POOL && currentMusic.getIdentifier().equals(lastMusic);
    }

    private static void clearStoredEvent() {
        lastPriority = Event.PriorityType.LOW;
        lastConditions = List.of();
        shouldSustain = true;
        lastCategory = null;
        lastMusic = null;
    }

    private static int randomMusicBreak() {
        Pair<Integer, Integer> frequency = getMusicFrequency();
        return SoundInstance.createUnseededRandom().nextIntBetweenInclusive(frequency.getFirst(), frequency.getSecond()) * 20;
    }

    private static boolean playRandomEventSong(Minecraft client, List<Identifier> songs) {
        List<Identifier> playableSongs = songs.stream()
                .filter(song -> MusicDiscHelper.isSoundUnlocked(client, song))
                .toList();
        if (playableSongs.isEmpty()) return false;
        Identifier song = playableSongs.get(SoundInstance.createUnseededRandom().nextInt(playableSongs.size()));
        return PlaylistHelper.playEvent(song, false);
    }

    private static void processEvents(WeightedList.Builder<Event> validEvents, Set<Event> events) {
        for (Event event : events) {
            boolean shouldBeActive = EventHelper.shouldBeActive(event.conditions);
            if (shouldBeActive) validEvents.add(event, event.weight);
        }
    }

    public static boolean shouldBeActive(List<Event.Condition> conditions) {
        return shouldBeActive(conditions, StructureMode.NORMAL);
    }

    private static boolean shouldBeActive(List<Event.Condition> conditions, StructureMode structureMode) {
        boolean shouldBeActive = true;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        Level level = client.level;
        for (Event.Condition condition : conditions) {
            if (condition.type() == Event.ConditionType.ALL_OF) {
                shouldBeActive = shouldBeActive && shouldBeActive(condition.conditions(), structureMode);
            }
            if (condition.type() == Event.ConditionType.ANY_OF) {
                shouldBeActive = shouldBeActive && condition.conditions().stream().anyMatch(nested -> shouldBeActive(List.of(nested), structureMode));
            }
            if (condition.type() == Event.ConditionType.BIOME) {
                shouldBeActive = shouldBeActive && player != null && level != null && level.getBiome(player.blockPosition()).is(condition.idValue().get());
            }
            if (condition.type() == Event.ConditionType.BIOME_TAG) {
                shouldBeActive = shouldBeActive && player != null && level != null && level.getBiome(player.blockPosition()).is(TagKey.create(Registries.BIOME, condition.idValue().get()));
            }
            if (condition.type() == Event.ConditionType.DIMENSION) {
                shouldBeActive = shouldBeActive && level != null && level.dimension().identifier().equals(condition.idValue().get());
            }
            if (condition.type() == Event.ConditionType.STRUCTURE) {
                shouldBeActive = shouldBeActive && StructureMusicHandler.getClientStructures().structures().contains(condition.idValue().get());
            }
            if (condition.type() == Event.ConditionType.STRUCTURE_TAG) {
                shouldBeActive = shouldBeActive && StructureMusicHandler.getClientStructures().tags().contains(condition.idValue().get());
            }
            if (condition.type() == Event.ConditionType.TIME) {
                shouldBeActive = shouldBeActive && level != null;
                if (level != null) {
                    long time = Math.floorMod(level.getDefaultClockTime(), 24000L);
                    switch (condition.timeValue().get()) {
                        case DAY -> shouldBeActive = shouldBeActive && time >= 0 && time < 12000;
                        case SUNSET -> shouldBeActive = shouldBeActive && time >= 12000 && time < 13000;
                        case NIGHT -> shouldBeActive = shouldBeActive && time >= 13000 && time < 23000;
                        case SUNRISE -> shouldBeActive = shouldBeActive && time >= 23000 && time < 24000;
                    }
                }
            }
            if (condition.type() == Event.ConditionType.WEATHER) {
                shouldBeActive = shouldBeActive && level != null;
                if (level != null) {
                    switch (condition.weatherValue().get()) {
                        case CLEAR -> shouldBeActive = shouldBeActive && !level.isRaining();
                        case RAIN -> shouldBeActive = shouldBeActive && level.isRaining();
                        case THUNDER -> shouldBeActive = shouldBeActive && level.isThundering();
                    }
                }
            }
            if (condition.type() == Event.ConditionType.GAME_MODE) {
                shouldBeActive = shouldBeActive && client.gameMode != null && matchesGameMode(client.gameMode.getPlayerMode(), condition.gameModeValue().get());
            }
            if (condition.type() == Event.ConditionType.EVENT) {
                switch (condition.eventValue().get()) {
                    case MENU -> shouldBeActive = shouldBeActive && client.screen != null && level == null;
                    case DRAGON -> shouldBeActive = shouldBeActive && level != null && level.dimension() == Level.END && client.gui.getBossOverlay().shouldPlayMusic();
                    case WITHER -> shouldBeActive = shouldBeActive && EventHelper.hasWitherBossBar();
                    case END_PORTAL -> shouldBeActive = shouldBeActive && EventHelper.isEndPortalFilled();
                    case UNDER_WATER -> shouldBeActive = shouldBeActive && player != null && player.isUnderWater();
                }
            }
            if (condition.type() == Event.ConditionType.ABOVE_Y) {
                shouldBeActive = shouldBeActive && player != null && player.blockPosition().getY() > condition.intValue().get();
            }
            if (condition.type() == Event.ConditionType.BELOW_Y) {
                shouldBeActive = shouldBeActive && player != null && player.blockPosition().getY() < condition.intValue().get();
            }
        }
        return shouldBeActive;
    }

    private static boolean matchesGameMode(GameType current, Event.GameModeCondition condition) {
        return switch (condition) {
            case SURVIVAL -> current == GameType.SURVIVAL;
            case CREATIVE -> current == GameType.CREATIVE;
            case ADVENTURE -> current == GameType.ADVENTURE;
            case SPECTATOR -> current == GameType.SPECTATOR;
        };
    }

    private record QueuedEvent(Event event, boolean replaceCurrentMusic) {}

    private enum StructureMode {
        NORMAL,
        IGNORE
    }
}
