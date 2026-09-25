package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.faction.DualBladesComboState;
import com.seroka.faction.DualBladesHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.network.payload.DualBladesSwingSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Enchaînement 3 coups Doubles Lames (client) — prédiction visuelle uniquement.
 * Les dégâts et l'état du combo sont gérés côté serveur.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierDualBladesClientHandler {

  private FrontalierDualBladesClientHandler() {}

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onAttackEntity(AttackEntityEvent event) {
    if (!event.getEntity().level().isClientSide()) {
      return;
    }

    if (!(event.getEntity() instanceof LocalPlayer player)) {
      return;
    }

    if (!isDualBladesActive(player)) {
      DualBladesComboState.reset(player.getUUID());
      return;
    }

    int step = DualBladesComboState.peekNextStep(player.getUUID());
    performComboSwing(player, step);

    if (DualBladesComboState.isOffHandStep(step)) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
    if (!event.isAttack()) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.screen != null) {
      return;
    }

    if (minecraft.hitResult instanceof EntityHitResult) {
      return;
    }

    if (!isDualBladesActive(player)) {
      DualBladesComboState.reset(player.getUUID());
      return;
    }

    int step = DualBladesComboState.peekNextStep(player.getUUID());
    performComboSwing(player, step);
    PacketDistributor.sendToServer(new DualBladesSwingSyncPayload(step));

    if (DualBladesComboState.isOffHandStep(step)) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    DualBladesComboState.clear(event.getEntity().getUUID());
  }

  private static boolean isDualBladesActive(LocalPlayer player) {
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    return DualBladesHelper.isDualBladesActive(player, faction);
  }

  private static void performComboSwing(LocalPlayer player, int comboStep) {
    FrontalierDualBladesAnimation.playComboSwing(player, comboStep);
    playVanillaHandSwing(player, comboStep);
    playSwingSound(player, comboStep);
  }

  /** En 1re personne, le swing vanilla anime les épées visibles à l'écran. */
  private static void playVanillaHandSwing(LocalPlayer player, int comboStep) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != player || !minecraft.options.getCameraType().isFirstPerson()) {
      return;
    }
    InteractionHand hand = DualBladesComboState.isOffHandStep(comboStep)
        ? InteractionHand.OFF_HAND
        : InteractionHand.MAIN_HAND;
    player.swing(hand);
  }

  private static void playSwingSound(LocalPlayer player, int comboStep) {
    var sound = comboStep == DualBladesComboState.STEP_CROSS_FINISHER
        ? SoundEvents.PLAYER_ATTACK_CRIT
        : SoundEvents.PLAYER_ATTACK_STRONG;
    float pitch = 0.95f + comboStep * 0.05f + player.getRandom().nextFloat() * 0.1f;
    player.level().playLocalSound(
        player.getX(),
        player.getY(),
        player.getZ(),
        sound,
        SoundSource.PLAYERS,
        0.75f,
        pitch,
        false
    );
  }
}
