package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

public final class ExosuitArmorStandLayer extends RenderLayer<ArmorStand, ArmorStandArmorModel> {

    private static final ResourceLocation EXOSUIT1_ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateCybernetics.MODID,
                    "textures/models/armor/exosuit1_layer_1.png"
            );

    private final ArmorStandArmorModel armorModel;

    public ExosuitArmorStandLayer(
            RenderLayerParent<ArmorStand, ArmorStandArmorModel> parent,
            EntityModelSet modelSet
    ) {
        super(parent);
        this.armorModel = new ArmorStandArmorModel(modelSet.bakeLayer(ModelLayers.ARMOR_STAND_OUTER_ARMOR));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            ArmorStand armorStand,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        ItemStack chestStack = armorStand.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestStack.is(ModItems.EXOSUIT1)) {
            return;
        }

        if (armorStand.isInvisible()) {
            return;
        }

        ArmorStandArmorModel parentModel = this.getParentModel();
        parentModel.copyPropertiesTo(this.armorModel);

        this.armorModel.setAllVisible(false);

        // Chest armor renders torso + arms, like a vanilla chestplate.
        this.armorModel.body.visible = true;
        this.armorModel.rightArm.visible = true;
        this.armorModel.leftArm.visible = true;

        var vertexConsumer = buffer.getBuffer(RenderType.armorCutoutNoCull(EXOSUIT1_ARMOR_TEXTURE));

        this.armorModel.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }
}