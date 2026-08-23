package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.rebel459.music_and_melody.network.CombatMusicHandler;
import net.rebel459.music_and_melody.network.ServerPresenceHandler;
import net.rebel459.unified.api.client.core.UnifiedClientEvents;
import net.rebel459.unified.api.event.EventTiming;

public class CombatStatus {
    private static int combatScore = 0;
    private static int combatSeconds = 0;

    private static int lastHurtByTimestamp;
    private static int lastHurtTimestamp;

    private static int ticks = 0;
    private static boolean reset = false;

    public static void increaseFromAttack() {
        increase(2);
        combatScore = Math.max(10, combatScore);
    }
    public static void increaseOverTime(boolean fullHealth) {
        increase(fullHealth ? 1 : 2);
        combatSeconds++;
    }
    public static void increase(int amount) {
        combatScore += amount;
        combatScore = Math.min(combatScore, 100);
    }

    public static void decreaseOverTime(boolean fullHealth) {
        decrease(fullHealth ? 8 : 4);
    }
    public static void decrease(int amount) {
        combatScore -= amount;
        combatScore = Math.max(combatScore, 0);
        if (combatScore == 0) combatSeconds = 0;
    }

    public static boolean inCombat(int combatScore) {
        return CombatStatus.combatScore >= combatScore && combatSeconds >= 5;
    }

    public static void init() {
        UnifiedClientEvents.Instance.onTick(EventTiming.PRE, client -> {
            LocalPlayer player = client.player;
            if (player == null) {
                if (!reset) {
                    combatScore = 0;
                    combatSeconds = 0;
                    lastHurtByTimestamp = 0;
                    lastHurtTimestamp = 0;
                    ticks = 0;
                    CombatMusicHandler.clientPlayerTrackedByMob = false;
                    reset = true;
                }
                return;
            }
            if (reset) reset = false;

            onTick(player);

            ticks++;
            if (ticks >= 20) {
                ticks = 0;
                onSecond(player);
            }
        });
    }

    private static void onTick(LocalPlayer player) {
        int hurtByTimestamp = player.getLastHurtByMobTimestamp();
        if (hurtByTimestamp != lastHurtByTimestamp) {
            lastHurtByTimestamp = hurtByTimestamp;

            if (player.getLastHurtByMob() instanceof Enemy) {
                increaseFromAttack();
            }
        }

        int hurtTimestamp = player.getLastHurtMobTimestamp();
        if (hurtTimestamp != lastHurtTimestamp) {
            lastHurtTimestamp = hurtTimestamp;

            if (player.getLastHurtMob() instanceof Enemy) {
                increaseFromAttack();
            }
        }
    }

    private static void onSecond(LocalPlayer player) {
        boolean playerTrackedByMob;
        if (ServerPresenceHandler.combatDetection) playerTrackedByMob = CombatMusicHandler.clientPlayerTrackedByMob;
        else playerTrackedByMob = !player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(16),
                mob -> mob instanceof Enemy && mob.isAlive() && mob.hasLineOfSight(player) && mob.distanceToSqr(player) <= 16 * 16
        ).isEmpty();

        boolean atFullHealth = player.getHealth() >= player.getMaxHealth();
        if (playerTrackedByMob) {
            increaseOverTime(atFullHealth);
        } else {
            decreaseOverTime(atFullHealth);
        }
    }
}