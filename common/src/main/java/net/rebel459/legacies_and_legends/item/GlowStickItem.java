package net.rebel459.legacies_and_legends.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.rebel459.legacies_and_legends.entity.GlowStickProjectile;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.unified.util.registry.SuppliedBlock;

import java.util.function.Supplier;

public class GlowStickItem extends BlockItem implements ProjectileItem {
    public static final float PROJECTILE_SHOOT_POWER = 1F;

    public GlowStickItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = super.use(level, player, hand);
        if (result == InteractionResult.SUCCESS) return result;
        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(GlowStickProjectile::new, serverLevel, itemStack, player, 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        itemStack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
        return new GlowStickProjectile(level, position.x(), position.y(), position.z(), itemStack);
    }
}