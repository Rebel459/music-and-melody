package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.rebel459.music_and_melody.client.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class SoundManagerPreparationsMixin {

    @Shadow
    private Map<Identifier, Resource> soundCache;

    @Shadow
    private void handleRegistration(Identifier eventLocation, SoundEventRegistration soundEventRegistration) {
        throw new AssertionError();
    }

    @Unique
    private ResourceManager resourceManager;

    @Unique
    private static final Map<Identifier, Identifier> REMAPPED_MUSIC = Map.of();

    @Inject(method = "listResources", at = @At("RETURN"))
    private void addConfigAlbumSounds(ResourceManager resourceManager, CallbackInfo ci) {
        this.resourceManager = resourceManager;
        PlaylistHelper.STORED_VOLUME.clear();
        CustomAlbums.addSoundResources(this.soundCache);
    }

    @Inject(method = "handleRegistration", at = @At("HEAD"))
    private void storeRawSoundPool(Identifier eventLocation, SoundEventRegistration soundEventRegistration, CallbackInfo ci) {
        processRaw(soundEventRegistration.getSounds());
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void addConfigSounds(Map<Identifier, WeighedSoundEvents> registry,
                                 Map<Identifier, Resource> soundCache,
                                 SoundEngine soundEngine,
                                 CallbackInfo ci) {
        CustomSounds.load().forEach((path, registration) -> {
            Identifier event = Identifier.tryBuild("config", path);
            if (event != null) this.handleRegistration(event, registration);
        });
    }

    @Unique
    private void processRaw(List<Sound> sounds) {
        ListIterator<Sound> iterator = sounds.listIterator();
        while (iterator.hasNext()) {
            Sound sound = iterator.next();
            Identifier id = sound.getLocation();
            Optional<SafeIdentifier> mappedSource = VanillaSoundSupport.source(id);
            SafeIdentifier source = mappedSource.orElseGet(() -> SafeIdentifier.convert(id));
            if (!(sound.getVolume() instanceof ConstantFloat(float volume)) || Float.compare(volume, 1.0F) != 0) PlaylistHelper.STORED_VOLUME.put(source, sound.getVolume());
            if (mappedSource.isPresent() && sound.getType() == Sound.Type.FILE && this.resourceManager != null) {
                Identifier fileId = sound.getPath();
                CustomAlbums.file(source).or(() -> SafeMusicHelper.resolve(source)).ifPresentOrElse(path -> {
                    DirectSoundFiles.register(fileId, id, source, source, path);
                    this.soundCache.put(fileId, new Resource(null, IoSupplier.create(path)));
                }, () -> SafeMusicHelper.resolveResource(source, this.resourceManager).ifPresent(resource -> {
                    DirectSoundFiles.registerResource(fileId, id, source, source, resource.stream(), resource.extension());
                    this.soundCache.put(fileId, new Resource(null, resource.stream()));
                }));
            }
            Identifier location = REMAPPED_MUSIC.get(id);
            if (location != null) iterator.set(copy(sound, location));
        }
    }

    @Unique
    private static Sound copy(Sound sound, Identifier location) {
        return new Sound(location, sound.getVolume(), sound.getPitch(), sound.getWeight(), sound.getType(), sound.shouldStream(), sound.shouldPreload(), sound.getAttenuationDistance());
    }
}
