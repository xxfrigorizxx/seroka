package com.seroka.chimere;

import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Bénédiction du Dieu Renard : vitesse passive + vol d'objet au clic molette. */
public final class RenardBlessingHandler {

  private static final ResourceLocation SPEED_ID = ModMain.id("renard_speed");
  private static final double SPEED_BONUS = 0.10D;
  private static final int COOLDOWN_TICKS = 2 * 60 * 20;

  private RenardBlessingHandler() {}

  public static void applyPassive(Player player) {
    if (!ChimereBlessingService.hasBlessing(player, GodIds.RENARD)) {
      return;
    }
    setMultiplierModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS);
  }

  public static void clear(Player player) {
    removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
  }

  public static void activate(ServerPlayer thief) {
    if (!ChimereBlessingService.hasBlessing(thief, GodIds.RENARD)) {
      return;
    }

    ChimereBlessingState state = thief.getData(ModAttachments.CHIMERE_BLESSING_STATE);
    long now = thief.level().getGameTime();
    long elapsed = now - state.renardStealLastTick();
    if (elapsed < COOLDOWN_TICKS) {
      int remainingSeconds = (int) Math.ceil((COOLDOWN_TICKS - elapsed) / 20.0D);
      thief.displayClientMessage(
          Component.translatable("blessing.seroka.renard.steal.cooldown", remainingSeconds)
              .withStyle(ChatFormatting.RED),
          true
      );
      return;
    }

    LivingEntity target = findTarget(thief);
    if (target == null) {
      thief.displayClientMessage(
          Component.translatable("blessing.seroka.renard.steal.no_target").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    if (!stealRandomItem(thief, target)) {
      thief.displayClientMessage(
          Component.translatable("blessing.seroka.renard.steal.empty").withStyle(ChatFormatting.GRAY),
          true
      );
      return;
    }

    thief.setData(ModAttachments.CHIMERE_BLESSING_STATE, state.withRenardStealLastTick(now));
    thief.level().playSound(null, thief.blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 0.6F, 1.4F);
    thief.level().playSound(null, target.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.7F);
    thief.displayClientMessage(
        Component.translatable("blessing.seroka.renard.steal.success", target.getDisplayName())
            .withStyle(ChatFormatting.GOLD),
        true
    );
  }

  private static LivingEntity findTarget(ServerPlayer thief) {
    double range = thief.entityInteractionRange();
    Vec3 eye = thief.getEyePosition(1.0F);
    Vec3 look = thief.getViewVector(1.0F);
    Vec3 end = eye.add(look.scale(range));
    AABB searchBox = thief.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
    EntityHitResult hit = ProjectileUtil.getEntityHitResult(
        thief,
        eye,
        end,
        searchBox,
        entity -> entity instanceof LivingEntity living
            && living != thief
            && living.isAlive()
            && !living.isSpectator()
            && entity.isPickable(),
        range * range
    );
    if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
      return null;
    }
    return target;
  }

  private static boolean stealRandomItem(ServerPlayer thief, LivingEntity target) {
    if (target instanceof ServerPlayer player) {
      return stealFromPlayerInventory(thief, player);
    }
    return stealFromEquipment(thief, target);
  }

  private static boolean stealFromPlayerInventory(ServerPlayer thief, ServerPlayer target) {
    Inventory inventory = target.getInventory();
    List<Integer> stealableSlots = new ArrayList<>();
    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
      if (isArmorSlot(slot)) {
        continue;
      }
      if (!inventory.getItem(slot).isEmpty()) {
        stealableSlots.add(slot);
      }
    }
    if (stealableSlots.isEmpty()) {
      return false;
    }

    int slot = stealableSlots.get(ThreadLocalRandom.current().nextInt(stealableSlots.size()));
    ItemStack stack = inventory.getItem(slot);
    ItemStack stolen = stack.copyWithCount(1);
    stack.shrink(1);
    if (stack.isEmpty()) {
      inventory.setItem(slot, ItemStack.EMPTY);
    }

    if (!thief.getInventory().add(stolen)) {
      thief.drop(stolen, false);
    }
    return true;
  }

  private static boolean stealFromEquipment(ServerPlayer thief, LivingEntity target) {
    List<EquipmentSlot> stealableSlots = new ArrayList<>();
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      if (!target.getItemBySlot(slot).isEmpty()) {
        stealableSlots.add(slot);
      }
    }
    if (stealableSlots.isEmpty()) {
      return false;
    }

    EquipmentSlot slot = stealableSlots.get(ThreadLocalRandom.current().nextInt(stealableSlots.size()));
    ItemStack stack = target.getItemBySlot(slot);
    ItemStack stolen = stack.copyWithCount(1);
    stack.shrink(1);
    target.setItemSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

    if (!thief.getInventory().add(stolen)) {
      thief.drop(stolen, false);
    }
    return true;
  }

  private static boolean isArmorSlot(int slot) {
    return slot >= Inventory.INVENTORY_SIZE && slot < Inventory.INVENTORY_SIZE + 4;
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
