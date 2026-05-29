package net.rebel459.music_and_melody.network;

import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.music_and_melody.platform.MaMPlatform;
import net.rebel459.music_and_melody.platform.event.PlayerEvents;

public final class ServerPresenceHandler {

    public static boolean discUnlocking = false;

    private ServerPresenceHandler() {}

    public static void init() {
        MaMPlatform.NETWORKING.registerPlayToClient(ServerPresencePacket.TYPE, ServerPresencePacket.CODEC, (packet, player) -> {
            discUnlocking = packet.discUnlocking();
        });
        PlayerEvents.onJoin(player -> MaMPlatform.NETWORKING.send(new ServerPresencePacket(MaMServerConfig.get().disc_unlocking), player));
    }
}
