package net.rebel459.music_and_melody.network;

import net.rebel459.music_and_melody.config.MaMConfig;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedHelpers;

public final class MaMNetwork {

    private MaMNetwork() {}

    public static void init() {
        UnifiedHelpers.NETWORKING.registerPlayToClient(ServerPresencePacket.TYPE, ServerPresencePacket.CODEC, (packet, player) -> {
            ServerHelper.countDiscUses = MaMConfig.get().server.count_disc_uses;
            ServerHelper.markPresent();
        });
        UnifiedEvents.Players.onJoin(player -> UnifiedHelpers.NETWORKING.send(new ServerPresencePacket(), player));
    }
}
