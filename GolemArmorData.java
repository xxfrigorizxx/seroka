package com.seroka.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record GolemArmorData(
    boolean active,
    float currentHp,
    float maxHp,
    Block block,
    long startTick
) {

  public static final GolemArmorData EMPTY =
      new GolemArmorData(false, 0.0F, 0.0F, Blocks.DIRT, 0L);

  public static final Codec<GolemArmorData> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.BOOL.fieldOf("active").forGetter(GolemArmorData::active),
          Codec.FLOAT.fieldOf("currentHp").forGetter(GolemArmorData::currentHp),
          Codec.FLOAT.fieldOf("maxHp").forGetter(GolemArmorData::maxHp),
          BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(GolemArmorData::block),
          Codec.LONG.fieldOf("startTick").forGetter(GolemArmorData::startTick)
      ).apply(instance, GolemArmorData::new)
  );

  public static final StreamCodec<RegistryFriendlyByteBuf, GolemArmorData> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.BOOL,
          GolemArmorData::active,
          ByteBufCodecs.FLOAT,
          GolemArmorData::currentHp,
          ByteBufCodecs.FLOAT,
          GolemArmorData::maxHp,
          ByteBufCodecs.registry(Registries.BLOCK),
          GolemArmorData::block,
          ByteBufCodecs.VAR_LONG,
          GolemArmorData::startTick,
          GolemArmorData::new
      );

  public boolean isActive() {
    return active && currentHp > 0.0F;
  }

  public GolemArmorData withHp(float hp) {
    return new GolemArmorData(active, hp, maxHp, block, startTick);
  }

  public BlockState getBlockState() {
    return block != null ? block.defaultBlockState() : Blocks.DIRT.defaultBlockState();
  }
}
