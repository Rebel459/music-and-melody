package net.rebel459.legacies_and_legends.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.legacies_and_legends.registry.LaLEntityTypes;
import org.spongepowered.asm.mixin.Unique;

public class GlowStickProjectile extends ThrowableItemProjectile {

    private boolean hitAndFalling = false;
    private BlockPos lastValidSupportPos;

    public GlowStickProjectile(EntityType<? extends GlowStickProjectile> type, Level level) {
        super(type, level);
    }

    public GlowStickProjectile(Level level, LivingEntity mob, ItemStack itemStack) {
        super(LaLEntityTypes.GLOW_STICK.get(), mob, level, itemStack);
    }

    public GlowStickProjectile(Level level, double x, double y, double z, ItemStack itemStack) {
        super(LaLEntityTypes.GLOW_STICK.get(), x, y, z, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return LaLBlocks.GLOW_STICK.asItem();
    }

    private ParticleOptions getParticle() {
        ItemStack item = this.getItem();
        return item.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(item));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particle = this.getParticle();

            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity entity = hitResult.getEntity();
        entity.hurt(this.damageSources().thrown(this, this.getOwner()), 0F);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
        }

        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.dropAsItem();
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        HitResult.Type type = hitResult.getType();
        if (type == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entityHit = entityHitResult.getEntity();
            if (entityHit.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entityHit instanceof Projectile projectile) {
                projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this.getOwner(), this.owner, true);
            }

            this.onHitEntity(entityHitResult);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, hitResult.getLocation(), GameEvent.Context.of(this, (BlockState) null));
        } else if (type == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            if (blockHitResult.getDirection().getAxis().isVertical()) {
                BlockPos landingSupportPos = this.getLandingSupportPos();
                if (landingSupportPos != null) {
                    this.land(landingSupportPos);
                } else {
                    this.dropAndDiscard(this.getSupportPos());
                }
                return;
            }

            this.hitAndFalling = true;
            Vec3 hitLocation = blockHitResult.getLocation();
            Direction direction = blockHitResult.getDirection();
            double wallOffset = this.getBbWidth() * 0.5D + 0.05D;
            this.setPos(
                    hitLocation.x + direction.getStepX() * wallOffset,
                    hitLocation.y - 0.02D,
                    hitLocation.z + direction.getStepZ() * wallOffset
            );
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Unique
    private void land(BlockPos supportPos) {
        this.level().gameEvent(GameEvent.PROJECTILE_LAND, supportPos, GameEvent.Context.of(this, this.level().getBlockState(supportPos)));

        if (!this.level().isClientSide()) {
            BlockPos placePos = supportPos.above();
            BlockState state = this.getPlacementState(placePos);
            if (state != null) {
                this.level().setBlock(placePos, state, Block.UPDATE_ALL);
            } else {
                this.dropAsItem();
            }

            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Unique
    private BlockPos getSupportPos() {
        return BlockPos.containing(this.getX(), this.getBoundingBox().minY - 0.05D, this.getZ());
    }

    @Unique
    private boolean isValidSupportPos(BlockPos supportPos) {
        return this.level().getBlockState(supportPos).isFaceSturdy(this.level(), supportPos, Direction.UP)
                && this.getPlacementState(supportPos.above()) != null;
    }

    @Unique
    private void updateLastValidSupportPos() {
        BlockPos supportPos = this.getSupportPos();
        if (this.isValidSupportPos(supportPos)) {
            this.lastValidSupportPos = supportPos.immutable();
        }
    }

    @Unique
    private BlockPos getLandingSupportPos() {
        BlockPos supportPos = this.getSupportPos();
        if (this.isValidSupportPos(supportPos)) {
            return supportPos;
        }

        if (this.lastValidSupportPos != null && this.isValidSupportPos(this.lastValidSupportPos)) {
            return this.lastValidSupportPos;
        }

        return null;
    }

    @Unique
    private BlockState getPlacementState(BlockPos placePos) {
        BlockState placeState = this.level().getBlockState(placePos);
        if (!placeState.canBeReplaced() || this.isInLava()) {
            return null;
        }

        Direction direction = switch (this.random.nextInt(4)) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };

        BlockState state = LaLBlocks.GLOW_STICK.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, this.level().getFluidState(placePos).getType().isSame(net.minecraft.world.level.material.Fluids.WATER));
        }

        return state.canSurvive(this.level(), placePos) ? state : null;
    }

    @Unique
    private void dropAsItem() {
        if (!this.level().isClientSide()) {
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem()));
        }
    }

    @Unique
    private void dropAndDiscard(BlockPos impactPos) {
        this.level().gameEvent(GameEvent.PROJECTILE_LAND, impactPos, GameEvent.Context.of(this, this.level().getBlockState(impactPos)));
        if (!this.level().isClientSide()) {
            this.dropAsItem();
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void tick() {
        if (this.hitAndFalling) {
            this.updateLastValidSupportPos();
            double previousY = this.getY();
            Vec3 velocity = this.getDeltaMovement().add(0.0D, -0.04D, 0.0D);
            this.setDeltaMovement(0.0D, Math.max(velocity.y, -0.4D), 0.0D);
            this.move(MoverType.SELF, this.getDeltaMovement());

            BlockPos landingSupportPos = this.getLandingSupportPos();
            boolean canSettle = landingSupportPos != null
                    && (this.onGround() || this.verticalCollisionBelow || this.getBoundingBox().minY <= landingSupportPos.above().getY() + 0.05D);
            if (canSettle) {
                this.land(landingSupportPos);
            } else if (landingSupportPos == null && (this.getY() >= previousY || this.verticalCollision || this.verticalCollisionBelow)) {
                this.dropAndDiscard(this.getSupportPos());
            }

            return;
        }

        super.tick();
        this.updateLastValidSupportPos();
    }
}
