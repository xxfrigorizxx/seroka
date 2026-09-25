package com.seroka.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.ModMain;
import com.seroka.client.renderer.FlowingSpriteVertexConsumer;
import com.seroka.faction.GanteletsTerreCastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/** Rendu première personne des gantelets de terre sur les bras. */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class GanteletsTerreClientHandler {

  private static final float DILATATION = 0.25F;
  private static final int COULEUR = 0xFFFFFFFF;

  /** Évite la récursion renderRightHand → RenderArmEvent → renderRightHand. */
  private static final ThreadLocal<Boolean> RENDERING_GAUNTLET_ARM = ThreadLocal.withInitial(() -> false);

  private GanteletsTerreClientHandler() {}

  @SubscribeEvent
  public static void onRenderArm(RenderArmEvent event) {
    if (RENDERING_GAUNTLET_ARM.get()) {
      return;
    }

    AbstractClientPlayer player = event.getPlayer();
    if (!GanteletsTerreCastHelper.estActif(player)) {
      return;
    }

    event.setCanceled(true);

    Minecraft minecraft = Minecraft.getInstance();
    PlayerRenderer playerRenderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
    PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
    HumanoidArm side = event.getArm();
    ModelPart armPart = side == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
    ModelPart sleevePart = side == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;

    PoseStack poseStack = event.getPoseStack();
    MultiBufferSource buffer = event.getMultiBufferSource();
    int packedLight = event.getPackedLight();

    RENDERING_GAUNTLET_ARM.set(true);
    try {
      if (side == HumanoidArm.RIGHT) {
        playerRenderer.renderRightHand(poseStack, buffer, packedLight, player);
      } else {
        playerRenderer.renderLeftHand(poseStack, buffer, packedLight, player);
      }
    } finally {
      RENDERING_GAUNTLET_ARM.set(false);
    }

    BlockState state = GanteletsTerreCastHelper.getBlockState(player);
    TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModel(state).getParticleIcon();
    VertexConsumer raw = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer consumer = new FlowingSpriteVertexConsumer(raw, sprite, 0.0F, 0.0F);

    renderGauntletMembre(armPart, poseStack, consumer, packedLight);
    renderGauntletMembre(sleevePart, poseStack, consumer, packedLight);
  }

  private static void renderGauntletMembre(
      ModelPart membre,
      PoseStack poseStack,
      VertexConsumer consumer,
      int packedLight
  ) {
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
