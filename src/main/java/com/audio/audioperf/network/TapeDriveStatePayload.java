package com.audio.audioperf.network;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.tile.TapeDriveState.State;
import com.audio.audioperf.tile.TileTapeDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TapeDriveStatePayload(BlockPos pos, byte stateOrdinal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TapeDriveStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "tape_drive_state"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, TapeDriveStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeByte(p.stateOrdinal);
            },
            buf -> new TapeDriveStatePayload(buf.readBlockPos(), buf.readByte())
    );

    public static void handle(TapeDriveStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            if (level.getBlockEntity(payload.pos) instanceof TileTapeDrive td) {
                State[] states = State.VALUES;
                if (payload.stateOrdinal >= 0 && payload.stateOrdinal < states.length) {
                    td.switchState(states[payload.stateOrdinal]);
                    // Acknowledge the new state back to the clicking client so the
                    // GUI updates immediately even if the drive rejected the change.
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            (net.minecraft.server.level.ServerPlayer) context.player(),
                            new TapeDriveStateSyncPayload(payload.pos, (byte) td.getEnumState().ordinal()));
                }
            }
        });
    }
}
