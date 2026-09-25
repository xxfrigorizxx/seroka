package com.seroka.chimere;

import com.seroka.ModMain;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quotas globaux serveur : qui occupe la bénédiction de chaque dieu en ce moment.
 */
public final class GodBlessingRegistry extends SavedData {

  private static final String DATA_ID = ModMain.MODID + "_god_blessings";
  private static final String TAG_GODS = "gods";
  private static final String TAG_GOD_ID = "id";
  private static final String TAG_PLAYERS = "players";
  private static final String TAG_TOTAL_CHIMERE_ROLLS = "totalChimereRolls";

  private final Map<String, Set<UUID>> activeByGod = new HashMap<>();
  private int totalChimereRolls;

  public GodBlessingRegistry() {}

  public static GodBlessingRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
    GodBlessingRegistry data = new GodBlessingRegistry();
    data.totalChimereRolls = tag.getInt(TAG_TOTAL_CHIMERE_ROLLS);
    ListTag gods = tag.getList(TAG_GODS, Tag.TAG_COMPOUND);
    for (int i = 0; i < gods.size(); i++) {
      CompoundTag entry = gods.getCompound(i);
      String godId = entry.getString(TAG_GOD_ID);
      Set<UUID> players = new HashSet<>();
      ListTag playerList = entry.getList(TAG_PLAYERS, Tag.TAG_INT_ARRAY);
      for (int j = 0; j < playerList.size(); j++) {
        players.add(NbtUtils.loadUUID(playerList.get(j)));
      }
      if (!players.isEmpty()) {
        data.activeByGod.put(godId, players);
      }
    }
    return data;
  }

  public static GodBlessingRegistry get(MinecraftServer server) {
    return server.overworld()
        .getDataStorage()
        .computeIfAbsent(
            new Factory<>(GodBlessingRegistry::new, GodBlessingRegistry::load, DataFixTypes.LEVEL),
            DATA_ID
        );
  }

  public int getActiveCount(String godId) {
    return activeByGod.getOrDefault(godId, Set.of()).size();
  }

  public int getTotalChimereRolls() {
    return totalChimereRolls;
  }

  public void recordChimereRoll() {
    totalChimereRolls++;
    setDirty();
  }

  public boolean hasCapacity(GodDefinition god) {
    return god.hasCapacity(getActiveCount(god.id()));
  }

  public boolean tryClaim(GodDefinition god, UUID playerId) {
    Set<UUID> active = activeByGod.computeIfAbsent(god.id(), id -> new HashSet<>());
    if (active.contains(playerId)) {
      return true;
    }
    if (active.size() >= god.maxBlessings()) {
      return false;
    }
    active.add(playerId);
    setDirty();
    return true;
  }

  public void release(UUID playerId) {
    boolean changed = false;
    for (Set<UUID> active : activeByGod.values()) {
      if (active.remove(playerId)) {
        changed = true;
        break;
      }
    }
    if (changed) {
      activeByGod.values().removeIf(Set::isEmpty);
      setDirty();
    }
  }

  /** Nettoie les quotas sauvegardés (dieux inconnus, dépassements). */
  public void reconcile() {
    boolean changed = false;

    for (String godId : new ArrayList<>(activeByGod.keySet())) {
      GodDefinition definition = GodCatalog.findById(godId);
      if (definition == null) {
        activeByGod.remove(godId);
        changed = true;
        continue;
      }

      Set<UUID> active = activeByGod.get(godId);
      if (active.size() <= definition.maxBlessings()) {
        continue;
      }

      Set<UUID> trimmed = new HashSet<>();
      int kept = 0;
      for (UUID playerId : active) {
        if (kept >= definition.maxBlessings()) {
          changed = true;
          continue;
        }
        trimmed.add(playerId);
        kept++;
      }
      activeByGod.put(godId, trimmed);
      changed = true;
    }

    if (changed) {
      activeByGod.values().removeIf(Set::isEmpty);
      setDirty();
    }
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    ListTag gods = new ListTag();
    for (Map.Entry<String, Set<UUID>> entry : activeByGod.entrySet()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      CompoundTag godTag = new CompoundTag();
      godTag.putString(TAG_GOD_ID, entry.getKey());
      ListTag players = new ListTag();
      for (UUID playerId : entry.getValue()) {
        players.add(NbtUtils.createUUID(playerId));
      }
      godTag.put(TAG_PLAYERS, players);
      gods.add(godTag);
    }
    tag.put(TAG_GODS, gods);
    tag.putInt(TAG_TOTAL_CHIMERE_ROLLS, totalChimereRolls);
    return tag;
  }
}
