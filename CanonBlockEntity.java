package com.seroka.block;

import com.seroka.ModRegistry;
import com.seroka.navire.CanonNavire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ce qu'un canon garde entre deux coups : munition, poudre, visée et mèche.
 */
public class CanonBlockEntity extends BlockEntity {

  public static final String CLE_MUNITION = "munition";
  public static final String CLE_POUDRE = "poudre";
  public static final String CLE_MECHE = "meche";
  public static final String CLE_RECUL = "recul";
  public static final String CLE_REFROIDISSEMENT = "refroidissement";
  public static final String CLE_VISEE_YAW = "visee_yaw";
  public static final String CLE_VISEE_PITCH = "visee_pitch";

  private ItemStack munition = ItemStack.EMPTY;
  private boolean poudre;
  private int mecheRestante;
  private int reculRestant;
  private int refroidissement;
  private float viseeYaw = Float.NaN;
  private float viseePitch = Float.NaN;

  public CanonBlockEntity(BlockPos pos, BlockState etat) {
    super(ModRegistry.CANON_BE.get(), pos, etat);
  }

  public ItemStack munition() {
    return munition;
  }

  public boolean aMunition() {
    return !munition.isEmpty();
  }

  public boolean aPoudre() {
    return poudre;
  }

  public int mecheRestante() {
    return mecheRestante;
  }

  public int reculRestant() {
    return reculRestant;
  }

  public int refroidissementRestant() {
    return refroidissement;
  }

  /** Tube encore brûlant : ni recharge ni mèche avant qu'il ait refroidi. */
  public boolean estChaud() {
    return refroidissement > 0;
  }

  public float viseeYaw() {
    return viseeYaw;
  }

  public float viseePitch() {
    return viseePitch;
  }

  public void definirVisée(float yaw, float pitch) {
    viseeYaw = yaw;
    viseePitch = pitch;
    setChanged();
    prevenirLesClients();
  }

  public boolean pretPourMeche() {
    return aMunition() && aPoudre() && mecheRestante <= 0 && !estChaud();
  }

  public void chargerMunition(ItemStack pile) {
    munition = pile;
    setChanged();
    prevenirLesClients();
  }

  public ItemStack retirerMunition() {
    ItemStack rendue = munition;
    munition = ItemStack.EMPTY;
    setChanged();
    prevenirLesClients();
    return rendue;
  }

  public void chargerPoudre() {
    poudre = true;
    setChanged();
    prevenirLesClients();
  }

  public void retirerPoudre() {
    poudre = false;
    setChanged();
    prevenirLesClients();
  }

  public void allumerMeche(int ticks) {
    poudre = false;
    mecheRestante = ticks;
    setChanged();
    prevenirLesClients();
  }

  public void decompterMeche() {
    if (mecheRestante > 0) {
      mecheRestante--;
      setChanged();
    }
  }

  public void lancerLeRecul(int ticks) {
    reculRestant = ticks;
    setChanged();
  }

  public void deconterLeRecul() {
    if (reculRestant > 0) {
      reculRestant--;
      setChanged();
    }
  }

  public void lancerLeRefroidissement(int ticks) {
    refroidissement = ticks;
    setChanged();
    prevenirLesClients();
  }

  public void deconterLeRefroidissement() {
    if (refroidissement > 0) {
      refroidissement--;
      setChanged();
      if (refroidissement == 0) {
        prevenirLesClients();
      }
    }
  }

  /** @deprecated utiliser {@link #aMunition()} */
  @Deprecated
  public boolean estCharge() {
    return aMunition();
  }

  /** @deprecated utiliser {@link #chargerMunition(ItemStack)} */
  @Deprecated
  public void charger(ItemStack pile) {
    chargerMunition(pile);
  }

  /** @deprecated utiliser {@link #retirerMunition()} */
  @Deprecated
  public ItemStack decharger() {
    return retirerMunition();
  }

  private void prevenirLesClients() {
    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }
  }

  public static void lireEtat(CompoundTag tag, HolderLookup.Provider registres, CanonNavire cible) {
    CanonNavire.appliquerDepuisNbt(tag, registres, cible);
  }

  public static void ecrireEtat(CanonBlockEntity canon, CanonNavire source) {
    if (canon.level == null) {
      return;
    }
    CompoundTag tag = new CompoundTag();
    CanonNavire.remplirNbtDepuis(tag, canon.level.registryAccess(), source);
    canon.appliquerDepuisNbt(tag, canon.level.registryAccess());
    canon.setChanged();
    canon.prevenirLesClients();
  }

  public static void remplirNbt(CompoundTag tag, HolderLookup.Provider registres, CanonNavire source) {
    CanonNavire.remplirNbtDepuis(tag, registres, source);
  }

  public static ItemStack lireMunition(CompoundTag tag, HolderLookup.Provider registres) {
    return tag.contains(CLE_MUNITION)
        ? ItemStack.parse(registres, tag.getCompound(CLE_MUNITION)).orElse(ItemStack.EMPTY)
        : ItemStack.EMPTY;
  }

  public static void ecrireMunition(CompoundTag tag, HolderLookup.Provider registres, ItemStack pile) {
    if (!pile.isEmpty()) {
      tag.put(CLE_MUNITION, pile.save(registres));
    }
  }

  private void appliquerDepuisNbt(CompoundTag tag, HolderLookup.Provider registres) {
    munition = lireMunition(tag, registres);
    poudre = tag.getBoolean(CLE_POUDRE);
    mecheRestante = tag.getInt(CLE_MECHE);
    reculRestant = tag.getInt(CLE_RECUL);
    refroidissement = tag.getInt(CLE_REFROIDISSEMENT);
    if (tag.contains(CLE_VISEE_YAW)) {
      viseeYaw = tag.getFloat(CLE_VISEE_YAW);
      viseePitch = tag.getFloat(CLE_VISEE_PITCH);
    } else {
      viseeYaw = Float.NaN;
      viseePitch = Float.NaN;
    }
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registres) {
    super.loadAdditional(tag, registres);
    appliquerDepuisNbt(tag, registres);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registres) {
    super.saveAdditional(tag, registres);
    ecrireMunition(tag, registres, munition);
    if (poudre) {
      tag.putBoolean(CLE_POUDRE, true);
    }
    if (mecheRestante > 0) {
      tag.putInt(CLE_MECHE, mecheRestante);
    }
    if (reculRestant > 0) {
      tag.putInt(CLE_RECUL, reculRestant);
    }
    if (refroidissement > 0) {
      tag.putInt(CLE_REFROIDISSEMENT, refroidissement);
    }
    if (!Float.isNaN(viseeYaw)) {
      tag.putFloat(CLE_VISEE_YAW, viseeYaw);
      tag.putFloat(CLE_VISEE_PITCH, viseePitch);
    }
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registres) {
    CompoundTag tag = super.getUpdateTag(registres);
    saveAdditional(tag, registres);
    return tag;
  }

  @Nullable
  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
