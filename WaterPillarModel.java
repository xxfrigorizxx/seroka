package com.seroka.client.model;

import com.seroka.ModMain;
import com.seroka.entity.WaterPillarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Modèle Blockbench du pilier d'eau (muret). */
public class WaterPillarModel extends EntityModel<WaterPillarEntity> {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("water_pillar"), "main");

  private final ModelPart bbMain;

  public WaterPillarModel(ModelPart root) {
    this.bbMain = root.getChild("bb_main");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    partdefinition.addOrReplaceChild(
        "bb_main",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-6.0F, -32.0F, -6.0F, 12.0F, 32.0F, 12.0F, CubeDeformation.NONE)
            .texOffs(0, 45)
            .addBox(-7.0F, -32.0F, -5.0F, 1.0F, 32.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(23, 45)
            .addBox(6.0F, -32.0F, -5.0F, 1.0F, 32.0F, 10.0F, CubeDeformation.NONE)
            .texOffs(46, 45)
            .addBox(-5.0F, -32.0F, 6.0F, 10.0F, 32.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(49, 0)
            .addBox(-5.0F, -32.0F, -7.0F, 10.0F, 32.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(78, 60)
            .addBox(1.0F, -26.0F, -8.0F, 3.0F, 26.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(46, 79)
            .addBox(-3.0F, -32.0F, -8.0F, 3.0F, 26.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(55, 79)
            .addBox(-3.0F, -32.0F, 7.0F, 3.0F, 26.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(81, 0)
            .addBox(1.0F, -26.0F, 7.0F, 3.0F, 26.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(69, 34)
            .addBox(7.0F, -26.0F, 1.0F, 1.0F, 26.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(69, 64)
            .addBox(7.0F, -32.0F, -3.0F, 1.0F, 26.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(72, 0)
            .addBox(-8.0F, -32.0F, -3.0F, 1.0F, 26.0F, 3.0F, CubeDeformation.NONE)
            .texOffs(78, 30)
            .addBox(-8.0F, -26.0F, 1.0F, 1.0F, 26.0F, 3.0F, CubeDeformation.NONE),
        PartPose.offset(0.0F, 24.0F, 0.0F)
    );

    return LayerDefinition.create(meshdefinition, 128, 128);
  }

  @Override
  public void setupAnim(
      WaterPillarEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
  }

  @Override
  public void renderToBuffer(
      com.mojang.blaze3d.vertex.PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    bbMain.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }
}
