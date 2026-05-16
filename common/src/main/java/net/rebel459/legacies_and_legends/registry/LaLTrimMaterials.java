package net.rebel459.legacies_and_legends.registry;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;

public class LaLTrimMaterials {

    public static final MaterialAssetGroup ECHO_GROUP = MaterialAssetGroup.create("echo");
    public static final MaterialAssetGroup SAPPHIRE_GROUP = MaterialAssetGroup.create("sapphire");

    public static final ResourceKey<TrimMaterial> ECHO = register("echo");
    public static final ResourceKey<TrimMaterial> SAPPHIRE = register("sapphire");

    public static void bootstrap(BootstrapContext<TrimMaterial> context) {
        TrimMaterials.register(context, ECHO, Style.EMPTY.withColor(675936), ECHO_GROUP);
        TrimMaterials.register(context, SAPPHIRE, Style.EMPTY.withColor(34303), SAPPHIRE_GROUP);
    }

    private static ResourceKey<TrimMaterial> register(String name) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, LaLConstants.id(name));
    }
}
