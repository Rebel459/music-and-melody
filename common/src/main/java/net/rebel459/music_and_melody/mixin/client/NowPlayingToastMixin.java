package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.network.chat.Component;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NowPlayingToast.class)
public class NowPlayingToastMixin {

    @Inject(method = "getNowPlayingString", at = @At("HEAD"), cancellable = true)
    private static void fallbackNowPlayingTranslation(String currentSongKey, CallbackInfoReturnable<Component> cir) {
        if (currentSongKey != null && currentSongKey.startsWith(PlaylistHelper.LITERAL_TRANSLATION_PREFIX)) {
            cir.setReturnValue(Component.literal(currentSongKey.substring(PlaylistHelper.LITERAL_TRANSLATION_PREFIX.length())));
        }
    }
}
