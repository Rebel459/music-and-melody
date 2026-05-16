package net.rebel459.legacies_and_legends.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {

    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 1))
    private boolean modifyNightVisionUniform(LocalPlayer player, Holder holder, Operation<Boolean> original) {
        if (player == null || holder == null) return original.call(player, holder);
        boolean hasWaterVision = original.call(player, holder);
        if (LaLConfig.get().misc.improved_turtle_shell) hasWaterVision = hasWaterVision || player.isEquipped(Items.TURTLE_HELMET);
        return hasWaterVision;
    }
}