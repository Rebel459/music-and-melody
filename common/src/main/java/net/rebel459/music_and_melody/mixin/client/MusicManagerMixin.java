package net.rebel459.music_and_melody.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.rebel459.music_and_melody.client.util.EventHelper;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow
    public abstract void stopPlaying();

    @Shadow
    @Nullable
    private SoundInstance currentMusic;

    @Shadow
    private int nextSongDelay;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getSituationalMusic()Lnet/minecraft/sounds/Music;"
            )
    )
    private Music afterSituationalMusic(Music music) {
        clearEmptyMusic();
        return music;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickClearEmptyMusic(CallbackInfo ci) {
        clearEmptyMusic();
    }

    @Unique
    private void clearEmptyMusic() {
        if (!PlaylistHelper.isEmptyMusic(this.currentMusic) || EventHelper.isCooldownEmptyMusic() || PlaylistHelper.isPlaying() || !MaMClientConfig.get().vanilla_music) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(this.currentMusic);
        this.currentMusic = null;
        this.nextSongDelay = Math.max(this.nextSongDelay, EventHelper.randomMusicBreak());
    }
}
