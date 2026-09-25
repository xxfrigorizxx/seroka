package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.faction.DualBladesComboState;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enchaînement KosmX 3 coups — Doubles Lames (3e personne).
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierDualBladesAnimation {

  public static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "dual_blades");

  private static final ResourceLocation[] COMBO_ANIMATION_IDS = {
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "dual_blades_combo_1"),
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "dual_blades_combo_2"),
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "dual_blades_combo_3"),
  };

  private static final int[] COMBO_TICKS = {10, 10, 12};

  private static final int GAMEPLAY_PRIORITY = 2000;

  private static final Map<UUID, SpeedModifier> SPEED_MODIFIERS = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> ACTIVE_SWING_TICKS = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> ACTIVE_COMBO_STEP = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> ACTIVE_SWING_DURATION = new ConcurrentHashMap<>();

  private FrontalierDualBladesAnimation() {}

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

  public static boolean isSwingAnimating(Player player) {
    return player != null && ACTIVE_SWING_TICKS.getOrDefault(player.getUUID(), 0) > 0;
  }

  public static int getActiveComboStep(Player player) {
    return ACTIVE_COMBO_STEP.getOrDefault(player.getUUID(), DualBladesComboState.STEP_RIGHT_SLASH);
  }

  /** 0.0 = début du coup, 1.0 = fin. */
  public static float getSwingProgress(Player player) {
    if (player == null || !isSwingAnimating(player)) {
      return 0.0f;
    }
    UUID id = player.getUUID();
    int remaining = ACTIVE_SWING_TICKS.getOrDefault(id, 0);
    int duration = ACTIVE_SWING_DURATION.getOrDefault(id, COMBO_TICKS[0]);
    if (duration <= 0) {
      return 0.0f;
    }
    return 1.0f - (remaining / (float) duration);
  }

  public static void playComboSwing(AbstractClientPlayer player, int comboStep) {
    if (player == null || comboStep < 0 || comboStep >= DualBladesComboState.COMBO_LENGTH) {
      return;
    }

    ModifierLayer<IAnimation> layer = getLayer(player);
    if (layer == null) {
      ModMain.LOGGER.warn("Layer doubles lames absent pour {}", player.getGameProfile().getName());
      return;
    }

    ResourceLocation animationId = COMBO_ANIMATION_IDS[comboStep];
    var playable = PlayerAnimationRegistry.getAnimation(animationId);
    if (!(playable instanceof KeyframeAnimation animation)) {
      ModMain.LOGGER.error("Animation doubles lames introuvable : {}", animationId);
      return;
    }

    int swingTicks = COMBO_TICKS[comboStep];
    KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = copy.endTick / (float) swingTicks;
    }

    layer.replaceAnimationWithFade(
        AbstractFadeModifier.standardFadeIn(copy.beginTick, Ease.INOUTSINE),
        new KeyframeAnimationPlayer(copy.build(), 0)
    );

    UUID id = player.getUUID();
    ACTIVE_SWING_TICKS.put(id, swingTicks);
    ACTIVE_SWING_DURATION.put(id, swingTicks);
    ACTIVE_COMBO_STEP.put(id, comboStep);
    if (shouldSuppressVanillaSwing(player)) {
      suppressVanillaSwing(player);
    }
  }

  public static boolean shouldSuppressVanillaSwing(Player player) {
    if (!(player instanceof LocalPlayer localPlayer)) {
      return true;
    }
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft.player == localPlayer && !minecraft.options.getCameraType().isFirstPerson();
  }

  public static void suppressVanillaSwing(Player player) {
    player.swinging = false;
    player.swingTime = 0;
    player.attackAnim = 0.0f;
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }

    for (AbstractClientPlayer player : minecraft.level.players()) {
      UUID id = player.getUUID();
      ACTIVE_SWING_TICKS.computeIfPresent(id, (uuid, ticks) -> ticks > 1 ? ticks - 1 : null);
      if (!isSwingAnimating(player)) {
        ACTIVE_COMBO_STEP.remove(id);
        ACTIVE_SWING_DURATION.remove(id);
      } else if (shouldSuppressVanillaSwing(player)) {
        suppressVanillaSwing(player);
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    SPEED_MODIFIERS.remove(id);
    ACTIVE_SWING_TICKS.remove(id);
    ACTIVE_COMBO_STEP.remove(id);
    ACTIVE_SWING_DURATION.remove(id);
  }

  @SuppressWarnings("unchecked")
  private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
    return (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
  }
}
