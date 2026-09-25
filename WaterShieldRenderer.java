package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.WaterShieldModel;
import com.seroka.entity.WaterShieldEntity;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Mur d'eau voxel — texture eau courante animée (atlas des blocs). */
public class WaterShieldRenderer extends EntityRenderer<WaterShieldEntity> {

  private static final ResourceLocation WATER_FLOW_SPRITE =
      ResourceLocation.withDefaultNamespace("block/water_flow");
  private static final float MODEL_SCALE = 1.15F;
  private static final int WATER_COLOR = 0xCC8CD2FF;

  private final WaterShieldModel model;

  public WaterShieldRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new WaterShieldModel(context.bakeLayer(WaterShieldModel.LAYER));
  }

  @Override
  public void render(
      WaterShieldEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
  ) {
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(WATER_FLOW_SPRITE);

    float scroll = (entity.tickCount + partialTick) * 0.035F;

    poseStack.pushPose();
    applyLookRotation(entity, entityYaw, partialTick, poseStack);
    poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

    model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);

    VertexConsumer raw = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer vertices = new FlowingSpriteVertexConsumer(raw, sprite, 0.0F, scroll);
    model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, WATER_COLOR);

    poseStack.popPose();
    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  private static void applyLookRotation(
      WaterShieldEntity entity,
      float entityYaw,
      float partialTick,
      PoseStack poseStack
  ) {
    poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 180.0F));

    Vec3 look = horizontalLook(entity, partialTick);
    Vector3f forward = new Vector3f((float) look.x, 0.0F, (float) look.z);
    if (forward.lengthSquared() < 1.0E-6F) {
      forward.set(0.0F, 0.0F, 1.0F);
    } else {
      forward.normalize();
    }
    poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), forward));
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
  }

  private static Vec3 horizontalLook(WaterShieldEntity entity, float partialTick) {
    LivingEntity owner = entity.getOwner();
    if (owner != null) {
      Vec3 view = owner.getViewVector(partialTick);
      double lenSqr = view.x * view.x + view.z * view.z;
      if (lenSqr > 1.0E-8) {
        double inv = 1.0D / Math.sqrt(lenSqr);
        return new Vec3(view.x * inv, 0.0D, view.z * inv);
      }
    }
    return entity.getLookDirection(partialTick);
  }

  @Override
  public ResourceLocation getTextureLocation(WaterShieldEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
