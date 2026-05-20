package net.rebel459.music_and_melody.network;

import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedHelpers;

public final class ServerPresenceHandler {

    private ServerPresenceHandler() {}

    public static void init() {
        UnifiedHelpers.NETWORKING.registerPlayToClient(ServerPresencePacket.TYPE, ServerPresencePacket.CODEC, (packet, player) -> {
            ServerHelper.countDiscUses = MaMServerConfig.get().count_disc_uses;
            ServerHelper.markPresent();
        });
        UnifiedEvents.Players.onJoin(player -> UnifiedHelpers.NETWORKING.send(new ServerPresencePacket(), player));
    }
}
