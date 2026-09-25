package com.seroka.chimere;

import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Bénédiction du Dieu Poulet : bascule chute lente au clic molette. */
public final class PouletBlessingHandler {

  private PouletBlessingHandler() {}

  public static void activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POULET)) {
      return;
    }

    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    boolean enabled = !state.pouletSlowFall();
    player.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withPouletSlowFall(enabled));
    setSlowFall(player, enabled);

    player.playNotifySound(SoundEvents.CHICKEN_AMBIENT, SoundSource.PLAYERS, 0.6F, enabled ? 1.3F : 0.8F);
    Component message = enabled
        ? Component.translatable("blessing.seroka.poulet.slow_fall.on").withStyle(ChatFormatting.GREEN)
        : Component.translatable("blessing.seroka.poulet.slow_fall.off").withStyle(ChatFormatting.GRAY);
    player.displayClientMessage(message, true);
  }

  public static void setSlowFall(ServerPlayer player, boolean enabled) {
    if (enabled) {
      player.addEffect(new MobEffectInstance(
          MobEffects.SLOW_FALLING,
          MobEffectInstance.INFINITE_DURATION,
          0,
          false,
          true,
          true
      ));
      return;
    }
    player.removeEffect(MobEffects.SLOW_FALLING);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POULET)) {
      return;
    }
    ChimereBlessingState state = player.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    if (state.pouletSlowFall() && !player.hasEffect(MobEffects.SLOW_FALLING)) {
      setSlowFall(player, true);
    }
  }

  public static void clear(ServerPlayer player) {
    player.removeEffect(MobEffects.SLOW_FALLING);
  }
}
