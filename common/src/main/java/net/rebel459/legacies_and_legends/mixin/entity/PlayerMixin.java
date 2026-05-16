package net.rebel459.legacies_and_legends.mixin.entity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.event.ServerEvents;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.util.PlatformInterface;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.rebel459.legacies_and_legends.util.AccessoryInterface;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.rebel459.unified.platform.UnifiedPlatform;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Player.class)
public abstract class PlayerMixin implements PlatformInterface, AccessoryInterface {

    @Unique
    private static final Codec<HashMap<BlockPos, BlockState>> LAL_STATES_CODEC = Codec.unboundedMap(BlockPos.CODEC, BlockState.CODEC)
            .xmap(HashMap::new, HashMap::new);

    @Unique
    private AccessoryHelper.Mutable mutable;

    @Override
    public AccessoryHelper.Mutable getAccessoryData() {
        if (this.mutable == null) this.mutable = new AccessoryHelper.Mutable();
        return this.mutable;
    }

    @Override
    public void setAccessoryData(AccessoryHelper.Mutable mutable) {
        this.mutable = mutable;
    }

    @Shadow public abstract Inventory getInventory();

    @Shadow public abstract boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource);

    @Unique
    private Optional<GlobalPos> lastPlatformPos = Optional.empty();

    @Unique
    private boolean isPlatformSummoned = false;

    @Override
    public boolean getPlatformSummoned() {
        return this.isPlatformSummoned;
    }

    @Override
    public void setPlatformSummoned(boolean summoned) {
        this.isPlatformSummoned = summoned;
    }

    @Unique
    private HashMap<BlockPos, BlockState> oldStates = new HashMap<>();

    @Unique
    private HashMap<BlockPos, BlockState> platformStates = new HashMap<>();

    @Override
    public HashMap<BlockPos, BlockState> getOldStates() {
        return this.oldStates;
    }

    @Override
    public void setOldStates(HashMap<BlockPos, BlockState> states) {
        this.oldStates = states;
    }

    @Override
    public HashMap<BlockPos, BlockState> getPlatformStates() {
        return this.platformStates;
    }

    @Override
    public void setPlatformStates(HashMap<BlockPos, BlockState> states) {
        this.platformStates = states;
    }

    @Inject(method = "actuallyHurt", at = @At(value = "TAIL"))
    private void cancelTabletUse(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo info) {
        Player player = Player.class.cast(this);
        if (player.getUseItem().is(LaLItemTags.TABLETS)) player.stopUsingItem();
    }

    @Inject(method = "actuallyHurt", at = @At(value = "HEAD"))
    private void damageNecklace(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo info) {
        Player player = Player.class.cast(this);
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (stack.is(LaLItemTags.NECKLACES) && !damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) AccessoryHelper.damageAccessory(player, stack);
    }

    @Inject(method = "actuallyHurt", at = @At(value = "HEAD"))
    private void necklaceOfRegeneration(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo info) {
        Player player = Player.class.cast(this);
        if (AccessoryHelper.getAccessory(player).is(LaLItems.NECKLACE_OF_REGENERATION.get()) && !player.hasEffect(MobEffects.REGENERATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60));
        }
    }

    @Inject(method = "attack", at = @At(value = "TAIL"))
    private void ringOfStriking(Entity target, CallbackInfo ci) {
        Player player = Player.class.cast(this);
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (stack.is(LaLItems.RING_OF_STRIKING.get())) AccessoryHelper.damageAccessory(player, stack);
    }

    @Inject(method = "hurtServer", at = @At(value = "HEAD"), cancellable = true)
    private void amuletOfObsidian(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = Player.class.cast(this);
        if (AccessoryHelper.hasAccessory(player, LaLItems.AMULET_OF_OBSIDIAN.get()) && damageSource.is(DamageTypeTags.IS_FIRE) && !this.isInvulnerableTo(level, damageSource) && !player.hasEffect(MobEffects.FIRE_RESISTANCE) && !player.fireImmune()) {
            if (player.getRemainingFireTicks() > 1) player.setRemainingFireTicks(1);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "HEAD"))
    private void amuletOfAbsorption(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo ci) {
        Player player = Player.class.cast(this);
        if (AccessoryHelper.hasAccessory(player, LaLItems.AMULET_OF_ABSORPTION.get())) {
            AccessoryHelper.damageAccessory(player, AccessoryHelper.getFirst(player, LaLItems.AMULET_OF_ABSORPTION.get()), (int) amount);
        }
    }

    @Inject(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"
            ),
            cancellable = true)
    private void necklaceOfResilience(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo ci) {
        Player player = Player.class.cast(this);
        if (AccessoryHelper.getAccessory(player).is(LaLItems.NECKLACE_OF_RESILIENCE.get())) {
            if (player.getHealth() > 6 && amount > player.getHealth() - 1) amount = player.getHealth() - 1;
            player.setHealth(player.getHealth() - amount);
            if (amount < 3.4028235E37F) {
                player.awardStat(Stats.DAMAGE_TAKEN, Math.round(amount * 10.0F));
            }
            player.gameEvent(GameEvent.ENTITY_DAMAGE);

            ci.cancel();
        }
    }

    @Inject(method = "killedEntity", at = @At(value = "TAIL"))
    private void ringOfHunting(ServerLevel serverLevel, LivingEntity livingEntity, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Player player = Player.class.cast(this);
        ItemStack stack = AccessoryHelper.getAccessory(player);
        if (stack.is(LaLItems.RING_OF_HUNTING.get())) {
            FoodData food = player.getFoodData();
            if (!food.needsFood()) return;
            food.setFoodLevel(food.getFoodLevel() + 2);
            serverLevel.playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1F, 1F);
            AccessoryHelper.damageAccessory(player, stack);
        }
    }

    @Inject(method = "actuallyHurt", at = @At(value = "TAIL"))
    private void activateTotem(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo info) {
        Player player = Player.class.cast(this);
        if (AccessoryHelper.isSlotFilled(player) && player instanceof AccessoryInterface accessory) {
            ItemStack stack = AccessoryHelper.getAccessory(player);
            AccessoryHelper.Mutable mutable = accessory.getAccessoryData();
            if (stack.is(LaLItems.TOTEM_OF_TELEPORTATION.get()) && amount >= player.getHealth()) {
                player.setHealth(1.0F);
                stack.get(DataComponents.DEATH_PROTECTION).applyEffects(stack, player);
                playTotemAnimation(stack, player);
                player.awardStat(Stats.ITEM_USED.get(LaLItems.TOTEM_OF_TELEPORTATION.get()));
                CriteriaTriggers.USED_TOTEM.trigger((ServerPlayer) player, stack);
                stack.copyAndClear();
            }
            if (stack.is(LaLItems.TOTEM_OF_RESURRECTION.get()) && amount >= player.getHealth()) {
                handleTotemOfResurrection(level, player, stack);
            }
            if (stack.is(Items.TOTEM_OF_UNDYING) && amount >= player.getHealth()) {
                player.setHealth(1.0F);
                Items.TOTEM_OF_UNDYING.getDefaultInstance().get(DataComponents.DEATH_PROTECTION).applyEffects(Items.TOTEM_OF_UNDYING.getDefaultInstance(), player);
                playTotemAnimation(Items.TOTEM_OF_UNDYING.getDefaultInstance(), player);
                player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                CriteriaTriggers.USED_TOTEM.trigger((ServerPlayer) player, Items.TOTEM_OF_UNDYING.getDefaultInstance());
                stack.copyAndClear();
            }
            accessory.setAccessoryData(mutable);
        }
        if ((player.getMainHandItem().is(LaLItems.TOTEM_OF_RESURRECTION.get()) || player.getOffhandItem().is(LaLItems.TOTEM_OF_RESURRECTION.get())) && amount >= player.getHealth()) {
            if (player.getMainHandItem().is(LaLItems.TOTEM_OF_RESURRECTION.get())) {
                handleTotemOfResurrection(level, player, player.getMainHandItem());
            }
            else if (player.getOffhandItem().is(LaLItems.TOTEM_OF_RESURRECTION.get())) {
                handleTotemOfResurrection(level, player, player.getOffhandItem());
            }
        }
    }

    @Unique
    private static void handleTotemOfResurrection(Level level, Player player, ItemStack stack) {
        player.setHealth(1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600));
        playTotemAnimation(LaLItems.TOTEM_OF_RESURRECTION.getDefaultInstance(), player);
        player.awardStat(Stats.ITEM_USED.get(LaLItems.TOTEM_OF_RESURRECTION.get()));
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, LaLItems.TOTEM_OF_RESURRECTION.getDefaultInstance());
            player.teleport(serverPlayer.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
            level.playSound(null, player.blockPosition(), LaLSounds.TABLET_TELEPORT.get(), SoundSource.PLAYERS, 0.6F, 1F);
        }
        stack.copyAndClear();
    }

    @Inject(method = "die", at = @At("HEAD"))
    public void destroyPlatformOnDeath(DamageSource damageSource, CallbackInfo info) {
        if (this.lastPlatformPos.isEmpty()) return;

        destroyPlatform();
    }

    @Inject(method = "drop", at = @At("TAIL"))
    public void destroyPlatformOnDrop(ItemStack itemStack, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        if (this.lastPlatformPos.isEmpty()) return;
        if (!itemStack.is(LaLItems.WAND.get())) return;
        if (this.getInventory().contains(LaLItems.WAND.getDefaultInstance())) return;

        destroyPlatform();
    }

    @Unique
    private void destroyPlatform() {
        Player player = Player.class.cast(this);
        GlobalPos globalPos = this.lastPlatformPos.get();
        if (!globalPos.dimension().equals(player.level().dimension())) return;

        BlockPos pos = globalPos.pos();
        WandItem.removePlatforms(player.level, (PlatformInterface)player, pos);

        ((PlatformInterface)player).setPlatformSummoned(false);
    }

    @Override
    public void lal$setLastPlatformPos(@NotNull Level level, BlockPos pos) {
        this.lastPlatformPos = Optional.of(GlobalPos.of(level.dimension(), pos));
    }

    @Override
    public void lal$eraseLastPlatformPos() {
        this.lastPlatformPos = Optional.empty();
    }

    @Override
    public Optional<GlobalPos> lal$getLastPlatformPos() {
        return this.lastPlatformPos;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readPlatformSaveData(ValueInput input, CallbackInfo ci) {
        this.lastPlatformPos = input.read("LalLastPlatformPos", GlobalPos.CODEC);
        this.isPlatformSummoned = input.getBooleanOr("LalPlatformSummoned", false);
        this.oldStates = input.read("LalPlatformOldStates", LAL_STATES_CODEC).orElseGet(HashMap::new);
        this.platformStates = input.read("LalPlatformStates", LAL_STATES_CODEC).orElseGet(HashMap::new);
        if (this.lastPlatformPos.isEmpty()) {
            this.isPlatformSummoned = false;
        } else if (this.isPlatformSummoned && !this.oldStates.isEmpty()) {
            int mainPlatformY = this.lastPlatformPos.get().pos().getY();
            for (BlockPos pos : this.oldStates.keySet()) {
                if (pos.getY() <= mainPlatformY && !this.platformStates.containsKey(pos)) {
                    this.platformStates.put(pos.immutable(), LaLBlocks.WAND_PLATFORM.get().defaultBlockState());
                }
            }
        }
        Player player = Player.class.cast(this);
        Level level = player.level();
        if (level.isClientSide()) return;
        for (BlockPos pos : this.oldStates.keySet()) {
            ServerEvents.cancelBlockChange(level.dimension(), pos);
        }
        for (BlockPos pos : this.platformStates.keySet()) {
            ServerEvents.cancelBlockChange(level.dimension(), pos);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        this.lastPlatformPos.ifPresent(pos -> output.store("LalLastPlatformPos", GlobalPos.CODEC, pos));
        output.putBoolean("LalPlatformSummoned", this.isPlatformSummoned);
        if (!this.oldStates.isEmpty()) {
            output.store("LalPlatformOldStates", LAL_STATES_CODEC, this.oldStates);
        }
        if (!this.platformStates.isEmpty()) {
            output.store("LalPlatformStates", LAL_STATES_CODEC, this.platformStates);
        }
    }

    @Unique
    private int intervalTick = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    public void accessoryTick(CallbackInfo ci) {
        Player player = Player.class.cast(this);

        AccessoryHelper.Mutable mutable = this.getAccessoryData();

        Set<ItemStack> accessories = AccessoryHelper.getAllAccessories(player);
        Set<Item> tickedItems = new HashSet<>();
        for (ItemStack stack : accessories) {
            Item item = stack.getItem();
            if (!tickedItems.contains(item)) {
                stack = AccessoryHelper.getFirst(player, item);
                mutable.onTick(player, stack);
                tickedItems.add(item);
                if (this.intervalTick >= 5) {
                    mutable.getAndApplyModifiers(player, stack);
                    this.intervalTick = 0;
                }
            }
            mutable.onTickAmulet(player, stack);
        }
        this.intervalTick++;
        this.setAccessoryData(mutable);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addAccessory(ValueOutput output, CallbackInfo ci) {
        Player player = Player.class.cast(this);
        ItemStack stack = AccessoryHelper.getActualAccessory(player);
        if (!stack.isEmpty()) {
            output.store(LaLConstants.string("accessory"), ItemStack.CODEC, stack);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAccessory(ValueInput input, CallbackInfo ci) {
        Player player = Player.class.cast(this);
        input.read(LaLConstants.string("accessory"), ItemStack.CODEC).ifPresentOrElse(stack -> {
            AccessoryHelper.setAccessory(player, stack);
        }, () -> AccessoryHelper.setAccessory(player, ItemStack.EMPTY));
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void dropAccessory(ServerLevel serverLevel, CallbackInfo ci) {
        Player player = Player.class.cast(this);
        ItemStack stack = AccessoryHelper.getActualAccessory(player);
        if (!serverLevel.getGameRules().get(GameRules.KEEP_INVENTORY) && !(AccessoryHelper.getActualAccessory(player) == ItemStack.EMPTY)) player.drop(stack, true, false);
    }

    @Unique
    private static void playTotemAnimation(ItemStack itemStack, Entity entity) {
        if (UnifiedPlatform.isServerSide()) return;
        Minecraft minecraftClient = Minecraft.getInstance();
        minecraftClient.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);

        ClientLevel clientWorld = minecraftClient.level;

        if (clientWorld != null) {
            clientWorld.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0f, 1.0f, false);
        }

        minecraftClient.gameRenderer.displayItemActivation(itemStack);
    }
}
