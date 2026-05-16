package net.rebel459.legacies_and_legends.mixin.client;

import net.minecraft.client.color.block.BlockColors;
import net.rebel459.legacies_and_legends.block.WandPlatformTintSource;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockColors.class)
public class BlockColorsMixin {

    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void addWandPlatformTint(CallbackInfoReturnable<BlockColors> cir) {
        cir.getReturnValue().register(List.of(new WandPlatformTintSource()), LaLBlocks.WAND_PLATFORM.get());
    }
}
