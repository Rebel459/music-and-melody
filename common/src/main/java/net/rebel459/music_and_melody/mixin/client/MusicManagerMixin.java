package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow
    public abstract void stopPlaying();

    @Shadow
    @Nullable
    private SoundInstance currentMusic;

    @Shadow
    private int nextSongDelay;

    @Unique
    private float currentGain = 1.0F;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void musicAndMelody$fadeMusic(CallbackInfo ci) {
        float targetGain = EventHelper.shouldFadeCurrentMusic(this.currentMusic) ? 0F : 1F;
        if (this.currentMusic != null && this.currentGain != targetGain && !fadePlaying(targetGain)) {
            ci.cancel();
        }
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
        if (!PlaylistHelper.isEmptyMusic(this.currentMusic) || EventHelper.isCooldownEmptyMusic() || PlaylistHelper.isPlaying() || !MaMClientConfig.get().vanilla_music) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(this.currentMusic);
        this.currentMusic = null;
        this.nextSongDelay = Math.max(this.nextSongDelay, EventHelper.randomMusicBreak());
    }

    @Unique
    private boolean fadePlaying(float targetGain) {
        if (this.currentMusic == null) {
            return false;
        }
        if (this.currentGain == targetGain) {
            return true;
        }
        if (this.currentGain < targetGain) {
            this.currentGain += Mth.clamp(this.currentGain, 5.0E-4F, 0.005F);
            if (this.currentGain > targetGain) {
                this.currentGain = targetGain;
            }
        } else {
            this.currentGain = 0.03F * targetGain + 0.97F * this.currentGain;
            if (Math.abs(this.currentGain - targetGain) < 1.0E-4F || this.currentGain < targetGain) {
                this.currentGain = targetGain;
            }
        }
        this.currentGain = Mth.clamp(this.currentGain, 0F, 1F);
        if (this.currentGain <= 1.0E-4F) {
            SoundInstance stoppedMusic = this.currentMusic;
            this.stopPlaying();
            EventHelper.clearStoredEventMusic(stoppedMusic);
            return false;
        }
        Minecraft.getInstance().getSoundManager().updateSourceVolume(SoundSource.MUSIC, this.currentGain);
        return true;
    }
}
