package com.audio.audioperf.tile;

import com.audio.audioperf.api.audio.AudioPacket;
import com.audio.audioperf.api.audio.IAudioColored;
import com.audio.audioperf.api.audio.IAudioReceiver;
import com.audio.audioperf.audio.AudioUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class TileSpeaker extends BlockEntity implements IAudioReceiver, IAudioColored {
    private final Set<Integer> packetIds = new HashSet<>();
    private int color = IAudioColored.DEFAULT_COLOR;

    public TileSpeaker(BlockPos pos, BlockState state) {
        super(AudioPerfBlockEntities.SPEAKER.get(), pos, state);
    }

    public void tickServer() {
        packetIds.clear();
    }

    @Override
    public boolean connectsAudio(Direction side) {
        // The front face of the speaker does not connect to audio cables
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (side == facing) return false;
        // A cable only connects when its paint matches the speaker paint.
        if (level != null) {
            BlockPos neighborPos = worldPosition.relative(side);
            if (level.isLoaded(neighborPos) && level.getBlockEntity(neighborPos) instanceof IAudioColored colored
                    && colored.getColor() != this.color) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void receivePacket(AudioPacket packet, Direction side) {
        if (packetIds.contains(packet.id)) return;
        packetIds.add(packet.id);
        packet.addReceiver(this);
    }

    @Override
    public Level getSoundWorld() { return level; }

    @Override
    public Vec3 getSoundPos() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
    }

    @Override
    public int getSoundDistance() { return 16; }

    @Override
    public String getID() {
        return AudioUtils.positionId(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    @Override
    public int getColor() { return color; }

    @Override
    public void setColor(int color) { this.color = color; setChanged(); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("color", color);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("color")) {
            color = tag.getInt("color");
        }
    }

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