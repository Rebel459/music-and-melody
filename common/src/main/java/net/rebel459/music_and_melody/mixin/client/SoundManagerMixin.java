package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.SoundVolumeController;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundManager.class, priority = 500)
public class SoundManagerMixin {

    @Shadow
    @Final
    public SoundEngine soundEngine;

    @Unique
    private float currentVolume = -1F;

    @Inject(method = "tick", at = @At("TAIL"))
    private void fadeMusic(boolean paused, CallbackInfo ci) {
        SoundManager manager = SoundManager.class.cast(this);
        MaMClientConfig clientConfig = MaMClientConfig.get();
        float targetVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
        boolean activeJukebox = this.soundEngine.instanceBySource.get(SoundSource.RECORDS).stream().anyMatch(SoundManager.class.cast(this)::isActive);
        float fade = targetVolume * Math.clamp(clientConfig.fade_speed, 0.001F, 1F);
        if (this.currentVolume == -1F) this.currentVolume = targetVolume;
        if (activeJukebox && clientConfig.jukebox_fading) {
            this.currentVolume = Math.max(this.currentVolume - fade, 0F);
            musicAndMelody$updateMusicVolume(this.currentVolume);
        } else if (!activeJukebox && EventHelper.isFadingOutCurrentEvent()) {
            if (!EventHelper.shouldContinueCurrentEventFadeOut()) {
                EventHelper.clearCurrentEventFadeOut();
                if (this.currentVolume < targetVolume) {
                    this.currentVolume = targetVolume;
                    musicAndMelody$updateMusicVolume(targetVolume);
                }
                return;
            }
            this.currentVolume = Math.max(this.currentVolume - fade, 0F);
            musicAndMelody$updateMusicVolume(this.currentVolume);
            if (this.currentVolume <= 0.001F) {
                EventHelper.finishCurrentEventFadeOut();
                this.currentVolume = 0F;
                musicAndMelody$updateMusicVolume(this.currentVolume);
            }
        } else if (!activeJukebox && EventHelper.isFading()) {
            if (!EventHelper.shouldContinueEventFade()) {
                EventHelper.clearFadeEvent();
                if (this.currentVolume < targetVolume) {
                    this.currentVolume = targetVolume;
                    musicAndMelody$updateMusicVolume(targetVolume);
                }
                return;
            }
            this.currentVolume = Math.max(this.currentVolume - fade, 0F);
            musicAndMelody$updateMusicVolume(this.currentVolume);
            if (this.currentVolume <= 0.001F) {
                Minecraft.getInstance().getMusicManager().stopPlaying();
                EventHelper.finishFade();
                this.currentVolume = 0F;
                musicAndMelody$updateMusicVolume(this.currentVolume);
            }
        } else {
            if (this.currentVolume > targetVolume) {
                this.currentVolume = targetVolume;
                musicAndMelody$updateMusicVolume(this.currentVolume);
            } else if (this.currentVolume < targetVolume) {
                this.currentVolume = Math.min(this.currentVolume + fade, targetVolume);
                musicAndMelody$updateMusicVolume(this.currentVolume);
            }
        }
    }

    @Unique
    private void musicAndMelody$updateMusicVolume(float volume) {
        ((SoundVolumeController) this.soundEngine).setMusicVolume(volume);
    }
}
