package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seroka.entity.BouletCanonEntity;
import com.seroka.navire.MunitionCanon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Rendu du boulet : le bloc tiré s'il en est un, l'objet à plat sinon.
 *
 * <p>Le projectile tourne sur lui-même le long de sa route, ce qui donne à la parabole sa lisibilité
 * : on voit d'un coup d'œil s'il monte encore ou s'il retombe.
 */
public class BouletCanonRenderer extends EntityRenderer<BouletCanonEntity> {

  private static final float TAILLE_BLOC = 0.65F;
  private static final float TOURS_PAR_TICK = 14.0F;

  private final BlockRenderDispatcher blockRenderer;
  private final ItemRenderer itemRenderer;

  public BouletCanonRenderer(EntityRendererProvider.Context contexte) {
    super(contexte);
    this.shadowRadius = 0.3F;
    this.blockRenderer = contexte.getBlockRenderDispatcher();
    this.itemRenderer = Minecraft.getInstance().getItemRenderer();
  }

  @Override
  public void render(BouletCanonEntity boulet, float yaw, float partialTick, PoseStack poseStack,
      MultiBufferSource buffer, int packedLight) {
    ItemStack munition = boulet.munition();
    if (munition.isEmpty()) {
      super.render(boulet, yaw, partialTick, poseStack, buffer, packedLight);
      return;
    }

    poseStack.pushPose();
    float rotation = (boulet.tickCount + partialTick) * TOURS_PAR_TICK;
    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees(boulet.getYRot())));
    poseStack.mulPose(Axis.XP.rotationDegrees(rotation));

    BlockState pose = MunitionCanon.etatPose(munition);
    if (pose != null && pose.getRenderShape() == RenderShape.MODEL) {
      poseStack.scale(TAILLE_BLOC, TAILLE_BLOC, TAILLE_BLOC);
      poseStack.translate(-0.5D, -0.5D, -0.5D);
      blockRenderer.renderSingleBlock(pose, poseStack, buffer, packedLight,
          OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
    } else {
      itemRenderer.renderStatic(munition, ItemDisplayContext.GROUND, packedLight,
          OverlayTexture.NO_OVERLAY, poseStack, buffer, boulet.level(), boulet.getId());
    }

    poseStack.popPose();
    super.render(boulet, yaw, partialTick, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(BouletCanonEntity boulet) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
