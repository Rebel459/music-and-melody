package net.rebel459.legacies_and_legends.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import net.rebel459.legacies_and_legends.registry.LaLMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/Collection;IIIII)V"))
    private void hideEffects(EffectsInInventory effectsInInventory, GuiGraphicsExtractor graphics, Collection<MobEffectInstance> activeEffects, int x0, int yStep, int mouseX, int mouseY, int maxWidth, Operation<Void> original) {
        activeEffects.removeIf(effect -> (effect.is(LaLMobEffects.LOW_GRAVITY) || effect.is(LaLMobEffects.PROJECTILE_PASSTHROUGH)) && !effect.showIcon() && !effect.isVisible());
        original.call(effectsInInventory, graphics, activeEffects, x0, yStep, mouseX, mouseY, maxWidth);
    }
}