package net.rebel459.music_and_melody.network;

import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;
import net.rebel459.music_and_melody.platform.event.PlayerEvents;

public final class ServerPresenceHandler {

    private ServerPresenceHandler() {}

    public static void init() {
        MaMPlatform.NETWORKING.registerPlayToClient(ServerPresencePacket.TYPE, ServerPresencePacket.CODEC, (packet, player) -> {
            ServerHelper.countDiscUses = MaMServerConfig.get().count_disc_uses;
            ServerHelper.markPresent();
        });
        PlayerEvents.onJoin(player -> MaMPlatform.NETWORKING.send(new ServerPresencePacket(), player));
    }
}
