package com.seroka.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.client.renderer.FlowingSpriteVertexConsumer;
import com.seroka.faction.ArmureTerreCastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmureTerreLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
  private static final float DILATATION = 0.16F;
  private static final int COULEUR = 0xFFFFFFFF;

  private final PlayerModel<AbstractClientPlayer> modeleArmure;

  public ArmureTerreLayer(
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
    if (!ArmureTerreCastHelper.estActive(joueur)) {
      return;
    }

    BlockState state = ArmureTerreCastHelper.getBlockState(joueur);
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getBlockRenderer()
        .getBlockModel(state)
        .getParticleIcon();

    PlayerModel<AbstractClientPlayer> modeleParent = this.getParentModel();
    modeleArmure.crouching = modeleParent.crouching;
    modeleArmure.riding = modeleParent.riding;
    modeleArmure.young = modeleParent.young;
    modeleArmure.head.copyFrom(modeleParent.head);
    modeleArmure.body.copyFrom(modeleParent.body);
    modeleArmure.leftArm.copyFrom(modeleParent.leftArm);
    modeleArmure.rightArm.copyFrom(modeleParent.rightArm);
    modeleArmure.leftLeg.copyFrom(modeleParent.leftLeg);
    modeleArmure.rightLeg.copyFrom(modeleParent.rightLeg);
    modeleArmure.setAllVisible(false);
    modeleArmure.head.visible = true;
    modeleArmure.body.visible = true;
    modeleArmure.leftArm.visible = true;
    modeleArmure.rightArm.visible = true;
    modeleArmure.leftLeg.visible = true;
    modeleArmure.rightLeg.visible = true;

    VertexConsumer raw = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer consumer = new FlowingSpriteVertexConsumer(raw, sprite, 0.0F, 0.0F);

    renderMembre(modeleArmure.head, poseStack, consumer, packedLight);
    renderMembre(modeleArmure.body, poseStack, consumer, packedLight);
    renderMembre(modeleArmure.leftArm, poseStack, consumer, packedLight);
    renderMembre(modeleArmure.rightArm, poseStack, consumer, packedLight);
    renderMembre(modeleArmure.leftLeg, poseStack, consumer, packedLight);
    renderMembre(modeleArmure.rightLeg, poseStack, consumer, packedLight);
  }

  private void renderMembre(ModelPart membre, PoseStack poseStack, VertexConsumer consumer, int packedLight) {
    float facteur = 1.0F + DILATATION;
    membre.xScale = facteur;
    membre.yScale = facteur;
    membre.zScale = facteur;
    membre.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COULEUR);
    membre.xScale = 1.0F;
    membre.yScale = 1.0F;
    membre.zScale = 1.0F;
  }
}
