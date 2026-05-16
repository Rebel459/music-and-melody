package net.rebel459.legacies_and_legends.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.registry.LaLMobEffects;
import net.rebel459.legacies_and_legends.util.Gem;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.util.EventType;

import java.util.*;

public class ServerEvents {

    public static final Map<PendingBlockChangeKey, PendingBlockChange> PENDING_BLOCK_CHANGES = new HashMap<>();
    public static final Map<PendingPlayerChangeKey, PendingPlayerChange> PENDING_PLAYER_CHANGES = new HashMap<>();

    public static final Map<ResourceKey<Level>, Set<BlockPos>> SAVED_PLATFORMS = new HashMap<>();

    public static void queueBlockChange(ServerLevel level, BlockPos pos, BlockState state, int delayTicks) {
        PendingBlockChangeKey key = new PendingBlockChangeKey(level.dimension(), pos);
        PENDING_BLOCK_CHANGES.put(key, new PendingBlockChange(level.dimension(), pos.immutable(), state, delayTicks));
    }

    public static void cancelBlockChange(ResourceKey<Level> dimension, BlockPos pos) {
        PENDING_BLOCK_CHANGES.remove(new PendingBlockChangeKey(dimension, pos));
    }

    public static void queuePlayerChange(Player player, Gem.Slots gems, int delayTicks) {
        PendingPlayerChangeKey key = new PendingPlayerChangeKey(player, gems);
        PENDING_PLAYER_CHANGES.put(key, new PendingPlayerChange(player, gems, delayTicks));
    }

    public static void init() {
        UnifiedEvents.Server.onLevelTick(EventType.PRE, level -> {
            Set<BlockPos> savedPlatforms = SAVED_PLATFORMS.get(level.dimension());
            if (savedPlatforms == null || savedPlatforms.isEmpty()) return;
            Set<BlockPos> retainedPlatforms = new HashSet<>();

            for (Entity entity : level.getEntities().getAll()) {
                if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.onGround()) continue;

                BlockPos onPos = livingEntity.getOnPos();
                if (savedPlatforms.contains(onPos)) {
                    retainedPlatforms.add(onPos);
                }
            }

            Iterator<BlockPos> iterator = savedPlatforms.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();

                if (retainedPlatforms.contains(pos)) continue;

                BlockState state = level.getBlockState(pos);
                level.scheduleTick(pos, state.getBlock(), 25);
                iterator.remove();
            }
            if (!retainedPlatforms.isEmpty()) SAVED_PLATFORMS.put(level.dimension(), retainedPlatforms);
        });
        UnifiedEvents.Server.onTick(EventType.PRE, server -> {
            Iterator<PendingBlockChange> iterator = PENDING_BLOCK_CHANGES.values().iterator();
            while (iterator.hasNext()) {
                PendingBlockChange change = iterator.next();
                change.ticksRemaining--;

                if (change.ticksRemaining <= 0) {
                    ServerLevel level = server.getLevel(change.dimension);
                    if (level != null) {
                        level.setBlock(change.pos, change.state, Block.UPDATE_ALL);
                    }
                    iterator.remove();
                }
            }
        });
        UnifiedEvents.Server.onTick(EventType.POST, server -> {
            Iterator<PendingPlayerChange> iterator = PENDING_PLAYER_CHANGES.values().iterator();
            while (iterator.hasNext()) {
                PendingPlayerChange change = iterator.next();
                change.ticksRemaining--;

                if (change.ticksRemaining <= 0) {
                    if (change.gems.primary() == Gem.TIMELOST) {
                        change.player.addEffect(new MobEffectInstance(LaLMobEffects.LOW_GRAVITY, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                        change.player.addEffect(new MobEffectInstance(LaLMobEffects.PROJECTILE_PASSTHROUGH, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                    }
                    iterator.remove();
                }
            }
        });
    }

    public record PendingBlockChangeKey(ResourceKey<Level> dimension, BlockPos pos) {}

    public static class PendingBlockChange {
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;
        public final BlockState state;
        public int ticksRemaining;

        public PendingBlockChange(ResourceKey<Level> dimension, BlockPos pos, BlockState state, int ticksRemaining) {
            this.dimension = dimension;
            this.pos = pos;
            this.state = state;
            this.ticksRemaining = ticksRemaining;
        }
    }

    public record PendingPlayerChangeKey(Player player, Gem.Slots gems) {}

    public static class PendingPlayerChange {
        public final Player player;
        public final Gem.Slots gems;
        public int ticksRemaining;

        public PendingPlayerChange(Player player, Gem.Slots gems, int ticksRemaining) {
            this.player = player;
            this.gems = gems;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
