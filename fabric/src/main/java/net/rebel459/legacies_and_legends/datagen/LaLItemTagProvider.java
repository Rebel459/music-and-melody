package net.rebel459.legacies_and_legends.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.rebel459.item_tooltips.tag.ITItemTags;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.rebel459.unified.tag.UnifiedItemTags;
import net.rebel459.unified.util.registry.SuppliedItem;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class LaLItemTagProvider extends FabricTagsProvider.ItemTagsProvider {

    public LaLItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    private TagKey<Item> getTag(String namespace, String path) {
        return TagKey.create(this.registryKey, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private ResourceKey<Item> getKey(String namespace, String path) {
        return ResourceKey.create(this.registryKey, Identifier.fromNamespaceAndPath(namespace, path));
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider wrapperLookup) {
        this.valueLookupBuilder(ItemTags.BEACON_PAYMENT_ITEMS)
                .add(LaLItems.SAPPHIRE.get());

        this.valueLookupBuilder(ItemTags.DECORATED_POT_SHERDS)
                .add(LaLItems.VERDANT_POTTERY_SHERD.get())
                .add(LaLItems.FORAGER_POTTERY_SHERD.get())
                .add(LaLItems.HARVEST_POTTERY_SHERD.get())
                .add(LaLItems.DUSK_POTTERY_SHERD.get());

        for (SuppliedItem item : LaLItems.GEMS) {
            this.valueLookupBuilder(LaLItemTags.GEMS)
                    .add(item.get());
        }

        this.valueLookupBuilder(LaLItemTags.TABLETS)
                .add(LaLItems.TABLET_OF_HASTE.get())
                .add(LaLItems.TABLET_OF_INSTABILITY.get())
                .add(LaLItems.TABLET_OF_WARPING.get())
                .add(LaLItems.TABLET_OF_RECALL.get())
                .add(LaLItems.TABLET_OF_DEAFENING.get())
                .add(LaLItems.TABLET_OF_CHANNELING.get())
                .add(LaLItems.TABLET_OF_REVEALING.get());

        this.valueLookupBuilder(LaLItemTags.RINGS)
                .add(LaLItems.RING_OF_EVASION.get())
                .add(LaLItems.RING_OF_HUNTING.get())
                .add(LaLItems.RING_OF_EXCAVATION.get())
                .add(LaLItems.RING_OF_CONSTRUCTION.get())
                .add(LaLItems.RING_OF_RESTORATION.get())
                .add(LaLItems.RING_OF_STRIKING.get())
                .add(LaLItems.RING_OF_ARCHERY.get());

        this.valueLookupBuilder(LaLItemTags.NECKLACES)
                .add(LaLItems.NECKLACE_OF_ISOLATION.get())
                .add(LaLItems.NECKLACE_OF_BARTERING.get())
                .add(LaLItems.NECKLACE_OF_LEAPING.get())
                .add(LaLItems.NECKLACE_OF_PROTECTION.get())
                .add(LaLItems.NECKLACE_OF_PURITY.get())
                .add(LaLItems.NECKLACE_OF_REGENERATION.get())
                .add(LaLItems.NECKLACE_OF_RESILIENCE.get());

        this.valueLookupBuilder(LaLItemTags.AMULETS)
                .add(LaLItems.AMULET_OF_ABSORPTION.get())
                .add(LaLItems.AMULET_OF_OBSIDIAN.get())
                .add(LaLItems.AMULET_OF_DEFLECTION.get());

        this.valueLookupBuilder(LaLItemTags.TOTEMS)
                .add(Items.TOTEM_OF_UNDYING)
                .add(LaLItems.TOTEM_OF_TELEPORTATION.get())
                .add(LaLItems.TOTEM_OF_RESURRECTION.get())
                .addOptionalTag(getTag("friendsandfoes","totems"));

        this.valueLookupBuilder(LaLItemTags.ARTIFACTS)
                .add(Items.TURTLE_HELMET)
                .add(LaLItems.REINFORCED_CHESTPLATE.get())
                .add(LaLItems.TRAVELLING_STRIDES.get())
                .add(LaLItems.WANDERER_BOOTS.get())
                .add(LaLItems.VERDANT_SWORD.get())
                .add(LaLItems.CLEAVING_BATTLEAXE.get())
                .add(LaLItems.MOLTEN_PICKAXE.get())
                .add(LaLItems.PROSPECTOR_SHOVEL.get())
                .add(LaLItems.WITHERED_HOE.get())
                .add(LaLItems.FROSTED_SPEAR.get())
                .addTag(LaLItemTags.TABLETS)
                .addTag(LaLItemTags.TOTEMS);

        this.valueLookupBuilder(LaLItemTags.ACCESSORIES)
                .addTag(LaLItemTags.RINGS)
                .addTag(LaLItemTags.NECKLACES)
                .addTag(LaLItemTags.AMULETS)
                .addTag(LaLItemTags.TOTEMS);

        this.valueLookupBuilder(ITItemTags.HAS_DESCRIPTION)
                .addTag(LaLItemTags.ARTIFACTS)
                .addTag(LaLItemTags.ACCESSORIES)
                .add(LaLBlocks.JEWELING_TABLE.asItem());

        this.valueLookupBuilder(LaLItemTags.HAS_USE_EFFECT)
                .add(LaLItems.TABLET_OF_CHANNELING.get())
                .add(LaLItems.TABLET_OF_DEAFENING.get())
                .add(LaLItems.TABLET_OF_REVEALING.get());

        this.valueLookupBuilder(LaLItemTags.CHILLING)
                .add(LaLItems.FROSTED_SPEAR.get());
        this.valueLookupBuilder(LaLItemTags.PROSPECTING)
                .add(LaLItems.PROSPECTOR_SHOVEL.get());

        this.valueLookupBuilder(LaLItemTags.REPAIRS_REINFORCED_ARMOR)
                .add(Items.ECHO_SHARD);
        this.valueLookupBuilder(LaLItemTags.REPAIRS_TRAVELLING_ARMOR)
                .add(Items.RABBIT_HIDE);
        this.valueLookupBuilder(LaLItemTags.REPAIRS_WANDERER_ARMOR)
                .add(LaLItems.METAL_CHUNK.get());

        this.valueLookupBuilder(LaLItemTags.BOOMERANG_REPAIR_MATERIALS)
                .add(LaLItems.METAL_CHUNK.get());
        this.valueLookupBuilder(LaLItemTags.WAND_REPAIR_MATERIALS)
                .add(Items.GOLD_INGOT);
        this.valueLookupBuilder(LaLItemTags.HOOK_REPAIR_MATERIALS)
                .add(LaLItems.METAL_CHUNK.get());
        this.valueLookupBuilder(LaLItemTags.KNIFE_REPAIR_MATERIALS)
                .addTag(ItemTags.DECORATED_POT_SHERDS);

        this.valueLookupBuilder(LaLItemTags.TRIDENT_REPAIR_MATERIALS)
                .add(LaLItems.TRIDENT_SHARD.get());

        this.valueLookupBuilder(LaLItemTags.VERDANT_TOOL_MATERIALS)
                .add(Items.MOSSY_COBBLESTONE.asItem());
        this.valueLookupBuilder(LaLItemTags.CLEAVING_TOOL_MATERIALS)
                .add(LaLItems.METAL_CHUNK.get());
        this.valueLookupBuilder(LaLItemTags.MOLTEN_TOOL_MATERIALS)
                .add(Items.NETHER_BRICK);
        this.valueLookupBuilder(LaLItemTags.PROSPECTOR_TOOL_MATERIALS)
                .add(Items.EMERALD);
        this.valueLookupBuilder(LaLItemTags.WITHERED_TOOL_MATERIALS)
                .add(Blocks.BLACKSTONE.asItem());
        this.builder(LaLItemTags.FROSTED_TOOL_MATERIALS)
                .addOptional(getKey("enchants_and_expeditions", "ice_shard"));
        this.valueLookupBuilder(LaLItemTags.FROSTED_TOOL_MATERIALS_FALLBACK)
                .add(Blocks.PACKED_ICE.asItem());

        this.valueLookupBuilder(LaLItemTags.HUNTING_RING_MATERIALS)
                .add(Items.QUARTZ);
        this.valueLookupBuilder(LaLItemTags.EVASION_RING_MATERIALS)
                .add(LaLItems.SAPPHIRE.get());
        this.valueLookupBuilder(LaLItemTags.CONSTRUCTION_RING_MATERIALS)
                .add(Items.IRON_INGOT);
        this.valueLookupBuilder(LaLItemTags.STRIKING_RING_MATERIALS)
                .add(Items.COPPER_INGOT);
        this.valueLookupBuilder(LaLItemTags.ARCHERY_RING_MATERIALS)
                .add(Items.DIAMOND);
        this.valueLookupBuilder(LaLItemTags.EXCAVATION_RING_MATERIALS)
                .add(Items.EMERALD);
        this.valueLookupBuilder(LaLItemTags.RESTORATION_RING_MATERIALS)
                .add(Items.REDSTONE);

        this.valueLookupBuilder(LaLItemTags.ISOLATION_NECKLACE_MATERIALS)
                .add(Items.AMETHYST_SHARD);
        this.valueLookupBuilder(LaLItemTags.PURITY_NECKLACE_MATERIALS)
                .add(Items.COPPER_INGOT);
        this.valueLookupBuilder(LaLItemTags.LEAPING_NECKLACE_MATERIALS)
                .add(Items.IRON_INGOT);
        this.valueLookupBuilder(LaLItemTags.PROTECTION_NECKLACE_MATERIALS)
                .add(Items.IRON_INGOT);
        this.valueLookupBuilder(LaLItemTags.RESILIENCE_NECKLACE_MATERIALS)
                .add(Items.IRON_INGOT);
        this.builder(LaLItemTags.REGENERATION_NECKLACE_MATERIALS)
                .addOptional(getKey("progression_reborn", "rose_ingot"));
        this.valueLookupBuilder(LaLItemTags.REGENERATION_NECKLACE_MATERIALS_FALLBACK)
                .addTag(LaLItemTags.REGENERATION_NECKLACE_MATERIALS)
                .add(Items.COPPER_INGOT);
        this.valueLookupBuilder(LaLItemTags.BARTERING_NECKLACE_MATERIALS)
                .add(Items.GOLD_INGOT);

        this.valueLookupBuilder(LaLItemTags.SAPPHIRE_ORES)
                .add(LaLBlocks.SAPPHIRE_ORE.asItem())
                .add(LaLBlocks.DEEPSLATE_SAPPHIRE_ORE.asItem());

        this.valueLookupBuilder(ItemTags.CHEST_ARMOR)
                .add(LaLItems.REINFORCED_CHESTPLATE.get());
        this.valueLookupBuilder(ItemTags.LEG_ARMOR)
                .add(LaLItems.TRAVELLING_STRIDES.get());
        this.valueLookupBuilder(ItemTags.FOOT_ARMOR)
                .add(LaLItems.WANDERER_BOOTS.get());

        this.valueLookupBuilder(ItemTags.SWORDS)
                .add(LaLItems.VERDANT_SWORD.get());
        this.valueLookupBuilder(ItemTags.AXES)
                .add(LaLItems.CLEAVING_BATTLEAXE.get());
        this.valueLookupBuilder(ItemTags.PICKAXES)
                .add(LaLItems.MOLTEN_PICKAXE.get());
        this.valueLookupBuilder(ItemTags.SHOVELS)
                .add(LaLItems.PROSPECTOR_SHOVEL.get());
        this.valueLookupBuilder(ItemTags.HOES)
                .add(LaLItems.WITHERED_HOE.get());
        this.valueLookupBuilder(ItemTags.SPEARS)
                .add(LaLItems.FROSTED_SPEAR.get());

        this.valueLookupBuilder(ItemTags.WEAPON_ENCHANTABLE)
                .add(LaLItems.HOOK.get());

        this.valueLookupBuilder(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                .add(LaLItems.KNIFE.get());

        this.valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .add(LaLItems.BOOMERANG.get())
                .add(LaLItems.HOOK.get())
                .add(LaLItems.KNIFE.get());

        this.valueLookupBuilder(ItemTags.TRIM_MATERIALS)
                .add(Items.ECHO_SHARD)
                .add(LaLItems.SAPPHIRE.get());

        this.valueLookupBuilder(LaLItemTags.VOID_IMMUNE)
                .add(LaLItems.TIMELOST_GEM.get());

        this.valueLookupBuilder(UnifiedItemTags.PERSISTENT_COOLDOWNS)
                .add(LaLItems.BOOMERANG.get())
                .add(LaLItems.WAND.get())
                .addTag(LaLItemTags.TABLETS);
    }
}
