package com.seroka.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;

import java.util.Optional;
import java.util.function.Function;

/**
 * Oriente le corps selon la direction de roulade.
 * Adapté depuis Combat Roll (ZsoltMolnarrr/CombatRoll, MIT).
 */
public final class RollAdjustmentModifier extends AbstractModifier {

  public record PartModifier(Vec3f rotation, Vec3f offset) {}

  public boolean enabled = true;

  private final Function<String, Optional<PartModifier>> source;

  public RollAdjustmentModifier(Function<String, Optional<PartModifier>> source) {
    this.source = source;
  }

  private float getFadeIn(float delta) {
    if (getAnim() instanceof KeyframeAnimationPlayer player) {
      float currentTick = player.getTick() + delta;
      float fadeIn = currentTick / (float) player.getData().beginTick;
      return Math.min(fadeIn, 1.0f);
    }
    return 1.0f;
  }

  private float getFadeOut(float delta) {
    if (getAnim() instanceof KeyframeAnimationPlayer player) {
      float currentTick = player.getTick() + delta;
      float position = (-1.0f) * (currentTick - player.getData().stopTick);
      float length = player.getData().stopTick - player.getData().endTick;
      if (length > 0.0f) {
        return Math.min(position / length, 1.0f);
      }
    }
    return 1.0f;
  }

  @Override
  public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0) {
    if (!enabled) {
      return super.get3DTransform(modelName, type, tickDelta, value0);
    }

    Optional<PartModifier> partModifier = source.apply(modelName);
    float fade = getFadeIn(tickDelta) * getFadeOut(tickDelta);
    if (partModifier.isPresent()) {
      Vec3f modifiedVector = super.get3DTransform(modelName, type, tickDelta, value0);
      return transformVector(modifiedVector, type, partModifier.get(), fade);
    }
    return super.get3DTransform(modelName, type, tickDelta, value0);
  }

  private static Vec3f transformVector(Vec3f vector, TransformType type, PartModifier partModifier, float fade) {
    return switch (type) {
      case POSITION -> vector.add(partModifier.offset);
      case ROTATION -> vector.add(partModifier.rotation.scale(fade));
      case BEND, SCALE -> vector;
    };
  }
}
