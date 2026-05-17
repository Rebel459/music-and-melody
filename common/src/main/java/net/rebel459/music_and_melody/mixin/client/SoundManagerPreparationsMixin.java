package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rebel459.music_and_melody.MusicAndMelody;
import net.rebel459.music_and_melody.client.Album;
import net.rebel459.music_and_melody.client.CommonMusicHelper;
import net.rebel459.music_and_melody.config.MaMConfig;
import org.spongepowered.asm.mixin.Final;
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
    @Final
    private Map<Identifier, WeighedSoundEvents> registry;

    @Unique
    private static final Set<Identifier> DISABLED_EVENTS = new HashSet<>();

    @Unique
    private static final Map<Identifier, Identifier> REMAPPED_MUSIC = Map.of();

    @Inject(method = "listResources", at = @At("HEAD"))
    private void clearRawSoundPools(ResourceManager resourceManager, CallbackInfo ci) {
        DISABLED_EVENTS.clear();
        CommonMusicHelper.clearSoundPools();
    }

    @Inject(method = "handleRegistration", at = @At("HEAD"), cancellable = true)
    private void storeRawSoundPool(Identifier eventLocation, SoundEventRegistration soundEventRegistration, CallbackInfo ci) {
        remapEntries(soundEventRegistration.getSounds());

        soundEventRegistration.getSounds().removeIf(SoundManagerPreparationsMixin::isDisabled);
        if (soundEventRegistration.getSounds().isEmpty()) {
            DISABLED_EVENTS.add(eventLocation);
            ci.cancel();
            return;
        }
        CommonMusicHelper.addSoundPool(eventLocation, soundEventRegistration.getSounds(), soundEventRegistration.isReplace());
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void storeFilteredMusicPools(Map<Identifier, WeighedSoundEvents> registry, Map<Identifier, Resource> soundCache, SoundEngine engine, CallbackInfo ci) {
        CommonMusicHelper.FILTERED_POOLS.clear();
        if (!MaMConfig.get().client.common_music) return;
        registry = this.registry;
        WeighedSoundEvents common = registry.get(CommonMusicHelper.BASE_POOL);
        if (common == null) return;

        Set<CommonMusicHelper.SoundKey> commonSounds = CommonMusicHelper.getCommonSounds(CommonMusicHelper.BASE_POOL);

        if (commonSounds.isEmpty()) return;

        registry.forEach((poolId, pool) -> {
            pool.list.removeIf(weighted -> weighted instanceof Sound sound && sound.getPath().getNamespace().equals(MusicAndMelody.MOD_ID));
            if (poolId.equals(CommonMusicHelper.BASE_POOL)) return;
            if (CommonMusicHelper.containsCommonSound(poolId, commonSounds)) {
                CommonMusicHelper.FILTERED_POOLS.add(poolId);
                return;
            }
            pool.list.forEach(weighted -> {
                List<Weighted<Sound>> sounds = List.of(weighted);
                if (weighted instanceof WeighedSoundEvents weighedSounds) {
                    if (CommonMusicHelper.filterWeighedSounds(weighedSounds, commonSounds)) CommonMusicHelper.FILTERED_POOLS.add(poolId);
                }
                for (Weighted<Sound> sound : sounds) {
                    if (sound instanceof Sound && commonSounds.contains(CommonMusicHelper.SoundKey.of((Sound) sound))) CommonMusicHelper.FILTERED_POOLS.add(poolId);
                }
            });
        });
    }

    @Unique
    private static boolean isDisabled(Sound sound) {
        Identifier soundLocation = sound.getLocation();
        if (sound.getType() == Sound.Type.FILE) {
            boolean disabled = false;
            for (Album album : Album.ALBUMS) {
                if (!album.enabled) {
                    if (album.namespace.isPresent()) {
                        disabled = disabled || soundLocation.getNamespace().equals(album.namespace.get());
                    }
                    if (album.songs.isPresent()) {
                        disabled = disabled || album.songs.get().contains(soundLocation);
                    }
                }
            }
            return disabled;
        }
        return sound.getType() == Sound.Type.SOUND_EVENT && DISABLED_EVENTS.contains(soundLocation);
    }

    @Unique
    private static void remapEntries(List<Sound> sounds) {
        ListIterator<Sound> iterator = sounds.listIterator();
        while (iterator.hasNext()) {
            Sound sound = iterator.next();
            Identifier location = REMAPPED_MUSIC.get(sound.getLocation());
            if (location != null) iterator.set(copy(sound, location));
        }
    }

    @Unique
    private static Sound copy(Sound sound, Identifier location) {
        return new Sound(location, sound.getVolume(), sound.getPitch(), sound.getWeight(), sound.getType(), sound.shouldStream(), sound.shouldPreload(), sound.getAttenuationDistance());
    }
}
