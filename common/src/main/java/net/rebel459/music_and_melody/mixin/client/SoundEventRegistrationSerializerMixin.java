package net.rebel459.music_and_melody.mixin.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.rebel459.music_and_melody.client.util.EventWeightHelper;
import net.rebel459.music_and_melody.client.util.VanillaSoundSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(SoundEventRegistrationSerializer.class)
public class SoundEventRegistrationSerializerMixin {

    @Inject(method = "deserialize*", at = @At("HEAD"))
    private void prepareSafeTracks(JsonElement json, Type typeOfT, JsonDeserializationContext context, CallbackInfoReturnable<SoundEventRegistration> cir) {
        VanillaSoundSupport.prepare(json);
    }

    @Inject(method = "getSound", at = @At("RETURN"))
    private void storeWeight(JsonObject object, CallbackInfoReturnable<Sound> cir) {
        Sound sound = cir.getReturnValue();
        if (object.has("weight") && sound.getType() == Sound.Type.SOUND_EVENT) {
            EventWeightHelper.add(sound);
        }
    }
}
