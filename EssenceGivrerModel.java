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

/**
 * Modèle Blockbench de l'essence givrée — noyau (glace bleue) et anneau extérieur (glace).
 * Géométrie recentrée sur l'origine (comme l'essence d'eau) pour l'inventaire, le sol et la main.
 */
public class EssenceGivrerModel {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("essence_givrer"), "main");

  private static final float VITESSE_ROTATION_CENTRE = 18.0F;
  private static final float VITESSE_ROTATION_CONTOUR = 14.0F;
  private static final float INCLINAISON_CENTRE = -0.3491F;
  private static final float INCLINAISON_CONTOUR = 0.1309F;
  private static final float DEG_EN_RAD = (float) Math.PI / 180F;

  private final ModelPart interieur;
  private final ModelPart exterieur;

  public EssenceGivrerModel(ModelPart root) {
    this.interieur = root.getChild("interieur");
    this.exterieur = root.getChild("exterieur");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition root = meshdefinition.getRoot();

    root.addOrReplaceChild(
        "interieur",
        CubeListBuilder.create()
            .texOffs(0, 0).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(-3, -2).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(-6, -4).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(-3, -2).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
        PartPose.offset(0.0F, 0.0F, 0.0F)
    );

    PartDefinition exterieur = root.addOrReplaceChild(
        "exterieur",
        CubeListBuilder.create()
            .texOffs(1, 1).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(1, 1).addBox(-1.0F, 0.0F, 3.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(1, 0).addBox(-4.0F, 0.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(1, 0).addBox(3.0F, 0.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(1.0F, 0.0F, 6.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-2.0F, 0.0F, 6.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-7.0F, 0.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-7.0F, 0.0F, -2.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-2.0F, 0.0F, -7.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(1.0F, 0.0F, -7.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(6.0F, 0.0F, -2.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(6.0F, 0.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
        PartPose.offset(0.0F, 0.0F, 0.0F)
    );

    exterieur.addOrReplaceChild(
        "exterieur_r1",
        CubeListBuilder.create()
            .texOffs(2, 1).addBox(-7.0F, 0.0F, -2.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-7.0F, 0.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(6.0F, 0.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(6.0F, 0.0F, -2.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(1.0F, 0.0F, -7.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-2.0F, 0.0F, -7.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(-2.0F, 0.0F, 6.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(2, 1).addBox(1.0F, 0.0F, 6.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
    );

    return LayerDefinition.create(meshdefinition, 16, 16);
  }

  /** Double rotation opposée — noyau glace bleue / anneau glace. */
  public void animer(float ageInTicks) {
    this.interieur.resetPose();
    this.exterieur.resetPose();
    this.interieur.yRot = INCLINAISON_CENTRE - (ageInTicks * VITESSE_ROTATION_CENTRE) * DEG_EN_RAD;
    this.exterieur.yRot = INCLINAISON_CONTOUR + (ageInTicks * VITESSE_ROTATION_CONTOUR) * DEG_EN_RAD;
  }

  public void renderExterieur(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    exterieur.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }

  public void renderInterieur(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    interieur.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }
}
