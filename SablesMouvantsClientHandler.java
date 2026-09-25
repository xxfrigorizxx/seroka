package com.seroka.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seroka.ModMain;
import com.seroka.faction.SablesMouvantsCastHelper;
import com.seroka.network.payload.SablesMouvantsSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tremblement visuel des blocs de sables mouvants (rendu client). */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class SablesMouvantsClientHandler {

  private record ActiveZone(
      BlockPos centre,
      long startGameTime,
      long expireGameTime,
      List<SablesMouvantsSyncPayload.ColumnEntry> columns
  ) {}

  private static final List<ActiveZone> ACTIVE_ZONES = new ArrayList<>();

  private SablesMouvantsClientHandler() {}

  public static void applySync(SablesMouvantsSyncPayload payload) {
    BlockPos centre = new BlockPos(payload.centreX(), payload.centreY(), payload.centreZ());

    if (payload.action() == SablesMouvantsSyncPayload.ACTION_END) {
      ACTIVE_ZONES.removeIf(zone -> zone.centre().equals(centre));
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }

    long start = minecraft.level.getGameTime();
    ACTIVE_ZONES.removeIf(zone -> zone.centre().equals(centre));
    ACTIVE_ZONES.add(new ActiveZone(
        centre,
        start,
        start + payload.durationTicks(),
        new ArrayList<>(payload.columns())
    ));
  }

  @SubscribeEvent
  public static void onRenderLevel(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
      return;
    }

    if (ACTIVE_ZONES.isEmpty()) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      ACTIVE_ZONES.clear();
      return;
    }

    long gameTime = minecraft.level.getGameTime();
    Iterator<ActiveZone> iterator = ACTIVE_ZONES.iterator();
    while (iterator.hasNext()) {
      ActiveZone zone = iterator.next();
      if (gameTime >= zone.expireGameTime()) {
        iterator.remove();
      }
    }

    if (ACTIVE_ZONES.isEmpty()) {
      return;
    }

    BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
    MultiBufferSource buffer = minecraft.renderBuffers().bufferSource();
    Vec3 camera = event.getCamera().getPosition();
    PoseStack poseStack = event.getPoseStack();

    poseStack.pushPose();
    poseStack.translate(-camera.x, -camera.y, -camera.z);

    for (ActiveZone zone : ACTIVE_ZONES) {
      long age = gameTime - zone.startGameTime();
      float sinkProgress = Mth.clamp((float) age / SablesMouvantsCastHelper.SINK_ANIMATION_TICKS, 0.0F, 1.0F);

      for (SablesMouvantsSyncPayload.ColumnEntry entry : zone.columns()) {
        Block block = BuiltInRegistries.BLOCK.byId(entry.blockId());
        if (block == null) {
          continue;
        }
        BlockState state = block.defaultBlockState();
        BlockPos pos = entry.pos();
        BlockState worldState = minecraft.level.getBlockState(pos);
        if (!worldState.isAir() && worldState.getBlock() == block) {
          state = worldState;
        } else {
          BlockState sunkState = minecraft.level.getBlockState(pos.below());
          if (!sunkState.isAir() && sunkState.getBlock() == block) {
            state = sunkState;
          }
        }

        float shakeX = Mth.sin((float) gameTime * 0.55F + pos.getX() * 0.7F) * SablesMouvantsCastHelper.SHAKE_AMPLITUDE;
        float shakeZ = Mth.cos((float) gameTime * 0.48F + pos.getZ() * 0.6F) * SablesMouvantsCastHelper.SHAKE_AMPLITUDE;
        float shakeY = Mth.sin((float) gameTime * 0.72F + pos.getY() * 0.4F) * SablesMouvantsCastHelper.SHAKE_AMPLITUDE_Y;
        float sinkOffset = -sinkProgress;

        BlockPos renderPos = new BlockPos(
            pos.getX(),
            Mth.floor(pos.getY() + sinkOffset),
            pos.getZ()
        );
        float subSink = pos.getY() + sinkOffset - renderPos.getY();

        poseStack.pushPose();
        poseStack.translate(
            pos.getX() + shakeX,
            pos.getY() + subSink + shakeY,
            pos.getZ() + shakeZ
        );

        int light = surfaceLight(minecraft, pos);
        blockRenderer.renderSingleBlock(
            state,
            poseStack,
            buffer,
            light,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            RenderType.cutout()
        );
        poseStack.popPose();
      }
    }

    poseStack.popPose();
  }

  /** Lumière du ciel au-dessus du trou — évite le noir quand le bloc est rendu sous terre. */
  private static int surfaceLight(Minecraft minecraft, BlockPos surfacePos) {
    BlockPos sample = surfacePos.above();
    while (sample.getY() < minecraft.level.getMaxBuildHeight()) {
      if (minecraft.level.getBlockState(sample).isAir()) {
        break;
      }
      sample = sample.above();
    }
    int light = LevelRenderer.getLightColor(minecraft.level, sample);
    if ((light & 0xFF) == 0) {
      light = LevelRenderer.getLightColor(minecraft.level, surfacePos.above());
    }
    return light;
  }
}
