package com.seroka.client;

import com.seroka.faction.FactionStaminaHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD flottant style SAO pour les Frontaliers.
 * Inertie indépendante : yaw → X, pitch → Y. Tout le HUD partage la même translation.
 */
@EventBusSubscriber(modid = com.seroka.ModMain.MODID, value = Dist.CLIENT)
public final class FrontalierHUD {

  private static final float BASE_HUD_X = 10f;
  private static final float BASE_HUD_Y = 10f;

  private static final float HUD_LERP_SPEED = 0.15f;
  private static final float YAW_TO_OFFSET_X = 0.4f;
  private static final float PITCH_TO_OFFSET_Y = 0.35f;
  private static final float OFFSET_SPRING_DECAY = 0.72f;
  private static final float MAX_OFFSET = 28f;

  private static final float BOTTOM_HUD_LIFT = 12f;

  private static final int PANEL_WIDTH = 196;
  private static final int PANEL_HEIGHT = 62;
  private static final int CONTENT_X_OFFSET = 6;
  private static final int BAR_WIDTH = 184;
  private static final int MAIN_BAR_HEIGHT = 7;
  private static final int THIN_BAR_HEIGHT = 4;
  private static final int FIRST_BAR_Y = 16;
  private static final int MAIN_BAR_SPACING = 10;
  private static final int THIN_BAR_SPACING = 6;

  private static final int BORDER_COLOR = 0xFFFFFFFF;
  private static final int PANEL_BG_COLOR = 0xC0282828;
  private static final int HEADER_COLOR = 0xFFFFFFFF;
  private static final int BAR_BG_COLOR = 0xFF1A1A1A;
  private static final int FOOD_COLOR = 0xFFE88824;
  private static final int VANILLA_XP_COLOR = 0xFF55AAFF;
  private static final int FACTION_XP_COLOR = 0xFF33FF99;

  private static float currentHUDX = BASE_HUD_X;
  private static float currentHUDY = BASE_HUD_Y;
  private static float targetHUDX = BASE_HUD_X;
  private static float targetHUDY = BASE_HUD_Y;

  private static float inertiaOffsetX = 0f;
  private static float inertiaOffsetY = 0f;
  private static float lastYaw = Float.NaN;
  private static float lastPitch = Float.NaN;

