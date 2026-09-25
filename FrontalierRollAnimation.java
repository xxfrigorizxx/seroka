package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.faction.FactionProneHandler;
import com.seroka.faction.FrontalierCombatVisual;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.RollMath;
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
import net.minecraft.util.Mth;
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
 * Roulade tactique via Player Animation Library.
 * Animation JSON empruntée à Combat Roll (Daedelus / ZsoltMolnarrr, MIT).
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierRollAnimation {

  public static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "tactical_roll");

  private static final ResourceLocation ROLL_ANIMATION_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "roll");

  private static final int GAMEPLAY_PRIORITY = 1500;

  private static final Map<UUID, Integer> LAST_ROLL_TICKS = new ConcurrentHashMap<>();
  private static final Set<UUID> ANIMATION_PLAYED = ConcurrentHashMap.newKeySet();
  private static final Map<UUID, Vec3> ROLL_DIRECTIONS = new ConcurrentHashMap<>();
  private static final Map<UUID, SpeedModifier> SPEED_MODIFIERS = new ConcurrentHashMap<>();

  private FrontalierRollAnimation() {}

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

  public static void triggerLocalRoll(AbstractClientPlayer player) {
    if (player == null) {
      return;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isFrontalier() || faction.isRolling()) {
      return;
    }

    Vec3 direction = RollMath.getMovementDirection(player);
    float rollYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    player.setData(ModAttachments.PLAYER_FACTION, faction.startRoll(rollYaw));
    FactionProneHandler.forceStanding(player);
    ROLL_DIRECTIONS.put(player.getUUID(), direction);
    playRollAnimation(player);
    ANIMATION_PLAYED.add(player.getUUID());
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }

    for (AbstractClientPlayer player : minecraft.level.players()) {
      FrontalierCombatVisual visual = combatVisual(player, minecraft);
      int rollTicks = visual.rollTicks();
      int previousRollTicks = LAST_ROLL_TICKS.getOrDefault(player.getUUID(), 0);
      LAST_ROLL_TICKS.put(player.getUUID(), rollTicks);

      if (!visual.frontalier()) {
        continue;
      }

      if (rollTicks <= 0) {
        if (previousRollTicks > 0) {
          FactionProneHandler.forceStanding(player);
        }
        ANIMATION_PLAYED.remove(player.getUUID());
        ROLL_DIRECTIONS.remove(player.getUUID());
        continue;
      }

      if (!ROLL_DIRECTIONS.containsKey(player.getUUID())) {
        ROLL_DIRECTIONS.put(player.getUUID(), directionFromYaw(visual.rollYaw()));
      }

      if (previousRollTicks <= 0 && rollTicks > 0 && ANIMATION_PLAYED.add(player.getUUID())) {
        playRollAnimation(player);
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    LAST_ROLL_TICKS.remove(id);
    ANIMATION_PLAYED.remove(id);
    ROLL_DIRECTIONS.remove(id);
    SPEED_MODIFIERS.remove(id);
  }

  private static void playRollAnimation(AbstractClientPlayer player) {
    ModifierLayer<IAnimation> layer = getLayer(player);
    if (layer == null) {
      ModMain.LOGGER.warn("Layer d'animation absent pour {}", player.getGameProfile().getName());
      return;
    }

    var playable = PlayerAnimationRegistry.getAnimation(ROLL_ANIMATION_ID);
    if (!(playable instanceof KeyframeAnimation animation)) {
      ModMain.LOGGER.error("Animation introuvable : {}", ROLL_ANIMATION_ID);
      return;
    }

    KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
    float animationLength = copy.endTick;
    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = animationLength / PlayerFaction.ROLL_DURATION_TICKS;
    }

    layer.replaceAnimationWithFade(
        AbstractFadeModifier.standardFadeIn(copy.beginTick, Ease.INOUTSINE),
        new KeyframeAnimationPlayer(copy.build(), 0)
    );
  }

  private static RollAdjustmentModifier createAdjustmentModifier(AbstractClientPlayer player) {
    return new RollAdjustmentModifier(partName -> {
      if (!"body".equals(partName)) {
        return Optional.empty();
      }

      Vec3 direction = ROLL_DIRECTIONS.get(player.getUUID());
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

  private static Vec3 directionFromYaw(float yawDegrees) {
    float radians = yawDegrees * ((float) Math.PI / 180.0f);
    return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
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

  private static FrontalierCombatVisual combatVisual(AbstractClientPlayer player, Minecraft minecraft) {
    if (player == minecraft.player) {
      return FrontalierCombatVisual.from(player.getData(ModAttachments.PLAYER_FACTION));
    }
    return player.getData(ModAttachments.FRONTALIER_COMBAT_VISUAL);
  }
}
