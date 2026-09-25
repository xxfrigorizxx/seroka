package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Poisson d'Argent : infiltration dans les blocs et rage. */
public final class PoissonArgentBlessingHandler {

  private static final ResourceLocation RAGE_DAMAGE_ID = ModMain.id("poisson_argent_rage_damage");
  private static final double RAGE_DAMAGE_BONUS = 2.0D;
  private static final int RAGE_DURATION_TICKS = 45 * 20;
  private static final int ABILITY_COOLDOWN_TICKS = 5 * 60 * 20;
  private static final double BURIED_HALF_SIZE = 0.2D;

  private static final Map<UUID, BlockPos> BURIED_ANCHOR = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> ABILITY_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> RAGE_DAMAGE_UNTIL_TICK = new ConcurrentHashMap<>();
  private static final Map<UUID, Boolean> SKIP_NEXT_COOLDOWN = new ConcurrentHashMap<>();
  private static final Map<UUID, GameType> PREVIOUS_GAME_MODE = new ConcurrentHashMap<>();

  private enum ExitReason {
    VOLUNTARY,
    BLOCK_BROKEN,
    FORCED
  }

  private PoissonArgentBlessingHandler() {}

  public static boolean isBuried(ServerPlayer player) {
    return BURIED_ANCHOR.containsKey(player.getUUID());
  }

  public static boolean isBurrowed(Player player) {
    return player.getData(ModAttachments.POISSON_ARGENT_BURROWED);
  }

