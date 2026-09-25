package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Bénédiction du Dieu Vache : clic molette avec un seau vide → seau de lait. */
public final class VacheBlessingHandler {

  private VacheBlessingHandler() {}

  public static boolean activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.VACHE)) {
      return false;
    }
    if (tryFillBucket(player, InteractionHand.MAIN_HAND)) {
      return true;
    }
    return tryFillBucket(player, InteractionHand.OFF_HAND);
  }

  private static boolean tryFillBucket(Player player, InteractionHand hand) {
    ItemStack held = player.getItemInHand(hand);
    if (!held.is(Items.BUCKET)) {
      return false;
    }
    player.setItemInHand(hand, new ItemStack(Items.MILK_BUCKET));
    player.level().playSound(
        null,
        player.blockPosition(),
        SoundEvents.COW_MILK,
        SoundSource.PLAYERS,
        1.0F,
        1.0F
    );
    return true;
  }
}
