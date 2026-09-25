package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.ThrownSwordEntity;
import com.seroka.faction.DagueInfinieHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Affiche l'épée orientée comme une flèche : pointe de la lame en avant.
 */
public class ThrownSwordRenderer extends EntityRenderer<ThrownSwordEntity> {

  private static final float MODEL_SCALE = 0.55F;
  private static final float DAGGER_SCALE = 0.42F;
  /** Recul du modèle pour aligner la pointe sur la position de l'entité. */
  private static final float TIP_PULLBACK = 1.0F;

  private final ItemRenderer itemRenderer;

  public ThrownSwordRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemRenderer = context.getItemRenderer();
  }

  @Override
  public void render(
      ThrownSwordEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    ItemStack stack = entity.getSwordStack();
    if (stack.isEmpty()) {
      return;
    }

    poseStack.pushPose();

    float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
    float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

    // Rotations identiques à ArrowRenderer, puis alignement du modèle d'épée (FIXED = lame sur -Y).
    poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(xRot));

    float shake = entity.shakeTime - partialTick;
    if (shake > 0.0F) {
      poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
    }

    poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    float scale = DagueInfinieHelper.isDagueInfinie(stack) ? DAGGER_SCALE : MODEL_SCALE;
    poseStack.scale(scale, scale, scale);
    poseStack.translate(-TIP_PULLBACK, 0.0F, 0.0F);

    this.itemRenderer.renderStatic(
        stack,
        ItemDisplayContext.FIXED,
        packedLight,
        OverlayTexture.NO_OVERLAY,
        poseStack,
        buffer,
        entity.level(),
        entity.getId()
    );
    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(ThrownSwordEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
