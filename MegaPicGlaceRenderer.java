package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.MegaPicGlaceModel;
import com.seroka.entity.MegaPicGlaceEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Rendu du mega pic de glace — texture translucide type glace. */
public class MegaPicGlaceRenderer extends EntityRenderer<MegaPicGlaceEntity> {

  private static final ResourceLocation TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/block/blue_ice.png");

  private final MegaPicGlaceModel model;

  public MegaPicGlaceRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new MegaPicGlaceModel(context.bakeLayer(MegaPicGlaceModel.LAYER));
    this.shadowRadius = 1.6F;
  }

  @Override
  public void render(
      MegaPicGlaceEntity entite,
      float yawEntite,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    poseStack.pushPose();
    Direction face = entite.getAncrageFace();
    float echelle = MegaPicGlaceEntity.ECHELLE_RENDU;

    // Animation d'émergence : enfoui dans le bloc → position finale calibrée à progression = 1.0f.
    float deltaPartial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    float progression = Math.min(1.0F, (entite.tickCount + deltaPartial) / (float) MegaPicGlaceEntity.DUREE_EMERGENCE_TICKS);
    float recul = (1.0F - progression) * -2.5F;
    poseStack.translate(face.getStepX() * recul, face.getStepY() * recul, face.getStepZ() * recul);

    float rotationBascule = calculerRotationBascule(entite, deltaPartial);

    if (face == Direction.UP) {
      renderSurSol(poseStack, echelle, entite.getYawVisuel());
    } else if (face == Direction.DOWN) {
      renderPlafond(poseStack, echelle, entite.getYawVisuel());
    } else {
      renderMur(poseStack, face, echelle, rotationBascule);
    }

    VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entite)));
    this.model.setupAnim(entite, 0.0F, 0.0F, entite.tickCount + partialTick, 0.0F, 0.0F);
    this.model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, -1);
    poseStack.popPose();

    super.render(entite, yawEntite, partialTick, poseStack, buffer, packedLight);
  }

  /** Interpolation fluide de la bascule mur (0° → 90°). */
  private static float calculerRotationBascule(MegaPicGlaceEntity entite, float deltaPartial) {
    if (entite.isChuting()) {
      return 90.0F;
    }
    float rotation = entite.getRotationChute();
    if (rotation > 0.0F && entite.tickCount < MegaPicGlaceEntity.TICK_DEBUT_CHUTE_MUR) {
      float progression = (entite.tickCount + deltaPartial - MegaPicGlaceEntity.TICK_DEBUT_BASCULE_MUR)
          / (float) MegaPicGlaceEntity.DUREE_BASCULE_TICKS;
      rotation = Mth.clamp(progression * 90.0F, 0.0F, 90.0F);
    }
    return rotation;
  }

  private static void renderSurSol(PoseStack poseStack, float echelle, float yaw) {
    poseStack.translate(0.0F, MegaPicGlaceEntity.AJUSTEMENT_SOL_BLOCS, 0.0F);
    poseStack.scale(echelle, echelle, echelle);
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
  }

  /** Plafond — pipeline validé par le joueur. */
  private static void renderPlafond(PoseStack poseStack, float echelle, float yaw) {
    poseStack.translate(0.0F, -MegaPicGlaceEntity.ANCRAGE_MUR_PLAFOND_BLOCS, 0.0F);
    poseStack.scale(echelle, echelle, echelle);
    poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
  }

  private static void renderMur(PoseStack poseStack, Direction face, float echelle, float rotationBascule) {
    if (face == Direction.NORTH || face == Direction.SOUTH) {
      renderMurNordSud(poseStack, face, echelle, rotationBascule);
    } else {
      renderMurEstOuest(poseStack, face, echelle, rotationBascule);
    }
  }

  /**
   * Murs nord/sud — base du pic sur l'origine (pas l'ancrage plafond en Y).
   * Rx(-90) : pointe modèle -Y → +Z (sud). Rx(90) : pointe → -Z (nord).
   */
  private static void renderMurNordSud(PoseStack poseStack, Direction face, float echelle, float rotationBascule) {
    decalerVersExterieurMur(poseStack, face);
    poseStack.scale(echelle, echelle, echelle);
    if (face == Direction.SOUTH) {
      poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
      appliquerBasculeMur(poseStack, face, rotationBascule);
    } else {
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      appliquerBasculeMur(poseStack, face, rotationBascule);
    }
  }

  /** Murs est/ouest — orientation projectile + 10px vers l'extérieur (axe X). */
  private static void renderMurEstOuest(PoseStack poseStack, Direction face, float echelle, float rotationBascule) {
    decalerVersExterieurMur(poseStack, face);
    poseStack.translate(0.0F, -MegaPicGlaceEntity.ANCRAGE_MUR_PLAFOND_BLOCS, 0.0F);
    poseStack.scale(echelle, echelle, echelle);
    float yawMur = face == Direction.EAST ? -90.0F : 90.0F;
    poseStack.mulPose(Axis.YP.rotationDegrees(yawMur + 180.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    appliquerBasculeMur(poseStack, face, rotationBascule);
  }

  /**
   * Bascule la pointe du pic vers le sol (pitch vers le bas, pas rotation horizontale).
   */
  private static void appliquerBasculeMur(PoseStack poseStack, Direction face, float rotationChute) {
    if (rotationChute <= 0.0F) {
      return;
    }
    switch (face) {
      case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-rotationChute));
      case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(rotationChute));
      case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(rotationChute));
      case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(rotationChute));
      default -> {}
    }
  }

  /** Pousse le modèle vers l'extérieur du bloc, le long de la normale de la face (espace monde). */
  private static void decalerVersExterieurMur(PoseStack poseStack, Direction face) {
    float decalage = MegaPicGlaceEntity.AJUSTEMENT_MUR_BLOCS;
    switch (face) {
      case SOUTH -> poseStack.translate(0.0F, 0.0F, decalage);
      case NORTH -> poseStack.translate(0.0F, 0.0F, -decalage);
      case EAST -> poseStack.translate(decalage, 0.0F, 0.0F);
      case WEST -> poseStack.translate(-decalage, 0.0F, 0.0F);
      default -> {}
    }
  }

  @Override
  public ResourceLocation getTextureLocation(MegaPicGlaceEntity entite) {
    return TEXTURE;
  }
}
