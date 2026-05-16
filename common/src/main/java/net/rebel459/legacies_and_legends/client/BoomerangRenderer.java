package net.rebel459.legacies_and_legends.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.entity.BoomerangProjectile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class BoomerangRenderer extends EntityRenderer<BoomerangProjectile, BoomerangRenderState> {
    public static final Identifier TEXTURE = LaLConstants.id("textures/entity/boomerang.png");
    private final BoomerangModel<BoomerangProjectile> model;

    public BoomerangRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BoomerangModel<>(context.bakeLayer(LaLModelLayers.BOOMERANG));
    }

    @Override
    public void submit(BoomerangRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 45 + state.spinTick * 20));

        submitNodeCollector.order(0).submitModel(
                this.model,
                state,
                poseStack,
                TEXTURE,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
        );

        if (state.isFoil) {
            submitNodeCollector.order(1).submitModel(
                    this.model,
                    state,
                    poseStack,
                    ItemFeatureRenderer.getFoilRenderType(
                            this.model.renderType(TEXTURE),
                            false
                    ),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor,
                    null
            );
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    public @NotNull BoomerangRenderState createRenderState() {
        return new BoomerangRenderState();
    }

    public void extractRenderState(BoomerangProjectile boomerangProjectile, BoomerangRenderState boomerangRenderState, float partialTick) {
        super.extractRenderState(boomerangProjectile, boomerangRenderState, partialTick);
        //boomerangRenderState.yRot = boomerangProjectile.getYRot(partialTick);
        boomerangRenderState.xRot = boomerangProjectile.getXRot(partialTick);
        boomerangRenderState.boomerangYaw = boomerangProjectile.getBoomerangYaw(partialTick);
        boomerangRenderState.wobbleProgress = boomerangProjectile.getWobbleProgress(partialTick);
        boomerangRenderState.spinTick = boomerangProjectile.getSpinTick();
        boomerangRenderState.isFoil = boomerangProjectile.isFoil();
    }
}
