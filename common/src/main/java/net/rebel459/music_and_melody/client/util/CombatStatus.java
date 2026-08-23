package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.rebel459.music_and_melody.network.CombatMusicHandler;
import net.rebel459.music_and_melody.network.ServerPresenceHandler;
import net.rebel459.unified.api.client.core.UnifiedClientEvents;
import net.rebel459.unified.api.event.EventTiming;

public class CombatStatus {
    private static int combatScore = 0;
    private static int pveCombatScore = 0;
    private static int pvpCombatScore = 0;

    private static int lastHurtByTimestamp;
    private static int lastHurtTimestamp;
    private static int lastFightingPlayerTimestamp = 1200;
    private static int lastPlayerAttackTimestamp = 1200;

    private static int ticks = 0;
    private static boolean reset = true;

    private static void updateCombatScore() {
        combatScore = Math.max(pveCombatScore, pvpCombatScore);
    }

    public static boolean inCombat(int combatScore) {
        return CombatStatus.combatScore >= combatScore;
    }

    public static class PvE {
        public static void increaseFromAttack() {
            increase(2);
            pveCombatScore = Math.max(10, pveCombatScore);
            updateCombatScore();
        }

        public static void increaseOverTime(boolean fullHealth) {
            increase(fullHealth ? 1 : 2);
        }

        public static void increase(int amount) {
            pveCombatScore = Math.min(pveCombatScore + amount, 100);
            updateCombatScore();
        }

        public static void decreaseOverTime(boolean fullHealth) {
            decrease(fullHealth ? 8 : 4);
        }

        public static void decrease(int amount) {
            pveCombatScore -= amount;
            pveCombatScore = Math.max(pveCombatScore, 0);
            updateCombatScore();
        }

        public static boolean inCombat(int combatScore) {
            return pveCombatScore >= combatScore;
        }
    }

    public static class PvP {
        public static void increaseFromAttack() {
            increase(10);
            lastPlayerAttackTimestamp = 0;
        }
        public static void increaseOverTime(boolean fullHealth) {
            increase(fullHealth ? 1 : 2);
        }
        public static void increase(int amount) {
            pvpCombatScore = Math.min(pvpCombatScore + amount, 100);
            lastFightingPlayerTimestamp = 0;
            updateCombatScore();
        }

        public static void decreaseOverTime(boolean fullHealth) {
            decrease(fullHealth ? 4 : 2);
        }
        public static void decrease(int amount) {
            pvpCombatScore = Math.max(pvpCombatScore - amount, 0);
            updateCombatScore();
        }

        public static boolean inCombat(int combatScore) {
            return pvpCombatScore >= combatScore;
        }
    }

    public static void init() {
        UnifiedClientEvents.Instance.onTick(EventTiming.PRE, client -> {
            LocalPlayer player = client.player;
            if (player == null || !EventHelper.isEnabled()) {
                if (!reset) {
                    combatScore = 0;
                    pveCombatScore = 0;
                    pvpCombatScore = 0;
                    lastPlayerAttackTimestamp = 1200;
                    lastFightingPlayerTimestamp = 1200;
                    ticks = 0;
                    CombatMusicHandler.clientPlayerTrackedByMob = false;
                    reset = true;
                }
                return;
            }
            if (reset) {
                lastHurtByTimestamp = player.getLastHurtByMobTimestamp();
                lastHurtTimestamp = player.getLastHurtMobTimestamp();
                reset = false;
            }

            onTick(player);

            ticks++;
            if (ticks >= 20) {
                ticks = 0;
                onSecond(player);
            }
        });
    }

    private static void onTick(LocalPlayer player) {
        if (lastFightingPlayerTimestamp < 1200) lastFightingPlayerTimestamp++;

        int hurtByTimestamp = player.getLastHurtByMobTimestamp();
        if (hurtByTimestamp != lastHurtByTimestamp) {
            lastHurtByTimestamp = hurtByTimestamp;

            if (player.getLastHurtByMob() instanceof Enemy) PvE.increaseFromAttack();
            else if (player.getLastHurtByMob() instanceof Player) PvP.increaseFromAttack();
        }

        int hurtTimestamp = player.getLastHurtMobTimestamp();
        if (hurtTimestamp != lastHurtTimestamp) {
            lastHurtTimestamp = hurtTimestamp;

            if (player.getLastHurtMob() instanceof Enemy) PvE.increaseFromAttack();
            else if (player.getLastHurtMob() instanceof Player) PvP.increaseFromAttack();
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
        if (playerTrackedByMob) PvE.increaseOverTime(atFullHealth);
        else PvE.decreaseOverTime(atFullHealth);

        if (pvpCombatScore >= 25 && !player.level().getEntitiesOfClass(
                Player.class,
                player.getBoundingBox().inflate(32),
                otherPlayer -> otherPlayer != player && !otherPlayer.isSpectator() && otherPlayer.isAlive() && otherPlayer.hasLineOfSight(player) && otherPlayer.distanceToSqr(player) <= 32 * 32
        ).isEmpty()) {
            PvP.increaseOverTime(atFullHealth);
        } else if (lastFightingPlayerTimestamp >= 100 || lastPlayerAttackTimestamp >= 600) PvP.decreaseOverTime(atFullHealth);
    }
}