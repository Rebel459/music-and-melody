package net.rebel459.legacies_and_legends.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.registry.LaLMobEffects;
import net.rebel459.legacies_and_legends.util.FallOnInterface;
import net.rebel459.legacies_and_legends.util.Gem;
import net.rebel459.legacies_and_legends.util.PlatformInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements FallOnInterface {

	@Unique
	private BlockState fallOnState;

	@Override
	public BlockState getFallOnState() {
		return this.fallOnState;
	}

	@Override
	public void setFallOnState(BlockState state) {
		this.fallOnState = state;
	}

	@Unique
	private int bounciness = 0;

	@Override
	public int getBounciness() {
		return this.bounciness;
	}

	@Override
	public void setBounciness(int bounciness) {
		this.bounciness = bounciness;
	}

	@WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
	public Block getFallOnState(BlockState state, Operation<Block> original) {
		Entity entity = Entity.class.cast(this);
		if (entity instanceof FallOnInterface fallOn) {
			fallOn.setFallOnState(state);
		}
		return original.call(state);
	}

	@Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
	public void obsidianPlatformSetFire(int remainingTicks, CallbackInfo ci) {
		Entity entity = Entity.class.cast(this);
		BlockState state = entity.getBlockStateOn();
		if (state.getBlock() instanceof WandPlatformBlock && state.getValue(WandPlatformBlock.PRIMARY_MATERIAL) == Gem.OBSIDIAN) ci.cancel();
	}

	@Inject(method = "getRemainingFireTicks", at = @At("HEAD"), cancellable = true)
	public void obsidianPlatformGetFire(CallbackInfoReturnable<Integer> cir) {
		Entity entity = Entity.class.cast(this);
		BlockState state = entity.getBlockStateOn();
		if (state.getBlock() instanceof WandPlatformBlock && state.getValue(WandPlatformBlock.PRIMARY_MATERIAL) == Gem.OBSIDIAN) cir.setReturnValue(0);
	}

    @Shadow
    public abstract int getTicksFrozen();

    @Inject(method = "canBeHitByProjectile", at = @At("TAIL"), cancellable = true)
	public void instabilityProjectile(CallbackInfoReturnable<Boolean> cir) {
		if (!(Entity.class.cast(this) instanceof Player player)) return;

		if (player.hasEffect(LaLMobEffects.INSTABILITY) || player.hasEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH)) cir.setReturnValue(false);
	}

	@Inject(method = "canBeCollidedWith", at = @At("TAIL"), cancellable = true)
	public void instabilityCollidedWith(CallbackInfoReturnable<Boolean> cir) {
		if (!(Entity.class.cast(this) instanceof Player player)) return;

		if (player.hasEffect(LaLMobEffects.INSTABILITY) || player.hasEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH)) cir.setReturnValue(false);
	}

	@Inject(method = "canCollideWith", at = @At("TAIL"), cancellable = true)
	public void instabilityCollideWith(CallbackInfoReturnable<Boolean> cir) {
		if (!(Entity.class.cast(this) instanceof Player player)) return;

		if (player.hasEffect(LaLMobEffects.INSTABILITY) || player.hasEffect(LaLMobEffects.PROJECTILE_PASSTHROUGH)) cir.setReturnValue(false);
	}

	@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	public void instabilityPush(Entity entity, CallbackInfo ci) {
		Entity entity2 = Entity.class.cast(this);
		if (entity instanceof LivingEntity livingEntity && entity2 instanceof LivingEntity livingEntity2) {
			if (livingEntity.hasEffect(LaLMobEffects.INSTABILITY) || livingEntity2.hasEffect(LaLMobEffects.INSTABILITY)) ci.cancel();
		}
	}

	@Inject(
			method = "teleport",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;teleportCrossDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;"
			)
	)
	public void removePlatformOnDimensionChange(TeleportTransition dimensionTransition, CallbackInfoReturnable<Entity> info, @Local(ordinal = 0) ServerLevel level) {
		if (!(Entity.class.cast(this) instanceof PlatformInterface platformInterface)) return;

		Optional<GlobalPos> globalPos = platformInterface.lal$getLastPlatformPos();
		if (globalPos.isEmpty()) return;

		GlobalPos lastPlatformPos = globalPos.get();
		if (!lastPlatformPos.dimension().equals(level.dimension())) return;
		WandItem.removePlatforms(level, platformInterface, lastPlatformPos.pos());

		if (Entity.class.cast(this) instanceof Player player && player instanceof PlatformInterface platform) {
			platform.setPlatformSummoned(false);
		}
	}

    @Inject(method = "remove", at = @At("HEAD"))
    public void removePlatformOnRemove(Entity.RemovalReason reason, CallbackInfo info) {
        if (!(Entity.class.cast(this) instanceof PlatformInterface platform)) return;
        if (Entity.class.cast(this) instanceof Player) return;

        Optional<GlobalPos> globalPos = platform.lal$getLastPlatformPos();
        if (globalPos.isEmpty()) return;

        GlobalPos lastPlatformPos = globalPos.get();
        Level level = Entity.class.cast(this).level();
        if (!lastPlatformPos.dimension().equals(level.dimension())) return;
		WandItem.removePlatforms(level, platform, lastPlatformPos.pos());

		platform.setPlatformSummoned(false);
    }

    @Inject(method = "setTicksFrozen", at = @At("HEAD"), cancellable = true)
    public void stopUnfreeze(int i, CallbackInfo ci) {
        Entity entity = Entity.class.cast(this);
        if (entity instanceof LivingEntity livingEntity && livingEntity.hasEffect(LaLMobEffects.FREEZING) && livingEntity.canFreeze() && i < this.getTicksFrozen()) ci.cancel();
    }
}
