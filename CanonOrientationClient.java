package com.seroka.client;

import com.seroka.navire.CanonLogique;
import com.seroka.navire.NavireEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Orientation des canons embarqués (sync serveur).
 *
 * <p>On garde le gisement et non un cap monde : rapporté au cap que la coque a sur ce client, il
 * fait tourner la pièce avec son navire au lieu de la laisser pointée vers le large.
 */
public final class CanonOrientationClient {

  private record Orientation(float traverse, float pitch) {}

  private static final Map<Integer, Map<Long, Orientation>> PAR_NAVIRE = new HashMap<>();

  private CanonOrientationClient() {}

  public static void recevoirNavire(int navireId, BlockPos culasse, float traverse, float pitch) {
    PAR_NAVIRE.computeIfAbsent(navireId, ignore -> new HashMap<>())
        .put(culasse.asLong(), new Orientation(traverse, pitch));
  }

  public static float pitchNavire(NavireEntity navire, BlockPos culasse) {
    Orientation o = lire(navire.getId(), culasse);
    return o != null && !Float.isNaN(o.pitch) ? o.pitch : 0.0F;
  }

  public static float yawNavire(NavireEntity navire, BlockPos culasse, Direction visee) {
    Orientation o = lire(navire.getId(), culasse);
    return CanonLogique.yawTirNavire(navire, visee, o != null ? o.traverse : Float.NaN);
  }

  @Nullable
  private static Orientation lire(int navireId, BlockPos culasse) {
    Map<Long, Orientation> cartes = PAR_NAVIRE.get(navireId);
    if (cartes == null) {
      return null;
    }
    return cartes.get(culasse.asLong());
  }

  public static void oublierNavire(int navireId) {
    PAR_NAVIRE.remove(navireId);
  }
}