  public static BlockPos getBuriedAnchor(ServerPlayer player) {
    return BURIED_ANCHOR.get(player.getUUID());
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_ARGENT)) {
      return;
    }
    if (isBuried(player)) {
      maintainBuriedState(player);
    }
    refreshRageDamage(player);
  }

  public static void activateBurrow(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.POISSON_ARGENT)) {
      return;
    }
    if (isBuried(player)) {
      exitBurrow(player, ExitReason.VOLUNTARY);
      return;
    }
    tryEnterBurrow(player);
  }

  public static void onAnchorBlockBroken(ServerPlayer player) {
    if (!isBuried(player)) {
      return;
    }
    exitBurrow(player, ExitReason.BLOCK_BROKEN);
  }

  public static void onForcedExit(ServerPlayer player) {
    if (!isBuried(player)) {
      return;
    }
    exitBurrow(player, ExitReason.FORCED);
  }

  public static void clear(ServerPlayer player) {
    if (isBuried(player)) {
      BlockPos anchor = BURIED_ANCHOR.get(player.getUUID());
      restorePlayerAppearance(player);
      if (anchor != null) {
        teleportAboveBlock(player, anchor);
      }
    }
    BURIED_ANCHOR.remove(player.getUUID());
    ABILITY_COOLDOWN_LAST_TICK.remove(player.getUUID());
    RAGE_DAMAGE_UNTIL_TICK.remove(player.getUUID());
    SKIP_NEXT_COOLDOWN.remove(player.getUUID());
    PREVIOUS_GAME_MODE.remove(player.getUUID());
    player.setData(ModAttachments.POISSON_ARGENT_BURROWED, false);
    removeRageDamageBonus(player);
  }

  private static void tryEnterBurrow(ServerPlayer player) {
    long now = player.level().getGameTime();
    if (!Boolean.TRUE.equals(SKIP_NEXT_COOLDOWN.remove(player.getUUID()))) {
      long lastTick = ABILITY_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), now - ABILITY_COOLDOWN_TICKS);
      long elapsed = now - lastTick;
      if (elapsed < ABILITY_COOLDOWN_TICKS) {
        int remainingSeconds = (int) Math.ceil((ABILITY_COOLDOWN_TICKS - elapsed) / 20.0D);
        player.displayClientMessage(
            Component.translatable("blessing.seroka.poisson_argent.burrow.cooldown", remainingSeconds)
                .withStyle(ChatFormatting.RED),
            true
        );
        return;
      }
    }

    if (!player.onGround()) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.poisson_argent.burrow.airborne")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    BlockPos anchor = resolveBurrowAnchor(player);
    BlockState state = player.level().getBlockState(anchor);
    if (!isValidBurrowBlock(player.level(), anchor, state)) {
      player.displayClientMessage(
          Component.translatable("blessing.seroka.poisson_argent.burrow.invalid_block")
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    BURIED_ANCHOR.put(player.getUUID(), anchor.immutable());
    applyBuriedState(player, anchor);
    player.playNotifySound(SoundEvents.SILVERFISH_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.2F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.poisson_argent.burrow.entered")
            .withStyle(ChatFormatting.GRAY),
        true
    );
  }

  private static void maintainBuriedState(ServerPlayer player) {
    BlockPos anchor = BURIED_ANCHOR.get(player.getUUID());
    if (anchor == null) {
      return;
    }

    BlockState state = player.level().getBlockState(anchor);
    if (!isValidBurrowBlock(player.level(), anchor, state)) {
      onAnchorBlockBroken(player);
      return;
    }

    applyBuriedState(player, anchor);
  }

  private static void applyBuriedState(ServerPlayer player, BlockPos anchor) {
    double centerX = anchor.getX() + 0.5D;
    double centerY = anchor.getY() + 0.5D;
    double centerZ = anchor.getZ() + 0.5D;

    PREVIOUS_GAME_MODE.putIfAbsent(player.getUUID(), player.gameMode.getGameModeForPlayer());
    player.setData(ModAttachments.POISSON_ARGENT_BURROWED, true);

    if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
      player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
    }
    if (!player.isSpectator()) {
      player.noPhysics = true;
    }

    player.teleportTo(centerX, centerY, centerZ);
    player.setDeltaMovement(Vec3.ZERO);
    player.setNoGravity(true);
    player.fallDistance = 0.0F;
    player.setInvisible(true);
    player.setBoundingBox(new AABB(
        centerX - BURIED_HALF_SIZE,
        centerY - BURIED_HALF_SIZE,
        centerZ - BURIED_HALF_SIZE,
        centerX + BURIED_HALF_SIZE,
        centerY + BURIED_HALF_SIZE,
        centerZ + BURIED_HALF_SIZE
    ));
    player.hurtMarked = true;

    if (!player.hasEffect(MobEffects.INVISIBILITY)) {
      player.addEffect(new MobEffectInstance(
          MobEffects.INVISIBILITY,
          MobEffectInstance.INFINITE_DURATION,
          0,
          false,
          false,
          false
      ));
    }
  }

  private static void exitBurrow(ServerPlayer player, ExitReason reason) {
    BlockPos anchor = BURIED_ANCHOR.remove(player.getUUID());
    if (anchor == null) {
      return;
    }

    long now = player.level().getGameTime();
    restorePlayerAppearance(player);
    teleportAboveBlock(player, anchor);

    switch (reason) {
      case VOLUNTARY -> {
        ABILITY_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
        player.displayClientMessage(
            Component.translatable("blessing.seroka.poisson_argent.burrow.exited")
                .withStyle(ChatFormatting.GRAY),
            true
        );
      }
      case BLOCK_BROKEN -> {
        grantRageDamage(player, now);
        SKIP_NEXT_COOLDOWN.put(player.getUUID(), true);
        player.playNotifySound(SoundEvents.SILVERFISH_HURT, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(
            Component.translatable("blessing.seroka.poisson_argent.rage.granted")
                .withStyle(ChatFormatting.GOLD),
            true
        );
      }
      case FORCED -> {
        SKIP_NEXT_COOLDOWN.put(player.getUUID(), true);
        player.displayClientMessage(
            Component.translatable("blessing.seroka.poisson_argent.burrow.forced")
                .withStyle(ChatFormatting.RED),
            true
        );
      }
    }
  }

  private static void grantRageDamage(ServerPlayer player, long now) {
    RAGE_DAMAGE_UNTIL_TICK.put(player.getUUID(), now + RAGE_DURATION_TICKS);
    applyRageDamageBonus(player);
  }

  private static void refreshRageDamage(ServerPlayer player) {
    Long until = RAGE_DAMAGE_UNTIL_TICK.get(player.getUUID());
    if (until == null) {
      removeRageDamageBonus(player);
      return;
    }
    if (player.level().getGameTime() >= until) {
      RAGE_DAMAGE_UNTIL_TICK.remove(player.getUUID());
      removeRageDamageBonus(player);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.poisson_argent.rage.ended")
              .withStyle(ChatFormatting.DARK_GRAY),
          true
      );
      return;
    }
    applyRageDamageBonus(player);
  }

  private static void applyRageDamageBonus(ServerPlayer player) {
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack == null) {
      return;
    }
    attack.removeModifier(RAGE_DAMAGE_ID);
    attack.addTransientModifier(
        new AttributeModifier(RAGE_DAMAGE_ID, RAGE_DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void removeRageDamageBonus(ServerPlayer player) {
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      attack.removeModifier(RAGE_DAMAGE_ID);
    }
  }

  private static void restorePlayerAppearance(ServerPlayer player) {
    GameType previous = PREVIOUS_GAME_MODE.remove(player.getUUID());
    player.setData(ModAttachments.POISSON_ARGENT_BURROWED, false);
    if (previous != null && player.gameMode.getGameModeForPlayer() != previous) {
      player.gameMode.changeGameModeForPlayer(previous);
    } else if (player.isSpectator()) {
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
    }
    player.noPhysics = false;
    player.setNoGravity(false);
    player.setInvisible(false);
    player.removeEffect(MobEffects.INVISIBILITY);
    player.refreshDimensions();
  }

  private static void teleportAboveBlock(ServerPlayer player, BlockPos anchor) {
    player.teleportTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D);
    player.setDeltaMovement(Vec3.ZERO);
    player.fallDistance = 0.0F;
    player.hurtMarked = true;
  }

  private static BlockPos resolveBurrowAnchor(ServerPlayer player) {
    Level level = player.level();
    BlockPos onPos = player.getOnPos();
    if (isValidBurrowBlock(level, onPos, level.getBlockState(onPos))) {
      return onPos;
    }
    BlockPos below = player.blockPosition().below();
    if (isValidBurrowBlock(level, below, level.getBlockState(below))) {
      return below;
    }
    return onPos;
  }

  private static boolean isValidBurrowBlock(Level level, BlockPos pos, BlockState state) {
    if (state.isAir() || state.liquid()) {
      return false;
    }
    if (!state.blocksMotion()) {
      return false;
    }
    return !state.getCollisionShape(level, pos).isEmpty();
  }
}
