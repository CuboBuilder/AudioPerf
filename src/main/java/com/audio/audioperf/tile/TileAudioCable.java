package com.audio.audioperf.tile;

import com.audio.audioperf.api.audio.AudioPacket;
import com.audio.audioperf.api.audio.IAudioConnection;
import com.audio.audioperf.api.audio.IAudioReceiver;
import com.audio.audioperf.audio.AudioUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

public class TileAudioCable extends BlockEntity implements IAudioReceiver {

    private static final double CORE_MIN = 0.3125;
    private static final double CORE_MAX = 0.6875;
    private static final double ARM_LENGTH = 0.375;

    private final Set<Integer> packetIds = new HashSet<>();
    private int color = 0xCCCCCC; // LightGray
    private VoxelShape cachedShape = null;

    public TileAudioCable(BlockPos pos, BlockState state) {
        super(AudioPerfBlockEntities.AUDIO_CABLE.get(), pos, state);
    }

    public void tickServer() {
        packetIds.clear();
        updateShape();
    }

    private void updateShape() {
        VoxelShape shape = Shapes.box(CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX);
        for (Direction dir : Direction.values()) {
            if (connectsAudio(dir)) {
                double from = 0, to = 0;
                switch (dir.getAxis()) {
                    case X -> { from = CORE_MIN; to = CORE_MAX; }
                    case Y -> { from = CORE_MIN; to = CORE_MAX; }
                    case Z -> { from = CORE_MIN; to = CORE_MAX; }
                }
                double minX = dir.getAxis() == Direction.Axis.X ? (dir.getStepX() < 0 ? 0 : CORE_MIN) : CORE_MIN;
                double maxX = dir.getAxis() == Direction.Axis.X ? (dir.getStepX() > 0 ? 1 : CORE_MAX) : CORE_MAX;
                double minY = dir.getAxis() == Direction.Axis.Y ? (dir.getStepY() < 0 ? 0 : CORE_MIN) : CORE_MIN;
                double maxY = dir.getAxis() == Direction.Axis.Y ? (dir.getStepY() > 0 ? 1 : CORE_MAX) : CORE_MAX;
                double minZ = dir.getAxis() == Direction.Axis.Z ? (dir.getStepZ() < 0 ? 0 : CORE_MIN) : CORE_MIN;
                double maxZ = dir.getAxis() == Direction.Axis.Z ? (dir.getStepZ() > 0 ? 1 : CORE_MAX) : CORE_MAX;
                shape = Shapes.joinUnoptimized(shape, Shapes.box(minX, minY, minZ, maxX, maxY, maxZ), BooleanOp.OR);
            }
        }
        this.cachedShape = shape.optimize();
    }

    public VoxelShape getShape() {
        if (cachedShape == null) updateShape();
        return cachedShape;
    }

    @Override
    public boolean connectsAudio(Direction side) {
        if (level == null) return false;
        BlockPos neighborPos = worldPosition.relative(side);
        if (!level.isLoaded(neighborPos)) return false;
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof TileAudioCable) {
            return true;
        }
        if (neighbor instanceof IAudioConnection conn) {
            return conn.connectsAudio(side.getOpposite());
        }
        return false;
    }

    @Override
    public void receivePacket(AudioPacket packet, Direction side) {
        if (packetIds.contains(packet.id)) return;
        packetIds.add(packet.id);

        for (Direction dir : Direction.values()) {
            if (dir == side) continue;
            if (!connectsAudio(dir)) continue;
            if (level == null || !level.isLoaded(worldPosition.relative(dir))) continue;

            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof IAudioReceiver receiver) {
                receiver.receivePacket(packet, dir.getOpposite());
            }
        }
    }

    @Override
    public Level getSoundWorld() { return null; }

    @Override
    public Vec3 getSoundPos() { return Vec3.ZERO; }

    @Override
    public int getSoundDistance() { return 0; }

    @Override
    public String getID() {
        return AudioUtils.positionId(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; setChanged(); }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("color")) {
            color = tag.getInt("color");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("color", color);
    }
}