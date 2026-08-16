package net.rebel459.music_and_melody.mixin.client;

import com.google.common.collect.Multimap;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import net.rebel459.music_and_melody.client.util.SoundEngineStopper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundEngineStopper {

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow
    @Final
    public Multimap<SoundSource, SoundInstance> instanceBySource;

    @Shadow
    @Final
    private List<TickableSoundInstance> tickingSounds;

    @Shadow
    @Final
    private Map<SoundInstance, Integer> queuedSounds;

    @Shadow
    @Final
    private Map<SoundInstance, Integer> soundDeleteTime;

    @Shadow
    @Final
    private List<TickableSoundInstance> queuedTickableSounds;

    @Shadow
    public abstract void stop(SoundInstance soundInstance);

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void interruptStoppedPlaylistSound(SoundInstance soundInstance, CallbackInfo ci) {
        PlaylistHelper.interruptCurrentPlayback(soundInstance);
    }

    @Inject(method = "reload", at = @At("HEAD"))
    private void beginPlaylistSoundReload(CallbackInfo ci) {
        PlaylistHelper.beginSoundEngineReload();
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void finishPlaylistSoundReload(CallbackInfo ci) {
        PlaylistHelper.finishSoundEngineReload();
    }

    @Inject(
            method = {
                    "destroy",
                    "emergencyShutdown",
                    "stopAll"
            },
            at = @At("HEAD")
    )
    private void interruptPlaylistSound(CallbackInfo ci) {
        PlaylistHelper.interruptCurrentPlayback(null);
    }

    @Override
    public boolean stopEverythingExceptPlaylist(SoundInstance preserved) {
        if (preserved == null || !this.instanceToChannel.containsKey(preserved)) return false;
        Set<SoundInstance> instances = new HashSet<>(this.instanceBySource.values());
        instances.addAll(this.instanceToChannel.keySet());
        for (SoundInstance instance : new ArrayList<>(instances)) {
            if (instance == preserved) continue;
            this.stop(instance);
            this.instanceToChannel.remove(instance);
            this.instanceBySource.remove(instance.getSource(), instance);
            this.soundDeleteTime.remove(instance);
            if (instance instanceof TickableSoundInstance tickableSound) {
                this.tickingSounds.remove(tickableSound);
            }
        }
        this.queuedSounds.clear();
        this.queuedTickableSounds.clear();
        return true;
    }

    @Override
    public boolean pausePlaylist(SoundInstance sound) {
        ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(sound);
        if (handle == null || handle.isStopped()) return false;
        handle.execute(channel -> channel.pause());
        return true;
    }

    @Override
    public boolean resumePlaylist(SoundInstance sound) {
        ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(sound);
        if (handle == null || handle.isStopped()) return false;
        handle.execute(channel -> channel.unpause());
        return true;
    }
}
