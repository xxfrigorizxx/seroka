package com.seroka.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.client.renderer.FlowingSpriteVertexConsumer;
import com.seroka.faction.ArmureTerreCastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Couche d'armure de terre pour mobs et entités vivantes non-joueur. */
@OnlyIn(Dist.CLIENT)
public class ArmureTerreLivingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

  private static final float DILATATION = 0.08F;
  private static final int COULEUR = 0xFFFFFFFF;

  public ArmureTerreLivingLayer(RenderLayerParent<T, M> parent) {
    super(parent);
  }

  @Override
  public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
    if (!ArmureTerreCastHelper.estActive(entity)) {
      return;
    }

    BlockState state = ArmureTerreCastHelper.getBlockState(entity);
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getBlockRenderer()
        .getBlockModel(state)
        .getParticleIcon();

    M model = this.getParentModel();
    VertexConsumer raw = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer consumer = new FlowingSpriteVertexConsumer(raw, sprite, 0.0F, 0.0F);

    poseStack.pushPose();
    poseStack.scale(1.0F + DILATATION, 1.0F + DILATATION, 1.0F + DILATATION);
    model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COULEUR);
    poseStack.popPose();
  }
}
