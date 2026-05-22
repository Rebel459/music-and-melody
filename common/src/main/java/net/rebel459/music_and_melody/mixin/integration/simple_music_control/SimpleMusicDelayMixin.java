package net.rebel459.music_and_melody.mixin.integration.simple_music_control;

import com.mojang.datafixers.util.Pair;
import net.rebel459.music_and_melody.client.util.EventHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EventHelper.class)
public abstract class SimpleMusicDelayMixin {

    @Inject(method = "getMusicFrequency", at = @At(value = "HEAD"), cancellable = true)
    private static void useSimpleMusicControlFrequency(CallbackInfoReturnable<Pair<Integer, Integer>> cir) {
        Class<?> config = simpleMusicControlConfig();
        if (config == null || !getBoolean(config, "modifyMusicDelays")) return;
        cir.setReturnValue(Pair.of(
                getInt(config, "musicMinDelay"),
                getInt(config, "musicMaxDelay")
        ));
    }

    @Unique
    private static Class<?> simpleMusicControlConfig() {
        try {
            return Class.forName("me.pajic.simple_music_control.config.ModClientConfig");
        } catch (ClassNotFoundException ignored) {
            try {
                return Class.forName("me.pajic.simple_music_control.config.ModConfig");
            } catch (ClassNotFoundException ignoredAgain) {
                return null;
            }
        }
    }

    @Unique
    private static boolean getBoolean(Class<?> config, String field) {
        try {
            return config.getField(field).getBoolean(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @Unique
    private static int getInt(Class<?> config, String field) {
        try {
            return config.getField(field).getInt(null);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }
}