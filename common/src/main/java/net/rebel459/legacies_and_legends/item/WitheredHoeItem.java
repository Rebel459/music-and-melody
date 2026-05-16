package net.rebel459.legacies_and_legends.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WitheredHoeItem extends Item {
    protected static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = Maps.newHashMap(
            ImmutableMap.of(
                    Blocks.WARPED_NYLIUM,
                    Pair.of(HoeItem::onlyIfAirAbove, HoeItem.changeIntoState(Blocks.NETHERRACK.defaultBlockState())),
                    Blocks.CRIMSON_NYLIUM,
                    Pair.of(HoeItem::onlyIfAirAbove, HoeItem.changeIntoState(Blocks.NETHERRACK.defaultBlockState())),
                    Blocks.NETHERRACK,
                    Pair.of(HoeItem::onlyIfAirAbove, HoeItem.changeIntoState(Blocks.SOUL_SOIL.defaultBlockState())),
                    Blocks.SOUL_SOIL,
                    Pair.of(HoeItem::onlyIfAirAbove, HoeItem.changeIntoState(Blocks.SOUL_SAND.defaultBlockState()))
            )
    );

    static {
        TILLABLES.putAll(HoeItem.TILLABLES);
    }

    public WitheredHoeItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties) {
        super(properties.hoe(material, attackDamage, attackSpeed));
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = TILLABLES.get(level.getBlockState(blockPos).getBlock());
        if (pair == null) return InteractionResult.PASS;

        Predicate<UseOnContext> predicate = pair.getFirst();
        Consumer<UseOnContext> consumer = pair.getSecond();
        if (predicate.test(context)) {
            Player player = context.getPlayer();
            level.playSound(player, blockPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1F, 1F);
            if (!level.isClientSide()) {
                consumer.accept(context);
                if (player != null) context.getItemInHand().hurtAndBreak(1, player, context.getHand());
            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void postHurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), attacker);
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }
}
