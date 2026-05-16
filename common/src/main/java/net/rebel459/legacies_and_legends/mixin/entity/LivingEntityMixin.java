package net.rebel459.legacies_and_legends.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import net.rebel459.legacies_and_legends.event.ServerEvents;
import net.rebel459.legacies_and_legends.item.HookItem;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.registry.LaLDataComponents;
import net.rebel459.legacies_and_legends.registry.LaLEnchantments;
import net.rebel459.legacies_and_legends.registry.LaLMobEffects;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.Gem;
import net.rebel459.legacies_and_legends.util.PlatformInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    public void obsidianPlatform(double power, double xd, double zd, CallbackInfo ci) {
        LivingEntity entity = LivingEntity.class.cast(this);
        BlockState state = entity.getBlockStateOn();
        if (state.getBlock() instanceof WandPlatformBlock && WandPlatformBlock.hasMaterial(state, Gem.OBSIDIAN)) ci.cancel();
    }

    @Unique
    private DamageSource knockbackSource;

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;getDirectEntity()Lnet/minecraft/world/entity/Entity;"))
    public Entity getKnockbackSource(DamageSource source, Operation<Entity> original) {
        this.knockbackSource = source;
        return original.call(source);
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    public void hookPull(LivingEntity entity, double power, double xd, double zd, Operation<Void> original) {
        ItemStack stack = this.knockbackSource.getWeaponItem();
        if (stack != null && stack.getItem() instanceof HookItem) {
            int hauling = stack.getEnchantments().getLevel(entity.level.registryAccess().lookup(Registries.ENCHANTMENT).get().getOrThrow(LaLEnchantments.PULL));
            power = 0.3 + 0.1 * hauling;
            xd = -xd;
            zd = -zd;
        }
        original.call(entity, power, xd, zd);
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void removeTimelostEffect(CallbackInfo ci) {
        LivingEntity entity = LivingEntity.class.cast(this);
        if (entity.hasEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH) || entity.hasEffect(LaLMobEffects.LOW_GRAVITY)) {
            if (entity.isFallFlying() || !entity.getBlockStateOn().is(BlockTags.AIR) || entity.isInWater()) {
                Set<BlockPos> savedPositions = ServerEvents.SAVED_PLATFORMS.get(entity.level.dimension());
                if (savedPositions != null && savedPositions.contains(entity.getOnPos())) return;
                entity.removeEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH);
                entity.removeEffect(LaLMobEffects.LOW_GRAVITY);
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    public void frostedSpearFreeze(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity attacked = LivingEntity.class.cast(this);
        Entity entity = source.getEntity();
        if (!(entity instanceof LivingEntity attacker)) return;
        ItemStack stack = attacker.getWeaponItem();
        if (stack.is(LaLItemTags.CHILLING)) {
            float ignoredDamage = 0;
            if (stack.isEnchanted()) {
                ignoredDamage = EnchantmentHelper.modifyDamage(level, stack, attacked, source, 0F);
            }
            int duration = (int) Math.max(damage - ignoredDamage, 0);
            duration = Math.min(duration, 20) * 20;
            LaLMobEffects.applyFreezing(level, attacked, attacker, duration);
        }
    }

    @Inject(method = "hurtServer", at = @At(value = "HEAD"), cancellable = true)
    private void warpBeforeProjectile(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = LivingEntity.class.cast(this);
        if (entity.hasEffect(LaLMobEffects.WARPING) && damageSource.is(DamageTypeTags.IS_PROJECTILE)) {
            double d = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * (double) 32;
            double e = Mth.clamp(entity.getY() + (entity.getRandom().nextDouble() - 0.5) * (double) 32, entity.level.getMinY(), (entity.level.getMinY() + entity.level.getHeight() - 1));
            double f = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * (double) 32;
            if (entity.isPassenger()) {
                entity.stopRiding();
            }

            Vec3 vec3 = entity.position();
            boolean teleported = false;
            for (int i = 0; i < 64; i++) {
                if (entity.randomTeleport(d, e, f, true)) {
                    teleported = true;
                }
                if (teleported) break;
            }
            if (teleported) {
                entity.level.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(entity));
                level.playSound(null, entity.blockPosition(), LaLSounds.TABLET_TELEPORT.get(), SoundSource.PLAYERS, 0.6F, 1F);
            }

            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "HEAD"))
    private void warpWhenHurt(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo ci) {
        LivingEntity entity = LivingEntity.class.cast(this);
        if (entity.hasEffect(LaLMobEffects.WARPING) && damageSource.isDirect()) {
            double d = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * (double) 16;
            double e = Mth.clamp(entity.getY() + (entity.getRandom().nextDouble() - 0.5) * (double) 16, entity.level.getMinY(), (entity.level.getMinY() + entity.level.getHeight() - 1));
            double f = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * (double) 16;
            if (entity.isPassenger()) {
                entity.stopRiding();
            }

            Vec3 vec3 = entity.position();
            boolean teleported = false;
            for (int i = 0; i < 64; i++) {
                if (entity.randomTeleport(d, e, f, true)) {
                    teleported = true;
                }
                if (teleported) break;
            }
            if (teleported) {
                entity.level.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(entity));
                level.playSound(null, entity.blockPosition(), LaLSounds.TABLET_TELEPORT.get(), SoundSource.PLAYERS, 0.6F, 1F);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onItemPickup")
    private void dropWand(ItemEntity entity, CallbackInfo ci) {
        ItemStack stack = entity.getItem();
        if (stack.getItem() instanceof WandItem && this instanceof PlatformInterface platform) {
            WandItem.checkComponents(stack);
            WandItem.updateModel(stack, stack.get(LaLDataComponents.WAND_SLOTS.get()), !platform.getPlatformSummoned());
        }
    }

    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 0), method = "aiStep")
    private boolean lowGravityFallReset(LivingEntity entity, Holder<MobEffect> effect, Operation<Boolean> original) {
        return original.call(entity, effect) || entity.hasEffect(LaLMobEffects.LOW_GRAVITY);
    }
}