  private FrontalierHUD() {}

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    if (!isLocalFrontalier()) {
      resetInertia();
      return;
    }

    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return;
    }

    float yaw = player.getYRot();
    float pitch = player.getXRot();

    if (Float.isNaN(lastYaw) || Float.isNaN(lastPitch)) {
      lastYaw = yaw;
      lastPitch = pitch;
      return;
    }

    float deltaYaw = Mth.wrapDegrees(yaw - lastYaw);
    float deltaPitch = pitch - lastPitch;
    lastYaw = yaw;
    lastPitch = pitch;

    inertiaOffsetX += deltaYaw * YAW_TO_OFFSET_X;
    inertiaOffsetY += deltaPitch * PITCH_TO_OFFSET_Y;

    inertiaOffsetX *= OFFSET_SPRING_DECAY;
    inertiaOffsetY *= OFFSET_SPRING_DECAY;

    inertiaOffsetX = Mth.clamp(inertiaOffsetX, -MAX_OFFSET, MAX_OFFSET);
    inertiaOffsetY = Mth.clamp(inertiaOffsetY, -MAX_OFFSET, MAX_OFFSET);

    targetHUDX = BASE_HUD_X + inertiaOffsetX;
    targetHUDY = BASE_HUD_Y + inertiaOffsetY;
  }

  @SubscribeEvent
  public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
    if (!isLocalFrontalier() || isHudSuppressed()) {
      return;
    }
    updateHudPosition(event.getPartialTick());
  }

  @SubscribeEvent
  public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
    if (!isLocalFrontalier() || isHudSuppressed()) {
      return;
    }

    ResourceLocation layer = event.getName();
    if (isReplacedVanillaLayer(layer)) {
      event.setCanceled(true);
      return;
    }

    if (isBottomFloatingLayer(layer)) {
      pushFloatingTransform(event.getGuiGraphics(), -BOTTOM_HUD_LIFT);
    }
  }

  @SubscribeEvent
  public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
    if (!isLocalFrontalier() || isHudSuppressed()) {
      return;
    }
    if (isBottomFloatingLayer(event.getName())) {
      popFloatingTransform(event.getGuiGraphics());
    }
  }

  @SubscribeEvent
  public static void onRenderGuiPost(RenderGuiEvent.Post event) {
    if (!isLocalFrontalier() || isHudSuppressed()) {
      return;
    }
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return;
    }
    renderSaoPanel(event.getGuiGraphics(), player);
  }

  @SubscribeEvent
  public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
    if (!FrontalierContainerUi.isEnabled()) {
      return;
    }
    if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
      return;
    }
    updateHudPosition(Minecraft.getInstance().getTimer());
    FrontalierContainerUi.applyScreenOffset(containerScreen, inertiaOffsetX(), inertiaOffsetY());
  }

  @SubscribeEvent
  public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
    if (!FrontalierContainerUi.isEnabled()) {
      return;
    }
    if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
      FrontalierContainerUi.clearScreenOffset(containerScreen);
    }
  }

  @SubscribeEvent
  public static void onScreenClosing(ScreenEvent.Closing event) {
    if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
      FrontalierContainerUi.clearScreenOffset(containerScreen);
    }
  }

  private static void updateHudPosition(net.minecraft.client.DeltaTracker deltaTracker) {
    float frameFactor = 1f - (float) Math.pow(1f - HUD_LERP_SPEED, deltaTracker.getRealtimeDeltaTicks());
    currentHUDX += (targetHUDX - currentHUDX) * frameFactor;
    currentHUDY += (targetHUDY - currentHUDY) * frameFactor;
  }

  private static float inertiaOffsetX() {
    return currentHUDX - BASE_HUD_X;
  }

  private static float inertiaOffsetY() {
    return currentHUDY - BASE_HUD_Y;
  }

  private static void pushFloatingTransform(GuiGraphics graphics, float extraYOffset) {
    graphics.pose().pushPose();
    graphics.pose().translate(inertiaOffsetX(), inertiaOffsetY() + extraYOffset, 0f);
  }

  private static void popFloatingTransform(GuiGraphics graphics) {
    graphics.pose().popPose();
  }

  private static void resetInertia() {
    lastYaw = Float.NaN;
    lastPitch = Float.NaN;
    inertiaOffsetX = 0f;
    inertiaOffsetY = 0f;
    currentHUDX = BASE_HUD_X;
    currentHUDY = BASE_HUD_Y;
    targetHUDX = BASE_HUD_X;
    targetHUDY = BASE_HUD_Y;
  }

  private static boolean isHudSuppressed() {
    Screen screen = Minecraft.getInstance().screen;
    if (screen == null) {
      return false;
    }
    // Inventaire, table de craft, four, etc. : garder le HUD SAO, masquer le vanilla.
    return !(screen instanceof AbstractContainerScreen);
  }

  private static boolean isLocalFrontalier() {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return false;
    }
    PlayerFaction faction = player.getData(ModAttachments.PLAYER_FACTION);
    return faction.isFrontalier();
  }

  private static boolean isReplacedVanillaLayer(ResourceLocation layer) {
    return VanillaGuiLayers.PLAYER_HEALTH.equals(layer)
        || VanillaGuiLayers.FOOD_LEVEL.equals(layer)
        || VanillaGuiLayers.EXPERIENCE_BAR.equals(layer)
        || VanillaGuiLayers.EXPERIENCE_LEVEL.equals(layer);
  }

  private static boolean isBottomFloatingLayer(ResourceLocation layer) {
    return VanillaGuiLayers.HOTBAR.equals(layer)
        || VanillaGuiLayers.SELECTED_ITEM_NAME.equals(layer);
  }

  private static void renderSaoPanel(GuiGraphics graphics, LocalPlayer player) {
    pushFloatingTransform(graphics, 0f);

    int x = Math.round(BASE_HUD_X);
    int y = Math.round(BASE_HUD_Y);
    int barX = x + CONTENT_X_OFFSET;
    PlayerFaction factionData = player.getData(ModAttachments.PLAYER_FACTION);

    drawBorder(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, BORDER_COLOR);
    graphics.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL_BG_COLOR);

    String header = player.getName().getString()
        + " | Lv." + factionData.factionLevel()
        + " | Enc." + player.experienceLevel;
    graphics.drawString(Minecraft.getInstance().font, header, barX, y + 5, HEADER_COLOR, true);

    float healthRatio = player.getMaxHealth() > 0 ? player.getHealth() / player.getMaxHealth() : 0f;
    float foodRatio = factionData.maxFoodLevel() > 0
        ? FactionStaminaHelper.getEffectiveFoodLevel(player) / (float) factionData.maxFoodLevel()
        : 0f;
    float vanillaXpRatio = player.experienceProgress;
    float factionXpRatio = factionData.factionXpProgress();

    int healthY = y + FIRST_BAR_Y;
    int foodY = healthY + MAIN_BAR_SPACING;
    int vanillaXpY = foodY + MAIN_BAR_SPACING;
    int factionXpY = vanillaXpY + THIN_BAR_SPACING;

    drawStatBar(graphics, barX, healthY, BAR_WIDTH, MAIN_BAR_HEIGHT, healthRatio, healthColorForRatio(healthRatio));
    drawStatBar(graphics, barX, foodY, BAR_WIDTH, MAIN_BAR_HEIGHT, foodRatio, FOOD_COLOR);
    drawStatBar(graphics, barX, vanillaXpY, BAR_WIDTH, THIN_BAR_HEIGHT, vanillaXpRatio, VANILLA_XP_COLOR);
    drawStatBar(graphics, barX, factionXpY, BAR_WIDTH, THIN_BAR_HEIGHT, factionXpRatio, FACTION_XP_COLOR);

    popFloatingTransform(graphics);
  }

  private static void drawStatBar(
      GuiGraphics graphics,
      int x,
      int y,
      int width,
      int height,
      float ratio,
      int fillColor
  ) {
    graphics.fill(x, y, x + width, y + height, BAR_BG_COLOR);
    if (ratio <= 0f) {
      return;
    }
    int filledWidth = Math.max(1, (int) (width * Math.min(1f, ratio)));
    graphics.fill(x, y, x + filledWidth, y + height, fillColor);
  }

  private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
    graphics.fill(x, y, x + width, y + 1, color);
    graphics.fill(x, y + height - 1, x + width, y + height, color);
    graphics.fill(x, y, x + 1, y + height, color);
    graphics.fill(x + width - 1, y, x + width, y + height, color);
  }

  private static int healthColorForRatio(float ratio) {
    if (ratio > 0.6f) {
      return 0xFF00CC44;
    }
    if (ratio > 0.3f) {
      return 0xFFFFCC00;
    }
    return 0xFFFF3333;
  }
}
