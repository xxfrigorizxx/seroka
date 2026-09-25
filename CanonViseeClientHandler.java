package com.seroka.client;

import com.seroka.ModMain;
import com.seroka.block.CanonBlock;
import com.seroka.navire.CanonLogique;
import com.seroka.navire.NavireEntity;
import com.seroka.network.payload.MajViseeCanonPayload;
import com.seroka.network.payload.ServirCanonPayload;
import com.seroka.network.payload.SortirViseeCanonPayload;
import com.seroka.network.payload.SyncViseeCanonPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * En mode visée, envoie au serveur la direction du regard pour pointer le canon.
 */
@EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public final class CanonViseeClientHandler {

  /**
   * Ticks accordés au serveur pour nous asseoir au poste d'une pièce embarquée.
   *
   * <p>L'ordre d'entrer en visée arrive avant la montée à bord. Passé ce délai sans être assis,
   * c'est qu'on a perdu le poste sans en être prévenu, et le clic droit resterait confisqué.
   */
  private static final int TICKS_SANS_POSTE_MAX = 40;

  private static boolean actif;
  private static int navireId = -1;
  private static int culasseX;
  private static int culasseY;
  private static int culasseZ;
  private static boolean blocMonde;
  /** Sortie déjà demandée : le serveur répondra, inutile de le harceler chaque tick. */
  private static boolean sortieDemandee;
  private static int ticksSansPoste;

  private CanonViseeClientHandler() {}

  public static void recevoir(SyncViseeCanonPayload payload) {
    actif = payload.actif();
    navireId = payload.navireId();
    culasseX = payload.culasseX();
    culasseY = payload.culasseY();
    culasseZ = payload.culasseZ();
    blocMonde = payload.navireId() < 0;
    sortieDemandee = false;
    ticksSansPoste = 0;
  }

  public static boolean enVisée() {
    return actif;
  }

  public static float pitchVisée() {
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft.player != null
        ? CanonLogique.pitchVisée(minecraft.player.getXRot()) : 0.0F;
  }

  public static float yawVisée(Direction visee) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null) {
      return CanonLogique.yawPourDirection(visee);
    }
    if (blocMonde) {
      return CanonLogique.yawViséeSol(minecraft.player.getYRot());
    }
    NavireEntity navire = navireServi(minecraft);
    if (navire != null) {
      return CanonLogique.yawViséeNavire(navire, visee, minecraft.player.getYRot());
    }
    return CanonLogique.yawPourDirection(visee);
  }

  public static boolean viseCeCanon(int idNavire, BlockPos culasse) {
    return actif && !blocMonde && navireId == idNavire
        && culasseX == culasse.getX() && culasseY == culasse.getY() && culasseZ == culasse.getZ();
  }

  public static boolean viseCanonAuSol(BlockPos culasse) {
    return actif && blocMonde && culasseX == culasse.getX() && culasseY == culasse.getY()
        && culasseZ == culasse.getZ();
  }

  @SubscribeEvent
  public static void surTick(ClientTickEvent.Post event) {
    if (!actif) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.isPaused()) {
      return;
    }
    if (minecraft.player.isShiftKeyDown()) {
      // S'accroupir descend du poste et laisse le canon pointé là où il était à cet instant.
      if (!sortieDemandee) {
        sortieDemandee = true;
        PacketDistributor.sendToServer(new SortirViseeCanonPayload());
      }
      return;
    }
    float yaw = minecraft.player.getYRot();
    if (!blocMonde) {
      NavireEntity navire = navireServi(minecraft);
      Direction visee = navire != null ? axeDeLaPiece(navire) : null;
      if (navire == null || visee == null || minecraft.player.getVehicle() != navire) {
        if (++ticksSansPoste > TICKS_SANS_POSTE_MAX) {
          actif = false;
          PacketDistributor.sendToServer(new SortirViseeCanonPayload());
        }
        return;
      }
      ticksSansPoste = 0;
      yaw = CanonLogique.traverseNavire(navire, visee, yaw);
    }
    PacketDistributor.sendToServer(new MajViseeCanonPayload(yaw, minecraft.player.getXRot()));
  }

  /**
   * En joue, le clic droit sert la pièce au lieu du geste vanilla : charger, verser la poudre,
   * allumer la mèche.
   *
   * <p>Priorité haute : le rejeu des clics de coque, plus bas, ne doit pas voir passer ce clic et
   * mettre l'artilleur à la barre.
   */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void surClicUtiliser(InputEvent.InteractionKeyMappingTriggered event) {
    if (!actif || !event.isUseItem()) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.screen != null) {
      return;
    }
    event.setCanceled(true);
    event.setSwingHand(true);
    PacketDistributor.sendToServer(new ServirCanonPayload());
  }

  /** Une visée ne survit pas à la déconnexion : sinon le clic droit resterait confisqué ailleurs. */
  @SubscribeEvent
  public static void surDeconnexion(ClientPlayerNetworkEvent.LoggingOut event) {
    actif = false;
    navireId = -1;
    sortieDemandee = false;
    ticksSansPoste = 0;
  }

  @Nullable
  private static NavireEntity navireServi(Minecraft minecraft) {
    if (minecraft.level == null) {
      return null;
    }
    Entity entite = minecraft.level.getEntity(navireId);
    return entite instanceof NavireEntity navire ? navire : null;
  }

  @Nullable
  private static Direction axeDeLaPiece(NavireEntity navire) {
    BlockState etat = navire.structure().bloc(new BlockPos(culasseX, culasseY, culasseZ));
    return etat.getBlock() instanceof CanonBlock ? etat.getValue(CanonBlock.FACING) : null;
  }
}
