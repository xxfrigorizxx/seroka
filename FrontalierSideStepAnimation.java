package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.faction.FrontalierCombatVisual;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pas de côté Frontalier via Player Animation Library.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierSideStepAnimation {

  public static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "side_step");

  private static final ResourceLocation LEFT_ANIMATION_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "side_step_left");

  private static final ResourceLocation RIGHT_ANIMATION_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "side_step_right");

  private static final int GAMEPLAY_PRIORITY = 1500;

  private static final Map<UUID, Integer> LAST_SIDE_STEP_TICKS = new ConcurrentHashMap<>();
  private static final Set<UUID> ANIMATION_PLAYED = ConcurrentHashMap.newKeySet();
  private static final Map<UUID, SpeedModifier> SPEED_MODIFIERS = new ConcurrentHashMap<>();

  private FrontalierSideStepAnimation() {}

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
          layer.addModifier(speedModifier, 0);
          return layer;
        }
    ));
  }

  public static void triggerLocalSideStep(AbstractClientPlayer player, boolean left) {
    if (player == null) {
      return;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isFrontalier()) {
      return;
    }

    playSideStepAnimation(player, left);
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
      int sideStepTicks = visual.sideStepTicks();
      int previousSideStepTicks = LAST_SIDE_STEP_TICKS.getOrDefault(player.getUUID(), 0);
      LAST_SIDE_STEP_TICKS.put(player.getUUID(), sideStepTicks);

      if (!visual.frontalier()) {
        continue;
      }

      if (sideStepTicks <= 0) {
        ANIMATION_PLAYED.remove(player.getUUID());
        continue;
      }

      if (previousSideStepTicks <= 0 && sideStepTicks > 0 && ANIMATION_PLAYED.add(player.getUUID())) {
        playSideStepAnimation(player, visual.sideStepLeft());
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    LAST_SIDE_STEP_TICKS.remove(id);
    ANIMATION_PLAYED.remove(id);
    SPEED_MODIFIERS.remove(id);
  }

  private static void playSideStepAnimation(AbstractClientPlayer player, boolean left) {
    ModifierLayer<IAnimation> layer = getLayer(player);
    if (layer == null) {
      ModMain.LOGGER.warn("Layer side step absent pour {}", player.getGameProfile().getName());
      return;
    }

    ResourceLocation animationId = left ? LEFT_ANIMATION_ID : RIGHT_ANIMATION_ID;
    var playable = PlayerAnimationRegistry.getAnimation(animationId);
    if (!(playable instanceof KeyframeAnimation animation)) {
      ModMain.LOGGER.error("Animation side step introuvable : {}", animationId);
      return;
    }

    KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
    float animationLength = copy.endTick;
    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = animationLength / PlayerFaction.SIDE_STEP_ANIM_TICKS;
    }

    layer.replaceAnimationWithFade(
        AbstractFadeModifier.standardFadeIn(copy.beginTick, Ease.INOUTSINE),
        new KeyframeAnimationPlayer(copy.build(), 0)
    );
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
