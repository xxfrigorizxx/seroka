package com.seroka.block;

import com.mojang.serialization.MapCodec;
import com.seroka.navire.AnalyseNavire;
import com.seroka.navire.AssemblageNavire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
 * Roue de gouvernail de navire.
 *
 * <p>Posée au sol elle occupe deux blocs de haut : socle et colonne en bas, roue centrée sur la
 * jointure des deux blocs pour une hauteur totale d'un bloc et demi. Adossée à la face d'un bloc
 * elle tient sur un seul bloc, avec une roue plus compacte calée dans sa moitié basse. Le bloc se
 * remplit d'eau pour ne pas creuser de poche d'air sous la ligne de flottaison.
 */
public class GouvernailBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

  public static final MapCodec<GouvernailBlock> CODEC = simpleCodec(GouvernailBlock::new);

  /** Vrai lorsque la roue est plaquée contre la face d'un bloc plutôt que montée sur son socle. */
  public static final BooleanProperty WALL = BooleanProperty.create("wall");

  public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  private static final Map<Direction, VoxelShape> SHAPES_SOCLE = new EnumMap<>(Direction.class);
  private static final Map<Direction, VoxelShape> SHAPES_ROUE_HAUTE = new EnumMap<>(Direction.class);
  private static final Map<Direction, VoxelShape> SHAPES_MUR = new EnumMap<>(Direction.class);

  static {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      SHAPES_SOCLE.put(facing, Shapes.or(
          BlockShapes.orient(facing, 4.0D, 0.0D, 8.0D, 12.0D, 16.0D, 15.0D),
          BlockShapes.orient(facing, 0.0D, 8.0D, 2.0D, 16.0D, 16.0D, 9.0D)));
      SHAPES_ROUE_HAUTE.put(facing, BlockShapes.orient(facing, 0.0D, 0.0D, 2.0D, 16.0D, 8.0D, 9.0D));
      SHAPES_MUR.put(facing, BlockShapes.orient(facing, 2.0D, 0.0D, 9.0D, 14.0D, 12.0D, 16.0D));
    }
  }

  public GouvernailBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any()
        .setValue(FACING, Direction.NORTH)
        .setValue(WALL, Boolean.FALSE)
        .setValue(HALF, DoubleBlockHalf.LOWER)
        .setValue(WATERLOGGED, Boolean.FALSE));
  }

  /** Vrai si l'état décrit l'autre moitié de la même colonne posée au sol. */
  private boolean estAutreMoitie(BlockState state, BlockState other) {
    return other.is(this)
        && !other.getValue(WALL)
        && other.getValue(HALF) != state.getValue(HALF);
  }

  @Override
  protected MapCodec<GouvernailBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, WALL, HALF, WATERLOGGED);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    BlockPos pos = context.getClickedPos();
    boolean dansEau = context.getLevel().getFluidState(pos).getType() == Fluids.WATER;
    Direction clicked = context.getClickedFace();
    if (clicked.getAxis().isHorizontal()) {
      // Clic sur le flanc d'un bloc : la roue s'y adosse et regarde vers l'extérieur.
      return defaultBlockState()
          .setValue(FACING, clicked)
          .setValue(WALL, Boolean.TRUE)
          .setValue(WATERLOGGED, dansEau);
    }
    if (!context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
      return null;
    }
    return defaultBlockState()
        .setValue(FACING, context.getHorizontalDirection().getOpposite())
        .setValue(WATERLOGGED, dansEau);
  }

  @Override
  public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    if (state.getValue(WALL)) {
      return;
    }
    BlockPos dessus = pos.above();
    // La ligne d'eau peut passer entre les deux moitiés : chacune retient son propre remplissage.
    boolean dansEau = level.getFluidState(dessus).getType() == Fluids.WATER;
    level.setBlock(dessus, state
        .setValue(HALF, DoubleBlockHalf.UPPER)
        .setValue(WATERLOGGED, dansEau), Block.UPDATE_ALL);
  }

  @Override
  protected FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  /**
   * Clic droit : détache le navire du monde. Accroupi, se contente d'en afficher la fiche.
   */
  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!level.isClientSide()) {
      AnalyseNavire.Resultat resultat = AnalyseNavire.analyser(level, baseDeLaBarre(state, pos));
      if (!resultat.estReussi()) {
        player.displayClientMessage(resultat.erreur(), false);
      } else if (player.isSecondaryUseActive()) {
        resultat.fiche().lignes().forEach(ligne -> player.displayClientMessage(ligne, false));
      } else if (!resultat.fiche().flotte()) {
        resultat.fiche().lignes().forEach(ligne -> player.displayClientMessage(ligne, false));
        player.displayClientMessage(
            Component.translatable("navire.seroka.assemblage.refuse"), false);
      } else if (AssemblageNavire.assembler(level, resultat.coque(), resultat.fiche()) == null) {
        player.displayClientMessage(
            Component.translatable("navire.seroka.assemblage.trop_grand_boite"), false);
      } else {
        player.displayClientMessage(
            Component.translatable("navire.seroka.assemblage.reussi"), false);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  /** Ramène à la moitié basse, pour que l'analyse démarre au même endroit dans les deux cas. */
  private static BlockPos baseDeLaBarre(BlockState state, BlockPos pos) {
    boolean moitieHaute = !state.getValue(WALL) && state.getValue(HALF) == DoubleBlockHalf.UPPER;
    return moitieHaute ? pos.below() : pos;
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    if (state.getValue(WALL) || state.getValue(HALF) == DoubleBlockHalf.LOWER) {
      return true;
    }
    return estAutreMoitie(state, level.getBlockState(pos.below()));
  }

  @Override
  protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(WATERLOGGED)) {
      level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }
    if (state.getValue(WALL)) {
      return state;
    }
    Direction versAutreMoitie = state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
    if (direction == versAutreMoitie && !estAutreMoitie(state, neighbor)) {
      return Blocks.AIR.defaultBlockState();
    }
    return state;
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    Map<Direction, VoxelShape> shapes;
    if (state.getValue(WALL)) {
      shapes = SHAPES_MUR;
    } else {
      shapes = state.getValue(HALF) == DoubleBlockHalf.LOWER ? SHAPES_SOCLE : SHAPES_ROUE_HAUTE;
    }
    return shapes.get(state.getValue(FACING));
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
