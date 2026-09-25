package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bénédiction du Dieu Lama : vitesse, step, sac, crachat et répulsion des loups. */
public final class LamaBlessingHandler {

  private static final ResourceLocation SPEED_ID = ModMain.id("lama_speed");
  private static final ResourceLocation STEP_ID = ModMain.id("lama_step");
  private static final double SPEED_BONUS = 0.10D;
  private static final double STEP_BONUS = 0.4D;
  private static final int SPIT_COOLDOWN_TICKS = 30 * 20;
  private static final double WOLF_FLEE_RADIUS = 20.0D;
  private static final double WOLF_FLEE_PUSH = 0.55D;
  private static final Map<UUID, Long> SPIT_COOLDOWN_LAST_TICK = new ConcurrentHashMap<>();

  private LamaBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LAMA)) {
      return;
    }
    setMultiplierModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS);
    setValueModifier(player, Attributes.STEP_HEIGHT, STEP_ID, STEP_BONUS);
  }

  public static void refreshIfNeeded(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LAMA)) {
      return;
    }
    applyPassive(player);
    scareWolves(player);
  }

  public static void activatePack(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LAMA)) {
      return;
    }
    player.playNotifySound(SoundEvents.LLAMA_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.0F);
    player.openMenu(createMenuProvider(player));
  }

  public static void activateSpit(ServerPlayer player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.LAMA)) {
      return;
    }

    long now = player.level().getGameTime();
    long elapsed = now - SPIT_COOLDOWN_LAST_TICK.getOrDefault(player.getUUID(), 0L);
    if (elapsed < SPIT_COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((SPIT_COOLDOWN_TICKS - elapsed) / 20.0D);
      player.displayClientMessage(
          Component.translatable("blessing.seroka.lama.spit.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    launchSpit(player);
    SPIT_COOLDOWN_LAST_TICK.put(player.getUUID(), now);
    player.playNotifySound(SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.displayClientMessage(
        Component.translatable("blessing.seroka.lama.spit.launched").withStyle(ChatFormatting.GRAY),
        true
    );
  }

  public static boolean isProtectedFromWolves(Player player) {
    return ChimereBlessingService.hasBlessing(player, GodIds.LAMA);
  }

  public static void clear(Player player) {
    SPIT_COOLDOWN_LAST_TICK.remove(player.getUUID());
    removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
    removeModifier(player, Attributes.STEP_HEIGHT, STEP_ID);
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }
    returnPackItems(serverPlayer);
    serverPlayer.setData(ModAttachments.LAMA_PACK, LamaPack.EMPTY);
  }

  public static void savePack(ServerPlayer player, LamaPack pack) {
    player.setData(ModAttachments.LAMA_PACK, pack);
  }

  public static LamaPack getPack(Player player) {
    return player.getData(ModAttachments.LAMA_PACK);
  }

  private static void launchSpit(ServerPlayer player) {
    ServerLevel level = player.serverLevel();
    LlamaSpit spit = new LlamaSpit(EntityType.LLAMA_SPIT, level);
    spit.setOwner(player);
    spit.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
    Vec3 look = player.getViewVector(1.0F);
    spit.shoot(look.x, look.y, look.z, 1.5F, 10.0F);
    level.addFreshEntity(spit);
  }

  private static void scareWolves(ServerPlayer player) {
    AABB area = player.getBoundingBox().inflate(WOLF_FLEE_RADIUS);
    for (Wolf wolf : player.serverLevel().getEntitiesOfClass(Wolf.class, area)) {
      if (wolf.getTarget() == player) {
        wolf.setTarget(null);
      }
      if (wolf.getLastHurtByMob() == player) {
        wolf.setLastHurtByMob(null);
      }
      fleeFromPlayer(player, wolf);
    }
  }

  private static void fleeFromPlayer(ServerPlayer player, Wolf wolf) {
    Vec3 away = wolf.position().subtract(player.position());
    if (away.lengthSqr() < 0.01D) {
      away = new Vec3(1.0D, 0.0D, 0.0D);
    }
    Vec3 push = away.normalize().scale(WOLF_FLEE_PUSH);
    wolf.setDeltaMovement(wolf.getDeltaMovement().add(push.x, 0.0D, push.z));
    wolf.getNavigation().stop();
    wolf.setTarget(null);
  }

  private static MenuProvider createMenuProvider(ServerPlayer player) {
    return new MenuProvider() {
      @Override
      public Component getDisplayName() {
        return Component.translatable("container.seroka.lama_pack");
      }

      @Override
      public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
        return new LamaPackMenu(containerId, inventory, player);
      }
    };
  }

  private static void returnPackItems(ServerPlayer player) {
    for (ItemStack stack : getPack(player).slots()) {
      if (stack.isEmpty()) {
        continue;
      }
      if (!player.getInventory().add(stack.copy())) {
        player.drop(stack.copy(), false);
      }
    }
  }

  private static void setMultiplierModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,
      double amount
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null) {
      return;
    }
    instance.removeModifier(id);
    instance.addPermanentModifier(
        new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );
  }

  private static void setValueModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,
      double amount
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null) {
      return;
    }
    instance.removeModifier(id);
    instance.addPermanentModifier(
        new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE)
    );
  }

  private static void removeModifier(
      Player player,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id
  ) {
    AttributeInstance instance = player.getAttribute(attribute);
    if (instance != null) {
      instance.removeModifier(id);
    }
  }
}
