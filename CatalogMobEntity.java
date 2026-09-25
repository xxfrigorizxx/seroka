package com.seroka.catalog;

import com.seroka.catalog.generated.CatalogMobEntries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CatalogMobEntity extends PathfinderMob implements GeoEntity {

  private static final EntityDataAccessor<String> DATA_VARIANT =
      SynchedEntityData.defineId(CatalogMobEntity.class, EntityDataSerializers.STRING);

  private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

  public CatalogMobEntity(EntityType<? extends CatalogMobEntity> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return PathfinderMob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 40.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.28D)
        .add(Attributes.ATTACK_DAMAGE, 6.0D)
        .add(Attributes.FOLLOW_RANGE, 24.0D);
  }

  public void setVariant(String variant) {
    this.entityData.set(DATA_VARIANT, variant);
  }

  public String getVariant() {
    return this.entityData.get(DATA_VARIANT);
  }

  CatalogMobEntries.Entry entry() {
    return CatalogMobRegistry.entryFor(getVariant());
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(DATA_VARIANT, CatalogMobEntries.ALL[0].id());
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
    this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
  }

  private <E extends CatalogMobEntity> PlayState mainAnim(AnimationState<E> state) {
    CatalogMobEntries.Entry e = state.getAnimatable().entry();
    if (e == null) {
      return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }
    if (state.getAnimatable().swinging) {
      return state.setAndContinue(RawAnimation.begin().thenPlay(e.attackAnim()));
    }
    if (state.isMoving()) {
      return state.setAndContinue(RawAnimation.begin().thenLoop(e.walkAnim()));
    }
    return state.setAndContinue(RawAnimation.begin().thenLoop(e.idleAnim()));
  }

  @Override
  public AnimatableInstanceCache getAnimatableInstanceCache() {
    return this.cache;
  }
}
