package net.rebel459.legacies_and_legends.mixin.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.rebel459.item_tooltips.config.ITConfig;
import net.rebel459.item_tooltips.util.ScreenHelper;
import net.rebel459.legacies_and_legends.LegaciesAndLegends;
import net.rebel459.legacies_and_legends.client.JewelingScreen;
import net.rebel459.legacies_and_legends.item.WandItem;
import net.rebel459.legacies_and_legends.registry.LaLDataComponents;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.AccessoryHelper;
import net.rebel459.legacies_and_legends.util.Gem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.function.Consumer;

@Mixin(value = ItemStack.class, priority = 500)
public abstract class ItemStackMixin {

    @Inject(at = @At("TAIL"), method = "addDetailsToTooltip")
    private void accessoryTooltips(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> consumer, CallbackInfo ci) {
        ItemStack stack = ItemStack.class.cast(this);
        if (!stack.is(LaLItemTags.ACCESSORIES)) return;
        if ((stack.is(LaLItemTags.RINGS) || stack.is(LaLItemTags.NECKLACES)) && !stack.has(LaLDataComponents.VARIABLE_DURABILITY.get())) stack.set(LaLDataComponents.VARIABLE_DURABILITY.get(), stack.getMaxDamage());
        if (stack.has(DataComponents.MAX_DAMAGE) && stack.has(LaLDataComponents.VARIABLE_DURABILITY.get())) {
            int durability = stack.getMaxDamage();
            int averageDurability = stack.get(LaLDataComponents.VARIABLE_DURABILITY.get());
            ChatFormatting color = ChatFormatting.DARK_GREEN;
            if (durability >= averageDurability * 1.3F) color = ChatFormatting.GREEN;
            if (durability <= averageDurability * 0.7F) color = ChatFormatting.YELLOW;
            if (durability <= averageDurability * 0.55F) color = ChatFormatting.RED;
            consumer.accept(Component.translatable("tooltip.legacies_and_legends.max_durability").append(": ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(String.valueOf(durability)).withStyle(color)));
        }
        if (stack.has(DataComponents.ENCHANTABLE) && LegaciesAndLegends.isEnchantsAndExpeditionsLoaded() && stack.is(LaLItemTags.VARIABLE_REPAIRABILITY)) {
            int repairability = Math.min(stack.get(DataComponents.ENCHANTABLE).value(), 25);
            int averageRepairability = 23;
            ChatFormatting color = ChatFormatting.DARK_GREEN;
            if (stack.has(DataComponents.RARITY)) {
                var rarity = stack.get(DataComponents.RARITY);
                if (rarity == Rarity.UNCOMMON) averageRepairability = 18;
                if (rarity == Rarity.RARE) averageRepairability = 13;
                if (rarity == Rarity.EPIC) averageRepairability = 8;
            }
            if (repairability >= averageRepairability + 4) color = ChatFormatting.GREEN;
            if (repairability <= averageRepairability - 4) color = ChatFormatting.YELLOW;
            if (repairability <= averageRepairability - 7) color = ChatFormatting.RED;
            consumer.accept(Component.translatable("tooltip.legacies_and_legends.repair_cost").append(": ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(String.valueOf(26 - repairability)).withStyle(color)));
        }
        if (AccessoryHelper.isBroken(stack)) {
            consumer.accept(Component.translatable("tooltip.legacies_and_legends.broken").withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Inject(at = @At("TAIL"), method = "addDetailsToTooltip")
    private void gemTooltips(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> consumer, CallbackInfo ci) {
        ItemStack stack = ItemStack.class.cast(this);
        if (stack.getItem() instanceof WandItem || stack.has(LaLDataComponents.WAND_SLOTS.get())) {
            Gem.Slots gems = WandItem.getGems(stack);
            createTooltip(consumer, gems.primary(), true);
            createTooltip(consumer, gems.secondary(), false);
        }
        if (stack.has(LaLDataComponents.GEM.get())) {
            createTooltip(consumer, stack.get(LaLDataComponents.GEM.get()), true);
        }
    }

    @Unique
    private void createTooltip(Consumer<Component> consumer, Gem gem, boolean bonus) {
        if (gem == Gem.EMPTY) return;
        MutableComponent prefixText = Component.translatable(ITConfig.get().enchantments.prefix.text).withColor(ITConfig.get().enchantments.prefix.color);
        int descriptionColor = ITConfig.get().enchantments.color;
        String path = "gem.legacies_and_legends." + gem.getSerializedName();
        consumer.accept(Component.translatable(path).withColor(gem.color()));
        Minecraft client = Minecraft.getInstance();
        boolean showDescriptions = ScreenHelper.Tooltip.hasKeyDown();
        if (!showDescriptions && client.screen instanceof JewelingScreen screen) {
            ItemStack stack = ItemStack.class.cast(this);
            NonNullList<Slot> slots = screen.getMenu().slots;
            if (slots.get(0).getItem() == stack || slots.get(1).getItem() == stack ||  slots.get(2).getItem() == stack) showDescriptions = true;
        };
        if (!showDescriptions) return;
        consumer.accept(Component.literal("")
                .append(prefixText)
                .append(Component.translatable(path + ".desc").withColor(descriptionColor)));
        if (!bonus) return;
        consumer.accept(Component.literal("")
                .append(prefixText)
                .append(Component.literal("+ ").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable(path + ".desc.bonus").withColor(descriptionColor)));
    }
}
