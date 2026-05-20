package net.rebel459.music_and_melody.mixin.integration.simple_music_control;

import com.mojang.datafixers.util.Pair;
import me.pajic.simple_music_control.SMC;
import net.rebel459.music_and_melody.client.EventHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EventHelper.class)
public abstract class SimpleMusicDelayMixin {

    @Inject(method = "getMusicFrequency", at = @At(value = "HEAD"), cancellable = true)
    private static void useSimpleMusicControlFrequency(CallbackInfoReturnable<Pair<Integer, Integer>> cir) {
        if (!SMC.CONFIG.modifyMusicDelays.get()) return;
        cir.setReturnValue(Pair.of(SMC.CONFIG.musicMinDelay.get(), SMC.CONFIG.musicMaxDelay.get()));
    }
}