package com.seroka.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.ModMain;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Armure de golem tellurique modelée autour du joueur.
 *
 * <p>Les coordonnées sont exprimées dans l'espace du modèle joueur : y = 0 aux épaules,
 * y = 24 au sol. Le casque laisse une cavité ouverte sur l'avant où vient se loger la tête
 * du joueur (voir {@link #HEAD_LIFT}).
 */
public class GolemArmorModel {

  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(ModMain.id("golem_armor"), "main");

  /** Décalage vertical à appliquer à la tête du joueur pour la centrer dans le casque. */
  public static final float HEAD_LIFT = -5.0F;

  /** Distance supplémentaire entre le pivot du bras et le poing, par rapport à un bras humain. */
  private static final float HAND_OFFSET = 7.0F;

  private static final float LEG_STAGE_START = 0.00F;
  private static final float LEG_STAGE_END = 0.30F;
  private static final float HIP_STAGE_START = 0.20F;
  private static final float HIP_STAGE_END = 0.50F;
  private static final float BODY_STAGE_START = 0.40F;
  private static final float BODY_STAGE_END = 0.75F;
  private static final float ARM_STAGE_START = 0.60F;
  private static final float ARM_STAGE_END = 0.90F;
  private static final float HEAD_STAGE_START = 0.78F;
  private static final float HEAD_STAGE_END = 1.00F;

  private final ModelPart head;
  private final ModelPart body;
  private final ModelPart hips;
  private final ModelPart rightArm;
  private final ModelPart leftArm;
  private final ModelPart rightLeg;
  private final ModelPart leftLeg;

  private final float headBaseY;
  private final float bodyBaseY;
  private final float hipsBaseY;
  private final float armBaseY;
  private final float legBaseY;

  public GolemArmorModel(ModelPart root) {
    this.head = root.getChild("head");
    this.body = root.getChild("body");
    this.hips = root.getChild("hips");
    this.rightArm = root.getChild("right_arm");
    this.leftArm = root.getChild("left_arm");
    this.rightLeg = root.getChild("right_leg");
    this.leftLeg = root.getChild("left_leg");

    this.headBaseY = head.y;
    this.bodyBaseY = body.y;
    this.hipsBaseY = hips.y;
    this.armBaseY = rightArm.y;
    this.legBaseY = rightLeg.y;
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();

    // Casque ouvert sur l'avant : couronne, nuque, joues, arcade et nez de golem.
    root.addOrReplaceChild(
        "head",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-6.0F, -11.0F, -6.0F, 12.0F, 3.0F, 12.0F)
            .texOffs(0, 16)
            .addBox(-6.0F, -8.0F, 4.0F, 12.0F, 8.0F, 2.0F)
            .texOffs(0, 28)
            .addBox(4.0F, -8.0F, -4.0F, 2.0F, 8.0F, 8.0F)
            .texOffs(20, 28)
            .addBox(-6.0F, -8.0F, -4.0F, 2.0F, 8.0F, 8.0F)
            .texOffs(40, 28)
            .addBox(-6.0F, -8.0F, -6.0F, 12.0F, 2.0F, 2.0F)
            .texOffs(40, 34)
            .addBox(-1.5F, -6.0F, -7.0F, 3.0F, 5.0F, 2.0F),
        PartPose.offset(0.0F, -5.0F, 0.0F)
    );

    // Épaules massives puis buste : enveloppe complètement le torse du joueur.
    root.addOrReplaceChild(
        "body",
        CubeListBuilder.create()
            .texOffs(0, 46)
            .addBox(-15.0F, -5.0F, -7.0F, 30.0F, 6.0F, 14.0F)
            .texOffs(0, 68)
            .addBox(-11.0F, 1.0F, -7.0F, 22.0F, 9.0F, 14.0F),
        PartPose.offset(0.0F, 0.0F, 0.0F)
    );

    root.addOrReplaceChild(
        "hips",
        CubeListBuilder.create()
            .texOffs(0, 92)
            .addBox(-9.0F, 0.0F, -6.0F, 18.0F, 4.0F, 12.0F),
        PartPose.offset(0.0F, 10.0F, 0.0F)
    );

    CubeListBuilder rightArmCubes = CubeListBuilder.create()
        .texOffs(64, 46)
        .addBox(-4.0F, -2.0F, -4.0F, 8.0F, 18.0F, 8.0F)
        .texOffs(64, 74)
        .addBox(-5.0F, 13.0F, -5.0F, 10.0F, 6.0F, 10.0F);
    root.addOrReplaceChild("right_arm", rightArmCubes, PartPose.offset(-14.0F, -3.0F, 0.0F));

    CubeListBuilder leftArmCubes = CubeListBuilder.create()
        .texOffs(96, 46)
        .addBox(-4.0F, -2.0F, -4.0F, 8.0F, 18.0F, 8.0F)
        .texOffs(96, 74)
        .addBox(-5.0F, 13.0F, -5.0F, 10.0F, 6.0F, 10.0F);
    root.addOrReplaceChild("left_arm", leftArmCubes, PartPose.offset(14.0F, -3.0F, 0.0F));

    CubeListBuilder legCubes = CubeListBuilder.create()
        .texOffs(64, 92)
        .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F)
        .texOffs(64, 110)
        .addBox(-5.5F, 8.0F, -6.0F, 11.0F, 2.0F, 12.0F);
    root.addOrReplaceChild("right_leg", legCubes, PartPose.offset(-5.0F, 14.0F, 0.0F));
    root.addOrReplaceChild("left_leg", legCubes, PartPose.offset(5.0F, 14.0F, 0.0F));

    return LayerDefinition.create(mesh, 128, 128);
  }

  /**
   * Pose le golem : marche, orientation de la tête, coup de bras et émergence progressive.
   *
   * @param formProgress 0 = rien de formé, 1 = armure complète
   */
  public void setupAnim(
      LivingEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float netHeadYaw,
      float headPitch,
      float formProgress
  ) {
    resetPose();

    head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
    head.xRot = headPitch * Mth.DEG_TO_RAD;

    float stride = Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
    rightLeg.xRot = -1.2F * stride;
    leftLeg.xRot = 1.2F * stride;
    rightArm.xRot = (-0.2F + 1.3F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
    leftArm.xRot = (-0.2F - 1.3F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;

    applyAttackSwing(entity, partialTick);
    applyFormation(formProgress);
  }

  /** Le bras du golem accompagne le coup du joueur, amplifié pour un impact de titan. */
  private void applyAttackSwing(LivingEntity entity, float partialTick) {
    float attack = entity.getAttackAnim(partialTick);
    if (attack <= 0.0F) {
      return;
    }

    float curve = Mth.sin(Mth.sqrt(attack) * Mth.PI);
    boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
    ModelPart arm = rightHanded ? rightArm : leftArm;
    arm.xRot = -2.3F * curve;
    arm.zRot = (rightHanded ? 0.35F : -0.35F) * curve;
    body.yRot = (rightHanded ? -0.25F : 0.25F) * curve;
  }

  /** Chaque pièce de roche jaillit du sol et grossit, des jambes vers la tête. */
  private void applyFormation(float formProgress) {
    if (formProgress >= 1.0F) {
      return;
    }

    stagePart(rightLeg, legBaseY, formProgress, LEG_STAGE_START, LEG_STAGE_END, 12.0F);
    stagePart(leftLeg, legBaseY, formProgress, LEG_STAGE_START, LEG_STAGE_END, 12.0F);
    stagePart(hips, hipsBaseY, formProgress, HIP_STAGE_START, HIP_STAGE_END, 10.0F);
    stagePart(body, bodyBaseY, formProgress, BODY_STAGE_START, BODY_STAGE_END, 10.0F);
    stagePart(rightArm, armBaseY, formProgress, ARM_STAGE_START, ARM_STAGE_END, 8.0F);
    stagePart(leftArm, armBaseY, formProgress, ARM_STAGE_START, ARM_STAGE_END, 8.0F);
    stagePart(head, headBaseY, formProgress, HEAD_STAGE_START, HEAD_STAGE_END, 6.0F);
  }

  private void stagePart(
      ModelPart part,
      float baseY,
      float formProgress,
      float start,
      float end,
      float rise
  ) {
    float stage = Mth.clamp((formProgress - start) / (end - start), 0.0F, 1.0F);
    if (stage <= 0.0F) {
      part.visible = false;
      return;
    }
    part.visible = true;
    float scale = 0.25F + 0.75F * stage;
    part.xScale = scale;
    part.yScale = scale;
    part.zScale = scale;
    part.y = baseY + (1.0F - stage) * rise;
  }

  private void resetPose() {
    resetPart(head, headBaseY);
    resetPart(body, bodyBaseY);
    resetPart(hips, hipsBaseY);
    resetPart(rightArm, armBaseY);
    resetPart(leftArm, armBaseY);
    resetPart(rightLeg, legBaseY);
    resetPart(leftLeg, legBaseY);
  }

  private void resetPart(ModelPart part, float baseY) {
    part.visible = true;
    part.xScale = 1.0F;
    part.yScale = 1.0F;
    part.zScale = 1.0F;
    part.y = baseY;
    part.xRot = 0.0F;
    part.yRot = 0.0F;
    part.zRot = 0.0F;
  }

  /** Amène le repère sur le poing du golem pour y rendre l'objet tenu. */
  public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
    ModelPart part = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
    part.translateAndRotate(poseStack);
    poseStack.translate(0.0F, HAND_OFFSET / 16.0F, 0.0F);
  }

  /** Progression à partir de laquelle la tête du joueur apparaît dans le casque. */
  public static float headRevealProgress() {
    return HEAD_STAGE_START;
  }

  public void renderToBuffer(
      PoseStack poseStack,
      VertexConsumer consumer,
      int packedLight,
      int packedOverlay,
      int color
  ) {
    head.render(poseStack, consumer, packedLight, packedOverlay, color);
    body.render(poseStack, consumer, packedLight, packedOverlay, color);
    hips.render(poseStack, consumer, packedLight, packedOverlay, color);
    rightArm.render(poseStack, consumer, packedLight, packedOverlay, color);
    leftArm.render(poseStack, consumer, packedLight, packedOverlay, color);
    rightLeg.render(poseStack, consumer, packedLight, packedOverlay, color);
    leftLeg.render(poseStack, consumer, packedLight, packedOverlay, color);
  }
}
