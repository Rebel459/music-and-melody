package net.rebel459.legacies_and_legends.tag;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class LaLItemTags {
    public static final TagKey<Item> BOOMERANG_REPAIR_MATERIALS = bind("boomerang_repair_materials");
    public static final TagKey<Item> WAND_REPAIR_MATERIALS = bind("wand_repair_materials");
    public static final TagKey<Item> HOOK_REPAIR_MATERIALS = bind("hook_repair_materials");
    public static final TagKey<Item> KNIFE_REPAIR_MATERIALS = bind("knife_repair_materials");

    public static final TagKey<Item> TRIDENT_REPAIR_MATERIALS = bind("trident_repair_materials");

    public static final TagKey<Item> VERDANT_TOOL_MATERIALS = bind("verdant_tool_materials");
    public static final TagKey<Item> CLEAVING_TOOL_MATERIALS = bind("cleaving_tool_materials");
    public static final TagKey<Item> MOLTEN_TOOL_MATERIALS = bind("molten_tool_materials");
    public static final TagKey<Item> PROSPECTOR_TOOL_MATERIALS = bind("prospector_tool_materials");
    public static final TagKey<Item> WITHERED_TOOL_MATERIALS = bind("withered_tool_materials");
    public static final TagKey<Item> FROSTED_TOOL_MATERIALS = bind("frosted_tool_materials");
    public static final TagKey<Item> FROSTED_TOOL_MATERIALS_FALLBACK = bind("frosted_tool_materials_fallback");

    public static final TagKey<Item> HUNTING_RING_MATERIALS = bind("hunting_ring_materials");
    public static final TagKey<Item> EVASION_RING_MATERIALS = bind("evasion_ring_materials");
    public static final TagKey<Item> CONSTRUCTION_RING_MATERIALS = bind("construction_ring_materials");
    public static final TagKey<Item> STRIKING_RING_MATERIALS = bind("striking_ring_materials");
    public static final TagKey<Item> ARCHERY_RING_MATERIALS = bind("archery_ring_materials");
    public static final TagKey<Item> EXCAVATION_RING_MATERIALS = bind("excavation_ring_materials");
    public static final TagKey<Item> RESTORATION_RING_MATERIALS = bind("restoration_ring_materials");

    public static final TagKey<Item> ISOLATION_NECKLACE_MATERIALS = bind("isolation_necklace_materials");
    public static final TagKey<Item> PURITY_NECKLACE_MATERIALS = bind("purity_necklace_materials");
    public static final TagKey<Item> LEAPING_NECKLACE_MATERIALS = bind("leaping_necklace_materials");
    public static final TagKey<Item> PROTECTION_NECKLACE_MATERIALS = bind("protection_necklace_materials");
    public static final TagKey<Item> RESILIENCE_NECKLACE_MATERIALS = bind("resilience_necklace_materials");
    public static final TagKey<Item> REGENERATION_NECKLACE_MATERIALS = bind("regeneration_necklace_materials");
    public static final TagKey<Item> REGENERATION_NECKLACE_MATERIALS_FALLBACK = bind("regeneration_necklace_materials_fallback");
    public static final TagKey<Item> BARTERING_NECKLACE_MATERIALS = bind("bartering_necklace_materials");

    public static final TagKey<Item> REPAIRS_WANDERER_ARMOR = bind("repairs_wanderer_armor");
    public static final TagKey<Item> REPAIRS_TRAVELLING_ARMOR = bind("repairs_travelling_armor");
    public static final TagKey<Item> REPAIRS_REINFORCED_ARMOR = bind("repairs_reinforced_armor");

    public static final TagKey<Item> TABLETS = bind("tablets");

    public static final TagKey<Item> GEMS = bind("gems");

    public static final TagKey<Item> RINGS = bind("rings");
    public static final TagKey<Item> NECKLACES = bind("necklaces");
    public static final TagKey<Item> AMULETS = bind("amulets");
    public static final TagKey<Item> TOTEMS = bind("totems");

    public static final TagKey<Item> ACCESSORIES = bind("accessories");
    public static final TagKey<Item> ARTIFACTS = bind("artifacts");

    public static final TagKey<Item> HAS_USE_EFFECT = bind("has_use_effect");

    public static final TagKey<Item> CHILLING = bind("chilling");
    public static final TagKey<Item> PROSPECTING = bind("prospecting");

    public static final TagKey<Item> SAPPHIRE_ORES = bind("sapphire_ores");

    public static final TagKey<Item> VARIABLE_REPAIRABILITY = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchants_and_expeditions", "variable_repairability"));
    public static final TagKey<Item> VOID_IMMUNE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("end_reborn", "void_immune"));

    @NotNull
    private static TagKey<Item> bind(@NotNull String path) {
        return TagKey.create(Registries.ITEM, LaLConstants.id(path));
    }

}