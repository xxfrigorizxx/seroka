package com.seroka.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.seroka.ModMain;
import com.seroka.navire.NavireEntity;
import com.seroka.network.payload.ReglerVoilesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Commande du gréement au clavier, pour le marin qui est à bord.
 *
 * <p>La flèche du bas fait descendre la toile, celle du haut la remonte : le geste suit ce que la
 * voile fait. L'ordre ne part qu'à l'appui, jamais sur la répétition du clavier, et il est absolu :
 * garder la touche enfoncée ne peut pas faire battre la voile.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class VoileClientHandler {

  private VoileClientHandler() {}

  @SubscribeEvent
  public static void surTouche(InputEvent.Key event) {
    if (event.getAction() != InputConstants.PRESS) {
      return;
    }
    boolean deployer = event.getKey() == GLFW.GLFW_KEY_DOWN;
    if (!deployer && event.getKey() != GLFW.GLFW_KEY_UP) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.screen != null) {
      return;
    }
    if (NavireEntity.porteurDe(minecraft.player) == null) {
      return;
    }
    PacketDistributor.sendToServer(new ReglerVoilesPayload(deployer));
  }
}
