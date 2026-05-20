package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
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
    private void jukeboxMusicSuppression(boolean paused, CallbackInfo ci) {
        MaMClientConfig clientConfig = MaMClientConfig.get();
        if (!clientConfig.jukebox_fading) return;
        SoundManager manager = SoundManager.class.cast(this);
        float targetVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
        float fade = targetVolume * Math.clamp(clientConfig.jukebox_fade_speed, 0.001F, 1F);
        if (this.currentVolume == -1F) this.currentVolume = targetVolume;

        if (this.soundEngine.instanceBySource.get(SoundSource.RECORDS).stream().anyMatch(SoundManager.class.cast(this)::isActive)) {
            this.currentVolume = Math.max(this.currentVolume - fade, 0F);
            manager.updateCategoryVolume(SoundSource.MUSIC, this.currentVolume);
        } else {
            if (this.currentVolume > targetVolume) {
                this.currentVolume = targetVolume;
                manager.updateCategoryVolume(SoundSource.MUSIC, this.currentVolume);
            } else if (this.currentVolume < targetVolume) {
                this.currentVolume = Math.min(this.currentVolume + fade, targetVolume);
                manager.updateCategoryVolume(SoundSource.MUSIC, this.currentVolume);
            }
        }
    }
}
