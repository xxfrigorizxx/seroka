package com.seroka.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.ModMain;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Modèle 3D de l'essence de lave — même géométrie que l'essence d'eau. */
public class EssenceLaveModel {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("essence_lave"), "main");

  private static final float VITESSE_ROTATION_CENTRE = 18.0F;
  private static final float VITESSE_ROTATION_CONTOUR = 14.0F;
  private static final float INCLINAISON_CENTRE = -0.3491F;
  private static final float INCLINAISON_CONTOUR = 0.1309F;
  private static final float DEG_EN_RAD = (float) Math.PI / 180F;

  private final ModelPart centre;
  private final ModelPart contour;

  public EssenceLaveModel(ModelPart root) {
    this.centre = root.getChild("centre");
    this.contour = root.getChild("contour");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition root = meshdefinition.getRoot();

    root.addOrReplaceChild(
        "centre",
        CubeListBuilder.create()
            .texOffs(0, 9).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(9, 9).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 13).addBox(-3.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(7, 13).addBox(2.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(14, 13).addBox(-1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(17, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
    );

    root.addOrReplaceChild(
        "contour",
        CubeListBuilder.create()
            .texOffs(2, 4).addBox(3.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(1.0F, -5.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, 1.0F, 3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -1.0F, 4.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -3.0F, 3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -3.0F, -5.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, 1.0F, -5.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, 3.0F, -3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, 3.0F, 1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -5.0F, 1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -5.0F, -3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-3.0F, -5.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-5.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-6.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-5.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(4.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(3.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-3.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 4).addBox(1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
    );

    return LayerDefinition.create(meshdefinition, 32, 32);
  }

  public void animer(float ageInTicks) {
    this.centre.resetPose();
    this.contour.resetPose();
    this.centre.yRot = INCLINAISON_CENTRE - (ageInTicks * VITESSE_ROTATION_CENTRE) * DEG_EN_RAD;
    this.contour.yRot = INCLINAISON_CONTOUR + (ageInTicks * VITESSE_ROTATION_CONTOUR) * DEG_EN_RAD;
  }

  public void renderContour(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    contour.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }

  public void renderCentre(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    centre.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }
}
