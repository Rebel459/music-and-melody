package net.rebel459.legacies_and_legends.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class BoomerangModel<T extends Entity> extends EntityModel<@NotNull BoomerangRenderState> {
    private final ModelPart body;

    public BoomerangModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
    }

    public static @NotNull LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition body = partDefinition.addOrReplaceChild("body",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F)
        );

        PartDefinition partDefinition2 = body.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6F, -24F, 1F, 10F, 1F, 3F, new CubeDeformation(0F)),
                PartPose.offset(0F, 24F, 0F)
        );
        partDefinition2.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(-6.0F, -24.0F, -1.0F, 7.0F, 1.0F, 3.0F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F)
        );
        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    @Override
    public void setupAnim(BoomerangRenderState renderState) {
        super.setupAnim(renderState);
        this.body.yRot = renderState.boomerangYaw;
    }
}

