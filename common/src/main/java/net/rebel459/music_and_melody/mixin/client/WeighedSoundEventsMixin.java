package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WeighedSoundEvents.class)
public abstract class WeighedSoundEventsMixin {

    @Mutable
    @Shadow
    @Final
    public List<Weighted<Sound>> list;

    @Inject(method = "getSound(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/resources/sounds/Sound;", at = @At("HEAD"))
    private void removeDisabledSounds(RandomSource random, CallbackInfoReturnable<Sound> cir) {
        this.list.removeIf(weighted -> weighted instanceof Sound sound && isDisabled(sound));
    }

    @Unique
    private static boolean isDisabled(Sound sound) {
        Identifier soundLocation = sound.getLocation();
        if (sound.getType() == Sound.Type.FILE) {
            return !Album.isSoundEnabled(SafeIdentifier.convert(soundLocation));
        }
        return false;
    }
}
