package net.rebel459.music_and_melody.mixin.client;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.rebel459.music_and_melody.client.util.EventWeightHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEventRegistrationSerializer.class)
public class SoundEventRegistrationSerializerMixin {

    @Inject(method = "getSound", at = @At("RETURN"))
    private void storeWeight(JsonObject object, CallbackInfoReturnable<Sound> cir) {
        Sound sound = cir.getReturnValue();
        if (object.has("weight") && sound.getType() == Sound.Type.SOUND_EVENT) {
            EventWeightHelper.add(sound);
        }
    }
}
