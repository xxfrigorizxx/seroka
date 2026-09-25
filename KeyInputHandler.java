package com.seroka.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.seroka.ModMain;
import com.seroka.client.screen.ChimereBlessingScreen;
import com.seroka.client.screen.FactionStatusScreen;
import com.seroka.client.screen.SkillMenuScreen;
import com.seroka.faction.FactionProneHandler;
import com.seroka.faction.FactionStaminaHelper;
import com.seroka.faction.ModAttachments;
import com.seroka.faction.PlayerFaction;
import com.seroka.faction.RollMath;
import com.seroka.faction.TetrasomieSpellSlots;
import com.seroka.tetrasomie.TetrasomieSpells;
import com.seroka.chimere.CalamarBlessingHandler;
import com.seroka.chimere.ChimereBlessingService;
import com.seroka.chimere.ChimereFlightHelper;
import com.seroka.chimere.GodIds;
import com.seroka.network.payload.CastSpellPayload;
import com.seroka.network.payload.ChimereFlightTogglePayload;
import com.seroka.network.payload.ChimereGodSecondaryAbilityPayload;
import com.seroka.network.payload.ChimereGodTertiaryAbilityPayload;
import com.seroka.network.payload.FrontalierDashPayload;
import com.seroka.network.payload.TacticalRollPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Enregistre les touches Frontalier (menu compétences, roulade tactique).
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class KeyInputHandler {

  public static final String SKILL_MENU_KEY = "key.seroka.skill_menu";
  public static final String TACTICAL_ROLL_KEY = "key.seroka.tactical_roll";
  public static final String FRONTALIER_DASH_KEY = "key.seroka.frontalier_dash";
  public static final String FACTION_MIDDLE_MOUSE_KEY = "key.seroka.faction_middle_mouse";
  public static final String CHIMERE_SECONDARY_ABILITY_KEY = "key.seroka.chimere_secondary_ability";
  public static final String CHIMERE_TERTIARY_ABILITY_KEY = "key.seroka.chimere_tertiary_ability";
  public static final String THROW_SWORD_KEY = "key.seroka.throw_sword";
  public static final String ELEMENT_RADIAL_MENU_KEY = "key.seroka.element_radial_menu";
  public static final String FACTION_STATUS_PANEL_KEY = "key.seroka.faction_status_panel";
  public static final String CHIMERE_FLIGHT_TOGGLE_KEY = "key.seroka.chimere_flight_toggle";
  public static final String CAST_ACTIVE_SPELL_KEY = "key.seroka.cast_active_spell";

  public static KeyMapping openSkillMenuKey;
  public static KeyMapping tacticalRollKey;
  public static KeyMapping frontalierDashKey;
  public static KeyMapping chimereSecondaryAbilityKey;
  public static KeyMapping chimereTertiaryAbilityKey;
  public static KeyMapping throwSwordKey;
  public static KeyMapping elementRadialMenuKey;
  public static KeyMapping factionStatusPanelKey;
  public static KeyMapping castActiveSpellKey;
  public static KeyMapping chimereFlightToggleKey;

  private static boolean shieldChanneling;
  private static boolean shieldChannelSecondary;
  private static int shieldTicksSinceStart;
  private static boolean sphereChanneling;
  private static boolean sphereChannelSecondary;
  private static int sphereTicksSinceStart;

  private static final int MIN_SHIELD_TICKS_BEFORE_RELEASE = 3;

  private KeyInputHandler() {}

  public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    openSkillMenuKey = new KeyMapping(
        SKILL_MENU_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        KeyMapping.CATEGORY_MISC
    );
    tacticalRollKey = new KeyMapping(
        TACTICAL_ROLL_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        KeyMapping.CATEGORY_MISC
    );
    event.register(openSkillMenuKey);
    event.register(tacticalRollKey);
    frontalierDashKey = new KeyMapping(
        FRONTALIER_DASH_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        KeyMapping.CATEGORY_MISC
    );
    event.register(frontalierDashKey);
    chimereSecondaryAbilityKey = new KeyMapping(
        CHIMERE_SECONDARY_ABILITY_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        KeyMapping.CATEGORY_MISC
    );
    event.register(chimereSecondaryAbilityKey);
    chimereTertiaryAbilityKey = new KeyMapping(
        CHIMERE_TERTIARY_ABILITY_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        KeyMapping.CATEGORY_MISC
    );
    event.register(chimereTertiaryAbilityKey);
    throwSwordKey = new KeyMapping(
        FACTION_MIDDLE_MOUSE_KEY,
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
        KeyMapping.CATEGORY_MISC
    );
    event.register(throwSwordKey);
    elementRadialMenuKey = new KeyMapping(
        ELEMENT_RADIAL_MENU_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        KeyMapping.CATEGORY_MISC
    );
    event.register(elementRadialMenuKey);
    factionStatusPanelKey = new KeyMapping(
        FACTION_STATUS_PANEL_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        KeyMapping.CATEGORY_MISC
    );
    event.register(factionStatusPanelKey);
    castActiveSpellKey = new KeyMapping(
        CAST_ACTIVE_SPELL_KEY,
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
        KeyMapping.CATEGORY_MISC
    );
    event.register(castActiveSpellKey);
    chimereFlightToggleKey = new KeyMapping(
        CHIMERE_FLIGHT_TOGGLE_KEY,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        KeyMapping.CATEGORY_MISC
    );
    event.register(chimereFlightToggleKey);
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null) {
      return;
    }

    handleSkillMenuKey(minecraft);
    handleTacticalRollKey(minecraft);
    handleFrontalierDashKey(minecraft);
    handleFactionMiddleMouseKey(minecraft);
    handleChimereSecondaryAbilityKey(minecraft);
    handleChimereTertiaryAbilityKey(minecraft);
    handleElementRadialMenuKey(minecraft);
    handleFactionStatusPanelKey(minecraft);
    handleCastActiveSpellKey(minecraft);
    handleChimereFlightToggleKey(minecraft);
  }

  private static void handleSkillMenuKey(Minecraft minecraft) {
    if (openSkillMenuKey == null) {
      return;
    }

    while (openSkillMenuKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      if (faction.isChimere()) {
        minecraft.setScreen(new ChimereBlessingScreen());
        continue;
      }
      if (faction.isFrontalier()) {
        minecraft.setScreen(new SkillMenuScreen(faction));
      }
    }
  }

  private static void handleTacticalRollKey(Minecraft minecraft) {
    if (tacticalRollKey == null) {
      return;
    }

    while (tacticalRollKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      if (!TacticalRollPayload.canRoll(faction)) {
        continue;
      }
      if (!FactionStaminaHelper.hasFood(minecraft.player, PlayerFaction.ROLL_MIN_FOOD)) {
        continue;
      }
      if (!minecraft.player.onGround()) {
        continue;
      }

      Vec3 direction = RollMath.getMovementDirection(minecraft.player);
      FactionProneHandler.forceStanding(minecraft.player);
      PacketDistributor.sendToServer(new TacticalRollPayload((float) direction.x, (float) direction.z));
      TacticalRollPayload.applyRollImpulse(minecraft.player, direction);
      FrontalierRollAnimation.triggerLocalRoll(minecraft.player);
    }
  }

  private static void handleFrontalierDashKey(Minecraft minecraft) {
    if (frontalierDashKey == null) {
      return;
    }

    while (frontalierDashKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      if (!FrontalierDashPayload.canDash(faction)) {
        continue;
      }
      if (!FactionStaminaHelper.hasFood(minecraft.player, PlayerFaction.DASH_MIN_FOOD)) {
        continue;
      }

      PacketDistributor.sendToServer(new FrontalierDashPayload());
      FrontalierDashPayload.applyDashVelocity(minecraft.player);
      FrontalierDashAnimation.triggerLocalDash(minecraft.player);
    }
  }

  private static void handleFactionMiddleMouseKey(Minecraft minecraft) {
    // Clic molette géré par FactionMiddleMouseClientHandler (InputEvent.MouseButton.Pre).
  }

  private static void handleCastActiveSpellKey(Minecraft minecraft) {
    if (castActiveSpellKey == null) {
      return;
    }

    if (minecraft.screen != null) {
      resetShieldChannelState();
      return;
    }

    if (ElementRadialMenuOverlay.isOpen()) {
      resetShieldChannelState();
      return;
    }

    PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isTetrasomie()) {
      resetShieldChannelState();
      return;
    }

    boolean secondary = minecraft.player.isShiftKeyDown();
    boolean middleDown = isMiddleMouseHeld(minecraft) || castActiveSpellKey.isDown();

    if (shieldChanneling) {
      if (!middleDown) {
        shieldTicksSinceStart++;
        if (shieldTicksSinceStart >= MIN_SHIELD_TICKS_BEFORE_RELEASE) {
          PacketDistributor.sendToServer(new CastSpellPayload(shieldChannelSecondary, true));
          shieldChanneling = false;
          shieldChannelSecondary = false;
          shieldTicksSinceStart = 0;
        }
      }
      return;
    }

    if (sphereChanneling) {
      if (!middleDown) {
        sphereTicksSinceStart++;
        if (sphereTicksSinceStart >= MIN_SHIELD_TICKS_BEFORE_RELEASE) {
          PacketDistributor.sendToServer(new CastSpellPayload(sphereChannelSecondary, true));
          sphereChanneling = false;
          sphereChannelSecondary = false;
          sphereTicksSinceStart = 0;
        }
      }
      return;
    }

    String spellId = equippedSpellForChamber(faction, secondary);

    if (TetrasomieSpells.WATER_SHIELD.equals(spellId)) {
      if (middleDown) {
        PacketDistributor.sendToServer(new CastSpellPayload(secondary, false));
        shieldChanneling = true;
        shieldChannelSecondary = secondary;
        shieldTicksSinceStart = 0;
      }
      return;
    }

    if (TetrasomieSpells.SPHERE_DILATABLE.equals(spellId)) {
      if (middleDown) {
        PacketDistributor.sendToServer(new CastSpellPayload(secondary, false));
        sphereChanneling = true;
        sphereChannelSecondary = secondary;
        sphereTicksSinceStart = 0;
      }
      return;
    }

    resetChannelState();
    while (castActiveSpellKey.consumeClick()) {
      PacketDistributor.sendToServer(new CastSpellPayload(secondary, false));
    }
  }

  private static String equippedSpellForChamber(PlayerFaction faction, boolean secondary) {
    int slot = secondary ? faction.secondarySpellSlot() : faction.primarySpellSlot();
    if (!TetrasomieSpellSlots.isValidSlot(slot)) {
      return "";
    }
    return faction.equippedSpell(slot);
  }

  private static void resetChannelState() {
    shieldChanneling = false;
    shieldChannelSecondary = false;
    shieldTicksSinceStart = 0;
    sphereChanneling = false;
    sphereChannelSecondary = false;
    sphereTicksSinceStart = 0;
  }

  private static void resetShieldChannelState() {
    resetChannelState();
  }

  private static boolean isMiddleMouseHeld(Minecraft minecraft) {
    return InputConstants.isKeyDown(
        minecraft.getWindow().getWindow(),
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE
    );
  }

  private static void handleChimereSecondaryAbilityKey(Minecraft minecraft) {
    if (chimereSecondaryAbilityKey == null) {
      return;
    }

    while (chimereSecondaryAbilityKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      if (!faction.isChimere()) {
        continue;
      }

      String godId = ChimereBlessingService.getGodId(minecraft.player);
      boolean needsWater = GodIds.CALAMAR.equals(godId) || GodIds.CALAMAR_LUMINESCENT.equals(godId);
      if (needsWater && !minecraft.player.isUnderWater()) {
        continue;
      }

      PacketDistributor.sendToServer(new ChimereGodSecondaryAbilityPayload());
      if (needsWater) {
        CalamarBlessingHandler.applyBoostVelocity(minecraft.player);
      }
    }
  }

  private static void handleChimereTertiaryAbilityKey(Minecraft minecraft) {
    if (chimereTertiaryAbilityKey == null) {
      return;
    }

    while (chimereTertiaryAbilityKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      if (!faction.isChimere()) {
        continue;
      }

      PacketDistributor.sendToServer(new ChimereGodTertiaryAbilityPayload());
    }
  }

  private static void handleElementRadialMenuKey(Minecraft minecraft) {
    if (elementRadialMenuKey == null) {
      return;
    }

    PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
    if (!faction.isTetrasomie()) {
      ElementRadialMenuOverlay.forceClose(minecraft);
      return;
    }

    if (elementRadialMenuKey.isDown()) {
      if (minecraft.screen == null && !ElementRadialMenuOverlay.isOpen()) {
        resetShieldChannelState();
        ElementRadialMenuOverlay.open(minecraft, minecraft.player.isShiftKeyDown());
      }
      return;
    }

    if (ElementRadialMenuOverlay.isOpen()) {
      ElementRadialMenuOverlay.closeAndSelect(minecraft);
    }
  }

  private static void handleChimereFlightToggleKey(Minecraft minecraft) {
    if (chimereFlightToggleKey == null) {
      return;
    }

    while (chimereFlightToggleKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      if (!minecraft.player.getData(ModAttachments.PLAYER_FACTION).isChimere()) {
        continue;
      }

      boolean enable = !ChimereFlightHelper.isFlightModeActive(minecraft.player);
      ChimereFlightHelper.setFlightMode(minecraft.player, enable);
      PacketDistributor.sendToServer(new ChimereFlightTogglePayload(enable));
    }
  }

  private static void handleFactionStatusPanelKey(Minecraft minecraft) {
    if (factionStatusPanelKey == null) {
      return;
    }

    while (factionStatusPanelKey.consumeClick()) {
      if (minecraft.screen != null) {
        continue;
      }

      PlayerFaction faction = minecraft.player.getData(ModAttachments.PLAYER_FACTION);
      if (faction.isTetrasomie()) {
        minecraft.setScreen(new FactionStatusScreen());
      }
    }
  }
}
