package com.seroka.chimere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Bénédiction active d'un joueur Chimère (die u tiré à la sélection de faction). */
public record ChimereBlessing(String godId) {

  public static final ChimereBlessing NONE = new ChimereBlessing("");

  public static final Codec<ChimereBlessing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("godId", "").forGetter(ChimereBlessing::godId)
  ).apply(instance, ChimereBlessing::new));

  public static final StreamCodec<ByteBuf, ChimereBlessing> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      ChimereBlessing::godId,
      ChimereBlessing::new
  );

  public boolean isActive() {
    return godId != null && !godId.isEmpty();
  }
}
