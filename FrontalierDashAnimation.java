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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Dash Frontalier via Player Animation Library.
 * Animation JSON adaptée depuis Better Combat (Daedelus, MIT).
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierDashAnimation {

  public static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "frontalier_dash");

  private static final ResourceLocation DASH_ANIMATION_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "frontalier_dash");

  private static final int GAMEPLAY_PRIORITY = 1500;

  private static final Map<UUID, Integer> LAST_DASH_TICKS = new ConcurrentHashMap<>();
  private static final Set<UUID> ANIMATION_PLAYED = ConcurrentHashMap.newKeySet();
  private static final Map<UUID, SpeedModifier> SPEED_MODIFIERS = new ConcurrentHashMap<>();

  private FrontalierDashAnimation() {}

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

  public static void triggerLocalDash(AbstractClientPlayer player) {
    if (player == null) {
      return;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isFrontalier() || !faction.canDash()) {
      return;
    }

    playDashAnimation(player);
    playDashSound(player);
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
      int dashTicks = visual.dashTicks();
      int previousDashTicks = LAST_DASH_TICKS.getOrDefault(player.getUUID(), 0);
      LAST_DASH_TICKS.put(player.getUUID(), dashTicks);

      if (!visual.frontalier()) {
        continue;
      }

      if (dashTicks <= 0) {
        ANIMATION_PLAYED.remove(player.getUUID());
        continue;
      }

      if (previousDashTicks <= 0 && dashTicks > 0 && ANIMATION_PLAYED.add(player.getUUID())) {
        playDashAnimation(player);
        playDashSound(player);
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    LAST_DASH_TICKS.remove(id);
    ANIMATION_PLAYED.remove(id);
    SPEED_MODIFIERS.remove(id);
  }

  private static void playDashAnimation(AbstractClientPlayer player) {
    ModifierLayer<IAnimation> layer = getLayer(player);
    if (layer == null) {
      ModMain.LOGGER.warn("Layer dash absent pour {}", player.getGameProfile().getName());
      return;
    }

    var playable = PlayerAnimationRegistry.getAnimation(DASH_ANIMATION_ID);
    if (!(playable instanceof KeyframeAnimation animation)) {
      ModMain.LOGGER.error("Animation dash introuvable : {}", DASH_ANIMATION_ID);
      return;
    }

    KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
    float animationLength = copy.endTick;
    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = animationLength / PlayerFaction.DASH_DURATION_TICKS;
    }

    layer.replaceAnimationWithFade(
        AbstractFadeModifier.standardFadeIn(copy.beginTick, Ease.INOUTSINE),
        new KeyframeAnimationPlayer(copy.build(), 0)
    );
  }

  private static void playDashSound(AbstractClientPlayer player) {
    player.level().playLocalSound(
        player.getX(),
        player.getY(),
        player.getZ(),
        SoundEvents.ENDER_DRAGON_SHOOT,
        SoundSource.PLAYERS,
        0.75f,
        1.75f,
        false
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
