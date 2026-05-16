package net.rebel459.legacies_and_legends.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import net.rebel459.legacies_and_legends.event.ServerEvents;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.legacies_and_legends.registry.LaLDataComponents;
import net.rebel459.legacies_and_legends.registry.LaLMobEffects;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.rebel459.legacies_and_legends.util.Gem;
import net.rebel459.legacies_and_legends.util.PlatformInterface;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WandItem extends Item {
    private static final double TELEPORT_STEP = 0.25D;
    private static final double TELEPORT_BLOCK_MARGIN = 0.1D;

    public WandItem(Properties properties) {
        super(properties);
    }

    public static boolean hasGems(ItemStack stack) {
        checkComponents(stack);
        return stack.has(LaLDataComponents.WAND_SLOTS.get()) && (stack.get(LaLDataComponents.WAND_SLOTS.get()).primary() != Gem.EMPTY || stack.get(LaLDataComponents.WAND_SLOTS.get()).secondary() != Gem.EMPTY);
    }

    public static Gem.Slots getGems(ItemStack stack) {
        checkComponents(stack);
        return stack.get(LaLDataComponents.WAND_SLOTS.get());
    }

    public static boolean hasGem(Gem.Slots gems, Gem gem) {
        return gems.primary() == gem || gems.secondary() == gem;
    }

    public static void setGems(ItemStack stack, Gem primary, Gem secondary) {
        checkComponents(stack);
        var currentGems = stack.get(LaLDataComponents.WAND_SLOTS.get());
        if (primary == null) primary = currentGems.primary();
        if (secondary == null) secondary = currentGems.secondary();
        String currentState = stack.get(DataComponents.CUSTOM_MODEL_DATA).getString(1);
        if (currentState == null || currentState.isEmpty()) currentState = "charged";
        Gem.Slots newGems = new Gem.Slots(primary, secondary);
        updateModel(stack, newGems, currentState.equals("charged"));
        stack.set(LaLDataComponents.WAND_SLOTS.get(), newGems);
        if (newGems.primary() == Gem.EMPTY && newGems.secondary() == Gem.EMPTY) stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
        else if (newGems.primary() != Gem.EMPTY && newGems.secondary() != Gem.EMPTY) stack.set(DataComponents.RARITY, Rarity.EPIC);
        else stack.set(DataComponents.RARITY, Rarity.RARE);
    }

    public static void checkComponents(ItemStack stack) {
        if (!stack.has(LaLDataComponents.WAND_SLOTS.get())) {
            Gem.Slots gems = new Gem.Slots(Gem.SAPPHIRE, Gem.EMPTY);
            String currentState = stack.get(DataComponents.CUSTOM_MODEL_DATA).getString(1);
            if (currentState == null || currentState.isEmpty()) currentState = "charged";
            updateModel(stack, gems, currentState.equals("charged"));
            stack.set(LaLDataComponents.WAND_SLOTS.get(), new Gem.Slots(Gem.SAPPHIRE, Gem.EMPTY));
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            stack.set(DataComponents.RARITY, Rarity.RARE);
        }
    }

    public static void updateModel(ItemStack stack, Gem.Slots gems, boolean charged) {
        String name = gems.primary().getSerializedName();
        if (gems.primary() == Gem.EMPTY) name = gems.secondary().getSerializedName();
        String currentState = "charged";
        if (!charged) currentState = "summoned";
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(name, currentState), List.of()));
    }

    public static float getCooldown(Gem.Slots gems) {
        float cooldown = 2F;
        if (hasGem(gems, Gem.METEORITE)) cooldown += 2F;
        if (hasGem(gems, Gem.BREEZE)) cooldown += 6F;
        if (gems.primary() == Gem.NEBULITE) cooldown += 8F;
        if (hasGem(gems, Gem.TIMELOST)) cooldown += 4F;
        if (hasGem(gems, Gem.SAPPHIRE)) cooldown -= 2F;
        cooldown = Math.max(cooldown, 1F);
        return cooldown;
    }

    private static HashMap<BlockPos, BlockState> getSurroundingBlocks(Level level, BlockPos pos, int radius, boolean grounded) {
        HashMap<BlockPos, BlockState> surroundingBlocks = new HashMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) continue;

                BlockPos basePos = pos.offset(x, 0, z);

                if (!grounded && level.getBlockState(basePos).canBeReplaced()) {
                    BlockState oldState = level.getBlockState(basePos);
                    if (oldState.getFluidState().isEmpty()) oldState = Blocks.AIR.defaultBlockState();
                    surroundingBlocks.put(basePos, oldState);
                    continue;
                }

                for (int drop = -1; drop <= 2; drop++) {
                    BlockPos candidate = basePos.offset(0, -drop, 0);
                    BlockState state = level.getBlockState(candidate);
                    BlockState ground = level.getBlockState(candidate.below());

                    if (!state.blocksMotion() && ground.blocksMotion() && state.canBeReplaced()) {
                        if (state.getFluidState().isEmpty()) state = Blocks.AIR.defaultBlockState();
                        surroundingBlocks.put(candidate, state);
                        break;
                    }
                }
            }
        }

        return surroundingBlocks;
    }

    private static HashMap<BlockPos, BlockState> getFireTargetPositions(Level level, BlockPos pos, Gem.Slots gems) {
        if (gems.primary() == Gem.METEORITE) return getSurroundingBlocks(level, pos, 4, true);
        if (gems.secondary() == Gem.METEORITE) return getSurroundingBlocks(level, pos, 2, true);
        return new HashMap<>();
    }

    private static HashMap<BlockPos, BlockState> getIceTargetPositions(Level level, BlockPos pos, Gem.Slots gems) {
        if (gems.primary() == Gem.ICE) return getSurroundingBlocks(level, pos, 2, false);
        if (gems.secondary() == Gem.ICE) return getSurroundingBlocks(level, pos, 1, false);
        return new HashMap<>();
    }

    private static HashMap<BlockPos, BlockState> getTargetPositions(Level level, BlockPos pos, Gem.Slots gems) {
        HashMap<BlockPos, BlockState> combinedPositions = new HashMap<>(getFireTargetPositions(level, pos, gems));
        combinedPositions.putAll(getIceTargetPositions(level, pos, gems));
        return combinedPositions;
    }

    private static boolean isUnderwaterPlacement(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    private void handlePrismarineMaterial(Level level, BlockPos pos, PlatformInterface platform, boolean useBottomSlab, boolean underwaterPlacement, boolean createBubbleColumns) {
        BlockState targetState = level.getBlockState(pos);
        if (!targetState.hasProperty(BlockStateProperties.WATERLOGGED)) return;

        HashMap<BlockPos, BlockState> states = platform.getOldStates();

        if (useBottomSlab && underwaterPlacement) {
            if (targetState.getFluidState().isEmpty()) {
                states.putIfAbsent(pos, Blocks.AIR.defaultBlockState());
            }

            targetState = targetState.setValue(BlockStateProperties.WATERLOGGED, true);
            level.setBlock(pos, targetState, Block.UPDATE_ALL);
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (underwaterPlacement && aboveState.canBeReplaced()) {
            states.putIfAbsent(abovePos, aboveState);
            level.setBlock(abovePos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (!createBubbleColumns) return;

        for (int height = 1; height <= 3; height++) {
            BlockPos columnPos = pos.above(height);
            BlockState columnState = level.getBlockState(columnPos);
            if (!columnState.getFluidState().is(FluidTags.WATER)) {
                break;
            }

            states.putIfAbsent(columnPos, columnState);
            level.setBlock(
                    columnPos,
                    Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, true),
                    Block.UPDATE_ALL
            );
        }
    }

    private static boolean tryTeleport(Player player, Gem.Slots gems, double distance, Vec3 end) {
        Vec3 start = player.position();
        Vec3 delta = end.subtract(start);

        if (delta.lengthSqr() > distance * distance) {
            end = start.add(delta.normalize().scale(distance));
            delta = end.subtract(start);
        }

        double pathLength = delta.length();
        if (pathLength <= 1.0E-6D) {
            return false;
        }

        Vec3 direction = delta.scale(1.0D / pathLength);

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hitResult.getType() != HitResult.Type.MISS) {
            pathLength = Math.max(0.0D, start.distanceTo(hitResult.getLocation()) - TELEPORT_BLOCK_MARGIN);
        }

        for (double candidateDistance = pathLength; candidateDistance > 1.0E-6D; candidateDistance -= TELEPORT_STEP) {
            Vec3 target = start.add(direction.scale(candidateDistance));
            if (!canTeleportTo(player, gems, target)) {
                continue;
            }

            player.teleportTo(target.x, target.y, target.z);
            return true;
        }

        return false;
    }

    private static boolean canTeleportTo(Player player, Gem.Slots gems, Vec3 target) {
        BlockPos targetPos = BlockPos.containing(target);
        BlockState feetState = player.level().getBlockState(targetPos);
        BlockState headState = player.level().getBlockState(targetPos.above());

        if (feetState.blocksMotion() || headState.blocksMotion() || (!hasGem(gems, Gem.PRISMARINE) && !feetState.getFluidState().isEmpty() && (feetState.getFluidState().is(FluidTags.WATER) || headState.getFluidState().is(FluidTags.WATER)))) {
            return false;
        }

        AABB targetBounds = player.getBoundingBox().move(target.subtract(player.position()));
        return player.level().noCollision(player, targetBounds);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, InteractionHand hand) {
        if (!(player instanceof PlatformInterface platformInterface)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        Gem.Slots gems = getGems(stack);
        if (gems.primary() == Gem.EMPTY && gems.secondary() == Gem.EMPTY || player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;

        Vec3 playerPos = player.position();
        BlockPos newPlatformPos = player.blockPosition();
        if (hasGem(gems, Gem.NEBULITE)) {
            Vec3 eyePos = player.getEyePosition();
            Vec3 reachPos = eyePos.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

            newPlatformPos = hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getBlockPos().relative(hitResult.getDirection()) : BlockPos.containing(reachPos);
        }

        boolean teleported = false;
        if (!platformInterface.getPlatformSummoned() && gems.primary() == Gem.NEBULITE) {
            double distance = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1D;
            BlockPos rubyPlatformPos = level.getBlockState(newPlatformPos).isAir() ? newPlatformPos : newPlatformPos.below();
            Vec3 targetPos = Vec3.atBottomCenterOf(rubyPlatformPos.above());
            teleported = tryTeleport(player, gems, distance, targetPos);
        }

        boolean useBottomSlab = false;
        if (playerPos.y() - newPlatformPos.getY() >= 0.5D && level.getBlockState(newPlatformPos).isAir()) {
            useBottomSlab = true;
        } else {
            newPlatformPos = newPlatformPos.below();
        }

        boolean useWaterloggedDoubleSlab = hasGem(gems, Gem.PRISMARINE) && level.getBlockState(newPlatformPos).getFluidState().is(FluidTags.WATER);
        boolean canPlacePlatform = (useBottomSlab || level.getBlockState(newPlatformPos).isAir() || useWaterloggedDoubleSlab) && !platformInterface.getPlatformSummoned() && (!player.onGround() || hasGem(gems, Gem.NEBULITE));
        HashMap<BlockPos, BlockState> targetPositions = getTargetPositions(level, newPlatformPos, gems);
        boolean hasStandaloneAbility = hasGem(gems, Gem.BREEZE) || teleported || (hasGem(gems, Gem.METEORITE) && !targetPositions.isEmpty());
        boolean canSummonWithoutMainPlatform = !canPlacePlatform && !platformInterface.getPlatformSummoned() && player.onGround() && hasStandaloneAbility;
        boolean shouldSummon = canPlacePlatform || canSummonWithoutMainPlatform;

        if (!platformInterface.getPlatformSummoned()) {
            platformInterface.lal$setLastPlatformPos(level, newPlatformPos);

            if (shouldSummon && !level.isClientSide()) {
                prePlatformSummoned(level, player, stack, gems);
                HashMap<BlockPos, BlockState> fireTargets = getFireTargetPositions(level, newPlatformPos, gems);
                HashMap<BlockPos, BlockState> iceTargets = getIceTargetPositions(level, newPlatformPos, gems);
                platformInterface.setOldStates(targetPositions);
                List<BlockPos> validBlocks = platformInterface.getOldStates().keySet().stream().toList();
                List<BlockPos> placedBlocks = new ArrayList<>();
                if (canPlacePlatform) {
                    placedBlocks.add(newPlatformPos);
                }
                placedBlocks.addAll(validBlocks);
                Set<BlockPos> underwaterBlocks = new HashSet<>();
                if (hasGem(gems, Gem.PRISMARINE)) {
                    for (BlockPos placedBlock : placedBlocks) {
                        if (isUnderwaterPlacement(level, placedBlock)) {
                            underwaterBlocks.add(placedBlock);
                        }
                    }
                }

                if (level instanceof ServerLevel serverLevel) {
                    for (BlockPos placedBlock : placedBlocks) {
                        ServerEvents.cancelBlockChange(serverLevel.dimension(), placedBlock);
                    }
                }

                if (canPlacePlatform) {
                    level.setBlock(
                            newPlatformPos,
                            WandPlatformBlock.getSummonedState(stack, useBottomSlab, useWaterloggedDoubleSlab),
                            Block.UPDATE_ALL
                    );
                }

                for (BlockPos targetPos : fireTargets.keySet()) {
                    level.setBlock(
                            targetPos,
                            Blocks.FIRE.defaultBlockState().setValue(WandPlatformBlock.CANCEL_TICK, true),
                            Block.UPDATE_ALL
                    );
                }

                for (BlockPos targetPos : iceTargets.keySet()) {
                    level.setBlock(
                            targetPos,
                            WandPlatformBlock.getSummonedState(stack, useBottomSlab, useWaterloggedDoubleSlab),
                            Block.UPDATE_ALL
                    );
                }

                if (hasGem(gems, Gem.PRISMARINE)) {
                    boolean createBubbleColumns = gems.primary() == Gem.PRISMARINE;
                    for (BlockPos targetPos : placedBlocks) {
                        boolean underwaterPlacement = underwaterBlocks.contains(targetPos);
                        handlePrismarineMaterial(level, targetPos, platformInterface, useBottomSlab, underwaterPlacement, createBubbleColumns && underwaterPlacement);
                    }
                }

                HashMap<BlockPos, BlockState> placedStates = new HashMap<>();
                for (BlockPos placedBlock : placedBlocks) {
                    placedStates.put(placedBlock.immutable(), level.getBlockState(placedBlock));
                }
                platformInterface.setPlatformStates(placedStates);
            }

            platformInterface.setPlatformSummoned(true);

            player.playSound(LaLSounds.WAND_SUMMON.get());

            hurtAndBreak(stack, gems, player, hand);

            stack.applyComponents(DataComponentPatch.builder()
                    .set(DataComponents.USE_COOLDOWN, new UseCooldown(getCooldown(gems)))
                    .build()
            );

            updateModel(stack, gems, false);

            return InteractionResult.SUCCESS;
        } else {
            InteractionResult result = removePlatforms(level, player);
            if (result == InteractionResult.SUCCESS) {
                if (!level.isClientSide()) postPlatformRecalled(level, player, stack, gems, newPlatformPos, useBottomSlab, useWaterloggedDoubleSlab);

                stack.applyComponents(DataComponentPatch.builder()
                        .set(DataComponents.USE_COOLDOWN, new UseCooldown(0.5F))
                        .build()
                );

                updateModel(stack, gems, true);

                return result;
            }

        }

        return InteractionResult.FAIL;
    }

    public static void hurtAndBreak(ItemStack stack, Gem.Slots gems, Player player, InteractionHand hand) {
        if (gems.primary() == Gem.SAPPHIRE && player.getRandom().nextBoolean()) return;
        stack.hurtAndBreak(1, player, hand);
    }

    public static InteractionResult removePlatforms(Level level, Player player) {
        if (!(player instanceof PlatformInterface platform)) return InteractionResult.PASS;
        Optional<GlobalPos> optionalLastPlatformPos = platform.lal$getLastPlatformPos();
        if (optionalLastPlatformPos.isPresent() && platform.getPlatformSummoned()) {
            GlobalPos lastPlatformPos = optionalLastPlatformPos.get();
            if (lastPlatformPos.dimension().equals(level.dimension())) {
                BlockPos lastPlatformBlockPos = lastPlatformPos.pos();
                player.playSound(LaLSounds.WAND_RECALL.get());
                removePlatforms(level, platform, lastPlatformBlockPos);

                return InteractionResult.SUCCESS;
            }
        }
        platform.setPlatformSummoned(false);
        return InteractionResult.PASS;
    }

    public static void removePlatforms(Level level, PlatformInterface platform, BlockPos lastPos) {
        platform.setPlatformSummoned(false);
        if (level.isClientSide()) return;
        Set<BlockPos> validBlocks = new HashSet<>(platform.getOldStates().keySet());
        validBlocks.addAll(platform.getPlatformStates().keySet());
        validBlocks.addAll(getCurrentPlatformBlocks(level, lastPos));
        validBlocks.remove(lastPos);

        Set<BlockPos> savedPlatforms = ServerEvents.SAVED_PLATFORMS.get(level.dimension());
        if (savedPlatforms == null) savedPlatforms = new HashSet<>();

        if (level instanceof ServerLevel serverLevel) {
            for (BlockPos targetPos : validBlocks) {
                BlockState state = serverLevel.getBlockState(targetPos);
                if (WandPlatformBlock.hasMaterial(state, Gem.RUBY)) {
                    savedPlatforms.add(targetPos);
                    continue;
                }
                BlockState restoredState = platform.getOldStates().getOrDefault(targetPos, Blocks.AIR.defaultBlockState());
                ServerEvents.queueBlockChange(serverLevel, targetPos, restoredState, 5);
            }
        }

        BlockState state = level.getBlockState(lastPos);
        if (state.hasProperty(WandPlatformBlock.PRIMARY_MATERIAL) && state.hasProperty(WandPlatformBlock.SECONDARY_MATERIAL) && WandPlatformBlock.hasMaterial(state, Gem.RUBY)) {
            savedPlatforms.add(lastPos);
            ServerEvents.SAVED_PLATFORMS.put(level.dimension(), savedPlatforms);
            return;
        } else {
            if (!savedPlatforms.isEmpty()) ServerEvents.SAVED_PLATFORMS.put(level.dimension(), savedPlatforms);
        }
        level.scheduleTick(lastPos, LaLBlocks.WAND_PLATFORM.get(), 5);
        platform.setPlatformStates(new HashMap<>());
    }

    private static Set<BlockPos> getCurrentPlatformBlocks(Level level, BlockPos lastPos) {
        Set<BlockPos> positions = new HashSet<>();
        BlockState mainState = level.getBlockState(lastPos);
        Gem primary = mainState.hasProperty(WandPlatformBlock.PRIMARY_MATERIAL) ? mainState.getValue(WandPlatformBlock.PRIMARY_MATERIAL) : null;
        Gem secondary = mainState.hasProperty(WandPlatformBlock.SECONDARY_MATERIAL) ? mainState.getValue(WandPlatformBlock.SECONDARY_MATERIAL) : null;

        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 1; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = lastPos.offset(x, y, z);
                    if (pos.equals(lastPos)) continue;

                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.FIRE) && state.hasProperty(WandPlatformBlock.CANCEL_TICK) && state.getValue(WandPlatformBlock.CANCEL_TICK)) {
                        positions.add(pos.immutable());
                        continue;
                    }

                    if (!state.is(LaLBlocks.WAND_PLATFORM.get()) || primary == null || secondary == null) continue;
                    if (state.getValue(WandPlatformBlock.PRIMARY_MATERIAL) != primary) continue;
                    if (state.getValue(WandPlatformBlock.SECONDARY_MATERIAL) != secondary) continue;
                    positions.add(pos.immutable());
                }
            }
        }

        return positions;
    }

    public static int applyBreezeKnockback(Level level, LivingEntity source, double strength) {
        level.levelEvent(2013, source.getOnPos(), 750);
        List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(3.5F), MaceItem.knockbackPredicate(source, source));

        int entityCount = 0;

        for (LivingEntity livingEntity : entityList) {
            Vec3 vec3 = livingEntity.position().subtract(source.position());
            double d = strength * (1.0 - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 vec32 = vec3.normalize().scale(d);
            if (d > (double)0.0F) {
                livingEntity.push(vec32.x, 0.7F, vec32.z);
                if (livingEntity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
                }
            }
            entityCount += 1;
        }

        return entityCount;
    }

    private void prePlatformSummoned(Level level, Player player, ItemStack stack, Gem.Slots gems) {
        if (hasGem(gems, Gem.BREEZE)) {
            applyBreezeKnockback(level, player, 2F);
        }
        if (player.hasEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH)) {
            player.removeEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH);
        }
    }
    private void postPlatformRecalled(Level level, Player player, ItemStack stack, Gem.Slots gems, BlockPos platformPos, boolean useBottomSlab, boolean useWaterloggedDoubleSlab) {
        if (gems.primary() == Gem.BREEZE) applyBreezeKnockback(level, player, 2F);
        if (hasGem(gems, Gem.TIMELOST)) {
            ServerEvents.queuePlayerChange(player, gems, 5);
        }
        if (gems.primary() == Gem.RUBY) {
            level.setBlock(platformPos, WandPlatformBlock.getSummonedState(stack, useBottomSlab, useWaterloggedDoubleSlab), Block.UPDATE_ALL);
            Set<BlockPos> savedPositions = ServerEvents.SAVED_PLATFORMS.get(level.dimension());
            savedPositions.add(platformPos);
            ServerEvents.SAVED_PLATFORMS.put(level.dimension(), savedPositions);
        }
    }
}
