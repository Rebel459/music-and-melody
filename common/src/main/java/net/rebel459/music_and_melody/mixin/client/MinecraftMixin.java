package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.Music;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SoundEngineStopper;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, priority = 500)
public abstract class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    @Final
    public Gui gui;

    @WrapOperation(
            method = "updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V",
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

    @Inject(method = "getSituationalMusic", at = @At(value = "RETURN"), cancellable = true)
    private void playlistAndEventMusic(CallbackInfoReturnable<Music> cir) {
        EventHelper.stopOldEventMusic();

        if (!PlaylistHelper.isPlaying() || PlaylistHelper.isEventPlaying()) {
            Music music = EventHelper.processEventMusic(cir.getReturnValue());
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

        if (!MaMClientConfig.get().vanilla_music) cir.setReturnValue(PlaylistHelper.EMPTY);
    }
}
