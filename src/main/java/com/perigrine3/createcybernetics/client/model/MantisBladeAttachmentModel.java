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

import java.util.function.Function;

public final class MantisBladeAttachmentModel extends Model {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "mantis_blade"),
                    "main"
            );

    private static final float BLADE_THICKNESS = 0.0005F;
    private static final float BLADE_HALF_THICKNESS = BLADE_THICKNESS / 2.0F;

    private final ModelPart handle;
    private final ModelPart blade;

    private boolean bladeVisible = true;

    public MantisBladeAttachmentModel(ModelPart root) {
        super((Function<ResourceLocation, RenderType>) RenderType::entityCutoutNoCull);

        this.handle = root.getChild("handle");
        this.blade = root.getChild("blade");
    }

    public void setBladeVisible(boolean bladeVisible) {
        this.bladeVisible = bladeVisible;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "handle",
                CubeListBuilder.create()
                        .texOffs(10, 1)
                        .addBox(
                                -8.5F,
                                6.5F,
                                -1.0F,
                                2.0F,
                                5.0F,
                                2.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "blade",
                CubeListBuilder.create()
                        .texOffs(2, 1)
                        .addBox(
                                -4.0F,
                                -8.0F,
                                -BLADE_HALF_THICKNESS,
                                4.0F,
                                15.0F,
                                BLADE_THICKNESS,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        -8.0F,
                        12.5F,
                        0.0F,
                        0.0F,
                        0.0F,
                        -0.0873F
                )
        );

        return LayerDefinition.create(mesh, 18, 18);
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color
    ) {
        this.handle.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                color
        );

        if (this.bladeVisible) {
            this.blade.render(
                    poseStack,
                    vertexConsumer,
                    packedLight,
                    packedOverlay,
                    color
            );
        }
    }
}