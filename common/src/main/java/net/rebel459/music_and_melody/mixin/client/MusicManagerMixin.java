package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;
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

    @Unique
    private SoundEvent toastSoundEvent = null;

    @WrapOperation(
            method = "startPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/sounds/Music;sound()Lnet/minecraft/core/Holder;"
            )
    )
    private Holder<SoundEvent> getToastEvent(Music music, Operation<Holder<SoundEvent>> original) {
        Holder<SoundEvent> event = original.call(music);
        this.toastSoundEvent = event.value();
        return event;
    }

    @WrapOperation(
            method = "startPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;showNowPlayingToast()V"
            )
    )
    private void hideEmptyToast(ToastManager toastManager, Operation<Void> original) {
        if (this.toastSoundEvent != null && this.toastSoundEvent.location().equals(MaMSounds.EMPTY.value().location())) return;
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

        clearEmptyMusic();
        return music;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickClearEmptyMusic(CallbackInfo ci) {
        clearEmptyMusic();
    }

    @Unique
    private void clearEmptyMusic() {
        if (!PlaylistHelper.isEmptyMusic(this.currentMusic) || EventHelper.isCooldownEmptyMusic() || PlaylistHelper.isPlaying() || !MaMDataConfig.get().vanilla_music) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(this.currentMusic);
        this.currentMusic = null;
        this.nextSongDelay = Math.max(this.nextSongDelay, EventHelper.randomMusicBreak());
    }
}
