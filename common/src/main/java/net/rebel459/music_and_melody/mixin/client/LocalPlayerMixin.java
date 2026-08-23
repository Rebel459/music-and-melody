package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.rebel459.music_and_melody.client.remote.DownloadedResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void invalidateDownloadedResources(CallbackInfo ci) {
        DownloadedResources.invalidate();
    }

}
