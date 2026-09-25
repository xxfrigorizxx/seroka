package com.seroka.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Safran de navire : mèche verticale et pale orientée dans l'axe du bateau.
 *
 * <p>Sert de repère de construction. Le bandeau rouge de son modèle, à mi-hauteur du bloc, marque
 * la ligne de flottaison visée ; {@link #hauteurFlottaison(BlockPos)} renvoie cette hauteur pour le
 * futur assemblage du navire. Le bloc se remplit d'eau pour ne pas creuser de poche d'air sous la
 * coque.
 */
public class SafranBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

  public static final MapCodec<SafranBlock> CODEC = simpleCodec(SafranBlock::new);

  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  /** Hauteur du repère de flottaison, en fraction de bloc depuis le bas du safran. */
  public static final double FLOTTAISON = 0.5D;

  private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

  static {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      SHAPES.put(facing, BlockShapes.orient(facing, 6.0D, 0.0D, 2.0D, 10.0D, 16.0D, 15.0D));
    }
  }

  public SafranBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any()
        .setValue(FACING, Direction.NORTH)
        .setValue(WATERLOGGED, Boolean.FALSE));
  }

  /** Altitude de la ligne d'eau visée par un safran posé à cette position. */
  public static double hauteurFlottaison(BlockPos pos) {
    return pos.getY() + FLOTTAISON;
  }

  @Override
  protected MapCodec<SafranBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, WATERLOGGED);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    boolean dansEau = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
    return defaultBlockState()
        .setValue(FACING, context.getHorizontalDirection().getOpposite())
        .setValue(WATERLOGGED, dansEau);
  }

  @Override
  protected FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Override
  protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    if (state.getValue(WATERLOGGED)) {
      level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }
    return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPES.get(state.getValue(FACING));
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
