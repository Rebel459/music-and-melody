package net.rebel459.music_and_melody.mixin.integration.fancymenu;

import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.sounds.SoundSource;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlobalCustomizationHandler.class, remap = false)
public abstract class GlobalCustomizationHandlerMixin {

    @Unique
    private static IAudio pausedMenuMusic;

    @Inject(method = "tickMenuMusic", at = @At("HEAD"), cancellable = true)
    private static void pauseMenuMusicDuringPlaylist(CallbackInfo ci) {
        IAudio current = GlobalCustomizationHandlerAccessor.getCurrentMenuMusic();
        if (PlaylistHelper.isPlaylistOrAlbumPlaying() && current != null && current.getSoundChannel() == SoundSource.MUSIC) {
            if (current != null && current.isReady() && current.isPlaying()) {
                current.pause();
                pausedMenuMusic = current;
            }
            ci.cancel();
            return;
        }

        if (pausedMenuMusic == null) return;
        if (current == pausedMenuMusic && current.isReady() && current.isPaused()) {
            current.play();
        }
        pausedMenuMusic = null;
    }
}
