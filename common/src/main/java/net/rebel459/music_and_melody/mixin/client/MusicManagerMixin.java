package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import net.rebel459.music_and_melody.sound.MaMSounds;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow
    public abstract void stopPlaying();

    @Shadow
    @Nullable
    private SoundInstance currentMusic;

    @Shadow
    private int nextSongDelay;

    @Shadow
    private float currentGain;

    @Inject(method = "getCurrentMusicTranslationKey", at = @At("HEAD"), cancellable = true)
    private void playlistMusicTranslationKey(CallbackInfoReturnable<String> cir) {
        String key = PlaylistHelper.getCurrentMusicTranslationKey();
        if (key != null) cir.setReturnValue(key);
    }

    @WrapOperation(
            method = "startPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;showNowPlayingToast()V"
            )
    )
    private void hideEmptyToast(ToastManager toastManager, Operation<Void> original, Music music) {
        if (music.sound().value().location().equals(MaMSounds.REGISTERED_SOUNDS.get("music.empty").value().location())) return;
        original.call(toastManager);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMusicVolume()F"
            )
    )
    private float fadeMusic(float volume) {
        if (EventHelper.shouldFadeCurrentMusic(this.currentMusic)) return 0F;
        return volume;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getSituationalMusic()Lnet/minecraft/sounds/Music;"
            )
    )
    private Music afterSituationalMusic(Music music) {
        if (EventHelper.shouldFadeCurrentMusic(this.currentMusic) && this.currentGain <= 1.0E-4F) {
            SoundInstance stoppedMusic = this.currentMusic;
            this.stopPlaying();
            EventHelper.clearStoredEventMusic(stoppedMusic);
        }

        musicAndMelody$clearEmptyMusic();
        return music;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clearEmptyMusic(CallbackInfo ci) {
        musicAndMelody$clearEmptyMusic();
    }

    @Unique
    private void musicAndMelody$clearEmptyMusic() {
        if (!PlaylistHelper.isEmptyMusic(this.currentMusic) || EventHelper.isCooldownEmptyMusic() || PlaylistHelper.isPlaying() || !MaMClientConfig.get().vanilla_music) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(this.currentMusic);
        this.currentMusic = null;
        this.nextSongDelay = Math.max(this.nextSongDelay, EventHelper.randomMusicBreak());
    }
}
