package com.seroka.chimere;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Bénédiction du Dieu Mouton : clic molette sur un bloc d'herbe → terre + nourriture. */
public final class MoutonBlessingHandler {

  /** Demi-icône de faim (1 point) et demi-unité de saturation, comme une bouchée d'herbe. */
  private static final int FOOD_GAIN = 1;
  private static final float SATURATION_GAIN = 0.5F;

  private MoutonBlessingHandler() {}

  public static boolean activate(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.MOUTON)) {
      return false;
    }

    HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
    if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
      return false;
    }

    BlockPos pos = blockHit.getBlockPos();
    Level level = player.level();
    if (!level.isLoaded(pos)) {
      return false;
    }

    BlockState state = level.getBlockState(pos);
    if (!state.is(Blocks.GRASS_BLOCK)) {
      return false;
    }

    if (!player.mayUseItemAt(pos, blockHit.getDirection(), player.getMainHandItem())) {
      return false;
    }

    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
    level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.2F);

    FoodData food = player.getFoodData();
    food.eat(FOOD_GAIN, SATURATION_GAIN);

    return true;
  }
}
