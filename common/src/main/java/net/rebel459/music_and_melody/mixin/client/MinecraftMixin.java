package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.rebel459.music_and_melody.client.*;
import net.rebel459.music_and_melody.client.screen.AlbumDetailsScreen;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.tag.MaMBiomeTags;
import net.rebel459.unified.util.helper.StructureMusicImpl;
import net.rebel459.unified.util.mixin.PlayerStructureMusic;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow
    @org.jspecify.annotations.Nullable
    public ClientLevel level;

    @Shadow
    @org.jspecify.annotations.Nullable
    public Screen screen;

    @WrapOperation(method = "getSituationalMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BackgroundMusic;select(ZZ)Ljava/util/Optional;"))
    private Optional<Music> musicFixesAndSituationalMusic(BackgroundMusic music, boolean isCreative, boolean isUnderwater, Operation<Optional<Music>> original, @Local(name = "playerLevel") Level playerLevel) {
        Holder<Biome> biome = playerLevel.getBiome(this.player.blockPosition());
        if (MaMClientConfig.get().creative_fix && (playerLevel.dimension() == Level.OVERWORLD || biome.is(MaMBiomeTags.HAS_CREATIVE_MUSIC)) && music.creativeMusic().isEmpty()) music = new BackgroundMusic(music.defaultMusic(), Optional.of(Musics.CREATIVE), music.underwaterMusic());
        if (MaMClientConfig.get().under_water_fix && (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(MaMBiomeTags.HAS_UNDER_WATER_MUSIC)) && music.underwaterMusic().isEmpty()) music = new BackgroundMusic(music.defaultMusic(), music.creativeMusic(), Optional.of(Musics.UNDER_WATER));
        return original.call(music, isCreative, isUnderwater);
    }

    @Unique
    private int currentBreak = 0;
    @Unique
    private int targetBreak = -1;

    @Inject(method = "getSituationalMusic", at = @At(value = "HEAD"), cancellable = true)
    private void playlistAndEventMusic(CallbackInfoReturnable<Music> cir) {
        Minecraft client = Minecraft.class.cast(this);

        if (targetBreak == -1) targetBreak = SoundInstance.createUnseededRandom().nextIntBetweenInclusive(300, 600);
        if (currentBreak < targetBreak) {
            currentBreak++;
        } else {
            WeightedList.Builder<EventMusic> validEvents = new WeightedList.Builder<>();
            processEvents(validEvents, EventMusic.HIGH_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, EventMusic.MEDIUM_PRIORITY);
            if (validEvents.build().isEmpty()) processEvents(validEvents, EventMusic.LOW_PRIORITY);

            WeightedList<EventMusic> events = validEvents.build();
            if (!events.isEmpty()) {
                EventMusic event = events.getRandomOrThrow(SoundInstance.createUnseededRandom());
                if (event.category == EventMusic.CategoryType.ALBUM) {
                    HashSet<Album> albums = new HashSet<>(Album.ALBUMS);
                    albums.removeIf(entry -> entry.album != event.music);
                    List<Identifier> songs = AlbumDetailsScreen.queueSongs(albums.stream().findFirst().get(), client);
                    PlaylistHelper.clear();
                    PlaylistHelper.pauseQueue();
                    PlaylistHelper.addAll(songs);
                    PlaylistHelper.playNow(0);
                }
                if (event.category == EventMusic.CategoryType.PLAYLIST) {
                    HashSet<Playlist> playlists = new HashSet<>(Playlist.PLAYLISTS);
                    playlists.removeIf(entry -> entry.playlist != event.music);
                    PlaylistHelper.clear();
                    PlaylistHelper.pauseQueue();
                    PlaylistHelper.addAll(playlists.stream().findFirst().get().tracks);
                    PlaylistHelper.playNow(0);
                }
                if (event.category == EventMusic.CategoryType.SONG) {
                    PlaylistHelper.play(event.music, false);
                }
                if (event.category == EventMusic.CategoryType.DISC && MusicDiscHelper.isDiscUnlocked(client, event.music)) {
                    PlaylistHelper.play(MusicDiscHelper.discSoundId(client, event.music), false);
                }
                currentBreak = 0;
            }
        }

        if (PlaylistHelper.isPlaying()) cir.setReturnValue(PlaylistHelper.EMPTY);
        if (PlaylistHelper.playNext() || !MaMClientConfig.get().background_music) cir.setReturnValue(PlaylistHelper.EMPTY);

        if (MaMClientConfig.get().end_portal_music && EventMusicHelper.isEndPortalFilled()) cir.setReturnValue(EventMusicHelper.THRESHOLD);
        if (MaMClientConfig.get().wither_music && EventMusicHelper.hasWitherBossBar()) cir.setReturnValue(EventMusicHelper.WITHER_BOSS);
    }

    @Unique
    private WeightedList.Builder<EventMusic> processEvents(WeightedList.Builder<EventMusic> validEvents, Set<EventMusic> events) {
        LocalPlayer player = this.player;
        ClientLevel level = this.level;
        for (EventMusic event : events) {
            boolean shouldBeActive = !events.isEmpty();
            for (EventMusic.Condition condition : event.conditions) {
                if (condition.type() == EventMusic.ConditionType.BIOME && player != null && level != null) {
                    Holder<Biome> biome = level.getBiome(this.player.blockPosition());
                    shouldBeActive = shouldBeActive && biome.is(condition.idValue().get());
                }
                if (condition.type() == EventMusic.ConditionType.BIOME_TAG && player != null && level != null) {
                    Holder<Biome> biome = level.getBiome(this.player.blockPosition());
                    shouldBeActive = shouldBeActive && biome.is(TagKey.create(Registries.BIOME, condition.idValue().get()));
                }
                if (condition.type() == EventMusic.ConditionType.STRUCTURE && player instanceof PlayerStructureMusic music) {
                    shouldBeActive = shouldBeActive && music.getPieceStructure() != null && music.getPieceStructure() == condition.idValue().get();
                }
                if (condition.type() == EventMusic.ConditionType.TIME && level != null) {
                    long time = level.getGameTime();
                    switch(condition.timeValue().get()) {
                        case DAY -> shouldBeActive = shouldBeActive && time > 0 && time <= 12000;
                        case SUNSET -> shouldBeActive = shouldBeActive && time > 12000 && time <= 13000;
                        case NIGHT -> shouldBeActive = shouldBeActive && time > 13000 && time <= 23000;
                        case SUNRISE -> shouldBeActive = shouldBeActive && time > 23000 && time <= 24000;
                    }
                }
                if (condition.type() == EventMusic.ConditionType.WEATHER && level != null) {
                    switch(condition.weatherValue().get()) {
                        case CLEAR -> shouldBeActive = shouldBeActive && !level.isRaining();
                        case RAIN -> shouldBeActive = shouldBeActive && level.isRaining();
                        case THUNDER -> shouldBeActive = shouldBeActive && level.isThundering();
                    }
                }
                if (condition.type() == EventMusic.ConditionType.MENU) {
                    shouldBeActive = shouldBeActive && this.screen != null && this.level == null;
                }
                if (condition.type() == EventMusic.ConditionType.ABOVE_Y && player != null) {
                    shouldBeActive = shouldBeActive && player.blockPosition().getY() > condition.intValue().get();
                }
                if (condition.type() == EventMusic.ConditionType.BELOW_Y && player != null) {
                    shouldBeActive = shouldBeActive && player.blockPosition().getY() < condition.intValue().get();
                }
                if (shouldBeActive) validEvents.add(event, event.weight);
            }
        }
        return validEvents;
    }
}
