package com.seroka.client.renderer;

import com.seroka.block.CanonBlock;
import com.seroka.navire.NavireEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Coffres, enseignes et autres meubles du bord, reconstruits seulement quand la coque a assez dérivé.
 *
 * <p>La charpente est cousue ailleurs : ici on ne s'occupe que des blocs à entité, trop particuliers
 * pour entrer dans le maillage.
 */
final class MobilierCoque {

  private static final Cargaison VIDE = new Cargaison();

  /** Blocs de dérive tolérés avant de replacer le mobilier du bord. */
  private static final int DERIVE_MOBILIER = 8;

  private final Map<NavireEntity, Cargaison> parNavire = new WeakHashMap<>();

  Cargaison pour(NavireEntity navire) {
    if (navire.structure().estVide()) {
      return VIDE;
    }
    Cargaison cargaison = parNavire.computeIfAbsent(navire, ignore -> new Cargaison());
    cargaison.rafraichirMobilier(navire);
    return cargaison;
  }

  private static List<Meuble> reconstruireMobilier(NavireEntity navire) {
    List<Meuble> meubles = new ArrayList<>();
    for (Map.Entry<BlockPos, CompoundTag> entree : navire.structure().mobilier().entrySet()) {
      BlockPos offset = entree.getKey();
      BlockState etat = navire.structure().bloc(offset);
      if (!(etat.getBlock() instanceof EntityBlock bloc)) {
        continue;
      }
      if (etat.getBlock() instanceof CanonBlock) {
        // Le canon est dessiné par son propre rendu, orienté : le reprendre ici le doublait, une
        // pièce pointée sur la visée et une restée droite par-dessus.
        continue;
      }
      BlockEntity meuble = bloc.newBlockEntity(navire.positionMonde(offset), etat);
      if (meuble == null) {
        continue;
      }
      meuble.setLevel(navire.level());
      meuble.loadWithComponents(entree.getValue(), navire.registryAccess());
      meubles.add(new Meuble(offset, meuble));
    }
    return meubles;
  }

  record Meuble(BlockPos offset, BlockEntity entite) {}

  static final class Cargaison {
    List<Meuble> meubles = List.of();
    private BlockPos ancrageMeubles;
    private int versionStructure = Integer.MIN_VALUE;

    void rafraichirMobilier(NavireEntity navire) {
      int version = navire.structure().version();
      BlockPos ancrage = navire.blockPosition();
      if (versionStructure == version
          && ancrageMeubles != null
          && ancrageMeubles.distSqr(ancrage) < DERIVE_MOBILIER * DERIVE_MOBILIER) {
        return;
      }
      versionStructure = version;
      ancrageMeubles = ancrage;
      meubles = reconstruireMobilier(navire);
    }
  }
}
