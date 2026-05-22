package net.rebel459.music_and_melody.mixin.integration.enderscape;

import net.bunten.enderscape.client.EnderscapeClient;
import net.bunten.enderscape.client.registry.EnderscapeClientNetworking;
import net.bunten.enderscape.network.ClientboundStructureChangedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(EnderscapeClientNetworking.class)
public class EnderscapeStructureMusicMixin {

    @Inject(method = "receiveStructureChangedPayload", at = @At(value = "HEAD"), cancellable = true)
    private static void disableEnderscapeStructureMusic(ClientboundStructureChangedPayload payload, ClientPlayNetworking.Context context, CallbackInfo ci) {
        EnderscapeClient.structureMusic = Optional.empty();
        ci.cancel();
    }
}
