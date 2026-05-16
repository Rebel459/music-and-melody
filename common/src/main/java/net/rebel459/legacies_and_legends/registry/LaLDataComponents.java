package net.rebel459.legacies_and_legends.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Repairable;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.LegaciesAndLegends;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.legacies_and_legends.item.VerdantSwordItem;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.Gem;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.util.registry.Supplied;

import java.util.function.Supplier;

public class LaLDataComponents {

    public static void init(){
        UnifiedEvents.DefaultDataComponents.modify((item, builder, provider) -> {
            if (!LegaciesAndLegends.isCombatRebornLoaded) {
                if (item == Items.TRIDENT) {
                    builder.set(DataComponents.ATTRIBUTE_MODIFIERS, TridentItem.createAttributes());
                    builder.set(DataComponents.REPAIRABLE, new Repairable(provider.lookup(Registries.ITEM).get().getOrThrow(LaLItemTags.TRIDENT_REPAIR_MATERIALS)));
                }
            }
            if (!LegaciesAndLegends.isProgressionRebornLoaded) {
                if (item == LaLItems.NECKLACE_OF_REGENERATION.get()) {
                    builder.set(DataComponents.REPAIRABLE, new Repairable(provider.lookup(Registries.ITEM).get().getOrThrow(LaLItemTags.REGENERATION_NECKLACE_MATERIALS_FALLBACK)));
                }
            }
            if (LaLConfig.get().misc.stackable_saddles) {
                if ((item == Items.SADDLE || item.builtInRegistryHolder().is(ItemTags.HARNESSES))) {
                    builder.set(DataComponents.MAX_STACK_SIZE, 16);
                }
            }
            if (LaLConfig.get().misc.echo_shard_trim) {
                if (item == Items.ECHO_SHARD) {
                    builder.set(DataComponents.PROVIDES_TRIM_MATERIAL, provider.lookup(Registries.TRIM_MATERIAL).get().getOrThrow(LaLTrimMaterials.ECHO));
                }
            }
            if (item == LaLItems.CLEAVING_BATTLEAXE.get()) {
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, VerdantSwordItem.createAttributes());
            }
        });
    }

    public static UnifiedRegistries.DataComponentTypes COMPONENTS = UnifiedRegistries.DataComponentTypes.create(LaLConstants.MOD_ID);

    public static final Supplied<DataComponentType<String>> LORE_BOOK = COMPONENTS.register(
            "lore_book", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
    );

    public static final Supplied<DataComponentType<Integer>> VARIABLE_DURABILITY = COMPONENTS.register(
            "variable_durability", builder -> builder.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static final Supplied<DataComponentType<Gem>> GEM = COMPONENTS.register(
            "gem", builder -> builder.persistent(Gem.CODEC).networkSynchronized(Gem.STREAM_CODEC)
    );
    public static final Supplied<DataComponentType<Gem.Slots>> WAND_SLOTS = COMPONENTS.register(
            "wand_slots", builder -> builder.persistent(Gem.Slots.CODEC).networkSynchronized(Gem.Slots.STREAM_CODEC)
    );
}