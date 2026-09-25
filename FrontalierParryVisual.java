package com.seroka.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import com.seroka.ModMain;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.network.payload.SwordParryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pose de garde à l'épée (clic unique, fenêtre courte).
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierParryVisual {

  private static final Map<UUID, Integer> LOCAL_PARRY_TICKS = new ConcurrentHashMap<>();
  private static final Map<UUID, Integer> LAST_PARRY_TICKS = new ConcurrentHashMap<>();

  private FrontalierParryVisual() {}

  public static boolean isParrying(Player player) {
    if (player == null) {
      return false;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    return faction.isFrontalier()
        && getParryTicks(player) > 0
        && player.getMainHandItem().is(ItemTags.SWORDS);
  }

  public static int getParryTicks(Player player) {
    int synced = player.getData(ModAttachments.PLAYER_FACTION).swordParryTicks();
    int local = LOCAL_PARRY_TICKS.getOrDefault(player.getUUID(), 0);
    return Math.max(synced, local);
  }

  @SubscribeEvent
  public static void onMouseClick(InputEvent.MouseButton.Pre event) {
    if (event.getAction() != InputConstants.PRESS) {
      return;
    }
    if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.screen != null) {
      return;
    }
    if (!player.getMainHandItem().is(ItemTags.SWORDS)) {
      return;
    }

    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    if (!SwordParryPayload.canStartSwordParry(faction)) {
      return;
    }

    PacketDistributor.sendToServer(new SwordParryPayload());
    startLocalParry(player);
    playParryStartSound(player);
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }

    for (AbstractClientPlayer player : minecraft.level.players()) {
      UUID id = player.getUUID();
      int parryTicks = getParryTicks(player);
      LAST_PARRY_TICKS.put(id, parryTicks);
      LOCAL_PARRY_TICKS.computeIfPresent(id, (uuid, ticks) -> ticks > 1 ? ticks - 1 : null);
    }
  }

  @SubscribeEvent
  public static void onRenderHand(RenderHandEvent event) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null || !isParrying(player)) {
      return;
    }

    ItemStack stack = event.getItemStack();
    if (stack.isEmpty()) {
      return;
    }

    int side = event.getHand() == InteractionHand.MAIN_HAND ? 1 : -1;
    PoseStack poseStack = event.getPoseStack();

    if (event.getHand() == InteractionHand.MAIN_HAND && stack.is(ItemTags.SWORDS)) {
      poseStack.translate(side * 0.04F, 0.12F, -0.22F);
      poseStack.mulPose(Axis.YP.rotationDegrees(side * 42.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(side * -28.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
      return;
    }

    if (event.getHand() == InteractionHand.OFF_HAND) {
      poseStack.translate(side * 0.06F, 0.08F, -0.08F);
      poseStack.mulPose(Axis.YP.rotationDegrees(side * -18.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(side * 12.0F));
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID id = event.getEntity().getUUID();
    LOCAL_PARRY_TICKS.remove(id);
    LAST_PARRY_TICKS.remove(id);
  }

  private static void startLocalParry(Player player) {
    LOCAL_PARRY_TICKS.put(player.getUUID(), PlayerFaction.SWORD_PARRY_DURATION_TICKS);
  }

  private static void playParryStartSound(Player player) {
    player.level().playLocalSound(
        player.getX(),
        player.getY(),
        player.getZ(),
        SoundEvents.ANVIL_PLACE,
        SoundSource.PLAYERS,
        0.35f,
        1.65f,
        false
    );
  }
}
