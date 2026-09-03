package net.rebel459.music_and_melody.mixin.integration.fancymenu;

import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElement;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.sounds.SoundSource;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMDataConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AudioElement.class, remap = false)
public abstract class AudioElementMixin {

    @Shadow
    public IAudio currentAudio;

    @Shadow
    public abstract SoundSource getSoundSource();

    @Unique
    private IAudio pausedAudio;

    @Inject(method = "renderTick", at = @At("HEAD"), cancellable = true)
    private void pauseAudioElementDuringPlaylist(CallbackInfo ci) {
        if (!MaMDataConfig.get().vanilla_music || PlaylistHelper.isPlaylistOrAlbumPlaying() && this.getSoundSource() == SoundSource.MUSIC) {
            if (this.currentAudio != null && this.currentAudio.isReady() && this.currentAudio.isPlaying()) {
                this.currentAudio.pause();
                this.pausedAudio = this.currentAudio;
            }
            ci.cancel();
            return;
        }

        if (this.pausedAudio == null) return;
        if (this.currentAudio == this.pausedAudio && this.currentAudio.isReady() && this.currentAudio.isPaused()) {
            this.currentAudio.play();
        }
        this.pausedAudio = null;
    }
}
