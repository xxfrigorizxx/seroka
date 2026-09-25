package com.seroka.catalog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class CatalogMobSpawnEggItem extends Item {

  private final String variant;

  public CatalogMobSpawnEggItem(String variant, Properties properties) {
    super(properties);
    this.variant = variant;
  }

  public String getVariant() {
    return this.variant;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    if (!(level instanceof ServerLevel serverLevel)) {
      return InteractionResult.SUCCESS;
    }

    BlockPos clicked = context.getClickedPos();
    Direction face = context.getClickedFace();
    BlockPos spawnPos = clicked.relative(face);
    BlockState state = level.getBlockState(spawnPos);

    if (!state.getCollisionShape(level, spawnPos).isEmpty()) {
      return InteractionResult.FAIL;
    }

    Vec3 spawn = Vec3.atBottomCenterOf(spawnPos);
    CatalogMobEntity mob = CatalogMobRegistry.CATALOG_MOB.get().create(serverLevel);
    if (mob == null) {
      return InteractionResult.FAIL;
    }

    mob.moveTo(spawn.x, spawn.y, spawn.z, context.getRotation(), 0.0F);
    mob.setVariant(this.variant);
    mob.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWN_EGG, null);
    if (!serverLevel.addFreshEntity(mob)) {
      return InteractionResult.FAIL;
    }

    context.getItemInHand().shrink(1);
    level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
    return InteractionResult.CONSUME;
  }
}
