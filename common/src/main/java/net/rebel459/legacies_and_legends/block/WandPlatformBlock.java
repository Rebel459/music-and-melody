package net.rebel459.legacies_and_legends.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.legacies_and_legends.registry.LaLDataComponents;
import net.rebel459.legacies_and_legends.util.FallOnInterface;
import net.rebel459.legacies_and_legends.util.Gem;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class WandPlatformBlock extends TransparentSlabBlock {

    public static final EnumProperty<Gem> PRIMARY_MATERIAL = EnumProperty.create("primary_material", Gem.class);
    public static final EnumProperty<Gem> SECONDARY_MATERIAL = EnumProperty.create("secondary_material", Gem.class);

    // External
    public static final BooleanProperty CANCEL_TICK = BooleanProperty.create("cancel_tick");

    public WandPlatformBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(PRIMARY_MATERIAL, Gem.SAPPHIRE).setValue(SECONDARY_MATERIAL, Gem.EMPTY));
    }

    @Override
    protected void tick(BlockState state, @NotNull ServerLevel level, BlockPos pos, RandomSource random) {
        level.destroyBlock(pos, false);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0F, entity.damageSources().fall());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Player player = context.getPlayer();
        if (player != null && player.getMainHandItem().getItem() instanceof WandItem && WandItem.hasGems(player.getMainHandItem())) {
            Gem.Slots gems = WandItem.getGems(player.getMainHandItem());
            state = state.setValue(PRIMARY_MATERIAL, gems.primary()).setValue(SECONDARY_MATERIAL, gems.secondary());
        }
        return state;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PRIMARY_MATERIAL);
        builder.add(SECONDARY_MATERIAL);
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce() || !(entity instanceof FallOnInterface fallOn) || !hasMaterial(fallOn.getFallOnState(), Gem.SLIME)) {
            super.updateEntityMovementAfterFallOn(level, entity);
        } else {
            handleSlimeBounciness(entity, fallOn);
        }
    }

    public static void handleSlimeBounciness(Entity entity, FallOnInterface fallOn) {
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < (double)0.0F) {
            double factor = entity instanceof LivingEntity ? (double) 1.0F : 0.8;
            if (fallOn.getFallOnState().hasProperty(PRIMARY_MATERIAL) && hasMaterial(fallOn.getFallOnState(), Gem.SLIME) && fallOn.getBounciness() <= 0) {
                int bounciness = 1;
                if (fallOn.getFallOnState().getValue(PRIMARY_MATERIAL) == Gem.SLIME) bounciness += 1;
                fallOn.setBounciness(bounciness);
            }
            entity.setDeltaMovement(movement.x, -movement.y * factor, movement.z);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        if (!hasMaterial(onState, Gem.SLIME)) {
            super.stepOn(level, pos, onState, entity);
            return;
        }
        double absDeltaY = Math.abs(entity.getDeltaMovement().y);
        if (absDeltaY < 0.1 && !entity.isSteppingCarefully()) {
            double scale = 0.4 + absDeltaY * 0.2;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(scale, 1.0F, scale));
        }

        super.stepOn(level, pos, onState, entity);
    }

    public static BlockState getSummonedState(ItemStack stack, boolean useBottomSlab, boolean useWaterloggedDoubleSlab) {
        Gem.Slots gems = WandItem.getGems(stack);
        SlabType type = SlabType.TOP;
        if (useWaterloggedDoubleSlab) type = SlabType.DOUBLE;
        else if (useBottomSlab) type = SlabType.BOTTOM;
        return LaLBlocks.WAND_PLATFORM.defaultBlockState().setValue(WandPlatformBlock.TYPE, type).setValue(PRIMARY_MATERIAL, gems.primary()).setValue(SECONDARY_MATERIAL, gems.secondary()).setValue(WandPlatformBlock.WATERLOGGED, useWaterloggedDoubleSlab);
    }

    public static float getFriction(BlockState state) {
        if (hasMaterial(state, Gem.ICE)) return 0.98F;
        return state.getBlock().getFriction();
    }

    public static boolean hasMaterial(BlockState state, Gem gem) {
        return state.getValue(PRIMARY_MATERIAL) == gem || state.getValue(SECONDARY_MATERIAL) == gem;
    }

    public static boolean supportsBubbleColumn(BlockState state) {
        return state.is(LaLBlocks.WAND_PLATFORM.get()) && state.getValue(PRIMARY_MATERIAL) == Gem.PRISMARINE;
    }
}
