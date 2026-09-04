package net.rebel459.music_and_melody.mixin.integration.simple_music_control;

import me.pajic.simple_music_control.gui.NowPlayingWidget;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NowPlayingWidget.class)
public abstract class SimpleMusicWidgetMixin {

    @Shadow
    private static Component trackName;

    @Inject(method = "displayWidget(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private static void hideEmptyMusic(SoundInstance sound, CallbackInfo ci) {
        if (PlaylistHelper.isEmptyMusic(sound)) ci.cancel();
    }

    @Inject(method = "displayWidget(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("TAIL"))
    private static void useMusicAndMelodyTrackName(SoundInstance sound, CallbackInfo ci) {
        String key = PlaylistHelper.getMusicTranslationKey(sound);
        if (key == null) return;
        if (key.startsWith(PlaylistHelper.LITERAL_TRANSLATION_PREFIX)) {
            trackName = Component.literal(key.substring(PlaylistHelper.LITERAL_TRANSLATION_PREFIX.length()));
            return;
        }
        trackName = Component.translatable(key);
    }
}
