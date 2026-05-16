package net.rebel459.legacies_and_legends.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.level.Level;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow private int rightClickDelay;

    @Shadow @Nullable public LocalPlayer player;

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;"))
    private void ringOfConstruction(CallbackInfo ci) {
        if (AccessoryHelper.getAccessory(this.player).is(LaLItems.RING_OF_CONSTRUCTION.get())) this.rightClickDelay = 3;
    }

    @WrapOperation(method = "getSituationalMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BackgroundMusic;select(ZZ)Ljava/util/Optional;"))
    private Optional<Music> creativeMusicFix(BackgroundMusic music, boolean isCreative, boolean isUnderwater, Operation<Optional<Music>> original) {
        if (LaLConfig.get().music.creative_fix && this.player.level.dimension() == Level.OVERWORLD && music.creativeMusic().isEmpty()) music = new BackgroundMusic(music.defaultMusic(), Optional.of(Musics.CREATIVE), music.underwaterMusic());
        return original.call(music, isCreative, isUnderwater);
    }
}
