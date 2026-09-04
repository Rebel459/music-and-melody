package net.rebel459.music_and_melody.mixin.integration.simple_music_control;

import com.mojang.datafixers.util.Pair;
import net.rebel459.music_and_melody.client.util.EventHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(EventHelper.class)
public abstract class SimpleMusicDelayMixin {

    @Inject(method = "getMusicFrequency", at = @At(value = "HEAD"), cancellable = true)
    private static void useSimpleMusicControlFrequency(CallbackInfoReturnable<Pair<Integer, Integer>> cir) {
        Object config = getObject("me.pajic.simple_music_control.SMC", "CONFIG");
        if (config != null) {
            Integer min = getInteger(config, "musicMinDelay");
            Integer max = getInteger(config, "musicMaxDelay");
            if (min != null && max != null) cir.setReturnValue(Pair.of(min, max));
        }
    }

    @Unique
    private static Object getObject(String className, String fieldName) {
        try {
            return Class.forName(className).getField(fieldName).get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            return null;
        }
    }

    @Unique
    private static Integer getInteger(Object owner, String fieldName) {
        try {
            Field field = owner.getClass().getField(fieldName);
            Object value = field.get(owner);
            if (value instanceof Number number) return number.intValue();

            Method get = value.getClass().getMethod("get");
            return ((Number) get.invoke(value)).intValue();
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }
}
