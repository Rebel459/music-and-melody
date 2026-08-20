package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SoundEngineStopper;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.tag.MaMBiomeTags;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = Minecraft.class, priority = 500)
public abstract class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

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

    @WrapOperation(
            method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"
            )
    )
    private void keepPlaylistMusic(SoundManager soundManager, Operation<Void> original) {
        SoundInstance currentSong = PlaylistHelper.getCurrentSong();
        if (currentSong != null && ((SoundEngineStopper) soundManager.soundEngine).stopEverythingExceptPlaylist(currentSong)) {
            return;
        }
        original.call(soundManager);
    }

    @Inject(method = "getSituationalMusic", at = @At(value = "HEAD"), cancellable = true)
    private void playlistAndEventMusic(CallbackInfoReturnable<Music> cir) {
        EventHelper.stopOldEventMusic();

        if (!PlaylistHelper.isPlaying() || PlaylistHelper.isEventPlaying()) {
            Music music = EventHelper.processEventMusic();
            if (music != null) {
                cir.setReturnValue(music);
                return;
            }
        }

        if (PlaylistHelper.isPlaying()) {
            cir.setReturnValue(PlaylistHelper.EMPTY);
            return;
        }

        if (PlaylistHelper.playNext()) {
            cir.setReturnValue(PlaylistHelper.EMPTY);
            return;
        }

        if (PlaylistHelper.hasActiveMusic()) {
            return;
        }

        EventHelper.clearStoredEvent();

        if (!MaMDataConfig.get().vanilla_music) cir.setReturnValue(PlaylistHelper.EMPTY);
    }
}
