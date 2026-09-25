package com.seroka.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.GolemArmorModel;
import com.seroka.client.renderer.FlowingSpriteVertexConsumer;
import com.seroka.faction.GolemArmorCastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Armure de golem tellurique : coque de roche autour du joueur, tête du joueur incrustée dans le
 * casque et objets tenus déplacés dans les poings du golem.
 */
@OnlyIn(Dist.CLIENT)
public class GolemArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

  private static final int COULEUR = 0xFFFFFFFF;
  /** Les objets sont grossis pour rester proportionnés aux mains du golem. */
  private static final float ITEM_SCALE = 1.8F;

  private final GolemArmorModel golemModel;
  private final PlayerModel<AbstractClientPlayer> headModel;

  public GolemArmorLayer(
      RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
      EntityRendererProvider.Context context
  ) {
    super(parent);
    this.golemModel = new GolemArmorModel(context.bakeLayer(GolemArmorModel.LAYER));
    this.headModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
  }

  @Override
  public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
  ) {
    if (!GolemArmorCastHelper.isActive(player)) {
      return;
    }

    float formProgress = GolemArmorCastHelper.formProgress(player, partialTick);
    BlockState state = GolemArmorCastHelper.getBlockState(player);
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getBlockRenderer()
        .getBlockModel(state)
        .getParticleIcon();

    golemModel.setupAnim(
        player, limbSwing, limbSwingAmount, partialTick, netHeadYaw, headPitch, formProgress
    );

    VertexConsumer raw = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer rocheux = new FlowingSpriteVertexConsumer(raw, sprite, 0.0F, 0.0F);
    golemModel.renderToBuffer(poseStack, rocheux, packedLight, OverlayTexture.NO_OVERLAY, COULEUR);

    if (formProgress >= GolemArmorModel.headRevealProgress()) {
      renderTeteJoueur(poseStack, buffer, packedLight, player);
    }

    renderObjetTenu(poseStack, buffer, packedLight, player, HumanoidArm.RIGHT);
    renderObjetTenu(poseStack, buffer, packedLight, player, HumanoidArm.LEFT);
  }

  /** Remonte la tête du joueur dans la cavité du casque, le reste du corps étant masqué. */
  private void renderTeteJoueur(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player
  ) {
    PlayerModel<AbstractClientPlayer> parent = getParentModel();
    headModel.setAllVisible(false);
    headModel.head.copyFrom(parent.head);
    headModel.hat.copyFrom(parent.hat);
    headModel.head.y += GolemArmorModel.HEAD_LIFT;
    headModel.head.visible = true;
    headModel.hat.visible = true;

    VertexConsumer consumer =
        buffer.getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
    headModel.head.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, COULEUR);
  }

  /** Rend l'objet tenu dans le poing du golem (le rendu vanilla est masqué par mixin). */
  private void renderObjetTenu(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      HumanoidArm arm
  ) {
    ItemStack stack = arm == player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
    if (stack.isEmpty()) {
      return;
    }

    boolean gauche = arm == HumanoidArm.LEFT;

    poseStack.pushPose();
    golemModel.translateToHand(arm, poseStack);
    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    poseStack.translate((gauche ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
    poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

    Minecraft.getInstance().getItemRenderer().renderStatic(
        player,
        stack,
        gauche ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
        gauche,
        poseStack,
        buffer,
        player.level(),
        packedLight,
        OverlayTexture.NO_OVERLAY,
        player.getId()
    );
    poseStack.popPose();
  }
}
