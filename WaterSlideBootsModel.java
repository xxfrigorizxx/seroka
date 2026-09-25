package com.seroka.client.model;

import com.seroka.ModMain;
import com.seroka.entity.WaterSlideEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Petits cubes d'eau translucides enveloppant les pieds du joueur. */
public class WaterSlideBootsModel extends EntityModel<WaterSlideEntity> {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("water_slide"), "main");

  /** Espacement latéral des pieds (coordonnées modèle, 16 px = 1 bloc). */
  private static final float FOOT_SPREAD = 5.0F;
  /** Hauteur des cubes enveloppant la botte. */
  private static final float BOOT_HEIGHT = 10.0F;
  private static final float BOOT_HALF = 3.5F;

  private final ModelPart leftBoot;
  private final ModelPart rightBoot;
  private final float leftBootBaseY;
  private final float rightBootBaseY;

  public WaterSlideBootsModel(ModelPart root) {
    this.leftBoot = root.getChild("left_boot");
    this.rightBoot = root.getChild("right_boot");
    this.leftBootBaseY = leftBoot.y;
    this.rightBootBaseY = rightBoot.y;
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();
    CubeDeformation inflate = new CubeDeformation(0.5F);

    partdefinition.addOrReplaceChild(
        "left_boot",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(
                -BOOT_HALF,
                -BOOT_HEIGHT * 0.55F,
                -BOOT_HALF,
                BOOT_HALF * 2.0F,
                BOOT_HEIGHT,
                BOOT_HALF * 2.0F,
                inflate
            ),
        PartPose.offset(-FOOT_SPREAD, 0.0F, 0.0F)
    );
    partdefinition.addOrReplaceChild(
        "right_boot",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(
                -BOOT_HALF,
                -BOOT_HEIGHT * 0.55F,
                -BOOT_HALF,
                BOOT_HALF * 2.0F,
                BOOT_HEIGHT,
                BOOT_HALF * 2.0F,
                inflate
            ),
        PartPose.offset(FOOT_SPREAD, 0.0F, 0.0F)
    );

    return LayerDefinition.create(meshdefinition, 32, 32);
  }

  @Override
  public void setupAnim(
      WaterSlideEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
    float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
    leftBoot.xRot = swing;
    rightBoot.xRot = -swing;
    float bob = Mth.abs(Mth.sin(limbSwing * 0.6662F) * limbSwingAmount) * 1.2F;
    leftBoot.y = leftBootBaseY + bob;
    rightBoot.y = rightBootBaseY + bob;
  }

  @Override
  public void renderToBuffer(
      com.mojang.blaze3d.vertex.PoseStack poseStack,
      com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    leftBoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    rightBoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
  }
}
