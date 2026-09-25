package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.WaterSlideBootsModel;
import com.seroka.entity.WaterSlideEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Bottes d'eau translucides suivant les animations de marche du joueur. */
public class WaterSlideRenderer extends EntityRenderer<WaterSlideEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");
  private static final int WATER_COLOR = 0xCC8CD2FF;
  /** 16 unités modèle = 1 bloc (même convention que le mur d'eau). */
  private static final float MODEL_SCALE = 1.0F;

  private final WaterSlideBootsModel model;

  public WaterSlideRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WaterSlideBootsModel(context.bakeLayer(WaterSlideBootsModel.LAYER));
  }

  @Override
  public void render(
      WaterSlideEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    LivingEntity target = resolveTarget(entity);
    if (target == null) {
      return;
    }

    float limbSwing = target.walkAnimation.position();
    float limbAmount = target.walkAnimation.speed();
    float bodyYaw = target.yBodyRot;

    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(WATER_FLOW_SPRITE);
    float scroll = (entity.tickCount + partialTick) * 0.04F;

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
    poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

    model.setupAnim(entity, limbSwing, limbAmount, entity.tickCount + partialTick, bodyYaw, target.getXRot());

    VertexConsumer raw = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer vertices = new FlowingSpriteVertexConsumer(raw, sprite, scroll, scroll * 0.5F);
    model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, WATER_COLOR);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private static LivingEntity resolveTarget(WaterSlideEntity entity) {
    LivingEntity target = entity.getTarget();
    if (target != null) {
      return target;
    }
    Player local = Minecraft.getInstance().player;
    if (local == null) {
      return null;
    }
    java.util.UUID targetUuid = entity.getTargetUuid();
    if (targetUuid != null && targetUuid.equals(local.getUUID())) {
      return local;
    }
    return null;
  }

  @Override
  public ResourceLocation getTextureLocation(WaterSlideEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
