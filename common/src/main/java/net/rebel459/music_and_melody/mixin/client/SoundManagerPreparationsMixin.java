package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.ConfigAlbum;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class SoundManagerPreparationsMixin {

    @Shadow
    private Map<ResourceLocation, Resource> soundCache;

    @Unique
    private static final Set<ResourceLocation> DISABLED_EVENTS = new HashSet<>();

    @Unique
    private static final Map<ResourceLocation, ResourceLocation> REMAPPED_MUSIC = Map.of();

    @Inject(method = "listResources", at = @At("HEAD"))
    private void clearRawSoundPools(ResourceManager resourceManager, CallbackInfo ci) {
        DISABLED_EVENTS.clear();
    }

    @Inject(method = "listResources", at = @At("RETURN"))
    private void addConfigAlbumSounds(ResourceManager resourceManager, CallbackInfo ci) {
        ConfigAlbum.addSoundResources(this.soundCache);
    }

    @Inject(method = "handleRegistration", at = @At("HEAD"), cancellable = true)
    private void storeRawSoundPool(ResourceLocation eventLocation, SoundEventRegistration soundEventRegistration, CallbackInfo ci) {
        processRaw(soundEventRegistration.getSounds());

        soundEventRegistration.getSounds().removeIf(SoundManagerPreparationsMixin::isDisabled);
        if (soundEventRegistration.getSounds().isEmpty()) {
            DISABLED_EVENTS.add(eventLocation);
            ci.cancel();
        }
    }

    @Unique
    private static boolean isDisabled(Sound sound) {
        ResourceLocation soundLocation = sound.getLocation();
        if (sound.getType() == Sound.Type.FILE) {
            boolean disabled = false;
            for (Album album : Album.ALBUMS) {
                if (!album.isEnabled()) {
                    disabled = disabled || (soundLocation.getNamespace().equals(album.album.getNamespace()) && album.tracks.contains(soundLocation.getPath()) && !album.isTrackForcedEnabled(soundLocation.getPath()));
                }
                if (soundLocation.getNamespace().equals(album.album.getNamespace()) && album.tracks.contains(soundLocation.getPath()) && !album.isTrackEnabled(soundLocation.getPath())) {
                    disabled = true;
                }
            }
            return disabled;
        }
        return sound.getType() == Sound.Type.SOUND_EVENT && DISABLED_EVENTS.contains(soundLocation);
    }

    @Unique
    private static void processRaw(List<Sound> sounds) {
        ListIterator<Sound> iterator = sounds.listIterator();
        while (iterator.hasNext()) {
            Sound sound = iterator.next();
            ResourceLocation id = sound.getLocation();
            PlaylistHelper.STORED_VOLUME.put(SafeIdentifier.convert(id), sound.getVolume());
            ResourceLocation location = REMAPPED_MUSIC.get(id);
            if (location != null) iterator.set(copy(sound, location));
        }
    }

    @Unique
    private static Sound copy(Sound sound, ResourceLocation location) {
        return new Sound(location, sound.getVolume(), sound.getPitch(), sound.getWeight(), sound.getType(), sound.shouldStream(), sound.shouldPreload(), sound.getAttenuationDistance());
    }
}
