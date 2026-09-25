package com.seroka.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.ModMain;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class PillardRockModel<T extends Entity> extends EntityModel<T> {
  public static final ModelLayerLocation LAYER_LOCATION =
      new ModelLayerLocation(ModMain.id("pillard_rock"), "main");
  private final ModelPart bbMain;

  public PillardRockModel(ModelPart root) {
    this.bbMain = root.getChild("bb_main");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    partdefinition.addOrReplaceChild(
        "bb_main",
        CubeListBuilder.create()
            .texOffs(0, 52).addBox(-7.0F, -14.0F, -8.0F, 13.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(60, 34).addBox(-8.0F, -2.0F, -7.0F, 15.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-7.0F, -4.0F, -8.0F, 14.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(60, 0).addBox(-8.0F, -6.0F, -7.0F, 14.0F, 2.0F, 15.0F, new CubeDeformation(0.0F))
            .texOffs(0, 70).addBox(-8.0F, -8.0F, -8.0F, 15.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(60, 17).addBox(-8.0F, -10.0F, -7.0F, 14.0F, 2.0F, 15.0F, new CubeDeformation(0.0F))
            .texOffs(58, 70).addBox(-8.0F, -12.0F, -7.0F, 15.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(0, 18).addBox(-8.0F, -16.0F, -8.0F, 15.0F, 2.0F, 15.0F, new CubeDeformation(0.0F))
            .texOffs(58, 52).addBox(-7.0F, -18.0F, -8.0F, 13.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(0, 35).addBox(-8.0F, -20.0F, -8.0F, 15.0F, 2.0F, 15.0F, new CubeDeformation(0.0F))
            .texOffs(0, 86).addBox(-8.0F, -22.0F, -6.0F, 14.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)),
        PartPose.offset(0.0F, 24.0F, 0.0F)
    );

    return LayerDefinition.create(meshdefinition, 128, 128);
  }

  @Override
  public void setupAnim(
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {}

  @Override
  public void renderToBuffer(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    bbMain.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }
}
