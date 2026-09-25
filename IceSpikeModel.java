package com.seroka.client.model;

import com.seroka.ModMain;
import com.seroka.entity.IceSpikeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class IceSpikeModel extends EntityModel<IceSpikeEntity> {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("ice_spike"), "main");

  private final ModelPart bbMain;

  public IceSpikeModel(ModelPart root) {
    this.bbMain = root.getChild("bb_main");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create()
            .texOffs(21, 21).addBox(-2.0F, -3.0F, -1.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(19, 31).addBox(-1.0F, -5.0F, -1.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(0, 33).addBox(2.0F, -5.0F, -1.0F, 1.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(40, 0).addBox(0.0F, -4.0F, 4.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(17, 40).addBox(-2.0F, -4.0F, -1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(40, 21).addBox(0.0F, -6.0F, -1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 42).addBox(0.0F, -2.0F, -1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(36, 31).addBox(2.0F, -4.0F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-2.0F, -5.0F, -8.0F, 4.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(21, 12).addBox(-1.0F, -6.0F, -8.0F, 3.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(23, 0).addBox(2.0F, -5.0F, -8.0F, 1.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(0, 23).addBox(-3.0F, -3.0F, -7.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(40, 7).addBox(-1.0F, -2.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
            .texOffs(36, 39).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(40, 27).addBox(0.0F, -1.0F, -6.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 12).addBox(3.0F, -4.0F, -6.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)),
        PartPose.offset(0.0F, 0.0F, 0.0F));

    return LayerDefinition.create(meshdefinition, 64, 64);
  }

  @Override
  public void setupAnim(IceSpikeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
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
