package net.rebel459.legacies_and_legends.mixin.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rebel459.legacies_and_legends.client.CommonMusicHelper;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class SoundManagerPreparationsMixin {

    @Shadow
    @Final
    private Map<Identifier, WeighedSoundEvents> registry;

    @Inject(method = "listResources", at = @At("HEAD"))
    private void clearRawSoundPools(ResourceManager resourceManager, CallbackInfo ci) {
        CommonMusicHelper.clearSoundPools();
    }

    @Inject(method = "handleRegistration", at = @At("HEAD"))
    private void storeRawSoundPool(Identifier eventLocation, SoundEventRegistration soundEventRegistration, CallbackInfo ci) {
        CommonMusicHelper.addSoundPool(eventLocation, soundEventRegistration.getSounds(), soundEventRegistration.isReplace());
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void storeFilteredMusicPools(Map<Identifier, WeighedSoundEvents> registry, Map<Identifier, Resource> soundCache, SoundEngine engine, CallbackInfo ci) {
        CommonMusicHelper.FILTERED_POOLS.clear();
        if (!LaLConfig.get().music.common_music) return;
        registry = this.registry;
        WeighedSoundEvents common = registry.get(CommonMusicHelper.BASE_POOL);
        if (common == null) return;

        Set<CommonMusicHelper.SoundKey> commonSounds = CommonMusicHelper.getCommonSounds(CommonMusicHelper.BASE_POOL);

        if (commonSounds.isEmpty()) return;

        registry.forEach((poolId, pool) -> {
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
}
