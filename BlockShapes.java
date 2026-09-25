package com.seroka.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Utilitaires de formes pour les blocs orientables sur l'horizontale. */
public final class BlockShapes {

  private BlockShapes() {}

  /**
   * Exprime une boîte décrite dans le repère {@code facing=north} vers l'orientation voulue.
   *
   * <p>Permet de ne saisir les cotes qu'une seule fois, comme dans le modèle JSON, et d'en dériver
   * les quatre orientations.
   */
  public static VoxelShape orient(Direction facing, double x1, double y1, double z1,
      double x2, double y2, double z2) {
    return switch (facing) {
      case SOUTH -> Block.box(16.0D - x2, y1, 16.0D - z2, 16.0D - x1, y2, 16.0D - z1);
      case WEST -> Block.box(z1, y1, 16.0D - x2, z2, y2, 16.0D - x1);
      case EAST -> Block.box(16.0D - z2, y1, x1, 16.0D - z1, y2, x2);
      default -> Block.box(x1, y1, z1, x2, y2, z2);
    };
  }
}
