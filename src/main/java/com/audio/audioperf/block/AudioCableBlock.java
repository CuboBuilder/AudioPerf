package com.audio.audioperf.block;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.api.audio.IAudioColored;
import com.audio.audioperf.tile.TileAudioCable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AudioCableBlock extends Block implements EntityBlock {
    private static final VoxelShape CORE = Shapes.box(0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875);

    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    public AudioCableBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(DOWN, false)
                .setValue(UP, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    public static BlockState getStateFor(BlockState state, TileAudioCable cable) {
        return state
                .setValue(DOWN, cable.connectsAudio(Direction.DOWN))
                .setValue(UP, cable.connectsAudio(Direction.UP))
                .setValue(NORTH, cable.connectsAudio(Direction.NORTH))
                .setValue(SOUTH, cable.connectsAudio(Direction.SOUTH))
                .setValue(WEST, cable.connectsAudio(Direction.WEST))
                .setValue(EAST, cable.connectsAudio(Direction.EAST));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (!level.isClientSide() && level instanceof Level l) {
            BlockEntity be = l.getBlockEntity(currentPos);
            if (be instanceof TileAudioCable cable) {
                BlockState newState = getStateFor(state, cable);
                if (!newState.equals(state)) {
                    l.setBlock(currentPos, newState, 2);
                }
                cable.tickServer();
            } else {
                l.scheduleTick(currentPos, this, 1);
            }
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileAudioCable cable) {
            BlockState newState = getStateFor(state, cable);
            if (!newState.equals(state)) {
                level.setBlock(pos, newState, 2);
            }
            cable.tickServer();
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ShearsItem && level.getBlockEntity(pos) instanceof TileAudioCable cable) {
            if (!level.isClientSide) {
                if (cable.getCasing() != null) {
                    // Clear the casing and give it back; the cable stays.
                    BlockState casing = cable.getCasing();
                    cable.setCasing(null);
                    syncCasing(level, pos, cable);
                    ItemStack drop = new ItemStack(casing.getBlock());
                    if (!drop.isEmpty()) {
                        Block.popResource(level, pos, drop);
                    }
                    damageShears(player, stack);
                } else {
                    // No casing: cutting works instantly in any gamemode and always drops the cable.
                    Block.popResource(level, pos, new ItemStack(AudioPerf.AUDIO_CABLE.get()));
                    level.levelEvent(null, 2001, pos, Block.getId(state));
                    level.removeBlock(pos, false);
                    damageShears(player, stack);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof DyeItem dye && level.getBlockEntity(pos) instanceof TileAudioCable cable) {
            if (!level.isClientSide) {
                int color = IAudioColored.dyeColor(dye);
                if (cable.getColor() != color) {
                    cable.setColor(color);
                    cable.refreshConnections();
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                                serverLevel, new net.minecraft.world.level.ChunkPos(pos),
                                new com.audio.audioperf.network.AudioCableColorPayload(pos, color));
                    }
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof BlockItem blockItem && level.getBlockEntity(pos) instanceof TileAudioCable cable) {
            BlockState casingCandidate = blockItem.getBlock().defaultBlockState();
            if (!isValidCasing(level, pos, casingCandidate)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                BlockState old = cable.getCasing();
                if (old != null && old.getBlock() == casingCandidate.getBlock()) {
                    return ItemInteractionResult.sidedSuccess(false);
                }
                if (old != null) {
                    ItemStack drop = new ItemStack(old.getBlock());
                    if (!drop.isEmpty()) {
                        Block.popResource(level, pos, drop);
                    }
                }
                cable.setCasing(casingCandidate);
                syncCasing(level, pos, cable);
                level.playSound(null, pos, casingCandidate.getSoundType().getPlaceSound(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** Only full-cube solid blocks without a block entity can be a casing. */
    public static boolean isValidCasing(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.hasBlockEntity()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        if (state.getBlock().asItem() == Items.AIR) return false;
        return Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }

    private static void damageShears(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild && stack.isDamageableItem()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                stack.shrink(1);
            }
        }
    }

    /** Resyncs casing to clients (update tag) and forces a rerender via the color payload path. */
    private static void syncCasing(Level level, BlockPos pos, TileAudioCable cable) {
        level.sendBlockUpdated(pos, cable.getBlockState(), cable.getBlockState(), 3);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel, new net.minecraft.world.level.ChunkPos(pos),
                    new com.audio.audioperf.network.AudioCableColorPayload(pos, cable.getColor()));
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileAudioCable cable) {
            if (cable.hasCasing()) return Shapes.block();
            return cable.getShape();
        }
        return CORE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileAudioCable cable && cable.hasCasing()) {
            return Shapes.block();
        }
        return super.getCollisionShape(state, level, pos, ctx);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileAudioCable cable && cable.hasCasing()) {
            return Shapes.block();
        }
        return super.getOcclusionShape(state, level, pos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Mining a cased cable in survival also returns the casing block.
        if (!level.isClientSide && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof TileAudioCable cable
                && cable.getCasing() != null) {
            ItemStack drop = new ItemStack(cable.getCasing().getBlock());
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAudioCable(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}