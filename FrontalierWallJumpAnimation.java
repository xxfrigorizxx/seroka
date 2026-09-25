package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.WallJumpMath;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animation de rebond mural — appui figé (frame 1) en chute contre un mur,
 * puis push-off complet au wall jump.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierWallJumpAnimation {

  public static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "wall_jump");

  private static final ResourceLocation WALL_JUMP_ANIMATION_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "wall_jump");

  private static final int GAMEPLAY_PRIORITY = 1500;
  private static final int WALL_JUMP_GAMEPLAY_TICKS = 8;
  private static final int WALL_CONTACT_FRAME = 1;

  private static final Map<UUID, Vec3> PUSH_DIRECTIONS = new ConcurrentHashMap<>();
  private static final Map<UUID, SpeedModifier> SPEED_MODIFIERS = new ConcurrentHashMap<>();
  private static final Set<UUID> CLINGING = ConcurrentHashMap.newKeySet();
  private static final Map<UUID, Long> JUMP_ACTIVE_UNTIL = new ConcurrentHashMap<>();

  private FrontalierWallJumpAnimation() {}

  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
        LAYER_ID,
        GAMEPLAY_PRIORITY,
        player -> {
          ModifierLayer<IAnimation> layer = new ModifierLayer<>();
          SpeedModifier speedModifier = new SpeedModifier();
          speedModifier.speed = 1.0f;
          SPEED_MODIFIERS.put(player.getUUID(), speedModifier);

          RollAdjustmentModifier adjustmentModifier = createAdjustmentModifier(player);
          layer.addModifier(adjustmentModifier, 0);
          layer.addModifier(speedModifier, 0);
          return layer;
        }
    ));
  }

  public static void playRemoteWallJump(AbstractClientPlayer player, Vec3 pushDirection) {
    if (player == null || player.level() == null) {
      return;
    }

    CLINGING.remove(player.getUUID());
    PUSH_DIRECTIONS.put(player.getUUID(), pushDirection);
    JUMP_ACTIVE_UNTIL.put(player.getUUID(), player.level().getGameTime() + WALL_JUMP_GAMEPLAY_TICKS);
    playWallJumpAnimation(player);
  }

  public static void triggerLocalWallJump(AbstractClientPlayer player) {
    if (player == null || player.level() == null) {
      return;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isFrontalier()) {
      return;
    }

    Vec3 pushDirection = WallJumpMath.findWallEscapeDirection(player);
    if (pushDirection == null) {
      return;
    }

    CLINGING.remove(player.getUUID());
    PUSH_DIRECTIONS.put(player.getUUID(), pushDirection);
    JUMP_ACTIVE_UNTIL.put(player.getUUID(), player.level().getGameTime() + WALL_JUMP_GAMEPLAY_TICKS);
    playWallJumpAnimation(player);
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }

    long gameTime = minecraft.level.getGameTime();
    JUMP_ACTIVE_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= gameTime);

    for (AbstractClientPlayer player : minecraft.level.players()) {
      PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
      if (!faction.isFrontalier()) {
        continue;
      }

      UUID id = player.getUUID();
      if (JUMP_ACTIVE_UNTIL.containsKey(id)) {
        continue;
      }

      if (canShowWallCling(player, faction)) {
        Vec3 pushDirection = WallJumpMath.findWallEscapeDirection(player);
        if (pushDirection != null) {
          PUSH_DIRECTIONS.put(id, pushDirection);
          playWallCling(player);
        }
      } else if (CLINGING.remove(id)) {
        stopWallAnimation(player);
        PUSH_DIRECTIONS.remove(id);
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    PUSH_DIRECTIONS.remove(id);
    SPEED_MODIFIERS.remove(id);
    CLINGING.remove(id);
    JUMP_ACTIVE_UNTIL.remove(id);
  }

  private static boolean canShowWallCling(AbstractClientPlayer player, PlayerFaction faction) {
    return !faction.isProne()
        && !faction.isRolling()
        && !player.isInWater()
        && WallJumpMath.isTrulyAirborne(player)
        && player.horizontalCollision
        && WallJumpMath.findWallEscapeDirection(player) != null;
  }

  private static void playWallCling(AbstractClientPlayer player) {
    UUID id = player.getUUID();
    if (CLINGING.contains(id)) {
      return;
    }

    ModifierLayer<IAnimation> layer = getLayer(player);
    KeyframeAnimation animation = loadAnimation();
    if (layer == null || animation == null) {
      return;
    }

    SpeedModifier speedModifier = SPEED_MODIFIERS.get(id);
    if (speedModifier != null) {
      speedModifier.speed = 0.0f;
    }

    layer.replaceAnimationWithFade(
        AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE),
        new KeyframeAnimationPlayer(animation, WALL_CONTACT_FRAME)
    );
    CLINGING.add(id);
  }

  private static void playWallJumpAnimation(AbstractClientPlayer player) {
    ModifierLayer<IAnimation> layer = getLayer(player);
    KeyframeAnimation animation = loadAnimation();
    if (layer == null || animation == null) {
      return;
    }

    KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
    float animationLength = copy.endTick;
    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = animationLength / WALL_JUMP_GAMEPLAY_TICKS;
    }

    layer.replaceAnimationWithFade(
        AbstractFadeModifier.standardFadeIn(copy.beginTick, Ease.INOUTSINE),
        new KeyframeAnimationPlayer(copy.build(), WALL_CONTACT_FRAME)
    );
  }

  private static void stopWallAnimation(AbstractClientPlayer player) {
    ModifierLayer<IAnimation> layer = getLayer(player);
    if (layer == null) {
      return;
    }

    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = 1.0f;
    }
    layer.setAnimation(null);
  }

  private static KeyframeAnimation loadAnimation() {
    var playable = PlayerAnimationRegistry.getAnimation(WALL_JUMP_ANIMATION_ID);
    if (!(playable instanceof KeyframeAnimation animation)) {
      ModMain.LOGGER.error("Animation wall jump introuvable : {}", WALL_JUMP_ANIMATION_ID);
      return null;
    }
    return animation;
  }

  private static RollAdjustmentModifier createAdjustmentModifier(AbstractClientPlayer player) {
    return new RollAdjustmentModifier(partName -> {
      if (!"body".equals(partName)) {
        return Optional.empty();
      }

      Vec3 direction = PUSH_DIRECTIONS.get(player.getUUID());
      if (direction == null) {
        return Optional.empty();
      }

      Vec3 bodyForward = new Vec3(0.0, 0.0, 1.0)
          .yRot((float) Math.toRadians(-player.yBodyRot));
      float angle = (float) angleWithSignBetween(bodyForward, direction, new Vec3(0.0, 1.0, 0.0));
      return Optional.of(new RollAdjustmentModifier.PartModifier(
          new Vec3f(0.0f, (float) Math.toRadians(angle), 0.0f),
          new Vec3f(0.0f, 0.0f, 0.0f)
      ));
    });
  }

  private static double angleWithSignBetween(Vec3 a, Vec3 b, Vec3 planeNormal) {
    double cosineTheta = a.dot(b) / (a.length() * b.length());
    double angle = Math.toDegrees(Math.acos(cosineTheta));
    angle *= Math.signum(a.cross(b).dot(planeNormal));
    return Double.isNaN(angle) ? 0.0 : angle;
  }

  @SuppressWarnings("unchecked")
  private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
    return (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
  }
}
