package com.seroka.client.model;

import com.seroka.ModMain;
import com.seroka.entity.WaterCocoonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Coque sphérique translucide pour le cocon d'eau. */
public class WaterCocoonModel extends EntityModel<WaterCocoonEntity> {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("water_cocoon"), "main");

  private final ModelPart shell;

  public WaterCocoonModel(ModelPart root) {
    this.shell = root.getChild("shell");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    partdefinition.addOrReplaceChild("shell", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(1.25F)),
        PartPose.ZERO);

    return LayerDefinition.create(meshdefinition, 64, 64);
  }

  @Override
  public void setupAnim(WaterCocoonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
  }

  @Override
  public void renderToBuffer(
      com.mojang.blaze3d.vertex.PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    shell.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }
}
