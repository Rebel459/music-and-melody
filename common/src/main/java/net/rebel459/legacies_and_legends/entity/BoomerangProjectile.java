package net.rebel459.legacies_and_legends.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rebel459.legacies_and_legends.item.BoomerangItem;
import net.rebel459.legacies_and_legends.registry.LaLEntityTypes;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BoomerangProjectile extends AbstractArrow {
    private static final EntityDataAccessor<Byte> ID_REBOUND = SynchedEntityData.defineId(BoomerangProjectile.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ID_FEATHERWEIGHT = SynchedEntityData.defineId(BoomerangProjectile.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ID_SHADOWSTEP = SynchedEntityData.defineId(BoomerangProjectile.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ID_FOIL = SynchedEntityData.defineId(BoomerangProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WOBBLING = SynchedEntityData.defineId(BoomerangProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final float WATER_INERTIA = 0.1F;
    private static final float ROTATION_AMOUNT = 55F;
    public int clientSideReturnBoomerangTickCount;

    private boolean dealtDamage;
    public int loopTick = 3;
    public float spinTick = 0F;
    public boolean hasTeleported = false;
    public float fBoost = 0F;
    public double gravity = 0D;
    public int s = 0;
    public int d = 0;

    public float prevWobbleProgress;
    public float wobbleProgress;
    public float prevYaw;
    public float yaw;
    private float lookRot;

    private byte getReboundFromItem(ItemStack stack) {
        return this.level() instanceof ServerLevel serverLevel
                ? (byte)Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this), 0, 127)
                : 0;
    }

    private byte getFeatherweightFromItem(ItemStack stack) {
        return this.level() instanceof ServerLevel serverLevel
                ? (byte)Mth.clamp(EnchantmentHelper.getFishingTimeReduction(serverLevel, stack, this), 0, 127)
                : 0;
    }

    private byte getShadowstepFromItem(ItemStack stack) {
        return this.level() instanceof ServerLevel serverLevel
                ? (byte)Mth.clamp(EnchantmentHelper.getTridentSpinAttackStrength(stack, (LivingEntity) this.getOwner()), 0, 127)
                : 0;
    }

    public boolean isFoil() {
        return this.entityData.get(ID_FOIL);
    }

    public BoomerangProjectile(EntityType<? extends BoomerangProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    public BoomerangProjectile(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(LaLEntityTypes.BOOMERANG.get(), shooter, level, pickupItemStack, null);
        this.entityData.set(ID_REBOUND, this.getReboundFromItem(pickupItemStack));
        this.entityData.set(ID_SHADOWSTEP, this.getShadowstepFromItem(pickupItemStack));
        this.entityData.set(ID_FEATHERWEIGHT, this.getFeatherweightFromItem(pickupItemStack));
        this.entityData.set(ID_FOIL, pickupItemStack.hasFoil());
    }

    public BoomerangProjectile(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(LaLEntityTypes.BOOMERANG.get(), x, y, z, level, pickupItemStack, pickupItemStack);
        this.entityData.set(ID_REBOUND, this.getReboundFromItem(pickupItemStack));
        this.entityData.set(ID_SHADOWSTEP, this.getShadowstepFromItem(pickupItemStack));
        this.entityData.set(ID_FEATHERWEIGHT, this.getFeatherweightFromItem(pickupItemStack));
        this.entityData.set(ID_FOIL, pickupItemStack.hasFoil());
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float divergence) {
        if (this.entityData.get(ID_FEATHERWEIGHT) / 10 >= 1) {
            this.s = this.entityData.get(ID_FEATHERWEIGHT) / 10;
            this.d = this.entityData.get(ID_FEATHERWEIGHT) * 10;
        } else {
            this.s = 1;
            this.d = 1;
        }
        super.shoot(x, y * 0.5D, z, speed * 0.8F * this.s, divergence * 0.5F / this.d);
    }

    @Override
    protected double getDefaultGravity() {
        if (this.isInWater() || this.isInPowderSnow || this.isInLava()) {
            this.gravity = 0.1D;
        } else if (this.wasTouchingWater || this.isInWaterOrRain()) {
            this.gravity = 0.02D;
        } else {
            this.gravity = 0.01D;
        }

        if (this.entityData.get(ID_FEATHERWEIGHT) / 10 >= 1) {
            this.gravity = this.gravity / this.entityData.get(ID_FEATHERWEIGHT) * 10D;
        }

        if (this.entityData.get(ID_SHADOWSTEP) > 0) {
            this.gravity = this.gravity * 1.5D;
        }

        return this.gravity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WOBBLING, false);
        builder.define(ID_REBOUND, (byte)0);
        builder.define(ID_SHADOWSTEP, (byte)0);
        builder.define(ID_FEATHERWEIGHT, (byte)0);
        builder.define(ID_FOIL, false);
    }

    @Override
    public void tick() {
        if (this.inGroundTime <= 0 && !this.isInWater() && !this.isInPowderSnow && !this.isInLava()) {
            this.spinTick = this.spinTick + 1;
            this.loopTick = this.loopTick + 1;
            if (this.loopTick >= 4){
                if (!this.isInWater() && !this.isInPowderSnow) {
                    this.playSound(LaLSounds.BOOMERANG_WHOOSH.get());
                }
                this.loopTick = 0;
            }
        }

        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity entity = this.getOwner();
        int rebound = this.entityData.get(ID_REBOUND);
        int shadowstep = this.entityData.get(ID_SHADOWSTEP);
        if (rebound > 0 && (this.dealtDamage || this.isNoPhysics()) && entity instanceof Player player) {
            if (!this.isAcceptibleReturnOwner()) {
                if (this.level() instanceof ServerLevel serverLevel && this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(serverLevel, this.getPickupItem(), 0.1F);
                }

                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 vec3 = entity.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + vec3.y * 0.015 * (double)rebound, this.getZ());
                double d = 0.05 * (double)rebound;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(vec3.normalize().scale(d)));
                if (this.clientSideReturnBoomerangTickCount == 0) {
                    this.playSound(LaLSounds.BOOMERANG_RETURN.get(), 10.0F, 1.0F);
                }

                this.clientSideReturnBoomerangTickCount++;
            }
        }
        else if (entity instanceof ServerPlayer player && shadowstep > 0 && (this.dealtDamage || this.isNoPhysics())) {
            if (!this.hasTeleported) {
                player.teleport(new TeleportTransition((ServerLevel) this.level(), this.position(), Vec3.ZERO, 0.0F, 0.0F, Relative.union(Relative.ROTATION, Relative.DELTA), TeleportTransition.DO_NOTHING));
                player.level().playSound(null, player.blockPosition(), LaLSounds.TABLET_TELEPORT.get(), SoundSource.PLAYERS, 0.6F, 1F);
                this.hasTeleported = true;
                if (player.gameMode() != GameType.CREATIVE) player.getCooldowns().addCooldown(this.getPickupItemStackOrigin(), 1200);;
            }
            this.setNoPhysics(true);
        }

        super.tick();
        Vec3 deltaPos = this.getDeltaPos();
        this.setAngles(deltaPos);
    }

    private boolean isAcceptibleReturnOwner() {
        Entity entity = this.getOwner();
        return entity != null && entity.isAlive() && (!(entity instanceof ServerPlayer) || !entity.isSpectator());
    }

    public void setAngles(@NotNull Vec3 deltaPos) {
        if (deltaPos.horizontalDistance() > 0.01D) this.lookRot = -((float) Mth.atan2(deltaPos.x, deltaPos.z)) * Mth.RAD_TO_DEG;
        float yRot = this.getYRot();
        this.setYRot(yRot + ((this.lookRot - yRot) * 0.25F));

        this.prevYaw = this.yaw;
        float speed = (float) deltaPos.length();
        this.yaw -= Math.min(speed, 1.0F) * 0.01F;

        if (this.yaw > 360F) {
            this.yaw -= 360F;
            this.prevYaw -= 360F;
        } else if (this.yaw < 0F) {
            this.yaw += 360F;
            this.prevYaw += 360F;
        }
    }

    @NotNull
    public Vec3 getDeltaPos() {
        return this.getPosition(1).subtract(this.getPosition(0));
    }

    public boolean isWobbling() {
        return this.entityData.get(WOBBLING);
    }

    public void setWobbling(boolean wobbling) {
        this.entityData.set(WOBBLING, wobbling);
    }

    public float getWobbleProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevWobbleProgress, this.wobbleProgress);
    }

    public float getSpinTick() {
        return this.spinTick;
    }

    public float getBoomerangYaw(float partialTick) {
        return Mth.lerp(partialTick, this.prevYaw, this.yaw);
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return this.dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    // Copy of Projectile onHit with added Rebound cooldown
    @Override
    public void onHit(HitResult result) {
        HitResult.Type type = result.getType();
        if (type == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)result;
            Entity entity = entityHitResult.getEntity();
            if (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
                projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this.getOwner(), this.owner, true);
            }
            this.onHitEntity(entityHitResult);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, result.getLocation(), GameEvent.Context.of(this, null));
        } else if (type == HitResult.Type.BLOCK) {
            if (this.getOwner() instanceof Player player && this.entityData.get(ID_REBOUND) > 0 && player.gameMode() != GameType.CREATIVE) player.getCooldowns().addCooldown(this.getPickupItemStackOrigin(), 600);
            BlockHitResult blockHitResult = (BlockHitResult)result;
            this.onHitBlock(blockHitResult);
            BlockPos blockPos = blockHitResult.getBlockPos();
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockPos, GameEvent.Context.of(this, this.level().getBlockState(blockPos)));
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity entity = result.getEntity();
        float f = BoomerangItem.THROW_DAMAGE;
        if (!this.isInWater() && !this.isInLava() && !this.isInPowderSnow) {
            if (this.entityData.get(ID_FEATHERWEIGHT) / 10 >= 1) {
                this.fBoost = this.spinTick * this.entityData.get(ID_FEATHERWEIGHT) / 10 / 15;
            } else {
                this.fBoost = this.spinTick * 5 / 10 / 15;
            }
            if (this.fBoost >= BoomerangItem.THROW_DAMAGE) {
                this.fBoost = BoomerangItem.THROW_DAMAGE;
            }
            f = f + this.fBoost;
        }
        Entity entity2 = this.getOwner();
        DamageSource damageSource = this.damageSources().trident(this, (Entity)(entity2 == null ? this : entity2));
        if (this.level() instanceof ServerLevel serverLevel) {
            f = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), entity, damageSource, f);
        }

        if (entity.hurtOrSimulate(damageSource, f)) {
            if (entity.getType() == EntityType.ENDERMAN) return;

            if (this.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(serverLevel, entity, damageSource, this.getWeaponItem(), item -> this.kill(serverLevel));
            }

            if (entity instanceof LivingEntity livingEntity) {
                this.doKnockback(livingEntity, damageSource);
                this.doPostHurtEffects(livingEntity);
                this.dealtDamage = true;
            }
        }

        this.deflect(ProjectileDeflection.REVERSE, entity, this.owner, false);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.02D, 0.2D, 0.02D));
        this.playSound(LaLSounds.BOOMERANG_HIT.get(), 1F, 1F);

    }

    @Override
    protected void hitBlockEnchantmentEffects(ServerLevel level, @NotNull BlockHitResult hitResult, ItemStack stack) {
        Vec3 vec3 = hitResult.getBlockPos().clampLocationWithin(hitResult.getLocation());
        EnchantmentHelper.onHitBlock(
                level,
                stack,
                this.getOwner() instanceof LivingEntity livingEntity ? livingEntity : null,
                this,
                null,
                vec3,
                level.getBlockState(hitResult.getBlockPos()),
                item -> this.kill(level)
        );
    }

    @Override
    public @NotNull ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player) || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return new ItemStack(LaLItems.BOOMERANG.get());
    }

    @Override
    protected @NotNull SoundEvent getDefaultHitGroundSoundEvent() {
        return LaLSounds.BOOMERANG_HIT.get();
    }

    @Override
    public void playerTouch(Player player) {
        this.hasTeleported = false;
        if (this.ownedBy(player) || this.getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        //this.dealtDamage = tag.getBoolean("DealtDamage");
        //this.setWobbling(tag.getBoolean("Wobbling"));
        this.entityData.set(ID_REBOUND, this.getReboundFromItem(this.getPickupItemStackOrigin()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", this.dealtDamage);
        output.putBoolean("Wobbling", this.isWobbling());
    }

    @Override
    public void tickDespawn() {
        int i = this.entityData.get(ID_REBOUND);
        if (this.pickup != Pickup.ALLOWED || i <= 0) super.tickDespawn();
    }

    @Override
    protected float getWaterInertia() {
        return WATER_INERTIA;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }
}
