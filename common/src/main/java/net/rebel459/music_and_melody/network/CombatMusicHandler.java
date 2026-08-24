package net.rebel459.music_and_melody.network;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.rebel459.unified.util.EventType;

import java.util.*;

public class CombatMusicHandler {

    private static int serverTicks = 0;
    private static final Map<UUID, Boolean> LAST_STATE = new HashMap<>();

    public static void init() {
        if (!MaMServerConfig.get().improved_pve_detection) return;
        UnifiedHelpers.NETWORKING.registerPlayToClient(CombatMusicPacket.TYPE, CombatMusicPacket.CODEC, (packet, player) -> {
            clientPlayerTrackedByMob = packet.playerTrackedByMob();
        });
        UnifiedEvents.Players.onLeave(player -> LAST_STATE.remove(player.getUUID()));
        UnifiedEvents.Server.onTick(EventType.PRE, server -> {
            if (++serverTicks < 20) return;
            serverTicks = 0;
            server.getPlayerList().getPlayers().forEach(player -> {
                boolean playerTrackedByMob = !player.level().getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(32),
                        mob -> mob instanceof Enemy && mob.isAlive() && mob.getTarget() == player && mob.distanceToSqr(player) <= 32 * 32
                ).isEmpty();
                Boolean lastState = LAST_STATE.get(player.getUUID());
                if (lastState == null || lastState != playerTrackedByMob) {
                    LAST_STATE.put(player.getUUID(), playerTrackedByMob);
                    UnifiedHelpers.NETWORKING.send(new CombatMusicPacket(playerTrackedByMob), player);
                }
            });
        });
    }

    public static boolean clientPlayerTrackedByMob;
}
