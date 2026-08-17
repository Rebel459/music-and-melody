package net.rebel459.music_and_melody.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.rebel459.music_and_melody.client.screen.MusicPlayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleAwardStats", at = @At("TAIL"))
    private void refreshAlbumDetailsStats(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
        ClientPacketListener listener = ClientPacketListener.class.cast(this);
        if (listener.minecraft.gui.screen() instanceof MusicPlayerScreen screen) {
            screen.onStatsUpdated();
        }
    }
}
