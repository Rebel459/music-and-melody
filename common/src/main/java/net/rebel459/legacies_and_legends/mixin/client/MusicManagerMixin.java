package net.rebel459.legacies_and_legends.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.legacies_and_legends.client.CommonMusicHelper;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MusicManager.class)
public class MusicManagerMixin {

    @ModifyVariable(method = "startPlaying", at = @At("HEAD"), argsOnly = true)
    private Music selectCommonMusic(Music music) {
        var config = LaLConfig.get().music;
        Identifier musicId = music.sound().value().location();
        if (config.common_music && CommonMusicHelper.FILTERED_POOLS.contains(musicId) && SoundInstance.createUnseededRandom().nextIntBetweenInclusive(1, 100) <= config.common_music_chance) {
            return new Music(LaLSounds.COMMON_MUSIC.holder(), music.minDelay(), music.maxDelay(), music.replaceCurrentMusic());
        }
        else return music;
    }

    @ModifyExpressionValue(
            method = "startPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forMusic(Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"
            )
    )
    private SimpleSoundInstance filterOnlyBackgroundMusic(SimpleSoundInstance original, @Local(name = "soundEvent") SoundEvent soundEvent) {
        if (LaLConfig.get().music.common_music) return new CommonMusicHelper.Instance(soundEvent);
        else return original;
    }
}