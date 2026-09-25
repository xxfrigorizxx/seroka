package com.seroka.chimere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** État persistant des bénédictions Chimère (bascules, etc.). */
public record ChimereBlessingState(
    PlayerPrefs playerPrefs,
    TimedAbilityState lefinMeow,
    TimedAbilityState poissonGlobePoison,
    SquidAbilityStates squids,
    TimedAbilityState tortueShell,
    AquaticSpecialistsState aquaticSpecialists
) {

  public record ToggleState(boolean pouletSlowFall, boolean lapinAgility) {
    public static final ToggleState DEFAULT = new ToggleState(false, false);

    public static final Codec<ToggleState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("pouletSlowFall", false).forGetter(ToggleState::pouletSlowFall),
        Codec.BOOL.optionalFieldOf("lapinAgility", false).forGetter(ToggleState::lapinAgility)
    ).apply(instance, ToggleState::new));

    public static final StreamCodec<ByteBuf, ToggleState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        ToggleState::pouletSlowFall,
        ByteBufCodecs.BOOL,
        ToggleState::lapinAgility,
        ToggleState::new
    );

    public ToggleState withPouletSlowFall(boolean enabled) {
      return new ToggleState(enabled, lapinAgility);
    }

    public ToggleState withLapinAgility(boolean enabled) {
      return new ToggleState(pouletSlowFall, enabled);
    }
  }

  public record PlayerPrefs(ToggleState toggles, long renardStealLastTick) {
    public static final PlayerPrefs DEFAULT = new PlayerPrefs(ToggleState.DEFAULT, 0L);

    public static final Codec<PlayerPrefs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ToggleState.CODEC.optionalFieldOf("toggles", ToggleState.DEFAULT).forGetter(PlayerPrefs::toggles),
        Codec.LONG.optionalFieldOf("renardStealLastTick", 0L).forGetter(PlayerPrefs::renardStealLastTick)
    ).apply(instance, PlayerPrefs::new));

    public static final StreamCodec<ByteBuf, PlayerPrefs> STREAM_CODEC = StreamCodec.composite(
        ToggleState.STREAM_CODEC,
        PlayerPrefs::toggles,
        ByteBufCodecs.VAR_LONG,
        PlayerPrefs::renardStealLastTick,
        PlayerPrefs::new
    );

    public PlayerPrefs withPouletSlowFall(boolean enabled) {
      return new PlayerPrefs(toggles.withPouletSlowFall(enabled), renardStealLastTick);
    }

    public PlayerPrefs withLapinAgility(boolean enabled) {
      return new PlayerPrefs(toggles.withLapinAgility(enabled), renardStealLastTick);
    }

    public PlayerPrefs withRenardStealLastTick(long tick) {
      return new PlayerPrefs(toggles, tick);
    }
  }

  public record TimedAbilityState(long lastTick, long activeUntil) {
    public static final TimedAbilityState DEFAULT = new TimedAbilityState(0L, 0L);

    public static final Codec<TimedAbilityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("lastTick", 0L).forGetter(TimedAbilityState::lastTick),
        Codec.LONG.optionalFieldOf("activeUntil", 0L).forGetter(TimedAbilityState::activeUntil)
    ).apply(instance, TimedAbilityState::new));

    public static final StreamCodec<ByteBuf, TimedAbilityState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        TimedAbilityState::lastTick,
        ByteBufCodecs.VAR_LONG,
        TimedAbilityState::activeUntil,
        TimedAbilityState::new
    );

    public TimedAbilityState withLastTick(long tick) {
      return new TimedAbilityState(tick, activeUntil);
    }

    public TimedAbilityState withActiveUntil(long tick) {
      return new TimedAbilityState(lastTick, tick);
    }
  }

  public record CalamarAbilityState(long boostLastTick, long secondaryLastTick) {
    public static final CalamarAbilityState DEFAULT = new CalamarAbilityState(0L, 0L);

    public static final Codec<CalamarAbilityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("boostLastTick", 0L).forGetter(CalamarAbilityState::boostLastTick),
        Codec.LONG.optionalFieldOf("inkLastTick", 0L).forGetter(CalamarAbilityState::secondaryLastTick)
    ).apply(instance, CalamarAbilityState::new));

    public static final StreamCodec<ByteBuf, CalamarAbilityState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        CalamarAbilityState::boostLastTick,
        ByteBufCodecs.VAR_LONG,
        CalamarAbilityState::secondaryLastTick,
        CalamarAbilityState::new
    );

    public CalamarAbilityState withBoostLastTick(long tick) {
      return new CalamarAbilityState(tick, secondaryLastTick);
    }

    public CalamarAbilityState withSecondaryLastTick(long tick) {
      return new CalamarAbilityState(boostLastTick, tick);
    }
  }

  public record SquidAbilityStates(CalamarAbilityState calamar, CalamarAbilityState luminescent) {
    public static final SquidAbilityStates DEFAULT =
        new SquidAbilityStates(CalamarAbilityState.DEFAULT, CalamarAbilityState.DEFAULT);

    public static final Codec<SquidAbilityStates> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CalamarAbilityState.CODEC.optionalFieldOf("calamar", CalamarAbilityState.DEFAULT)
            .forGetter(SquidAbilityStates::calamar),
        CalamarAbilityState.CODEC.optionalFieldOf("luminescent", CalamarAbilityState.DEFAULT)
            .forGetter(SquidAbilityStates::luminescent)
    ).apply(instance, SquidAbilityStates::new));

    public static final StreamCodec<ByteBuf, SquidAbilityStates> STREAM_CODEC = StreamCodec.composite(
        CalamarAbilityState.STREAM_CODEC,
        SquidAbilityStates::calamar,
        CalamarAbilityState.STREAM_CODEC,
        SquidAbilityStates::luminescent,
        SquidAbilityStates::new
    );

    public SquidAbilityStates withCalamar(CalamarAbilityState state) {
      return new SquidAbilityStates(state, luminescent);
    }

    public SquidAbilityStates withLuminescent(CalamarAbilityState state) {
      return new SquidAbilityStates(calamar, state);
    }
  }

  public record DauphinAbilityState(long boostLastTick) {
    public static final DauphinAbilityState DEFAULT = new DauphinAbilityState(0L);

    public static final Codec<DauphinAbilityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("boostLastTick", 0L).forGetter(DauphinAbilityState::boostLastTick)
    ).apply(instance, DauphinAbilityState::new));

    public static final StreamCodec<ByteBuf, DauphinAbilityState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        DauphinAbilityState::boostLastTick,
        DauphinAbilityState::new
    );

    public DauphinAbilityState withBoostLastTick(long tick) {
      return new DauphinAbilityState(tick);
    }
  }

  public record PlayDeadRuntimeState(boolean active, double anchorX, double anchorY, double anchorZ) {
    public static final PlayDeadRuntimeState DEFAULT = new PlayDeadRuntimeState(false, 0.0D, 0.0D, 0.0D);

    public static final Codec<PlayDeadRuntimeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("active", false).forGetter(PlayDeadRuntimeState::active),
        Codec.DOUBLE.optionalFieldOf("anchorX", 0.0D).forGetter(PlayDeadRuntimeState::anchorX),
        Codec.DOUBLE.optionalFieldOf("anchorY", 0.0D).forGetter(PlayDeadRuntimeState::anchorY),
        Codec.DOUBLE.optionalFieldOf("anchorZ", 0.0D).forGetter(PlayDeadRuntimeState::anchorZ)
    ).apply(instance, PlayDeadRuntimeState::new));

    public static final StreamCodec<ByteBuf, PlayDeadRuntimeState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        PlayDeadRuntimeState::active,
        ByteBufCodecs.DOUBLE,
        PlayDeadRuntimeState::anchorX,
        ByteBufCodecs.DOUBLE,
        PlayDeadRuntimeState::anchorY,
        ByteBufCodecs.DOUBLE,
        PlayDeadRuntimeState::anchorZ,
        PlayDeadRuntimeState::new
    );

    public PlayDeadRuntimeState withActive(boolean value, double x, double y, double z) {
      return new PlayDeadRuntimeState(value, x, y, z);
    }

    public PlayDeadRuntimeState cleared() {
      return DEFAULT;
    }
  }

  public record AxolotlAbilityState(long playDeadCooldownLastTick, long regenCooldownLastTick, PlayDeadRuntimeState playDead) {
    public static final AxolotlAbilityState DEFAULT =
        new AxolotlAbilityState(0L, 0L, PlayDeadRuntimeState.DEFAULT);

    public static final Codec<AxolotlAbilityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("playDeadCooldownLastTick", 0L).forGetter(AxolotlAbilityState::playDeadCooldownLastTick),
        Codec.LONG.optionalFieldOf("regenCooldownLastTick", 0L).forGetter(AxolotlAbilityState::regenCooldownLastTick),
        PlayDeadRuntimeState.CODEC.optionalFieldOf("playDead", PlayDeadRuntimeState.DEFAULT)
            .forGetter(AxolotlAbilityState::playDead)
    ).apply(instance, AxolotlAbilityState::new));

    public static final StreamCodec<ByteBuf, AxolotlAbilityState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        AxolotlAbilityState::playDeadCooldownLastTick,
        ByteBufCodecs.VAR_LONG,
        AxolotlAbilityState::regenCooldownLastTick,
        PlayDeadRuntimeState.STREAM_CODEC,
        AxolotlAbilityState::playDead,
        AxolotlAbilityState::new
    );

    public AxolotlAbilityState withPlayDeadCooldownLastTick(long tick) {
      return new AxolotlAbilityState(tick, regenCooldownLastTick, playDead);
    }

    public AxolotlAbilityState withRegenCooldownLastTick(long tick) {
      return new AxolotlAbilityState(playDeadCooldownLastTick, tick, playDead);
    }

    public AxolotlAbilityState withPlayDead(PlayDeadRuntimeState state) {
      return new AxolotlAbilityState(playDeadCooldownLastTick, regenCooldownLastTick, state);
    }
  }

  public record GrenouilleAbilityState(long jumpLastTick) {
    public static final GrenouilleAbilityState DEFAULT = new GrenouilleAbilityState(0L);

    public static final Codec<GrenouilleAbilityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("jumpLastTick", 0L).forGetter(GrenouilleAbilityState::jumpLastTick)
    ).apply(instance, GrenouilleAbilityState::new));

    public static final StreamCodec<ByteBuf, GrenouilleAbilityState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        GrenouilleAbilityState::jumpLastTick,
        GrenouilleAbilityState::new
    );

    public GrenouilleAbilityState withJumpLastTick(long tick) {
      return new GrenouilleAbilityState(tick);
    }
  }

  public record AquaticSpecialistsState(
      DauphinAbilityState dauphin,
      AxolotlAbilityState axolotl,
      GrenouilleAbilityState grenouille
  ) {
    public static final AquaticSpecialistsState DEFAULT =
        new AquaticSpecialistsState(DauphinAbilityState.DEFAULT, AxolotlAbilityState.DEFAULT, GrenouilleAbilityState.DEFAULT);

    public static final Codec<AquaticSpecialistsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DauphinAbilityState.CODEC.optionalFieldOf("dauphin", DauphinAbilityState.DEFAULT)
            .forGetter(AquaticSpecialistsState::dauphin),
        AxolotlAbilityState.CODEC.optionalFieldOf("axolotl", AxolotlAbilityState.DEFAULT)
            .forGetter(AquaticSpecialistsState::axolotl),
        GrenouilleAbilityState.CODEC.optionalFieldOf("grenouille", GrenouilleAbilityState.DEFAULT)
            .forGetter(AquaticSpecialistsState::grenouille)
    ).apply(instance, AquaticSpecialistsState::new));

    public static final StreamCodec<ByteBuf, AquaticSpecialistsState> STREAM_CODEC = StreamCodec.composite(
        DauphinAbilityState.STREAM_CODEC,
        AquaticSpecialistsState::dauphin,
        AxolotlAbilityState.STREAM_CODEC,
        AquaticSpecialistsState::axolotl,
        GrenouilleAbilityState.STREAM_CODEC,
        AquaticSpecialistsState::grenouille,
        AquaticSpecialistsState::new
    );

    public AquaticSpecialistsState withDauphin(DauphinAbilityState state) {
      return new AquaticSpecialistsState(state, axolotl, grenouille);
    }

    public AquaticSpecialistsState withAxolotl(AxolotlAbilityState state) {
      return new AquaticSpecialistsState(dauphin, state, grenouille);
    }

    public AquaticSpecialistsState withGrenouille(GrenouilleAbilityState state) {
      return new AquaticSpecialistsState(dauphin, axolotl, state);
    }
  }

  public static final ChimereBlessingState DEFAULT = new ChimereBlessingState(
      PlayerPrefs.DEFAULT,
      TimedAbilityState.DEFAULT,
      TimedAbilityState.DEFAULT,
      SquidAbilityStates.DEFAULT,
      TimedAbilityState.DEFAULT,
      AquaticSpecialistsState.DEFAULT
  );

  public static final Codec<ChimereBlessingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      PlayerPrefs.CODEC.optionalFieldOf("playerPrefs", PlayerPrefs.DEFAULT).forGetter(ChimereBlessingState::playerPrefs),
      ToggleState.CODEC.optionalFieldOf("toggles", ToggleState.DEFAULT).forGetter(ChimereBlessingState::toggles),
      Codec.BOOL.optionalFieldOf("pouletSlowFall", false).forGetter(ChimereBlessingState::pouletSlowFall),
      Codec.BOOL.optionalFieldOf("lapinAgility", false).forGetter(ChimereBlessingState::lapinAgility),
      Codec.LONG.optionalFieldOf("renardStealLastTick", 0L).forGetter(ChimereBlessingState::renardStealLastTick),
      Codec.LONG.optionalFieldOf("lefinMeowLastTick", 0L).forGetter(ChimereBlessingState::lefinMeowLastTick),
      Codec.LONG.optionalFieldOf("lefinMeowActiveUntil", 0L).forGetter(ChimereBlessingState::lefinMeowActiveUntil),
      TimedAbilityState.CODEC.optionalFieldOf("poissonGlobePoison", TimedAbilityState.DEFAULT)
          .forGetter(ChimereBlessingState::poissonGlobePoison),
      SquidAbilityStates.CODEC.optionalFieldOf("squids", SquidAbilityStates.DEFAULT)
          .forGetter(ChimereBlessingState::squids),
      CalamarAbilityState.CODEC.optionalFieldOf("calamar", CalamarAbilityState.DEFAULT)
          .forGetter(ChimereBlessingState::calamar),
      CalamarAbilityState.CODEC.optionalFieldOf("calamarLuminescent", CalamarAbilityState.DEFAULT)
          .forGetter(ChimereBlessingState::calamarLuminescent),
      TimedAbilityState.CODEC.optionalFieldOf("tortueShell", TimedAbilityState.DEFAULT)
          .forGetter(ChimereBlessingState::tortueShell),
      DauphinAbilityState.CODEC.optionalFieldOf("dauphin", DauphinAbilityState.DEFAULT)
          .forGetter(ChimereBlessingState::dauphin),
      AquaticSpecialistsState.CODEC.optionalFieldOf("aquaticSpecialists", AquaticSpecialistsState.DEFAULT)
          .forGetter(ChimereBlessingState::aquaticSpecialists)
  ).apply(instance, (playerPrefs, toggles, pouletSlowFall, lapinAgility, renardStealLastTick, lefinMeowLastTick, lefinMeowActiveUntil, poissonGlobePoison, squids, calamar, calamarLuminescent, tortueShell, dauphin, aquaticSpecialists) -> {
    PlayerPrefs resolvedPrefs = resolvePlayerPrefs(playerPrefs, toggles, pouletSlowFall, lapinAgility, renardStealLastTick);
    SquidAbilityStates resolvedSquids = squids != SquidAbilityStates.DEFAULT
        ? squids
        : new SquidAbilityStates(calamar, calamarLuminescent);
    AquaticSpecialistsState resolvedAquatic = aquaticSpecialists != AquaticSpecialistsState.DEFAULT
        ? aquaticSpecialists
        : new AquaticSpecialistsState(dauphin, AxolotlAbilityState.DEFAULT, GrenouilleAbilityState.DEFAULT);
    return new ChimereBlessingState(
        resolvedPrefs,
        new TimedAbilityState(lefinMeowLastTick, lefinMeowActiveUntil),
        poissonGlobePoison,
        resolvedSquids,
        tortueShell,
        resolvedAquatic
    );
  }));

  public static final StreamCodec<ByteBuf, ChimereBlessingState> STREAM_CODEC = StreamCodec.composite(
      PlayerPrefs.STREAM_CODEC,
      ChimereBlessingState::playerPrefs,
      TimedAbilityState.STREAM_CODEC,
      ChimereBlessingState::lefinMeow,
      TimedAbilityState.STREAM_CODEC,
      ChimereBlessingState::poissonGlobePoison,
      SquidAbilityStates.STREAM_CODEC,
      ChimereBlessingState::squids,
      TimedAbilityState.STREAM_CODEC,
      ChimereBlessingState::tortueShell,
      AquaticSpecialistsState.STREAM_CODEC,
      ChimereBlessingState::aquaticSpecialists,
      ChimereBlessingState::new
  );

  private static PlayerPrefs resolvePlayerPrefs(
      PlayerPrefs playerPrefs,
      ToggleState toggles,
      boolean pouletSlowFall,
      boolean lapinAgility,
      long renardStealLastTick
  ) {
    if (playerPrefs != PlayerPrefs.DEFAULT) {
      return playerPrefs;
    }
    ToggleState resolvedToggles = toggles != ToggleState.DEFAULT
        ? toggles
        : new ToggleState(pouletSlowFall, lapinAgility);
    return new PlayerPrefs(resolvedToggles, renardStealLastTick);
  }

  public ToggleState toggles() {
    return playerPrefs.toggles();
  }

  public boolean pouletSlowFall() {
    return playerPrefs.toggles().pouletSlowFall();
  }

  public boolean lapinAgility() {
    return playerPrefs.toggles().lapinAgility();
  }

  public long renardStealLastTick() {
    return playerPrefs.renardStealLastTick();
  }

  public CalamarAbilityState calamar() {
    return squids.calamar();
  }

  public CalamarAbilityState calamarLuminescent() {
    return squids.luminescent();
  }

  public DauphinAbilityState dauphin() {
    return aquaticSpecialists.dauphin();
  }

  public AxolotlAbilityState axolotl() {
    return aquaticSpecialists.axolotl();
  }

  public GrenouilleAbilityState grenouille() {
    return aquaticSpecialists.grenouille();
  }

  public long lefinMeowLastTick() {
    return lefinMeow.lastTick();
  }

  public long lefinMeowActiveUntil() {
    return lefinMeow.activeUntil();
  }

  public long poissonGlobePoisonLastTick() {
    return poissonGlobePoison.lastTick();
  }

  public long poissonGlobePoisonActiveUntil() {
    return poissonGlobePoison.activeUntil();
  }

  public long calamarBoostLastTick() {
    return squids.calamar().boostLastTick();
  }

  public long calamarInkLastTick() {
    return squids.calamar().secondaryLastTick();
  }

  public long calamarLuminescentBoostLastTick() {
    return squids.luminescent().boostLastTick();
  }

  public long tortueShellLastTick() {
    return tortueShell.lastTick();
  }

  public long tortueShellActiveUntil() {
    return tortueShell.activeUntil();
  }

  public long dauphinBoostLastTick() {
    return aquaticSpecialists.dauphin().boostLastTick();
  }

  public long axolotlPlayDeadCooldownLastTick() {
    return aquaticSpecialists.axolotl().playDeadCooldownLastTick();
  }

  public long axolotlRegenCooldownLastTick() {
    return aquaticSpecialists.axolotl().regenCooldownLastTick();
  }

  public long grenouilleJumpLastTick() {
    return aquaticSpecialists.grenouille().jumpLastTick();
  }

  public boolean axolotlPlayDeadActive() {
    return aquaticSpecialists.axolotl().playDead().active();
  }

  public ChimereBlessingState withPouletSlowFall(boolean enabled) {
    return new ChimereBlessingState(
        playerPrefs.withPouletSlowFall(enabled), lefinMeow, poissonGlobePoison, squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withLapinAgility(boolean enabled) {
    return new ChimereBlessingState(
        playerPrefs.withLapinAgility(enabled), lefinMeow, poissonGlobePoison, squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withRenardStealLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs.withRenardStealLastTick(tick), lefinMeow, poissonGlobePoison, squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withLefinMeowLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow.withLastTick(tick), poissonGlobePoison, squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withLefinMeowActiveUntil(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow.withActiveUntil(tick), poissonGlobePoison, squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withPoissonGlobePoisonLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison.withLastTick(tick), squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withPoissonGlobePoisonActiveUntil(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison.withActiveUntil(tick), squids, tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withCalamarBoostLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids.withCalamar(squids.calamar().withBoostLastTick(tick)), tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withCalamarInkLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids.withCalamar(squids.calamar().withSecondaryLastTick(tick)), tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withCalamarLuminescentBoostLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids.withLuminescent(squids.luminescent().withBoostLastTick(tick)), tortueShell, aquaticSpecialists
    );
  }

  public ChimereBlessingState withTortueShellLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell.withLastTick(tick), aquaticSpecialists
    );
  }

  public ChimereBlessingState withTortueShellActiveUntil(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell.withActiveUntil(tick), aquaticSpecialists
    );
  }

  public ChimereBlessingState withDauphinBoostLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell,
        aquaticSpecialists.withDauphin(aquaticSpecialists.dauphin().withBoostLastTick(tick))
    );
  }

  public ChimereBlessingState withAxolotlPlayDeadCooldownLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell,
        aquaticSpecialists.withAxolotl(aquaticSpecialists.axolotl().withPlayDeadCooldownLastTick(tick))
    );
  }

  public ChimereBlessingState withAxolotlRegenCooldownLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell,
        aquaticSpecialists.withAxolotl(aquaticSpecialists.axolotl().withRegenCooldownLastTick(tick))
    );
  }

  public ChimereBlessingState withAxolotlPlayDead(PlayDeadRuntimeState playDead) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell,
        aquaticSpecialists.withAxolotl(aquaticSpecialists.axolotl().withPlayDead(playDead))
    );
  }

  public ChimereBlessingState withGrenouilleJumpLastTick(long tick) {
    return new ChimereBlessingState(
        playerPrefs, lefinMeow, poissonGlobePoison, squids, tortueShell,
        aquaticSpecialists.withGrenouille(aquaticSpecialists.grenouille().withJumpLastTick(tick))
    );
  }
}
