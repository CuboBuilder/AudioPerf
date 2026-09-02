package com.audio.audioperf.tile;

import com.audio.audioperf.api.audio.AudioPacket;
import com.audio.audioperf.api.audio.IAudioReceiver;
import com.audio.audioperf.audio.AudioUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class TileSpeaker extends BlockEntity implements IAudioReceiver {
    private final Set<Integer> packetIds = new HashSet<>();

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
        return side != facing;
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
}