package net.rebel459.legacies_and_legends.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class HookItem extends Item {

    public HookItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties) {
        super(material.applySwordProperties(properties, attackDamage, attackSpeed));
    }
}