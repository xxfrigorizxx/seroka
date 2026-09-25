package com.seroka.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.faction.ArmureEauCastHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Couche de rendu translucide — dilate chaque membre du HumanoidModel natif
 * sans décaler les pivots d'animation (copie de pose + scale local sur chaque ModelPart).
 */
@OnlyIn(Dist.CLIENT)
public class ArmureEauLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

  /** Texture d'eau statique vanilla (translucide). */
  private static final ResourceLocation TEXTURE_EAU =
      ResourceLocation.withDefaultNamespace("textures/block/water_still.png");

  /** Dilatation légère (0,1–0,2) pour englober la peau sans casser les pivots. */
  private static final float DILATATION = 0.15F;

  /** Teinte eau translucide (ARGB). */
  private static final int COULEUR_EAU = 0xB080B8FF;

  private final PlayerModel<AbstractClientPlayer> modeleArmure;

  public ArmureEauLayer(
      RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
      net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
      boolean modeleSlim
  ) {
    super(parent);
    this.modeleArmure = new PlayerModel<>(
        context.bakeLayer(modeleSlim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER),
        modeleSlim
    );
  }

  @Override
  public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer joueur,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
    if (!ArmureEauCastHelper.estActive(joueur)) {
      return;
    }

    PlayerModel<AbstractClientPlayer> modeleParent = this.getParentModel();
    copierPose(modeleParent, modeleArmure);

    modeleArmure.setAllVisible(false);
    modeleArmure.head.visible = true;
    modeleArmure.body.visible = true;
    modeleArmure.leftArm.visible = true;
    modeleArmure.rightArm.visible = true;
    modeleArmure.leftLeg.visible = true;
    modeleArmure.rightLeg.visible = true;

    VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE_EAU));

    renderMembreDilate(modeleArmure.head, poseStack, consumer, packedLight);
    renderMembreDilate(modeleArmure.body, poseStack, consumer, packedLight);
    renderMembreDilate(modeleArmure.leftArm, poseStack, consumer, packedLight);
    renderMembreDilate(modeleArmure.rightArm, poseStack, consumer, packedLight);
    renderMembreDilate(modeleArmure.leftLeg, poseStack, consumer, packedLight);
    renderMembreDilate(modeleArmure.rightLeg, poseStack, consumer, packedLight);
  }

  /** Copie la pose animée du modèle parent sans toucher aux pivots du parent. */
  private static void copierPose(PlayerModel<AbstractClientPlayer> source, PlayerModel<AbstractClientPlayer> cible) {
    cible.crouching = source.crouching;
    cible.riding = source.riding;
    cible.young = source.young;
    cible.head.copyFrom(source.head);
    cible.body.copyFrom(source.body);
    cible.leftArm.copyFrom(source.leftArm);
    cible.rightArm.copyFrom(source.rightArm);
    cible.leftLeg.copyFrom(source.leftLeg);
    cible.rightLeg.copyFrom(source.rightLeg);
  }

  /**
   * Dilate un membre autour de son pivot local (scale uniforme) puis restaure l'échelle.
   * Les translations/rotations d'animation restent intactes car elles sont copiées avant le scale.
   */
  private void renderMembreDilate(
      ModelPart membre,
      PoseStack poseStack,
      VertexConsumer consumer,
      int packedLight
  ) {
    float facteur = 1.0F + DILATATION;
    membre.xScale = facteur;
    membre.yScale = facteur;
    membre.zScale = facteur;
    membre.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COULEUR_EAU);
    membre.xScale = 1.0F;
    membre.yScale = 1.0F;
    membre.zScale = 1.0F;
  }
}
