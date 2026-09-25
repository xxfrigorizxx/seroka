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
 * Animation de lancer d'épée via Player Animation Library.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierThrowSwordAnimation {

  public static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "throw_sword");

  private static final ResourceLocation THROW_ANIMATION_ID =
      ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "throw_sword");

  private static final int GAMEPLAY_PRIORITY = 1500;

  private static final Map<UUID, Integer> LAST_THROW_TICKS = new ConcurrentHashMap<>();
  private static final Set<UUID> ANIMATION_PLAYED = ConcurrentHashMap.newKeySet();
  private static final Map<UUID, SpeedModifier> SPEED_MODIFIERS = new ConcurrentHashMap<>();

  private FrontalierThrowSwordAnimation() {}

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

  public static void triggerLocalThrow(AbstractClientPlayer player) {
    if (player == null) {
      return;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isFrontalier()) {
      return;
    }

    playThrowAnimation(player);
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
      int throwTicks = visual.throwSwordTicks();
      int previousThrowTicks = LAST_THROW_TICKS.getOrDefault(player.getUUID(), 0);
      LAST_THROW_TICKS.put(player.getUUID(), throwTicks);

      if (!visual.frontalier()) {
        continue;
      }

      if (throwTicks <= 0) {
        ANIMATION_PLAYED.remove(player.getUUID());
        continue;
      }

      if (previousThrowTicks <= 0 && throwTicks > 0 && ANIMATION_PLAYED.add(player.getUUID())) {
        playThrowAnimation(player);
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    LAST_THROW_TICKS.remove(id);
    ANIMATION_PLAYED.remove(id);
    SPEED_MODIFIERS.remove(id);
  }

  private static void playThrowAnimation(AbstractClientPlayer player) {
    ModifierLayer<IAnimation> layer = getLayer(player);
    if (layer == null) {
      return;
    }

    var playable = PlayerAnimationRegistry.getAnimation(THROW_ANIMATION_ID);
    if (!(playable instanceof KeyframeAnimation animation)) {
      ModMain.LOGGER.error("Animation lancer d'épée introuvable : {}", THROW_ANIMATION_ID);
      return;
    }

    KeyframeAnimation.AnimationBuilder copy = animation.mutableCopy();
    float animationLength = copy.endTick;
    SpeedModifier speedModifier = SPEED_MODIFIERS.get(player.getUUID());
    if (speedModifier != null) {
      speedModifier.speed = animationLength / PlayerFaction.THROW_SWORD_ANIM_TICKS;
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
