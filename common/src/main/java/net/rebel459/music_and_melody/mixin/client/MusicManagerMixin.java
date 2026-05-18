package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.music_and_melody.client.CommonMusicHelper;
import net.rebel459.music_and_melody.client.EventMusicHelper;
import net.rebel459.music_and_melody.client.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMConfig;
import net.rebel459.music_and_melody.sound.MaMSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow
    public abstract boolean isPlayingMusic(Music music);

    @Inject(method = "getCurrentMusicTranslationKey", at = @At("HEAD"), cancellable = true)
    private void playlistMusicTranslationKey(CallbackInfoReturnable<String> cir) {
        String key = PlaylistHelper.getCurrentMusicTranslationKey();
        if (key != null) cir.setReturnValue(key);
    }

    @ModifyVariable(method = "startPlaying", at = @At("HEAD"), argsOnly = true)
    private Music selectCommonMusic(Music music) {
        var config = MaMConfig.get().client;
        Identifier musicId = music.sound().value().location();
        if (config.common_music && CommonMusicHelper.FILTERED_POOLS.contains(musicId) && SoundInstance.createUnseededRandom().nextIntBetweenInclusive(1, 100) <= config.common_music_chance) {
            return new Music(MaMSounds.MUSIC_COMMON.holder(), music.minDelay(), music.maxDelay(), music.replaceCurrentMusic());
        }
        else return music;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMusicVolume()F"
            )
    )
    private float fadeWitherMusic(float volume) {
        if (this.isPlayingMusic(EventMusicHelper.WITHER_BOSS) && !EventMusicHelper.hasWitherBossBar()) return 0F;
        return volume;
    }

    @ModifyExpressionValue(
            method = "startPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forMusic(Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"
            )
    )
    private SimpleSoundInstance filterOnlyBackgroundMusic(SimpleSoundInstance original, @Local(name = "soundEvent") SoundEvent soundEvent) {
        if (MaMConfig.get().client.common_music) return new CommonMusicHelper.Instance(soundEvent);
        else return original;
    }
}
