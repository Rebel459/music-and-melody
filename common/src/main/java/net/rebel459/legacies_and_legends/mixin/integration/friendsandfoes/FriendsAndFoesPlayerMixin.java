package net.rebel459.legacies_and_legends.mixin.integration.friendsandfoes;

import net.rebel459.legacies_and_legends.friendsandfoes.FriendsAndFoesTotemUtil;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class FriendsAndFoesPlayerMixin {
    @Unique
    private static final Identifier TOTEM_OF_FREEZING_ID = Identifier.fromNamespaceAndPath("friendsandfoes", "totem_of_freezing");
    @Unique
    private static final Identifier TOTEM_OF_ILLUSION_ID = Identifier.fromNamespaceAndPath("friendsandfoes", "totem_of_illusion");
    @Unique
    private static final TagKey<Item> TOTEMS_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("friendsandfoes", "totems"));

    @Inject(method = "actuallyHurt", at = @At(value = "TAIL"))
    private void activateTotem(ServerLevel level, DamageSource damageSource, float amount, CallbackInfo info) {
        Player player = Player.class.cast(this);
        if (AccessoryHelper.isSlotFilled(player)) {
            ItemStack stack = AccessoryHelper.getAccessory(player);
            if (isTotem(stack, TOTEM_OF_FREEZING_ID) && player.getHealth() <= player.getMaxHealth() / 2) {
                invokeTotemAction("freezeEntities", player, level);
                handleTotem(player, stack, getParticle(TOTEM_OF_FREEZING_ID));
                return;
            }
            if (isTotem(stack, TOTEM_OF_ILLUSION_ID) && player.getHealth() <= player.getMaxHealth() / 2) {
                invokeTotemAction("createIllusions", player, level);
                handleTotem(player, stack, getParticle(TOTEM_OF_ILLUSION_ID));
                return;
            }
        }
        if ((player.getMainHandItem().is(TOTEMS_TAG) || player.getOffhandItem().is(TOTEMS_TAG)) && player.getHealth() <= player.getMaxHealth() / 2) {
            if (isTotem(player.getMainHandItem(), TOTEM_OF_FREEZING_ID)) {
                ItemStack stack = player.getItemBySlot(EquipmentSlot.MAINHAND);
                invokeTotemAction("freezeEntities", player, level);
                handleTotem(player, stack, getParticle(TOTEM_OF_FREEZING_ID));
            }
            else if (isTotem(player.getMainHandItem(), TOTEM_OF_ILLUSION_ID)) {
                ItemStack stack = player.getItemBySlot(EquipmentSlot.MAINHAND);
                invokeTotemAction("createIllusions", player, level);
                handleTotem(player, stack, getParticle(TOTEM_OF_ILLUSION_ID));
            }
            else if (isTotem(player.getOffhandItem(), TOTEM_OF_FREEZING_ID)) {
                ItemStack stack = player.getItemBySlot(EquipmentSlot.OFFHAND);
                invokeTotemAction("freezeEntities", player, level);
                handleTotem(player, stack, getParticle(TOTEM_OF_FREEZING_ID));
            }
            else if (isTotem(player.getOffhandItem(), TOTEM_OF_ILLUSION_ID)) {
                ItemStack stack = player.getItemBySlot(EquipmentSlot.OFFHAND);
                invokeTotemAction("createIllusions", player, level);
                handleTotem(player, stack, getParticle(TOTEM_OF_ILLUSION_ID));
            }
        }
    }

    @Unique
    private static void handleTotem(Player player, ItemStack stack, SimpleParticleType particle) {
        if (particle != null) {
            FriendsAndFoesTotemUtil.playActivateAnimation(player, particle);
        }
        FriendsAndFoesTotemUtil.playActivateAnimationOnly(stack);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, stack);
        AccessoryHelper.clearAccessory(player);
    }

    @Unique
    private static boolean isTotem(ItemStack stack, Identifier id) {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item != null && stack.is(item);
    }

    @Unique
    private static SimpleParticleType getParticle(Identifier id) {
        return BuiltInRegistries.PARTICLE_TYPE.getValue(id) instanceof SimpleParticleType particle ? particle : null;
    }

    @Unique
    private static void invokeTotemAction(String action, Player player, ServerLevel level) {
        try {
            Class<?> totemUtil = Class.forName("com.faboslav.friendsandfoes.common.util.TotemUtil");
            totemUtil.getMethod(action, Player.class, ServerLevel.class).invoke(null, player, level);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
