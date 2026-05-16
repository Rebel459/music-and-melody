package net.rebel459.legacies_and_legends.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.rebel459.legacies_and_legends.registry.LaLEntityTypes;
import net.rebel459.unified.platform.client.UnifiedClientHelpers;

public class LaLEntityRenderers {

    public static void init() {
        UnifiedClientHelpers.ENTITY_RENDERERS.addEntityRenderer(LaLEntityTypes.BOOMERANG::get, BoomerangRenderer::new);
        UnifiedClientHelpers.ENTITY_RENDERERS.addEntityRenderer(LaLEntityTypes.GLOW_STICK::get, context -> new ThrownItemRenderer<>(context, 1F, true));
    }
}
