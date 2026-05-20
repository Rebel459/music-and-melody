package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.rebel459.music_and_melody.client.*;
import net.rebel459.music_and_melody.client.screen.AlbumDetailsScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.tag.MaMBiomeTags;
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

@Mixin(value = Minecraft.class, priority = 500)
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

        if (Event.targetBreak == -1) {
            Event.currentBreak = 0;
            Event.targetBreak = Event.createMusicBreak();
        }
        if (Event.currentBreak < Event.targetBreak) {
            Event.currentBreak++;
        } else {
            WeightedList.Builder<Event> validEvents = new WeightedList.Builder<>();
            processEvents(validEvents, Event.VERY_HIGH_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, Event.HIGH_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, Event.MEDIUM_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, Event.LOW_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, Event.VERY_LOW_PRIORITY);

            WeightedList<Event> events = validEvents.build();
            if (!events.isEmpty()) {
                Event event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
                if (PlaylistHelper.hasActiveMusic() && event.priority.ordinal() <= EventHelper.lastPriority.ordinal()) {
                    return;
                }
                if (playEvent(client, event)) {
                    EventHelper.lastPriority = event.priority;
                    EventHelper.lastConditions = event.conditions;
                    EventHelper.shouldSustain = event.sustain;
                    Event.currentBreak = 0;
                    Event.targetBreak = Event.createMusicBreak();
                    if (event.category == Event.CategoryType.POOL) {
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

        EventHelper.shouldSustain = false;
        EventHelper.lastConditions = List.of();
        EventHelper.lastPriority = Event.PriorityType.LOW;

        if (!MaMClientConfig.get().background_music) cir.setReturnValue(PlaylistHelper.EMPTY);
    }

    @Unique
    private boolean playEvent(Minecraft client, Event event) {
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

    @Unique
    private boolean playRandomEventSong(Minecraft client, List<Identifier> songs) {
        List<Identifier> playableSongs = songs.stream()
                .filter(song -> MusicDiscHelper.isSoundUnlocked(client, song))
                .toList();
        if (playableSongs.isEmpty()) return false;
        Identifier song = playableSongs.get(SoundInstance.createUnseededRandom().nextInt(playableSongs.size()));
        return PlaylistHelper.play(song, false);
    }

    private void processEvents(WeightedList.Builder<Event> validEvents, Set<Event> events) {
        for (Event event : events) {
            boolean shouldBeActive = EventHelper.shouldBeActive(event.conditions);
            if (shouldBeActive) validEvents.add(event, event.weight);
        }
    }
}
