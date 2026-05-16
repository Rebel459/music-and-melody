package net.rebel459.legacies_and_legends.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.unified.platform.client.UnifiedClientHelpers;

public final class LaLModelLayers {
    public static final ModelLayerLocation BOOMERANG = new ModelLayerLocation(LaLConstants.id("boomerang"), "main");

    public static void init() {
        UnifiedClientHelpers.ENTITY_RENDERERS.addLayerDefinition(BOOMERANG, BoomerangModel::createBodyLayer);
    }
}
