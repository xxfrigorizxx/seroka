package com.seroka.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.seroka.client.model.EarthSlideBootsModel;
import com.seroka.entity.EarthSlideEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Planche tellurique animée sous le joueur, texturée avec le sol solide qu'il survole. */
public class EarthSlideRenderer extends EntityRenderer<EarthSlideEntity> {

  private static final float MODEL_SCALE = 1.0F;
  private static final int EARTH_TINT = 0xFFFFFFFF;
  private static final int GROUND_SCAN_DEPTH = 4;
  private static final double MOVING_SPEED_SQR = 0.004D;

  private final EarthSlideBootsModel model;

  public EarthSlideRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new EarthSlideBootsModel(context.bakeLayer(EarthSlideBootsModel.LAYER));
  }

  @Override
  public void render(
      EarthSlideEntity entity,
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
    float bodyYaw = Mth.rotLerp(partialTick, target.yBodyRotO, target.yBodyRot);
    Vec3 velocity = target.getDeltaMovement();
    double speedSqr = velocity.horizontalDistanceSqr();
    boolean moving = speedSqr > MOVING_SPEED_SQR;

    BlockState ground = resolveGroundState(entity, target);
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getBlockRenderer()
        .getBlockModel(ground)
        .getParticleIcon();
    float scroll = (entity.tickCount + partialTick) * (moving ? 0.05F : 0.015F);

    float yawRad = bodyYaw * Mth.DEG_TO_RAD;
    double lateral = velocity.x * Mth.cos(yawRad) + velocity.z * Mth.sin(yawRad);
    float roll = (float) Mth.clamp(-lateral * 1.4D, -0.35D, 0.35D);
    float pitch = -(float) Mth.clamp(Math.sqrt(speedSqr) * 0.6D, 0.0D, 0.18D);

    poseStack.pushPose();
    // L'entité se cale sur les ticks, le joueur est rendu interpolé : on recale sur ce dernier.
    poseStack.translate(
        Mth.lerp(partialTick, target.xOld, target.getX()) - entity.getX(),
        Mth.lerp(partialTick, target.yOld, target.getY()) - entity.getY() + 0.02D,
        Mth.lerp(partialTick, target.zOld, target.getZ()) - entity.getZ()
    );
    poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
    poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

    model.setSurfPose(roll, pitch, moving);
    model.setupAnim(entity, limbSwing, limbAmount, entity.tickCount + partialTick, bodyYaw, target.getXRot());

    VertexConsumer raw = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
    VertexConsumer vertices = new FlowingSpriteVertexConsumer(raw, sprite, scroll, scroll * 0.4F);
    model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, EARTH_TINT);

    poseStack.popPose();

    if (moving) {
      spawnClientTrailParticles(target, entity, ground, velocity);
    }

    super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
  }

  /**
   * Cherche le premier bloc plein sous les pieds. Les végétaux (fleurs, hautes herbes) n'ont pas de
   * boîte de collision et sont ignorés, sinon la planche prendrait leur texture.
   */
  private static BlockState resolveGroundState(EarthSlideEntity entity, LivingEntity target) {
    Level level = target.level();
    BlockPos feet = target.blockPosition();
    for (int offset = 0; offset <= GROUND_SCAN_DEPTH; offset++) {
      BlockPos pos = feet.below(offset);
      BlockState state = level.getBlockState(pos);
      if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
        return state;
      }
    }

    int blockId = entity.getGroundBlockId();
    if (blockId > 0) {
      var block = BuiltInRegistries.BLOCK.byId(blockId);
      if (block != null && !block.defaultBlockState().isAir()) {
        return block.defaultBlockState();
      }
    }
    return Blocks.STONE.defaultBlockState();
  }

  private static void spawnClientTrailParticles(
      LivingEntity target,
      EarthSlideEntity entity,
      BlockState ground,
      Vec3 velocity
  ) {
    if (entity.tickCount % 3 != 0) {
      return;
    }
    Level level = target.level();
    Vec3 feet = target.position();
    double trailX = -velocity.x * 0.25D;
    double trailZ = -velocity.z * 0.25D;
    double spread = target.getBbWidth() * 0.4D;

    for (int i = 0; i < 2; i++) {
      double offsetX = (level.random.nextDouble() - 0.5D) * spread;
      double offsetZ = (level.random.nextDouble() - 0.5D) * spread;
      level.addParticle(
          new BlockParticleOption(ParticleTypes.BLOCK, ground),
          feet.x + offsetX + trailX,
          feet.y + 0.1D,
          feet.z + offsetZ + trailZ,
          trailX * 0.5D,
          0.05D,
          trailZ * 0.5D
      );
    }
  }

  private static LivingEntity resolveTarget(EarthSlideEntity entity) {
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
  public ResourceLocation getTextureLocation(EarthSlideEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }
}
