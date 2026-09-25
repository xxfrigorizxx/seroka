package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.navire.NavireEntity;
import com.seroka.network.payload.ClicNavirePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Clic droit sur un navire : c'est ce client qui dit quel bloc il visait.
 *
 * <p>Il voit la coque exactement là où le joueur la voit, alors que le serveur a quelques ticks
 * d'avance sur elle : à pleine allure, sa propre visée tombait plusieurs blocs derrière la cible.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class NavireClientHandler {

  private NavireClientHandler() {}

  @SubscribeEvent(priority = EventPriority.LOW)
  public static void surClicUtiliser(InputEvent.InteractionKeyMappingTriggered event) {
    if (!event.isUseItem()) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer joueur = minecraft.player;
    if (joueur == null || minecraft.screen != null) {
      return;
    }
    if (minecraft.hitResult instanceof EntityHitResult vise
        && vise.getEntity() instanceof NavireEntity touche) {
      // Le geste vanilla suit son cours : c'est lui qui fait manger, boire ou poser un bloc quand
      // le navire n'a rien à offrir sous le curseur. Le serveur, lui, n'agit que sur ce paquet.
      envoyer(event, touche, joueur);
      return;
    }
    NavireEntity porteur = NavireEntity.porteurDe(joueur);
    if (porteur == null || porteur.structure().estVide()) {
      return;
    }
    BlockPos bloc = porteur.blocVisePar(joueur);
    if (bloc == null || leDecorEstDevant(minecraft.hitResult, joueur, porteur.centreMonde(bloc))) {
      return;
    }
    // Le jeu a retenu autre chose que la coque, plus loin : c'est pourtant elle que le joueur vise.
    envoyer(event, porteur, joueur);
    event.setCanceled(true);
    event.setSwingHand(true);
  }

  private static void envoyer(InputEvent.InteractionKeyMappingTriggered event, NavireEntity navire,
      LocalPlayer joueur) {
    boolean coqueConnue = !navire.structure().estVide();
    BlockPos bloc = coqueConnue ? navire.blocVisePar(joueur) : null;
    PacketDistributor.sendToServer(new ClicNavirePayload(navire.getId(), event.getHand(),
        coqueConnue, Optional.ofNullable(bloc)));
  }

  private static boolean leDecorEstDevant(@Nullable HitResult touche, LocalPlayer joueur,
      Vec3 blocDeCoque) {
    if (touche == null || touche.getType() == HitResult.Type.MISS) {
      return false;
    }
    Vec3 oeil = joueur.getEyePosition();
    return oeil.distanceToSqr(touche.getLocation()) < oeil.distanceToSqr(blocDeCoque);
  }
}
