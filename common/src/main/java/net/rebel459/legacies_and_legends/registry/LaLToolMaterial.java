package net.rebel459.legacies_and_legends.registry;

import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

public record LaLToolMaterial(TagKey<Block> incorrectBlocksForDrops, int durability, float speed, float attackDamageBonus, int enchantmentValue, TagKey<Item> repairItems) {
    public static final ToolMaterial HOOK = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 5.0F, 15, LaLItemTags.HOOK_REPAIR_MATERIALS);
    public static final ToolMaterial KNIFE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 5.0F, 3.0F, 15, LaLItemTags.KNIFE_REPAIR_MATERIALS);

    public static final ToolMaterial VERDANT = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 384, 4.0F, 1.0F, 10,LaLItemTags.VERDANT_TOOL_MATERIALS);
    public static final ToolMaterial CLEAVING = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 432, 6.0F, 2.0F, 12, LaLItemTags.CLEAVING_TOOL_MATERIALS);
    public static final ToolMaterial MOLTEN = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 501, 7.0F, 2.0F, 15, LaLItemTags.MOLTEN_TOOL_MATERIALS);
    public static final ToolMaterial PROSPECTOR = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 97, 10.0F, 2.0F, 18, LaLItemTags.PROSPECTOR_TOOL_MATERIALS);
    public static final ToolMaterial WITHERED = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 215, 6.0F, 2.0F, 8, LaLItemTags.WITHERED_TOOL_MATERIALS);
    public static final ToolMaterial FROSTED = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 308, 4.0F, 1.0F, 17, LaLItemTags.FROSTED_TOOL_MATERIALS_FALLBACK);
}