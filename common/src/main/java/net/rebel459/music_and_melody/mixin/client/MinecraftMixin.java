package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.rebel459.music_and_melody.client.*;
import net.rebel459.music_and_melody.client.screen.AlbumDetailsScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.tag.MaMBiomeTags;
import net.rebel459.unified.util.mixin.PlayerStructureMusic;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    @Final
    public Gui gui;

    @WrapOperation(method = "getSituationalMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BackgroundMusic;select(ZZ)Ljava/util/Optional;"))
    private Optional<Music> musicFixesAndSituationalMusic(BackgroundMusic music, boolean isCreative, boolean isUnderwater, Operation<Optional<Music>> original, @Local(name = "playerLevel") Level playerLevel) {
        Holder<Biome> biome = playerLevel.getBiome(this.player.blockPosition());
        if (MaMClientConfig.get().creative_fix && (playerLevel.dimension() == Level.OVERWORLD || biome.is(MaMBiomeTags.HAS_CREATIVE_MUSIC)) && music.creativeMusic().isEmpty()) music = new BackgroundMusic(music.defaultMusic(), Optional.of(Musics.CREATIVE), music.underwaterMusic());
        if (MaMClientConfig.get().under_water_fix && (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(MaMBiomeTags.HAS_UNDER_WATER_MUSIC)) && music.underwaterMusic().isEmpty()) music = new BackgroundMusic(music.defaultMusic(), music.creativeMusic(), Optional.of(Musics.UNDER_WATER));
        return original.call(music, isCreative, isUnderwater);
    }

    private EventMusic.PriorityType lastPriority = EventMusic.PriorityType.VERY_LOW;

    @Inject(method = "getSituationalMusic", at = @At(value = "HEAD"), cancellable = true)
    private void playlistAndEventMusic(CallbackInfoReturnable<Music> cir) {
        Minecraft client = Minecraft.class.cast(this);

        if (PlaylistHelper.isPlaying()) {
            cir.setReturnValue(PlaylistHelper.EMPTY);
            return;
        }
        if (PlaylistHelper.playNext()) {
            cir.setReturnValue(PlaylistHelper.EMPTY);
            return;
        }

        if (MaMClientConfig.get().background_music) {
            if (MaMClientConfig.get().end_portal_music && MusicHelper.isEndPortalFilled()) {
                lastPriority = EventMusic.PriorityType.HIGH;
                cir.setReturnValue(MusicHelper.THRESHOLD);
            }
            if (MaMClientConfig.get().wither_music && MusicHelper.hasWitherBossBar()) {
                lastPriority = EventMusic.PriorityType.HIGH;
                cir.setReturnValue(MusicHelper.WITHER_BOSS);
            }
            if (client.level != null && client.level.dimension() == Level.END && client.gui.getBossOverlay().shouldPlayMusic()) {
                lastPriority = EventMusic.PriorityType.VERY_HIGH;
                cir.setReturnValue(Musics.END_BOSS);
            }
        }

        if (EventMusic.targetBreak == -1) {
            EventMusic.currentBreak = 0;
            EventMusic.targetBreak = EventMusic.createMusicBreak();
        }
        if (EventMusic.currentBreak < EventMusic.targetBreak) {
            EventMusic.currentBreak++;
        } else {
            WeightedList.Builder<EventMusic> validEvents = new WeightedList.Builder<>();
            processEvents(validEvents, EventMusic.VERY_HIGH_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, EventMusic.HIGH_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, EventMusic.MEDIUM_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, EventMusic.LOW_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, EventMusic.VERY_LOW_PRIORITY);

            WeightedList<EventMusic> events = validEvents.build();
            if (!events.isEmpty()) {
                EventMusic event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
                if (PlaylistHelper.hasActiveMusic() && event.priority.ordinal() <= lastPriority.ordinal()) {
                    return;
                }
                if (playEvent(client, event)) {
                    EventMusic.currentBreak = 0;
                    EventMusic.targetBreak = EventMusic.createMusicBreak();
                    if (event.category == EventMusic.CategoryType.POOL) {
                        Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.get(event.music);
                        sound.ifPresent(soundEventReference -> cir.setReturnValue(new Music(soundEventReference, MaMClientConfig.get().event_music_min * 20, MaMClientConfig.get().event_music_max * 20, true)));
                    }
                    else cir.setReturnValue(PlaylistHelper.EMPTY);
                    return;
                }
            }
        }

        if (PlaylistHelper.hasActiveMusic()) {
            return;
        }

        lastPriority = EventMusic.PriorityType.LOW;

        if (!MaMClientConfig.get().background_music) cir.setReturnValue(PlaylistHelper.EMPTY);
    }

    @Unique
    private boolean playEvent(Minecraft client, EventMusic event) {
        if (event.category == EventMusic.CategoryType.ALBUM) {
            Optional<Album> album = Album.ALBUMS.stream().filter(entry -> entry.album.equals(event.music)).findFirst();
            return album.filter(value -> playRandomEventSong(client, AlbumDetailsScreen.queueSongs(value, client))).isPresent();
        }
        if (event.category == EventMusic.CategoryType.PLAYLIST) {
            Optional<Playlist> playlist = Playlist.PLAYLISTS.stream().filter(entry -> entry.playlist.equals(event.music)).findFirst();
            if (playlist.isEmpty()) return false;
            List<Identifier> songs = new ArrayList<>(playlist.get().tracks);
            playlist.get().discs.stream()
                    .map(disc -> MusicDiscHelper.discSoundId(client, disc))
                    .forEach(songs::add);
            return playRandomEventSong(client, songs);
        }
        if (event.category == EventMusic.CategoryType.POOL) {
            return true;
        }
        if (event.category == EventMusic.CategoryType.SONG) {
            return PlaylistHelper.play(event.music, false);
        }
        if (event.category == EventMusic.CategoryType.DISC && MusicDiscHelper.isDiscUnlocked(client, event.music)) {
            return PlaylistHelper.play(MusicDiscHelper.discSoundId(client, event.music), false);
        }
        return false;
    }

    @Unique
    private boolean playRandomEventSong(Minecraft client, List<Identifier> songs) {
        List<Identifier> playableSongs = songs.stream()
                .filter(song -> MusicDiscHelper.isSoundUnlocked(client, song))
                .toList();
        if (playableSongs.isEmpty()) return false;
        Identifier song = playableSongs.get(SoundInstance.createUnseededRandom().nextInt(playableSongs.size()));
        return PlaylistHelper.play(song, false);
    }

    @Unique
    private void processEvents(WeightedList.Builder<EventMusic> validEvents, Set<EventMusic> events) {
        LocalPlayer player = this.player;
        ClientLevel level = this.level;
        for (EventMusic event : events) {
            boolean shouldBeActive = true;
            for (EventMusic.Condition condition : event.conditions) {
                if (condition.type() == EventMusic.ConditionType.BIOME) {
                    shouldBeActive = shouldBeActive && player != null && level != null && level.getBiome(this.player.blockPosition()).is(condition.idValue().get());
                }
                if (condition.type() == EventMusic.ConditionType.BIOME_TAG) {
                    shouldBeActive = shouldBeActive && player != null && level != null && level.getBiome(this.player.blockPosition()).is(TagKey.create(Registries.BIOME, condition.idValue().get()));
                }
                if (condition.type() == EventMusic.ConditionType.DIMENSION) {
                    shouldBeActive = shouldBeActive && level != null && level.dimension().identifier().equals(condition.idValue().get());
                }
                if (condition.type() == EventMusic.ConditionType.STRUCTURE) {
                    shouldBeActive = shouldBeActive && player instanceof PlayerStructureMusic music && condition.idValue().get().equals(music.getPieceStructure());
                }
                if (condition.type() == EventMusic.ConditionType.TIME) {
                    shouldBeActive = shouldBeActive && level != null;
                    if (level != null) {
                        long time = Math.floorMod(level.getDefaultClockTime(), 24000L);
                        switch (condition.timeValue().get()) {
                            case DAY -> shouldBeActive = shouldBeActive && time > 0 && time <= 12000;
                            case SUNSET -> shouldBeActive = shouldBeActive && time > 12000 && time <= 13000;
                            case NIGHT -> shouldBeActive = shouldBeActive && time > 13000 && time <= 23000;
                            case SUNRISE -> shouldBeActive = shouldBeActive && time > 23000 && time <= 24000;
                        }
                    }
                }
                if (condition.type() == EventMusic.ConditionType.WEATHER) {
                    shouldBeActive = shouldBeActive && level != null;
                    if (level != null) {
                        switch (condition.weatherValue().get()) {
                            case CLEAR -> shouldBeActive = shouldBeActive && !level.isRaining();
                            case RAIN -> shouldBeActive = shouldBeActive && level.isRaining();
                            case THUNDER -> shouldBeActive = shouldBeActive && level.isThundering();
                        }
                    }
                }
                if (condition.type() == EventMusic.ConditionType.GAME_MODE) {
                    Minecraft client = Minecraft.class.cast(this);
                    shouldBeActive = shouldBeActive && client.gameMode != null && matchesGameMode(client.gameMode.getPlayerMode(), condition.gameModeValue().get());
                }
                if (condition.type() == EventMusic.ConditionType.EVENT) {
                    switch (condition.eventValue().get()) {
                        case MENU -> shouldBeActive = shouldBeActive && this.screen != null && this.level == null;
                        case DRAGON -> shouldBeActive = shouldBeActive && this.level != null && this.level.dimension() == Level.END && this.gui.getBossOverlay().shouldPlayMusic();
                        case WITHER -> shouldBeActive = shouldBeActive && MusicHelper.hasWitherBossBar();
                        case END_PORTAL -> shouldBeActive = shouldBeActive && MusicHelper.isEndPortalFilled();
                        case UNDER_WATER -> shouldBeActive = shouldBeActive && this.player != null && this.player.isUnderWater();
                    }
                }
                if (condition.type() == EventMusic.ConditionType.ABOVE_Y) {
                    shouldBeActive = shouldBeActive && player != null && player.blockPosition().getY() > condition.intValue().get();
                }
                if (condition.type() == EventMusic.ConditionType.BELOW_Y) {
                    shouldBeActive = shouldBeActive && player != null && player.blockPosition().getY() < condition.intValue().get();
                }
            }
            if (shouldBeActive) validEvents.add(event, event.weight);
        }
    }

    @Unique
    private boolean matchesGameMode(GameType current, EventMusic.GameModeCondition condition) {
        return switch (condition) {
            case SURVIVAL -> current == GameType.SURVIVAL;
            case CREATIVE -> current == GameType.CREATIVE;
            case ADVENTURE -> current == GameType.ADVENTURE;
            case SPECTATOR -> current == GameType.SPECTATOR;
        };
    }
}
