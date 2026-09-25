package com.seroka.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.seroka.ModMain;
import com.seroka.client.organic.OrganicEvolutionClient;
import com.seroka.organic.IEvolvingMob;
import com.seroka.organic.OrganicEvolutionData;
import com.seroka.organic.OrganicLevelCurve;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;

/**
 * Niveau organique : pas de nametag vanilla, affichage dynamique sur la cible du
 * réticule (≤ 15 blocs).
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class MobLevelTagRenderer {

  private static final double MAX_TARGET_DISTANCE = 15.0D;

  private static final float CAMERA_PUSH = 0.25F;
  private static final float BAR_WIDTH = 48.0F;
  private static final float BAR_HEIGHT = 4.0F;
  private static final float BAR_PAD = 0.6F;
  private static final float BAR_OFFSET_Y = 10.0F;

  private static final int COLOR_BAR_BACKGROUND = 0xC0101010;
  private static final int COLOR_BAR_BORDER = 0xE0000000;

  private MobLevelTagRenderer() {}

  @SubscribeEvent
  public static void onRenderNameTag(RenderNameTagEvent event) {
    if (OrganicEvolutionClient.isEvolvingMob(event.getEntity())) {
      event.setCanRender(TriState.FALSE);
    }
  }

  @SubscribeEvent
  public static void onRenderLevel(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
      return;
    }

    Entity picked = minecraft.crosshairPickEntity;
    if (picked == null && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
      picked = entityHit.getEntity();
    }
    IEvolvingMob evolving = OrganicEvolutionClient.asEvolvingMob(picked);
    if (evolving == null || !(picked instanceof Mob mob)) {
      return;
    }

    if (mob.isInvisibleTo(minecraft.player) || mob == minecraft.getCameraEntity()) {
      return;
    }
    if (minecraft.player.distanceTo(mob) > MAX_TARGET_DISTANCE) {
      return;
    }

    float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
    Vec3 camera = event.getCamera().getPosition();
    Vec3 anchor = mob.getAttachments()
        .getNullable(EntityAttachment.NAME_TAG, 0, mob.getViewYRot(partialTick));
    double x = Mth.lerp(partialTick, mob.xOld, mob.getX());
    double y = Mth.lerp(partialTick, mob.yOld, mob.getY())
        + (anchor != null ? anchor.y : mob.getBbHeight()) + 0.55D;
    double z = Mth.lerp(partialTick, mob.zOld, mob.getZ());

    OrganicEvolutionData data = evolving.data();
    PoseStack pose = event.getPoseStack();
    pose.pushPose();
    pose.translate(x - camera.x, y - camera.y, z - camera.z);
    pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
    pose.translate(0.0F, 0.0F, CAMERA_PUSH);
    pose.scale(0.025F, -0.025F, 0.025F);

    Matrix4f matrix = pose.last().pose();
    MultiBufferSource buffer = minecraft.renderBuffers().bufferSource();
    int packedLight = minecraft.getEntityRenderDispatcher().getPackedLightCoords(mob, partialTick);

    int textColor = levelTextColorArgb(data.level());
    drawLabel(minecraft.font, matrix, buffer, packedLight, data, textColor);
    drawProgressBar(matrix, buffer, data, barFillColor(data.level()));

    pose.popPose();
  }

  static int levelTextColorArgb(int level) {
    Integer rgb = levelStyle(level).getColor();
    return rgb != null ? 0xFF000000 | rgb : 0xFFE9B44C;
  }

  private static ChatFormatting levelStyle(int level) {
    if (level >= 50) {
      return ChatFormatting.RED;
    }
    if (level >= 40) {
      return ChatFormatting.GOLD;
    }
    if (level >= 30) {
      return ChatFormatting.LIGHT_PURPLE;
    }
    if (level >= 20) {
      return ChatFormatting.AQUA;
    }
    if (level >= 10) {
      return ChatFormatting.GREEN;
    }
    return ChatFormatting.YELLOW;
  }

  private static int barFillColor(int level) {
    return levelTextColorArgb(level) & 0x00FFFFFF | 0xFF000000;
  }

  private static void drawLabel(
      Font font,
      Matrix4f matrix,
      MultiBufferSource buffer,
      int packedLight,
      OrganicEvolutionData data,
      int textColor
  ) {
    MutableComponent label = Component.literal("◆ ")
        .withStyle(levelStyle(data.level()))
        .append(Component.translatable("organic.seroka.level_tag", data.level()).withStyle(levelStyle(data.level())));
    float x = -font.width(label) / 2.0F;
    font.drawInBatch(
        label, x, 0.0F, textColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    font.drawInBatch(
        label, x, 0.0F, textColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
  }

  private static void drawProgressBar(Matrix4f matrix, MultiBufferSource buffer, OrganicEvolutionData data, int fillArgb) {
    float left = -BAR_WIDTH / 2.0F;
    float right = left + BAR_WIDTH;
    float top = BAR_OFFSET_Y;
    float bottom = top + BAR_HEIGHT;
    float filled = (float) (BAR_WIDTH * OrganicLevelCurve.progress(data));
    int light = LightTexture.FULL_BRIGHT;

    VertexConsumer consumer = buffer.getBuffer(RenderType.textBackgroundSeeThrough());

    if (filled > 0.0F) {
      doubleSidedQuad(consumer, matrix, light, left, top, filled, BAR_HEIGHT, fillArgb);
    }
    if (filled < BAR_WIDTH) {
      doubleSidedQuad(
          consumer, matrix, light, left + filled, top, BAR_WIDTH - filled, BAR_HEIGHT, COLOR_BAR_BACKGROUND);
    }

    doubleSidedQuad(
        consumer, matrix, light, left - BAR_PAD, top - BAR_PAD, BAR_WIDTH + BAR_PAD * 2.0F, BAR_PAD, COLOR_BAR_BORDER);
    doubleSidedQuad(
        consumer, matrix, light, left - BAR_PAD, bottom, BAR_WIDTH + BAR_PAD * 2.0F, BAR_PAD, COLOR_BAR_BORDER);
    doubleSidedQuad(consumer, matrix, light, left - BAR_PAD, top, BAR_PAD, BAR_HEIGHT, COLOR_BAR_BORDER);
    doubleSidedQuad(consumer, matrix, light, right, top, BAR_PAD, BAR_HEIGHT, COLOR_BAR_BORDER);
  }

  private static void doubleSidedQuad(
      VertexConsumer consumer,
      Matrix4f matrix,
      int light,
      float left,
      float top,
      float width,
      float height,
      int argb
  ) {
    quad(consumer, matrix, light, left, top, width, height, argb, false);
    quad(consumer, matrix, light, left, top, width, height, argb, true);
  }

  private static void quad(
      VertexConsumer consumer,
      Matrix4f matrix,
      int light,
      float left,
      float top,
      float width,
      float height,
      int argb,
      boolean reverse
  ) {
    int alpha = argb >>> 24;
    int red = argb >> 16 & 0xFF;
    int green = argb >> 8 & 0xFF;
    int blue = argb & 0xFF;
    float right = left + width;
    float bottom = top + height;

    if (reverse) {
      vertex(consumer, matrix, light, left, top, red, green, blue, alpha);
      vertex(consumer, matrix, light, right, top, red, green, blue, alpha);
      vertex(consumer, matrix, light, right, bottom, red, green, blue, alpha);
      vertex(consumer, matrix, light, left, bottom, red, green, blue, alpha);
      return;
    }

    vertex(consumer, matrix, light, left, bottom, red, green, blue, alpha);
    vertex(consumer, matrix, light, right, bottom, red, green, blue, alpha);
    vertex(consumer, matrix, light, right, top, red, green, blue, alpha);
    vertex(consumer, matrix, light, left, top, red, green, blue, alpha);
  }

  private static void vertex(
      VertexConsumer consumer,
      Matrix4f matrix,
      int light,
      float x,
      float y,
      int red,
      int green,
      int blue,
      int alpha
  ) {
    consumer.addVertex(matrix, x, y, 0.0F).setColor(red, green, blue, alpha).setLight(light);
  }
}
