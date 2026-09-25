package com.seroka.block;

import com.mojang.serialization.MapCodec;
import com.seroka.navire.CanonLogique;
import com.seroka.navire.InteractionsCanon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Canon de marine : un bloc de haut, deux de long.
 *
 * <p>La culasse est le bloc posé, la bouche celui qui part devant : c'est elle qui regarde le large.
 * Ce qu'on glisse dans la culasse est ce que le coup enverra, bloc ou objet ; la culasse garde donc
 * sa munition comme un fourneau garde son charbon.
 *
 * <p>Le recul demande un troisième bloc libre derrière la culasse. Rien ne l'exige à la pose — un
 * canon adossé à une cloison se charge et tire tout de même — mais sans cette place il ne pourra pas
 * s'ébranler à la détonation.
 */
public class CanonBlock extends HorizontalDirectionalBlock
    implements SimpleWaterloggedBlock, EntityBlock {

  public static final MapCodec<CanonBlock> CODEC = simpleCodec(CanonBlock::new);

  public static final EnumProperty<CanonPartie> PARTIE =
      EnumProperty.create("partie", CanonPartie.class);

  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  private static final Map<Direction, VoxelShape> FORMES_CULASSE = new EnumMap<>(Direction.class);
  private static final Map<Direction, VoxelShape> FORMES_BOUCHE = new EnumMap<>(Direction.class);

  static {
    for (Direction visee : Direction.Plane.HORIZONTAL) {
      FORMES_CULASSE.put(visee, Shapes.or(
          BlockShapes.orient(visee, 3.0D, 0.0D, 2.0D, 13.0D, 5.0D, 16.0D),
          BlockShapes.orient(visee, 4.0D, 4.5D, 0.0D, 12.0D, 12.5D, 16.0D)));
      FORMES_BOUCHE.put(visee, Shapes.or(
          BlockShapes.orient(visee, 3.0D, 0.0D, 0.0D, 13.0D, 5.0D, 14.0D),
          BlockShapes.orient(visee, 4.0D, 4.5D, 1.0D, 12.0D, 12.5D, 16.0D)));
    }
  }

  public CanonBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any()
        .setValue(FACING, Direction.NORTH)
        .setValue(PARTIE, CanonPartie.CULASSE)
        .setValue(WATERLOGGED, Boolean.FALSE));
  }

  @Override
  protected MapCodec<CanonBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, PARTIE, WATERLOGGED);
  }

  /** Position de la culasse, qu'on ait cliqué sur elle ou sur la bouche. */
  public static BlockPos culasse(BlockState state, BlockPos pos) {
    return state.getValue(PARTIE) == CanonPartie.CULASSE
        ? pos
        : pos.relative(state.getValue(FACING).getOpposite());
  }

  /** Bloc qui reçoit le canon quand il recule : le troisième, derrière la culasse. */
  public static BlockPos placeDeRecul(BlockState state, BlockPos culasse) {
    return culasse.relative(state.getValue(FACING).getOpposite());
  }

  private boolean estLautrePartie(BlockState state, BlockState autre) {
    return autre.is(this)
        && autre.getValue(FACING) == state.getValue(FACING)
        && autre.getValue(PARTIE) != state.getValue(PARTIE);
  }

  // ------------------------------------------------------------------- pose

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Level level = context.getLevel();
    BlockPos culasse = context.getClickedPos();
    // La bouche part devant le poseur : on met le canon en batterie dans le sens du regard.
    Direction visee = context.getHorizontalDirection();
    if (!level.getBlockState(culasse.relative(visee)).canBeReplaced(context)) {
      return null;
    }
    return defaultBlockState()
        .setValue(FACING, visee)
        .setValue(PARTIE, CanonPartie.CULASSE)
        .setValue(WATERLOGGED, level.getFluidState(culasse).getType() == Fluids.WATER);
  }

  @Override
  public void setPlacedBy(Level level, BlockPos pos, BlockState state,
      @Nullable LivingEntity poseur, ItemStack pile) {
    BlockPos bouche = pos.relative(state.getValue(FACING));
    // La ligne d'eau peut passer entre les deux blocs : chacun retient son propre remplissage.
    boolean dansEau = level.getFluidState(bouche).getType() == Fluids.WATER;
    level.setBlock(bouche, state
        .setValue(PARTIE, CanonPartie.BOUCHE)
        .setValue(WATERLOGGED, dansEau), Block.UPDATE_ALL);
  }

  @Override
  protected FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    if (state.getValue(PARTIE) == CanonPartie.CULASSE) {
      return true;
    }
    return estLautrePartie(state,
        level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())));
  }

  @Override
  protected BlockState updateShape(BlockState state, Direction direction, BlockState voisin,
      LevelAccessor level, BlockPos pos, BlockPos posVoisin) {
    if (state.getValue(WATERLOGGED)) {
      level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }
    Direction versLautrePartie = state.getValue(PARTIE) == CanonPartie.CULASSE
        ? state.getValue(FACING)
        : state.getValue(FACING).getOpposite();
    if (direction == versLautrePartie && !estLautrePartie(state, voisin)) {
      // Une moitié cassée emporte l'autre : un demi-canon ne veut rien dire.
      return Blocks.AIR.defaultBlockState();
    }
    return state;
  }

  // -------------------------------------------------------------- chargement

  @Override
  protected ItemInteractionResult useItemOn(ItemStack pile, BlockState state, Level level,
      BlockPos pos, Player joueur, InteractionHand main, BlockHitResult touche) {
    if (level.isClientSide()) {
      return ItemInteractionResult.SUCCESS;
    }
    if (joueur instanceof ServerPlayer serveur
        && InteractionsCanon.interagirBloc(level, pos, state, serveur, main, pile)) {
      return ItemInteractionResult.CONSUME;
    }
    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
      Player joueur, BlockHitResult touche) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }
    if (joueur instanceof ServerPlayer serveur
        && InteractionsCanon.interagirBloc(level, pos, state, serveur, InteractionHand.MAIN_HAND,
            ItemStack.EMPTY)) {
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
      BlockEntityType<T> type) {
    if (level.isClientSide() || state.getValue(PARTIE) != CanonPartie.CULASSE) {
      return null;
    }
    return (niveau, position, etat, entite) -> CanonLogique.tickBloc((CanonBlockEntity) entite);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    // Seule la culasse tient la munition : la bouche n'est qu'un bout de fonte.
    return state.getValue(PARTIE) == CanonPartie.CULASSE ? new CanonBlockEntity(pos, state) : null;
  }

  // ------------------------------------------------------------- présentation

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.INVISIBLE;
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
      CollisionContext context) {
    Map<Direction, VoxelShape> formes =
        state.getValue(PARTIE) == CanonPartie.CULASSE ? FORMES_CULASSE : FORMES_BOUCHE;
    return formes.get(state.getValue(FACING));
  }

  @Override
  protected BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  protected BlockState mirror(BlockState state, Mirror mirror) {
    return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
  }
}
