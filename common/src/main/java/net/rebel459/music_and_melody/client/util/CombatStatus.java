package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.rebel459.music_and_melody.network.CombatMusicHandler;
import net.rebel459.music_and_melody.network.ServerPresenceHandler;
import net.rebel459.unified.platform.client.UnifiedClientEvents;
import net.rebel459.unified.util.EventType;

import java.util.HashSet;
import java.util.Set;

public class CombatStatus {
    private static int pveScore = 0;
    private static int pvpScore = 0;

    private static int lastHurtByTimestamp;
    private static int lastHurtTimestamp;
    private static int lastFightingPlayerTimestamp = 1200;
    private static int lastPlayerAttackTimestamp = 1200;

    private static int ticks = 0;
    private static boolean reset = true;

    private static final Set<Integer> PVE_CONDITIONS = new HashSet<>();
    private static final Set<Integer> PVP_CONDITIONS = new HashSet<>();

    public static class PvE {
        public static void increaseFromAttack() {
            increase(2);
            pveScore = Math.max(10, pveScore);
        }

        public static void increaseOverTime(boolean fullHealth) {
            increase(fullHealth ? 1 : 2);
        }

        public static void increase(int amount) {
            pveScore = Math.min(pveScore + amount, 100);
        }

        public static void decreaseOverTime(boolean fullHealth) {
            decrease(fullHealth ? 8 : 4);
        }

        public static void decrease(int amount) {
            pveScore -= amount;
            pveScore = Math.max(pveScore, 0);
        }

        public static boolean inCombat(int combatScore) {
            if (pveScore >= combatScore) PVE_CONDITIONS.add(combatScore);
            return PVE_CONDITIONS.contains(combatScore);
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
            pvpScore = Math.min(pvpScore + amount, 100);
            lastFightingPlayerTimestamp = 0;
        }

        public static void decreaseOverTime(boolean fullHealth) {
            decrease(fullHealth ? 4 : 2);
        }
        public static void decrease(int amount) {
            pvpScore = Math.max(pvpScore - amount, 0);
        }

        public static boolean inCombat(int combatScore) {
            if (pvpScore >= combatScore) PVP_CONDITIONS.add(combatScore);
            return PVP_CONDITIONS.contains(combatScore);
        }
    }

    public static void init() {
        UnifiedClientEvents.Instance.onTick(EventType.PRE, client -> {
            LocalPlayer player = client.player;
            if (player == null || !EventHelper.isEnabled()) {
                if (!reset) {
                    pveScore = 0;
                    pvpScore = 0;
                    PVE_CONDITIONS.clear();
                    PVP_CONDITIONS.clear();
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
        if (lastPlayerAttackTimestamp < 1200) lastPlayerAttackTimestamp++;

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

        if (pvpScore >= 25 && !player.level().getEntitiesOfClass(
                Player.class,
                player.getBoundingBox().inflate(32),
                otherPlayer -> otherPlayer != player && !otherPlayer.isSpectator() && otherPlayer.isAlive() && otherPlayer.hasLineOfSight(player) && otherPlayer.distanceToSqr(player) <= 32 * 32
        ).isEmpty()) {
            PvP.increaseOverTime(atFullHealth);
        } else if (lastFightingPlayerTimestamp >= 100 || lastPlayerAttackTimestamp >= 600) PvP.decreaseOverTime(atFullHealth);

        if (pveScore == 0) PVE_CONDITIONS.clear();
        if (pvpScore == 0) PVP_CONDITIONS.clear();
    }
}