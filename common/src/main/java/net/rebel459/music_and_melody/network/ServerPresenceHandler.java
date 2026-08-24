package net.rebel459.music_and_melody.network;

import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedHelpers;

public final class ServerPresenceHandler {

    public static boolean discUnlocking = false;
    public static boolean improvedPveDetection = false;

    private ServerPresenceHandler() {}

    public static void init() {
        UnifiedHelpers.NETWORKING.registerPlayToClient(ServerPresencePacket.TYPE, ServerPresencePacket.CODEC, (packet, player) -> {
            discUnlocking = packet.discUnlocking();
        });
        UnifiedEvents.Players.onJoin(player -> UnifiedHelpers.NETWORKING.send(new ServerPresencePacket(MaMServerConfig.get().disc_unlocking, MaMServerConfig.get().improved_pve_detection), player));
    }
}
