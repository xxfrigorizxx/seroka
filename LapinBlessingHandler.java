package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Bénédiction du Dieu Lapin : bascule vitesse x2 + Saut II au clic molette. */
public final class LapinBlessingHandler {

  /** Vitesse V (+100 %) = déplacement doublé. */
  private static final int SPEED_AMPLIFIER = 4;
  /** Saut II. */
  private static final int JUMP_BOOST_AMPLIFIER = 1;

  private LapinBlessingHandler() {}

  public static void activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LAPIN)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    boolean enabled = !state.lapinAgility();
    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withLapinAgility(enabled));
    setAgility(player, enabled);

    player.playNotifySound(SoundEvents.RABBIT_JUMP, SoundSource.PLAYERS, 0.7F, enabled ? 1.2F : 0.9F);
    Component message = enabled
        ? Component.translatable("blessing.seroka.lapin.agility.on").withStyle(ChatFormatting.GREEN)
        : Component.translatable("blessing.seroka.lapin.agility.off").withStyle(ChatFormatting.GRAY);
    player.displayClientMessage(message, true);
  }

  public static void setAgility(ServerPlayer player, boolean enabled) {
    if (enabled) {
      player.addEffect(createInfiniteEffect(MobEffects.MOVEMENT_SPEED, SPEED_AMPLIFIER));
      player.addEffect(createInfiniteEffect(MobEffects.JUMP, JUMP_BOOST_AMPLIFIER));
      return;
    }
    player.removeEffect(MobEffects.MOVEMENT_SPEED);
    player.removeEffect(MobEffects.JUMP);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LAPIN)) {
      return;
    }
    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    if (!state.lapinAgility()) {
      return;
    }
    if (!player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
      player.addEffect(createInfiniteEffect(MobEffects.MOVEMENT_SPEED, SPEED_AMPLIFIER));
    }
    if (!player.hasEffect(MobEffects.JUMP)) {
      player.addEffect(createInfiniteEffect(MobEffects.JUMP, JUMP_BOOST_AMPLIFIER));
    }
  }

  public static void clear(ServerPlayer player) {
    player.removeEffect(MobEffects.MOVEMENT_SPEED);
    player.removeEffect(MobEffects.JUMP);
  }

  private static MobEffectInstance createInfiniteEffect(
      net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
      int amplifier
  ) {
    return new MobEffectInstance(
        effect,
        MobEffectInstance.INFINITE_DURATION,
        amplifier,
        false,
        true,
        true
    );
  }
}
