package com.perigrine3.createcybernetics.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class VampyresAttachmentModel extends Model {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "vampyres"),
                    "main"
            );

    private final ModelPart bb_main;

    public VampyresAttachmentModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bb_main = root.addOrReplaceChild(
                "bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -2.0F,
                                -4.0F,
                                -5.0F,
                                1.0F,
                                2.0F,
                                1.0F,
                                new CubeDeformation(0.0F)
                        )
                        .texOffs(0, 3)
                        .addBox(
                                1.0F,
                                -4.0F,
                                -5.0F,
                                1.0F,
                                2.0F,
                                1.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.ZERO
        );

        bb_main.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(4, 3)
                        .addBox(
                                0.0F,
                                0.0F,
                                -0.5F,
                                0.0F,
                                2.0F,
                                1.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        1.5F,
                        -3.0F,
                        -4.5F,
                        0.1309F,
                        -0.7854F,
                        0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(4, 0)
                        .addBox(
                                0.0F,
                                0.0F,
                                -0.5F,
                                0.0F,
                                2.0F,
                                1.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        -1.5F,
                        -3.0F,
                        -4.5F,
                        0.1309F,
                        0.7854F,
                        0.0F
                )
        );

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color
    ) {
        bb_main.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                color
        );
    }
}