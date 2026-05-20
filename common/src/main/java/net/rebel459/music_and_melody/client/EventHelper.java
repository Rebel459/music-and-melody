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
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.rebel459.music_and_melody.client.screen.AlbumDetailsScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.unified.network.StructurePackets;
import net.rebel459.unified.platform.client.UnifiedClientHelpers;
import net.rebel459.unified.util.mixin.PlayerStructureMusic;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EventHelper {

    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static int structureRequestCooldown;

    public static Event.PriorityType lastPriority = Event.PriorityType.VERY_LOW;
    public static List<Event.Condition> lastConditions = List.of();
    public static boolean shouldSustain = true;

    public static boolean hasWitherBossBar() {
        return Minecraft.getInstance().gui.getBossOverlay().events.values().stream().anyMatch(event -> event.getName().getString().equals(Component.translatable("entity.minecraft.wither").getString()));
    }

    public static boolean isEndPortalFilled() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        return manager.soundEngine.instanceBySource.values().stream().filter(instance -> SoundEvents.END_PORTAL_SPAWN.location().equals(instance.getIdentifier())).anyMatch(manager::isActive);
    }

    public static Music processEventMusic(WeightedList<Event> events) {
        if (!events.isEmpty()) {
            Event event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
            if (PlaylistHelper.hasActiveMusic() && (event.priority.ordinal() <= EventHelper.lastPriority.ordinal())) {
                return null;
            }
            if (playEvent(Minecraft.getInstance(), event)) {
                Music music = PlaylistHelper.EMPTY;
                if (event.category == Event.CategoryType.POOL) {
                    Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.get(event.music);
                    Pair<Integer, Integer> frequency = getMusicFrequency();
                    if (sound.isPresent()) music = new Music(sound.get(), frequency.getFirst() * 20, frequency.getSecond() * 20, event.priority.ordinal() <= EventHelper.lastPriority.ordinal());
                }
                EventHelper.lastPriority = event.priority;
                EventHelper.lastConditions = event.conditions;
                EventHelper.shouldSustain = event.sustain;
                return music;
            }
        }
        return null;
    }

    public static WeightedList<Event> getValidEvents() {
        WeightedList.Builder<Event> validEvents = new WeightedList.Builder<>();
        processEvents(validEvents, Event.VERY_HIGH_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.HIGH_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.MEDIUM_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.LOW_PRIORITY);
        if (validEvents.build().isEmpty()) processEvents(validEvents, Event.VERY_LOW_PRIORITY);
        return validEvents.build();
    }

    public static Pair<Integer, Integer> getMusicFrequency() {
        MusicManager.MusicFrequency frequency = Minecraft.getInstance().getMusicManager().gameMusicFrequency;
        return switch (frequency) {
            case DEFAULT -> Pair.of(600, 1200);
            case FREQUENT -> Pair.of(300, 600);
            case CONSTANT -> Pair.of(0, 0);
        };
    }

    private static boolean playEvent(Minecraft client, Event event) {
        if (event.category == Event.CategoryType.ALBUM) {
            Optional<Album> album = Album.ALBUMS.stream().filter(entry -> entry.album.equals(event.music)).findFirst();
            return album.filter(value -> playRandomEventSong(client, AlbumDetailsScreen.queueSongs(value, client))).isPresent();
        }
        if (event.category == Event.CategoryType.PLAYLIST) {
            Optional<Playlist> playlist = Playlist.PLAYLISTS.stream().filter(entry -> entry.playlist.equals(event.music)).findFirst();
            if (playlist.isEmpty()) return false;
            List<Identifier> songs = new ArrayList<>(playlist.get().tracks);
            playlist.get().discs.stream()
                    .map(disc -> MusicDiscHelper.discSoundId(client, disc))
                    .forEach(songs::add);
            return playRandomEventSong(client, songs);
        }
        if (event.category == Event.CategoryType.POOL) {
            return true;
        }
        if (event.category == Event.CategoryType.SONG) {
            return PlaylistHelper.play(event.music, false);
        }
        if (event.category == Event.CategoryType.DISC && MusicDiscHelper.isDiscUnlocked(client, event.music)) {
            return PlaylistHelper.play(MusicDiscHelper.discSoundId(client, event.music), false);
        }
        return false;
    }

    private static boolean playRandomEventSong(Minecraft client, List<Identifier> songs) {
        List<Identifier> playableSongs = songs.stream()
                .filter(song -> MusicDiscHelper.isSoundUnlocked(client, song))
                .toList();
        if (playableSongs.isEmpty()) return false;
        Identifier song = playableSongs.get(SoundInstance.createUnseededRandom().nextInt(playableSongs.size()));
        return PlaylistHelper.play(song, false);
    }

    private static void processEvents(WeightedList.Builder<Event> validEvents, Set<Event> events) {
        for (Event event : events) {
            boolean shouldBeActive = EventHelper.shouldBeActive(event.conditions);
            if (shouldBeActive) validEvents.add(event, event.weight);
        }
    }

    public static boolean shouldBeActive(List<Event.Condition> conditions) {
        boolean shouldBeActive = true;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        Level level = client.level;
        for (Event.Condition condition : conditions) {
            if (condition.type() == Event.ConditionType.ALL_OF) {
                shouldBeActive = shouldBeActive && shouldBeActive(condition.conditions());
            }
            if (condition.type() == Event.ConditionType.ANY_OF) {
                shouldBeActive = shouldBeActive && condition.conditions().stream().anyMatch(nested -> shouldBeActive(List.of(nested)));
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
                shouldBeActive = shouldBeActive && condition.idValue().get().equals(pieceStructure(player));
            }
            if (condition.type() == Event.ConditionType.STRUCTURE_TAG) {
                Identifier structureId = pieceStructure(player);
                shouldBeActive = shouldBeActive && level != null && !isEmptyStructure(structureId);
                if (level != null && !isEmptyStructure(structureId)) {
                    Optional<Holder.Reference<Structure>> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(structureId);
                    shouldBeActive = shouldBeActive && structure.isPresent() && structure.get().is(TagKey.create(Registries.STRUCTURE, condition.idValue().get()));
                }
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

    private static Identifier pieceStructure(Player player) {
        if (!(player instanceof PlayerStructureMusic music)) {
            return EMPTY_STRUCTURE;
        }

        Identifier structure = music.getPieceStructure();
        if (isEmptyStructure(structure)) {
            requestStructureSyncThrottled();
            return EMPTY_STRUCTURE;
        }

        return structure;
    }

    private static boolean isEmptyStructure(Identifier structure) {
        return structure == null || EMPTY_STRUCTURE.equals(structure);
    }

    private static void requestStructureSyncThrottled() {
        if (structureRequestCooldown > 0) {
            structureRequestCooldown--;
            return;
        }

        requestStructureSync();
    }

    public static void requestStructureSync() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return;
        }

        UnifiedClientHelpers.NETWORKING.send(new StructurePackets.Request());
        structureRequestCooldown = 20;
    }

    private static boolean matchesGameMode(GameType current, Event.GameModeCondition condition) {
        return switch (condition) {
            case SURVIVAL -> current == GameType.SURVIVAL;
            case CREATIVE -> current == GameType.CREATIVE;
            case ADVENTURE -> current == GameType.ADVENTURE;
            case SPECTATOR -> current == GameType.SPECTATOR;
        };
    }
}
