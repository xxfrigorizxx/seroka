package com.seroka.block;

import net.minecraft.util.StringRepresentable;

/** Les deux blocs d'un canon : la culasse qu'on charge, la bouche par où le coup sort. */
public enum CanonPartie implements StringRepresentable {
  CULASSE("culasse"),
  BOUCHE("bouche");

  private final String nom;

  CanonPartie(String nom) {
    this.nom = nom;
  }

  @Override
  public String getSerializedName() {
    return nom;
  }
}
