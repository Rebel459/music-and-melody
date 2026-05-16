package net.rebel459.legacies_and_legends.registry;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;

public interface LaLEquipmentAssets {
    ResourceKey<? extends Registry<EquipmentAsset>> ROOT_ID = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));
    ResourceKey<EquipmentAsset> WANDERER = createId("wanderer");
    ResourceKey<EquipmentAsset> TRAVELLING = createId("travelling");
    ResourceKey<EquipmentAsset> REINFORCED = createId("reinforced");

    static ResourceKey<EquipmentAsset> createId(String path) {
        return ResourceKey.create(ROOT_ID, LaLConstants.id(path));
    }
}
