package net.rebel459.legacies_and_legends.mixin.block.entity;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.rebel459.legacies_and_legends.registry.LaLDecoratedPotPatterns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotPatterns.class)
public class DecoratedPotPatternsMixin {

    @Inject(method = "bootstrap", at = @At("HEAD"))
    private static void lal$registerCustomPatterns(Registry<DecoratedPotPattern> registry, CallbackInfoReturnable<DecoratedPotPattern> cir) {
        LaLDecoratedPotPatterns.bootstrap(registry);
    }

    @Inject(method = "getPatternFromItem", at = @At("RETURN"), cancellable = true)
    private static void lal$useCustomPatterns(Item item, CallbackInfoReturnable<ResourceKey<DecoratedPotPattern>> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }

        ResourceKey<DecoratedPotPattern> pattern = LaLDecoratedPotPatterns.fromItem(item);
        if (pattern != null) {
            cir.setReturnValue(pattern);
        }
    }
}
