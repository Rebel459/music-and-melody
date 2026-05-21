package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.resources.sounds.Sound;
import net.rebel459.music_and_melody.client.util.EventWeightHelper;
import net.rebel459.music_and_melody.config.MaMClientConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations$1", remap = false)
public class SoundManagerPreparationsWeightMixin {

    @Shadow(remap = false)
    @Final
    private Sound val$sound;

    @Inject(method = "getWeight()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void enforceWeight(CallbackInfoReturnable<Integer> cir) {
        if (!MaMClientConfig.get().pool_weight_fix) return;
        if (EventWeightHelper.contains(this.val$sound)) {
            cir.setReturnValue(this.val$sound.getWeight());
        }
    }
}
